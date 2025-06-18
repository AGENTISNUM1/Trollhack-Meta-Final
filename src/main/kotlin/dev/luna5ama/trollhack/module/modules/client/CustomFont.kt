package dev.luna5ama.trollhack.module.modules.client

import dev.fastmc.common.TimeUnit
import dev.fastmc.common.ceilToInt
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.graphics.font.GlyphCache
import dev.luna5ama.trollhack.graphics.font.renderer.MainFontRenderer
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.delegate.AsyncCachedValue
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.onMainThread
import org.lwjgl.opengl.Display
import java.awt.Font
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object CustomFont : Module(
    name = "Font Settings",
    description = "Custom font settings for the client",
    visible = false,
    category = Category.CLIENT,
    alwaysEnabled = true
) {
    val availableFonts = listOf("Orbitron", "Underdog", "WinkySans", "Gidole", "Geo", "Comic", "Queen")

    val fontName = setting("Font", "Orbitron")
    val overrideMinecraft by setting("Override Minecraft", false)
    var animatedTitle by setting("Animated Title", false)
    private val sizeSetting = setting("Size", 1.0f, 0.5f..2.0f, 0.05f)
    private val charGapSetting = setting("Char Gap", 0.0f, -10f..10f, 0.5f)
    private val lineSpaceSetting = setting("Line Space", 0.0f, -10f..10f, 0.05f)
    private val baselineOffsetSetting = setting("Baseline Offset", 0.0f, -10.0f..10.0f, 0.05f)
    private val lodBiasSetting = setting("Lod Bias", 0.0f, -10.0f..10.0f, 0.05f)

    // Animated title variables
    private const val FULL_TITLE = "${TrollHackMod.NAME} ${TrollHackMod.VERSION}"
    private enum class AnimationState { TYPING, DISPLAYING, BACKSPACING, WAITING }
    private var animationState = AnimationState.TYPING
    private var currentTitle = ""
    private var tickCounter = 0
    private const val TYPE_DELAY = 2 // ticks between characters
    private const val DISPLAY_TIME = 40 // ticks (2 seconds at 20tps)
    private const val WAIT_TIME = 20 // ticks (1 second)
    private lateinit var titleField: Field
    private lateinit var displayImpl: Any
    private lateinit var setTitleMethod: Method
    private var initialized = false

    val isDefaultFont get() = fontName.value.equals("Orbitron", true)
    val size get() = sizeSetting.value * 0.140625f
    val charGap get() = charGapSetting.value * 0.5f
    val lineSpace get() = size * (lineSpaceSetting.value * 0.05f + 0.75f)
    val lodBias get() = lodBiasSetting.value * 0.25f - 0.5375f
    val baselineOffset get() = baselineOffsetSetting.value * 2.0f - 9.5f

    init {
        listener<TickEvent.Post>(true) {
            // Font validation
            if (fontName.value !in availableFonts) {
                fontName.value = "Orbitron"
                NoSpamMessage.sendMessage("$chatName Invalid font! Please use ${ClientSettings.prefix}fonts for a list of fonts.")
            }

            // Minecraft font override
            mc.fontRenderer.FONT_HEIGHT = if (overrideMinecraft) {
                MainFontRenderer.getHeight().ceilToInt()
            } else {
                9
            }

            // Animated title
            if (animatedTitle) {
                if (!initialized) {
                    initializeTitleAnimation()
                    initialized = true
                }
                updateAnimatedTitle()
            } else if (initialized) {
                // Reset title when animation is disabled
                updateTitle(FULL_TITLE)
                initialized = false
            }
        }
    }

    init {
        fontName.valueListeners.add { prev, it ->
            if (prev == it) return@add
            GlyphCache.delete(Font(prev, Font.PLAIN, 64))
            onMainThread {
                MainFontRenderer.reloadFonts()
            }
        }
    }

    private fun initializeTitleAnimation() {
        try {
            val displayClazz = Display::class.java
            titleField = displayClazz.getDeclaredField("title")

            val displayImplField = displayClazz.getDeclaredField("display_impl")
            displayImplField.isAccessible = true
            displayImpl = displayImplField.get(null)
            displayImplField.isAccessible = false

            val displayImplClass = Class.forName("org.lwjgl.opengl.DisplayImplementation")
            setTitleMethod = displayImplClass.getDeclaredMethod("setTitle", String::class.java)
        } catch (e: Exception) {
            NoSpamMessage.sendMessage("$chatName Failed to initialize animated title: ${e.message}")
            animatedTitle = false
        }
    }

    private fun updateAnimatedTitle() {
        when (animationState) {
            AnimationState.TYPING -> {
                if (tickCounter % TYPE_DELAY == 0) {
                    if (currentTitle.length < FULL_TITLE.length) {
                        currentTitle = FULL_TITLE.substring(0, currentTitle.length + 1)
                        updateTitle(currentTitle)
                    } else {
                        animationState = AnimationState.DISPLAYING
                        tickCounter = 0
                    }
                }
                tickCounter++
            }

            AnimationState.DISPLAYING -> {
                if (tickCounter++ >= DISPLAY_TIME) {
                    animationState = AnimationState.BACKSPACING
                    tickCounter = 0
                }
            }

            AnimationState.BACKSPACING -> {
                if (tickCounter % TYPE_DELAY == 0) {
                    if (currentTitle.isNotEmpty()) {
                        currentTitle = currentTitle.substring(0, currentTitle.length - 1)
                        updateTitle(currentTitle)
                    } else {
                        animationState = AnimationState.WAITING
                        tickCounter = 0
                    }
                }
                tickCounter++
            }

            AnimationState.WAITING -> {
                if (tickCounter++ >= WAIT_TIME) {
                    animationState = AnimationState.TYPING
                    tickCounter = 0
                }
            }
        }
    }

    private fun updateTitle(newTitle: String) {
        try {
            titleField.isAccessible = true
            titleField.set(null, newTitle)
            titleField.isAccessible = false

            setTitleMethod.isAccessible = true
            setTitleMethod.invoke(displayImpl, newTitle)
            setTitleMethod.isAccessible = false
        } catch (t: Throwable) {
            // Error handling
        }
    }
}