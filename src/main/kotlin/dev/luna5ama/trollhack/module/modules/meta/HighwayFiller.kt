package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.util.EntityUtils.spoofSneak
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
import net.minecraft.block.Block.getBlockFromItem
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

internal object HighwayFiller : Module(
    name = "Highway Filler",
    category = Category.META,
    description = "NOOOO AAAA NOO"
) {
    private val rotate by setting("Rotate", true)
    private val placeDelay by setting("Delay", 100, 0..500, 10)
    private val wallHeight by setting("Height", 3, 1..5, 1)
    private val wallWidth by setting("Width", 3, 1..6, 1)
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

    override fun isActive(): Boolean {
        return isEnabled && job.isActiveOrFalse
    }

    init {
        onDisable {
            placedBlocks.clear()
        }

        safeListener<TickEvent.Post> {
            if (!job.isActiveOrFalse && hasObsidian()) {
                job = runClogger()
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

    private fun SafeClientEvent.runClogger() = ConcurrentScope.launch {
        val player = player ?: return@launch

        val yaw = player.rotationYaw
        val rad = Math.toRadians(yaw.toDouble())
        val forwardX = -sin(rad)
        val forwardZ = cos(rad)
        val forward = Vec3d(forwardX, 0.0, forwardZ)
        val right = Vec3d(-forwardZ, 0.0, forwardX)

        val centerX = player.posX + forwardX * 2
        val centerZ = player.posZ + forwardZ * 2
        val horizontalOffset = (wallWidth - 1) / 2.0
        val baseX = centerX - right.x * horizontalOffset
        val baseZ = centerZ - right.z * horizontalOffset
        val baseY = player.posY.toInt() - 1

        var placedBlock = false

        for (j in 0 until wallHeight) {
            if (placedBlock) break
            for (i in 0 until wallWidth) {
                if (placedBlock) break

                val posX = baseX + right.x * i
                val posZ = baseZ + right.z * i
                val targetPos = BlockPos(posX, (baseY + j).toDouble(), posZ)

                if (!world.isPlaceable(targetPos)) continue

                val placeInfo = getPlacement(
                    targetPos,
                    3,
                    PlacementSearchOption.range(4.25f),
                    PlacementSearchOption.ENTITY_COLLISION,
                    PlacementSearchOption.VISIBLE_SIDE.takeIf { rotate }
                ) ?: continue

                runSafeSuspend {
                    placeBlock(placeInfo)
                }

                placedBlock = true
                placedBlocks[targetPos] = System.currentTimeMillis()
                delay(placeDelay.toLong())
            }
        }
    }

    private suspend fun SafeClientEvent.placeBlock(placeInfo: PlaceInfo) {
        val player = player ?: return
        val slot = getObbySlot() ?: run {
            NoSpamMessage.sendMessage("$chatName No obsidian in hotbar, disabling!")
            disable()
            return
        }

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
            if (!(player.heldItemMainhand.item is net.minecraft.item.ItemBlock
                        && getBlockFromItem(player.heldItemMainhand.item) == Blocks.OBSIDIAN)) {
                ghostSwitch(slot) {
                    connection.sendPacket(placePacket)
                }
            } else {
                connection.sendPacket(placePacket)
            }
        }

        player.swingArm(EnumHand.MAIN_HAND)
    }

    private fun SafeClientEvent.getObbySlot(): HotbarSlot? {
        return player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
    }

    private fun SafeClientEvent.hasObsidian(): Boolean {
        return player.hotbarSlots.any { slot ->
            slot.stack.item is net.minecraft.item.ItemBlock
                    && getBlockFromItem(slot.stack.item) == Blocks.OBSIDIAN
        }
    }
}