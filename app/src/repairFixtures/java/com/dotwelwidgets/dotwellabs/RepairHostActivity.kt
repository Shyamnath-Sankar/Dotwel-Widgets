package com.dotwelwidgets.dotwellabs

import android.app.Activity
import android.appwidget.*
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*

/** Emulator-only debug fixture. Never included in release builds. */
class RepairHostActivity:Activity() {
    private lateinit var host:AppWidgetHost
    var widgetId=-1
        private set
    var widgetView:AppWidgetHostView?=null
        private set
    private var provider:ComponentName?=null
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        host=AppWidgetHost(this,9377)
        host.startListening()
        show(intent)
    }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent);setIntent(intent);show(intent) }
    private fun show(input:Intent) {
        val root=FrameLayout(this).apply { setBackgroundColor(Color.rgb(91,116,120)) }
        try {
            val name=input.getStringExtra("provider") ?: error("provider missing")
            val component=ComponentName(this,name)
            val width=input.getIntExtra("width",320)
            val height=input.getIntExtra("height",260)
            val style=WidgetStyle.fromId(input.getIntExtra("style",0))
            WidgetPrefs.setStyle(this,style)
            val manager=AppWidgetManager.getInstance(this)
            val options=Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,width)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,width)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,height)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,height)
                if(android.os.Build.VERSION.SDK_INT>=31) putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES,arrayListOf(android.util.SizeF(width.toFloat(),height.toFloat())))
            }
            if(component!=provider) {
                if(widgetId>=0)host.deleteAppWidgetId(widgetId)
                widgetId=host.allocateAppWidgetId()
                check(manager.bindAppWidgetIdIfAllowed(widgetId,component,options)) { "bind refused" }
                provider=component
            } else manager.updateAppWidgetOptions(widgetId,options)
            val instance=Class.forName(name).getDeclaredConstructor().newInstance() as AppWidgetProvider
            instance.onUpdate(this,manager,intArrayOf(widgetId))
            val view=host.createView(this,widgetId,manager.getAppWidgetInfo(widgetId))
            view.updateAppWidgetSize(options,width,height,width,height)
            val density=resources.displayMetrics.density
            root.addView(view,FrameLayout.LayoutParams((width*density).toInt(),(height*density).toInt(),Gravity.CENTER))
            widgetView=view
            root.addView(TextView(this).apply { text="$name / $width x $height / ${style.name} / id=$widgetId";setTextColor(Color.WHITE) })
        } catch(error:Throwable) {
            root.addView(TextView(this).apply { text="HOST ERROR: $error";setTextColor(Color.RED) })
            widgetView=null
        }
        setContentView(root)
        current=this
        shownKey=input.getStringExtra("evidenceKey")
    }
    override fun onDestroy() { if(current===this)current=null;host.stopListening();super.onDestroy() }
    companion object {
        @Volatile var current:RepairHostActivity?=null
        @Volatile var shownKey:String?=null
    }
}
