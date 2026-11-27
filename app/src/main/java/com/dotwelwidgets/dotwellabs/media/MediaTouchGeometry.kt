package com.dotwelwidgets.dotwellabs.media

/** Shared logical design-space centres used by ReferenceMediaCard, mapped to each host. */
object MediaTouchGeometry {
    data class Box(val left:Float,val top:Float,val right:Float,val bottom:Float)
    data class Controls(val previous:Box?,val play:Box?,val next:Box?,val clear:Box?=null,val volumeUp:Box?=null,val volumeDown:Box?=null)

    fun design(kind:MediaWidgetKind):Controls {
        if (kind == MediaWidgetKind.NOTIFICATION) {
            val rect = ReferenceMediaCard.clearRect(kind)
            return Controls(
                null,
                null,
                null,
                Box(rect.left, rect.top, rect.right, rect.bottom),
            )
        }
        val controls = ReferenceMediaCard.controlLayout(kind)
        fun box(cx:Float,cy:Float,half:Float)=Box(cx-half,cy-half,cx+half,cy+half)
        return Controls(
            previous = box(controls.previousCenterX, controls.centerY, controls.skipSize * 1.8f),
            play = box(controls.playCenterX, controls.centerY, controls.playRadius * 1.7f),
            next = box(controls.nextCenterX, controls.centerY, controls.skipSize * 1.8f),
        )
    }

    fun map(box:Box,kind:MediaWidgetKind,width:Float,height:Float):Box {
        val scale=minOf(width/kind.designWidth,height/kind.designHeight)
        val x=(width-kind.designWidth*scale)/2
        val y=(height-kind.designHeight*scale)/2
        return Box(x+box.left*scale,y+box.top*scale,x+box.right*scale,y+box.bottom*scale)
    }
}
