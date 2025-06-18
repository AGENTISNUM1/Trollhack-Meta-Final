package dev.luna5ama.trollhack.graphics

import dev.fastmc.common.toRadians
import dev.luna5ama.trollhack.graphics.buffer.PersistentMappedVBO
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.shaders.Shader
import dev.luna5ama.trollhack.structs.Pos2Color
import dev.luna5ama.trollhack.structs.Pos3Color
import dev.luna5ama.trollhack.structs.sizeof
import dev.luna5ama.trollhack.util.Wrapper
import dev.luna5ama.trollhack.util.math.MathUtils
import dev.luna5ama.trollhack.util.math.vector.Vec2f
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL30.glBindVertexArray
import kotlin.math.*

/**
 * Utils for basic 2D shapes rendering with improved rounded corners
 */
object RenderUtils2D {
    val mc = Wrapper.minecraft
    var vertexSize = 0

    // Minimum segments for a quarter circle (90 degrees)
    private const val MIN_SEGMENTS = 8
    // Maximum segments for a full circle (360 degrees)
    private const val MAX_SEGMENTS = 64

    fun drawItem(itemStack: ItemStack, x: Int, y: Int, text: String? = null, drawOverlay: Boolean = true) {
        glUseProgram(0)
        GlStateUtils.blend(true)
        GlStateUtils.depth(true)
        RenderHelper.enableGUIStandardItemLighting()

        mc.renderItem.zLevel = 0.0f
        mc.renderItem.renderItemAndEffectIntoGUI(itemStack, x, y)
        if (drawOverlay) mc.renderItem.renderItemOverlayIntoGUI(mc.fontRenderer, itemStack, x, y, text)
        mc.renderItem.zLevel = 0.0f

        RenderHelper.disableStandardItemLighting()
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)

