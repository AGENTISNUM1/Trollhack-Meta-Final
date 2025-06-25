package dev.luna5ama.trollhack.module.modules.meta

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.inventory.hasPotion
import dev.luna5ama.trollhack.util.inventory.slot.allSlots
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.isHotbarSlot
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.world.getGroundLevel
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

internal object SpeedPot : Module(
    name = "SpeedPot",
    description = "speed",
    category = Category.META,
    modulePriority = 100
) {
    private val BAD_BLOCKS: Set<Block> = HashSet<Block>(listOf(Blocks.AIR, Blocks.WATER, Blocks.LAVA, Blocks.ICE, Blocks.PACKED_ICE))
    private val ghostSwitchBypass = HotbarSwitchManager.Override.NONE
    private val speed = true
    private val speedDelay by setting("Speed Delay", 5000, 0..10000, 50, SpeedPot::speed)
    private val debug by setting("debug messages", false)
   // private val hotbarSlot = 1

    private var currentPotion = PotionType.NONE

    override fun getHudInfo(): String {
        return currentPotion.displayString
    }

    init {
        listener<PacketEvent.PostReceive> {}

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            if (!groundCheck()) {
                return@safeListener
            }

            if (currentPotion == PotionType.NONE) {
                currentPotion = PotionType.VALUES.first {
                    it.check(this)
                }
            }

            if (currentPotion == PotionType.SPEED) {
                sendPlayerPacket {
                    rotate(Vec2f(player.rotationYaw, 90.0f))
                }
            }
        }

        safeListener<OnUpdateWalkingPlayerEvent.Post> {
            val potionType = currentPotion
            if (PlayerPacketManager.prevRotation.y <= 85.0f || PlayerPacketManager.rotation.y <= 85.0f) return@safeListener

            getSlot(potionType)?.let {
                if (!BAD_BLOCKS.contains(
                        mc.world.getBlockState(
                            BlockPos(
                                mc.player.posX,
                                mc.player.posY - 1,
                                mc.player.posZ
                            )
                        ).block
                    )
                ) {
                    ghostSwitch(ghostSwitchBypass, it) {
                        connection.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                        potionType.timer.reset()
                        currentPotion = PotionType.NONE
                        if (debug) {
                            NoSpamMessage.sendMessage("$chatName you now have speed")
                        }
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.groundCheck(): Boolean {
        return player.onGround
                || player.posY - world.getGroundLevel(player) < 3.0
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
                if (it.isHotbarSlot) -1
                else it.stack.count
            }
    }
    private fun countInstantHealthPotions(): Int {
        return mc.player.allSlots.sumBy { slot ->
            if (isInstantHealthPotion(slot.stack)) slot.stack.count else 0
        }
    }
    private fun isInstantHealthPotion(stack: ItemStack): Boolean {
        return stack.item == Items.SPLASH_POTION && stack.hasPotion(MobEffects.SPEED)
    }

    private enum class PotionType(override val displayName: CharSequence, val potion: Potion) : DisplayEnum {
        SPEED("Speed", MobEffects.SPEED) {
            override fun check(event: SafeClientEvent): Boolean {
                return speed
                        && timer.tick(speedDelay)
                        && !event.player.isPotionActive(MobEffects.SPEED)
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
            return event.player.allSlots.any { it.stack.hasPotion(this.potion) }
        }

        companion object {
            @JvmField
            val VALUES = values()
        }
    }
}