package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.process.PauseProcess.pauseBaritone
import dev.luna5ama.trollhack.process.PauseProcess.unpauseBaritone
import dev.luna5ama.trollhack.util.combat.CombatUtils.scaledHealth
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.confirmedOrTrue
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.swapToItem
import dev.luna5ama.trollhack.util.inventory.operation.swapToSlot
import dev.luna5ama.trollhack.util.inventory.operation.swapWith
import dev.luna5ama.trollhack.util.inventory.slot.anyHotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.inventory.slot.storageSlots
import dev.luna5ama.trollhack.util.threads.runSafe
import dev.luna5ama.trollhack.util.atValue
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemFood
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemTool
import net.minecraft.util.EnumHand

internal object AutoEatPlus : Module(
    name = "Auto Eat Plus",
    description = "Automatically eat",
    category = Category.META
) {
    private val mode0 = setting("Mode", EatMode.HEALTH)
    private val mode by mode0
    private val belowHealth by setting("Below Health", 10, 1..36, 1, mode0.atValue(EatMode.HEALTH))
    private val eatBadFood by setting("Eat Bad Food", false)
    private val onlyGoldenApples by setting("Only Golden Apples", false, description = "Only eat golden apples and enchanted golden apples")
    private val autoSwitch by setting("Auto Switch", false, description = "Automatically switch to food in hotbar/inventory")
    private val pauseBaritone by setting("Pause Baritone", true)

    private var lastSlot = -1
    private var eating = false
    private var currentFoodStack: ItemStack? = null
    private var lastTask: InventoryTask? = null
    private var wasTakingDamage = false

    private enum class EatMode(override val displayName: CharSequence) : DisplayEnum {
        DAMAGE("When Taking Damage"),
        HEALTH("Below Health"),
        ALWAYS("Always Eat")
    }

    override fun isActive(): Boolean {
        return isEnabled && eating
    }

    override fun getHudInfo(): String {
        return mode.displayString
    }

    init {
        onDisable {
            stopEating()
            swapBack()
        }

        safeListener<TickEvent.Pre> {
            if (!lastTask.confirmedOrTrue) return@safeListener

            if (!player.isEntityAlive) {
                if (eating) stopEating()
                return@safeListener
            }

            val isTakingDamage = player.hurtTime > 0
            wasTakingDamage = isTakingDamage

            if (eating) {
                val activeStack = player.getHeldItem(currentHand())

                if (currentFoodStack == null
                    || !ItemStack.areItemStacksEqual(activeStack, currentFoodStack) ||
                    (currentFoodStack?.count ?: 0) > activeStack.count) {
                    stopEating()
                }
                return@safeListener
            }

            val hand = when {
                !shouldEat() -> null
                isValid(player.heldItemOffhand) -> EnumHand.OFF_HAND
                isValid(player.heldItemMainhand) -> EnumHand.MAIN_HAND
                autoSwitch -> {
                    if (swapToFood()) {
                        // Will start eating on next tick after swap
                        return@safeListener
                    } else {
                        null
                    }
                }
                else -> null
            }

            if (hand != null) {
                currentFoodStack = player.getHeldItem(hand).copy()
                eat(hand)
            } else {
                swapBack()
            }
        }
    }

    private fun currentHand(): EnumHand {
        return if (mc.player.heldItemOffhand.item is ItemFood) EnumHand.OFF_HAND else EnumHand.MAIN_HAND
    }

    private fun SafeClientEvent.shouldEat(): Boolean {
        return when (mode) {
            EatMode.ALWAYS -> player.canEat(true)
            EatMode.HEALTH -> player.scaledHealth < belowHealth && player.canEat(true)
            EatMode.DAMAGE -> player.hurtTime > 0 && player.canEat(true)
        }
    }

    private fun SafeClientEvent.eat(hand: EnumHand) {
        if (!player.isHandActive || player.activeHand != hand) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
            playerController.processRightClick(player, world, hand)
        }
        startEating()
    }

    private fun startEating() {
        if (pauseBaritone) pauseBaritone()
        eating = true
    }

    private fun stopEating() {
        unpauseBaritone()
        runSafe {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
        }
        eating = false
        currentFoodStack = null
    }

    private fun swapBack() {
        val slot = lastSlot
        if (slot == -1) return

        lastSlot = -1
        runSafe {
            swapToSlot(slot)
        }
    }

    private fun SafeClientEvent.swapToFood(): Boolean {
        // First try to find food in hotbar
        lastSlot = player.inventory.currentItem
        
        // Try to swap to food in hotbar first
        val hasFoodInHotbar = swapToItem<ItemFood> { isValid(it) }
        
        return if (hasFoodInHotbar) {
            true
        } else {
            // If no food in hotbar, try to move from inventory
            moveFoodToHotbar()
        }
    }

    private fun SafeClientEvent.moveFoodToHotbar(): Boolean {
        val slotFrom = player.storageSlots.firstItem<ItemFood, Slot> {
            isValid(it)
        } ?: return false

        val slotTo = player.anyHotbarSlot {
            val item = it.item
            item !is ItemTool && item !is ItemBlock
        }

        lastTask = inventoryTask {
            swapWith(slotFrom, slotTo)
        }

        return true
    }

    private fun SafeClientEvent.isValid(itemStack: ItemStack): Boolean {
        val item = itemStack.item

        return item is ItemFood
                && item != Items.CHORUS_FRUIT
                && (eatBadFood || !isBadFood(itemStack, item))
                && (!onlyGoldenApples || item == Items.GOLDEN_APPLE)
                && player.canEat(mode == EatMode.ALWAYS)
    }

    private fun isBadFood(itemStack: ItemStack, item: ItemFood) =
        item == Items.ROTTEN_FLESH
                || item == Items.SPIDER_EYE
                || item == Items.POISONOUS_POTATO
                || (item == Items.FISH && (itemStack.metadata == 3 || itemStack.metadata == 2))
}
