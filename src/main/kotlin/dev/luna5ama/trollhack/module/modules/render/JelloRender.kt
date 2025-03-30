package dev.luna5ama.trollhack.module.modules.render

import dev.luna5ama.trollhack.event.events.render.Render3DEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.color.setGLColor
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object JelloRender : Module(
    name = "JelloRender",
    description = "cool",
    category = Category.RENDER,
    modulePriority = 200
) {
    private val mc = Minecraft.getMinecraft()
    private val renderManager: RenderManager get() = mc.renderManager

    private val player by setting("Player", true)
    private val crystal by setting("Crystal", true)
    private val scale by setting("Scale", 1.0f, 0.1f..2.0f, 0.1f)
    private val intensity by setting("Intensity", 0.5f, 0.1f..2.0f, 0.1f)
    private val speed by setting("Speed", 1.0f, 0.1f..5.0f, 0.1f)
    private val color by setting("Color", ColorRGB(150, 180, 255, 100), false)

    init {
        safeListener<Render3DEvent> {
            for (entity in mc.world.loadedEntityList) {
                if (!shouldRender(entity)) continue

                // Save current GL state
                GlStateManager.pushMatrix()
                GlStateManager.pushAttrib()

                // Set up rendering
                setupGL()

                // Get entity position
                val partialTicks = mc.renderPartialTicks
                val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.viewerPosX
                val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.viewerPosY
                val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.viewerPosZ

                // Calculate animation time
                val time = System.currentTimeMillis() * 0.001 * speed

                // Apply jelly effect
                renderJelloEffect(entity, x, y, z, time)

                // Restore GL state
                cleanupGL()
                GlStateManager.popAttrib()
                GlStateManager.popMatrix()
            }
        }
    }

    private fun shouldRender(entity: Any) = when (entity) {
        is EntityEnderCrystal -> crystal
        is EntityPlayer -> player && entity != mc.player
        else -> false
    }

    private fun setupGL() {
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableDepth()
        GlStateManager.disableCull()
        color.setGLColor()
    }

    private fun cleanupGL() {
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.enableDepth()
        GlStateManager.enableCull()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
    }

    private fun renderJelloEffect(entity: Entity, x: Double, y: Double, z: Double, time: Double) {
        // Calculate squish factors based on time
        val squishY = 1.0 + sin(time * 2.0) * 0.2 * intensity
        val squishXZ = 1.0 + cos(time) * 0.1 * intensity

        // Move to entity position
        GlStateManager.translate(x, y, z)

        // Apply scaling for jelly effect
        GlStateManager.scale(squishXZ * scale, squishY * scale, squishXZ * scale)

        // Render a simple sphere-like shape
        val radius = 0.5
        val slices = 16
        val stacks = 16

        // Draw the jelly body
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP)
        for (i in 0..stacks) {
            val phi = PI * i / stacks
            for (j in 0..slices) {
                val theta = 2.0 * PI * j / slices

                val x1 = radius * sin(phi) * cos(theta)
                val y1 = radius * cos(phi)
                val z1 = radius * sin(phi) * sin(theta)

                val x2 = radius * sin(phi + PI / stacks) * cos(theta)
                val y2 = radius * cos(phi + PI / stacks)
                val z2 = radius * sin(phi + PI / stacks) * sin(theta)

                GL11.glVertex3d(x1, y1, z1)
                GL11.glVertex3d(x2, y2, z2)
            }
        }
        GL11.glEnd()

        // Draw outline
        GlStateManager.glLineWidth(1.5f)
        GL11.glBegin(GL11.GL_LINE_LOOP)
        for (i in 0..360 step 10) {
            val angle = i * PI / 180.0
            GL11.glVertex3d(
                radius * cos(angle),
                radius * sin(time) * 0.2,
                radius * sin(angle)
            )
        }
        GL11.glEnd()
    }
}