package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.collection.CircularArray
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.RunGameLoopEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.combat.CombatEvent
import dev.luna5ama.trollhack.event.events.player.OnUpdateWalkingPlayerEvent
import dev.luna5ama.trollhack.event.events.render.Render2DEvent
import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.ESPRenderer
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.ProjectionUtils
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.graphics.mask.EnumFacingMask
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Notification
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager.ghostSwitch
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.module.modules.meta.LegacyBasePlace
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.EntityUtils.eyePosition
import dev.luna5ama.trollhack.util.EntityUtils.spoofUnSneak
import dev.luna5ama.trollhack.util.accessor.renderPosX
import dev.luna5ama.trollhack.util.accessor.renderPosY
import dev.luna5ama.trollhack.util.accessor.renderPosZ
import dev.luna5ama.trollhack.util.and
import dev.luna5ama.trollhack.util.atValue
import dev.luna5ama.trollhack.util.collections.averageOrZero
import dev.luna5ama.trollhack.util.combat.CalcContext
import dev.luna5ama.trollhack.util.combat.CombatUtils.scaledHealth
import dev.luna5ama.trollhack.util.combat.CrystalUtils
import dev.luna5ama.trollhack.util.extension.rootName
import dev.luna5ama.trollhack.util.inventory.InventoryTask
import dev.luna5ama.trollhack.util.inventory.blockBlacklist
import dev.luna5ama.trollhack.util.inventory.executedOrTrue
import dev.luna5ama.trollhack.util.inventory.inventoryTask
import dev.luna5ama.trollhack.util.inventory.operation.swapWith
import dev.luna5ama.trollhack.util.inventory.slot.*
import dev.luna5ama.trollhack.util.math.RotationUtils
import dev.luna5ama.trollhack.util.math.RotationUtils.getRotationTo
import dev.luna5ama.trollhack.util.math.RotationUtils.yaw
import dev.luna5ama.trollhack.util.math.VectorUtils
import dev.luna5ama.trollhack.util.math.vector.*
import dev.luna5ama.trollhack.util.pause.OffhandPause
import dev.luna5ama.trollhack.util.pause.withPause
import dev.luna5ama.trollhack.util.threads.ConcurrentScope
import dev.luna5ama.trollhack.util.threads.TimerScope
import dev.luna5ama.trollhack.util.threads.onMainThread
import dev.luna5ama.trollhack.util.threads.runSafe
import dev.luna5ama.trollhack.util.world.*
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import kotlinx.coroutines.launch
import net.minecraft.block.BlockBed
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.Slot
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.server.SPacketBlockChange
import net.minecraft.network.play.server.SPacketSoundEffect
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

