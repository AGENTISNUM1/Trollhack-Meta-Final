package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Notification
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.combat.CalcContext
import dev.luna5ama.trollhack.util.combat.CrystalDamage
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.combat.CrystalUtils.hasValidSpaceForCrystal
import dev.luna5ama.trollhack.util.inventory.blockBlacklist
import dev.luna5ama.trollhack.util.inventory.slot.allSlots
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.inventory.slot.hasItem
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.VectorUtils
import dev.luna5ama.trollhack.util.math.vector.distanceToCenter
import dev.luna5ama.trollhack.util.math.vector.toVec3d
import dev.luna5ama.trollhack.util.threads.ConcurrentScope
import dev.luna5ama.trollhack.util.threads.onMainThread
import dev.luna5ama.trollhack.util.threads.runSafe
import dev.luna5ama.trollhack.util.world.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.block.BlockBed
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.max

@CombatManager.CombatModule
internal object CrystalBasePlace : Module(
    name = "CrystalBasePlace",
    description = "Places obby for placing crystal on",
    category = Category.COMBAT,
    modulePriority = 90
) {
    private val manualPlaceBind by setting("Place Bind", Bind())
    private val minDamage by setting("Min Damage", 8.0f, 0.0f..20.0f, 0.5f)
    private val maxSelfDamage by setting("Max Self Damage", 8.0f, 0.0f..20.0f, 0.5f)
    private val range by setting("Range", 4.0f, 0.0f..8.0f, 0.5f)
    private val delay by setting("Delay", 20, 0..50, 5)

    private val timer = TickTimer(TimeUnit.TICKS)
    private val renderer = ESPRenderer().apply { aFilled = 33; aOutline = 233 }
    private var rotationTo: Vec3d? = null

    init {
        onDisable {
            rotationTo = null
        }

        listener<Render3DEvent> {
            renderer.render(false) // Don't auto-clear, only clear when disabled
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            rotationTo?.let { hitVec ->
                sendPlayerPacket {
                    rotate(getRotationTo(hitVec))
                }
            }
        }
    }

    fun SafeClientEvent.prePlace(minDamage: Float) {
        if (!timer.tick(delay)) return

        val slot = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN) ?: return
        val eyePos = player.eyePosition
        val posList = VectorUtils.getBlockPosInSphere(eyePos, range)
            .filter { isValidBasePos(it)}
            .filter { world.isPlaceable(it) }
            .toList()

        ConcurrentScope.launch {
            val placeInfo = getPlaceInfo(eyePos, posList, minDamage)

            if (placeInfo != null) {
                rotationTo = placeInfo.hitVec
                renderer.replaceAll(
                    mutableListOf(
                        ESPRenderer.Info(
                            AxisAlignedBB(placeInfo.placedPos),
                            ColorRGB(255, 255, 255)
                        )
                    )
                )
                timer.reset()

                delay(50)
                onMainThread {
                    placeBlock(placeInfo, slot)
                    Notification.send(CrystalBasePlace, "$chatName Obsidian placed")
                }
            }
        }
    }

    private fun SafeClientEvent.getPlaceInfo(eyePos: Vec3d, posList: List<BlockPos>, minDamage: Float): PlaceInfo? {
        val contextSelf = CombatManager.contextSelf ?: return null
        val contextTarget = CombatManager.contextTarget ?: return null

        val mutableBlockPos = BlockPos.MutableBlockPos()
        val cacheList = PriorityQueue<CrystalDamage>(compareByDescending { it.targetDamage })

        for (pos in posList) {
            if (!hasNeighbor(pos)) continue

            val crystalPos = pos.toVec3d(0.5, 1.0, 0.5)
            if (!contextSelf.checkColliding(crystalPos)) continue
            if (!contextTarget.checkColliding(crystalPos)) continue

            val crystalDamage = calculateDamage(contextSelf, contextTarget, eyePos, crystalPos, pos, mutableBlockPos)
            if (crystalDamage.selfDamage > maxSelfDamage || crystalDamage.targetDamage < minDamage) continue

            cacheList.add(crystalDamage)
        }

        var current = cacheList.poll()
        while (current != null) {
            val neighbor = getPlacement(
                current.blockPos,
                1,
                PlacementSearchOption.ENTITY_COLLISION,
                PlacementSearchOption.range(5.0)
            )
            if (neighbor != null) {
                return neighbor
            }
            current = cacheList.poll()
        }

        return null
    }
    private fun SafeClientEvent.isValidBasePos(basePos: BlockPos): Boolean {
        return world.getBlockState(basePos).isSideSolid(world, basePos, EnumFacing.UP)
    }

    private class CalcInfo(
        val side: EnumFacing,
        val hitVec: Vec3d,
        val basePosFoot: BlockPos,
        val basePosHead: BlockPos,
        val bedPosFoot: BlockPos,
        val bedPosHead: BlockPos,
    )

    private fun SafeClientEvent.isValidBedPos(ignoreNonFullBox: Boolean, calcInfo: CalcInfo): Boolean {
        val headState = world.getBlockState(calcInfo.bedPosHead)
        val footState = world.getBlockState(calcInfo.bedPosFoot)

        val headBlock = headState.block
        val footBlock = footState.block

        return (checkBedBlock(ignoreNonFullBox, calcInfo.bedPosFoot, footState) || footBlock == Blocks.BED)
                && (checkBedBlock(ignoreNonFullBox, calcInfo.bedPosHead, headState)
                || headBlock == Blocks.BED
                && headState.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD
                && headState.getValue(BlockBed.FACING) == calcInfo.side)
    }
    private fun checkBedBlock(
        ignoreNonFullBox: Boolean,
        pos: BlockPos,
        state: IBlockState,
    ): Boolean {
        val block = state.block
        return block == Blocks.AIR
                || PacketMine.isInstantMining(pos)
                || ignoreNonFullBox && !blockBlacklist.contains(block) && block != Blocks.BED && !state.isFullBox
    }
    private fun calculateDamage(
        contextSelf: CalcContext,
        contextTarget: CalcContext?,
        eyePos: Vec3d,
        crystalPos: Vec3d,
        pos: BlockPos,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): CrystalDamage {
        val function = FastRayTraceFunction { rayTracePos, blockState ->
            when {
                rayTracePos == pos -> {
                    FastRayTraceAction.HIT
                }
                blockState.block != Blocks.AIR && CrystalUtils.isResistant(blockState) -> {
                    FastRayTraceAction.CALC
                }
                else -> {
                    FastRayTraceAction.SKIP
                }
            }
        }

        val selfDamage = max(
            contextSelf.calcDamage(crystalPos, true, mutableBlockPos, function),
            contextSelf.calcDamage(crystalPos, false, mutableBlockPos, function)
        )
        val targetDamage = contextTarget?.calcDamage(crystalPos, true, mutableBlockPos, function) ?: 0.0f

        return CrystalDamage(
            crystalPos,
            pos,
            selfDamage,
            targetDamage,
            eyePos.distanceTo(crystalPos),
            contextSelf.currentPos.distanceTo(crystalPos)
        )
    }
}