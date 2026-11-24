package com.dotwelwidgets.dotwellabs

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build

enum class WidgetGroup(val labelRes: Int) {
    CLOCKS(R.string.category_clocks),
    TIME(R.string.category_time),
    WEATHER(R.string.category_weather),
    MEDIA(R.string.category_media),
    MEDIA_PLAYER(R.string.category_media_player),
    KWGT(R.string.category_kwgt),
    OEM(R.string.category_oem),
}

enum class ImportState(val labelRes: Int) {
    READY(R.string.status_ready),
    PENDING(R.string.status_pending),
    BLOCKED(R.string.status_blocked),
    UNAVAILABLE(R.string.status_unavailable),
}

enum class PreviewKind {
    CLOCK,
    DATE,
    ANALOG_BOLD,
    ANALOG_SCALE,
    WORLD,
    QUOTES,
    KWGT_BATTERY,
    KWGT_BATTERY_MINI,
    KWGT_BATTERY_2,
    KWGT_DATE_TIME,
    KWGT_DATE_TIME_2,
    KWGT_DIGITAL_3,
    KWGT_DIGITAL_4,
    KWGT_GRADIENT_CLOCK,
    KWGT_ANALOG_2,
    KWGT_FUTURE,
    KWGT_ANALOG_3,
    KWGT_ANALOG_SECONDS,
    KWGT_ANALOG_5,
    KWGT_ANALOG_5_CLEAR,
    KWGT_ORBIT,
    KWGT_MINI_CALENDAR,
    KWGT_CLEAN_INFO,
    KWGT_IOS_BATTERY,
    KWGT_DIGITAL_2,
    KWGT_DATE_TIME_3,
    INTERSTELLAR_ENDURANCE,
    INTERSTELLAR_ENDURANCE_ANALOG,
    INTERSTELLAR_ENDURANCE_ANALOG_2,
    MEDIA_M3_EXPANDED,
    MEDIA_M3_OUTPUT,
    MEDIA_MINI,
    MEDIA_MINIMAL,
    MEDIA_MINIMAL_2,
    MEDIA_MINIMAL_3,
    MEDIA_MASTER,
    MEDIA_PLAYER,
    MEDIA_PLAYER_2,
    MEDIA_PLAYER_3,
    MEDIA_UNIVERSAL,
    MEDIA_NOTIFICATION,
    WX_TEMP,
    WX_SUN,
    PLACEHOLDER,
}

enum class CardAction {
    NONE,
    CLOCK_ACCESS,
    WORLD_ZONES,
    WEATHER,
    MEDIA_ACCESS,
}

data class CatalogEntry(
    val id: String,
    val name: String,
    val group: WidgetGroup,
    val state: ImportState,
    val upstream: String,
    val descriptionRes: Int,
    val detailRes: Int,
    val previewKind: PreviewKind,
    val providerClass: Class<out BaseWidgetProvider>? = null,
    val pinKind: String? = null,
    val action: CardAction = CardAction.NONE,
)

