# DotGrid — Nothing OS-Inspired Home Screen Widgets Suite

DotGrid is a monochromatic, dot-matrix Android home screen widget studio and launcher widget pack featuring **56 customizable widgets** with a dedicated focus on ultra-low-glare **OLED Night ("Nighting")** widgets, crisp **Light & Dark** themes, fluid Jetpack Compose motion, real-time data feeds, and a strict low-battery footprint.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The decisions below reflect your Phase 1 selections. Please review the confirmed choices and the recommended defaults before approving to begin implementation.

- **Confirmed — App Name & Brand**: **DotGrid**, styled with authentic monochrome dot-matrix minimalism and the signature Nothing red accent dot (`#D71921`).
- **Confirmed — Color & Nighting Approach**: Pure monochrome surfaces across **Light**, **Dark**, and **OLED Night (Nighting)** modes, paired with restrained red accent dots for live indicators, alerts, and active states (with an optional per-widget toggle to switch to strict monochrome).
- **Confirmed — Real-Time Data Integrations**:
  - **Live Weather, Sunrise/Sunset & Moon Phase** via keyless Open-Meteo REST + Geocoding APIs and an on-device astronomical lunar calculator.
  - **Live Device Telemetry** via Android system APIs (battery level/temp/charging, internal storage, RAM memory, connectivity, and uptime).
  - **Live World Clocks, Calendars, Pomodoro & Countdowns** powered by `java.time` and interactive local state.
- **Confirmed — Local On-Device Storage**: All customized widget presets, pinned widget bindings, habits, notes, countdowns, and shared weather caches are stored locally via **Room** and **DataStore** for zero-latency offline operation.
- **Default — Location Privacy & Zero-Friction Weather**: Uses optional foreground coarse location (`ACCESS_COARSE_LOCATION`, never background location) alongside built-in manual city search so weather widgets work immediately even without granting GPS access.
- **Default — Home Screen Pinning**: Supports one-tap **Pin to Home Screen** (`AppWidgetManager.requestPinAppWidget`) across `2×2`, `4×2`, and `4×4` resizable Android widget slots, plus an in-app live interactive preview wall so every widget can be tested, customized, and interacted with right inside DotGrid.

---

## 1. Overview & Core Concept

- **What It Does**: DotGrid gives Android users a curated library of **56 Nothing OS-inspired widgets** spanning 7 categories—led by a flagship **Nighting (Bedside & Night)** collection. Users can browse live-animated previews, fine-tune every visual and behavioral detail of each individual widget in a dedicated **Widget Customizer Studio**, save reusable personal presets in **My Widgets**, and pin any configured widget directly to their Android home screen.
- **Target Audience / Persona**: Minimalist Android enthusiasts, Nothing OS aesthetic fans, OLED phone users who want distraction-free low-glare bedside/night widgets, and productivity-minded users who want live glanceable telemetry without battery drain.
- **Key Value**: Combines hardware-inspired industrial design (dot-matrix numerals, precision hairlines, surgical red accent pips) with real live Android data, deep per-widget configurability, and a shared-cache low-power update architecture that respects battery life.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Discover & Nighting Spotlight (Explore Tab)**:
   - Opens to a sleek monochrome dot-grid header featuring a live **Theme Preview Switcher** (`Light` / `Dark` / `OLED Night`) that smoothly crossfades the entire gallery in real time so users can immediately inspect how widgets behave at night.
   - Features a dedicated **Nighting Essentials** horizontal carousel at the top, followed by 7 filterable category pills (`All 56`, `Nighting`, `Time`, `Weather`, `System`, `Focus`, `Calendar`, `Wellness`) and instant search.
   - Every widget card renders live real-time data (actual device battery, live clock tick, real Open-Meteo weather, live lunar illumination) inside an adaptive 2-column (phone) or multi-column (tablet) bento grid.
