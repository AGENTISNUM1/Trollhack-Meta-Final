package dev.luna5ama.trollhack.module.modules.dev

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.world.isReplaceable
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.Item.getItemFromBlock
import net.minecraft.item.ItemSkull
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos

internal object AutoSkull : Module(
    name = "AutoSkull",
    category = Category.META,
    description = "Places a skull at your feet and disables"
) {
    private val ghostSwitchBypass by setting("Ghost Switch Bypass", HotbarSwitchManager.Override.DEFAULT)
    private val disableAfterPlace by setting("Disable After Place", true)
    private val placeIfSurrounded by setting("Place If Surrounded", false)
    private val placeIfSandAbove by setting("Place If Sand Above", false)
    private val torchMode by setting("Torch Mode", false)
    init {
        safeListener<TickEvent.Post> {
            if (isEnabled) {
                if (disableAfterPlace) {
                    placeSkull()
                    disable()
                } else {
                    if (shouldPlaceSkull()) {
                        placeSkull()
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.shouldPlaceSkull(): Boolean {
        val playerPos = mc.player.betterPosition

        if (placeIfSurrounded && isSurrounded(playerPos)) {
            return true
        }

        if (placeIfSandAbove && isSandAbove(playerPos)) {
            return true
        }

        return false
    }

    private fun SafeClientEvent.isSurrounded(playerPos: BlockPos): Boolean {
        val directions = arrayOf(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST)
        return directions.any { world.getBlockState(playerPos.offset(it)).isReplaceable }
    }

    private fun SafeClientEvent.isSandAbove(playerPos: BlockPos): Boolean {
        return world.getBlockState(playerPos.up()) == Blocks.SAND.defaultState
    }


    private fun SafeClientEvent.placeSkull() {
        val playerPos = player.betterPosition
        val blockBelow = playerPos.down()
        if (world.getBlockState(blockBelow).isReplaceable) {
            NoSpamMessage.sendMessage("$chatName Cannot place!")
            return
        } else if (!world.getBlockState(blockBelow).isFullBlock) {
            NoSpamMessage.sendMessage("$chatName Cannot place!")
            return
        }

        val slot = if (torchMode) getTorchSlot() else getSkullSlot()
        slot ?: run {
            NoSpamMessage.sendMessage("$chatName No ${if (torchMode) "torch" else "skull"} in inventory!")
            return
        }

        val sneak = !player.isSneaking
        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))

        ghostSwitch(ghostSwitchBypass, slot) {
            connection.sendPacket(
                CPacketPlayerTryUseItemOnBlock(
                    blockBelow,
                    EnumFacing.UP,
                    EnumHand.MAIN_HAND,
                    0.5f, 0.5f, 0.5f
                )
            )
        }

        if (sneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))
    }


    private fun SafeClientEvent.getSkullSlot(): Slot? {
        for (slot in player.inventoryContainer.inventorySlots) {
            val stack = slot.stack
            if (!stack.isEmpty && stack.item is ItemSkull) {
                return slot
            }
        }
        return null
    }

    private fun SafeClientEvent.getTorchSlot(): Slot? {
        for (slot in player.inventoryContainer.inventorySlots) {
            val stack = slot.stack
            if (!stack.isEmpty && stack.item is ItemBlock && (stack.item as ItemBlock).block == Blocks.REDSTONE_TORCH) {
                return slot
            }
        }
        return null
    }

}