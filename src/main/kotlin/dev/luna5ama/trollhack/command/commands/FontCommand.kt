package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.util.text.NoSpamMessage

object FontCommand : ClientCommand(
    name = "fonts",
    description = "list fonts"
) {
    init {
        executeSafe {
            NoSpamMessage.sendMessage("Available fonts: Comic, Geo, Gidole, Orbitron, Queen, Underdog, WinkySans")
        }
    }
}