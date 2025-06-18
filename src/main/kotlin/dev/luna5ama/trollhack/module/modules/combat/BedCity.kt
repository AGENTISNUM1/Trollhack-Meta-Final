package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.manager.managers.HoleManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.math.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.util.world.canBreakBlock
import dev.luna5ama.trollhack.util.world.checkBlockCollision
import dev.luna5ama.trollhack.util.world.isAir
import net.minecraft.block.BlockBed
import net.minecraft.block.BlockFalling
import net.minecraft.block.BlockRail
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

internal object BedCity : Module(
    name = "CombatMine",
    category = Category.COMBAT,
    description = "Mines blocks around targets and prevents player trapping",
    modulePriority = 100,
    alias = arrayOf("AutoMine", "BedCity", "AutoCity", "City", "CivBreaker", "Civ")
) {
    private val keyCity by setting("KeyCity", false)
    private val antiTrap by setting("AntiTrap", false, description = "Mines blocks above player when head is surrounded")

    private var lastSurrounded = false
    private var lastTargetPos: BlockPos? = null
    private var lastMinePos: BlockPos? = null

    private val facings = arrayOf(
        EnumFacing.WEST,
        EnumFacing.NORTH,
        EnumFacing.EAST,
        EnumFacing.SOUTH
    )

    override fun isActive(): Boolean {
        return isEnabled && (lastMinePos != null || antiTrap)
    }

    init {
        onEnable {
            enable()
        }

        onDisable {
            lastSurrounded = false
            lastTargetPos = null
            lastMinePos = null
            PacketMine.reset(this)
        }

        safeParallelListener<TickEvent.Post> {
            run()
        }
    }

    private fun SafeClientEvent.run() {
        // Original combat mining logic
        val target = CombatManager.target ?: return
        val targetPos = target.betterPosition
        val minePos = BlockPos.MutableBlockPos()

        val currentSurrounded = HoleManager.getHoleInfo(targetPos).isHole
        val surrounded = currentSurrounded && lastSurrounded
        lastSurrounded = currentSurrounded

        val diffX = target.posX - (targetPos.x + 0.5)
        val diffZ = target.posZ - (targetPos.z + 0.5)
        facings.sortWith(
            compareByDescending<EnumFacing> {
                lastMinePos == minePos.setAndAdd(targetPos, it)
            }.thenBy {
                val directionVec = it.directionVec
                diffX * directionVec.x + diffZ * directionVec.z
            }
        )

        fun checkEmpty(pos: BlockPos): Boolean {
            if (!world.canBreakBlock(pos)) return true

            val blockState = world.getBlockState(pos)
            val block = blockState.block
            if (block is BlockBed) return true
            if (block is BlockFalling) return true
            if (block is BlockRail) return true

            return world.isAir(pos)
        }

        fun minePos(minePos: BlockPos?): Boolean {
            if (minePos == null) return false
            PacketMine.mineBlock(BedCity, minePos, if (checkEmpty(targetPos)) -100 else modulePriority)
            lastTargetPos = targetPos
            lastMinePos = minePos

            if (keyCity) {
                disable()
            }

            return true
        }

        fun tryMine(pos: BlockPos): Boolean {
            if (checkEmpty(pos)) return false
            return minePos(pos)
        }

        fun tryMineSurround(pos: BlockPos): Boolean {
            if (checkEmpty(pos)) return false
            if (!surrounded && !world.checkBlockCollision(pos, target.entityBoundingBox)) return false
            return minePos(pos)
        }

        fun tryHeadMineSurround(pos: BlockPos): Boolean {
            if (checkEmpty(pos)) return false
            if (!world.checkBlockCollision(pos, target.entityBoundingBox)) return false
            return minePos(pos)
        }

        // Original target mining sequence
        if (tryMine(minePos.setPos(targetPos))) return
        if (tryMine(minePos.setAndAdd(targetPos, EnumFacing.UP))) return
        if (tryMineSurround(minePos.setAndAdd(targetPos, facings[0]))) return
        if (tryMineSurround(minePos.setAndAdd(targetPos, facings[1]))) return
        if (tryMineSurround(minePos.setAndAdd(targetPos, facings[2]))) return
        if (tryMineSurround(minePos.setAndAdd(targetPos, facings[3]))) return
        if (tryHeadMineSurround(minePos.setAndAdd(targetPos, facings[0]).move(EnumFacing.UP))) return
        if (tryHeadMineSurround(minePos.setAndAdd(targetPos, facings[1]).move(EnumFacing.UP))) return
        if (tryHeadMineSurround(minePos.setAndAdd(targetPos, facings[2]).move(EnumFacing.UP))) return
        if (tryHeadMineSurround(minePos.setAndAdd(targetPos, facings[3]).move(EnumFacing.UP))) return
        if (minePos(lastMinePos)) return

        // New AntiTrap logic
        if (antiTrap) {
            val playerPos = player.betterPosition
            val posAbove1 = playerPos.up(1)
            val posAbove2 = playerPos.up(2)

            if (isHeadSurrounded(playerPos)) {
                when {
                    world.canBreakBlock(posAbove1) && !world.isAir(posAbove1) -> {
                        // Mine the first block above if it exists and is breakable
                        PacketMine.mineBlock(BedCity, posAbove1, Int.MAX_VALUE)
                    }
                    world.isAir(posAbove1) && world.canBreakBlock(posAbove2) -> {
                        // Only mine second block if first is already cleared
                        PacketMine.mineBlock(BedCity, posAbove2, Int.MAX_VALUE)
                    }
                }
            }
        }
    }

    private fun SafeClientEvent.isHeadSurrounded(playerPos: BlockPos): Boolean {
        val headPos = playerPos.up(1)
        return EnumFacing.HORIZONTALS.all { facing ->
            !world.isAir(headPos.offset(facing))
        } && !world.isAir(headPos.up(1))
    }
}