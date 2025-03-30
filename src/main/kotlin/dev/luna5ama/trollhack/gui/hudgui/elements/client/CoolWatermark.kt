package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.fastmc.common.collection.CircularArray.Companion.average
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.gui.hudgui.elements.misc.TPS.tpsBuffer
import dev.luna5ama.trollhack.module.modules.client.GuiSetting
import dev.luna5ama.trollhack.util.InfoCalculator

internal object CoolWatermark : LabelHud(
    name = "CoolWatermark",
    category = Category.CLIENT,
    description = "Player username"
) {

    override fun SafeClientEvent.updateText() {
        displayText.add(mc.session.username, GuiSetting.primary)
        displayText.add("|", GuiSetting.text)
        displayText.add(InfoCalculator.ping().toString(), GuiSetting.text)
        displayText.add("ms", GuiSetting.text)
        displayText.add("|", GuiSetting.text)
        displayText.add("%.2f".format(tpsBuffer.average()), GuiSetting.text)
        displayText.add("|", GuiSetting.text)
        mc.world.minecraftServer?.let { displayText.add(it.name, GuiSetting.text) }
    }

}