object WidgetCatalog {
    private val analogSecondsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val entries: List<CatalogEntry> = listOf(
        CatalogEntry(
            id = "digital",
            name = "Digital Clock",
            group = WidgetGroup.CLOCKS,
            state = ImportState.READY,
            upstream = "NtDigital.build · NtDraw.SCHEME/fit/center/surface",
            descriptionRes = R.string.catalog_digital_desc,
            detailRes = R.string.ledger_ready,
            previewKind = PreviewKind.CLOCK,
            providerClass = ClockWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_CLOCK,
            action = CardAction.CLOCK_ACCESS,
        ),
        CatalogEntry(
            id = "analog_bold",
            name = "Analog Bold",
            group = WidgetGroup.CLOCKS,
            state = ImportState.READY,
            upstream = "NtAnalogBold.build layout selection · NtDraw palette",
            descriptionRes = R.string.catalog_analog_bold_desc,
            detailRes = R.string.ledger_ready,
            previewKind = PreviewKind.ANALOG_BOLD,
            providerClass = AnalogBoldWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_ANALOG_BOLD,
            action = CardAction.CLOCK_ACCESS,
        ),
        CatalogEntry(
            id = "analog_scale",
            name = "Analog Scale",
            group = WidgetGroup.CLOCKS,
            state = ImportState.READY,
            upstream = "NtAnalogScale.build layout selection · NtDraw palette",
            descriptionRes = R.string.catalog_analog_scale_desc,
            detailRes = R.string.ledger_ready,
            previewKind = PreviewKind.ANALOG_SCALE,
            providerClass = AnalogScaleWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_ANALOG_SCALE,
            action = CardAction.CLOCK_ACCESS,
        ),
        CatalogEntry(
            id = "date",
            name = "Date",
            group = WidgetGroup.TIME,
            state = ImportState.READY,
            upstream = "NtDate.build · NtDraw.fit/center/surface",
            descriptionRes = R.string.catalog_date_desc,
            detailRes = R.string.ledger_ready,
            previewKind = PreviewKind.DATE,
            providerClass = DateWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_DATE,
        ),
        CatalogEntry(
            id = "world",
            name = "World Clock",
            group = WidgetGroup.TIME,
            state = ImportState.READY,
            upstream = "NtWorld.build · zones chosen in app before pin",
            descriptionRes = R.string.catalog_world_desc,
            detailRes = R.string.ledger_ready,
            previewKind = PreviewKind.WORLD,
            providerClass = WorldWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_WORLD,
            action = CardAction.WORLD_ZONES,
        ),
        CatalogEntry(
            id = "away",
            name = "Away Time",
            group = WidgetGroup.TIME,
            state = ImportState.PENDING,
            upstream = "NtAway.build · screen-interactive sampling",
            descriptionRes = R.string.catalog_away_desc,
            detailRes = R.string.ledger_pending_away,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
        weather(
            id = "wx_circle",
            name = "Weather Circle",
            upstream = "NtWxCircle · NtWxData/NtWxDraw/NtWxFetch",
        ),
        weather(
            id = "wx_square",
            name = "Weather Square",
            upstream = "NtWxSquare · NtWxData/NtWxDraw/NtWxFetch",
        ),
        weather(
            id = "wx_rect",
            name = "Weather Forecast",
            upstream = "NtWxRect · NtWxData/NtWxDraw/NtWxFetch",
        ),
        weather(
            id = "wx_combo",
            name = "Weather Combo",
            upstream = "NtWxCombo · NtWxData/NtWxDraw/NtWxFetch",
        ),
        weather(
            id = "wx_sunset",
            name = "Weather Sunrise/Sunset",
            upstream = "NtWxSunset · NtWxData/NtWxDraw/NtWxFetch",
        ),
        weather(
            id = "wx_air",
            name = "Weather Air Quality",
            upstream = "NtWxAir · NtWxData/NtWxDraw/NtWxFetch",
        ),
        weather(
            id = "wx_w1temp",
            name = "Weather 1x1 Temperature",
            upstream = "NtWxW1temp · NtWxData/NtWxDraw/NtWxFetch",
        ),
        weather(
            id = "wx_w1status",
            name = "Weather 1x1 Condition",
            upstream = "NtWxW1status · NtWxData/NtWxDraw/NtWxFetch",
        ),
        weather(
            id = "wx_w1rain",
            name = "Weather 1x1 Rain Chance",
            upstream = "NtWxW1rain · NtWxData/NtWxDraw/NtWxFetch",
        ),
        CatalogEntry(
            id = "wx_nothing_temp",
            name = "Nothing Temp",
            group = WidgetGroup.WEATHER,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Temp · Open-Meteo forecast · optional manual city",
            descriptionRes = R.string.catalog_nothing_temp_desc,
            detailRes = R.string.ledger_ready_weather,
            previewKind = PreviewKind.WX_TEMP,
            providerClass = NothingTempWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_WEATHER_TEMP,
            action = CardAction.WEATHER,
        ),
        CatalogEntry(
            id = "wx_nothing_sun",
            name = "Nothing Sun Info",
            group = WidgetGroup.WEATHER,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Sun_Info · Open-Meteo sunrise/sunset · optional manual city",
            descriptionRes = R.string.catalog_nothing_sun_desc,
            detailRes = R.string.ledger_ready_weather,
            previewKind = PreviewKind.WX_SUN,
            providerClass = NothingSunInfoWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_WEATHER_SUN,
            action = CardAction.WEATHER,
        ),
        CatalogEntry(
            id = "photo_circle",
            name = "Photo (Circle)",
            group = WidgetGroup.MEDIA,
            state = ImportState.PENDING,
            upstream = "NtPhotoCircle · NtPhoto.render/config",
            descriptionRes = R.string.catalog_photo_circle_desc,
            detailRes = R.string.ledger_pending_photo,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
        CatalogEntry(
            id = "photo_rect",
            name = "Photo (Rectangle)",
            group = WidgetGroup.MEDIA,
            state = ImportState.PENDING,
            upstream = "NtPhotoRect · NtPhoto.render/config",
            descriptionRes = R.string.catalog_photo_rect_desc,
            detailRes = R.string.ledger_pending_photo,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
        CatalogEntry(
            id = "quotes",
            name = "Quotes",
            group = WidgetGroup.MEDIA,
            state = ImportState.READY,
            upstream = "NtQuotes.build · assets/nt_quotes.csv",
            descriptionRes = R.string.catalog_quotes_desc,
            detailRes = R.string.ledger_ready,
            previewKind = PreviewKind.QUOTES,
            providerClass = QuotesWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_QUOTES,
        ),
        CatalogEntry(
            id = "kwgt_nothing_battery",
            name = "Battery Gauge",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Battery · level*3.6 gauge · cm(215,g,b)",
            descriptionRes = R.string.catalog_kwgt_battery_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_BATTERY,
            providerClass = BatteryWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_BATTERY,
        ),
        CatalogEntry(
            id = "kwgt_nothing_battery_mini",
            name = "Battery Mini",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Battery_Mini · 11 concentric level rings",
            descriptionRes = R.string.catalog_kwgt_battery_mini_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_BATTERY_MINI,
            providerClass = BatteryMiniWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_BATTERY_MINI,
        ),
        CatalogEntry(
            id = "kwgt_nothing_battery2",
            name = "KWGT Battery2",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Battery2 · 16-segment body · real level/charging",
            descriptionRes = R.string.catalog_kwgt_battery2_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_BATTERY_2,
            providerClass = KwgtBattery2WidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_BATTERY_2,
        ),
        CatalogEntry(
            id = "kwgt_nothing_date_time",
            name = "KWGT Date Time",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Date_Time · EEE / hh:mma / MMMM / d ordinal stack",
            descriptionRes = R.string.catalog_kwgt_date_time_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_DATE_TIME,
            providerClass = KwgtDateTimeWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_DATE_TIME,
            action = CardAction.CLOCK_ACCESS,
        ),
        CatalogEntry(
            id = "kwgt_nothing_date_time2",
            name = "KWGT Date Time 2",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Date_Time2 · EEE / huge d / hour and minute disc rows",
            descriptionRes = R.string.catalog_kwgt_date_time2_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_DATE_TIME_2,
            providerClass = KwgtDateTime2WidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_DATE_TIME_2,
            action = CardAction.CLOCK_ACCESS,
        ),
        CatalogEntry(
            id = "kwgt_nothing_digital4",
            name = "KWGT Split Clock",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Digital4 · red hh / centre divider / ink mm",
            descriptionRes = R.string.catalog_kwgt_digital4_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_DIGITAL_4,
            providerClass = KwgtDigital4WidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_DIGITAL_4,
            action = CardAction.CLOCK_ACCESS,
        ),
        CatalogEntry(
            id = "kwgt_nothing_digital3",
            name = "KWGT Digital3 Dot Clock",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Digital3 · 4x4 minute-lit dot matrix / hh / mm / aa",
            descriptionRes = R.string.catalog_kwgt_digital3_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_DIGITAL_3,
            providerClass = KwgtDigital3WidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_DIGITAL_3,
            action = CardAction.CLOCK_ACCESS,
        ),
        CatalogEntry(
            id = "kwgt_gradient_clock",
            name = "KWGT Gradient Clock",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Digital · red edge gradient circles / hh / minute chip",
            descriptionRes = R.string.catalog_kwgt_gradient_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_GRADIENT_CLOCK,
            providerClass = KwgtGradientClockWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_GRADIENT_CLOCK,
            action = CardAction.CLOCK_ACCESS,
        ),
        CatalogEntry(
            id = "kwgt_analog2_clock",
            name = "KWGT Analog2",
            group = WidgetGroup.KWGT,
            state = if (analogSecondsSupported) {
                ImportState.READY
            } else {
                ImportState.UNAVAILABLE
            },
            upstream = "KWGT Nothing_Analog_2 · 277 dial / tick rings / red seconds",
            descriptionRes = R.string.catalog_kwgt_analog2_desc,
            detailRes = if (analogSecondsSupported) {
                R.string.ledger_ready_analog2
            } else {
                R.string.ledger_unavailable_pre31
            },
            previewKind = PreviewKind.KWGT_ANALOG_2,
            providerClass = KwgtAnalog2WidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_ANALOG_2,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_future_clock",
            name = "KWGT Future Clock",
            group = WidgetGroup.KWGT,
            state = if (analogSecondsSupported) {
                ImportState.READY
            } else {
                ImportState.UNAVAILABLE
            },
            upstream = "KWGT Nothing_Future_Clock · three sweep discs / hour / minute / second",
            descriptionRes = R.string.catalog_kwgt_future_desc,
            detailRes = if (analogSecondsSupported) {
                R.string.ledger_ready_future
            } else {
                R.string.ledger_unavailable_pre31
            },
            previewKind = PreviewKind.KWGT_FUTURE,
            providerClass = KwgtFutureWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_FUTURE,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_nothing_analog3",
            name = "KWGT Analog3",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Analog_3 · 277 dial / two hollow capsule hands",
            descriptionRes = R.string.catalog_kwgt_analog3_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_ANALOG_3,
            providerClass = KwgtAnalog3WidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_ANALOG_3,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_analog_seconds_clock",
            name = "KWGT Analog Seconds",
            group = WidgetGroup.KWGT,
            state = if (analogSecondsSupported) {
                ImportState.READY
            } else {
                ImportState.UNAVAILABLE
            },
            upstream = "KWGT Nothing_Analog_ · 277 source-seconds dial · active CLOCK_SECOND",
            descriptionRes = R.string.catalog_kwgt_analog_seconds_desc,
            detailRes = if (analogSecondsSupported) {
                R.string.ledger_ready_analog_seconds
            } else {
                R.string.ledger_unavailable_pre31
            },
            previewKind = PreviewKind.KWGT_ANALOG_SECONDS,
            providerClass = KwgtAnalogSecondsWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_ANALOG_SECONDS,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_nothing_analog5",
            name = "KWGT Analog5",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Analog_5 · 277 square dial / 60+12 tick ring / pill hands",
            descriptionRes = R.string.catalog_kwgt_analog5_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_ANALOG_5,
            providerClass = KwgtAnalog5WidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_ANALOG_5,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_nothing_analog5_transparent",
            name = "KWGT Analog5 Clear",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Analog_5_Transparent · 277 clear-cut tick dial / 1.05 minute ring",
            descriptionRes = R.string.catalog_kwgt_analog5clear_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_ANALOG_5_CLEAR,
            providerClass = KwgtAnalog5TransparentWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_ANALOG_5_CLEAR,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_orbit_clock",
            name = "KWGT Orbit Clock",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Digital_Clock · 277 circles / 205+245 orbits / red hour dot",
            descriptionRes = R.string.catalog_kwgt_orbit_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_ORBIT,
            providerClass = KwgtOrbitClockWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_ORBIT,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_mini_calendar",
            name = "KWGT Mini Calendar",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Mini_Calender · five-day EEE / dd strip",
            descriptionRes = R.string.catalog_kwgt_mini_calendar_desc,
            detailRes = R.string.ledger_ready_kwgt_date,
            previewKind = PreviewKind.KWGT_MINI_CALENDAR,
            providerClass = KwgtMiniCalendarWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_MINI_CALENDAR,
        ),
        CatalogEntry(
            id = "kwgt_clean_info",
            name = "KWGT Clean Info",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Clean_Time_Date_Info · ghost h:mm / weekday-date stack / real battery pill",
            descriptionRes = R.string.catalog_kwgt_clean_info_desc,
            detailRes = R.string.ledger_ready_kwgt_date,
            previewKind = PreviewKind.KWGT_CLEAN_INFO,
            providerClass = KwgtCleanInfoWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_CLEAN_INFO,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_ios_battery",
            name = "KWGT iOS Battery",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT iOS_Battery_Widget · 92 level ring · real charging icon",
            descriptionRes = R.string.catalog_kwgt_ios_battery_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_IOS_BATTERY,
            providerClass = IosBatteryWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_IOS_BATTERY,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_nothing_digital2",
            name = "KWGT Digital2 Minutes",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Digital2 · previous r1m / live hh:mm / next a1m",
            descriptionRes = R.string.catalog_kwgt_digital2_desc,
            detailRes = R.string.ledger_ready_kwgt,
            previewKind = PreviewKind.KWGT_DIGITAL_2,
            providerClass = KwgtDigital2WidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_DIGITAL_2,
            action = CardAction.CLOCK_ACCESS,
        ),
        CatalogEntry(
            id = "kwgt_nothing_date_time3",
            name = "KWGT Date Time 3 Weather",
            group = WidgetGroup.KWGT,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Date_Time3 · live hh:mm / date / current weather + US AQI",
            descriptionRes = R.string.catalog_kwgt_date_time3_desc,
            detailRes = R.string.ledger_ready_weather,
            previewKind = PreviewKind.KWGT_DATE_TIME_3,
            providerClass = KwgtDateTime3WidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_DATE_TIME_3,
            action = CardAction.WEATHER,
        ),
        CatalogEntry(
            id = "kwgt_interstellar_endurance",
            name = "KWGT Endurance",
            group = WidgetGroup.KWGT,
            state = if (analogSecondsSupported) {
                ImportState.READY
            } else {
                ImportState.UNAVAILABLE
            },
            upstream = "KWGT Interstellar_Endurance · source shape spacecraft / second layer",
            descriptionRes = R.string.catalog_interstellar_endurance_desc,
            detailRes = if (analogSecondsSupported) {
                R.string.ledger_ready_interstellar
            } else {
                R.string.ledger_unavailable_pre31
            },
            previewKind = PreviewKind.INTERSTELLAR_ENDURANCE,
            providerClass = KwgtInterstellarEnduranceProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_INTERSTELLAR_ENDURANCE,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_interstellar_endurance_analog",
            name = "KWGT Endurance Analog",
            group = WidgetGroup.KWGT,
            state = if (analogSecondsSupported) {
                ImportState.READY
            } else {
                ImportState.UNAVAILABLE
            },
            upstream = "KWGT Interstellar_Endurance_Analog · source shapes / hour + minute hands",
            descriptionRes = R.string.catalog_interstellar_analog_desc,
            detailRes = if (analogSecondsSupported) {
                R.string.ledger_ready_interstellar
            } else {
                R.string.ledger_unavailable_pre31
            },
            previewKind = PreviewKind.INTERSTELLAR_ENDURANCE_ANALOG,
            providerClass = KwgtInterstellarEnduranceAnalogProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_INTERSTELLAR_ANALOG,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_interstellar_endurance_analog_2",
            name = "KWGT Endurance Analog 2",
            group = WidgetGroup.KWGT,
            state = if (analogSecondsSupported) {
                ImportState.READY
            } else {
                ImportState.UNAVAILABLE
            },
            upstream = "KWGT Interstellar_Endurance_Analog_2 · source shapes / stepped hour hand",
            descriptionRes = R.string.catalog_interstellar_analog2_desc,
            detailRes = if (analogSecondsSupported) {
                R.string.ledger_ready_interstellar
            } else {
                R.string.ledger_unavailable_pre31
            },
            previewKind = PreviewKind.INTERSTELLAR_ENDURANCE_ANALOG_2,
            providerClass = KwgtInterstellarEnduranceAnalog2Provider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_KWGT_INTERSTELLAR_ANALOG_2,
            action = CardAction.NONE,
        ),
        CatalogEntry(
            id = "kwgt_interstellar_analog_bitmap",
            name = "KWGT Interstellar Analog (bitmap)",
            group = WidgetGroup.KWGT,
            state = ImportState.BLOCKED,
            upstream = "KWGT Interstellar_Analog · ShapeModule Lines fx_gradient_bitmap IMG718befb261acf17a52fc205d18d0efe8",
            descriptionRes = R.string.catalog_interstellar_analog_bitmap_desc,
            detailRes = R.string.ledger_blocked_interstellar_bitmap,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
        CatalogEntry(
            id = "kwgt_interstellar_khaki",
            name = "KWGT Khaki Field Murph Auto",
            group = WidgetGroup.KWGT,
            state = ImportState.BLOCKED,
            upstream = "KWGT Interstellar_Analog_Khaki_Field_Murph_Auto · BitmapModule IMG1624e2ce457a50dd7d112dbb0ea90d2b, IMGefbbf6e7d14b6b4d37ec363d3ae494fd, IMG54339d4ea5aa221031a056f1a88f89b4, IMGcb239f2ed780a09d48828a39c8687086 (x2)",
            descriptionRes = R.string.catalog_interstellar_khaki_desc,
            detailRes = R.string.ledger_blocked_interstellar_bitmap,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
        CatalogEntry(
            id = "kwgt_nothing_cpu",
            name = "KWGT Nothing CPU",
            group = WidgetGroup.KWGT,
            state = ImportState.UNAVAILABLE,
            upstream = "KWGT Nothing_CPU · whole-device rm(cused/csys/cusr/cidle), not per-process",
            descriptionRes = R.string.catalog_nothing_cpu_desc,
            detailRes = R.string.ledger_unavailable_cpu,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
        CatalogEntry(
            id = "media_m3_expanded",
            name = "Material3 Media Expanded",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Material3_Expanded_Media_Player · source art card + transport",
            descriptionRes = R.string.catalog_m3_expanded_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_M3_EXPANDED,
            providerClass = Material3ExpandedMediaProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_EXPANDED,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_m3_output",
            name = "Material3 Media Output",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Material3_Media_Player_Output · player/output label + controls",
            descriptionRes = R.string.catalog_m3_output_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_M3_OUTPUT,
            providerClass = Material3MediaOutputProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_OUTPUT,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_mini",
            name = "Mini Media Player",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Mini_Media_Player · source cover + song + play/pause",
            descriptionRes = R.string.catalog_mini_media_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_MINI,
            providerClass = MiniMediaPlayerProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_MINI,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_minimal",
            name = "Minimal Media Player",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Minimal_Media_Player · source horizontal info + buttons",
            descriptionRes = R.string.catalog_minimal_media_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_MINIMAL,
            providerClass = MinimalMediaPlayerProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_MINIMAL,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_minimal2",
            name = "Minimal Media Player 2",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Minimal_Media_Player_2 · source accent bar + round play",
            descriptionRes = R.string.catalog_minimal_media2_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_MINIMAL_2,
            providerClass = MinimalMediaPlayer2Provider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_MINIMAL2,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_minimal3",
            name = "Minimal Media Player 3",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Minimal_Media_Player_3 · source info / progress / buttons",
            descriptionRes = R.string.catalog_minimal_media3_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_MINIMAL_3,
            providerClass = MinimalMediaPlayer3Provider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_MINIMAL3,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_master",
            name = "Nothing Master Widget",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Master_Widget · battery / date / weather / time / media",
            descriptionRes = R.string.catalog_nothing_master_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_MASTER,
            providerClass = NothingMasterWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_MASTER,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_nothing_player",
            name = "Nothing Media Player",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Media_Player · source circular player / progress / controls",
            descriptionRes = R.string.catalog_nothing_media_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_PLAYER,
            providerClass = NothingMediaPlayerProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_PLAYER,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_nothing_player2",
            name = "Nothing Media Player2",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Media_Player2 · source pill / package / round control",
            descriptionRes = R.string.catalog_nothing_media2_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_PLAYER_2,
            providerClass = NothingMediaPlayer2Provider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_PLAYER2,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_nothing_player3",
            name = "Nothing Media Player3",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Nothing_Media_Player3 · source side panel / progress / controls",
            descriptionRes = R.string.catalog_nothing_media3_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_PLAYER_3,
            providerClass = NothingMediaPlayer3Provider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_PLAYER3,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_universal",
            name = "Universal Media Player",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT Universal_Media_Player · source cover / info / progress / controls",
            descriptionRes = R.string.catalog_universal_media_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_UNIVERSAL,
            providerClass = UniversalMediaPlayerProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_UNIVERSAL,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "media_notification",
            name = "Notification Widget",
            group = WidgetGroup.MEDIA_PLAYER,
            state = ImportState.READY,
            upstream = "KWGT material_notification_widget · source notification card",
            descriptionRes = R.string.catalog_notification_desc,
            detailRes = R.string.ledger_ready_media,
            previewKind = PreviewKind.MEDIA_NOTIFICATION,
            providerClass = MaterialNotificationWidgetProvider::class.java,
            pinKind = PinResultReceiver.PIN_KIND_MEDIA_NOTIFICATION,
            action = CardAction.MEDIA_ACCESS,
        ),
        CatalogEntry(
            id = "compass",
            name = "Compass",
            group = WidgetGroup.MEDIA,
            state = ImportState.BLOCKED,
            upstream = "NtCompass · sensor live updates",
            descriptionRes = R.string.catalog_compass_desc,
            detailRes = R.string.ledger_blocked_compass,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
        CatalogEntry(
            id = "snake",
            name = "Snake",
            group = WidgetGroup.MEDIA,
            state = ImportState.BLOCKED,
            upstream = "NtSnake/NtSnakeGame · live game service",
            descriptionRes = R.string.catalog_snake_desc,
            detailRes = R.string.ledger_blocked_snake,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
        CatalogEntry(
            id = "oem_clock",
            name = "OnePlus Clock",
            group = WidgetGroup.OEM,
            state = ImportState.UNAVAILABLE,
            upstream = "net.oneplus.widget.op.ClockProvider",
            descriptionRes = R.string.catalog_oem_clock_desc,
            detailRes = R.string.ledger_unavailable_oem,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
        CatalogEntry(
            id = "oem_weather",
            name = "OnePlus Weather",
            group = WidgetGroup.OEM,
            state = ImportState.UNAVAILABLE,
            upstream = "net.oneplus.widget.op.WeatherProvider",
            descriptionRes = R.string.catalog_oem_weather_desc,
            detailRes = R.string.ledger_unavailable_oem,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
        CatalogEntry(
            id = "oem_weather_clock",
            name = "OnePlus Weather Clock",
            group = WidgetGroup.OEM,
            state = ImportState.UNAVAILABLE,
            upstream = "net.oneplus.widget.op.WeatherClockProvider",
            descriptionRes = R.string.catalog_oem_weather_clock_desc,
            detailRes = R.string.ledger_unavailable_oem,
            previewKind = PreviewKind.PLACEHOLDER,
        ),
    )

    private fun weather(id: String, name: String, upstream: String): CatalogEntry =
        CatalogEntry(
            id = id,
            name = name,
            group = WidgetGroup.WEATHER,
            state = ImportState.PENDING,
            upstream = upstream,
            descriptionRes = R.string.catalog_weather_desc,
            detailRes = R.string.ledger_pending_weather,
            previewKind = PreviewKind.PLACEHOLDER,
        )

    fun entryById(id: String): CatalogEntry? = entries.firstOrNull { it.id == id }

    fun readyEntries(category: WidgetCategory): List<CatalogEntry> =
        category.entryIds
            .mapNotNull(::entryById)
            .filter { it.state == ImportState.READY && it.providerClass != null }

    fun providerInfo(context: Context, entry: CatalogEntry): AppWidgetProviderInfo? {
        val provider = entry.providerClass ?: return null
        val component = ComponentName(context, provider)
        return AppWidgetManager.getInstance(context)
            .installedProviders
            .firstOrNull { it.provider == component }
    }
}

/**
 * Functional HOME categories, deliberately independent of the upstream group
 * names. Only the first four currently contain ready providers. The remaining
 * categories stay visible with a truthful zero-ready count so nothing is
 * silently hidden, but they never invent devices, readings or providers.
 */
enum class WidgetCategory(
    val id: String,
    val labelRes: Int,
    val entryIds: List<String>,
) {
    CLOCK(
        "clock",
        R.string.category_clock,
        listOf(
            "digital",
            "analog_bold",
            "analog_scale",
            "world",
            "kwgt_nothing_digital4",
            "kwgt_nothing_digital3",
            "kwgt_gradient_clock",
            "kwgt_analog2_clock",
            "kwgt_future_clock",
            "kwgt_nothing_analog3",
            "kwgt_analog_seconds_clock",
            "kwgt_nothing_analog5",
            "kwgt_nothing_analog5_transparent",
            "kwgt_orbit_clock",
            "kwgt_nothing_digital2",
            "kwgt_interstellar_endurance",
            "kwgt_interstellar_endurance_analog",
            "kwgt_interstellar_endurance_analog_2",
        ),
    ),
    CALENDAR(
        "calendar",
        R.string.category_calendar,
        listOf(
            "date",
            "kwgt_nothing_date_time",
            "kwgt_nothing_date_time2",
            "kwgt_mini_calendar",
            "kwgt_clean_info",
            "kwgt_nothing_date_time3",
        ),
    ),
    BATTERY(
        "battery",
        R.string.category_battery,
        listOf(
            "kwgt_nothing_battery",
            "kwgt_nothing_battery_mini",
            "kwgt_nothing_battery2",
            "kwgt_ios_battery",
        ),
    ),
    QUOTES(
        "quotes",
        R.string.category_quotes,
        listOf("quotes"),
    ),
    WEATHER(
        "weather",
        R.string.category_weather,
        listOf(
            "wx_nothing_temp",
            "wx_nothing_sun",
            "wx_circle",
            "wx_square",
            "wx_rect",
            "wx_combo",
            "wx_sunset",
            "wx_air",
            "wx_w1temp",
            "wx_w1status",
            "wx_w1rain",
        ),
    ),
    PHOTOS(
        "photos",
        R.string.category_photos,
        listOf("photo_circle", "photo_rect"),
    ),
    BLUETOOTH(
        "bluetooth",
        R.string.category_bluetooth,
        emptyList(),
    ),
    WIFI(
        "wifi",
        R.string.category_wifi,
        emptyList(),
    ),
    LIVE(
        "live",
        R.string.category_live,
        listOf("compass", "snake"),
    ),
    MEDIA_PLAYER(
        "media_player",
        R.string.category_media_player,
        listOf(
            "media_m3_expanded",
            "media_m3_output",
            "media_mini",
            "media_minimal",
            "media_minimal2",
            "media_minimal3",
            "media_master",
            "media_nothing_player",
            "media_nothing_player2",
            "media_nothing_player3",
            "media_universal",
            "media_notification",
        ),
    ),
    OEM(
        "oem",
        R.string.category_oem,
        listOf("oem_clock", "oem_weather", "oem_weather_clock"),
    );

    val representativeId: String?
        get() = entryIds.firstOrNull()
}

object WidgetModeLabels {
    fun labelRes(context: Context, entry: CatalogEntry): Int {
        val access = MinuteTickScheduler.hasExactAccess(context)
        return when (entry.previewKind) {
            PreviewKind.CLOCK -> if (access) R.string.mode_dotted_clock else R.string.mode_live_clock
            PreviewKind.ANALOG_BOLD,
            PreviewKind.ANALOG_SCALE,
            -> if (access) R.string.mode_styled_analog else R.string.mode_live_analog
            PreviewKind.WORLD -> if (access) R.string.mode_styled_world else R.string.mode_live_world
            PreviewKind.DATE -> R.string.mode_date
            PreviewKind.QUOTES -> R.string.mode_quote
            PreviewKind.KWGT_BATTERY -> R.string.mode_battery
            PreviewKind.KWGT_BATTERY_MINI -> R.string.mode_battery_mini
            PreviewKind.KWGT_BATTERY_2 -> R.string.mode_battery2
            PreviewKind.KWGT_DATE_TIME,
            PreviewKind.KWGT_DATE_TIME_2,
            -> if (access) {
                R.string.mode_kwgt_datetime_styled
            } else {
                R.string.mode_kwgt_datetime_live
            }
            PreviewKind.KWGT_DIGITAL_4 -> if (access) {
                R.string.mode_kwgt_split_styled
            } else {
                R.string.mode_kwgt_split_live
            }
            PreviewKind.KWGT_DIGITAL_3 -> if (access) {
                R.string.mode_kwgt_digital3_styled
            } else {
                R.string.mode_kwgt_digital3_live
            }
            PreviewKind.KWGT_GRADIENT_CLOCK -> if (access) {
                R.string.mode_kwgt_gradient_styled
            } else {
                R.string.mode_kwgt_gradient_live
            }
            PreviewKind.KWGT_ANALOG_2 -> R.string.mode_kwgt_analog2
            PreviewKind.KWGT_FUTURE -> R.string.mode_kwgt_future
            PreviewKind.KWGT_ANALOG_3 -> R.string.mode_kwgt_analog3
            PreviewKind.KWGT_ANALOG_SECONDS -> R.string.mode_kwgt_analog_seconds
            PreviewKind.KWGT_ANALOG_5 -> R.string.mode_kwgt_analog5
            PreviewKind.KWGT_ANALOG_5_CLEAR -> R.string.mode_kwgt_analog5clear
            PreviewKind.KWGT_ORBIT -> R.string.mode_kwgt_orbit
            PreviewKind.KWGT_MINI_CALENDAR -> R.string.mode_kwgt_mini_calendar
            PreviewKind.KWGT_CLEAN_INFO -> R.string.mode_kwgt_clean_info
            PreviewKind.KWGT_IOS_BATTERY -> R.string.mode_ios_battery
            PreviewKind.KWGT_DIGITAL_2 -> R.string.mode_kwgt_digital2
            PreviewKind.KWGT_DATE_TIME_3 -> R.string.mode_kwgt_date_time3
            PreviewKind.INTERSTELLAR_ENDURANCE,
            PreviewKind.INTERSTELLAR_ENDURANCE_ANALOG,
            PreviewKind.INTERSTELLAR_ENDURANCE_ANALOG_2,
            -> R.string.mode_interstellar
            PreviewKind.MEDIA_M3_EXPANDED,
            PreviewKind.MEDIA_M3_OUTPUT,
            PreviewKind.MEDIA_MINI,
            PreviewKind.MEDIA_MINIMAL,
            PreviewKind.MEDIA_MINIMAL_2,
            PreviewKind.MEDIA_MINIMAL_3,
            PreviewKind.MEDIA_PLAYER,
            PreviewKind.MEDIA_PLAYER_2,
            PreviewKind.MEDIA_PLAYER_3,
            PreviewKind.MEDIA_UNIVERSAL,
            -> R.string.mode_media_live
            PreviewKind.MEDIA_MASTER -> R.string.mode_media_master
            PreviewKind.MEDIA_NOTIFICATION -> R.string.mode_media_notification
            PreviewKind.WX_TEMP -> R.string.mode_weather_temp
            PreviewKind.WX_SUN -> R.string.mode_weather_sun
            PreviewKind.PLACEHOLDER -> entry.state.labelRes
        }
    }

    fun label(context: Context, entry: CatalogEntry): String =
        context.getString(labelRes(context, entry))
}
