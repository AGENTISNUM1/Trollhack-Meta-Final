package dev.luna5ama.trollhack.module.modules.client

import dev.fastmc.common.TickTimer
import dev.luna5ama.trollhack.TrollHackMod.Companion.logger
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.ShutdownEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.gui.clickgui.TrollClickGui
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.delegate.FrameFloat
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import org.lwjgl.input.Keyboard
import kotlin.math.round
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader

internal object ClickGUI : Module(
    name = "ClickGui",
    description = "GUI",
    visible = false,
    category = Category.CLIENT,
    alwaysListening = true
) {
    private val scaleSetting = setting("Scale", 100, 50..400, 5)
    val particle = false
    val backGroundBlur by setting("Background Blur", 0.0f, 0.0f..1.0f, 0.05f)
    val windowOutline by setting("Window Outline", true)
    val titleBar by setting("Title Bar", false)
    val windowBlurPass by setting("Window Blur Pass", 2, 0..10, 1)
    val xMargin by setting("X Margin", 4.0f, 0.0f..10.0f, 0.5f)
    val yMargin by setting("Y Margin", 1.0f, 0.0f..10.0f, 0.5f)
    val darkness by setting("Darkness", 0.25f, 0.0f..1.0f, 0.05f)
    val fadeInTime by setting("Fade In Time", 0.4f, 0.0f..1.0f, 0.05f)
    val fadeOutTime by setting("Fade Out Time", 0.4f, 0.0f..1.0f, 0.05f)
    var primarySetting by setting("Primary Color", ColorRGB(255, 140, 180, 220))
    var backgroundSetting by setting("Background Color", ColorRGB(40, 32, 36, 160))
    val radius by setting("Radius", 1.7f, 0.3f..2.9f, 0.1f)
    private val textSetting by setting("Text Color", ColorRGB(255, 250, 253, 255))
    private val aHover by setting("Hover Alpha", 32, 0..255, 1)


    val primary get() = primarySetting
    val idle get() = if (primary.lightness < 0.9f) ColorRGB(255, 255, 255, 0) else ColorRGB(0, 0, 0, 0)
    val hover get() = idle.alpha(aHover)
    val click get() = idle.alpha(aHover * 2)
    val backGround get() = backgroundSetting
    val text get() = textSetting

    private var prevScale = scaleSetting.value / 100.0f
    private var scale = prevScale
    private val settingTimer = TickTimer()

    fun resetScale() {
        scaleSetting.value = 100
        prevScale = 1.0f
        scale = 1.0f
    }

    val scaleFactor by FrameFloat { (prevScale + (scale - prevScale) * mc.renderPartialTicks) * 2.0f }

    private fun getRoundedScale(): Float {
        return round((scaleSetting.value / 100.0f) / 0.1f) * 0.1f
    }

    private fun fetchAllowedUsernames(): List<String> {
        return try {
            val url = URL("https://gist.githubusercontent.com/AGENTISNUM1/b7bb0c2df60491a91b355bf0c93266d5/raw/40658d22b2de8f2eb536cb7e95efb382f88d340a/auth.txt")
            val connection = url.openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            BufferedReader(InputStreamReader(connection.getInputStream())).use { reader ->
                reader.readLines().map { it.trim() }.filter { it.isNotBlank() }
            }
        } catch (e: Exception) {
            logger.warn("Failed to fetch allowed usernames: ${e.message}")
            System.exit(1)
            emptyList()
        }
    }

    init {
        onEnable {
            onMainThreadSafe {
                if (mc.currentScreen !is TrollClickGui) {
                    HudEditor.disable()
                    mc.displayGuiScreen(TrollClickGui)
                    TrollClickGui.onDisplayed()
                }
                val allowedUsernames = fetchAllowedUsernames()
                if (allowedUsernames.contains(mc.player.name)) {
                    logger.info("auth check done, its just ${mc.player.name} we good")
                } else {
                    logger.warn("hey! ${mc.player.name} ur not allowed to use this!")
                    logger.warn("actually im curious, who leaked/gave this to u?")
                    System.exit(1)
                }
            }

        }
        onDisable {
            onMainThreadSafe {
                if (mc.currentScreen is TrollClickGui) {
                    mc.displayGuiScreen(null)
                }
            }
        }
        safeParallelListener<TickEvent.Post> {
            prevScale = scale
            if (settingTimer.tick(500L)) {
                val diff = scale - getRoundedScale()
                when {
                    diff < -0.025 -> scale += 0.025f
                    diff > 0.025 -> scale -= 0.025f
                    else -> scale = getRoundedScale()
                }
            }
        }
        listener<ShutdownEvent> {
            disable()
        }
        bind.value.setBind(Keyboard.KEY_Y)

        scaleSetting.listeners.add {
            settingTimer.reset()
        }
    }
}