package dev.luna5ama.trollhack.module.modules.combat

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
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.combat.AutoTrap.renderColor
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.spoofSneak
import dev.luna5ama.trollhack.util.inventory.slot.HotbarSlot
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.vector.toBlockPos
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.ConcurrentScope
import dev.luna5ama.trollhack.util.threads.isActiveOrFalse
import dev.luna5ama.trollhack.util.threads.runSafeSuspend
import dev.luna5ama.trollhack.util.world.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

@CombatManager.CombatModule
internal object AutoTrap : Module(
    name = "Auto Trap",
    category = Category.COMBAT,
    description = "Traps your enemies in obsidian",
    modulePriority = 150
) {
    private val trapMode by setting("Trap Mode", TrapMode.FULL_TRAP)
    private val bindSelfTrap by setting("Bind Self Trap", Bind(), {
        if (it) {
            selfTrap = true
            enable()
        }
    })
    private val autoDisable by setting("Auto Disable", true)
    private val strictDirection by setting("Strict Direction", false)
    private val antistep by setting("AntiStep", true)
    private val placeSpeed by setting("Places Per Tick", 4f, 1f..12f, 1f)
    private val delay by setting("Delay", 50.0f, 10.0f..100.0f, 5f)
    private val render by setting("Render", true)
    private val renderColor by setting("Render Color", ColorRGB(255, 0, 0), false, { render })
    private val renderFade by setting("Render Fade", true, { render })
    private val renderTime by setting("Render Time", 2000, 500..5000, 100, { render })

    private val renderer = ESPRenderer().apply {
        aFilled = 31
        aOutline = 233
    }
    private val placedBlocks = LinkedHashMap<BlockPos, Long>()

    private var selfTrap = false
    private var job: Job? = null

    override fun isActive(): Boolean {
        return isEnabled && job.isActiveOrFalse
    }

    init {
        onDisable {
            selfTrap = false
            placedBlocks.clear()
        }

        safeListener<TickEvent.Post> {
            if (!job.isActiveOrFalse && canRun()) job = runAutoTrap()

            if (job.isActiveOrFalse && Bypass.blockPlaceRotation) {
                sendPlayerPacket {
                    cancelAll()
                }
            }

            // Clean up old renders
            if (renderFade) {
                val currentTime = System.currentTimeMillis()
                placedBlocks.entries.removeAll { currentTime - it.value > renderTime }
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

    private fun SafeClientEvent.canRun(): Boolean {
        (if (selfTrap) player else CombatManager.target)?.positionVector?.toBlockPos()?.let {
            for (offset in trapMode.offset) {
                if (!world.isPlaceable(it.add(offset))) continue
                return true
            }
        }
        return false
    }

    private fun SafeClientEvent.getObby(): HotbarSlot? {
        val slots = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)

        if (slots == null) { // Obsidian check
            NoSpamMessage.sendMessage("$chatName No obsidian in hotbar, disabling!")
            disable()
            return null
        }

        return slots
    }

    private fun SafeClientEvent.runAutoTrap() = ConcurrentScope.launch {
        val entity = if (selfTrap) player else CombatManager.target ?: return@launch

        val emptySet = emptySet<BlockPos>()
        val placed = HashSet<BlockPos>()
        var placeCount = 0
        var lastInfo = getStructurePlaceInfo(
            entity.flooredPosition,
            trapMode.offset,
            emptySet, 3,
            4.25f,
            strictDirection
        )

        while (lastInfo != null) {
            if (!(isEnabled && CombatManager.isOnTopPriority(AutoTrap))) break

            val placingInfo =
                getStructurePlaceInfo(entity.flooredPosition, trapMode.offset, placed, 3, 4.25f, strictDirection)
                    ?: lastInfo

            placeCount++
            placed.add(placingInfo.placedPos)
            placedBlocks[placingInfo.placedPos] = System.currentTimeMillis()
            val slot = getObby() ?: break

            runSafeSuspend {
                doPlace(placingInfo, slot)
            }

            if (placeCount >= 4) {
                placeCount = 0
                placed.clear()
            }

            lastInfo =
                getStructurePlaceInfo(entity.flooredPosition, trapMode.offset, emptySet, 3, 4.25f, strictDirection)
        }

        if (autoDisable) disable()
    }

    private fun SafeClientEvent.getStructurePlaceInfo(
        center: BlockPos,
        structureOffset: Array<BlockPos>,
        toIgnore: Set<BlockPos>,
        attempts: Int,
        range: Float,
        visibleSideCheck: Boolean
    ): PlaceInfo? {
        for (offset in structureOffset) {
            val pos = center.add(offset)
            if (toIgnore.contains(pos)) continue
            if (!world.getBlockState(pos).isReplaceable) continue
            if (!EntityManager.checkNoEntityCollision(AxisAlignedBB(pos))) continue

            return getPlacement(
                pos,
                attempts,
                PlacementSearchOption.range(range),
                PlacementSearchOption.ENTITY_COLLISION,
                PlacementSearchOption.VISIBLE_SIDE.takeIf { visibleSideCheck }
            ) ?: continue
        }

        if (attempts > 1) return getStructurePlaceInfo(
            center,
            structureOffset,
            toIgnore,
            attempts - 1,
            range,
            visibleSideCheck
        )

        return null
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
            delay((delay / placeSpeed).toLong())
        }

        player.spoofSneak {
            ghostSwitch(slot) {
                connection.sendPacket(placePacket)
            }
        }

        player.swingArm(EnumHand.MAIN_HAND)
        if (needRotation) {
            delay((delay / placeSpeed).toLong())
        } else {
            delay((delay / placeSpeed).toLong())
        }
    }

    @Suppress("UNUSED")
    private enum class TrapMode(val offset: Array<BlockPos>) {
        FULL_TRAP(
            arrayOf(
                BlockPos(1, 0, 0),
                BlockPos(-1, 0, 0),
                BlockPos(0, 0, 1),
                BlockPos(0, 0, -1),
                BlockPos(1, 1, 0),
                BlockPos(-1, 1, 0),
                BlockPos(0, 1, 1),
                BlockPos(0, 1, -1),
                BlockPos(0, 2, 0),
                BlockPos(0, 3, 0)
            )
        ),
        CRYSTAL_TRAP(
            arrayOf(
                BlockPos(1, 1, 1),
                BlockPos(1, 1, 0),
                BlockPos(1, 1, -1),
                BlockPos(0, 1, -1),
                BlockPos(-1, 1, -1),
                BlockPos(-1, 1, 0),
                BlockPos(-1, 1, 1),
                BlockPos(0, 1, 1),
                BlockPos(0, 2, 0),
                BlockPos(0, 3, 0)
            )
        )
    }
}