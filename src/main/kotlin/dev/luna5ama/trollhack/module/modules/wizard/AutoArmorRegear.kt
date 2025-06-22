package dev.luna5ama.trollhack.module.modules.wizard

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.*
import dev.luna5ama.trollhack.util.inventory.operation.action
import dev.luna5ama.trollhack.util.inventory.operation.throwAll
import dev.luna5ama.trollhack.module.modules.combat.AutoArmor
import dev.luna5ama.trollhack.module.modules.combat.AutoRegear
import dev.luna5ama.trollhack.util.inventory.slot.*
import net.minecraft.item.ItemArmor

internal object AutoArmorRegear : Module(
    name = "Auto Armor Regear",
    category = Category.META,
    description = "Drops all armor when enabled, then automatically equips best armor",
    modulePriority = 500
) {

    private val moveTimer = TickTimer(TimeUnit.TICKS)
    private var lastTask: InventoryTask? = null
    private var hasDroppedArmor = false

    init {
        onEnable {
            AutoArmor.enable()
            AutoArmor.antiGlitchArmor = true
            AutoArmor.stackedArmor = true
            AutoArmor.runInGui = true
            AutoRegear.takeArmor = true
            dropAllArmor()
            AutoRegear.toggleShulkerPlace()
            AutoArmorRegear.disable()
        }
    }

    private fun dropAllArmor() {
        inventoryTask {
            // Drop all armor slots completely (handle stacked armor)
            mc.player.armorSlots.forEach { slot ->
                if (slot.hasStack) {
                    throwAll(slot)
                }
            }
            
            // Drop armor from inventory slots 5, 6, 7, 8 if any
            listOf(5, 6, 7, 8).forEach { slotNumber ->
                val slot = mc.player.inventorySlots.getOrNull(slotNumber)
                if (slot?.hasStack == true && slot.stack.item is ItemArmor) {
                    throwAll(slot)
                }
            }
            
            postDelay(5, TimeUnit.TICKS) // Wait for items to drop
            
            // Double-check and drop any remaining armor
            action {
                player.armorSlots.forEach { slot ->
                    while (slot.hasStack) {
                        throwAll(slot)
                    }
                }
            }
            
            postDelay(3, TimeUnit.TICKS) // Wait before starting equip logic
            runInGui()
        }
        
        moveTimer.reset()
    }
}

