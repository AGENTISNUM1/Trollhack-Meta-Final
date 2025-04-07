package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.confirmedOrTrue
import dev.luna5ama.trollhack.util.inventory.inventoryTaskNow
import dev.luna5ama.trollhack.util.inventory.operation.moveTo
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.inventory.slot.inventorySlots
import dev.luna5ama.trollhack.util.inventory.slot.offhandSlot
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.onMainThread
import net.minecraft.init.Items
import net.minecraft.inventory.Slot

internal object  AutoTotem : Module(
    name = "AutoTotem",
    description = "Puts a totem in your offhand always",
    category = Category.COMBAT,
    modulePriority = 2000
) {
    private val delay by setting("Delay", 1, 1..20, 1, description = "Ticks to wait between each move")
    private val confirmTimeout by setting(
        "Confirm Timeout",
        2,
        0..20,
        1,
        description = "Maximum ticks to wait for confirm packets from server"
    )

    private var lastTask: InventoryTask? = null

    override fun getHudInfo(): String {
        return "TOTEM"
    }

    init {
        onEnable {
            if (Offhand.isEnabled) {
                NoSpamMessage.sendMessage("$chatName Offhand disabled")
                Offhand.disable()
            }
        }
        onDisable {
            lastTask = null
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (player.isDead || player.health <= 0.0f || !lastTask.confirmedOrTrue) return@safeListener

            ensureTotemInOffhand()
        }
    }

    private fun SafeClientEvent.ensureTotemInOffhand() {
        if (player.heldItemOffhand.item == Items.TOTEM_OF_UNDYING) return

        val slot = getTotemSlot() ?: return

        onMainThread {
            lastTask = inventoryTaskNow {
                postDelay(delay, TimeUnit.TICKS)
                timeout(confirmTimeout, TimeUnit.TICKS)
                moveTo(slot, player.offhandSlot)
            }
        }    }

    private fun SafeClientEvent.getTotemSlot(): Slot? {
        return player.hotbarSlots.find { it.stack.item == Items.TOTEM_OF_UNDYING }
            ?: player.inventorySlots.find { it.stack.item == Items.TOTEM_OF_UNDYING }
    }
}