package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import net.minecraft.entity.item.EntityEnderPearl
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumHand
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2
import kotlin.math.sqrt

internal object AutoChase : Module(
    name = "PearlChase",
    description = "Automatically throws ender pearls to chase players",
    category = Category.META,
    modulePriority = 200
) {
    private val replicate = true
    private val range by setting("Range", 50.0f, 5.0f..100.0f, 5.0f)
    private val dotThreshold = 0f

    private val processedPearlIds = HashSet<Int>()
    private var originalYaw = 0f
    private var originalPitch = 0f

    init {
        onDisable {
            processedPearlIds.clear()
        }
        safeListener<TickEvent.Post> {
            if (!replicate) return@safeListener

            for (entity in world.loadedEntityList) {
                if (entity !is EntityEnderPearl || processedPearlIds.contains(entity.entityId)) continue

                val pearl = entity
                var detected = false

                for (other in world.playerEntities) {
                    if (other == player || FriendManager.friends.contains(other.name)) continue

                    val eyePos = other.getPositionEyes(1.0f)
                    val pearlPos = Vec3d(pearl.posX, pearl.posY, pearl.posZ)
                    val toPearl = pearlPos.subtract(eyePos)
                    val distance = toPearl.length()

                    if (distance > range) continue

                    val lookVec = other.lookVec
                    val dot = toPearl.normalize().dotProduct(lookVec)

                    if (dot < dotThreshold) continue

                    detected = true
                    break
                }

                if (!detected) continue

                val rotations = computeRotationsFromVelocity(pearl) ?: continue
                replicatePearl(rotations)
                processedPearlIds.add(pearl.entityId)
            }
        }
    }

    private fun computeRotationsFromVelocity(pearl: EntityEnderPearl): FloatArray? {
        val vx = pearl.motionX
        val vy = pearl.motionY
        val vz = pearl.motionZ
        val horiz = sqrt(vx * vx + vz * vz)

        if (horiz == 0.0) return null

        val yaw = (Math.toDegrees(atan2(vz, vx)) - 90.0).toFloat()
        val pitch = (-Math.toDegrees(atan2(vy, horiz))).toFloat()
        return floatArrayOf(yaw, pitch)
    }

    private fun SafeClientEvent.replicatePearl(rotations: FloatArray) {
        originalYaw = player.rotationYaw
        originalPitch = player.rotationPitch

        val pearlSlot = player.hotbarSlots.firstItem(Items.ENDER_PEARL)
        val offhand = player.heldItemOffhand.item == Items.ENDER_PEARL

        if (pearlSlot == null && !offhand) return

        val oldSlot = player.inventory.currentItem

        if (!offhand) {
            ghostSwitch(HotbarSwitchManager.Override.NONE, pearlSlot!!) {
                connection.sendPacket(
                    CPacketPlayer.Rotation(
                        rotations[0],
                        rotations[1],
                        player.onGround
                    )
                )

                playerController.processRightClick(
                    player,
                    world,
                    EnumHand.MAIN_HAND
                )
            }
        } else {
            connection.sendPacket(
                CPacketPlayer.Rotation(
                    rotations[0],
                    rotations[1],
                    player.onGround
                )
            )

            playerController.processRightClick(
                player,
                world,
                EnumHand.OFF_HAND
            )
        }

        player.rotationYaw = originalYaw
        player.rotationPitch = originalPitch

    }
}