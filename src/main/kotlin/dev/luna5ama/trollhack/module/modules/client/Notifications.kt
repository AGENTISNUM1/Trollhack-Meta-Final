package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.event.events.ModuleToggleEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.util.text.TextFormatting

internal object Notifications : Module(
    name = "Notifications",
    description = "Module toggle notifications",
    category = Category.CLIENT,
    visible = false
) {

    private enum class Mode {
        NAME, PLUS, ONOFF, ARROW
    }
    private val mode by setting("Mode", Mode.NAME)
    init {
        safeListener<ModuleToggleEvent> {
            val enablemessage = when (mode) {
                Mode.NAME -> "${TextFormatting.GREEN}${it.module.nameAsString}"
                Mode.PLUS -> "${TextFormatting.RESET}${it.module.nameAsString} [${TextFormatting.GREEN}+${TextFormatting.RESET}]"
                Mode.ONOFF -> "${TextFormatting.RESET}${it.module.nameAsString}${TextFormatting.GREEN} on"
                Mode.ARROW -> "${TextFormatting.RESET}${it.module.nameAsString} -> ${TextFormatting.GREEN} Enabled"
            }
            val disablemessage = when (mode) {
                Mode.NAME -> "${TextFormatting.RED}${it.module.nameAsString}"
                Mode.PLUS -> "${TextFormatting.RESET}${it.module.nameAsString} [${TextFormatting.RED}+${TextFormatting.RESET}]"
                Mode.ONOFF -> "${TextFormatting.RESET}${it.module.nameAsString}${TextFormatting.RED} off"
                Mode.ARROW -> "${TextFormatting.RESET}${it.module.nameAsString} -> ${TextFormatting.RED} Disabled"
            }
            if (it.module.isDisabled) {
                NoSpamMessage.sendMessage(enablemessage)
            }
            if (it.module.isEnabled) {
                NoSpamMessage.sendMessage(disablemessage)
            }
        }
    }

}