2. **Per-Widget Customizer Studio (Individual Widget Settings)**:
   - Tapping any widget smoothly transitions into the **Widget Studio** with a live, interactive hero preview on an authentic wallpaper-style dot-grid canvas (toggleable between Light, Dark, and OLED Night preview backdrops).
   - **Visual Controls**: Per-widget Theme (`Follow System`, `Light`, `Dark`, `OLED Night`, `Auto-Night Sunset–Sunrise`), Corner Radius (`12dp–32dp`), Surface Opacity (`40%–100%`), Dot Matrix Density (`Compact`, `Balanced`, `Expressive`), Frame Style (`Hairline`, `Dot Border`, `Borderless`), Red Accent Dot toggle, and Grid Footprint (`2×2`, `4×2`, `4×4`).
   - **Widget-Specific Data Controls**: Dynamically tailored per widget type—such as 12h/24h format, °C/°F unit, city/timezone picker for world clocks, custom title & target date for countdowns, custom checklist/note text, Pomodoro focus/break minutes, or daily hydration/habit goals.
   - **One-Tap Actions**: **Pin to Home Screen** (triggers native Android widget pinning and binds the configuration to the newly created `appWidgetId`) or **Save to My Widgets** for quick switching later.
3. **My Widgets & Interactive Dashboard (Studio Tab)**:
   - Displays all user-customized and home-screen-bound widgets in one place. Users can interact directly with stateful widgets (start/pause Pomodoro, log water intake, check off habits/tasks, increment counters), duplicate presets, edit settings, or re-pin to the home screen.
4. **Power, Data & Nighting Control (Settings Tab)**:
   - Manages global defaults (App Theme, Auto-Night schedule window, Weather City & GPS option, Temperature & Time formats).
   - Includes the **Low-Battery Footprint Dashboard**: lets users pick background sync intervals (`30 min`, `1 hr`, `3 hr`, `Manual Only`), toggle **Ultra-Eco Mode** (pauses background weather sync under 20% battery or when Battery Saver is active), inspect the last sync timestamp, and trigger an on-demand refresh.

### Visual Identity & Theme

- **Aesthetic Direction**: Industrial-minimal Nothing OS aesthetic—tactile dot-matrix typography, hairline geometric dividers, generous negative space, crisp pill contours, and zero visual clutter or generic gradients.
- **Color Palette & Mood** (mapped to Material 3 `ColorScheme` + custom `DotGridColors` extension):
  - *Light Mode*: Canvas `#F2F2F0`, Elevated Surface `#FFFFFF`, Secondary Pill `#E6E6E2`, Primary Ink `#111111`, Muted Ink `#666662`, Structural Dot `#D4D4CE`, Signature Accent `#D71921`.
  - *Dark Mode*: Canvas `#0E0E0E`, Elevated Surface `#1A1A1A`, Secondary Pill `#252525`, Primary Ink `#F5F5F3`, Muted Ink `#8E8E8A`, Structural Dot `#2D2D2A`, Signature Accent `#D71921`.
  - *OLED Night Mode ("Nighting")*: True Black `#000000` canvas & card base, Subtle Surface `#0A0A0A`, Dimmed Night Ink `#B8B8B2`, Low-Glare Muted `#5C5C58`, Structural Dot `#1A1A1A`, Night-Calibrated Red Dot `#B0161C` to prevent eye strain in pitch-dark rooms.
- **Typography & Hierarchy** (bundled locally via `font-util` into `res/font/`):
  - *Display & Dot-Matrix Numerals*: **DotGothic16** (authentic dot-matrix aesthetic for clock digits, temperatures, widget headlines, and badges).
  - *UI Headings & Controls*: **Space Grotesk** (clean neo-grotesque geometric clarity for navigation, customization sliders, and descriptions).
  - *Telemetry & Technical Metadata*: **JetBrains Mono** (tabular numerals for percentages, coordinates, uptime, and countdowns).
- **Component Styling & Adaptive Layout**:
  - Material 3 components styled with crisp `1.dp` hairline borders, micro dot-grid `Canvas` backgrounds, and minimum `48.dp` touch targets.
  - Responsive layout adapts from `NavigationBar` + 2-column widget grid on Compact phones to `NavigationRail` + 3–4 column grid and split-pane Widget Studio on Medium/Expanded tablets and foldables.
