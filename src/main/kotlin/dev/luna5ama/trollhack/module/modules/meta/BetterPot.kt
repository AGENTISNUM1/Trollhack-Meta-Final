package dev.luna5ama.trollhack.module.modules.meta

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.FastUse
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.confirmedOrTrue
import dev.luna5ama.trollhack.util.inventory.hasPotion
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.swapWith
import dev.luna5ama.trollhack.util.inventory.slot.*
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.MobEffects
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.potion.Potion
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos


internal object BetterPot : Module(
    name = "BetterPot",
    description = "yes",
    category = Category.META,
    modulePriority = 9999
) {
    private val BAD_BLOCKS: Set<Block> = HashSet<Block>(listOf(Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.ICE, Blocks.PACKED_ICE))
    var ghostSwitchBypass by setting("Ghost Switch Bypass", HotbarSwitchManager.Override.NONE)
    var keepHealInHotbar by setting("Keep Heal Potion In Hotbar", true)
    private val healHotbar by setting("Heal Potion Hotbar", 7, 1..9, 1)
    var healHealth by setting("Heal Health", 12.0f, 0.0f..20.0f, 0.5f)
    var healDelay by setting("Heal Delay", 68, 0..300, 50)
    private var currentPotion = PotionType.NONE
    private var cachedPotionCount = 0
    private var lastTask: InventoryTask? = null

    override fun getHudInfo(): String {
        return cachedPotionCount.toString()
    }

    init {
        onEnable {
            if (!FastUse.isEnabled) FastUse.enable()
            cachedPotionCount = countInstantHealthPotions()
        }
        onDisable {
            currentPotion = PotionType.NONE
            lastTask = null
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (currentPotion == PotionType.NONE) {
                currentPotion = PotionType.VALUES.first {
                    it.check(this)
                }
            }

            if (currentPotion != PotionType.NONE) {
                if (!BAD_BLOCKS.contains(
                        mc.world.getBlockState(
                            BlockPos(
                                mc.player.posX,
                                mc.player.posY + 2.0,
                                mc.player.posZ
                            )
                        ).block
                    )
                ) {
                    sendPlayerPacket {
                        rotate(Vec2f(player.rotationYaw, -90.0f))
                    }
                } else {
                    sendPlayerPacket {
                        rotate(Vec2f(player.rotationYaw, 90.0f))
                    }
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Post> {
            val potionType = currentPotion
            if (potionType == PotionType.NONE) return@safeListener
            getSlot(potionType)?.let {
                ghostSwitch(ghostSwitchBypass, it) {
                    connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                    potionType.timer.reset()
                    currentPotion = PotionType.NONE

                }
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (!keepHealInHotbar) return@safeParallelListener
            if (player.hotbarSlots.hasPotion(PotionType.INSTANT_HEALTH)) return@safeParallelListener
            if (!lastTask.confirmedOrTrue) return@safeParallelListener
            if (currentPotion != PotionType.NONE && currentPotion != PotionType.INSTANT_HEALTH) return@safeParallelListener

            val slotFrom = player.allSlots.findPotion(PotionType.INSTANT_HEALTH) ?: return@safeParallelListener
            lastTask = inventoryTask {
                swapWith(slotFrom, player.hotbarSlots[healHotbar - 1])
                postDelay(100L)
                runInGui()
            }
        }
    }

    private fun SafeClientEvent.getSlot(potionType: PotionType): Slot? {
        return player.allSlotsPrioritized.findPotion(potionType)
    }

    private fun List<Slot>.findPotion(potionType: PotionType): Slot? {
        return this.asSequence()
            .filter {
                val stack = it.stack
                stack.item == Items.SPLASH_POTION && stack.hasPotion(potionType.potion)
            }.minByOrNull {
                if (it.isHotbarSlot) -1 else it.stack.count
            }
    }

    private fun List<Slot>.hasPotion(potionType: PotionType): Boolean {
        return hasItem(Items.SPLASH_POTION) { itemStack ->
            itemStack.hasPotion(potionType.potion)
        }
    }
    private fun countInstantHealthPotions(): Int {
        return mc.player.allSlots.sumBy { slot ->
            if (isInstantHealthPotion(slot.stack)) slot.stack.count else 0
        }
    }
    private fun isInstantHealthPotion(stack: ItemStack): Boolean {
        return stack.item == Items.SPLASH_POTION && stack.hasPotion(MobEffects.INSTANT_HEALTH)
    }
    private enum class PotionType(override val displayName: CharSequence, val potion: Potion) : DisplayEnum {
        INSTANT_HEALTH("Heal", MobEffects.INSTANT_HEALTH) {
            override fun check(event: SafeClientEvent): Boolean {
                return timer.tick(healDelay)
                        && event.player.health <= healHealth
                        && super.check(event)
            }
        },

        NONE("", MobEffects.LUCK) {
            override fun check(event: SafeClientEvent): Boolean {
                return true
            }
        };

        val timer = TickTimer()

        open fun check(event: SafeClientEvent): Boolean {
            return event.player.allSlots.hasPotion(this)
        }

        companion object {
            @JvmField
            val VALUES = values()
        }
    }
}