package com.dotwelwidgets.dotwellabs

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context

enum class WidgetGroup(val labelRes: Int) {
    CLOCKS(R.string.category_clocks),
    TIME(R.string.category_time),
    WEATHER(R.string.category_weather),
    MEDIA(R.string.category_media),
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
    KWGT_ANALOG_3,
    KWGT_MINI_CALENDAR,
    KWGT_CLEAN_INFO,
    PLACEHOLDER,
}

enum class CardAction {
    NONE,
    CLOCK_ACCESS,
    WORLD_ZONES,
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
            "kwgt_nothing_analog3",
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
        ),
    ),
    BATTERY(
        "battery",
        R.string.category_battery,
        listOf(
            "kwgt_nothing_battery",
            "kwgt_nothing_battery_mini",
            "kwgt_nothing_battery2",
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
            PreviewKind.KWGT_ANALOG_3 -> R.string.mode_kwgt_analog3
            PreviewKind.KWGT_MINI_CALENDAR -> R.string.mode_kwgt_mini_calendar
            PreviewKind.KWGT_CLEAN_INFO -> R.string.mode_kwgt_clean_info
            PreviewKind.PLACEHOLDER -> entry.state.labelRes
        }
    }

    fun label(context: Context, entry: CatalogEntry): String =
        context.getString(labelRes(context, entry))
}
