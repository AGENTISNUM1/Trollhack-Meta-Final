package dev.luna5ama.trollhack.module.modules.wizard

import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.MovementUtils.isMoving
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.MathHelper
import kotlin.math.floor

internal object CornerClip : Module(
    name = "Clip",
    description = "clips u in wall",
    category = Category.WIZARD,
    modulePriority = 9999
) {
    private val delay by setting("Delay", 5, 1..10, 1)
    private val timeout by setting("Timout", 5, 1..20, 1)
    private var packets = 0
    var ticks: Int = 0

    init {
        onDisable {
            packets = 0
            ticks = 0
        }
        safeListener<TickEvent.Pre> {
            if (mc.player.isMoving) {
                ticks = 0
                return@safeListener
            }
            ++ticks
            if (ticks > timeout) {
                return@safeListener
            }
            if (Companion.mc.world.getCollisionBoxes(Companion.mc.player as Entity, Companion.mc.player.getEntityBoundingBox().grow(0.01, 0.0, 0.01))
                    .size < 2
            ) {
                Companion.mc.player.setPosition(
                    roundToClosest(
                        Companion.mc.player.posX, floor(Companion.mc.player.posX) + 0.301, floor(
                            Companion.mc.player.posX) + 0.699),
                    Companion.mc.player.posY,
                    roundToClosest(
                        Companion.mc.player.posZ, floor(Companion.mc.player.posZ) + 0.301, floor(
                            Companion.mc.player.posZ) + 0.699)
                )
                packets = 0
            } else if (Companion.mc.player.ticksExisted % delay === 0) {
                Companion.mc.player.setPosition(
                    Companion.mc.player.posX + MathHelper.clamp(
                        (roundToClosest(
                            Companion.mc.player.posX,
                            floor(Companion.mc.player.posX) + 0.241,
                            floor(Companion.mc.player.posX) + 0.759
                        ) - Companion.mc.player.posX) as Double,
                        -0.03, 0.03
                    ), Companion.mc.player.posY, Companion.mc.player.posZ + MathHelper.clamp(
                        (roundToClosest(
                            Companion.mc.player.posZ,
                            floor(Companion.mc.player.posZ) + 0.241,
                            floor(Companion.mc.player.posZ) + 0.759
                        ) - Companion.mc.player.posZ) as Double,
                        -0.03, 0.03
                    )
                )
                Companion.mc.player.connection.sendPacket(
                    CPacketPlayer.Position(
                        Companion.mc.player.posX,
                        Companion.mc.player.posY,
                        Companion.mc.player.posZ,
                        true
                    ) as Packet<*>
                )
                Companion.mc.player.connection.sendPacket(
                    CPacketPlayer.Position(
                        roundToClosest(
                            Companion.mc.player.posX, floor(Companion.mc.player.posX) + 0.23, floor(
                                Companion.mc.player.posX) + 0.77),
                        Companion.mc.player.posY,
                        roundToClosest(
                            Companion.mc.player.posZ, floor(Companion.mc.player.posZ) + 0.23, floor(
                                Companion.mc.player.posZ) + 0.77),
                        true
                    ) as Packet<*>
                )
                ++packets
            }
        }

    }


    val info: String
        get() = packets.toString()

    private fun roundToClosest(num: Double, low: Double, high: Double): Double {
        val d2 = high - num
        val d1 = num - low
        if (d2 > d1) {
            return low
        }
        return high
    }
}