        GlStateUtils.depth(false)
        GlStateUtils.texture2d(true)
    }

    fun drawCircleOutline(
        center: Vec2f = Vec2f.ZERO,
        radius: Float,
        segments: Int = 0,
        lineWidth: Float = 1f,
        color: ColorRGB
    ) {
        drawArcOutline(center, radius, Pair(0f, 360f), segments, lineWidth, color)
    }

    fun drawCircleFilled(center: Vec2f = Vec2f.ZERO, radius: Float, segments: Int = 0, color: ColorRGB) {
        drawArcFilled(center, radius, Pair(0f, 360f), segments, color)
    }

    fun drawArcOutline(
        center: Vec2f = Vec2f.ZERO,
        radius: Float,
        angleRange: Pair<Float, Float>,
        segments: Int = 0,
        lineWidth: Float = 1f,
        color: ColorRGB
    ) {
        val arcVertices = getArcVertices(center, radius, angleRange, segments)
        drawLineStrip(arcVertices, lineWidth, color)
    }

    fun drawArcFilled(
        center: Vec2f = Vec2f.ZERO,
        radius: Float,
        angleRange: Pair<Float, Float>,
        segments: Int = 0,
        color: ColorRGB
    ) {
        val arcVertices = getArcVertices(center, radius, angleRange, segments)
        drawTriangleFan(center, arcVertices, color)
    }

    fun drawRectOutline(width: Float, height: Float, lineWidth: Float = 1.0f, color: ColorRGB) {
        drawRectOutline(0.0f, 0.0f, width, height, lineWidth, color)
    }

    fun drawRoundedRectFilled(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: ColorRGB,
        segments: Int = 0
    ) {
        if (radius <= 0f) {
            drawRectFilled(x, y, x + width, y + height, color)
            return
        }

        val clampedRadius = min(radius, min(width, height) / 2f)
        val right = x + width
        val bottom = y + height

        // Calculate the number of segments for each quarter circle based on radius
        val seg = if (segments > 0) segments else calculateOptimalSegments(clampedRadius)

        // Draw the main rectangular areas
        drawRectFilled(
            x + clampedRadius, y,
            right - clampedRadius, bottom,
            color
        )
        drawRectFilled(
            x, y + clampedRadius,
            x + clampedRadius, bottom - clampedRadius,
            color
        )
        drawRectFilled(
            right - clampedRadius, y + clampedRadius,
            right, bottom - clampedRadius,
            color
        )

        // Draw the four rounded corners with optimized segments
        drawArcFilled(
            Vec2f(x + clampedRadius, y + clampedRadius),
            clampedRadius,
            Pair(180f, 270f),
            seg,
            color
        )
        drawArcFilled(
            Vec2f(right - clampedRadius, y + clampedRadius),
            clampedRadius,
            Pair(270f, 360f),
            seg,
            color
        )
        drawArcFilled(
            Vec2f(x + clampedRadius, bottom - clampedRadius),
            clampedRadius,
            Pair(90f, 180f),
            seg,
            color
        )
        drawArcFilled(
            Vec2f(right - clampedRadius, bottom - clampedRadius),
            clampedRadius,
            Pair(0f, 90f),
            seg,
            color
        )
    }

    fun drawRoundedRectOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        lineWidth: Float = 1f,
        color: ColorRGB,
        segments: Int = 0
    ) {
        if (radius <= 0f) {
            drawRectOutline(x, y, x + width, y + height, lineWidth, color)
            return
        }

        val clampedRadius = min(radius, min(width, height) / 2f)
        val right = x + width
        val bottom = y + height
        val seg = if (segments > 0) segments else calculateOptimalSegments(clampedRadius)

        // Draw the straight lines
        drawLine(Vec2f(x + clampedRadius, y), Vec2f(right - clampedRadius, y), lineWidth, color) // Top
        drawLine(Vec2f(right, y + clampedRadius), Vec2f(right, bottom - clampedRadius), lineWidth, color) // Right
        drawLine(Vec2f(x + clampedRadius, bottom), Vec2f(right - clampedRadius, bottom), lineWidth, color) // Bottom
        drawLine(Vec2f(x, y + clampedRadius), Vec2f(x, bottom - clampedRadius), lineWidth, color) // Left

        // Draw the rounded corners with optimized segments
        drawArcOutline(
            Vec2f(x + clampedRadius, y + clampedRadius),
            clampedRadius,
            Pair(180f, 270f),
            seg,
            lineWidth,
            color
        )
        drawArcOutline(
            Vec2f(right - clampedRadius, y + clampedRadius),
            clampedRadius,
            Pair(270f, 360f),
            seg,
            lineWidth,
            color
        )
        drawArcOutline(
            Vec2f(x + clampedRadius, bottom - clampedRadius),
            clampedRadius,
            Pair(90f, 180f),
            seg,
            lineWidth,
            color
        )
        drawArcOutline(
            Vec2f(right - clampedRadius, bottom - clampedRadius),
            clampedRadius,
            Pair(0f, 90f),
            seg,
            lineWidth,
            color
        )
    }
    fun drawGradientRectHorizontal(
        x1: Float, y1: Float, x2: Float, y2: Float,
        startColor: ColorRGB, endColor: ColorRGB
    ) {
        prepareGL()
        putVertex(x1, y1, startColor)
        putVertex(x2, y1, startColor)
        putVertex(x2, y2, endColor)
        putVertex(x1, y2, endColor)
        draw(GL_QUADS)
        releaseGL()
    }

    fun drawGradientRectVertical(
        x1: Float, y1: Float, x2: Float, y2: Float,
        startColor: ColorRGB, endColor: ColorRGB
    ) {
        prepareGL()
        putVertex(x1, y1, startColor)
        putVertex(x2, y1, endColor)
        putVertex(x2, y2, endColor)
        putVertex(x1, y2, startColor)
        draw(GL_QUADS)
        releaseGL()
    }
    fun drawTopRoundedRectFilled(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: ColorRGB,
        segments: Int = 0
    ) {
        if (radius <= 0f) {
            drawRectFilled(x, y, x + width, y + height, color)
            return
        }

        val clampedRadius = min(radius, min(width, height) / 2f)
        val right = x + width
        val bottom = y + height
        val seg = if (segments > 0) segments else calculateOptimalSegments(clampedRadius)

        // Draw main rectangular area
        drawRectFilled(x, y + clampedRadius, right, bottom, color)

        // Draw top rectangular area between rounded corners
        drawRectFilled(x + clampedRadius, y, right - clampedRadius, y + clampedRadius, color)

        // Draw the two top rounded corners
        drawArcFilled(
            Vec2f(x + clampedRadius, y + clampedRadius),
            clampedRadius,
            Pair(180f, 270f),
            seg,
            color
        )
        drawArcFilled(
            Vec2f(right - clampedRadius, y + clampedRadius),
            clampedRadius,
            Pair(270f, 360f),
            seg,
            color
        )
    }

    fun drawBottomRoundedRectFilled(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        color: ColorRGB,
        segments: Int = 0
    ) {
        if (radius <= 0f) {
            drawRectFilled(x, y, x + width, y + height, color)
            return
        }

        val clampedRadius = min(radius, min(width, height) / 2f)
        val right = x + width
        val bottom = y + height
        val seg = if (segments > 0) segments else calculateOptimalSegments(clampedRadius)

        // Draw main rectangular area
        drawRectFilled(x, y, right, bottom - clampedRadius, color)

        // Draw bottom rectangular area between rounded corners
        drawRectFilled(x + clampedRadius, bottom - clampedRadius, right - clampedRadius, bottom, color)

        // Draw the two bottom rounded corners
        drawArcFilled(
            Vec2f(x + clampedRadius, bottom - clampedRadius),
            clampedRadius,
            Pair(90f, 180f),
            seg,
            color
        )
        drawArcFilled(
            Vec2f(right - clampedRadius, bottom - clampedRadius),
            clampedRadius,
            Pair(0f, 90f),
            seg,
            color
        )
    }

    fun drawTopRoundedRectOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        lineWidth: Float = 1f,
        color: ColorRGB,
        segments: Int = 0
    ) {
        if (radius <= 0f) {
            drawRectOutline(x, y, x + width, y + height, lineWidth, color)
            return
        }

        val clampedRadius = min(radius, min(width, height) / 2f)
        val right = x + width
        val bottom = y + height
        val seg = if (segments > 0) segments else calculateOptimalSegments(clampedRadius)

        // Draw straight lines
        drawLine(Vec2f(x + clampedRadius, y), Vec2f(right - clampedRadius, y), lineWidth, color) // Top
        drawLine(Vec2f(right, y + clampedRadius), Vec2f(right, bottom), lineWidth, color) // Right
        drawLine(Vec2f(x, bottom), Vec2f(right, bottom), lineWidth, color) // Bottom
        drawLine(Vec2f(x, y + clampedRadius), Vec2f(x, bottom), lineWidth, color) // Left

        // Draw top rounded corners
        drawArcOutline(
            Vec2f(x + clampedRadius, y + clampedRadius),
            clampedRadius,
            Pair(180f, 270f),
            seg,
            lineWidth,
            color
        )
        drawArcOutline(
            Vec2f(right - clampedRadius, y + clampedRadius),
            clampedRadius,
            Pair(270f, 360f),
            seg,
            lineWidth,
            color
        )
    }

    fun drawBottomRoundedRectOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        lineWidth: Float = 1f,
        color: ColorRGB,
        segments: Int = 0
    ) {
        if (radius <= 0f) {
            drawRectOutline(x, y, x + width, y + height, lineWidth, color)
            return
        }

        val clampedRadius = min(radius, min(width, height) / 2f)
        val right = x + width
        val bottom = y + height
        val seg = if (segments > 0) segments else calculateOptimalSegments(clampedRadius)

        // Draw straight lines
        drawLine(Vec2f(x, y), Vec2f(right, y), lineWidth, color) // Top
        drawLine(Vec2f(right, y), Vec2f(right, bottom - clampedRadius), lineWidth, color) // Right
        drawLine(Vec2f(x + clampedRadius, bottom), Vec2f(right - clampedRadius, bottom), lineWidth, color) // Bottom
        drawLine(Vec2f(x, y), Vec2f(x, bottom - clampedRadius), lineWidth, color) // Left

        // Draw bottom rounded corners
        drawArcOutline(
            Vec2f(x + clampedRadius, bottom - clampedRadius),
            clampedRadius,
            Pair(90f, 180f),
            seg,
            lineWidth,
            color
        )
        drawArcOutline(
            Vec2f(right - clampedRadius, bottom - clampedRadius),
            clampedRadius,
            Pair(0f, 90f),
            seg,
            lineWidth,
            color
        )
    }

    fun drawRectOutline(x1: Float, y1: Float, x2: Float, y2: Float, lineWidth: Float = 1.0f, color: ColorRGB) {
        prepareGL()
        GlStateManager.glLineWidth(lineWidth)

        putVertex(x1, y2, color)
        putVertex(x1, y1, color)
        putVertex(x2, y1, color)
        putVertex(x2, y2, color)
        draw(GL_LINE_LOOP)

        releaseGL()
    }

    fun drawRectFilled(width: Float, height: Float, color: ColorRGB) {
        drawRectFilled(0.0f, 0.0f, width, height, color)
    }

    fun drawRectFilled(x1: Float, y1: Float, x2: Float, y2: Float, color: ColorRGB) {
        prepareGL()

        putVertex(x1, y2, color)
        putVertex(x2, y2, color)
        putVertex(x2, y1, color)
        putVertex(x1, y1, color)

        draw(GL_QUADS)

        releaseGL()
    }

    fun drawQuad(pos1: Vec2f, pos2: Vec2f, pos3: Vec2f, pos4: Vec2f, color: ColorRGB) {
        prepareGL()

        putVertex(pos1, color)
        putVertex(pos2, color)
        putVertex(pos4, color)
        putVertex(pos3, color)

        draw(GL_TRIANGLE_STRIP)

        releaseGL()
    }

    fun drawTriangleOutline(pos1: Vec2f, pos2: Vec2f, pos3: Vec2f, lineWidth: Float = 1f, color: ColorRGB) {
        val vertices = arrayOf(pos1, pos2, pos3)
        drawLineLoop(vertices, lineWidth, color)
    }

    fun drawTriangleFilled(pos1: Vec2f, pos2: Vec2f, pos3: Vec2f, color: ColorRGB) {
        prepareGL()

        putVertex(pos1, color)
        putVertex(pos2, color)
        putVertex(pos3, color)
        draw(GL_TRIANGLES)

        releaseGL()
    }

    fun drawTriangleFan(center: Vec2f, vertices: Array<Vec2f>, color: ColorRGB) {
        prepareGL()

        putVertex(center, color)
        for (vertex in vertices) {
            putVertex(vertex, color)
        }
        draw(GL_TRIANGLE_FAN)

        releaseGL()
    }


    fun drawTriangleStrip(vertices: Array<Vec2f>, color: ColorRGB) {
        prepareGL()

        for (vertex in vertices) {
            putVertex(vertex, color)
        }
        draw(GL_TRIANGLE_STRIP)

        releaseGL()
    }

    fun drawLineLoop(vertices: Array<Vec2f>, lineWidth: Float = 1f, color: ColorRGB) {
        prepareGL()
        GlStateManager.glLineWidth(lineWidth)

        for (vertex in vertices) {
            putVertex(vertex, color)
        }
        draw(GL_LINE_LOOP)

        releaseGL()
        GlStateManager.glLineWidth(1f)
    }

    fun drawLineStrip(vertices: Array<Vec2f>, lineWidth: Float = 1f, color: ColorRGB) {
        prepareGL()
        GlStateManager.glLineWidth(lineWidth)

        for (vertex in vertices) {
            putVertex(vertex, color)
        }
        draw(GL_LINE_STRIP)

        releaseGL()
        GlStateManager.glLineWidth(1f)
    }

    fun drawLine(posBegin: Vec2f, posEnd: Vec2f, lineWidth: Float = 1f, color: ColorRGB) {
        prepareGL()
        GlStateManager.glLineWidth(lineWidth)

        putVertex(posBegin, color)
        putVertex(posEnd, color)
        draw(GL_LINES)

        releaseGL()
        GlStateManager.glLineWidth(1f)
    }

    fun putVertex(pos: Vec2f, color: ColorRGB) {
        putVertex(pos.x, pos.y, color)
    }

    fun putVertex(posX: Float, posY: Float, color: ColorRGB) {
        val array = PersistentMappedVBO.arr
        val struct = Pos2Color(array)
        struct.pos.x = posX
        struct.pos.y = posY
        struct.color = color.rgba
        array += sizeof(Pos3Color)
        vertexSize++
    }

    fun draw(mode: Int) {
        if (vertexSize == 0) return

        DrawShader.bind()
        glBindVertexArray(PersistentMappedVBO.POS2_COLOR)
        glDrawArrays(mode, PersistentMappedVBO.drawOffset, vertexSize)
        PersistentMappedVBO.end()
        glBindVertexArray(0)

        vertexSize = 0
    }

    private fun getArcVertices(
        center: Vec2f,
        radius: Float,
        angleRange: Pair<Float, Float>,
        segments: Int
    ): Array<Vec2f> {
        val range = max(angleRange.first, angleRange.second) - min(angleRange.first, angleRange.second)
        val seg = if (segments > 0) segments else calculateOptimalSegments(radius, range)
        val segAngle = (range / seg.toFloat())

        return Array(seg + 1) {
            val angle = (it * segAngle + angleRange.first).toRadians()
            Vec2f(
                center.x + radius * sin(angle),
                center.y - radius * cos(angle)
            )
        }
    }

    /**
     * Calculates optimal number of segments for a smooth circle/arc based on radius and angle range
     */
    private fun calculateOptimalSegments(radius: Float, angleRange: Float = 360f): Int {
        // Base segments on radius and angle range
        val segments = (radius * 0.5f * PI * (angleRange / 360f)).roundToInt()

        // Scale between MIN_SEGMENTS and MAX_SEGMENTS based on the angle range proportion
        val rangeProportion = angleRange / 360f
        val minSeg = max((MIN_SEGMENTS * rangeProportion).roundToInt(), 4)
        val maxSeg = max((MAX_SEGMENTS * rangeProportion).roundToInt(), 16)

        return segments.coerceIn(minSeg, maxSeg)
    }

    fun prepareGL() {
        GlStateUtils.alpha(false)
        GlStateUtils.blend(true)
        GlStateUtils.lineSmooth(true)
        GlStateUtils.cull(false)
    }

    fun releaseGL() {
        GlStateUtils.lineSmooth(false)
        GlStateUtils.cull(true)
    }

    private object DrawShader :
        Shader("/assets/trollhack/shaders/general/Pos2Color.vsh", "/assets/trollhack/shaders/general/Pos2Color.fsh")
}