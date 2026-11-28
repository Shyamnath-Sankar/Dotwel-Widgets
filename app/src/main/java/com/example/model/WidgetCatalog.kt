package com.example.model

object WidgetCatalog {

    val allWidgets: List<WidgetSpec> = listOf(
        // =========================================================================
        // 1. NIGHTING & BEDSIDE (8 Widgets)
        // =========================================================================
        WidgetSpec(
            typeId = "night_bedside_clock",
            number = 1,
            name = "Nightstand Dot Clock",
            subtitle = "Zero-glare OLED dot-matrix bedside clock with red AM/PM pip",
            category = WidgetCategory.NIGHTING,
            defaultSize = WidgetGridSize.WIDE_4X2,
            isNightingFeatured = true
        ),
        WidgetSpec(
            typeId = "night_lunar_matrix",
            number = 2,
            name = "Lunar Phase Matrix",
            subtitle = "Real-time astronomical moon phase dot sphere & illumination",
            category = WidgetCategory.NIGHTING,
            defaultSize = WidgetGridSize.SMALL_2X2,
            isNightingFeatured = true
        ),
        WidgetSpec(
            typeId = "night_solar_horizon",
            number = 3,
            name = "Nocturnal Horizon Arc",
            subtitle = "On-device day/night solar arc aligned to your Auto-Night schedule",
            category = WidgetCategory.NIGHTING,
            defaultSize = WidgetGridSize.WIDE_4X2,
            isNightingFeatured = true
        ),
        WidgetSpec(
            typeId = "night_sleep_cycles",
            number = 4,
            name = "Sleep Window Cycles",
            subtitle = "Bedtime target & 90-minute REM sleep cycle dot blocks",
            category = WidgetCategory.NIGHTING,
            defaultSize = WidgetGridSize.WIDE_4X2,
            isNightingFeatured = true,
            customFieldType = WidgetCustomFieldType.SLEEP_TARGET,
            defaultTitle = "23:00",
            defaultGoal = 5
        ),
        WidgetSpec(
            typeId = "night_dimmer_mantra",
            number = 5,
            name = "Night Dimmer Mantra",
            subtitle = "Low-luminance nocturnal reflection for calm late hours",
            category = WidgetCategory.NIGHTING,
            defaultSize = WidgetGridSize.WIDE_4X2,
            isNightingFeatured = true,
            interactiveAction = WidgetInteractiveAction.NEXT_QUOTE,
            defaultTitle = "SILENCE IS A FORM OF CLARITY."
        ),
        WidgetSpec(
            typeId = "night_circadian_ribbon",
            number = 6,
            name = "Circadian 24h Ribbon",
            subtitle = "24-dot daily rhythm bar tracking your current hour",
            category = WidgetCategory.NIGHTING,
            defaultSize = WidgetGridSize.WIDE_4X2,
            isNightingFeatured = true
        ),
        WidgetSpec(
            typeId = "night_starlight_analog",
            number = 7,
            name = "Starlight Red-Pip Dial",
            subtitle = "Ultra-minimal OLED analog dial with single red hour pip",
            category = WidgetCategory.NIGHTING,
            defaultSize = WidgetGridSize.SMALL_2X2,
            isNightingFeatured = true
        ),
        WidgetSpec(
            typeId = "night_quiet_hours",
            number = 8,
            name = "Quiet Hours Status",
            subtitle = "Wind-down window indicator & auto-night schedule monitor",
            category = WidgetCategory.NIGHTING,
            defaultSize = WidgetGridSize.SMALL_2X2,
            isNightingFeatured = true,
            customFieldType = WidgetCustomFieldType.SLEEP_TARGET,
            defaultTitle = "22:00"
        ),

        // =========================================================================
        // 2. CONNECTIVITY, TOGGLES & BUDS (8 Widgets — Play-Policy Compliant Panels)
        // =========================================================================
        WidgetSpec(
            typeId = "ctl_wifi_toggle",
            number = 9,
            name = "Wi-Fi Toggle Tile",
            subtitle = "Live Wi-Fi ON/OFF dot arcs — tap opens Android Wi-Fi toggle panel",
            category = WidgetCategory.CONTROLS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            interactiveAction = WidgetInteractiveAction.OPEN_WIFI_PANEL
        ),
        WidgetSpec(
            typeId = "ctl_bluetooth_toggle",
            number = 10,
            name = "Bluetooth Toggle Tile",
            subtitle = "Live Bluetooth ON/OFF dot glyph — tap opens Bluetooth controls",
            category = WidgetCategory.CONTROLS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            interactiveAction = WidgetInteractiveAction.OPEN_BLUETOOTH_SETTINGS
        ),
        WidgetSpec(
            typeId = "ctl_internet_switch",
            number = 11,
            name = "Internet On / Off Pill",
            subtitle = "Live Wi-Fi & Cellular status — tap opens Android Internet panel",
            category = WidgetCategory.CONTROLS,
            defaultSize = WidgetGridSize.WIDE_4X2,
            interactiveAction = WidgetInteractiveAction.OPEN_INTERNET_PANEL
        ),
        WidgetSpec(
            typeId = "ctl_buds_card",
            number = 12,
            name = "Ear Buds Audio Card",
            subtitle = "Live wireless buds & headset connection badge + volume readout",
            category = WidgetCategory.CONTROLS,
            defaultSize = WidgetGridSize.WIDE_4X2,
            interactiveAction = WidgetInteractiveAction.OPEN_BLUETOOTH_SETTINGS,
            customFieldType = WidgetCustomFieldType.BUDS_LABEL,
            defaultTitle = "EAR (2)"
        ),
        WidgetSpec(
            typeId = "ctl_buds_volume",
            number = 13,
            name = "Buds Music Control",
            subtitle = "Live earphone output, equalizer & volume — tap to Play/Pause music",
            category = WidgetCategory.CONTROLS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            interactiveAction = WidgetInteractiveAction.TOGGLE_MEDIA_PLAY_PAUSE
        ),
        WidgetSpec(
            typeId = "ctl_quick_trio",
            number = 14,
            name = "Connectivity Trio Bar",
            subtitle = "Wi-Fi, Bluetooth & Internet ON/OFF indicators in one 4×2 strip",
            category = WidgetCategory.CONTROLS,
            defaultSize = WidgetGridSize.WIDE_4X2,
            interactiveAction = WidgetInteractiveAction.OPEN_INTERNET_PANEL
        ),
        WidgetSpec(
            typeId = "ctl_link_guard",
            number = 15,
            name = "Data & Link Guard",
            subtitle = "Shows active network link & metered state — tap opens Internet panel",
            category = WidgetCategory.CONTROLS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            interactiveAction = WidgetInteractiveAction.OPEN_INTERNET_PANEL
        ),
        WidgetSpec(
            typeId = "ctl_buds_night",
            number = 16,
            name = "Night Listening Buds",
            subtitle = "Low-glare OLED earbuds volume & Bluetooth status for bedtime",
            category = WidgetCategory.CONTROLS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            isNightingFeatured = true,
            interactiveAction = WidgetInteractiveAction.OPEN_AUDIO_PANEL,
            customFieldType = WidgetCustomFieldType.BUDS_LABEL,
            defaultTitle = "EAR BUDS"
        ),

        // =========================================================================
        // 3. PHOTO STUDIO & QUOTES (8 Widgets — Zero-Permission Android Photo Picker)
        // =========================================================================
        WidgetSpec(
            typeId = "studio_photo_mono",
            number = 17,
            name = "Studio Monochrome Photo",
            subtitle = "Zero-permission Photo Picker frame with high-contrast B&W filter",
            category = WidgetCategory.STUDIO,
            defaultSize = WidgetGridSize.SMALL_2X2,
            customFieldType = WidgetCustomFieldType.PHOTO_PICKER,
            defaultTitle = "FRAME 01"
        ),
        WidgetSpec(
            typeId = "studio_photo_polaroid",
            number = 18,
            name = "Studio Print Polaroid",
            subtitle = "Gallery matte photo print with custom title & signature red dot",
            category = WidgetCategory.STUDIO,
            defaultSize = WidgetGridSize.LARGE_4X4,
            customFieldType = WidgetCustomFieldType.PHOTO_PICKER,
            defaultTitle = "ARCHIVE · 2026"
        ),
        WidgetSpec(
            typeId = "studio_photo_circle",
            number = 19,
            name = "Dot-Aperture Portrait",
            subtitle = "Circular studio crop with dot-matrix viewfinder ring",
            category = WidgetCategory.STUDIO,
            defaultSize = WidgetGridSize.SMALL_2X2,
            customFieldType = WidgetCustomFieldType.PHOTO_PICKER,
            defaultTitle = "PORTRAIT"
        ),
        WidgetSpec(
            typeId = "studio_photo_wide",
            number = 20,
            name = "Cinematic Photo Strip",
            subtitle = "Wide 4×2 editorial studio photo frame with subtle caption bar",
            category = WidgetCategory.STUDIO,
            defaultSize = WidgetGridSize.WIDE_4X2,
            customFieldType = WidgetCustomFieldType.PHOTO_PICKER,
            defaultTitle = "STUDIO STILL"
        ),
        WidgetSpec(
            typeId = "studio_quote_stoic",
            number = 21,
            name = "Daily Stoic Quote",
            subtitle = "Timeless stoic philosophy in crisp monochrome — tap to cycle",
            category = WidgetCategory.STUDIO,
            defaultSize = WidgetGridSize.WIDE_4X2,
            interactiveAction = WidgetInteractiveAction.NEXT_QUOTE
        ),
        WidgetSpec(
            typeId = "studio_quote_design",
            number = 22,
            name = "Design Ethos Quote",
            subtitle = "Industrial & minimalist design maxims — tap for next principle",
            category = WidgetCategory.STUDIO,
            defaultSize = WidgetGridSize.WIDE_4X2,
            interactiveAction = WidgetInteractiveAction.NEXT_QUOTE
        ),
        WidgetSpec(
            typeId = "studio_quote_custom",
            number = 23,
            name = "Custom Personal Quote",
            subtitle = "Pin your own custom quote & author signature on the home screen",
            category = WidgetCategory.STUDIO,
            defaultSize = WidgetGridSize.WIDE_4X2,
            customFieldType = WidgetCustomFieldType.CUSTOM_QUOTE,
            defaultTitle = "PERSONAL ETHOS",
            defaultSecondaryText = "Build things that respect people's attention."
        ),
        WidgetSpec(
            typeId = "studio_photo_quote",
            number = 24,
            name = "Photo + Quote Studio",
            subtitle = "Editorial 4×2 split card combining your photo with a live quote",
            category = WidgetCategory.STUDIO,
            defaultSize = WidgetGridSize.WIDE_4X2,
            interactiveAction = WidgetInteractiveAction.NEXT_QUOTE,
            customFieldType = WidgetCustomFieldType.PHOTO_AND_QUOTE,
            defaultTitle = "STILLNESS",
            defaultSecondaryText = "Good design is as little design as possible."
        ),

        // =========================================================================
        // 4. TIME & WORLD CLOCKS (8 Widgets)
        // =========================================================================
        WidgetSpec(
            typeId = "time_matrix_digital",
            number = 25,
            name = "Matrix Digital Clock",
            subtitle = "Signature Nothing dot-matrix HH:MM with crisp date strip",
            category = WidgetCategory.TIME,
            defaultSize = WidgetGridSize.WIDE_4X2
        ),
        WidgetSpec(
            typeId = "time_analog_60dot",
            number = 26,
            name = "60-Dot Analog Dial",
            subtitle = "Perimeter 60-dot precision dial with red second marker",
            category = WidgetCategory.TIME,
            defaultSize = WidgetGridSize.SMALL_2X2
        ),
        WidgetSpec(
            typeId = "time_split_flap",
            number = 27,
            name = "Split-Flap Dot Clock",
            subtitle = "Mechanical dual-tile monochrome flip-board timepiece",
            category = WidgetCategory.TIME,
            defaultSize = WidgetGridSize.WIDE_4X2
        ),
        WidgetSpec(
            typeId = "time_dual_world",
            number = 28,
            name = "Dual World Clock",
            subtitle = "Local time side-by-side with your selected global city",
            category = WidgetCategory.TIME,
            defaultSize = WidgetGridSize.WIDE_4X2,
            customFieldType = WidgetCustomFieldType.TIMEZONE,
            defaultTitle = "Tokyo"
        ),
        WidgetSpec(
            typeId = "time_quad_global",
            number = 29,
            name = "Quad Global Matrix",
            subtitle = "Four major world timezones aligned in a crisp 2×2 grid",
            category = WidgetCategory.TIME,
            defaultSize = WidgetGridSize.LARGE_4X4
        ),
        WidgetSpec(
            typeId = "time_binary_bcd",
            number = 30,
            name = "Binary BCD Dot Clock",
            subtitle = "6-column binary-coded decimal dot matrix for purists",
            category = WidgetCategory.TIME,
            defaultSize = WidgetGridSize.SMALL_2X2
        ),
        WidgetSpec(
            typeId = "time_word_clock",
            number = 31,
            name = "Typographic Word Clock",
            subtitle = "Spelled-out uppercase dot-grid prose time readout",
            category = WidgetCategory.TIME,
            defaultSize = WidgetGridSize.SMALL_2X2
        ),
        WidgetSpec(
            typeId = "time_epoch_telemetry",
            number = 32,
            name = "Epoch & UTC Telemetry",
            subtitle = "Live Unix timestamp, UTC offset & ISO week number",
            category = WidgetCategory.TIME,
            defaultSize = WidgetGridSize.WIDE_4X2
        ),

        // =========================================================================
        // 5. DEVICE TELEMETRY & SYSTEM (8 Widgets)
        // =========================================================================
        WidgetSpec(
            typeId = "sys_battery_ring",
            number = 33,
            name = "Battery Dot Ring",
            subtitle = "Live battery percentage radial dot ring & charge status",
            category = WidgetCategory.SYSTEM,
            defaultSize = WidgetGridSize.SMALL_2X2
        ),
        WidgetSpec(
            typeId = "sys_battery_thermal",
            number = 34,
            name = "Battery Thermal & Volt",
            subtitle = "Live cell temperature °C, millivolts & hardware health",
            category = WidgetCategory.SYSTEM,
            defaultSize = WidgetGridSize.SMALL_2X2
        ),
        WidgetSpec(
            typeId = "sys_storage_matrix",
            number = 35,
            name = "Storage Matrix Bar",
            subtitle = "Live internal flash storage used vs free dot bar",
            category = WidgetCategory.SYSTEM,
            defaultSize = WidgetGridSize.WIDE_4X2
        ),
        WidgetSpec(
            typeId = "sys_ram_gauge",
            number = 36,
            name = "RAM Memory Gauge",
            subtitle = "Real-time system RAM pressure & available memory",
            category = WidgetCategory.SYSTEM,
            defaultSize = WidgetGridSize.SMALL_2X2
        ),
        WidgetSpec(
            typeId = "sys_network_signal",
            number = 37,
            name = "Network & Link Card",
            subtitle = "Active Wi-Fi/Cellular link, metered status & online pip",
            category = WidgetCategory.SYSTEM,
            defaultSize = WidgetGridSize.SMALL_2X2,
            interactiveAction = WidgetInteractiveAction.OPEN_INTERNET_PANEL
        ),
        WidgetSpec(
            typeId = "sys_uptime_counter",
            number = 38,
            name = "System Uptime Matrix",
            subtitle = "Live hours and minutes elapsed since last device boot",
            category = WidgetCategory.SYSTEM,
            defaultSize = WidgetGridSize.SMALL_2X2
        ),
        WidgetSpec(
            typeId = "sys_trio_dashboard",
            number = 39,
            name = "Trio System Dashboard",
            subtitle = "Battery, Storage & RAM aligned in a single 4×2 monitor",
            category = WidgetCategory.SYSTEM,
            defaultSize = WidgetGridSize.WIDE_4X2
        ),
        WidgetSpec(
            typeId = "sys_power_footprint",
            number = 40,
            name = "Power Footprint Badge",
            subtitle = "Shows zero-network offline eco status & battery saver mode",
            category = WidgetCategory.SYSTEM,
            defaultSize = WidgetGridSize.SMALL_2X2
        ),

        // =========================================================================
        // 6. FOCUS, TASKS & CALENDAR (8 Widgets)
        // =========================================================================
        WidgetSpec(
            typeId = "focus_pomodoro_ring",
            number = 41,
            name = "Pomodoro Focus Ring",
            subtitle = "Tap-to-start monochrome focus timer with radial dot progress",
            category = WidgetCategory.FOCUS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            interactiveAction = WidgetInteractiveAction.TOGGLE_POMODORO,
            customFieldType = WidgetCustomFieldType.POMODORO_MINUTES,
            defaultTitle = "DEEP FOCUS",
            defaultGoal = 25
        ),
        WidgetSpec(
            typeId = "focus_event_countdown",
            number = 42,
            name = "Event Countdown",
            subtitle = "Days and hours remaining to your milestone target date",
            category = WidgetCategory.FOCUS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            customFieldType = WidgetCustomFieldType.COUNTDOWN_EVENT,
            defaultTitle = "LAUNCH DAY"
        ),
        WidgetSpec(
            typeId = "focus_year_365",
            number = 43,
            name = "Year Progress Matrix",
            subtitle = "Visual dot matrix of the year elapsed with red today pip",
            category = WidgetCategory.FOCUS,
            defaultSize = WidgetGridSize.WIDE_4X2
        ),
        WidgetSpec(
            typeId = "focus_quick_checklist",
            number = 44,
            name = "Quick 3-Task Checklist",
            subtitle = "Tap to cycle & check off your 3 highest-priority tasks",
            category = WidgetCategory.FOCUS,
            defaultSize = WidgetGridSize.WIDE_4X2,
            interactiveAction = WidgetInteractiveAction.CYCLE_CHECKLIST,
            customFieldType = WidgetCustomFieldType.CHECKLIST_ITEMS,
            defaultTitle = "TODAY",
            defaultSecondaryText = "Ship release build|Review design tokens|Evening run"
        ),
        WidgetSpec(
            typeId = "focus_sticky_note",
            number = 45,
            name = "Sticky Dot Note",
            subtitle = "Stark monospace quick memo pinned right on your home screen",
            category = WidgetCategory.FOCUS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            customFieldType = WidgetCustomFieldType.NOTE_TEXT,
            defaultTitle = "MEMO",
            defaultSecondaryText = "Less noise. More signal."
        ),
        WidgetSpec(
            typeId = "cal_bold_date",
            number = 46,
            name = "Bold Date Tile",
            subtitle = "Oversized dot-matrix day numeral with weekday & month pill",
            category = WidgetCategory.FOCUS,
            defaultSize = WidgetGridSize.SMALL_2X2
        ),
        WidgetSpec(
            typeId = "cal_month_grid",
            number = 47,
            name = "Monthly Dot Calendar",
            subtitle = "Full current month grid highlighting today in signature red",
            category = WidgetCategory.FOCUS,
            defaultSize = WidgetGridSize.LARGE_4X4
        ),
        WidgetSpec(
            typeId = "cal_week_strip",
            number = 48,
            name = "7-Day Week Strip",
            subtitle = "Horizontal monochrome week ribbon with active day pill",
            category = WidgetCategory.FOCUS,
            defaultSize = WidgetGridSize.WIDE_4X2
        ),

        // =========================================================================
        // 7. HABITS, WELLNESS & LIFESTYLE (8 Widgets)
        // =========================================================================
        WidgetSpec(
            typeId = "well_habit_streak",
            number = 49,
            name = "7-Day Habit Streak",
            subtitle = "Tap to log today's check-in across a 7-dot streak bar",
            category = WidgetCategory.WELLNESS,
            defaultSize = WidgetGridSize.WIDE_4X2,
            interactiveAction = WidgetInteractiveAction.TOGGLE_HABIT_TODAY,
            customFieldType = WidgetCustomFieldType.HABIT_GOAL,
            defaultTitle = "MORNING WALK",
            defaultGoal = 7
        ),
        WidgetSpec(
            typeId = "well_hydration_dots",
            number = 50,
            name = "Hydration 8-Dot Counter",
            subtitle = "Tap +1 glass to fill your daily monochrome hydration matrix",
            category = WidgetCategory.WELLNESS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            interactiveAction = WidgetInteractiveAction.INCREMENT_HYDRATION,
            customFieldType = WidgetCustomFieldType.HABIT_GOAL,
            defaultTitle = "WATER",
            defaultGoal = 8
        ),
        WidgetSpec(
            typeId = "well_breathing_478",
            number = 51,
            name = "4-7-8 Breathing Pacer",
            subtitle = "Calming dot-ring breathing cadence for night wind-down",
            category = WidgetCategory.WELLNESS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            isNightingFeatured = true,
            interactiveAction = WidgetInteractiveAction.TOGGLE_BREATHING,
            defaultTitle = "BREATHE"
        ),
        WidgetSpec(
            typeId = "well_tally_counter",
            number = 52,
            name = "Minimal Tally Counter",
            subtitle = "One-tap counter for coffees, reps, pages, or daily wins",
            category = WidgetCategory.WELLNESS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            interactiveAction = WidgetInteractiveAction.INCREMENT_TALLY,
            customFieldType = WidgetCustomFieldType.TALLY_LABEL,
            defaultTitle = "ESPRESSO",
            defaultGoal = 10
        ),
        WidgetSpec(
            typeId = "well_focus_word",
            number = 53,
            name = "Daily Focus Word",
            subtitle = "Single high-impact dot-matrix intention anchoring your day",
            category = WidgetCategory.WELLNESS,
            defaultSize = WidgetGridSize.WIDE_4X2,
            customFieldType = WidgetCustomFieldType.FOCUS_WORD,
            defaultTitle = "CLARITY"
        ),
        WidgetSpec(
            typeId = "well_mood_dots",
            number = 54,
            name = "5-Dot Mood Logger",
            subtitle = "Tap to log current energy state across 5 tactile monochrome dots",
            category = WidgetCategory.WELLNESS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            interactiveAction = WidgetInteractiveAction.CYCLE_MOOD,
            defaultTitle = "STATE"
        ),
        WidgetSpec(
            typeId = "well_reading_tracker",
            number = 55,
            name = "Reading Page Tracker",
            subtitle = "Tap +10 pages to advance your current book dot bar",
            category = WidgetCategory.WELLNESS,
            defaultSize = WidgetGridSize.WIDE_4X2,
            interactiveAction = WidgetInteractiveAction.INCREMENT_PAGE,
            customFieldType = WidgetCustomFieldType.READING_BOOK,
            defaultTitle = "DIETER RAMS",
            defaultGoal = 240
        ),
        WidgetSpec(
            typeId = "focus_stopwatch",
            number = 56,
            name = "Tap Stopwatch Matrix",
            subtitle = "Precision tap-to-toggle elapsed stopwatch with red active pip",
            category = WidgetCategory.WELLNESS,
            defaultSize = WidgetGridSize.SMALL_2X2,
            interactiveAction = WidgetInteractiveAction.TOGGLE_STOPWATCH,
            defaultTitle = "STOPWATCH"
        )
    )