- **Interactive Feedback & Motion**:
  - Spring physics (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessMediumLow`) on card presses, toggle switches, and category pill selection.
  - Smooth shared-axis screen transitions (`AnimatedContent` / Navigation Compose fade + gentle scale) and staggered entrance reveals under 300ms.
  - Real-time animated previews inside the app (pulsing red active dot, sweeping analog hands, breathing pacer dot ring, smooth theme crossfades), while home-screen `RemoteViews` intentionally stay static between smart updates to preserve battery.

### Complete 56-Widget Catalog (7 Categories × 8 Widgets)

| Category | Included Widgets (8 per category = 56 total) |
| :--- | :--- |
| **1. Nighting & Bedside** | 1. Nightstand Dot Clock · 2. Lunar Phase Matrix · 3. Sunrise/Sunset Horizon · 4. Sleep Window Cycles · 5. Night Dimmer Mantra · 6. Circadian 24h Ribbon · 7. Starlight Red-Pip Analog · 8. Quiet Hours Wind-Down |
| **2. Time & World Clocks** | 9. Matrix Digital Clock · 10. 60-Dot Analog Dial · 11. Split-Flap Dot Clock · 12. Dual World Clock · 13. Quad Global Matrix · 14. Binary BCD Dot Clock · 15. Typographic Word Clock · 16. Epoch & Precision Telemetry |
| **3. Weather & Atmosphere** | 17. Dot Weather Hero · 18. Hourly Temp Sparkline · 19. 5-Day Matrix Forecast · 20. Feels Like & Humidity · 21. Wind Vector Compass · 22. 11-Dot UV Meter · 23. Rain Probability Bars · 24. Barometer & Dew Point |
| **4. Device Telemetry** | 25. Battery Dot Ring · 26. Battery Thermal & Health · 27. Storage Matrix Bar · 28. RAM Memory Gauge · 29. Network & Signal Card · 30. Device Uptime Matrix · 31. Trio System Dashboard · 32. Power Footprint Monitor |
| **5. Focus & Countdowns** | 33. Pomodoro Focus Ring · 34. Event Countdown · 35. Year Progress 365-Grid · 36. Day Progress Bar · 37. Quick 3-Task Checklist · 38. Sticky Dot Note · 39. Next Up Agenda Card · 40. Tap Stopwatch Matrix |
| **6. Calendar & Dates** | 41. Bold Date Tile · 42. Monthly Dot Calendar · 43. 7-Day Week Strip · 44. Day-of-Year Counter · 45. Quarter Progress Blocks · 46. Recurring Milestone Ring · 47. Workweek Dot Bar · 48. ISO Week & Lunar Pair |
| **7. Habits & Wellness** | 49. 7-Day Habit Streak · 50. Hydration 8-Dot Counter · 51. 4-7-8 Breathing Pacer · 52. Minimal Tally Counter · 53. Daily Focus Word · 54. 5-Dot Mood Logger · 55. Reading Page Tracker · 56. Minimalist Quote Card |

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Unified High-DPI Canvas Renderer for Home Screen Widgets (`RemoteViews` + Bitmap)**
  - *Chosen Approach*: Render customized home screen widgets via a dedicated high-DPI Android `Canvas` renderer backed by the same bundled dot-matrix fonts and per-widget configuration data, pushed into resizable `2×2`, `4×2`, and `4×4` `AppWidgetProvider` slots with interactive `PendingIntent` tap targets.
  - *Why*: Standard Android `RemoteViews` XML lacks custom corner radii, dot-matrix grids, circular dot arcs, and arbitrary opacity/night-mode palettes across API 24–36. Rendering crisp vector-sharp bitmaps scaled to the exact widget cell bounds guarantees 100% visual parity between the in-app Compose studio and the real home screen for all 56 widgets.
  - *Alternatives Considered*: Jetpack Glance or 56 separate XML `RemoteViews` layouts—Glance still compiles to restricted `RemoteViews` primitives that cannot draw dot-matrix arcs, binary grids, or variable dot borders cleanly, and 56 separate providers would bloat the launcher widget picker.
- **Decision 2: Low-Battery Architecture (Zero-Polling, Shared Cache & Constrained `WorkManager`)**
  - *Chosen Approach*: Use a single shared Open-Meteo weather cache in Room refreshed via battery-aware `WorkManager` (minimum 30-minute window, `requiresBatteryNotLow = true`, network-connected constraint, skipped automatically during Android Battery Saver if Ultra-Eco Mode is on). Device telemetry (battery, storage, RAM, network) is read passively on widget refresh or user tap—never via persistent foreground services or 1-second alarms.
  - *Why*: Home screen widgets that run frequent wake-locks or per-widget network calls destroy battery life. Sharing one weather snapshot across all 8 weather widgets cuts network & radio wakeups by up to 8×.
  - *Alternatives Considered*: Frequent `AlarmManager` exact alarms or persistent `ForegroundService`—rejected because both violate low-battery best practices and trigger Android battery warnings.
- **Decision 3: Three Clean Home Screen Provider Sizes (`2×2`, `4×2`, `4×4`) + Reconfigurable Config Activity**
  - *Chosen Approach*: Register 3 resizable `AppWidgetProvider` entry points (`DotGrid Small 2×2`, `DotGrid Wide 4×2`, `DotGrid Large 4×4`), each linked to a single Compose `WidgetConfigureActivity` and pin-able directly from inside DotGrid with any of the 56 widgets pre-selected.
  - *Why*: Keeps the Android launcher's widget drawer clean and uncluttered while giving users access to all 56 widgets in any size, and lets users tap a widget's corner/settings zone to reconfigure it anytime.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       PRESENTATION LAYER (Compose)                      │
│  ┌──────────────────┐  ┌─────────────────────┐  ┌────────────────────┐  │
│  │  Explore Gallery │  │ Widget Studio       │  │ My Widgets &       │  │
│  │  • Nighting Row  │─▶│ • Live Preview      │─▶│ Power/Settings Tab │  │
│  │  • 7 Categories  │  │ • Theme/Radius/Dots │  │ • Active Instances │  │
│  │  • 56 Widgets    │  │ • Pin / Save Action │  │ • Eco & Sync Stats │  │
│  └────────┬─────────┘  └──────────┬──────────┘  └─────────┬──────────┘  │
└───────────┼───────────────────────┼───────────────────────┼─────────────┘
            │ StateFlow             │ Events / Saves        │ Preferences
            ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      STATE & DOMAIN LAYER (MVVM)                        │
│  ┌──────────────────────┐  ┌─────────────────────┐  ┌────────────────┐  │
│  │ DotGridViewModel     │  │ LiveTelemetryEngine │  │ Lunar & Solar  │  │
│  │ • Filter/Search/Mode │  │ • Battery / Thermal │  │ Calculator     │  │
│  │ • Active Draft Edit  │  │ • Storage / RAM     │  │ • Moon Phase % │  │
│  │ • Interactive State  │  │ • Net / Uptime      │  │ • Auto-Night   │  │
│  └──────────┬───────────┘  └──────────┬──────────┘  └───────┬────────┘  │
└─────────────┼─────────────────────────┼─────────────────────┼───────────┘
              ▼                         ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 LOCAL PERSISTENCE & DATA LAYER (Offline-First)          │
│  ┌─────────────────────────────┐   ┌─────────────────────────────────┐  │
│  │ Room Database (KSP)         │   │ DataStore Preferences           │  │
│  │ • WidgetInstanceEntity      │   │ • Global Theme & Night Schedule │  │
│  │ • WeatherCacheEntity        │   │ • Weather City & Lat/Lon        │  │
│  │ • InteractiveWidgetState    │   │ • Sync Cadence & Eco Mode       │  │
│  └──────────────┬──────────────┘   └───────────────┬─────────────────┘  │
│                 │                                  │                    │
│                 ▼                                  ▼                    │
│  ┌─────────────────────────────┐   ┌─────────────────────────────────┐  │
│  │ Open-Meteo Retrofit Client  │   │ WorkManager (Battery-Safe Sync) │  │
│  │ • Forecast + Sunrise/Sunset │   │ • Single Shared Weather Fetch   │  │
│  │ • City Geocoding Search     │   │ • Throttled on Battery Saver    │  │
│  └─────────────────────────────┘   └───────────────┬─────────────────┘  │
└────────────────────────────────────────────────────┼────────────────────┘
                                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│              ANDROID HOME SCREEN APPWIDGET ENGINE                       │
│  ┌──────────────────────────────┐  ┌─────────────────────────────────┐  │
│  │ 3 Resizable AppWidgetProviders│  │ High-DPI Dot-Matrix Canvas      │  │
│  │ (2×2, 4×2, 4×4) + Pin API    │─▶│ Bitmap Renderer + PendingIntent │  │
│  │ + WidgetConfigureActivity    │  │ Interactive Tap Broadcasts      │  │
│  └──────────────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

- **Widget Catalog Definition (Immutable Registry)**: 56 structured widget specs containing `widgetTypeId`, category (`NIGHTING`, `TIME`, `WEATHER`, `SYSTEM`, `FOCUS`, `CALENDAR`, `WELLNESS`), supported grid sizes, default custom config, and interactive capability metadata.
- **Widget Instance (Room Entity)**: Persists every user-configured preset and pinned home-screen widget, keyed by local ID and optional Android `appWidgetId`. Stores theme mode (`SYSTEM`, `LIGHT`, `DARK`, `OLED_NIGHT`, `AUTO_NIGHT`), corner radius, opacity, dot density, border style, red-accent toggle, grid size, and widget-specific fields (custom title, subtitle/note, target epoch, timezone IDs, goal count, current count, checklist bitmask, Pomodoro state/end time, 7-day habit bitmask, mood index, quote index).
- **Weather Cache (Room Entity)**: Single latest snapshot for the user's chosen location (temperature, feels-like, humidity, wind speed/direction, UV index, pressure, dew point, weather code, sunrise/sunset epoch, hourly 6-point array, 5-day high/low array, `fetchedAtMillis`).
- **App Settings (DataStore Preferences)**: Global preview theme, Auto-Night start/end hours (default `21:00–06:30`), temperature unit (`°C`/`°F`), 24-hour clock toggle, selected city name + coordinates, sync interval minutes (`30`/`60`/`180`/`0`), and Ultra-Eco Mode toggle.

### Interactive Component & State Mapping

- **Theme Switcher Bar (Explore & Studio)** -> Updates global preview mode or per-widget theme override; animates surface/ink/dot color tokens via `animateColorAsState` and updates live Canvas previews immediately.
- **Category Chips & Search Input** -> Filters the 56-widget catalog via derived `StateFlow` without recomposing unaffected cards; empty search shows a dot-matrix empty state with a one-tap reset.
- **Widget Customizer Controls (Sliders, Segmented Pills, Text/Date/Timezone Pickers)** -> Mutates the active draft state with validation (non-empty titles, valid future dates, clamped goals), live-updates the hero widget preview, and persists to Room on **Save** or **Pin to Home Screen**.
- **Interactive Widget Tap Actions (In-App & Home Screen)** -> Dispatches typed actions (`TOGGLE_POMODORO`, `INCREMENT_HYDRATION`, `TOGGLE_HABIT_TODAY`, `TOGGLE_CHECKLIST_ITEM`, `INCREMENT_TALLY`, `CYCLE_MOOD`, `NEXT_QUOTE`, `REFRESH_WEATHER`) that atomically update Room state and trigger an immediate home-screen `AppWidgetManager.updateAppWidget` redraw.
- **City Search & Coarse Location Button** -> Queries Open-Meteo Geocoding API (with loading/error/retry states) or reads last known coarse location via `ActivityResultContracts.RequestPermission`, saves coordinates to DataStore, fetches fresh weather, and updates all weather widgets in one pass.

### Key Sequences & Integrations

1. **Pinning a Customized Widget to the Home Screen**:
   User customizes widget in Studio -> taps **Pin to Home Screen** -> app saves preset to Room -> invokes `AppWidgetManager.requestPinAppWidget` targeting the matching size provider (`2×2`, `4×2`, or `4×4`) with a pinned-callback `PendingIntent` -> on system callback, binds the new `appWidgetId` to the Room preset -> renders and pushes the dot-matrix `RemoteViews` immediately.
2. **Low-Battery Background Refresh**:
   `WorkManager` periodic job fires only when network is available and battery is not low -> checks Ultra-Eco / `PowerManager.isPowerSaveMode` -> fetches a single Open-Meteo JSON payload if cache age exceeds the configured cadence -> persists to `WeatherCacheEntity` -> broadcasts an update to all active DotGrid `appWidgetId`s -> finishes in milliseconds with zero lingering wake-locks.
