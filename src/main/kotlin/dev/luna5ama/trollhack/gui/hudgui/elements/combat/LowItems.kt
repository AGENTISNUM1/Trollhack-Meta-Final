package dev.luna5ama.trollhack.gui.hudgui.elements.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.util.inventory.hasPotion
import dev.luna5ama.trollhack.util.inventory.slot.allSlots
import dev.fastmc.common.TickTimer
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.item.ItemStack

internal object LowItems : LabelHud(
    name = "LowItems",
    category = Category.COMBAT,
    description = "Warns when low on splash potions or beds"
) {
    private val potionThreshold by setting("Potion Stack Threshold", 1.0f, 0.5f..3.0f, 0.1f)
    private val bedThreshold by setting("Bed Stack Threshold", 1.0f, 0.5f..3.0f, 0.1f)
    private val showPotionWarning by setting("Show Potion Warning", true)
    private val showBedWarning by setting("Show Bed Warning", true)

    private val updateTimer = TickTimer()
    private var cachedPotionCount = 0
    private var cachedBedCount = 0

    override fun SafeClientEvent.updateText() {
        if (updateTimer.tickAndReset(5)) {
            cachedPotionCount = countInstantHealthPotions()
            cachedBedCount = countBeds()
        }

        displayText.clear()

        if (showPotionWarning) {
            val potionThresholdAmount = (potionThreshold * 64).toInt()
            if (cachedPotionCount < potionThresholdAmount) {
                displayText.add("Low on pots! ($cachedPotionCount/${potionThresholdAmount})", ClickGUI.primary)
            }
        }

        if (showBedWarning) {
            val bedThresholdAmount = (bedThreshold * 64).toInt()
            if (cachedBedCount < bedThresholdAmount) {
                displayText.add("Low on beds! ($cachedBedCount/${bedThresholdAmount})", ClickGUI.primary)
            }
        }
    }

    private fun SafeClientEvent.countInstantHealthPotions(): Int {
        return player.allSlots.sumBy { slot ->
            if (isInstantHealthPotion(slot.stack)) slot.stack.count else 0
        }
    }

    private fun SafeClientEvent.countBeds(): Int {
        return player.allSlots.sumBy { slot ->
            if (isBed(slot.stack)) slot.stack.count else 0
        }
    }

    private fun isInstantHealthPotion(stack: ItemStack): Boolean {
        return stack.item == Items.SPLASH_POTION && stack.hasPotion(MobEffects.INSTANT_HEALTH)
    }

    private fun isBed(stack: ItemStack): Boolean {
        return stack.item == Items.BED ||
                stack.item.registryName?.path?.endsWith("_bed") == true // Colored beds
    }
}