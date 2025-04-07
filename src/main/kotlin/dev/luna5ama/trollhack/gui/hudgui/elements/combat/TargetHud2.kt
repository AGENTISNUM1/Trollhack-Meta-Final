package dev.luna5ama.trollhack.gui.hudgui.elements.combat

import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.RenderUtils3D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.hudgui.HudElement
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.util.threads.runSafe
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.MathHelper
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glUseProgram

internal object TargetHud2 : HudElement(
    name = "TargetHud2",
    category = Category.COMBAT,
    description = "Displays nearest non-friendly player with info"
) {
    private val emulatePitch by setting("Emulate Pitch", true)
    private val emulateYaw by setting("Emulate Yaw", false)
    private val showDistance by setting("Show Distance", true)
    private val showPing by setting("Show Ping", true)
    private val showHealth by setting("Show Health", true)
    private val maxDistance by setting("Max Distance", 100.0f, 10.0f..200.0f, 10.0f)
    private val textColor by setting("Text Color", ClickGUI.text)

    override val hudWidth get() = 60.0f
    override val hudHeight get() = 90.0f

    private var target: EntityPlayer? = null
    private var lastUpdate = 0L

    override fun renderHud() {
        super.renderHud()
        runSafe {
            // Update target every 10 ticks to prevent flickering
            if (System.currentTimeMillis() - lastUpdate > 200) {
                target = findNearestEnemy()
                lastUpdate = System.currentTimeMillis()
            }

            target?.let { enemy ->
                // Draw enemy model
                drawPlayerModel(enemy)

                // Draw enemy info
                drawPlayerInfo(enemy)
            } ?: run {
                // No enemy found message
                MainFontRenderer.drawString("No enemy", renderPosX + 5.0f, renderPosY + 10.0f, textColor)
            }
        }
    }

    private fun findNearestEnemy(): EntityPlayer? {
        return mc.world.playerEntities
            .filter { it != mc.player }
            .filter { !FriendManager.isFriend(it.name) }
            .filter { mc.player.getDistance(it) <= maxDistance }
            .minByOrNull { mc.player.getDistance(it) }
    }

    private fun drawPlayerModel(enemy: EntityPlayer) {
        val yaw = if (emulateYaw) interpolateAndWrap(enemy.prevRotationYaw, enemy.rotationYaw) else 0.0f
        val pitch = if (emulatePitch) interpolateAndWrap(enemy.prevRotationPitch, enemy.rotationPitch) else 0.0f

        GlStateManager.pushMatrix()
        GlStateManager.translate(renderPosX + 30.0f, renderPosY + 70.0f, 0.0f)
        GlStateUtils.depth(true)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

        glUseProgram(0)
        GuiInventory.drawEntityOnScreen(0, 0, 30, -yaw, -pitch, enemy)

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateUtils.depth(false)
        GlStateUtils.texture2d(true)
        GlStateUtils.blend(true)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE)

        GlStateManager.disableColorMaterial()
        GlStateManager.popMatrix()
    }

    private fun drawPlayerInfo(enemy: EntityPlayer) {
        val textY = renderPosY + 10.0f
        val textX = renderPosX + 5.0f

        // Draw enemy name
        MainFontRenderer.drawString(enemy.name, textX, textY, textColor)

        // Draw health
        if (showHealth) {
            val healthText = "HP: ${enemy.health.toInt()}"
            MainFontRenderer.drawString(healthText, textX, textY + 12.0f, getHealthColor(enemy.health))
        }

        // Draw distance
        if (showDistance) {
            val distance = mc.player.getDistance(enemy).toInt()
            MainFontRenderer.drawString("${distance}m", textX, textY + 24.0f, textColor)
        }

        // Draw ping
        if (showPing) {
            val ping = mc.connection?.getPlayerInfo(enemy.uniqueID)?.responseTime?.coerceAtLeast(0) ?: 0
            MainFontRenderer.drawString("${ping}ms", textX, textY + 36.0f, textColor)
        }
    }

    private fun getHealthColor(health: Float): ColorRGB {
        return when {
            health < 10 -> ColorRGB(255, 0, 0)       // Red
            health < 15 -> ColorRGB(255, 165, 0)    // Orange
            else -> ColorRGB(0, 255, 0)             // Green
        }
    }

    private fun interpolateAndWrap(prev: Float, current: Float): Float {
        return MathHelper.wrapDegrees(prev + (current - prev) * RenderUtils3D.partialTicks)
    }
}