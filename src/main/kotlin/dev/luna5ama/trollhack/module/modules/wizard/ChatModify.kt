package dev.luna5ama.trollhack.module.modules.wizard

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.manager.managers.MessageManager.newMessageModifier
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.MessageDetection
import net.minecraft.client.gui.ChatLine
import net.minecraft.util.text.ITextComponent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.min
import kotlin.random.Random

internal object ChatModify : Module(
    name = "Chat Modifier",
    category = Category.META,
    description = "Edits for chat",
    visible = false,
    modulePriority = 200
) {

    private val extrahistory by setting("ExtraChatHistory", false)
    private val maxMessages by setting("Max Message", 1000, 100..5000, 100, { extrahistory })
    private val antispambypass by setting("Bypass anti spam", false)
    private val antispambypassammount by setting("Anti spam bypass ammount", 3, 1..15, 1, { antispambypass })
    private val commands by setting("Commands", false)
    private val suffix = setting("Suffix", Suffix.NONE)
    private val seperator = setting("Seperator", Seperator.NONE, { suffix.value != Suffix.NONE })
    private val prefix = setting("Prefix", Prefix.NONE)
    private val timer = TickTimer(TimeUnit.SECONDS)

    private enum class Suffix() {
        TROLLHACK, EARTHHACK, RUSHERHACK, KONAS, GAMESENSE, GSPLUSPLUS, METEOR, BOZE, NONE
    }
    private enum class Seperator() {
        NORMAL, BAR, ARROW, NONE
    }
    private enum class Prefix() {
        GREEN, RUSHER, NONE
    }

    @JvmStatic
    fun handleSetChatLine(
        drawnChatLines: MutableList<ChatLine>,
        chatLines: MutableList<ChatLine>,
        chatComponent: ITextComponent,
        chatLineId: Int,
        updateCounter: Int,
        displayOnly: Boolean,
        ci: CallbackInfo
    ) {
        if (!extrahistory) return

        while (drawnChatLines.isNotEmpty() && drawnChatLines.size > maxMessages) {
            drawnChatLines.removeLast()
        }

        if (!displayOnly) {
            chatLines.add(0, ChatLine(updateCounter, chatComponent, chatLineId))

            while (chatLines.isNotEmpty() && chatLines.size > maxMessages) {
                chatLines.removeLast()
            }
        }

        ci.cancel()
    }

    private val modifier = newMessageModifier(
        filter = {
            (commands || MessageDetection.Command.ANY detectNot it.packet.message)
        },
        modifier = {
            var message = it.packet.message
            message = message.substring(0, min(256, message.length))
            if (prefix.value != Prefix.NONE) {
                message = getprefix() + it.packet.message
            }
            if (antispambypass) {
                message += generateRandomSuffix()
            }
            if (suffix.value != Suffix.NONE) {
                message += "${getseperator()} ${getchatsuffix()}"
            }
            message
        }
    )

    private fun generateRandomSuffix(): String {
        val random = Random(System.currentTimeMillis())
        val suffix = StringBuilder()
        val numbers = '0'..'9'
        val letters = ('a'..'z') + ('A'..'Z')
        val allChars = numbers + letters
        val positions = (0 until antispambypassammount).shuffled(random)
        suffix.append(numbers.random(random))
        if (antispambypassammount > 1) {
            suffix.append(letters.random(random))
        }
        for (i in 2 until antispambypassammount) {
            suffix.append(allChars.random(random))
        }
        val chars = suffix.substring(2).toMutableList()
        chars.shuffle(random)
        suffix.replace(2, suffix.length, chars.joinToString(""))
        return suffix.toString()
    }
    init {
        onEnable {
            modifier.enable()
        }

        onDisable {
            modifier.disable()
        }
    }

    private fun getchatsuffix() = when (suffix.value) {
        Suffix.TROLLHACK -> "ＴＲＯＬＬＨＡＣＫ"
        Suffix.EARTHHACK -> "³ᵃʳᵗʰʰ⁴ᶜᵏ- 1.8.5"
        Suffix.RUSHERHACK -> "ʳᵘˢʰᵉʳʰᵃᶜᵏ"
        Suffix.KONAS -> "K o n a s"
        Suffix.GAMESENSE -> "ɢᴀᴍᴇѕᴇɴѕᴇ"
        Suffix.GSPLUSPLUS -> "ᴳˢ⁺⁺"
        Suffix.METEOR -> "Meteor On Crack"
        Suffix.BOZE -> "Ｂｏｚｅ"
        Suffix.NONE -> ""
    }
    private fun getseperator() = when (seperator.value) {
        Seperator.NORMAL -> " | "
        Seperator.BAR -> " ┃ "
        Seperator.ARROW -> " ➔ "
        Seperator.NONE -> " "
    }
    private fun getprefix() = when (prefix.value) {
        Prefix.GREEN -> "> "
        Prefix.RUSHER -> "ʳᵘˢʰᵉʳʰᵃᶜᵏ  "
        Prefix.NONE -> ""
    }
    init {
        safeParallelListener<TickEvent.Post> {
        }
    }
}