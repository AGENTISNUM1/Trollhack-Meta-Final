package dev.luna5ama.trollhack.command.commands

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import java.util.concurrent.TimeUnit

object KillCommand : ClientCommand(
    name = "kill",
    description = "kills u",
) {
    private val timer = TickTimer(dev.fastmc.common.TimeUnit.SECONDS)
    init {
        executeSafe {
            mc.player.sendChatMessage("> I no longer wish to be alive")
            if (timer.tickAndReset(3)) mc.player.sendChatMessage("/kill")

        }
    }
}