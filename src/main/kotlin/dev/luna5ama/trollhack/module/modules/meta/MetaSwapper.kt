package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.combat.*
import dev.luna5ama.trollhack.module.modules.combat.AutoTotem
import dev.luna5ama.trollhack.module.modules.combat.BedAura
import dev.luna5ama.trollhack.module.modules.combat.BedCity
import dev.luna5ama.trollhack.module.modules.player.Kit

internal object MetaSwapper : Module(
    name = "Meta Swapper",
    category = Category.META,
    description = "wizards module, autoconfigs for a meta"
) {
    private val mode by setting("Mode", Mode.BED)
    private enum class Mode(){
        CRYSTAL, BED
    }
    private val switchkit by setting("Switch Kit", false)
    private val bedkitname by setting("Bed Kit Name", "bpvp2", { switchkit })
    private val crystalkitname by setting("Crystal Kit Name", "cpvp2", { switchkit })
    private val futureca by setting("Future CA", false)
    private val futureoffhand by setting("Future Offhand", false)
    private val futureprefix by setting("Future Prefix", ".", { futureca || futureoffhand })
    init {
        onEnable {
            if (mode == Mode.CRYSTAL) {
                BedAura.disable()
                BetterPot.healHealth = 14.5f
                BedCity.enable()
                PacketEat.enable()
                LegacyBasePlace.disable()
                CornerClip.disable()
                SelfTarget.disable()
                AutoTotem.disable()
                if (futureoffhand) {
                    mc.player.sendChatMessage(futureprefix + "t AutoTotem")
                } else {
                    Offhand.enable()
                }
                Criticals.enable()
                if (switchkit) {
                    Kit.kitName = crystalkitname
                }

            }
            if (mode == Mode.BED) {
//                ZealotCrystalPlus.disable()
//                if (futureca) {
//                    mc.player.sendChatMessage(futureprefix + "t AutoCrystal")
//                }
                BetterPot.healHealth = 19.0f
                BedCity.enable()
                PacketEat.enable()
                CrystalBasePlace.disable()
                CornerClip.disable()
                SelfTarget.disable()
                if (futureoffhand) {
                    mc.player.sendChatMessage(futureprefix + "t AutoTotem")
                } else {
                    Offhand.disable()
                }
                AutoTotem.enable()
                Criticals.disable()
                if (switchkit) {
                    Kit.kitName = bedkitname
                }

            }
            MetaSwapper.disable()
        }
    }
}