    val byId: Map<String, WidgetSpec> = allWidgets.associateBy { it.typeId }

    fun getSpec(typeId: String): WidgetSpec = byId[typeId] ?: allWidgets.first()

    fun createDefaultConfig(
        spec: WidgetSpec,
        defaultTheme: WidgetThemeMode = WidgetThemeMode.OLED_NIGHT,
        use24Hour: Boolean = true
    ): WidgetConfig {
        val defaultTarget = System.currentTimeMillis() + 14L * 24L * 60L * 60L * 1000L
        return WidgetConfig(
            widgetTypeId = spec.typeId,
            customName = spec.name,
            themeMode = if (spec.category == WidgetCategory.NIGHTING) WidgetThemeMode.OLED_NIGHT else defaultTheme,
            gridSize = spec.defaultSize,
            cornerRadiusDp = 24,
            backgroundOpacity = 1.0f,
            dotDensity = DotDensity.BALANCED,
            borderStyle = WidgetBorderStyle.HAIRLINE,
            showRedAccent = true,
            showDotBackdrop = true,
            use24Hour = use24Hour,
            monochromePhoto = true,
            photoPath = "",
            timezoneId = "Asia/Tokyo",
            customTitle = spec.defaultTitle.ifBlank { spec.name.uppercase() },
            customNote = spec.defaultSecondaryText,
            targetEpochMillis = defaultTarget,
            goalValue = spec.defaultGoal,
            currentValue = when (spec.typeId) {
                "well_hydration_dots" -> 4
                "well_mood_dots" -> 3
                "well_reading_tracker" -> 80
                "well_tally_counter" -> 2
                else -> 0
            },
            bitmaskState = when (spec.typeId) {
                "well_habit_streak" -> 0b0111011
                "focus_quick_checklist" -> 0b001
                else -> 0
            },
            timerDurationSec = if (spec.typeId == "focus_pomodoro_ring") spec.defaultGoal * 60 else 1500
        )
    }

