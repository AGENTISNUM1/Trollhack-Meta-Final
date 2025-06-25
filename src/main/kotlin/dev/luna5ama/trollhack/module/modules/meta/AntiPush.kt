package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.manager.managers.TimerManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.spoofSneak
import dev.luna5ama.trollhack.util.MovementUtils.realSpeed
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.ConcurrentScope
import dev.luna5ama.trollhack.util.threads.isActiveOrFalse
import dev.luna5ama.trollhack.util.world.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import java.util.*

@CombatManager.CombatModule
internal object AntiPush : Module(
    name = "Anti Push",
    category = Category.META,
    description = "Protects against piston traps",
    modulePriority = 150
) {
    private val rotate by setting("Rotate", true)
    private val packet by setting("Packet", true)
    private val maxSelfSpeed by setting("Max Self Speed", 6.0f, 1.0f..30.0f, 1.0f)
    private val helper by setting("Helper", true)
    private val trap by setting("Trap", true)
    private val onlyBurrow by setting("Only Burrow", true, { trap })
    private val whenDouble by setting("When Double", true, { onlyBurrow })
    private val render by setting("Render", true)
    private val renderColor by setting("Render Color", ColorRGB(255, 0, 0), false, { render })
    private val renderFade by setting("Render Fade", true, { render })
    private val renderTime by setting("Render Time", 2000, 500..5000, 100, { render })

    private val renderer = ESPRenderer().apply {
        aFilled = 31
        aOutline = 233
    }
    private val placedBlocks = LinkedHashMap<BlockPos, Long>()
    private var job: Job? = null
    private val speedList = ArrayDeque<Double>()
    private var applyTimer = true
    private val averageSpeedTime = 5

    override fun isActive(): Boolean {
        return isEnabled && job.isActiveOrFalse
    }

    init {
        onDisable {
            placedBlocks.clear()
            speedList.clear()
        }

        safeListener<TickEvent.Post> {
            updateSpeedList()
            if (!job.isActiveOrFalse && canRun()) {
                job = runProtection()
            }

            if (job.isActiveOrFalse && Bypass.blockPlaceRotation) {
                sendPlayerPacket {
                    cancelAll()
                }
            }
        }

        safeListener<Render3DEvent> {
            if (render) {
                renderer.aFilled = if (renderFade) 15 else 31
                renderer.aOutline = if (renderFade) 100 else 233

                placedBlocks.keys.forEach { pos ->
                    val box = AxisAlignedBB(pos)
                    if (renderFade) {
                        val timeLeft = renderTime - (System.currentTimeMillis() - placedBlocks[pos]!!)
                        val alpha = (timeLeft.toFloat() / renderTime * 255).toInt().coerceIn(0, 255)
                        renderer.add(box, renderColor.alpha(alpha))
                    } else {
                        renderer.add(box, renderColor)
                    }
                }
                renderer.render(true)
            }
        }
    }

    private fun SafeClientEvent.updateSpeedList() {
        val tps = if (applyTimer) 1000.0 / TimerManager.tickLength else 20.0
        val speed = player.realSpeed * tps

        if (speed > 0.0 || player.ticksExisted % 4 == 0) {
            speedList.add(speed) // Only adding it every 4 ticks if speed is 0
        } else {
            speedList.pollFirst()
        }

        while (speedList.size > averageSpeedTime) speedList.pollFirst()
    }

    private fun SafeClientEvent.getCurrentSpeed(): Double {
        return if (speedList.isEmpty()) 0.0 else speedList.average()
    }

    private fun SafeClientEvent.canRun(): Boolean {
        return player.onGround &&
                CombatManager.isOnTopPriority(this@AntiPush) &&
                getCurrentSpeed() <= maxSelfSpeed
    }

    private fun SafeClientEvent.runProtection() = ConcurrentScope.launch {
        val pos = player.flooredPosition
        if (world.getBlockState(pos.up(2)).block == Blocks.OBSIDIAN ||
            world.getBlockState(pos.up(2)).block == Blocks.BEDROCK) return@launch

        var progress = 0
        if (whenDouble) {
            for (facing in EnumFacing.VALUES) {
                if (facing == EnumFacing.DOWN || facing == EnumFacing.UP) continue

                val blockPos = pos.offset(facing).up()
                val blockState = world.getBlockState(blockPos)
                if (blockState.block !is net.minecraft.block.BlockPistonBase) continue

                val pistonFacing = blockState.getValue(net.minecraft.block.BlockDirectional.FACING)
                if (pistonFacing.opposite != facing) continue

                progress++
            }
        }

        for (facing in EnumFacing.VALUES) {
            if (facing == EnumFacing.DOWN || facing == EnumFacing.UP) continue

            val blockPos = pos.offset(facing).up()
            val blockState = world.getBlockState(blockPos)
            if (blockState.block !is net.minecraft.block.BlockPistonBase) continue

            val pistonFacing = blockState.getValue(net.minecraft.block.BlockDirectional.FACING)
            if (pistonFacing.opposite != facing) continue

            placeBlock(pos.up().offset(facing.opposite))
            placedBlocks[pos.up().offset(facing.opposite)] = System.currentTimeMillis()

            if (trap && (world.isAirBlock(pos) || !onlyBurrow || progress >= 2)) {
                placeBlock(pos.up(2))
                placedBlocks[pos.up(2)] = System.currentTimeMillis()

                if (!world.isPlaceable(pos.up(2))) {
                    for (facing2 in EnumFacing.VALUES) {
                        val alternatePos = pos.offset(facing2).up(2)
                        if (!canPlace(alternatePos)) continue

                        placeBlock(alternatePos)
                        placedBlocks[alternatePos] = System.currentTimeMillis()
                        break
                    }
                }
            }

            if (world.isPlaceable(pos.up().offset(facing.opposite)) || !helper) continue

            val helperPos1 = pos.offset(facing.opposite)
            if (world.isPlaceable(helperPos1)) {
                placeBlock(helperPos1)
                placedBlocks[helperPos1] = System.currentTimeMillis()
                continue
            }

            val helperPos2 = pos.offset(facing.opposite).down()
            placeBlock(helperPos2)
            placedBlocks[helperPos2] = System.currentTimeMillis()
        }
    }

    private suspend fun SafeClientEvent.placeBlock(pos: BlockPos) {
        if (!canPlace(pos)) return

        val slot = getObbySlot() ?: run {
            NoSpamMessage.sendMessage("$chatName No obsidian in hotbar, disabling!")
            disable()
            return
        }

        val placeInfo = getPlacement(
            pos,
            3,
            PlacementSearchOption.range(4.25f),
            PlacementSearchOption.ENTITY_COLLISION,
            PlacementSearchOption.VISIBLE_SIDE.takeIf { rotate }
        ) ?: return

        val placePacket = placeInfo.toPlacePacket(EnumHand.MAIN_HAND)

        if (rotate) {
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
            delay(10)
        }

        player.spoofSneak {
            ghostSwitch(slot) {
                connection.sendPacket(placePacket)
            }
        }

        player.swingArm(EnumHand.MAIN_HAND)
    }

    private fun SafeClientEvent.getObbySlot(): HotbarSlot? {
        return player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
    }

    private fun SafeClientEvent.canPlace(pos: BlockPos): Boolean {
        return world.isPlaceable(pos) &&
                EntityManager.checkNoEntityCollision(AxisAlignedBB(pos))
    }
}