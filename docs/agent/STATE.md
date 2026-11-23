# Build and verification state

- Candidate: Dotwel Widgets 0.1.8 (versionCode 9), debug only. Package name
  `com.dotwelwidgets.dotwellabs` is unchanged.
- Branch: `agent/split-host-repair-45178491151`, based on `525b5de`. The
  separate launcher repository, default main, repository visibility and signing
  setup are unchanged.
- Scope of this run: repair the Android 15 native pin/placement inflation
  failure for the 12th provider, `KwgtDigital4WidgetProvider` (KWGT Split
  Clock). This was a real app defect, not a research task.

## Root cause

`app/src/main/res/layout/widget_kwgt_digital4.xml` used a raw
`<View android:id="@+id/kwgt_digital4_divider" ...>` for the centre divider.
RemoteViews only inflates classes annotated `@RemoteViews.RemoteView` (the
documented allowlist: `FrameLayout`, `LinearLayout`, `RelativeLayout`,
`TextClock`, `TextView`, `ImageView`, etc.). `android.view.View` is not on that
list, so both `initialLayout` and `previewLayout` inflation failed. On the
Android 15 host this surfaced as the native pin preview "Couldn't add widget"
and HOME never appearing.

## Fix

- Replaced the raw `<View>` divider with a static `<ImageView>` at
  `app/src/main/res/layout/widget_kwgt_digital4.xml`. The id, `1dp` width,
  `match_parent` height and `@color/dot_accent` (`#D71921`) background are
  preserved, so the source split composition and geometry are unchanged.
- Marked the divider decorative with `contentDescription="@null"` and
  `importantForAccessibility="no"`; the two live `TextClock` children are
  untouched.
- No provider Kotlin change was needed: `kwgt_digital4_divider` is not
  referenced at runtime and the fallback `LinearLayout` content description is
  unchanged.
- Audited the other new provider layouts (`widget_kwgt_datetime.xml`,
  `widget_kwgt_datetime2.xml`, `widget_kwgt_mini_calendar.xml`). None contains a
  raw `<View>`, `<Space>`, `<include>`, `<merge>` or custom view. The
  `ScrollView` and `DotHeadingView` are in `activity_main.xml` (an Activity
  layout, not a RemoteViews layout) and were not changed.
- Preserved all 12 registered providers, source composition, Mini Calendar fit,
  provenance, labels and the shared `MinuteTickScheduler`. No feature,
  permission, dependency, font or asset was added. NothingWidgetPort remains
  excluded; the launcher is untouched.

## Regression test

Added `app/src/test/java/com/dotwelwidgets/dotwellabs/RemoteViewsLayoutSafetyTest.kt`.
It reads the four shipped provider layout XML files and asserts every tag is on
the RemoteViews-supported list, and that the split-clock divider is a static
`1dp` `ImageView` with the accent background. Both tests failed against the
pre-fix layout and pass after the fix.

## Build and lint in this run

```
./gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon
```

Result: `BUILD SUCCESSFUL`. All unit tests pass. Lint: `0 errors, 103 warnings`
(no suppressions added or changed). Warnings are the pre-existing SDK/AGP,
API-gate, legacy-resource, plural, small-text, RTL-symmetry and static-context
notices.

The packaged layout was checked with `aapt2 dump xmltree`: the divider is an
`ImageView` with `contentDescription=@null` and
`importantForAccessibility=2` (`no`), not a raw `View`.

## Artifact

Debug APK: `checkpoints/dotwel-widgets-digital4-rvfix-debug.apk`.
SHA-256 in `checkpoints/DIGITAL4-RVFIX-SHA256SUMS`:

```
5acfc938fae85921ea41bc8e0bcd9fe5385da44a958ba5161cab7cbf3519cb41  checkpoints/dotwel-widgets-digital4-rvfix-debug.apk
```

Older checkpoint APKs and checksum files were left untouched.

## Verified versus not verified

Verified here: root cause, layout fix, regression tests, source compiles, debug
APK assembles, lint has no errors, the packaged layout no longer contains a raw
`View`, and all 12 providers/resources are still packaged.

Not verified here: actual Android-host native pin preview, HOME placement,
screenshots, resize, style switching, or minute advance. Runtime remains
pending. The independent native / plain-minute / styled-minute / tap-gate run
still has to be executed. No runtime or screenshot pass is claimed for the
split clock.

## Limits and remaining work

- README and `docs/KWGT-LEDGER.md` still describe the previous 11-provider
  checkpoint and still mark `Nothing_Digital4` pending. They were not rewritten
  in this narrow repair.
- The collection remains incomplete: the remaining KWGT presets are still
  pending or dependency-blocked.
- Weather, Photo, Away, Compass, Snake, media/notification, CPU and iOS
  Komponent presets remain in their prior blocked or pending states.
- OEM OnePlus entries and the NothingWidgetPort remain excluded.
- Play policy review, signing-key setup, repository visibility changes, launcher
  integration and default-main promotion remain outside this checkpoint.
