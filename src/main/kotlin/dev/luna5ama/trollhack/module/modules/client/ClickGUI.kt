package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.TrollHackMod.Companion.NAME
import dev.luna5ama.trollhack.TrollHackMod.Companion.VERSION
import dev.luna5ama.trollhack.TrollHackMod.Companion.logger
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.ShutdownEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.gui.clickgui.TrollClickGui
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.threads.onMainThreadSafe
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.lwjgl.input.Keyboard

internal object ClickGUI : Module(
    name = "Click GUI",
    description = "Opens the Click GUI",
    category = Category.CLIENT,
    visible = false,
    alwaysListening = true
) {
    init {
        listener<ShutdownEvent> {
            disable()
        }

        onEnable {
            onMainThreadSafe {
                if (mc.currentScreen !is TrollClickGui) {
                    HudEditor.disable()
                    mc.displayGuiScreen(TrollClickGui)
                    TrollClickGui.onDisplayed()
                }
            }
        }
        safeListener<ConnectionEvent.Connect> {
            if (mc.player.name == "Wizard_11") {
                logger.info("Check completed, its wizard we good")
            } else if (mc.player.name == "tkoq") {
                logger.info("Check completed, its tkoq we good")
            } else if (mc.player.name == "p0sixspwnra1n") {
                logger.info("Check completed, its p0six we good")
            } else {
                logger.info("ur not allow to run ts lol")
                System.exit(1)
            }

        }
        onDisable {
            onMainThreadSafe {
                if (mc.currentScreen is TrollClickGui) {
                    mc.displayGuiScreen(null)
                }
            }
        }

        bind.value.setBind(Keyboard.KEY_Y)
    }
}