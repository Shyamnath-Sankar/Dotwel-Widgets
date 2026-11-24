package com.dotwelwidgets.dotwellabs

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/** Original geometric weather symbols, not copied upstream icon files. */
object DotwelWeatherIcons : NtWxRenderer.IconDrawer {
    override fun draw(canvas:Canvas,code:Int,isDay:Boolean,cx:Float,cy:Float,side:Float,color:Int) {
        val saved=canvas.save()
        canvas.translate(cx-side/2,cy-side/2)
        canvas.scale(side/100,side/100)
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color=color; style=Paint.Style.STROKE; strokeWidth=4f; strokeCap=Paint.Cap.ROUND
        }
        if(code<=1) {
            if(isDay) {
                canvas.drawCircle(50f,50f,19f,p)
                for(i in 0..7) {
                    val s=canvas.save();canvas.rotate(i*45f,50f,50f)
                    canvas.drawLine(50f,16f,50f,24f,p);canvas.restoreToCount(s)
                }
            } else {
                val moon=Path().apply {
                    moveTo(63f,18f);cubicTo(9f,17f,12f,86f,68f,80f)
                    cubicTo(33f,71f,29f,39f,63f,18f)
                }
                canvas.drawPath(moon,p)
            }
        } else {
            val cloud=Path().apply {
                moveTo(24f,67f);cubicTo(2f,65f,8f,37f,29f,40f)
                cubicTo(35f,14f,72f,18f,74f,43f)
                cubicTo(99f,39f,103f,68f,78f,68f);close()
            }
            canvas.drawPath(cloud,p)
            when(code) {
                in 51..67,in 80..82 -> for(i in 0..2) canvas.drawLine(31f+i*19,77f,26f+i*19,88f,p)
                in 71..77,in 85..86 -> for(i in 0..2) {
                    canvas.drawLine(28f+i*20,78f,38f+i*20,88f,p)
                    canvas.drawLine(38f+i*20,78f,28f+i*20,88f,p)
                }
                in 95..99 -> canvas.drawPath(Path().apply {
                    moveTo(52f,70f);lineTo(43f,83f);lineTo(55f,83f);lineTo(47f,96f)
                },p)
                45,48 -> {canvas.drawLine(18f,78f,84f,78f,p);canvas.drawLine(26f,88f,74f,88f,p)}
            }
        }
        canvas.restoreToCount(saved)
    }
}
