package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import com.mojang.text2speech.Narrator;
import dev.luna5ama.trollhack.event.events.ModuleToggleEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.modules.combat.BedAura

internal object Narrator : Module(
    name = "Narrator",
    category = Category.META,
    description = "narrates"
) {
    private final val narrator = Narrator.getNarrator()
    private val moduletoggles by setting("Narrate Toggles", true)

    init{
        onEnable {
            narrator.clear()
        }
        onDisable {
            narrator.clear()
            narrator.say("Shutdown complete. Goodbye")
        }
        safeListener<ModuleToggleEvent> {
            if (moduletoggles) {
                if (it.module == BedAura && !BedAura.isEnabled) {
                    narrator.say("Bedaura engaged!")
                } else if (it.module == BetterPot && !BetterPot.isEnabled) {
                    narrator.say("Self Preservation Unit engaged.")
                } else if (it.module == BedAura && BedAura.isEnabled) {
                    narrator.say("Bedaura disengaged.")
                } else {
                    narrator.say("${it.module.name} toggled")
                }
            }
        }
    }
}