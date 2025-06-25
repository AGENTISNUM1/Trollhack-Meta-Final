package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.EntityManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.betterPosition
import dev.luna5ama.trollhack.util.EntityUtils.flooredPosition
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.inventory.slot.firstBlock
import dev.luna5ama.trollhack.util.inventory.slot.hotbarSlots
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendChatMessage
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendErrorMessage
import dev.luna5ama.trollhack.util.threads.runSafe
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.world.*
import net.minecraft.block.BlockDirectional
import net.minecraft.block.properties.IProperty
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

internal object HoleKicker : Module(
    name = "HoleKicker",
    description = "Automatically pushes players with pistons",
    category = Category.EXPLOIT
) {

    private val range by setting("Range", 5.2f, 1.0f..6.0f, 0.5f)
    private val burrow by setting("Push Burrow", true)
    private val pullback by setting("Pullback", true)
    private val autoToggle by setting("Auto Toggle", true)
    private val maxAttempts by setting("Max Attempts", 2, 1..10, 1)

    private var redstoneSlot = -1
    private var torchSlot = -1
    private var pistonSlot = -1
    private var attempts = 0
    private var targetPlayer: EntityPlayer? = null

    init {
        onDisable {
            reset()
        }

        onEnable {
            runSafe {
                // Break crystals on enable
                breakCrystals()
            } ?: disable()
        }

        safeListener<TickEvent.Post> {
            if (Wrapper.player == null || Wrapper.world == null) return@safeListener

            findSlots()

            if (!hasRequiredItems()) {
                sendErrorMessage("Missing redstone block/torch or piston!")
                disable()
                return@safeListener
            }

            if (tryPushPlayer()) {
                attempts++
                if (autoToggle && attempts >= maxAttempts) {
                    sendChatMessage("Reached max attempts, disabling...")
                    disable()
                }
            }
        }
    }

    private fun SafeClientEvent.findSlots() {
        redstoneSlot = player.hotbarSlots.firstBlock(Blocks.REDSTONE_BLOCK)?.hotbarSlot ?: -1
        torchSlot = player.hotbarSlots.firstBlock(Blocks.REDSTONE_TORCH)?.hotbarSlot ?: -1
        pistonSlot = player.hotbarSlots.firstBlock(Blocks.PISTON)?.hotbarSlot ?: -1
    }

    private fun hasRequiredItems(): Boolean {
        return (redstoneSlot != -1 || torchSlot != -1) && pistonSlot != -1
    }

    private fun SafeClientEvent.tryPushPlayer(): Boolean {
        if (!player.onGround) return false

        for (target in EntityManager.players) {
            if (!isValidTarget(target)) continue

            val targetPos = target.betterPosition
            if (!canPushTarget(target, targetPos)) continue

            targetPlayer = target

            // Try each direction
            for (facing in EnumFacing.HORIZONTALS) {
                val pistonPos = targetPos.up().offset(facing)

                if (!canPlacePiston(pistonPos, facing.opposite)) continue

                val redstonePos = findRedstonePosition(pistonPos) ?: continue

                val yaw = getYawForDirection(facing)
                placePiston(pistonPos, redstonePos, yaw)
                return true
            }
        }

        if (autoToggle && attempts >= maxAttempts) {
            sendErrorMessage("No valid position found!")
            disable()
        }

        return false
    }

    private fun SafeClientEvent.isValidTarget(target: EntityPlayer): Boolean {
        if (target.isFakeOrSelf) return false
        if (target.distanceTo(player) > range) return false
        if (target.isDead || target.health <= 0f) return false

        val speed = Vec3d(target.motionX, target.motionY, target.motionZ).length()
        if (speed > 20.0) return false

        return true
    }

    private fun SafeClientEvent.canPushTarget(target: EntityPlayer, targetPos: BlockPos): Boolean {
        // Check if target is in a valid position for pushing
        val airAbove = world.isAir(targetPos.up()) && world.isAir(targetPos.up(2))

        if (!autoToggle) {
            // Only push if target is in hole or if we allow burrow pushing
            val inHole = isInHole(target)
            if (!inHole && !burrow) return false
        }

        if (!airAbove && !burrow) return false

        return true
    }

    private fun SafeClientEvent.isInHole(entity: Entity): Boolean {
        val pos = entity.flooredPosition
        return !world.isAir(pos)
    }

    private fun SafeClientEvent.canPlacePiston(pistonPos: BlockPos, facing: EnumFacing): Boolean {
        val currentBlock = world.getBlockState(pistonPos).block

        // Check if position is clear or already has a correctly oriented piston
        if (currentBlock != Blocks.AIR &&
            !(currentBlock == Blocks.PISTON &&
                    world.getBlockState(pistonPos).getValue(BlockDirectional.FACING as IProperty<EnumFacing>) == facing)) {
            return false
        }

        // Check if we can place here
        if (!world.isPlaceable(pistonPos)) return false

        // Check distance
        if (player.distanceTo(pistonPos.x + 0.5, pistonPos.y + 0.5, pistonPos.z + 0.5) > range) return false

        return true
    }

    private fun SafeClientEvent.findRedstonePosition(pistonPos: BlockPos): BlockPos? {
        val positions = arrayOf(
            pistonPos.up(),    // Above piston
            pistonPos.down(),  // Below piston
            pistonPos.east(),  // East of piston
            pistonPos.west(),  // West of piston
            pistonPos.north(), // North of piston
            pistonPos.south()  // South of piston
        )

        for (pos in positions) {
            if (canPlaceRedstone(pos)) {
                return pos
            }
        }

        // If redstone block is available, try above piston
        if (redstoneSlot != -1 && world.isAir(pistonPos.up()) && hasSupport(pistonPos.up())) {
            return pistonPos.up()
        }

        return null
    }

    private fun SafeClientEvent.canPlaceRedstone(pos: BlockPos): Boolean {
        val currentBlock = world.getBlockState(pos).block

        // Position already has redstone
        if (currentBlock == Blocks.REDSTONE_BLOCK || currentBlock == Blocks.REDSTONE_TORCH) {
            return true
        }

        // Can place redstone here
        if (world.isAir(pos) && hasSupport(pos) && world.isPlaceable(pos)) {
            return true
        }

        return false
    }

    private fun SafeClientEvent.hasSupport(pos: BlockPos): Boolean {
        // Check if there's a solid block to place on
        return isHardBlock(pos.down()) ||
                isHardBlock(pos.east()) ||
                isHardBlock(pos.west()) ||
                isHardBlock(pos.north()) ||
                isHardBlock(pos.south())
    }

    private fun SafeClientEvent.isHardBlock(pos: BlockPos): Boolean {
        val block = world.getBlockState(pos).block
        return block == Blocks.OBSIDIAN ||
                block == Blocks.BEDROCK ||
                (block == Blocks.PISTON && redstoneSlot != -1)
    }

    private fun getYawForDirection(facing: EnumFacing): Float {
        return when (facing) {
            EnumFacing.NORTH -> 180f
            EnumFacing.EAST -> 270f
            EnumFacing.SOUTH -> 0f
            EnumFacing.WEST -> 90f
            else -> 0f
        }
    }

    private fun SafeClientEvent.placePiston(pistonPos: BlockPos, redstonePos: BlockPos, yaw: Float) {
        val target = targetPlayer ?: return

        // Break crystals if they're in the way
        if (hasCrystal(pistonPos) || hasCrystal(redstonePos)) {
            breakCrystals()
            return
        }

        val oldSlot = player.inventory.currentItem
        val pitch = target.rotationPitch

        // Place piston first
        if (world.isAir(pistonPos)) {
            val placeInfo = getPlaceInfo(pistonPos) ?: return
            player.inventory.currentItem = pistonSlot
            sendPlayerPacket {
                rotate(Vec2f(yaw, pitch))
            }
            placeBlock(placeInfo, EnumHand.MAIN_HAND)
        }

        // Place redstone
        val redstoneBlock = world.getBlockState(redstonePos).block
        if (redstoneBlock != Blocks.REDSTONE_BLOCK && redstoneBlock != Blocks.REDSTONE_TORCH) {
            val redSlot = if (redstoneSlot != -1) redstoneSlot else torchSlot
            val placeInfo = getPlaceInfo(redstonePos) ?: return
            player.inventory.currentItem = redSlot
            placeBlock(placeInfo, EnumHand.MAIN_HAND)
        }

        // Restore slot
        player.inventory.currentItem = oldSlot

        // Pull back redstone if enabled
        if (redstoneSlot != -1 && pullback) {
            val breakPos = if (redstoneBlock == Blocks.AIR) redstonePos else redstonePos
            world.sendBlockBreakProgress(player.entityId, breakPos, 0)
        }
    }

    private fun SafeClientEvent.getPlaceInfo(pos: BlockPos): PlaceInfo? {
        return getNeighbor(pos)
    }

    private fun SafeClientEvent.getNeighbor(pos: BlockPos): PlaceInfo? {
        for (side in EnumFacing.values()) {
            val offsetPos = pos.offset(side)
            val oppositeSide = side.opposite

            if (world.getBlockState(offsetPos).isReplaceable) continue

            val hitVec = getHitVec(offsetPos, oppositeSide)
            val hitVecOffset = getHitVecOffset(oppositeSide)

            return PlaceInfo(offsetPos, oppositeSide, 0.0, hitVecOffset, hitVec, pos)
        }

        return null
    }

    private fun SafeClientEvent.breakCrystals() {
        // Find and break nearby crystals
        for (crystal in world.loadedEntityList) {
            if (crystal is EntityEnderCrystal && player.distanceTo(crystal) <= range) {
                playerController.attackEntity(player, crystal)
            }
        }
    }

    private fun SafeClientEvent.hasCrystal(pos: BlockPos): Boolean {
        return world.getEntitiesWithinAABB(EntityEnderCrystal::class.java,
            player.entityBoundingBox.grow(range.toDouble())).any { crystal ->
            val crystalPos = BlockPos(crystal.posX, crystal.posY, crystal.posZ)
            crystalPos == pos || crystalPos == pos.up()
        }
    }

    private fun reset() {
        redstoneSlot = -1
        torchSlot = -1
        pistonSlot = -1
        attempts = 0
        targetPlayer = null
    }
}
