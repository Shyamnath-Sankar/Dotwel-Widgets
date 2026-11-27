package com.dotwelwidgets.dotwellabs

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.dotwelwidgets.dotwellabs.weather.WeatherRenderState

/** Source NtWx page state and geometry, using Dotwel's opt-in private weather cache. */
abstract class BaseNtWxWidgetProvider : BaseWeatherWidgetProvider() {
    protected abstract val kind: NtWxRenderer.Kind
    override val layoutRes=R.layout.widget_weather_temp
    override val bitmapViewId=R.id.weather_temp_bitmap
    override val fallbackViewId=R.id.weather_temp_fallback
    private var renderingPage=0

    override fun render(context:Context,widthPx:Int,heightPx:Int,style:WidgetStyle,state:WeatherRenderState):Bitmap =
        NtWxRenderer.render(context,kind,widthPx,heightPx,style,state,renderingPage,DotwelWeatherIcons)

    override fun buildViews(context:Context,appWidgetId:Int):RemoteViews {
        renderingPage=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("pg_$appWidgetId",0)
        return super.buildViews(context,appWidgetId)
    }

    // Configure each size variant before buildBitmapViews combines them. On API
    // 31+ super.buildViews can return a combined RemoteViews that throws if it is
    // modified after construction.
    override fun decorateWeatherViews(views:RemoteViews,context:Context,appWidgetId:Int,usedBitmap:Boolean) {
        if(kind.pages>1 && WeatherRenderState.from(context).snapshot!=null) {
            val intent=Intent(context,this::class.java).setAction(NEXT).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,appWidgetId)
            views.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getBroadcast(context,appWidgetId,intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
    }

    override fun onReceive(context:Context,intent:Intent) {
        if(intent.action==NEXT) {
            val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID)
            val manager=AppWidgetManager.getInstance(context)
            // An exported receiver accepts page changes only for its own current widgets.
            if(manager.getAppWidgetInfo(id)?.provider?.className==this::class.java.name) {
                val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
                val key="pg_$id"
                prefs.edit().putInt(key,NtWxRenderer.page(kind,prefs.getInt(key,0)+1)).apply()
                manager.updateAppWidget(id,buildViews(context,id))
            }
            return
        }
        super.onReceive(context,intent)
    }

    override fun onDeleted(context:Context,appWidgetIds:IntArray) {
        val edit=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach { edit.remove("pg_$it") };edit.apply()
        super.onDeleted(context,appWidgetIds)
    }

    companion object {
        private const val PREFS="nt_weather_pages"
        private const val NEXT="com.dotwelwidgets.dotwellabs.WX_NEXT"
    }
}
class NtWxCircleProvider:BaseNtWxWidgetProvider() { override val kind=NtWxRenderer.Kind.CIRCLE }
class NtWxSquareProvider:BaseNtWxWidgetProvider() { override val kind=NtWxRenderer.Kind.SQUARE }
class NtWxRectProvider:BaseNtWxWidgetProvider() { override val kind=NtWxRenderer.Kind.RECT }
class NtWxComboProvider:BaseNtWxWidgetProvider() { override val kind=NtWxRenderer.Kind.COMBO }
class NtWxSunsetProvider:BaseNtWxWidgetProvider() { override val kind=NtWxRenderer.Kind.SUNSET }
class NtWxAirProvider:BaseNtWxWidgetProvider() { override val kind=NtWxRenderer.Kind.AIR }
class NtWxW1TempProvider:BaseNtWxWidgetProvider() { override val kind=NtWxRenderer.Kind.W1_TEMP }
class NtWxW1StatusProvider:BaseNtWxWidgetProvider() { override val kind=NtWxRenderer.Kind.W1_STATUS }
class NtWxW1RainProvider:BaseNtWxWidgetProvider() { override val kind=NtWxRenderer.Kind.W1_RAIN }
