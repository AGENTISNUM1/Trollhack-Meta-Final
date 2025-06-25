package dev.luna5ama.trollhack.module.modules.meta

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.confirmedOrTrue
import dev.luna5ama.trollhack.util.inventory.hasPotion
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.throwAll
import dev.luna5ama.trollhack.util.inventory.slot.allSlots
import dev.luna5ama.trollhack.util.inventory.slot.isHotbarSlot
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemBed
import net.minecraft.item.ItemStack

internal object AutoClean : Module(
    name = "AutoClean",
    description = "Automatically manages inventory by keeping only specified amounts of items",
    category = Category.META,
    modulePriority = 50
) {
    private val cleanDelay by setting("Clean Delay", 1000, 0..5000, 50)
    private val keepInHotbar by setting("Keep In Hotbar", true)
    private val debug by setting("Debug Messages", false)
    
    // Item toggles and their max slots settings
    private val speedPotions by setting("Speed Potions", true)
    private val speedPotionsMaxSlots by setting("Speed Potions Max Slots", 1, 1..10, 1, {speedPotions})
    
    private val goldenApples by setting("Golden Apples", false)
    private val goldenApplesMaxSlots by setting("Golden Apples Max Slots", 1, 1..10, 1, {goldenApples})
    
    private val beds by setting("Beds", false)
    private val bedsMaxSlots by setting("Beds Max Slots", 1, 1..10, 1, {beds})
    
    private val healingPotions by setting("Healing Potions", false)
    private val healingPotionsMaxSlots by setting("Healing Potions Max Slots", 1, 1..10, 1, {healingPotions})
    
    private val totems by setting("Totems", false)
    private val totemsMaxSlots by setting("Totems Max Slots", 1, 1..32, 1, {totems})
    
    private val obsidian by setting("Obsidian", false)
    private val obsidianMaxSlots by setting("Obsidian Max Slots", 1, 1..10, 1, {obsidian})
    
    // Single item throwing toggles
    private val throwSingleBeds by setting("Throw Single Beds", false)
    private val throwSinglePotions by setting("Throw Single Potions", false)
    
    private val cleanTimer = TickTimer()
    private var lastTask: InventoryTask? = null

    init {

        onDisable {
            lastTask = null
        }

        safeParallelListener<TickEvent.Post> {
            if (!cleanTimer.tick(cleanDelay)) return@safeParallelListener
            if (!lastTask.confirmedOrTrue) return@safeParallelListener

            val allSlotsToThrow = mutableListOf<Slot>()
            
            // Process each item type with their specific max slots
            if (speedPotions) {
                allSlotsToThrow.addAll(processItemType(ItemType.SPEED_POTION, speedPotionsMaxSlots))
            }
            if (goldenApples) {
                allSlotsToThrow.addAll(processItemType(ItemType.GOLDEN_APPLE, goldenApplesMaxSlots))
            }
            if (beds) {
                allSlotsToThrow.addAll(processItemType(ItemType.BED, bedsMaxSlots))
            }
            if (healingPotions) {
                allSlotsToThrow.addAll(processItemType(ItemType.HEALING_POTION, healingPotionsMaxSlots))
            }
            if (totems) {
                allSlotsToThrow.addAll(processItemType(ItemType.TOTEM, totemsMaxSlots))
            }
            if (obsidian) {
                allSlotsToThrow.addAll(processItemType(ItemType.OBSIDIAN, obsidianMaxSlots))
            }
            
            // Process single item throwing
            if (throwSingleBeds) {
                allSlotsToThrow.addAll(getSingleItemSlots(ItemType.BED))
            }
            if (throwSinglePotions) {
                allSlotsToThrow.addAll(getSingleItemSlots(ItemType.HEALING_POTION))
            }

            if (allSlotsToThrow.isNotEmpty()) {
                if (debug) {
                    val totalToThrow = allSlotsToThrow.sumBy { it.stack.count }
                    NoSpamMessage.sendMessage("$chatName Throwing $totalToThrow items from ${allSlotsToThrow.size} stacks")
                }

                lastTask = inventoryTask {
                    allSlotsToThrow.forEach { slot ->
                        throwAll(slot)
                    }
                    postDelay(100L)
                    runInGui()
                }
            }
        }
    }

    private enum class ItemType(val maxStackSize: Int) {
        SPEED_POTION(64),
        GOLDEN_APPLE(64),
        BED(64),
        HEALING_POTION(64),
        TOTEM(1),
        OBSIDIAN(64)
    }
    
    private fun SafeClientEvent.processItemType(itemType: ItemType, maxSlots: Int): List<Slot> {
        val itemSlots = getItemSlots(itemType)
        if (itemSlots.isEmpty()) return emptyList()

        val totalNeededSlots = calculateNeededSlots(itemSlots, itemType.maxStackSize)
        if (totalNeededSlots <= maxSlots) return emptyList()

        // Find the best slots to keep
        val slotsToKeep = findBestSlotsToKeep(itemSlots, itemType.maxStackSize, maxSlots)
        return itemSlots.filter { it !in slotsToKeep }
    }
    
    private fun SafeClientEvent.getSingleItemSlots(itemType: ItemType): List<Slot> {
        return player.allSlots.filter { slot ->
            isItemType(slot.stack, itemType) && slot.stack.count == 1
        }
    }

    private fun SafeClientEvent.getItemSlots(itemType: ItemType): List<Slot> {
        return player.allSlots.filter { slot ->
            isItemType(slot.stack, itemType)
        }
    }
    
    private fun calculateNeededSlots(slots: List<Slot>, maxStackSize: Int): Int {
        val totalItems = slots.sumBy { it.stack.count }
        return (totalItems + maxStackSize - 1) / maxStackSize // Ceiling division
    }

    private fun SafeClientEvent.findBestSlotsToKeep(slots: List<Slot>, maxStackSize: Int, maxSlots: Int): List<Slot> {
        if (slots.isEmpty()) return emptyList()

        // Sort by priority: hotbar first, then by stack count (descending)
        val sortedSlots = slots.sortedWith(compareBy<Slot> { 
            if (keepInHotbar && it.isHotbarSlot) 0 else 1 
        }.thenByDescending { 
            it.stack.count 
        })

        val slotsToKeep = mutableListOf<Slot>()
        var remainingSlots = maxSlots
        var remainingItems = sortedSlots.sumBy { it.stack.count }

        for (slot in sortedSlots) {
            if (remainingSlots <= 0) break
            
            val itemsInSlot = slot.stack.count
            val itemsAfterThisSlot = remainingItems - itemsInSlot
            val slotsNeededAfterThis = (itemsAfterThisSlot + maxStackSize - 1) / maxStackSize
            
            if (slotsNeededAfterThis < remainingSlots) {
                // We can afford to keep this slot
                slotsToKeep.add(slot)
                remainingSlots--
            }
            remainingItems -= itemsInSlot
        }

        return slotsToKeep
    }

    private fun isItemType(stack: ItemStack, itemType: ItemType): Boolean {
        return when (itemType) {
            ItemType.SPEED_POTION -> stack.item == Items.SPLASH_POTION && stack.hasPotion(MobEffects.SPEED)
            ItemType.GOLDEN_APPLE -> stack.item == Items.GOLDEN_APPLE
            ItemType.BED -> stack.item is ItemBed || isBedBlock(stack.item)
            ItemType.HEALING_POTION -> (stack.item == Items.SPLASH_POTION || stack.item == Items.POTIONITEM) && 
                                     stack.hasPotion(MobEffects.INSTANT_HEALTH)
            ItemType.TOTEM -> stack.item == Items.TOTEM_OF_UNDYING
            ItemType.OBSIDIAN -> stack.item == Item.getItemFromBlock(Blocks.OBSIDIAN)
        }
    }
    
    private fun isBedBlock(item: Item): Boolean {
        return item == Item.getItemFromBlock(Blocks.BED)
    }
}
