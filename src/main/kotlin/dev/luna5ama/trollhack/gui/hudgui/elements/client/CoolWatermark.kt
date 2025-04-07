package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.fastmc.common.collection.CircularArray.Companion.average
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.gui.hudgui.elements.misc.TPS.tpsBuffer
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.util.InfoCalculator

internal object CoolWatermark : LabelHud(
    name = "CoolWatermark",
    category = Category.CLIENT,
    description = "Player username with server info"
) {
    private val background = setting("Background", true)

    private var fullText = ""

    override fun SafeClientEvent.updateText() {
        displayText.clear()

        val stringBuilder = StringBuilder()
        stringBuilder.append("TrollHack - Wizard Edit")
        stringBuilder.append("|")
        stringBuilder.append(mc.session.username)
        stringBuilder.append("|")
        stringBuilder.append(InfoCalculator.ping().toString())
        stringBuilder.append("ms")
        stringBuilder.append("|")
        stringBuilder.append("%.2f".format(tpsBuffer.average()))
        stringBuilder.append("|")

        val serverInfo = when {
            mc.isSingleplayer -> "Singleplayer"
            mc.currentServerData != null -> {
                val serverData = mc.currentServerData!!
                if (serverData.serverIP.isNotEmpty()) serverData.serverIP else "Unknown Server"
            }
            else -> "Main Menu"
        }
        stringBuilder.append(serverInfo)

        fullText = stringBuilder.toString()
        displayText.add("TrollHack - Wizard Edit", ClickGUI.primary)
        displayText.add("|", ClickGUI.text)
        displayText.add(mc.session.username, ClickGUI.primary)
        displayText.add("|", ClickGUI.text)
        displayText.add(InfoCalculator.ping().toString(), ClickGUI.text)
        displayText.add("ms", ClickGUI.text)
        displayText.add("|", ClickGUI.text)
        displayText.add("%.2f".format(tpsBuffer.average()), ClickGUI.text)
        displayText.add("|", ClickGUI.text)
        displayText.add(serverInfo, ClickGUI.text)

    }

}