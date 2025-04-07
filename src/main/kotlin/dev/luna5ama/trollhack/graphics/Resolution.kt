package dev.luna5ama.trollhack.graphics

import dev.fastmc.common.ceilToInt
import dev.luna5ama.trollhack.event.AlwaysListening
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.util.interfaces.Helper

object Resolution : AlwaysListening, Helper {
    val widthI
        get() = mc.displayWidth

    val heightI
        get() = mc.displayHeight

    val heightF
        get() = mc.displayHeight.toFloat()

    val widthF
        get() = mc.displayWidth.toFloat()

    val trollWidthF
        get() = widthF / ClickGUI.scaleFactor

    val trollHeightF
        get() = heightF / ClickGUI.scaleFactor

    val trollWidthI
        get() = trollWidthF.ceilToInt()

    val trollHeightI
        get() = trollHeightF.ceilToInt()
}