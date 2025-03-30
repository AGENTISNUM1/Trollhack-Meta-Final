package dev.luna5ama.trollhack.module.modules.dev

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.world.canBreakBlock
import net.minecraft.block.*
import net.minecraft.util.math.BlockPos
import org.lwjgl.input.Keyboard

internal object Remover : Module(
    name = "Remover",
    category = Category.COMBAT,
    description = "Removes specified blocks within a range",
    modulePriority = 100
) {
    private val page by setting("Page", Page.GENERAL)
    private val toggleShulkerBox by setting("Toggle Shulker Box", true, { page == Page.GENERAL })
    private val toggleAnvil by setting("Toggle Anvil", true, { page == Page.GENERAL })
    private val toggleRedstone by setting("Toggle Redstone", true, { page == Page.GENERAL })
    private val togglePiston by setting("Toggle Piston", true, { page == Page.GENERAL })
    private val toggleWeb by setting("Toggle Web", true, { page == Page.GENERAL })
    private val toggleBed by setting("Toggle Bed", true, { page == Page.GENERAL })
    private val toggleRail by setting("Toggle Rail", true, { page == Page.GENERAL })
    private val shulkerBoxRange by setting("Shulker Box Range", 5, 1..10, 1, { toggleShulkerBox ; page == Page.RANGE })
    private val anvilRange by setting("Anvil Range", 5, 1..10, 1, { toggleAnvil ; page == Page.RANGE })
    private val redstoneRange by setting("Redstone Range", 5, 1..10, 1, { toggleRedstone ; page == Page.RANGE })
    private val pistonRange by setting("Piston Range", 5, 1..10, 1, { togglePiston ; page == Page.RANGE })
    private val webRange by setting("Web Range", 5, 1..10, 1, { toggleWeb ; page == Page.RANGE })
    private val bedRange by setting("Bed Range", 5, 1..10, 1, { toggleBed ; page == Page.RANGE })
    private val railRange by setting("Rail Range", 5, 1..10, 1, { toggleRail ; page == Page.RANGE })
    private val shulkerBoxPriority by setting("Shulker Box Priority", 1, 1..10, 1, { toggleShulkerBox ; page == Page.PRIORITY })
    private val anvilPriority by setting("Anvil Priority", 3, 1..10, 1, { toggleAnvil ; page == Page.PRIORITY })
    private val redstonePriority by setting("Redstone Priority", 4, 1..10, 1, { toggleRedstone ; page == Page.PRIORITY })
    private val pistonPriority by setting("Piston Priority", 5, 1..10, 1, { togglePiston ; page == Page.PRIORITY })
    private val webPriority by setting("Web Priority", 6, 1..10, 1, { toggleWeb ; page == Page.PRIORITY })
    private val bedPriority by setting("Bed Priority", 7, 1..10, 1, { toggleBed ; page == Page.PRIORITY })
    private val railPriority by setting("Rail Priority", 8, 1..10, 1, { toggleRail ; page == Page.PRIORITY })
    private val shulkerpause by setting("Shulker Pause", false, { toggleShulkerBox ; page == Page.GENERAL })
    private val shulkerPauseDelay by setting("Shulker Pause Delay", 1000, 100..5000, 100, { shulkerpause ; page == Page.GENERAL })
    private val shulkerPauseBind by setting("Shulker Pause Bind", Bind(), { shulkerpause ; page == Page.GENERAL })
    private var lastMinePos: BlockPos? = null
    private var shulkerPauseTime = 0L
    private enum class Page {
        GENERAL, RANGE, PRIORITY
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
        if (!shulkerPauseBind.isEmpty && Keyboard.isKeyDown(shulkerPauseBind.key)) {
            shulkerPauseTime = System.currentTimeMillis() + shulkerPauseDelay
        }
        if (System.currentTimeMillis() < shulkerPauseTime) {
            return
        }
        val playerPos = player.betterPosition
        val minePos = BlockPos.MutableBlockPos()
        val blocksToRemove = mutableListOf<Pair<BlockPos, Int>>()
        if (toggleShulkerBox && shulkerBoxRange > 0) {
            checkBlocksInRange(playerPos, shulkerBoxRange, BlockShulkerBox::class.java)?.let {
                blocksToRemove.add(Pair(it, shulkerBoxPriority))
            }
        }

        if (toggleAnvil && anvilRange > 0) {
            checkBlocksInRange(playerPos, anvilRange, BlockAnvil::class.java)?.let {
                blocksToRemove.add(Pair(it, anvilPriority))
            }
        }

        if (toggleRedstone && redstoneRange > 0) {
            checkBlocksInRange(playerPos, redstoneRange, BlockRedstoneWire::class.java)?.let {
                blocksToRemove.add(Pair(it, redstonePriority))
            }
            checkBlocksInRange(playerPos, redstoneRange, BlockRedstoneTorch::class.java)?.let {
                blocksToRemove.add(Pair(it, redstonePriority))
            }
        }

        if (togglePiston && pistonRange > 0) {
            checkBlocksInRange(playerPos, pistonRange, BlockPistonBase::class.java)?.let {
                blocksToRemove.add(Pair(it, pistonPriority))
            }
        }

        if (toggleWeb && webRange > 0) {
            checkBlocksInRange(playerPos, webRange, BlockWeb::class.java)?.let {
                blocksToRemove.add(Pair(it, webPriority))
            }
        }

        if (toggleBed && bedRange > 0) {
            checkBlocksInRange(playerPos, bedRange, BlockBed::class.java)?.let {
                blocksToRemove.add(Pair(it, bedPriority))
            }
        }

        if (toggleRail && railRange > 0) {
            checkBlocksInRange(playerPos, railRange, BlockRailBase::class.java)?.let {
                blocksToRemove.add(Pair(it, railPriority))
            }
        }

        blocksToRemove.sortBy { it.second }
        if (blocksToRemove.isNotEmpty()) {
            minePos(blocksToRemove[0].first)
        }
    }

    private fun SafeClientEvent.checkBlocksInRange(playerPos: BlockPos, range: Int, blockClass: Class<out Block>): BlockPos? {
        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    val pos = playerPos.add(x, y, z)
                    if (world.getBlockState(pos).block::class.java == blockClass) {
                        return pos
                    }
                }
            }
        }
        return null
    }

    private fun SafeClientEvent.minePos(pos: BlockPos): Boolean {
        if (!world.canBreakBlock(pos)) return false

        PacketMine.mineBlock(Remover, pos, modulePriority)
        lastMinePos = pos
        return true
    }
}