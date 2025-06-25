package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.render.RenderEntityEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.threads.onMainThread
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.client.entity.EntityOtherPlayerMP
import com.mojang.authlib.GameProfile
import dev.luna5ama.trollhack.module.modules.combat.BedAura
import dev.luna5ama.trollhack.module.modules.movement.Speed
import java.util.*

internal object SelfTarget : Module(
    name = "SelfTarget",
    description = "target urself",
    category = Category.META
) {
    private val Bed by setting("SelfBed", false)
    private val disableba by setting("Disable Bedaura", false, { Bed })
    private val disablespeed by setting("Disable Speed", false, { speed })
    private val speed by setting("Enable speed", false)
    private const val ENTITY_ID = -696969421
    private var fakePlayer: EntityOtherPlayerMP? = null
    private const val PLAYER_NAME = "Selftarget"
    private const val visible = false
    private val healthThreshold by setting("Health Threshold", 10.0f, 1.0f..20.0f, 0.5f)
    private var wasDisabledBySafety = false
    private var enabledba = false
    private var enabledspeed = false
    private var oldTimingMode: BedAura.TimingMode? = null

    override fun getHudInfo(): String {
        return PLAYER_NAME
    }

    init {
        onEnable {
            runSafe {
                if (shouldSpawnFakePlayer()) {
                    spawnFakePlayer()
                } else {
                    wasDisabledBySafety = true
                    disable()
                }
            } ?: disable()
            if (Bed) {
                BedAura.enable()
                oldTimingMode = BedAura.timingMode
                BedAura.timingMode = BedAura.TimingMode.INSTANT
                enabledba = true
            }
            if (speed) {
                Speed.enable()
                enabledspeed = true
            }

        }

        onDisable {
            onMainThread {
                removeFakePlayer()
            }
            if (Bed && BedAura.isEnabled && enabledba && disableba) {
                BedAura.disable()
                enabledba = false
                BedAura.timingMode = oldTimingMode!!
            }
            if (speed && Speed.isEnabled && enabledspeed && disablespeed) {
                Speed.disable()
                enabledspeed = false
                oldTimingMode = null
            }
        }

        listener<ConnectionEvent.Disconnect> {
            disable()
        }

        listener<RenderEntityEvent.All.Pre> {
            if (it.entity === fakePlayer) {
                it.cancelled = !visible
            }
        }

        safeParallelListener<TickEvent.Post> {
            val player = mc.player ?: return@safeParallelListener

            if (player.health < healthThreshold) {
                if (fakePlayer != null) {
                    onMainThread {
                        removeFakePlayer()
                    }
                    wasDisabledBySafety = true
                }
            } else if (wasDisabledBySafety && fakePlayer == null) {
                if (isEnabled) {
                    onMainThread {
                        spawnFakePlayer()
                    }
                }
                wasDisabledBySafety = false
            }

            // Update position if fake player exists
            fakePlayer?.let {
                it.setPosition(player.posX, player.posY, player.posZ)
            }
        }
    }

    private fun spawnFakePlayer() {
        mc.player?.let { player ->
            if (player.health >= healthThreshold) {
                fakePlayer = EntityOtherPlayerMP(mc.world, GameProfile(UUID.randomUUID(), PLAYER_NAME)).apply {
                    setPosition(player.posX, player.posY, player.posZ)
                }.also {
                    mc.world?.addEntityToWorld(ENTITY_ID, it)
                }
            }
        }
    }
    private fun removeFakePlayer() {
        fakePlayer?.setDead()
        mc.world?.removeEntityFromWorld(ENTITY_ID)
        fakePlayer = null
    }

    private fun shouldSpawnFakePlayer(): Boolean {
        return (mc.player?.health ?: 0f) >= healthThreshold
    }
}