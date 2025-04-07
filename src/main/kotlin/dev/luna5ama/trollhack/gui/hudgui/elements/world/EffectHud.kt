package dev.luna5ama.trollhack.gui.hudgui.elements.world

import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.gui.hudgui.LabelHud
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import net.minecraft.potion.PotionEffect
import java.util.*

internal object EffectHud : LabelHud(
    name = "EffectHud",
    category = Category.WORLD,
    description = "Display all active effects and their remaining duration"
) {

    override fun SafeClientEvent.updateText() {
        val player = player
        val activePotionEffects = player.activePotionEffects

        if (activePotionEffects.isEmpty()) {
            displayText.add("No active effects", ClickGUI.text)
            return
        }

        for (effect in activePotionEffects.sortedBy { it.effectName }) {
            val effectName = getEffectName(effect)
            val amplifier = effect.amplifier + 1
            val durationText = getDurationText(effect)

            val effectText = "$effectName $amplifier: "

            displayText.addLine(effectText, ClickGUI.primary)
            displayText.add(durationText, ClickGUI.text)
        }
    }

    private fun getEffectName(effect: PotionEffect): String {
        return effect.effectName
            .removePrefix("effect.")
            .replace('.', ' ')
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    private fun getDurationText(effect: PotionEffect): String {
        return when (effect.duration) {
            -1 -> "∞"
            else -> {
                val duration = effect.duration / 20
                val minutes = duration / 60
                val seconds = duration % 60
                String.format("%d:%02d", minutes, seconds)
            }
        }
    }
}