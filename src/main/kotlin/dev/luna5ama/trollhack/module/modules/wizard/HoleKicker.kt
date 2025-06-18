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
    category = Category.META,
    description = "Kicks players out of holes using pistons",
    modulePriority = 100
) {
    private val range by setting("Range", 8.0f, 1.0f..12.0f, 0.5f)
    private val disable by setting("Auto Disable", true)
    private val rotate by setting("Rotate", true)
    private val debug by setting("Debug", false)
    private val breakCrystal by setting("Break Crystal", true)

    private var one = 0
    private var two = 0
    private var three = 0
    private var four = 0
    private var target: EntityPlayer? = null

    init {
        onEnable {
            if (breakCrystal) breakCrystals()
        }

        onDisable {
            resetState()
        }

        safeListener<TickEvent.Pre> {
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
        }
    }

    private fun resetState() {
        one = 0
        two = 0
        three = 0
        four = 0
        target = null
    }

    private fun breakCrystals() {
        mc.world.loadedEntityList
            .filterIsInstance<EntityEnderCrystal>()
            .filter { !it.isDead && mc.player.getDistance(it) <= 4.0f }
            .sortedBy { mc.player.getDistance(it) }
            .forEach { crystal ->
                mc.connection?.sendPacket(CPacketUseEntity(crystal))
            }
    }

    private fun SafeClientEvent.hasRequiredItems(): Boolean {
        return player.allSlotsPrioritized.firstBlock<BlockPistonBase>() != null &&
                player.allSlotsPrioritized.firstBlock(Blocks.REDSTONE_BLOCK) != null
    }

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
            pitch in -71f..71f && yaw in -51f..51f -> handleDirection(
                pos,
                pos.add(0, 1, 1),  // Piston position
                listOf(
                    pos.add(0, 2, 1),  // Redstone positions
                    pos.add(1, 1, 1),
                    pos.add(0, 1, 2),
                    pos.add(-1, 1, 1)
                ),
                { one++ }
            )
            pitch in -71f..71f && (yaw >= 129f || yaw <= -129f) -> handleDirection(
                pos,
                pos.add(0, 1, -1),
                listOf(
                    pos.add(0, 2, -1),
                    pos.add(1, 1, -1),
                    pos.add(0, 1, -2),
                    pos.add(-1, 1, -1)
                ),
                { two++ }
            )
            pitch in -71f..71f && yaw in -129f..-51f -> handleDirection(
                pos,
                pos.add(1, 1, 0),
                listOf(
                    pos.add(1, 2, 0),
                    pos.add(1, 1, 1),
                    pos.add(2, 1, 0),
                    pos.add(1, 1, -1)
                ),
                { three++ }
            )
            pitch in -71f..71f && yaw in 51f..129f -> handleDirection(
                pos,
                pos.add(-1, 1, 0),
                listOf(
                    pos.add(-1, 2, 0),
                    pos.add(-1, 1, 1),
                    pos.add(-2, 1, 0),
                    pos.add(-1, 1, -1)
                ),
                { four++ }
            )
            disable -> disable()
        }
    }

    private fun SafeClientEvent.handleDirection(
        targetPos: BlockPos,
        pistonPos: BlockPos,
        redstonePositions: List<BlockPos>,
        counterIncrement: () -> Unit
    ) {
        val pistonState = world.getBlockState(pistonPos)
        if (pistonState.block !is BlockPistonBase && !pistonState.block.isReplaceable(world, pistonPos)) {
            return
        }

        // Place piston
        player.allSlotsPrioritized.firstBlock<BlockPistonBase>()?.let { pistonSlot ->
            val pistonPlacement = getPlacement(
                pistonPos,
                PlacementSearchOption.range(5.0),
                PlacementSearchOption.ENTITY_COLLISION,
                if (rotate) PlacementSearchOption.VISIBLE_SIDE else null
            ) ?: return@let

            ghostSwitch(pistonSlot) {
                placeBlock(pistonPlacement)
            }

            // Place redstone
            player.allSlotsPrioritized.firstBlock(Blocks.REDSTONE_BLOCK)?.let { redstoneSlot ->
                redstonePositions.firstOrNull { redstonePos ->
                    val state = world.getBlockState(redstonePos)
                    state.block.isReplaceable(world, redstonePos) || state.block is BlockPistonBase
                }?.let { redstonePos ->
                    val redstonePlacement = getPlacement(
                        redstonePos,
                        PlacementSearchOption.range(5.0),
                        PlacementSearchOption.ENTITY_COLLISION,
                        if (rotate) PlacementSearchOption.VISIBLE_SIDE else null
                    ) ?: return@let

                    ghostSwitch(redstoneSlot) {
                        placeBlock(redstonePlacement)
                    }
                }
            }
        }

        counterIncrement()
    }
}