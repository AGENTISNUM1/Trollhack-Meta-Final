package dev.luna5ama.trollhack.command.commands

import dev.luna5ama.trollhack.command.ClientCommand
import dev.luna5ama.trollhack.command.commands.FriendCommand.player
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.entity.player.EntityPlayer

object SendCoords : ClientCommand(
    name = "sendcoords",
    alias = arrayOf(),
    description = "send ur coords"
) {
    init {
        player("Player") { playerArg ->
            executeSafe {
                mc.player.sendChatMessage("/w ${playerArg.value.name} My coordinates are ${pos()} in the ${getdim()}")
            }
        }

        executeSafe {
            mc.player.sendChatMessage("My coordinates are ${pos()} in the ${getdim()}")
        }
    }

    private fun getdim(): String {
        return when (mc.player.dimension) {
            0 -> "Overworld"
            -1 -> "Nether"
            1 -> "End"
            else -> "Unknown Dimension"
        }
    }

    private fun pos(): String {
        val posx = mc.player.posX.toInt()
        val posy = mc.player.posY.toInt()
        val posz = mc.player.posZ.toInt()
        return "$posx, $posy, $posz"
    }
}