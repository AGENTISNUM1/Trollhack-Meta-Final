package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.ClickGUI

internal object Username : LabelHud(
    name = "Welcomer",
    category = Category.CLIENT,
    description = "Player Welcomer"
) {
    private val prefix = "Welcome,"
    private val suffix = "!"
    override fun SafeClientEvent.updateText() {
        displayText.add(prefix, ClickGUI.text)
        displayText.add(mc.session.username, ClickGUI.primary)
        displayText.add(suffix, ClickGUI.text)
    }

}