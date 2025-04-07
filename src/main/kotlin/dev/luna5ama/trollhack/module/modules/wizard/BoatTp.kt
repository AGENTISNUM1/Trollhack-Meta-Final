package dev.luna5ama.trollhack.module.modules.wizard

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.player.PlayerAttackEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.inventory.slot.hotbarIndex
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import net.minecraft.entity.item.EntityBoat
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketVehicleMove
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import java.util.concurrent.TimeUnit

internal object BoatTp : Module(
    name = "BoatTp",
    description = "Allows you to teleport with a boat",
    category = Category.WIZARD,
    modulePriority = 200
) {
    private val y1 = setting("Desync Value", 0.1f, 0.1f..0.9f, 0.1f)
    private val y2 = setting("Teleport Distance", 130.0f, 1.0f..300f, 10f)
    private val placeBoat = setting("Place Boat", true)
    private val loopCount = setting("Loop Count", 8, 1..20, 1)
    private val maxWaitTime = setting("Max Wait Time", 2000, 500..5000, 100)

    init {
        onEnable {
            onMainThreadSafe {
                try {
                    if (!placeBoatAndCheck()) return@onMainThreadSafe
                    performBoatTeleport()
                } finally {
                    disable()
                }
            }
        }

        safeListener<PlayerAttackEvent> { event ->
            if (isEnabled && event.entity is EntityBoat) {
                event.cancel()
            }
        }
    }

    private fun SafeClientEvent.placeBoatAndCheck(): Boolean {
        if (placeBoat.value && player.ridingEntity !is EntityBoat) {
            // Search all inventory slots (prioritized) for a boat
            val boatSlot = player.allSlotsPrioritized.firstItem(Items.BOAT) ?: run {
                NoSpamMessage.sendError("$chatName No boat found in inventory")
                return false
            }

            // Switch to boat slot and place it
            ghostSwitch(boatSlot) {
                val lookPos = player.positionVector.add(player.lookVec.scale(2.0))
                val blockPos = BlockPos(lookPos)
                connection.sendPacket(
                    CPacketPlayerTryUseItemOnBlock(
                        blockPos,
                        EnumFacing.UP,
                        EnumHand.MAIN_HAND,
                        0.5f, 1.0f, 0.5f
                    )
                )
            }

            // Wait for boat entry with timeout
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < maxWaitTime.value) {
                if (player.ridingEntity is EntityBoat) break
                TimeUnit.MILLISECONDS.sleep(50)
            }

            if (player.ridingEntity !is EntityBoat) {
                NoSpamMessage.sendError("$chatName Failed to enter boat")
                return false
            }
        }
        return true
    }

    private fun SafeClientEvent.performBoatTeleport() {
        val boat = player.ridingEntity as? EntityBoat ?: return
        val originalPos = boat.positionVector

        // Create movement packets
        boat.setPosition(boat.posX, boat.posY + y1.value, boat.posZ)
        val groundPacket = CPacketVehicleMove(boat)

        boat.setPosition(boat.posX, boat.posY + y2.value, boat.posZ)
        val skyPacket = CPacketVehicleMove(boat)

        // Reset position
        boat.setPosition(originalPos.x, originalPos.y, originalPos.z)

        // Send teleport packets
        repeat(loopCount.value) {
            connection.sendPacket(skyPacket)
            connection.sendPacket(groundPacket)
        }
        connection.sendPacket(CPacketVehicleMove(boat))
    }
}