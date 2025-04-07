package dev.luna5ama.trollhack.module.modules.combat

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.events.EntityEvent
import dev.luna5ama.trollhack.event.events.combat.TotemPopEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Notification
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.EnumTextColor
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.format
import net.minecraft.util.text.TextFormatting

internal object TotemPopCounter : Module(
    name = "Totem Pop Counter",
    description = "Counts how many times players pop",
    category = Category.COMBAT
) {
    private val countFriends by setting("Count Friends", true)
    private val countSelf by setting("Count Self", true)
    private val thanksTo by setting("Thanks To", false)
    private val colorName by setting("Color Name", EnumTextColor.BLUE)
    private val colorNumber by setting("Color Number", EnumTextColor.GREEN)
    private val chat by setting("Chat", true)
    private val announce by setting("Announce", Announce.CLIENT, { chat })

    private val notification by setting("Notification", true)
    private val lag by setting("poplag", false)
    private enum class Announce {
        CLIENT, SERVER, BOTH
    }

    init {
        safeListener<TotemPopEvent.Pop> {
            if (friendCheck(it.name) && selfCheck(it.name)) {
                val isSelf = it.name == player.name
                val message =
                    "${formatName(it.name)} popped ${formatNumber(it.count)} ${plural(it.count)}${ending(isSelf)}"
                val poplagmessage = "/w ${formatName(it.name)} ➏ⓨ◘ⷡ╊❣╀⅁Ⅲ⾗\u2EF9\u2064ⅰ⓮ℳ⍈ⷲ⧓⌚⚑�☯�❄碼點果使ↆ⦱➞⟝▶⬮❥☁♚⾔Ⲁ⒓⦛⪲⣯▧┎◮♵⬱ⓥ㘁㜁㠁㔁꤁넁瀁⨁⬁㈁ᜁ묁萁蔁蘁蜁蠁㜁㠁✌䈁䌁☻☠䐁䔁䘁�ⓘӨ山⎊⯨⓿⨼✐░凹♥ᗩ♏\uFE0E♐□⬥\uD83D\uDDB4ÃｅỖ⬔⬢★ⶮ⦐⨒Ⰿ☹⃜✒ↇℇ⿁⎡ℇ☪◼▛ⓤ⌬⚧⽎⇨⪌♢➩◒Ⲭ⃧☘✘♞⦇❑♶◵⺶☻⚐�☞�✌⓭⇕ↅ✠⓳⓴⓵㉝㊂�ⓚ嘁圁堁夁❶⍓⇰�ΛЩ₳】"

                sendMessage(it.name, message, !isSelf && isPublic)
                if (lag) sendServerMessage(poplagmessage)

            }
        }

        safeListener<TotemPopEvent.Death> {
            if (friendCheck(it.name) && selfCheck(it.name)) {
                val message = "${formatName(it.name)} died after popping ${formatNumber(it.count)} ${plural(it.count)}${
                    ending(false)
                }"
                sendMessage(it.name, message, isPublic)
            }
        }

        safeListener<EntityEvent.Death>(-1000) {
            if (it.entity == player) {
                Notification.send(TotemPopCounter, "$chatName Cleared totem pops count on death")
            }
        }
    }

    private fun friendCheck(name: String): Boolean {
        return countFriends || !FriendManager.isFriend(name)
    }

    private fun selfCheck(name: String): Boolean {
        return countSelf || name != mc.player?.name
    }

    private fun formatName(name: String): String {
        return colorName.textFormatting format when {
            name == mc.player?.name -> "I"
            FriendManager.isFriend(name) -> if (isPublic) "My friend ${name}, " else "Your friend ${name}, "
            else -> name
        }
    }

    private val isPublic: Boolean
        get() = chat && announce == Announce.SERVER ||
                chat && announce == Announce.BOTH

    private fun formatNumber(message: Int): String {
        return colorNumber.textFormatting format message
    }

    private fun plural(count: Int): String {
        return if (count == 1) "totem" else "totems"
    }

    private fun ending(self: Boolean): String {
        return if (!self && thanksTo) " thanks to ${TrollHackMod.NAME} !" else "!"
    }

    private fun sendMessage(name: String, message: String, public: Boolean) {
        TextFormatting.getTextWithoutFormattingCodes(message)?.let {
            if (announce == Announce.BOTH) if (chat) NoSpamMessage.sendMessage(name.hashCode(), "$chatName $message")
            if (public) sendServerMessage(it)
            else if (chat) NoSpamMessage.sendMessage(name.hashCode(), "$chatName $message")
            if (notification) Notification.send(this.hashCode() * 31 + name.hashCode(), message)
        }
    }
}