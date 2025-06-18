package dev.luna5ama.trollhack.module.modules.wizard

import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.item.ItemFood
import net.minecraft.network.play.client.CPacketPlayerDigging

internal object PacketEat : Module(
    name = "PacketEat",
    description = "eat w/ packet",
    category = Category.META,
    modulePriority = 200
) {
    init {
        safeListener<PacketEvent.Send> {
            if (it.packet is CPacketPlayerDigging && (it.packet as CPacketPlayerDigging).action == CPacketPlayerDigging.Action.RELEASE_USE_ITEM && mc.player.heldItemMainhand.item is ItemFood) {
                it.cancel()
                it.cancelled = true
            }
        }
    }
}