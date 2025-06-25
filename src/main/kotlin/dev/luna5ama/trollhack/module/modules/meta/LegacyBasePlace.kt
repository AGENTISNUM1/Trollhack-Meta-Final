package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.mask.EnumFacingMask
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.spoofSneak
import dev.luna5ama.trollhack.util.accessor.renderPosX
import dev.luna5ama.trollhack.util.accessor.renderPosY
import dev.luna5ama.trollhack.util.accessor.renderPosZ
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.ConcurrentScope
import dev.luna5ama.trollhack.util.threads.isActiveOrFalse
import dev.luna5ama.trollhack.util.threads.runSafeSuspend
import dev.luna5ama.trollhack.util.world.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

@CombatManager.CombatModule
internal object LegacyBasePlace : Module(
    name = "LegacyBasePlace",
    category = Category.META,
    description = "old bedbaseplace",
    modulePriority = 68
) {
    private val autoDisable by setting("Auto Disable", true)
    private val placeSpeed by setting("Places Per Tick", 4f, 0.25f..5f, 0.25f)
    private val updateInterval by setting("Update Interval", 5, 1..10, 1, description = "Time in seconds between updates")

    private val renderColor by setting("Render Color", ColorRGB(255, 160, 255), false)
    private val renderAlpha by setting("Render Alpha", 128, 0..255, 1)

    private var job: Job? = null
    private var blocksPlacedThisTick = 0
    private var renderPositions = emptyList<BlockPos>()
    private var lastUpdateTime = 0L

    override fun isActive(): Boolean {
        return isEnabled && job.isActiveOrFalse
    }

    init {
        onDisable {
            job?.cancel()
            job = null
            blocksPlacedThisTick = 0
            renderPositions = emptyList()
            lastUpdateTime = 0L
        }
        safeListener<TickEvent.Post> {
            blocksPlacedThisTick = 0
            if (!job.isActiveOrFalse && canRun()) {
                job = runBedBasePlace()
            }
            if (job.isActiveOrFalse && Bypass.blockPlaceRotation) {
                sendPlayerPacket {
                    cancelAll()
                }
            }
        }
        safeListener<Render3DEvent> {
            renderPositions.forEach { pos ->
                renderBlock(pos)
            }
        }
    }

    private fun SafeClientEvent.canRun(): Boolean {
        val target = CombatManager.target ?: return false
        val targetPos = target.flooredPosition
        if (player.getDistanceSq(targetPos) > 7 * 7) return false
        return EnumFacing.HORIZONTALS.any { world.getBlockState(targetPos.down().offset(it)).isReplaceable }
    }

    private fun SafeClientEvent.getObby(): HotbarSlot? {
        val slots = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
        if (slots == null) {
            NoSpamMessage.sendMessage("$chatName No obsidian in hotbar, disabling!")
            disable()
            return null
        }
        return slots
    }

    private fun SafeClientEvent.runBedBasePlace() = ConcurrentScope.launch {
        while (isEnabled) {
            val target = CombatManager.target ?: break
            val targetPos = target.flooredPosition
            val surroundingBlocks = EnumFacing.HORIZONTALS.map { targetPos.down().offset(it) }
            val validBlocks = surroundingBlocks.filter { pos ->
                world.getBlockState(pos).isReplaceable && EntityManager.checkNoEntityCollision(AxisAlignedBB(pos))
            }

            if (validBlocks.isEmpty()) break
            val placePos = validBlocks.first()
            renderPositions = listOf(placePos)
            val placeInfo = getPlacement(
                placePos,
                3,
                PlacementSearchOption.range(5f),
                PlacementSearchOption.ENTITY_COLLISION
            ) ?: break
            val slot = getObby() ?: break
            runSafeSuspend {
                doPlace(placeInfo, slot)
            }
            blocksPlacedThisTick++
            if (isSurroundedByAir(placePos)) {
                val closestSide = getClosestSideToTarget(placePos, targetPos)
                val secondPlacePos = placePos.offset(closestSide)
                val secondPlaceInfo = getPlacement(
                    secondPlacePos,
                    3,
                    PlacementSearchOption.range(5f),
                    PlacementSearchOption.ENTITY_COLLISION
                ) ?: break
                runSafeSuspend {
                    doPlace(secondPlaceInfo, slot)
                }
                blocksPlacedThisTick++
            }
            delay(updateInterval * 1000L)
        }
        if (autoDisable) disable()
    }

    private suspend fun SafeClientEvent.doPlace(
        placeInfo: PlaceInfo,
        slot: HotbarSlot
    ) {
        val placePacket = placeInfo.toPlacePacket(EnumHand.MAIN_HAND)
        val needRotation = Bypass.blockPlaceRotation

        if (needRotation) {
            val rotation = getRotationTo(placeInfo.hitVec)
            val rotationPacket = CPacketPlayer.PositionRotation(
                player.posX,
                player.posY,
                player.posZ,
                rotation.x,
                rotation.y,
                player.onGround
            )
            connection.sendPacket(rotationPacket)
        }

        player.spoofSneak {
            ghostSwitch(slot) {
                connection.sendPacket(placePacket)
            }
        }

        player.swingArm(EnumHand.MAIN_HAND)
    }

    private fun SafeClientEvent.isSurroundedByAir(pos: BlockPos): Boolean {
        return EnumFacing.HORIZONTALS.all { world.isAirBlock(pos.offset(it)) }
    }

    private fun getClosestSideToTarget(placePos: BlockPos, targetPos: BlockPos): EnumFacing {
        val deltaX = targetPos.x - placePos.x
        val deltaZ = targetPos.z - placePos.z

        return when {
            deltaX > 0 -> EnumFacing.EAST
            deltaX < 0 -> EnumFacing.WEST
            deltaZ > 0 -> EnumFacing.SOUTH
            else -> EnumFacing.NORTH
        }
    }

    private fun renderBlock(pos: BlockPos) {
        val renderer = ESPRenderer()
        renderer.aFilled = renderAlpha
        renderer.aOutline = renderAlpha

        val box = AxisAlignedBB(pos)
        renderer.add(box, renderColor, EnumFacingMask.ALL)

        GlStateManager.pushMatrix()
        RenderUtils3D.setTranslation(-mc.renderManager.renderPosX, -mc.renderManager.renderPosY, -mc.renderManager.renderPosZ)
        renderer.render(false)
        GlStateManager.popMatrix()
    }
}