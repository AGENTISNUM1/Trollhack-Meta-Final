package dev.luna5ama.trollhack.gui.hudgui.elements.combat

import dev.fastmc.common.MathUtil.clamp
import dev.luna5ama.trollhack.graphics.GlStateUtils
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.RenderUtils2D.drawCircleFilled
import dev.luna5ama.trollhack.graphics.RenderUtils2D.drawRectFilled
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.hudgui.HudElement
import dev.luna5ama.trollhack.manager.managers.CombatManager
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import dev.luna5ama.trollhack.util.math.vector.distanceTo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import kotlin.math.roundToInt

internal object TargetHud : HudElement(
    name = "Target HUD",
    category = Category.COMBAT,
    description = "Displays information about your current target"
) {
    private val scaleSetting by setting("Scale", 1.0f, 0.5f..2.0f, 0.1f)
    private val showDistance by setting("Show Distance", true)
    private val showHealth by setting("Show Health", true)
    private val showHead by setting("Show Head", true)

    // Color settings
    private val healthColor by setting("Health Color", ColorRGB(255, 0, 0))

    private var displayHealth = 0.0f
    private var health = 0.0f
    private var ticks = 0.0f

    override val hudWidth: Float get() = 145.0f * scaleSetting
    override val hudHeight: Float get() = 48.0f * scaleSetting

    override fun renderHud() {
        super.renderHud()

        val target = CombatManager.target

        if (scale <= 0.0f) return

        val scaledX = interpolate(0.0, hudWidth.toDouble(), scale.toDouble())
        val scaledY = interpolate(0.0, hudHeight.toDouble(), scale.toDouble())

        GlStateManager.pushMatrix()
        GlStateManager.translate(renderPosX + scaledX, renderPosY + scaledY, 0.0)
        GlStateManager.scale(scale.toDouble(), scale.toDouble(), 1.0)

        // Draw background
        drawRoundedRect(0.0, 0.0, hudWidth.toDouble(), hudHeight.toDouble(), 8.0, ClickGUI.backGround)

        if (target != null && target is EntityPlayer) {
            // Update health display
            health = clamp(target.health, 0.0f, 20.0f)
            displayHealth = (displayHealth * 5.0f + health) / 6.0f
            ticks += 0.1f

            // Draw player head (like tab list)
            if (showHead) {
                val headSize = 32.0f
                val headX = 5.0f
                val headY = 5.0f

                GlStateManager.pushMatrix()
                GlStateManager.translate(headX.toDouble(), headY.toDouble(), 0.0)
                drawPlayerHead(target, headSize)
                GlStateManager.popMatrix()
            }

            // Draw name
            MainFontRenderer.drawString(target.name, 38.0f, 5.0f, ClickGUI.text)

            // Draw distance
            if (showDistance) {
                val distance = mc.player.distanceTo(target).roundToInt()
                MainFontRenderer.drawString("Distance: $distance", 80.0f, 22.0f, ClickGUI.text)
            }

            // Draw health bar
            if (showHealth) {
                drawHealthBar(38.0f, 35.0f, 100.0f, 5.0f, displayHealth / 20.0f)

                // Draw health text
                MainFontRenderer.drawString(displayHealth.roundToInt().toString(), 140.0f, 32.0f, ClickGUI.text)
            }
        }

        GlStateManager.popMatrix()
    }

    fun drawTexturedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        u: Float,
        v: Float,
        uWidth: Float,
        vHeight: Float
    ) {
        glBegin(GL_QUADS)
        glTexCoord2f(u, v)
        glVertex2f(x, y)
        glTexCoord2f(u + uWidth, v)
        glVertex2f(x + width, y)
        glTexCoord2f(u + uWidth, v + vHeight)
        glVertex2f(x + width, y + height)
        glTexCoord2f(u, v + vHeight)
        glVertex2f(x, y + height)
        glEnd()
    }
    private fun drawPlayerHead(player: EntityPlayer, size: Float) {
        GlStateUtils.depth(true)
        GlStateUtils.texture2d(true)
        GlStateUtils.blend(true)
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

        // Bind player skin texture
        val skin = mc.player.locationSkin
        mc.textureManager.bindTexture(skin)

        // Draw the face (8x8 texture coordinates from the skin)
        drawTexturedRect(
            0.0f, 0.0f, size, size,
            8.0f / 64.0f, 8.0f / 64.0f, 16.0f / 64.0f, 16.0f / 64.0f
        )

        // Draw the hat layer (40x8 texture coordinates from the skin)
        drawTexturedRect(
            0.0f, 0.0f, size, size,
            40.0f / 64.0f, 8.0f / 64.0f, 48.0f / 64.0f, 16.0f / 64.0f
        )

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        GlStateUtils.depth(false)
    }

    private fun drawHealthBar(x: Float, y: Float, width: Float, height: Float, progress: Float) {
        val progressWidth = (width * progress).coerceIn(0.0f, width)

        // Draw background (dark gray)
        drawRectFilled(
            x,               // x1
            y,               // y1
            x + width,       // x2
            y + height,      // y2
            ColorRGB(50, 50, 50) // color
        )

        // Draw health (solid color)
        drawRectFilled(
            x,               // x1
            y,               // y1
            x + progressWidth, // x2
            y + height,      // y2
            healthColor      // color
        )
    }

    private fun drawRoundedRect(x: Double, y: Double, width: Double, height: Double, radius: Double, color: ColorRGB) {
        // Draw the main rectangular parts
        drawRectFilled(
            (x + radius).toFloat(), y.toFloat(),
            (x + width - radius).toFloat(), (y + height).toFloat(),
            color
        )
        drawRectFilled(
            x.toFloat(), (y + radius).toFloat(),
            (x + width).toFloat(), (y + height - radius).toFloat(),
            color
        )

        // Draw the four rounded corners
        val positions = arrayOf(
            Vec2f((x + radius).toFloat(), (y + radius).toFloat()),               // Top-left
            Vec2f((x + width - radius).toFloat(), (y + radius).toFloat()),      // Top-right
            Vec2f((x + radius).toFloat(), (y + height - radius).toFloat()),     // Bottom-left
            Vec2f((x + width - radius).toFloat(), (y + height - radius).toFloat()) // Bottom-right
        )

        positions.forEach { pos ->
            drawCircleFilled(
                center = pos,
                radius = radius.toFloat(),
                color = color
            )
        }
    }

    private fun interpolate(start: Double, end: Double, progress: Double): Double {
        return start + (end - start) * progress
    }
}