@CombatManager.CombatModule
internal object BedAura : Module(
    name = "BedAura",
    description = "Place bed and kills enemies",
    category = Category.COMBAT,
    modulePriority = 70
) {
    private val page = setting("Page", Page.GENERAL)

    // General Settings
    private val handMode by setting("Hand Mode", EnumHand.OFF_HAND, page.atValue(Page.GENERAL))
    private val autoEat by setting("Enable AutoEat", false, page.atValue(Page.GENERAL))
    private val antifire by setting("AntiFire", false, page.atValue(Page.GENERAL))
    private val autoPlaceStationary by setting("BasePlace", false, page.atValue(Page.GENERAL))
    private val stationaryTime by setting("BasePlace Delay", 1.0f, 0.5f..5.0f, 0.1f, page.atValue(Page.GENERAL) and { autoPlaceStationary })
    var enablerotationPitch by setting("Enable Pitch", false, page.atValue(Page.GENERAL))
    private val rotationPitch by setting("Rotation Pitch", 90, -90..90, 1, page.atValue(Page.GENERAL) and { enablerotationPitch })
    var ghostSwitchBypass by setting(
        "Ghost Switch Bypass",
        HotbarSwitchManager.Override.NONE,
        page.atValue(Page.GENERAL) and ::handMode.atValue(EnumHand.MAIN_HAND)
    )
    private val bedSlot by setting(
        "Bed Slot",
        3,
        1..9,
        1,
        page.atValue(Page.GENERAL) and { handMode == EnumHand.MAIN_HAND })
    private val assumeInstantMine by setting("Assume Instant Mine", true, page.atValue(Page.GENERAL))
    private val antiBlocker by setting("Anti Blocker", true, page.atValue(Page.GENERAL))
    private val antiBlockerSwitch by setting(
        "Anti Blocker Switch",
        200,
        0..500,
        10,
        page.atValue(Page.GENERAL) and ::antiBlocker
    )
    private val strictDirection by setting("Strict Direction", false, page.atValue(Page.GENERAL))
    val newPlacement by setting("1.13 Placement", false, page.atValue(Page.GENERAL))
    var smartDamage by setting("Smart Damage", true, page.atValue(Page.GENERAL))
    val damageStep by setting(
        "Damage Step",
        2.0f,
        0.0f..5.0f,
        0.1f,
        page.atValue(Page.GENERAL) and ::smartDamage
    )
    var noSuicide by setting("No Suicide", 8.0f, 0.0f..20.0f, 0.25f, page.atValue(Page.GENERAL))
    var minDamage by setting("Min Damage", 6.0f, 0.0f..20.0f, 0.25f, page.atValue(Page.GENERAL))
    var maxSelfDamage by setting("Max Self Damage", 6.0f, 0.0f..20.0f, 0.25f, page.atValue(Page.GENERAL))
    var damageBalance by setting("Damage Balance", -2.5f, -10.0f..10.0f, 0.25f, page.atValue(Page.GENERAL))
    var range by setting("Range", 5.4f, 0.0f..6.0f, 0.25f, page.atValue(Page.GENERAL))

    // Base Place Settings
    private val basePlaceRange by setting("BasePlace Range", 4.0f, 0.0f..8.0f, 0.5f, page.atValue(Page.GENERAL))
    private val basePlaceDelay by setting("BasePlace Delay", 20, 0..50, 5, page.atValue(Page.GENERAL))
    private val basePlaceColor by setting("BasePlace Color", ColorRGB(32, 255, 32), false, page.atValue(Page.GENERAL))
    val oldbaseplace by setting("Use old baseplace", false, page.atValue(Page.GENERAL))

    // Timing Settings
    var updateDelay by setting("Update Delay", 50, 5..250, 1, page.atValue(Page.TIMING))
    var autoSetSpeed by setting("Auto", false, page.atValue(Page.TIMING))
    var timingMode by setting("Timing Mode", TimingMode.INSTANT, page.atValue(Page.TIMING) and  { !autoSetSpeed })
    var delay by setting(
        "Delay",
        75,
        0..1000,
        1,
        page.atValue(Page.TIMING) and { autoSetSpeed || timingMode != TimingMode.SWITCH }
    )
    var placeDelay by setting(
        "Place Delay",
        25,
        0..1000,
        1,
        page.atValue(Page.TIMING) and { autoSetSpeed || timingMode == TimingMode.SWITCH }
    )
    var breakDelay by setting(
        "Break Delay",
        50,
        0..1000,
        1,
        page.atValue(Page.TIMING) and { autoSetSpeed || timingMode == TimingMode.SWITCH }
    )

    // Force Place Settings
    private val forcePlaceBind by setting("Force Place Bind", Bind(), {
        if (isEnabled && it) {
            toggleForcePlace = !toggleForcePlace
            Notification.send(BedAura, "$chatName Force placing" + if (toggleForcePlace) " §aenabled" else " §cdisabled")
        }
    }, page.atValue(Page.FORCE_PLACE))
    private val forcePlaceHealth by setting(
        "Force Place Health",
        8.0f,
        0.0f..20.0f,
        0.5f,
        page.atValue(Page.FORCE_PLACE)
    )
    private val forcePlaceMinDamage by setting(
        "Force Place Min Damage",
        1.5f,
        0.0f..10.0f,
        0.25f,
        page.atValue(Page.FORCE_PLACE)
    )
    private val forcePlaceDamageBalance by setting(
        "Force Place Damage Balance",
        0.0f,
        -10.0f..10.0f,
        0.25f,
        page.atValue(Page.FORCE_PLACE)
    )

    // Motion Detect Settings
    private val motionDetect by setting("Motion Detect", true, page.atValue(Page.MOTION_DETECT))
    private val targetMotion by setting(
        "Target Motion",
        0.15f,
        0.0f..0.3f,
        0.01f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val selfMotion by setting(
        "Self Motion",
        0.22f,
        0.0f..0.3f,
        0.01f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionMinDamage by setting(
        "Motion Min Damage",
        3.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionMaxSelfDamage by setting(
        "Motion Max Self Damage",
        8.0f,
        0.0f..20.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })
    private val motionDamageBalance by setting(
        "Motion Damage Balance",
        -5.0f,
        -10.0f..10.0f,
        0.25f,
        page.atValue(Page.MOTION_DETECT) and { motionDetect })

    // Render Settings
    private val joinedRenders by setting("Joined Renders", false, page.atValue(Page.RENDER))
    private val renderBase by setting("Render Base", true, page.atValue(Page.RENDER))
    private val renderDamage by setting("Render Damage", true, page.atValue(Page.RENDER))
    private val joinedColor by setting(
        "Joined Color",
        ColorRGB(255, 160, 255),
        false,
        page.atValue(Page.RENDER) and { joinedRenders })
    private val renderFoot by setting("Render Foot", true, page.atValue(Page.RENDER) and { !joinedRenders })
    private val renderHead by setting("Render Head", true, page.atValue(Page.RENDER) and { !joinedRenders })
    private val footColor by setting(
        "Foot Color",
        ColorRGB(255, 160, 255),
        false,
        page.atValue(Page.RENDER) and { !joinedRenders && renderFoot }
    )
    private val headColor by setting(
        "Head Color",
        ColorRGB(255, 32, 64),
        false,
        page.atValue(Page.RENDER) and { !joinedRenders && renderHead }
    )
    private val baseColor by setting(
        "Base Color",
        ColorRGB(32, 255, 32),
        false,
        page.atValue(Page.RENDER) and ::renderBase
    )
    private val rotateLength by setting("Rotate Length", 250, 0..1000, 50, page.atValue(Page.RENDER))
    private val movingLength by setting("Moving Length", 500, 0..1000, 50, page.atValue(Page.RENDER))
    private val fadeLength by setting("Fade Length", 250, 0..1000, 50, page.atValue(Page.RENDER))

    private enum class Page {
        GENERAL, TIMING, FORCE_PLACE, MOTION_DETECT, RENDER
    }

    enum class TimingMode {
        INSTANT, SWITCH
    }

    private val updateTimer = TickTimer()
    private val timer = TickTimer()
    private val basePlaceTimer = TickTimer()
    private val blockerExists = AtomicBoolean(true)
    private val blockerSwitch = AtomicBoolean(false)
    private val blockerTimer = TickTimer()

    private var lastTargetPos: Vec3d? = null
    private var stationaryStartTime = 0L

    private var switchPlacing = false
    private var placeInfo: PlaceInfo? = null
    private var selfMoving = false
    private var targetMoving = false
    private var toggleForcePlace = false
    private var shouldForcePlace = false
    private var lastDamage = 0.0f
    private var lastTask: InventoryTask? = null

    private val explosionTimer = TickTimer()
    private val explosionCountArray = CircularArray<Int>(8)
    private var explosionCount = 0

    private var inactiveTicks = 10
    var needOffhandBed = false; private set

    // Base Place Renderer
    private val basePlaceRenderer = ESPRenderer().apply {
        aFilled = 33
        aOutline = 233
    }
    private var basePlaceRotationTo: Vec3d? = null
    private var lastBasePlacePos: BlockPos? = null

    private val function = FastRayTraceFunction { pos, blockState ->
        val block = blockState.block
        if (block == Blocks.AIR
            || block == Blocks.BED
            || assumeInstantMine && PacketMine.isInstantMining(pos)
            || !CrystalUtils.isResistant(blockState)
        ) {
            FastRayTraceAction.SKIP
        } else {
            FastRayTraceAction.CALC
        }
    }

    override fun isActive(): Boolean {
        return isEnabled && inactiveTicks < 10
    }

    override fun getHudInfo(): String {
        return "%.1f".format(explosionCountArray.averageOrZero() * 4.0)
    }

    init {
        onEnable {
        }
        onDisable {
            reset()
            if (oldbaseplace && LegacyBasePlace.isEnabled) {
                LegacyBasePlace.disable()
            }
        }

        safeListener<PacketEvent.Receive>(114514) {
            val target = CombatManager.target ?: return@safeListener
            when (val packet = it.packet) {
                is SPacketSoundEffect -> {
                    val placeInfo = placeInfo ?: return@safeListener
                    if (packet.category != SoundCategory.BLOCKS) return@safeListener
                    if (packet.sound != SoundEvents.ENTITY_GENERIC_EXPLODE) return@safeListener
                    if (placeInfo.center.distanceSqTo(packet.x, packet.y, packet.z) > 4.0) return@safeListener

                    explosionCount++
                }
                is SPacketBlockChange -> {
                    val targetPos = target.betterPosition
                    if (packet.blockPosition != targetPos) return@safeListener
                    if (CrystalUtils.isResistant(packet.blockState)) return@safeListener

                    blockerExists.set(true)
                }
            }
        }

        listener<Render3DEvent> {
            Renderer.onRender3D()
            basePlaceRenderer.render(false)
        }

        safeListener<Render2DEvent.Absolute> {
            Renderer.onRender2D()
        }

        listener<EntityEvent.UpdateHealth> {
            if (it.entity == CombatManager.target) {
                val diff = it.prevHealth - it.health
                if (diff > 0.0f) {
                    lastDamage += diff
                }
            }
        }

        listener<CombatEvent.UpdateTarget> {
            lastDamage = 0.0f
        }

        safeListener<OnUpdateWalkingPlayerEvent.Pre> {
            placeInfo?.let {
                val rotation = if (Bypass.blockPlaceRotation) {
                    getRotationTo(it.hitVec)
                } else {
                    Vec2f(it.direction.yaw, if (enablerotationPitch) rotationPitch.toFloat() else player.rotationPitch)
                }

                sendPlayerPacket {
                    rotate(rotation)
                }
            }

            basePlaceRotationTo?.let { hitVec ->
                sendPlayerPacket {
                    rotate(getRotationTo(hitVec))
                }
            }
        }
        safeListener<TickEvent.Pre> {
            if (oldbaseplace && BedAura.isEnabled) {
                LegacyBasePlace.enable()
            }
        }
        safeListener<TickEvent.Post> {
            inactiveTicks++
            update()
            runLoop()
            checkBasePlace()
        }

        safeListener<RunGameLoopEvent.Tick> {
            if (explosionTimer.tickAndReset(250L)) {
                val count = explosionCount
                explosionCount = 0
                explosionCountArray.add(count)
            }

            update()
            runLoop()
            checkBasePlace()
        }

        TimerScope.launchLooping(rootName, 5L) {
            if (isEnabled) {
                runSafe {
                    runLoop()
                    checkBasePlace()
                }
            }
        }
    }

    private fun SafeClientEvent.checkBasePlace() {
        if ((autoPlaceStationary && System.currentTimeMillis() - stationaryStartTime >= stationaryTime * 1000L)) {
            prePlaceBase()
        }
    }

    private fun SafeClientEvent.prePlaceBase() {
        if (!basePlaceTimer.tick(basePlaceDelay)) return
        if (!player.allSlots.hasItem(Items.BED)) return

        val slot = player.hotbarSlots.firstBlock(Blocks.OBSIDIAN) ?: return
        val eyePos = player.eyePosition

        ConcurrentScope.launch {
            val placeInfo = findBestBasePlacement(eyePos) ?: return@launch

            onMainThread {
                placeBaseBlock(placeInfo, slot)
                lastBasePlacePos = placeInfo.placedPos
                basePlaceRenderer.add(ESPRenderer.Info(
                    AxisAlignedBB(placeInfo.placedPos),
                    basePlaceColor
                ))
                Notification.send(BedAura, "$chatName Base placed")
                stationaryStartTime = System.currentTimeMillis()
            }
            basePlaceTimer.reset()
        }
    }

    private fun SafeClientEvent.findBestBasePlacement(eyePos: Vec3d): BasePlaceInfo? {
        val target = CombatManager.target ?: return null
        val targetPos = target.betterPosition

        return VectorUtils.getBlockPosInSphere(eyePos, basePlaceRange)
            .filter { isValidBasePos(it) }
            .filter { world.isPlaceable(it) }
            .filter { hasNeighbor(it) }
            .minByOrNull { it.distanceTo(targetPos) }
            ?.let { pos ->
                val hitVec = pos.toVec3d(0.5, 1.0, 0.5)
                basePlaceRotationTo = hitVec
                BasePlaceInfo(pos, hitVec)
            }
    }

    private fun SafeClientEvent.isValidBasePos(basePos: BlockPos): Boolean {
        return world.getBlockState(basePos).isSideSolid(world, basePos, EnumFacing.UP)
    }

    private fun SafeClientEvent.placeBaseBlock(placeInfo: BasePlaceInfo, slot: HotbarSlot) {
        val hitVec = placeInfo.hitVec
        val placedPos = placeInfo.placedPos

        placeBlock2(
            slot,
            placeInfo.placedPos,
            EnumFacing.UP,
            placeInfo.hitVec,
            swingHand = true,
            rotate = false
        )
    }

    private data class BasePlaceInfo(
        val placedPos: BlockPos,
        val hitVec: Vec3d
    )
    fun SafeClientEvent.placeBlock2(
        slot: Slot,
        placedPos: BlockPos,
        direction: EnumFacing,
        hitVec: Vec3d,
        swingHand: Boolean = true,
        rotate: Boolean = false
    ) {
        val hitVecOffset = getHitVecOffset(direction)

        val placeInfo = PlaceInfo(
            placedPos,
            direction,
            player.eyePosition.distanceTo(hitVec),
            hitVecOffset,
            hitVec,
            placedPos.offset(direction.opposite)
        )

        placeBlock(placeInfo, slot)

        if (swingHand) {
            val hand = if (slot == player.offhandSlot) EnumHand.OFF_HAND else EnumHand.MAIN_HAND
            player.swingArm(hand)
        }
    }
    private fun SafeClientEvent.runLoop() {
        val placeInfo = placeInfo
        if (placeInfo == null || CombatSetting.pause) {
            needOffhandBed = false
            return
        } else {
            needOffhandBed = handMode == EnumHand.OFF_HAND
        }

        if (handMode == EnumHand.MAIN_HAND) {
            if (!lastTask.executedOrTrue) return
            val hotbarSlot = player.hotbarSlots[bedSlot - 1]
            if (hotbarSlot.stack.item != Items.BED) {
                refillBed(hotbarSlot)
                return
            }
        } else {
            if (player.heldItemOffhand.item != Items.BED) return
        }

        val validDamage = !smartDamage || shouldForcePlace || placeInfo.targetDamage - lastDamage >= damageStep

        val isTargetFalling = (CombatManager.target?.motionY ?: 0.0) != 0.0
        when {
            autoSetSpeed && isTargetFalling -> switchTiming(placeInfo, validDamage)
            else -> instantTiming(placeInfo, validDamage)
        }
    }

    private fun SafeClientEvent.refillBed(hotbarSlot: HotbarSlot) {
        (player.storageSlots.firstItem(Items.BED)
            ?: player.craftingSlots.firstItem(Items.BED))?.let {
            lastTask = inventoryTask {
                swapWith(it, hotbarSlot)
            }
        }
    }

    private fun SafeClientEvent.instantTiming(placeInfo: PlaceInfo, validDamage: Boolean) {
        if (validDamage) {
            if (timer.tick(delay)) {
                placeBed(placeInfo)
                breakBed(placeInfo)
            }
        } else {
            breakIfPlaced(placeInfo, delay)
        }
    }

    private fun SafeClientEvent.switchTiming(placeInfo: PlaceInfo, validDamage: Boolean) {
        if (validDamage) {
            if (switchPlacing) {
                if (timer.tick(placeDelay)) {
                    breakBed(placeInfo)
                    switchPlacing = !switchPlacing
                }
            } else {
                if (timer.tick(breakDelay)) {
                    placeBed(placeInfo)
                    switchPlacing = !switchPlacing
                }
            }
        } else {
            breakIfPlaced(placeInfo, breakDelay)
        }
    }

    private fun SafeClientEvent.breakIfPlaced(placeInfo: PlaceInfo, delay: Int) {
        if (timer.tick(delay)) {
            if (isBedPlaced(placeInfo)) {
                breakBed(placeInfo)
            }
        }
    }

    private fun SafeClientEvent.isBedPlaced(placeInfo: PlaceInfo): Boolean {
        return world.getBlock(placeInfo.bedPosFoot) == Blocks.BED
                || world.getBlock(placeInfo.bedPosHead) == Blocks.BED
    }

    private fun SafeClientEvent.breakBed(placeInfo: PlaceInfo) {
        val side = getMiningSide(placeInfo.bedPosFoot) ?: EnumFacing.UP
        val hitVecOffset = getHitVecOffset(side)

        player.spoofUnSneak {
            connection.sendPacket(
                CPacketPlayerTryUseItemOnBlock(
                    placeInfo.bedPosFoot,
                    side,
                    handMode,
                    hitVecOffset.x,
                    hitVecOffset.y,
                    hitVecOffset.z
                )
            )
        }
        connection.sendPacket(CPacketAnimation(handMode))

        blockerExists.set(false)
        timer.reset()
        inactiveTicks = 0
    }

    private fun SafeClientEvent.placeBed(placeInfo: PlaceInfo) {
        val shouldSneak = !player.isSneaking
        if (shouldSneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING))

        if (player.heldItemOffhand.item == Items.BED) {
            val placePacket = CPacketPlayerTryUseItemOnBlock(placeInfo.basePos, EnumFacing.UP, EnumHand.OFF_HAND, 0.5f, 1.0f, 0.5f)
            OffhandPause.withPause(BedAura) {
                connection.sendPacket(placePacket)
            }
        } else {
            val placePacket = CPacketPlayerTryUseItemOnBlock(placeInfo.basePos, EnumFacing.UP, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f)
            ghostSwitch(ghostSwitchBypass, bedSlot - 1) {
                connection.sendPacket(placePacket)
            }
        }

        connection.sendPacket(CPacketAnimation(handMode))
        if (shouldSneak) connection.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING))

        CombatManager.target?.let { player.setLastAttackedEntity(it) }
        timer.reset()
        inactiveTicks = 0
    }

    private fun SafeClientEvent.update() {
        if (player.dimension == 0 || !player.allSlotsPrioritized.hasItem(Items.BED)) {
            reset()
        } else if (updateTimer.tickAndReset(updateDelay)) {
            ConcurrentScope.launch {
                placeInfo = calcPlaceInfo()
            }
        }

        // Track target position for automatic placement
        CombatManager.target?.let { target ->
            val currentPos = target.positionVector
            if (currentPos != lastTargetPos) {
                lastTargetPos = currentPos
                stationaryStartTime = System.currentTimeMillis()
            } else if (autoPlaceStationary && System.currentTimeMillis() - stationaryStartTime >= stationaryTime * 1000L) {
                prePlaceBase()
                stationaryStartTime = System.currentTimeMillis()
            }
        }

        selfMoving = motionDetect && CombatManager.trackerSelf?.let { it.motion.length() > selfMotion } ?: false
        targetMoving = motionDetect && CombatManager.trackerTarget?.let { it.motion.length() > targetMotion } ?: false

        shouldForcePlace = CombatManager.target?.let {
            it.health <= forcePlaceHealth
        } ?: false

        CombatManager.target?.let {
            if (System.currentTimeMillis() - CombatManager.getHurtTime(it) > 500L) {
                lastDamage = 0.0f
            }
        }
    }

    private fun SafeClientEvent.calcPlaceInfo(): PlaceInfo? {
        val contextSelf = CombatManager.contextSelf ?: return null
        val contextTarget = CombatManager.contextTarget ?: return null
        val eyePos = player.eyePosition
        val map = Long2ObjectOpenHashMap<Vec2f>()
        val mutableBlockPos = BlockPos.MutableBlockPos()

        var ignoreNonFullBox = false
        if (antiBlocker) {
            if (blockerTimer.tickAndReset(antiBlockerSwitch)) {
                ignoreNonFullBox = blockerSwitch.getAndSet(!blockerSwitch.get())
            }
            ignoreNonFullBox = antiBlocker && !blockerExists.get()
        }

        return VectorUtils.getBlockPosInSphere(eyePos, range)
            .filter { !strictDirection || eyePos.y > it.y + 1.0 }
            .mapToCalcInfo(eyePos)
            .filterNot { contextTarget.entity.distanceSqToCenter(it.bedPosHead) > 100.0 }
            .filter { isValidBasePos(it.basePosFoot) && (newPlacement || isValidBasePos(it.basePosHead)) }
            .filter { isValidBedPos(ignoreNonFullBox, it) }
            .mapNotNull { checkDamage(map, contextSelf, contextTarget, it, mutableBlockPos) }
            .maxWithOrNull(
                compareBy<DamageInfo> { it.targetDamage }
                    .thenByDescending { eyePos.distanceSqToCenter(it.basePos) }
            )
            ?.toPlaceInfo()
    }

    private fun Sequence<BlockPos>.mapToCalcInfo(eyePos: Vec3d): Sequence<CalcInfo> {
        return if (Bypass.blockPlaceRotation) {
            map {
                val bedPos = it.up()
                val hitVec = it.toVec3d(0.5, 1.0, 0.5)
                val side = calcDirection(eyePos, hitVec)

                CalcInfo(
                    side,
                    hitVec,
                    it,
                    it.offset(side),
                    bedPos,
                    bedPos.offset(side)
                )
            }
        } else {
            flatMap {
                val hitVec = it.toVec3d(0.5, 1.0, 0.5)

                sequenceOf(
                    newCalcInfo(EnumFacing.NORTH, it, hitVec),
                    newCalcInfo(EnumFacing.SOUTH, it, hitVec),
                    newCalcInfo(EnumFacing.WEST, it, hitVec),
                    newCalcInfo(EnumFacing.EAST, it, hitVec)
                )
            }
        }
    }

    private fun calcDirection(eyePos: Vec3d, hitVec: Vec3d): EnumFacing {
        val x = hitVec.x - eyePos.x
        val z = hitVec.z - eyePos.z

        return EnumFacing.HORIZONTALS.maxByOrNull {
            x * it.directionVec.x + z * it.directionVec.z
        } ?: EnumFacing.NORTH
    }

    private fun newCalcInfo(side: EnumFacing, pos: BlockPos, hitVec: Vec3d): CalcInfo {
        val bedPos = pos.up()

        return CalcInfo(
            side,
            hitVec,
            pos,
            pos.offset(side),
            bedPos,
            bedPos.offset(side)
        )
    }

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
                || assumeInstantMine && PacketMine.isInstantMining(pos)
                || ignoreNonFullBox && !blockBlacklist.contains(block) && block != Blocks.BED && !state.isFullBox
    }

    private fun checkDamage(
        map: Long2ObjectOpenHashMap<Vec2f>,
        contextSelf: CalcContext,
        contextTarget: CalcContext,
        calcInfo: CalcInfo,
        mutableBlockPos: BlockPos.MutableBlockPos
    ): DamageInfo? {
        val scaledHealth = contextSelf.entity.scaledHealth
        val (targetDamage, selfDamage) = map.computeIfAbsent(calcInfo.basePosHead.toLong()) {
            val center = calcInfo.bedPosHead.toVec3dCenter()
            Vec2f(
                contextTarget.calcDamage(center, true, 5.0f, mutableBlockPos, function),
                max(
                    contextSelf.calcDamage(center, false, 5.0f, mutableBlockPos, function),
                    contextSelf.calcDamage(center, true, 5.0f, mutableBlockPos, function)
                )
            )
        }

        val diff = targetDamage - selfDamage
        return if (scaledHealth - selfDamage > noSuicide
            && checkSelfDamage(selfDamage)
            && (checkDamage(targetDamage, diff)
                    || checkForcePlaceDamage(targetDamage, diff))
        ) {
            DamageInfo(
                calcInfo.side,
                calcInfo.hitVec,
                calcInfo.basePosFoot,
                calcInfo.bedPosFoot,
                calcInfo.bedPosHead,
                targetDamage,
                selfDamage
            )
        } else {
            null
        }
    }

    private fun checkSelfDamage(selfDamage: Float): Boolean {
        return selfMoving && selfDamage <= motionMaxSelfDamage || selfDamage <= maxSelfDamage
    }

    private fun checkDamage(targetDamage: Float, diff: Float): Boolean {
        return (targetMoving && targetDamage >= motionMinDamage || targetDamage >= minDamage)
                && (targetMoving && diff >= motionDamageBalance || diff >= damageBalance)
    }

    private fun checkForcePlaceDamage(targetDamage: Float, diff: Float): Boolean {
        return (toggleForcePlace || shouldForcePlace) && targetDamage >= forcePlaceMinDamage && diff >= forcePlaceDamageBalance
    }

    private fun DamageInfo.toPlaceInfo(): PlaceInfo {
        val directionVec = side.directionVec
        val center = Vec3d(
            bedPosFoot.x + 0.5 + directionVec.x * 0.5,
            bedPosFoot.y.toDouble(),
            bedPosFoot.z + 0.5 + directionVec.z * 0.5
        )

        return PlaceInfo(
            side,
            hitVec,
            basePos,
            bedPosFoot,
            bedPosHead,
            targetDamage,
            selfDamage,
            center,
            "${"%.1f".format(targetDamage)}/${"%.1f".format(selfDamage)}"
        )
    }

    private fun reset() {
        blockerExists.set(true)

        updateTimer.reset(-69420L)
        timer.reset(-69420L)
        basePlaceTimer.reset(-69420L)

        switchPlacing = false
        placeInfo = null
        selfMoving = false
        targetMoving = false
        toggleForcePlace = false
        shouldForcePlace = false
        lastDamage = 0.0f
        lastTask = null
        lastBasePlacePos = null
        basePlaceRotationTo = null
        basePlaceRenderer.clear()

        inactiveTicks = 10
        needOffhandBed = false

        Renderer.reset()
    }

    private class CalcInfo(
        val side: EnumFacing,
        val hitVec: Vec3d,
        val basePosFoot: BlockPos,
        val basePosHead: BlockPos,
        val bedPosFoot: BlockPos,
        val bedPosHead: BlockPos,
    )

    private class DamageInfo(
        val side: EnumFacing,
        val hitVec: Vec3d,
        val basePos: BlockPos,
        val bedPosFoot: BlockPos,
        val bedPosHead: BlockPos,
        val targetDamage: Float,
        val selfDamage: Float
    )

    private class PlaceInfo(
        val direction: EnumFacing,
        val hitVec: Vec3d,
        val basePos: BlockPos,
        val bedPosFoot: BlockPos,
        val bedPosHead: BlockPos,
        val targetDamage: Float,
        val selfDamage: Float,
        val center: Vec3d,
        val string: String
    )

    private object Renderer {
        var lastBedPlacement: Pair<BlockPos, EnumFacing>? = null

        var lastRotation = Float.NaN
        var currentRotation = Float.NaN

        var lastPos: Vec3d? = null
        var currentPos: Vec3d? = null

        var lastRenderRotation = Float.NaN
        var lastRenderPos: Vec3d? = null

        var lastUpdateTime = 0L
        var startTime = 0L
        var scale = 0.0f
        var lastDamageString = ""

        val boxBase = AxisAlignedBB(
            -0.5, -0.4375, -1.0,
            0.5, 0.0, 1.0,
        )
        val boxFoot = AxisAlignedBB(
            -0.5, 0.0, -1.0,
            0.5, 0.5625, 0.0,
        )
        val boxHead = AxisAlignedBB(
            -0.5, 0.0, 0.0,
            0.5, 0.5625, 1.0,
        )

        fun reset() {
            lastBedPlacement = null

            lastRotation = Float.NaN
            currentRotation = Float.NaN

            lastPos = null
            currentPos = null

            lastRenderRotation = Float.NaN
            lastRenderPos = null

            lastUpdateTime = 0L
            startTime = 0L
            scale = 0.0f
            lastDamageString = ""
        }

        fun onRender3D() {
            val flag = renderBase || renderDamage
            if (flag || renderDamage) {
                val placeInfo = BedAura.placeInfo
                update(placeInfo)

                if (flag) {
                    val lastPos = lastPos ?: return
                    val currentPos = currentPos ?: return
                    val lastRotation = lastRotation
                    val currentRotation = currentRotation
                    if (lastRotation.isNaN() || currentRotation.isNaN()) return

                    val rotateMul = Easing.OUT_CUBIC.inc(Easing.toDelta(lastUpdateTime, rotateLength))
                    val renderRotation = lastRotation + (currentRotation - lastRotation) * rotateMul
                    lastRenderRotation = renderRotation

                    val movingMul = Easing.OUT_QUINT.inc(Easing.toDelta(lastUpdateTime, movingLength))
                    val renderPos = lastPos.add(currentPos.subtract(lastPos).scale(movingMul.toDouble()))
                    lastRenderPos = renderPos

                    scale = if (placeInfo != null) {
                        Easing.OUT_CUBIC.inc(Easing.toDelta(startTime, fadeLength))
                    } else {
                        Easing.IN_CUBIC.dec(Easing.toDelta(startTime, fadeLength))
                    }

                    GlStateManager.pushMatrix()
                    GlStateManager.translate(
                        (renderPos.x - mc.renderManager.renderPosX).toFloat(),
                        (renderPos.y - mc.renderManager.renderPosY).toFloat(),
                        (renderPos.z - mc.renderManager.renderPosZ).toFloat()
                    )
                    GlStateManager.rotate(
                        renderRotation,
                        0.0f,
                        1.0f,
                        0.0f
                    )

                    val renderer = ESPRenderer()
                    renderer.aFilled = (32.0f * scale).toInt()
                    renderer.aOutline = (233.0f * scale).toInt()

                    if (renderBase) {
                        renderer.add(boxBase, baseColor)
                    }

                    if (joinedRenders) {
                        val boxBed = AxisAlignedBB(
                            -0.5, 0.0, -1.0,
                            0.5, 0.5625, 1.0
                        )
                        renderer.add(boxBed, joinedColor)
                    } else {
                        if (renderFoot) {
                            renderer.add(boxFoot, footColor, EnumFacingMask.ALL xor EnumFacingMask.SOUTH)
                        }
                        if (renderHead) {
                            renderer.add(boxHead, headColor, EnumFacingMask.ALL xor EnumFacingMask.NORTH)
                        }
                    }

                    RenderUtils3D.resetTranslation()
                    renderer.render(false)
                    RenderUtils3D.setTranslation(
                        -mc.renderManager.renderPosX,
                        -mc.renderManager.renderPosY,
                        -mc.renderManager.renderPosZ
                    )

                    GlStateManager.popMatrix()
                }
            }
        }

        fun onRender2D() {
            if (scale != 0.0f && renderDamage) {
                lastRenderPos?.let {
                    val screenPos = ProjectionUtils.toAbsoluteScreenPos(it)
                    val alpha = (255.0f * scale).toInt()
                    val color = if (scale == 1.0f) ColorRGB(255, 255, 255) else ColorRGB(255, 255, 255, alpha)

                    MainFontRenderer.drawString(
                        lastDamageString,
                        screenPos.x.toFloat() - MainFontRenderer.getWidth(lastDamageString, 2.0f) * 0.5f,
                        screenPos.y.toFloat() - MainFontRenderer.getHeight(2.0f) * 0.5f,
                        color,
                        2.0f
                    )
                }
            }
        }

        private fun update(placeInfo: PlaceInfo?) {
            val lastBedPlacement = lastBedPlacement
            val newBedPlacement = placeInfo?.let { it.basePos to it.direction }

            if (newBedPlacement != lastBedPlacement) {
                if (placeInfo != null) {
                    currentPos = placeInfo.center
                    lastPos = lastRenderPos ?: currentPos

                    if (currentRotation.isNaN()) {
                        currentRotation = placeInfo.direction.horizontalAngle
                    } else {
                        val newAngle = placeInfo.direction.horizontalAngle
                        val lastAngle = lastBedPlacement?.second?.horizontalAngle ?: 0.0f
                        val deltaAngle = RotationUtils.normalizeAngle(newAngle - lastAngle)
                        currentRotation += deltaAngle
                    }

                    lastRotation = if (!lastRenderRotation.isNaN()) lastRenderRotation else currentRotation

                    lastUpdateTime = System.currentTimeMillis()
                    if (lastBedPlacement == null) {
                        startTime = System.currentTimeMillis()
                    }
                } else {
                    lastUpdateTime = System.currentTimeMillis()
                    startTime = System.currentTimeMillis()
                }

                Renderer.lastBedPlacement = newBedPlacement
            }

            if (placeInfo != null) {
                lastDamageString = placeInfo.string
            }
        }
    }
}