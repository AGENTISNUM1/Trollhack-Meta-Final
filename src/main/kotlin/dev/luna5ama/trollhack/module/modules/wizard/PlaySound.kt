package dev.luna5ama.trollhack.module.modules.wizard

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.threads.onMainThread
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundEvent

internal object PlaySound : Module(
    name = "PlaySound",
    description = "Plays the fortnite sound",
    category = Category.META
) {
    init {
        onEnable {
            playSound()
            disable()
        }
    }

    private fun playSound() {
        onMainThread {
            mc.player?.playSound(sound, 100.0f, 1.0f)
        }
    }

    private val sound: SoundEvent
        get() = SoundEvent(ResourceLocation("trollhack", "fortnite"))
}