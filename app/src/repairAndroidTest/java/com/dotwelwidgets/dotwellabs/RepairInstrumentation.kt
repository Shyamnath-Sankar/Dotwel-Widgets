package com.dotwelwidgets.dotwellabs

import android.app.Instrumentation
import android.content.*
import android.os.*
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** Sequential real-Android host sweep. Pixel evidence still requires inspection. */
class RepairInstrumentation:Instrumentation() {
    private var args=Bundle()
    override fun onCreate(arguments:Bundle?) { super.onCreate(arguments);args=arguments ?: Bundle();start() }
    override fun onStart() {
        val out=Bundle()
        val dir=File(targetContext.noBackupFilesDir,"repair-evidence").apply { mkdirs() }
        val results=JSONArray()
        val failures=JSONArray()
        try {
            val catalog=JSONArray()
            for(category in WidgetCategory.values()) for(entry in WidgetCatalog.readyEntries(category)) {
                catalog.put(JSONObject().put("id",entry.id).put("name",entry.name).put("provider",entry.providerClass!!.name).put("category",targetContext.getString(category.labelRes)).put("action",entry.action.name))
            }
            File(dir,"catalog.json").writeText(catalog.toString(2))
            val selected=args.getString("provider")
            val entries=WidgetCatalog.entries.filter { it.providerClass!=null && (selected==null || it.providerClass!!.name==selected) }
            check(entries.isNotEmpty())
            for(entry in entries) {
                val result=JSONObject().put("id",entry.id).put("provider",entry.providerClass!!.name)
                try {
                    for(style in WidgetStyle.values()) {
                        for((w,h) in listOf(180 to 180,360 to 180)) {
                            val name="${entry.id}-${style.name}-$w"
                            val intent=Intent(targetContext,RepairHostActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra("provider",entry.providerClass!!.name)
                                putExtra("evidenceKey",name)
                                putExtra("style",style.id);putExtra("width",w);putExtra("height",h)
                            }
                            targetContext.startActivity(intent)
                            var attempts=0
                            while(RepairHostActivity.shownKey!=name && attempts++<50)Thread.sleep(100)
                            check(RepairHostActivity.shownKey==name) { "host activity did not load" }
                            Thread.sleep(250)
                            check(RepairHostActivity.current?.widgetView!=null) { "native RemoteViews inflation failed" }
                            if(entry.id.startsWith("media_") && w==360 && style==WidgetStyle.BLACK) {
                                val events=File(targetContext.noBackupFilesDir,"repair-media-events.txt")
                                for((viewId,expected) in listOf(R.id.media_play_pause to "state:",R.id.media_next to "next",R.id.media_previous to "previous",R.id.media_volume_up to "volume:1",R.id.media_volume_down to "volume:-1")) {
                                    val before=if(events.exists()) events.readText().length else 0
                                    var clicked=false
                                    runOnMainSync {
                                        val button=RepairHostActivity.current?.widgetView?.findViewById<android.view.View>(viewId)
                                        if(button?.visibility==android.view.View.VISIBLE && button.width>1) clicked=button.performClick()
                                    }
                                    if(clicked) {
                                        Thread.sleep(350)
                                        check(events.exists() && events.readText().drop(before).contains(expected)) { "transport callback missing: $expected" }
                                    }
                                }
                            }
                            val bitmap=uiAutomation.takeScreenshot() ?: error("screenshot missing")
                            File(dir,"$name.png").outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)) }
                            bitmap.recycle()
                            result.put(name,"captured; pixel review pending")
                        }
                    }
                } catch(error:Throwable) {
                    result.put("error",error.toString());failures.put(JSONObject().put("id",entry.id).put("error",error.toString()))
                }
                results.put(result)
                File(dir,"RESULT.json").writeText(JSONObject().put("results",results).put("failures",failures).put("limits","Isolated Android AppWidgetHost, not Pixel launcher placement. Synthetic media only if enabled separately. Screenshots require inspection.").toString(2))
            }
            out.putInt("providerCount",results.length());out.putInt("failureCount",failures.length())
            finish(if(failures.length()==0)-1 else 1,out)
        } catch(error:Throwable) { out.putString("error",error.toString());finish(1,out) }
    }
}
