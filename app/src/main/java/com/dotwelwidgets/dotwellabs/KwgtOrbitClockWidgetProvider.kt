package com.dotwelwidgets.dotwellabs

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews

/**
 * Separate native provider for KWGT Nothing_Digital_Clock ("KWGT Orbit Clock").
 *
 * The actual source is a 277 circle with a 164 inner disc, a central `hh:mm`
 * TextModule, a 205 hour orbit and a 245 minute orbit drawn as thin 0.5
 * strokes, plus a red 12 hour dot and a 12 minute dot. It is reproduced with a
 * platform [android.widget.AnalogClock] whose static vector dial carries the
 * outer circle, inner circle and both orbits, and whose transparent 277 hand
 * vectors carry the dots at 12 o'clock. The platform clock rotates the dots
 * itself; a live [android.widget.TextClock] supplies the central time.
 *
 * The source stores `config_rotate_mode` values `CLOCK_HOUR_SMOOTH` and
 * `CLOCK_MINUTE`. Its formula values `CLOCK_SECOND` carry toggle 0 and are
 * inactive, so no seconds hand is added and there is no API 31 restriction.
 *
 * The source rotate offset 90 with the horizontal-stack right paddings 204/245
 * puts the dots at radius 102/122.5 on the 9 o'clock side before rotation. The
 * native 12-o'clock dot vectors plus the platform clockwise rotation absorb
 * that mapping: at the upstream preview 02:11 the red hour dot is around 2 and
 * the minute dot just after 2.
 *
 * One layout exists per Dotwel style: black uses the source `#1B1B1D` outer
 * circle with a white inner disc and dark central ink; translucent uses a
 * `#A6FFFFFF` outer circle with a dark inner disc and white ink; transparent
 * keeps the white inner disc and orbits without a filled outer card. The hour
 * dot stays the source red `#D71921` in every style.
 */
class KwgtOrbitClockWidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_kwgt_orbit_black
    override val tapAction: String = AlarmClock.ACTION_SHOW_ALARMS

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, layoutFor(WidgetPrefs.style(context)))
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(AlarmClock.ACTION_SHOW_ALARMS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        return views
    }

    companion object {
        fun layoutFor(style: WidgetStyle): Int = when (style) {
            WidgetStyle.BLACK -> R.layout.widget_kwgt_orbit_black
            WidgetStyle.TRANSLUCENT -> R.layout.widget_kwgt_orbit_translucent
            WidgetStyle.TRANSPARENT -> R.layout.widget_kwgt_orbit_transparent
        }
    }
}
