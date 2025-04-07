package dev.luna5ama.trollhack.module.modules.wizard

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.process.PauseProcess.pauseBaritone
import dev.luna5ama.trollhack.process.PauseProcess.unpauseBaritone
import dev.luna5ama.trollhack.util.combat.CombatUtils.scaledHealth
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
    category = Category.WIZARD
) {
    private val alwayseat by setting("Always Eat", false)
    private val health by setting("Health", false, description = "Eat when ur health gets below a certain amount")
    private val belowHealth by setting("Below Health", 10, 1..36, 1)
    private val eatBadFood by setting("Eat Bad Food", false)
    private val onlygapples by setting("Only Gapples", false, description = "Only eat enchanted golden apples")
    private val pauseBaritone by setting("Pause Baritone", true)
    private val swap by setting("Swap", false, description = "swaps to the best food")
    private val eatWhileTakingDamage by setting("Eat While Taking Damage", true,
        description = "Force eat when taking damage")

    private var lastSlot = -1
    private var eating = false
    private var currentFoodStack: ItemStack? = null
    private var lastTask: InventoryTask? = null
    private var wasTakingDamage = false

    override fun isActive(): Boolean {
        return isEnabled && eating
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
            val shouldForceEat = eatWhileTakingDamage && isTakingDamage && !wasTakingDamage
            wasTakingDamage = isTakingDamage

            // If we're currently eating, check if we should continue
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
                !shouldEat() && !shouldForceEat -> null
                isValid(player.heldItemOffhand) -> EnumHand.OFF_HAND
                isValid(player.heldItemMainhand) -> EnumHand.MAIN_HAND
                swap -> {
                    if (swapToFood()) {
                        startEating()
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

    private fun SafeClientEvent.shouldEat() =
        alwayseat || (health && player.scaledHealth < belowHealth)

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
        lastSlot = player.inventory.currentItem
        val hasFoodInSlots = swapToItem<ItemFood> { isValid(it) }

        return if (hasFoodInSlots) {
            true
        } else {
            lastSlot = -1
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
                && (!onlygapples || item == Items.GOLDEN_APPLE || item == Items.GOLDEN_CARROT)
                && player.canEat(true)
    }

    private fun isBadFood(itemStack: ItemStack, item: ItemFood) =
        item == Items.ROTTEN_FLESH
                || item == Items.SPIDER_EYE
                || item == Items.POISONOUS_POTATO
                || item == Items.FISH && (itemStack.metadata == 3 || itemStack.metadata == 2)


}