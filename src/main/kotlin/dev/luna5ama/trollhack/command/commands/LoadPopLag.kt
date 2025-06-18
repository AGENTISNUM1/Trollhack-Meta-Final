package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.module.modules.combat.PopLag
import dev.luna5ama.trollhack.util.text.NoSpamMessage


internal object LoadPopLag : ClientCommand(
    name = "LoadPopLag",
    description = "Loads poplag text to reduce its impact",
    alias = arrayOf("lp", "loadpoplag")
){
    init {
        executeSafe {
            NoSpamMessage.sendMessage(PopLag.poplagtext)
        }
    }
}