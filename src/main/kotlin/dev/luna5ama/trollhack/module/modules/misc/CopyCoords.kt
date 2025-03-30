package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

internal object CopyCoords : Module(
    name = "CopyCoords",
    description = "Copies your current coordinates to clipboard",
    category = Category.MISC,
    modulePriority = 200
) {
    init {
        onEnable {
            try {
                val player = mc.player ?: run {
                    NoSpamMessage.sendMessage("§cError: Player is null")
                    disable()
                    return@onEnable
                }

                val posX = player.posX.toInt()
                val posY = player.posY.toInt()
                val posZ = player.posZ.toInt()
                val coords = "X: $posX Y: $posY Z: $posZ"

                try {
                    val stringSelection = StringSelection(coords)
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(stringSelection, null)
                    NoSpamMessage.sendMessage("§aCoordinates copied to clipboard: $coords")
                } catch (e: Exception) {
                    NoSpamMessage.sendMessage("§cFailed to access clipboard: ${e.message}")
                }
            } catch (e: Exception) {
                NoSpamMessage.sendMessage("§cError copying coordinates: ${e.message}")
            } finally {
                disable()
            }
        }
    }
}