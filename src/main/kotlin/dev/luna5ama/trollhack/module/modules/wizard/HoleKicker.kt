package dev.luna5ama.trollhack.module.modules.wizard

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.inventory.slot.allSlotsPrioritized
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.inventory.slot.firstItem
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.world.PlacementSearchOption
import dev.luna5ama.trollhack.util.world.getPlacement
import dev.luna5ama.trollhack.util.world.placeBlock
import net.minecraft.block.BlockPistonBase
import net.minecraft.block.BlockPistonExtension
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.pow

internal object HoleKicker : Module(
    name = "HoleKicker",
    category = Category.WIZARD,
    description = "Kicks players out of holes using pistons",
    modulePriority = 100
) {
    private val range by setting("Range", 6, 1..8, 1)
    private val disable by setting("Auto Disable", true)
    private val rotate by setting("Rotate", true)
    private val placeDelay by setting("Place Delay", 100, 0..500, 50)

    private var one = 0
    private var two = 0
    private var three = 0
    private var four = 0
    private var target: EntityPlayer? = null
    private var lastPlaceTime = 0L

    init {

        onDisable {
            resetState()
        }

        safeListener<TickEvent.Pre> {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPlaceTime < placeDelay) return@safeListener

            if (!hasRequiredItems()) {
                NoSpamMessage.sendError("$chatName Missing required items (piston & redstone block)")
                if (disable) disable()
                return@safeListener
            }

            target = findTarget(range.toDouble()) ?: return@safeListener
            val pos = BlockPos(target!!.posX, target!!.posY, target!!.posZ)
            val angle = calculateAngleToPos(pos)

            handlePositionBasedOnAngle(pos, angle)

            if (disable && (one >= 4 || two >= 4 || three >= 4 || four >= 4)) {
                disable()
            }

            lastPlaceTime = currentTime
        }
    }

    private fun resetState() {
        one = 0
        two = 0
        three = 0
        four = 0
        target = null
    }

    private fun SafeClientEvent.hasRequiredItems(): Boolean {
        return findPistonSlot() != null && findRedstoneBlockSlot() != null
    }

    private fun SafeClientEvent.findPistonSlot() = player.allSlotsPrioritized.firstBlock<BlockPistonBase>()

    private fun SafeClientEvent.findRedstoneBlockSlot() = player.allSlotsPrioritized.firstBlock(Blocks.REDSTONE_BLOCK)

    private fun SafeClientEvent.findTarget(range: Double): EntityPlayer? {
        return world.playerEntities
            .filter { it != player && it.isEntityAlive && player.getDistanceSq(it) <= range.pow(2) }
            .minByOrNull { player.getDistanceSq(it) }
    }

    private fun SafeClientEvent.calculateAngleToPos(pos: BlockPos): FloatArray {
        val eyesPos = Vec3d(
            player.posX,
            player.posY + player.getEyeHeight(),
            player.posZ
        )

        val targetVec = Vec3d(
            pos.x + 0.5,
            pos.y + 0.5,
            pos.z + 0.5
        )

        val diff = targetVec.subtract(eyesPos)
        val distance = diff.length()

        val yaw = Math.toDegrees(atan2(diff.z, diff.x)).toFloat() - 90.0f
        val pitch = (-Math.toDegrees(asin(diff.y / distance))).toFloat()

        return floatArrayOf(
            MathHelper.wrapDegrees(yaw),
            MathHelper.wrapDegrees(pitch)
        )
    }

    private fun SafeClientEvent.handlePositionBasedOnAngle(pos: BlockPos, angle: FloatArray) {
        val yaw = angle[0]
        val pitch = angle[1]

        when {
            abs(yaw) < 45 && pitch in -30f..30f -> handleDirection(pos, 0, 1, 1, 0, 2, 1, 1, 1, 1, 0, 1, 2, -1, 1, 1)
            (yaw > 135 || yaw < -135) && pitch in -30f..30f -> handleDirection(pos, 0, 1, -1, 0, 2, -1, 1, 1, -1, 0, 1, -2, -1, 1, -1)
            yaw in -135f..-45f && pitch in -30f..30f -> handleDirection(pos, 1, 1, 0, 1, 2, 0, 1, 1, 1, 2, 1, 0, 1, 1, -1)
            yaw in 45f..135f && pitch in -30f..30f -> handleDirection(pos, -1, 1, 0, -1, 2, 0, -1, 1, 1, -2, 1, 0, -1, 1, -1)
            disable -> disable()
        }
    }

    private fun SafeClientEvent.handleDirection(
        basePos: BlockPos,
        pistonX: Int, pistonY: Int, pistonZ: Int,
        redstone1X: Int, redstone1Y: Int, redstone1Z: Int,
        redstone2X: Int, redstone2Y: Int, redstone2Z: Int,
        redstone3X: Int, redstone3Y: Int, redstone3Z: Int,
        redstone4X: Int, redstone4Y: Int, redstone4Z: Int
    ) {
        val pistonPos = basePos.add(pistonX, pistonY, pistonZ)
        if (!isPlaceable(pistonPos)) return

        // Place piston first
        findPistonSlot()?.let { pistonSlot ->
            val pistonPlaceInfo = getPlacement(
                pistonPos,
                PlacementSearchOption.range(5.0),
                PlacementSearchOption.ENTITY_COLLISION,
                if (rotate) PlacementSearchOption.VISIBLE_SIDE else null
            ) ?: return

            ghostSwitch(pistonSlot) {
                placeBlock(pistonPlaceInfo)
            }

            // Then try to place redstone in one of the positions
            val redstonePositions = listOf(
                basePos.add(redstone1X, redstone1Y, redstone1Z),
                basePos.add(redstone2X, redstone2Y, redstone2Z),
                basePos.add(redstone3X, redstone3Y, redstone3Z),
                basePos.add(redstone4X, redstone4Y, redstone4Z)
            )

            findRedstoneBlockSlot()?.let { redstoneSlot ->
                redstonePositions.firstOrNull { isPlaceable(it) }?.let { redstonePos ->
                    val redstonePlaceInfo = getPlacement(
                        redstonePos,
                        PlacementSearchOption.range(5.0),
                        PlacementSearchOption.ENTITY_COLLISION,
                        if (rotate) PlacementSearchOption.VISIBLE_SIDE else null
                    ) ?: return

                    ghostSwitch(redstoneSlot) {
                        placeBlock(redstonePlaceInfo)
                    }
                }
            }
        }

        // Increment counter based on direction
        when {
            pistonX == 0 && pistonZ == 1 -> one++
            pistonX == 0 && pistonZ == -1 -> two++
            pistonX == 1 && pistonZ == 0 -> three++
            pistonX == -1 && pistonZ == 0 -> four++
        }
    }

    private fun SafeClientEvent.isPlaceable(pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        return state.block.isReplaceable(world, pos) ||
                state.block is BlockPistonBase ||
                state.block is BlockPistonExtension
    }

}