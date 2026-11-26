#!/usr/bin/env python3
"""Emulator-free checks for the repair sweep helpers.

Run with: python3 tools/repair/test_sweep_helpers.py
"""

import pathlib
import sys
import unittest
import xml.etree.ElementTree as ET

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

import gallery_sweep as gs  # noqa: E402
import profile_sweep as ps  # noqa: E402
import sweep_common as sc  # noqa: E402

PACKAGE = "com.dotwelwidgets.dotwellabs"


class BudgetMathTest(unittest.TestCase):
    def test_61_widgets_exits_before_outer_wrapper(self):
        budget = sc.compute_graceful_budget(
            61, per_entry=16, overhead=75, outer=1250, margin=90
        )
        self.assertLessEqual(budget, 1160)
        self.assertGreater(budget, 0)
        self.assertEqual(budget, 1051)

    def test_budget_scales_with_catalog_growth(self):
        small = sc.compute_graceful_budget(
            57, per_entry=16, overhead=75, outer=1250, margin=90
        )
        large = sc.compute_graceful_budget(
            65, per_entry=16, overhead=75, outer=1250, margin=90
        )
        self.assertGreater(large, small)
        self.assertLessEqual(large, 1160)

    def test_ceiling_never_exceeds_outer_minus_margin(self):
        budget = sc.compute_graceful_budget(
            10000, per_entry=60, overhead=100, outer=1250, margin=90
        )
        self.assertEqual(budget, 1160)


class CatalogCountTest(unittest.TestCase):
    def test_reads_current_catalog_dynamically(self):
        count = sc.widget_catalog_count(".")
        ready = sc.widget_catalog_ready_count(".")
        self.assertGreaterEqual(count, 57)
        self.assertGreaterEqual(ready, 57)
        self.assertLessEqual(ready, count)

    def test_missing_catalog_is_zero_not_exception(self):
        self.assertEqual(sc.widget_catalog_count("/nonexistent"), 0)
        self.assertEqual(sc.widget_catalog_ready_count("/nonexistent"), 0)


class UidParsingTest(unittest.TestCase):
    def test_cmd_package_list_uid(self):
        text = (
            "package:com.example.other uid:1000\n"
            f"package:{PACKAGE} uid:10234\n"
        )
        self.assertEqual(sc.parse_uid_from_package_list(text, PACKAGE), 10234)

    def test_pm_list_uid(self):
        text = f"package:{PACKAGE} uid:10123\r\n"
        self.assertEqual(sc.parse_uid_from_package_list(text, PACKAGE), 10123)

    def test_package_prefix_is_not_matched(self):
        text = f"package:{PACKAGE}.debug uid:1111\n"
        self.assertIsNone(sc.parse_uid_from_package_list(text, PACKAGE))

    def test_dumpsys_user_id(self):
        text = (
            "Packages:\n"
            f"  Package [{PACKAGE}] (abc):\n"
            "    userId=10234\n"
        )
        self.assertEqual(sc.parse_uid_from_dumpsys(text), 10234)

    def test_parse_package_uid_falls_back_to_dumpsys(self):
        text = f"Package [{PACKAGE}]:\n    userId=10456\n"
        self.assertEqual(sc.parse_package_uid(text, PACKAGE), 10456)

    def test_empty_input(self):
        self.assertIsNone(sc.parse_package_uid("", PACKAGE))
        self.assertIsNone(sc.parse_package_uid(None, PACKAGE))


class TotalPssParsingTest(unittest.TestCase):
    def test_comma_grouped_total_pss(self):
        self.assertEqual(ps.total_pss_kb("TOTAL PSS:   18,732\n"), 18732)

    def test_plain_total_pss(self):
        self.assertEqual(ps.total_pss_kb("TOTAL PSS: 12345\n"), 12345)

    def test_total_line_variants(self):
        self.assertEqual(ps.total_pss_kb("  TOTAL: 9,001 kB\n"), 9001)
        self.assertEqual(ps.total_pss_kb("  TOTAL 8,500\n"), 8500)

    def test_missing_or_empty_text(self):
        self.assertIsNone(ps.total_pss_kb("No process found\n"))
        self.assertIsNone(ps.total_pss_kb(""))
        self.assertIsNone(ps.total_pss_kb(None))


class CrashDialogTest(unittest.TestCase):
    def test_aerr_close_id_detected(self):
        root = ET.fromstring(
            '<hierarchy><node resource-id="android:id/aerr_close" '
            'text="Close app" bounds="[0,0][10,10]"/></hierarchy>')
        self.assertIsNotNone(gs.crash_close_button(root))

    def test_close_app_text_detected(self):
        root = ET.fromstring(
            '<hierarchy><node text="Dotwel Widgets keeps stopping"/>'
            '<node text="Close app" bounds="[0,0][10,10]"/></hierarchy>')
        self.assertIsNotNone(gs.crash_close_button(root))

    def test_normal_gallery_is_not_a_crash_dialog(self):
        root = ET.fromstring(
            '<hierarchy><node text="CATEGORIES"/></hierarchy>')
        self.assertIsNone(gs.crash_close_button(root))


class DeadlineBudgetTest(unittest.TestCase):
    def test_absolute_deadline_wins_over_elapsed_budget(self):
        clock = [100.0]
        budget = sc.SweepBudget(
            budget_seconds=1000,
            deadline_epoch=105,
            monotonic=lambda: clock[0],
            wall_clock=lambda: clock[0],
        )
        self.assertEqual(budget.remaining(), 5)
        clock[0] = 105
        with self.assertRaises(sc.BudgetExhausted):
            budget.ensure(0)

    def test_reserve_raises_before_work(self):
        clock = [0.0]
        budget = sc.SweepBudget(
            budget_seconds=10,
            monotonic=lambda: clock[0],
            wall_clock=lambda: clock[0],
        )
        clock[0] = 9
        with self.assertRaises(sc.BudgetExhausted):
            budget.ensure(2)

    def test_timeout_is_capped_by_remaining(self):
        clock = [0.0]
        budget = sc.SweepBudget(
            budget_seconds=10,
            monotonic=lambda: clock[0],
            wall_clock=lambda: clock[0],
        )
        clock[0] = 8
        self.assertEqual(budget.timeout(30), 2)


class GalleryMarkerTest(unittest.TestCase):
    def _root(self, xml):
        return ET.fromstring(xml)

    def test_categories_text_is_a_gallery_marker(self):
        self.assertTrue(gs.gallery_marker(
            self._root('<hierarchy><node text="CATEGORIES"/></hierarchy>')))

    def test_ready_category_description_is_a_gallery_marker(self):
        self.assertTrue(gs.gallery_marker(self._root(
            '<hierarchy><node content-desc="Open Clock, 18 ready"/>'
            '</hierarchy>')))

    def test_launcher_home_is_not_a_gallery_marker(self):
        self.assertFalse(gs.gallery_marker(
            self._root('<hierarchy><node text="Home"/></hierarchy>')))

    def test_confirmation_ignores_open_category_sheet(self):
        sheet = self._root(
            '<hierarchy>'
            '<node content-desc="Close category"/>'
            '<node text="Add widget"/>'
            '</hierarchy>')
        self.assertIsNone(gs.confirmation(sheet))
        pin = self._root(
            '<hierarchy><node text="Add to home screen"/></hierarchy>')
        self.assertIsNotNone(gs.confirmation(pin))


if __name__ == "__main__":
    unittest.main(verbosity=2)
