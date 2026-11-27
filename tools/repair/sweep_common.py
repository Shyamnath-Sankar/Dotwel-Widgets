#!/usr/bin/env python3
"""Shared, dependency-free helpers for the repair sweeps.

The helpers are intentionally small so they can be unit-tested without an
emulator.  They cover three things the two sweeps kept getting wrong:

* a graceful budget that is bounded by both an elapsed budget and an absolute
  wall-clock deadline supplied by the outer workflow wrapper;
* package-uid parsing from the package-manager formats actually emitted by
  ``cmd package list packages -U``, ``pm list packages -U`` and
  ``dumpsys package``;
* reading the provider count from ``WidgetCatalog.kt`` instead of hardcoding
  the current catalog size.
"""

import json
import pathlib
import re
import time

OUTER_WRAPPER_SECONDS = 1250.0
TEARDOWN_MARGIN_SECONDS = 90.0
CATALOG_RELATIVE_PATH = pathlib.Path(
    "app/src/main/java/com/dotwelwidgets/dotwellabs/WidgetCatalog.kt"
)


class BudgetExhausted(RuntimeError):
    """Raised before a long operation would overrun the sweep budget."""


def compute_graceful_budget(
    widget_count,
    *,
    per_entry,
    overhead,
    outer=OUTER_WRAPPER_SECONDS,
    margin=TEARDOWN_MARGIN_SECONDS,
    minimum=60.0,
):
    """Return a budget that exits before the outer wrapper's timeout.

    ``outer - margin`` is the hard ceiling: the sweep must leave the outer
    wrapper at least ``margin`` seconds for teardown and evidence upload.
    ``overhead + widget_count * per_entry`` is the work estimate.  The smaller
    of the two wins, with ``minimum`` as a floor so tiny/empty catalogs still
    get a usable run.
    """

    ceiling = max(float(minimum), float(outer) - float(margin))
    estimate = float(overhead) + max(0, int(widget_count)) * float(per_entry)
    return round(max(float(minimum), min(ceiling, estimate)), 1)


class SweepBudget:
    """Track an elapsed budget and/or an absolute wall-clock deadline."""

    def __init__(
        self,
        *,
        budget_seconds=None,
        deadline_epoch=None,
        monotonic=time.monotonic,
        wall_clock=time.time,
    ):
        self.budget_seconds = (
            float(budget_seconds) if budget_seconds is not None else None
        )
        self.deadline_epoch = (
            float(deadline_epoch) if deadline_epoch is not None else None
        )
        self._monotonic = monotonic
        self._wall_clock = wall_clock
        self._start = monotonic()

    def remaining(self):
        values = []
        if self.budget_seconds is not None:
            values.append(
                self.budget_seconds - (self._monotonic() - self._start)
            )
        if self.deadline_epoch is not None:
            values.append(self.deadline_epoch - self._wall_clock())
        if not values:
            return float("inf")
        return min(values)

    def ensure(self, reserve=0.0, reason="sweep budget exhausted"):
        remaining = self.remaining()
        if remaining <= float(reserve):
            raise BudgetExhausted(reason)
        return remaining

    def timeout(self, cap, reserve=0.0):
        remaining = self.remaining() - float(reserve)
        if remaining <= 0:
            raise BudgetExhausted("sweep budget exhausted")
        return max(0.25, min(float(cap), remaining))


def parse_uid_from_package_list(text, package):
    """Parse ``package:<name> uid:<n>`` from cmd/pm package-list output."""

    if not text:
        return None
    match = re.search(
        r"(?m)^package:%s\s+uid:(\d+)\s*$" % re.escape(package), text
    )
    return int(match.group(1)) if match else None


def parse_uid_from_dumpsys(text):
    """Parse the ``userId=<n>`` line from ``dumpsys package <pkg>``."""

    if not text:
        return None
    match = re.search(r"\buserId=(\d+)", text)
    return int(match.group(1)) if match else None


def parse_package_uid(text, package):
    """Try the package-list format first, then the dumpsys format."""

    return parse_uid_from_package_list(text, package) or parse_uid_from_dumpsys(
        text
    )


def _catalog_entries_block(root="."):
    path = pathlib.Path(root) / CATALOG_RELATIVE_PATH
    try:
        text = path.read_text()
    except OSError:
        return ""
    marker = "val entries: List<CatalogEntry> = listOf("
    start = text.find(marker)
    if start < 0:
        return ""
    end = None
    for match in re.finditer(r"(?m)^    \)\s*$", text[start:]):
        end = start + match.start()
        break
    return text[start:end if end is not None else len(text)]


def _catalog_entry_chunks(root="."):
    """Split the catalog list into one chunk per entry.

    Some weather entries are built by a small ``weather(...)`` helper rather
    than by an inline ``CatalogEntry(...)``, so counting the constructor name
    alone undercounts the catalog.  Splitting on the indented entry starts
    keeps both forms visible.
    """

    block = _catalog_entries_block(root)
    parts = re.split(
        r"(?m)^        (?=(?:CatalogEntry|[a-z_][a-z0-9_]*)\(\s*$)", block
    )
    return [part for part in parts[1:] if part.strip()]


def widget_catalog_count(root="."):
    """Count every catalog entry in WidgetCatalog.kt."""

    return len(_catalog_entry_chunks(root))


def widget_catalog_ready_count(root="."):
    """Count entries that are READY or built by a READY-returning helper."""

    count = 0
    for chunk in _catalog_entry_chunks(root):
        if "ImportState.READY" in chunk:
            count += 1
        elif not chunk.startswith("CatalogEntry("):
            # Helper calls such as weather(...) return a READY CatalogEntry.
            count += 1
    return count


def ready_catalog_count(path="runtime-evidence/ready/catalog.json"):
    """Count the entries the ready instrumentation actually published."""

    try:
        data = json.loads(pathlib.Path(path).read_text())
    except (OSError, ValueError):
        return 0
    return len(data) if isinstance(data, list) else 0
