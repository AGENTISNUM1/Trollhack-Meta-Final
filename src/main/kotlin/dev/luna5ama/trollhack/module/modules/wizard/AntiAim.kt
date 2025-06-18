package dev.luna5ama.trollhack.module.modules.wizard

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager
import dev.luna5ama.trollhack.manager.managers.PlayerPacketManager.sendPlayerPacket
import dev.luna5ama.trollhack.module.modules.combat.BedAura
import dev.luna5ama.trollhack.util.math.RotationUtils
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import kotlin.math.floor

internal object AntiAim : Module(
    name = "AntiAim",
    category = Category.META,
    description = "spin spin spin"
) {
    private val speed by setting("Speed", 5, 1..30, 5)
    private val yawDelta by setting("YawDelta", 60, -360..360, 10)
    private val allowInteract by setting("AllowInteract", false)
    private var rotationYaw = 0f

    init {
        safeListener<TickEvent.Pre> {
            if (allowInteract && (mc.gameSettings.keyBindAttack.isKeyDown || mc.gameSettings.keyBindUseItem.isKeyDown)) {
                return@safeListener
            }

            val ticks = floor(mc.player.ticksExisted.toDouble() / speed).toInt()
            rotationYaw = (ticks * yawDelta).toFloat()

            sendPlayerPacket {
                rotate(Vec2f(rotationYaw, player.rotationPitch))
            }
        }
    }
}
