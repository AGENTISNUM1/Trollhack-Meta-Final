package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.WorldEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.manager.managers.WaypointManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.client.audio.PositionedSoundRecord
import com.mojang.text2speech.Narrator;
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.util.text.TextFormatting

internal object VisualRange : Module(
    name = "VisualRange",
    description = "Shows players who enter and leave range in chat",
    category = Category.CHAT
) {
    private final val narrator = Narrator.getNarrator()
    private val playSound by setting("Play Sound", false)
    private val sound by setting("Sound", sound0.PING, { playSound })
    private val leaving0 = setting("Count Leaving", false)
    private val leaving by leaving0
    private val friends by setting("Friends", true)
    private val msgaura by setting("Msg Aura", false)
    private val logToFile by setting("Log To File", false)
    private val Message by setting("Msg Aura Message", "I see u :D", visibility = { msgaura })
    private val oppspotted by setting("Opp Spotted", false)
    private enum class sound0{
        PING, LEVELUP, HURT
    }
    private val playerSet = LinkedHashSet<EntityPlayer>()
    private val timer = TickTimer(TimeUnit.SECONDS)

    init {
        onDisable {
            playerSet.clear()
        }

        listener<WorldEvent.Unload> {
            playerSet.clear()
        }

        listener<WorldEvent.Entity.Remove> {
            if (it.entity !is EntityPlayer) return@listener
            if (playerSet.remove(it.entity)) {
                onLeave(it.entity)
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (!timer.tickAndReset(1L)) return@safeParallelListener

            for (entityPlayer in world.playerEntities) {
                if (entityPlayer.isFakeOrSelf) continue
                if (!friends && FriendManager.isFriend(entityPlayer.name)) continue

                if (playerSet.add(entityPlayer)) {
                    onEnter(entityPlayer)
                }
            }
        }
    }
    private fun getSound() = when (sound) {
        sound0.PING -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP
        sound0.LEVELUP -> SoundEvents.ENTITY_PLAYER_LEVELUP
        sound0.HURT -> SoundEvents.ENTITY_PLAYER_HURT
    }

    private fun onEnter(player: EntityPlayer) {
        val pos = player.flooredPosition
        var message = ""
        if (player.name == "g2i" || player.name == "ekenegas") {
            message = "Confio spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "mudhutpvper") {
            message = "CCP (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "coinbaselogs") {
            message = "CCP (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "NakadashiLover") {
            message = "CCP (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "m0omoomeadows") {
            message = "CCP (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "rwdo") {
            message = "CCP (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "7pee") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "Y4vi") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "1Q13") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "Tkoq") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "absolutist") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "Niur") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "Silverless") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "Keleemo") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "Wizard_11") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "SammyTheElf") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else if (player.name == "YOSHETTO") {
            message = "Homie (${player.name}) spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        } else {
            message = "${player.name} spotted at (${pos.x}, ${pos.y}, ${pos.z})"
        }
        if (oppspotted && FriendManager.isFriend(player.name)) {
            narrator.clear()
            narrator.say("Ah pu  spotted! i repeat, Ah pu  spotted!")
        }
        
        sendNotification(player, message)
        if (logToFile) WaypointManager.add(pos, message)
        if (msgaura) sendServerMessage("/w ${player.name} $Message")
    }

    private fun onLeave(player: EntityPlayer) {
        if (!leaving) return
        val pos = player.flooredPosition
        val message = "${player.name} left at (${pos.x}, ${pos.y}, ${pos.z})"

        sendNotification(player, message)
        if (logToFile) WaypointManager.add(pos, message)
    }

    private fun getColor(player: EntityPlayer) =
        if (FriendManager.isFriend(player.name)) TextFormatting.GREEN
        else TextFormatting.RED

    private fun sendNotification(player: EntityPlayer, message: String) {
        if (playSound) mc.soundHandler.playSound(
            PositionedSoundRecord.getRecord(
                getSound(),
                1.0f,
                1.0f
            )
        )
        NoSpamMessage.sendMessage(VisualRange.hashCode() xor player.name.hashCode(), message)
    }
}
