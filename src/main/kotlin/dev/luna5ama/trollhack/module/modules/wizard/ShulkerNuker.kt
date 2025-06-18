package dev.luna5ama.trollhack.module.modules.wizard

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.world.canBreakBlock
import net.minecraft.block.BlockShulkerBox
import net.minecraft.util.math.BlockPos
import org.lwjgl.input.Keyboard

internal object ShulkerNuker : Module(
    name = "Shulker Nuker",
    category = Category.META,
    description = "Removes Shulker Boxes within range",
    modulePriority = 100
) {
    private val range by setting("Range", 5, 1..10, 1)
    private val pauseBind by setting("Pause Bind", Bind())
    private val pauseDelay by setting("Pause Delay", 1000, 100..5000, 100)
    private val detectPlayers by setting("Detect Players", false)
    private val playerRange by setting("Player Range", 8, 5..13, 1, { detectPlayers })
    private val ignoreFriends by setting("Ignore Friends", true, { detectPlayers })

    private var lastMinePos: BlockPos? = null
    private var pauseTime = 0L

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
        if (!pauseBind.isEmpty && Keyboard.isKeyDown(pauseBind.key)) {
            pauseTime = System.currentTimeMillis() + pauseDelay
        }
        if (System.currentTimeMillis() < pauseTime) {
            return
        }

        if (detectPlayers) {
            val nearbyEnemies = checkNearbyPlayers()
            if (!nearbyEnemies) {
                PacketMine.reset(this@ShulkerNuker)
                return
            }
        }

        val playerPos = player.betterPosition
        val shulkerPos = checkBlocksInRange(playerPos, range, BlockShulkerBox::class.java)

        if (shulkerPos != null) {
            minePos(shulkerPos)
        }
    }

    private fun SafeClientEvent.checkNearbyPlayers(): Boolean {
        return world.playerEntities.any { entity ->
            entity != player &&
                    player.getDistanceSq(entity) <= playerRange * playerRange &&
                    (!ignoreFriends || !FriendManager.isFriend(entity.name))
        }
    }

    private fun SafeClientEvent.checkBlocksInRange(playerPos: BlockPos, range: Int, blockClass: Class<out BlockShulkerBox>): BlockPos? {
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

        PacketMine.mineBlock(this@ShulkerNuker, pos, 100)
        lastMinePos = pos
        return true
    }
}