    val nightMantras: List<String> = listOf(
        "SILENCE IS A FORM OF CLARITY.",
        "REST NOW. BUILD TOMORROW.",
        "LESS LIGHT. DEEPER SLEEP.",
        "STILLNESS SHARPENS THE MIND.",
        "DISCONNECT TO RECONNECT.",
        "THE NIGHT BELONGS TO CALM."
    )

    val stoicQuotes: List<Pair<String, String>> = listOf(
        "You have power over your mind—not outside events. Realize this, and you will find strength." to "MARCUS AURELIUS",
        "Waste no more time arguing what a good person should be. Be one." to "MARCUS AURELIUS",
        "Luck is what happens when preparation meets opportunity." to "SENECA",
        "We suffer more often in imagination than in reality." to "SENECA",
        "Wealth consists not in having great possessions, but in having few wants." to "EPICTETUS",
        "The obstacle is the way." to "MARCUS AURELIUS"
    )

    val designQuotes: List<Pair<String, String>> = listOf(
        "Good design is as little design as possible." to "DIETER RAMS",
        "Simplicity is the ultimate sophistication." to "LEONARDO DA VINCI",
        "Perfection is achieved when there is nothing left to take away." to "SAINT-EXUPÉRY",
        "Make technology feel invisible, honest, and human." to "INDUSTRIAL ETHOS",
        "Details are not the details. They make the design." to "CHARLES EAMES",
        "Focus is saying no to a hundred good ideas." to "MINIMALISM"
    )

    val curatedTimezones: List<Pair<String, String>> = listOf(
        "America/New_York" to "New York",
        "America/Los_Angeles" to "Los Angeles",
        "Europe/London" to "London",
        "Europe/Berlin" to "Berlin",
        "Asia/Dubai" to "Dubai",
        "Asia/Kolkata" to "Bengaluru",
        "Asia/Singapore" to "Singapore",
        "Asia/Tokyo" to "Tokyo",
        "Australia/Sydney" to "Sydney"
    )
}
