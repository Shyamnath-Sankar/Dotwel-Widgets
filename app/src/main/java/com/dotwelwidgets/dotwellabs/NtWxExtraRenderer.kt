package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.dotwelwidgets.dotwellabs.weather.*
import kotlin.math.sin

/** NtWxDraw.render kinds2-8, MIT Sameer Al Sahab. Platform font substitution. */
internal object NtWxExtraRenderer {
    private fun paint(color:Int)=Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color=color }
    fun draw(context:Context,canvas:Canvas,kind:NtWxRenderer.Kind,w:Float,h:Float,style:WidgetStyle,
        state:WeatherRenderState,icons:NtWxRenderer.IconDrawer) {
        val snapshot=state.snapshot ?: return
        val today=WeatherDisplay.today(snapshot,state.now)
        val side=minOf(w,h)
        val pad=side*.1f
        val ink=UpstreamNtDraw.ink(context,style)
        val muted=UpstreamNtDraw.muted(context,style)
        val bg=UpstreamNtDraw.background(context,style)
        val accent=UpstreamNtDraw.accent(context,style)
        fun text(value:String,x:Float,y:Float,size:Float,color:Int=ink,align:Paint.Align=Paint.Align.LEFT,max:Float=w)=
            NtWxRenderer.text(canvas,value,x,y,size,color,align,max)
        fun temp(value:Double?,x:Float,y:Float,size:Float,align:Paint.Align=Paint.Align.LEFT)=
            NtWxRenderer.temperature(canvas,NtWxRenderer.number(value),x,y,size,align,ink)
        when(kind) {
            NtWxRenderer.Kind.RECT -> {
                val split=w*.38f
                icons.draw(canvas,snapshot.current.weatherCode,snapshot.current.isDay,pad+h*.11f,pad+h*.11f,h*.22f,ink)
                temp(snapshot.current.temperature,pad,h*.66f,h*.36f)
                text(snapshot.location.name.substringBefore(','),pad,h-pad,h*.075f,muted,max=split-pad)
                val line=paint((ink and 0xFFFFFF) or (40 shl 24)).apply { strokeWidth=maxOf(1f,h*.008f) }
                canvas.drawLine(split,pad,split,h-pad,line)
                val columns=(w-split-pad)/4
                snapshot.forecast.filter { it.localDate>(today?.localDate ?: "") }.take(4).forEachIndexed { i,day ->
                    val x=split+(i+.5f)*columns
                    val label=runCatching { java.time.LocalDate.parse(day.localDate).dayOfWeek.name.take(3) }.getOrDefault("--")
                    text(label,x,h*.26f,h*.085f,muted,Paint.Align.CENTER,columns*.9f)
                    icons.draw(canvas,day.weatherCode,true,x,h*.5f,h*.22f,ink)
                    temp(day.temperatureMax,x,h*.8f,h*.14f,Paint.Align.CENTER)
                }
            }
            NtWxRenderer.Kind.COMBO -> {
                val gap=side*.03f
                val radius=side*.13f
                val tile=if(Color.alpha(bg)>0) bg else (ink and 0xFFFFFF) or (40 shl 24)
                listOf(RectF(0f,0f,w,h/2-gap),RectF(0f,h/2+gap,w/2-gap,h),
                    RectF(w/2+gap,h/2+gap,w,h)).forEach { canvas.drawRoundRect(it,radius,radius,paint(tile)) }
                val iconY=h*.25f-gap/2
                icons.draw(canvas,snapshot.current.weatherCode,snapshot.current.isDay,side*.17f,iconY,side*.22f,ink)
                text(WeatherDisplay.conditionLabel(snapshot.current.weatherCode),side*.33f,iconY+side*.035f,side*.1f,max=w-side*.4f)
                val value=NtWxRenderer.number(snapshot.current.temperature)
                val size=NtWxRenderer.fitTemperature(value,w/2-side*.2f,side*.26f)
                NtWxRenderer.temperature(canvas,value,w/4-gap/2,h*.75f+gap/2+size*.36f,size,Paint.Align.CENTER,ink)
                NtWxRenderer.arrowTemperature(canvas,true,today?.temperatureMax,w*.75f+gap/2,h*.75f-side*.07f,side*.12f,ink)
                NtWxRenderer.arrowTemperature(canvas,false,today?.temperatureMin,w*.75f+gap/2,h*.75f+side*.11f,side*.12f,ink)
            }
            NtWxRenderer.Kind.SUNSET -> sunset(canvas,w,h,snapshot,state.now,today,ink,accent)
            NtWxRenderer.Kind.AIR -> {
                val aqi=snapshot.airQuality?.usAqi
                val band=airBand(aqi)
                val x=h*.3f
                text("US AQI",x,h*.36f,h*.15f,muted,max=w*.4f)
                text(aqi?.toString() ?: "--",x,h*.8f,h*.4f,max=w*.4f)
                val labels=arrayOf("--","Good","Moderate","Sensitive","Unhealthy","Very unhealthy","Hazardous")
                text(labels[band],w-x,h*.36f,h*.15f,align=Paint.Align.RIGHT,max=w*.42f)
                for(i in 1..6) {
                    val selected=i==band
                    val color=if(selected) accent else (ink and 0xFFFFFF) or (90 shl 24)
                    canvas.drawCircle(w-x-(6-i)*h*.13f-h*.045f,h*.7f,h*.045f*(if(selected)1.5f else 1f),paint(color))
                }
            }
            NtWxRenderer.Kind.W1_TEMP -> {
                val value=NtWxRenderer.number(snapshot.current.temperature)
                val size=NtWxRenderer.fitTemperature(value,side*.62f,side*.4f)
                NtWxRenderer.temperature(canvas,value,w/2,h/2+size*.35f,size,Paint.Align.CENTER,ink)
            }
            NtWxRenderer.Kind.W1_STATUS -> icons.draw(canvas,snapshot.current.weatherCode,snapshot.current.isDay,w/2,h/2,side*.5f,ink)
            NtWxRenderer.Kind.W1_RAIN -> {
                icons.draw(canvas,61,true,w/2,h*.4f,side*.46f,ink)
                text(today?.precipitationProbabilityMaxPercent?.let { "$it%" } ?: "--%",w/2,h*.8f,side*.17f,align=Paint.Align.CENTER,max=side*.8f)
            }
            else -> error("Wrong NtWx branch")
        }
    }

    fun airBand(value:Int?):Int=when {
        value==null || value<0 -> 0
        value<=50 -> 1
        value<=100 -> 2
        value<=150 -> 3
        value<=200 -> 4
        value<=300 -> 5
        else -> 6
    }

    private fun sunset(canvas:Canvas,w:Float,h:Float,snapshot:WeatherSnapshot,now:Long,today:WeatherDay?,ink:Int,accent:Int) {
        val rise=today?.sunriseEpochMillis
        val set=today?.sunsetEpochMillis
        val zone=WeatherDisplay.zone(snapshot.location)
        val formatter=java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        fun time(value:Long?)=value?.let { formatter.format(java.time.Instant.ofEpochMilli(it).atZone(zone)) } ?: "--:--"
        val left=h*.3f
        val right=w-left
        NtWxRenderer.text(canvas,time(rise),left,h*.36f,h*.17f,ink,Paint.Align.LEFT,w*.35f)
        NtWxRenderer.text(canvas,time(set),right,h*.36f,h*.17f,ink,Paint.Align.RIGHT,w*.35f)
        val base=h*.66f
        val amplitude=h*.24f
        val dots=paint((ink and 0xFFFFFF) or (110 shl 24))
        var x=left
        while(x<=right) { canvas.drawCircle(x,base,h*.012f,dots); x+=h*.075f }
        val path=android.graphics.Path()
        for(i in 0..60) {
            val f=-.28f+i*1.56f/60
            val px=left+(right-left)*(f+.28f)/1.56f
            val py=base-sin(f*Math.PI).toFloat()*amplitude
            if(i==0) path.moveTo(px,py) else path.lineTo(px,py)
        }
        val p=paint((ink and 0xFFFFFF) or (150 shl 24)).apply { style=Paint.Style.STROKE; strokeWidth=maxOf(1.5f,h*.015f) }
        canvas.drawPath(path,p)
        if(rise!=null && set!=null && set>rise) {
            val f=((now-rise).toFloat()/(set-rise)).coerceIn(-.28f,1.28f)
            canvas.drawCircle(left+(right-left)*(f+.28f)/1.56f,base-sin(f*Math.PI).toFloat()*amplitude,h*.055f,paint(accent))
        }
    }
}
