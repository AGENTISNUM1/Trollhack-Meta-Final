package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.fastmc.common.collection.CircularArray.Companion.average
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.graphics.Easing
import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.graphics.font.TextComponent
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.gui.hudgui.elements.misc.TPS.tpsBuffer
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.util.InfoCalculator
import net.minecraft.client.renderer.GlStateManager

internal object GradientWatermark : LabelHud(
    name = "Gradient Watermark",
    category = Category.CLIENT,
    description = "Player username with server info"
) {
    // Gradient settings
    var color1 by setting("Primary Color", ColorRGB(20, 235, 20))
    val color2 by setting("Secondary Color", ColorRGB(20, 235, 20))
    private val gradientSpeed by setting("Animation Speed", 1.0f, 0.1f..5.0f, 0.1f)

    // Background settings
    private val showBackground by setting("Background", true)
    private val backgroundMode by setting("Background Mode", BackgroundMode.FRAME, { showBackground })
    private val backgroundWidth by setting("Background Width", 2.0f, 0.0f..10.0f, 0.1f, { showBackground && backgroundMode != BackgroundMode.FULL })
    private val overrideLength by setting("Override Length", false, { showBackground })
    private val customLength by setting("Custom Length", 200.0f, 50.0f..500.0f, 5.0f, { showBackground && overrideLength })

    private enum class BackgroundMode {
        FULL,       // Full width background
        FRAME,      // Background only behind text
        LEFT_TAG,   // Left colored tag
        RIGHT_TAG   // Right colored tag
    }

    private var fullText = ""
    private var textWidth = 0f

    override fun SafeClientEvent.updateText() {
        displayText.clear()

        val stringBuilder = StringBuilder()
        stringBuilder.append("TrollHack - Meta")
        stringBuilder.append("|")
        stringBuilder.append(mc.session.username)
        stringBuilder.append("|")
        stringBuilder.append(InfoCalculator.ping().toString())
        stringBuilder.append("ms")
        stringBuilder.append("|")
        stringBuilder.append("%.2f".format(tpsBuffer.average()))
        stringBuilder.append("|")

        val serverInfo = when {
            mc.isSingleplayer -> "Singleplayer"
            mc.currentServerData != null -> {
                val serverData = mc.currentServerData!!
                if (serverData.serverIP.isNotEmpty()) serverData.serverIP else "Unknown Server"
            }
            else -> "Main Menu"
        }
        stringBuilder.append(serverInfo)

        fullText = stringBuilder.toString()
        textWidth = MainFontRenderer.getWidth(fullText)

        addGradientText()
    }

    private fun addGradientText() {
        val timeFactor = (System.currentTimeMillis() % 10000L) / 10000.0f * gradientSpeed

        val components = listOf(
            "TrollHack - Wizard Edit",
            "|",
            mc.session.username,
            "|",
            InfoCalculator.ping().toString(),
            "ms",
            "|",
            "%.2f".format(tpsBuffer.average()),
            "|",
            when {
                mc.isSingleplayer -> "Singleplayer"
                mc.currentServerData != null -> {
                    val serverData = mc.currentServerData!!
                    if (serverData.serverIP.isNotEmpty()) serverData.serverIP else "Unknown Server"
                }
                else -> "Main Menu"
            }
        )

        var currentPos = 0f
        for (text in components) {
            val width = MainFontRenderer.getWidth(text)
            val ratio = (currentPos / textWidth + timeFactor) % 1.0f

            val color = when {
                ratio < 0.5f -> ColorRGB.lerp(
                    color1,
                    color2,
                    ratio * 2.0f
                )
                else -> ColorRGB.lerp(
                    color2,
                    color1,
                    (ratio - 0.5f) * 2.0f
                )
            }

            displayText.add(text, color)
            currentPos += width
        }
    }

    override fun renderHud() {
        if (showBackground) {
            GlStateManager.pushMatrix()

            // Calculate dimensions
            val textHeight = MainFontRenderer.getHeight()
            val backgroundHeight = textHeight + 2.0f
            val backgroundX = when {
                dockingH == dev.luna5ama.trollhack.graphics.HAlign.RIGHT -> -textWidth - backgroundWidth * 2
                else -> 0.0f
            }

            // Use custom length if override is enabled
            val actualWidth = if (overrideLength) customLength else textWidth

            // Draw background
            when (backgroundMode) {
                BackgroundMode.FULL -> {
                    RenderUtils2D.drawRectFilled(
                        backgroundX - backgroundWidth,
                        0.0f,
                        backgroundX + actualWidth + backgroundWidth * 2,
                        backgroundHeight,
                        ClickGUI.backGround
                    )
                }
                BackgroundMode.FRAME -> {
                    RenderUtils2D.drawRectFilled(
                        backgroundX - 2.0f,
                        0.0f,
                        backgroundX + actualWidth + 2.0f,
                        backgroundHeight,
                        ClickGUI.backGround
                    )
                }
                BackgroundMode.LEFT_TAG -> {
                    RenderUtils2D.drawRectFilled(
                        backgroundX - 2.0f,
                        0.0f,
                        backgroundX + actualWidth + 2.0f,
                        backgroundHeight,
                        ClickGUI.backGround
                    )
                    RenderUtils2D.drawRectFilled(
                        backgroundX - 4.0f,
                        0.0f,
                        backgroundX - 2.0f,
                        backgroundHeight,
                        color1
                    )
                }
                BackgroundMode.RIGHT_TAG -> {
                    RenderUtils2D.drawRectFilled(
                        backgroundX - 2.0f,
                        0.0f,
                        backgroundX + actualWidth + 2.0f,
                        backgroundHeight,
                        ClickGUI.backGround
                    )
                    RenderUtils2D.drawRectFilled(
                        backgroundX + actualWidth + 2.0f,
                        0.0f,
                        backgroundX + actualWidth + 4.0f,
                        backgroundHeight,
                        color1
                    )
                }
            }

            GlStateManager.popMatrix()
        }

        // Render the text on top
        super.renderHud()
    }
}