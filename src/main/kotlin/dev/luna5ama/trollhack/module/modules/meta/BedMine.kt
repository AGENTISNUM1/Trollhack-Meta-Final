package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.math.VectorUtils.setAndAdd
import dev.luna5ama.trollhack.util.world.canBreakBlock
import net.minecraft.block.BlockBed
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

internal object BedMine : Module(
    name = "Bed Mine",
    category = Category.META,
    description = "Automatically mines beds near you",
    modulePriority = 67
) {
    private val range by setting("Range", 2.0f, 0.5f..2.0f, 0.5f)
    private val mineFeet by setting("Mine Feet", true)
    private val mineAdjacent by setting("Mine Adjacent", true)

    private var lastMinePos: BlockPos? = null

    override fun isActive(): Boolean {
        return isEnabled && lastMinePos != null
    }

    init {
        onEnable {
            enable()
        }

        onDisable {
            lastMinePos = null
            PacketMine.reset(this)
        }

        safeParallelListener<TickEvent.Post> {
            run()
        }
    }

    private fun SafeClientEvent.run() {
        val playerPos = player.betterPosition
        val minePos = BlockPos.MutableBlockPos()

        // Check feet first if enabled
        if (mineFeet && checkBed(minePos.setPos(playerPos))) {
            mineBed(minePos)
            return
        }

        // Check adjacent blocks if enabled
        if (mineAdjacent) {
            for (facing in EnumFacing.values()) {
                if (facing == EnumFacing.UP || facing == EnumFacing.DOWN) continue

                if (checkBed(minePos.setAndAdd(playerPos, facing))) {
                    mineBed(minePos)
                    return
                }
            }
        }

        // Check surrounding area within range
        val rangeInt = range.toInt()
        for (x in -rangeInt..rangeInt) {
            for (y in -1..1) { // Only check 1 block above and below
                for (z in -rangeInt..rangeInt) {
                    if (x == 0 && y == 0 && z == 0) continue // Skip player position

                    if (checkBed(minePos.setAndAdd(playerPos, x, y, z))) {
                        mineBed(minePos)
                        return
                    }
                }
            }
        }

        // If no new bed found but we were mining one, continue mining it
        lastMinePos?.let {
            if (world.getBlockState(it).block is BlockBed && world.canBreakBlock(it)) {
                PacketMine.mineBlock(this@BedMine, it, modulePriority)
            } else {
                lastMinePos = null
            }
        }
    }

    private fun SafeClientEvent.checkBed(pos: BlockPos): Boolean {
        if (!world.canBreakBlock(pos)) return false
        return world.getBlockState(pos).block is BlockBed
    }

    private fun SafeClientEvent.mineBed(pos: BlockPos) {
        PacketMine.mineBlock(this@BedMine, pos, modulePriority)
        lastMinePos = pos
    }
}