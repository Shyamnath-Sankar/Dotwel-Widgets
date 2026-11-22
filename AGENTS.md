# AGENTS.md - Dotwel Widgets build rules

Dotwel Widgets is a Nothing-OS-inspired Android home-screen widget app by Dotwel Labs.
It is a standalone app (not part of the Dotwel/NothingOS launcher repo). Widgets must work
on ANY launcher via the standard Android AppWidget framework.

## Identity
- applicationId: com.dotwelwidgets.dotwellabs (approved; never change without an explicit task)
- App name: Dotwel Widgets
- minSdk 26, target/compile latest stable, Kotlin, single `app` module

## Design language (Nothing OS feel)
- True black (#000000) AMOLED-friendly widget backgrounds, monochrome white elements
- Dot-matrix rendering for clocks/dates: render dots programmatically on Canvas/Bitmap.
  Do NOT use font files for the dot look.
- Nothing red accent (use sparingly, e.g. a single digit or hand)
- Rounded minimal shapes; no gradients, no skeuomorphism

## Hard rules
1. NEVER copy or ship proprietary assets: no Nothing or OnePlus fonts (ndot, NType82,
   Lettera Mono, OPlusSans), no images, smali, XML or code from decompiled apps.
   Reference repos (jaxparrow07/nothing-kde-widgets, SameerAlSahab/AwesomeAWidgets) are
   visual/behavior references ONLY. Reimplement everything from scratch.
2. Widgets use AppWidgetProvider + RemoteViews (Jetpack Glance is allowed if simpler).
3. Weather data (when added): Open-Meteo API, no API key, cache responses.
4. Small verified steps. Conventional Commits. Push after every commit.
5. Never edit .github/workflows/. Never change applicationId/minSdk/targetSdk unless tasked.
6. Do not claim done what you did not run. Update docs/agent/STATE.md before stopping.
7. No paid services, no new accounts, no signing keys. Debug APKs only for now.

## Widget roadmap (one per task unless told otherwise)
1. Dot-matrix digital clock (HH:MM)
2. Date widget (dot-matrix or minimal)
3. Battery widget (circular)
4. Weather widget (Open-Meteo, square + circular variants)
5. Analog clock
6. World clock
