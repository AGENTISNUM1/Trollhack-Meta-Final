package dev.luna5ama.trollhack.module.modules.combat

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.combat.TotemPopEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.event.safeParallelListener
import dev.luna5ama.trollhack.gui.hudgui.elements.client.Notification
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.accessor.textComponent
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.text.unformatted
import dev.luna5ama.trollhack.util.threads.DefaultScope
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.network.play.server.SPacketChat
import net.minecraft.util.text.TextFormatting
import java.io.File
import java.util.*
import kotlin.random.Random

internal object AutoEZ : Module(
    name = "Auto EZ",
    category = Category.COMBAT,
    description = "Sends messages when killing players or when they pop totems"
) {
    private const val NAME_PLACEHOLDER = "\$NAME"
    private const val COUNT_PLACEHOLDER = "\$COUNT"

    private val detectMode by setting("Detect Mode", DetectMode.HEALTH)
    private val notification by setting("Notification", true)
    private val popMessages by setting("Pop Messages", true)
    private val popNotification by setting("Pop Notification", true)
    private val showPopCount by setting("Show Pop Count", true)
    private val resetOnDeath by setting("Reset Pop Count On Death", true)

    private val ezFile = File("${TrollHackMod.DIRECTORY}/autoez.txt")
    private val popFile = File("${TrollHackMod.DIRECTORY}/autopop.txt")
    private val ezMessages = ArrayList<String>().synchronized()
    private val popMessagesList = ArrayList<String>().synchronized()
    private val timer = TickTimer(TimeUnit.SECONDS)
    private val attackedPlayers = WeakHashMap<EntityPlayer, Int>().synchronized()
    private val confirmedKills = IntSets.synchronize(IntOpenHashSet())
    private val popCountMap = HashMap<String, Int>().synchronized()
    private val sound0 by setting("soundtype", soundtype.PING)
    private enum class DetectMode {
        BROADCAST, HEALTH
    }
    private enum class soundtype{
        PING, SPIDER, SKELETON
    }

    init {
        onDisable {
            reset()
        }

        onEnable {
            loadMessages()
        }

        listener<ConnectionEvent.Disconnect> {
            reset()
        }

        safeListener<PacketEvent.Receive>(Int.MAX_VALUE) { event ->
            if (event.packet !is SPacketChat || detectMode != DetectMode.BROADCAST || !player.isEntityAlive) return@safeListener

            val message = event.packet.textComponent.unformatted
            if (!message.contains(player.name)) return@safeListener

            attackedPlayers.keys.find {
                message.contains(it.name)
            }?.let {
                confirmedKills.add(it.entityId)
            }
        }

        safeParallelListener<TickEvent.Post> {
            if (!player.isEntityAlive && resetOnDeath) {
                popCountMap.clear()
                return@safeParallelListener
            }

            updateAttackedPlayer()
            removeInvalidPlayers()
            sendEzMessage()
            checkFilesEmpty()
        }

        listener<TotemPopEvent.Pop> { event ->
            if (!popMessages || event.entity.isFakeOrSelf) return@listener

            val playerName = event.entity.name
            val currentCount = popCountMap.getOrDefault(playerName, 0) + 1
            popCountMap[playerName] = currentCount

            if (popMessagesList.isEmpty()) {
                NoSpamMessage.sendError("$chatName No AutoPop messages loaded!")
                return@listener
            }

            val randomMessage = popMessagesList[Random.nextInt(popMessagesList.size)]
            var message = randomMessage.replace(NAME_PLACEHOLDER, "$playerName")

            if (showPopCount) {
                message = message.replace(COUNT_PLACEHOLDER, currentCount.toString())
            }

            if (FriendManager.isFriend(playerName)) {
                sendServerMessage("My friend $playerName has popped a totem (Total: $currentCount)")
            } else {
                sendServerMessage(message)
                sendServerMessage("/w $playerName ➏ⓨ◘ⷡ╊❣╀⅁Ⅲ⾗\u2EF9\u2064ⅰ⓮ℳ⍈ⷲ⧓⌚⚑�☯�❄碼點果使ↆ⦱➞⟝▶⬮❥☁♚⾔Ⲁ⒓⦛⪲⣯▧┎◮♵⬱ⓥ㘁㜁㠁㔁꤁넁瀁⨁⬁㈁ᜁ묁萁蔁蘁蜁蠁㜁㠁✌䈁䌁☻☠䐁䔁䘁�ⓘӨ山⎊⯨⓿⨼✐░凹♥ᗩ♏\uFE0E♐□⬥\uD83D\uDDB4ÃｅỖ⬔⬢★ⶮ⦐⨒Ⰿ☹⃜✒ↇℇ⿁⎡ℇ☪◼▛ⓤ⌬⚧⽎⇨⪌♢➩◒Ⲭ⃧☘✘♞⦇❑♶◵⺶☻⚐�☞�✌⓭⇕ↅ✠⓳⓴⓵㉝㊂�ⓚ嘁圁堁夁❶⍓⇰�ΛЩ₳】")
            }

            if (popNotification) {
                Notification.send(
                    "${TextFormatting.RED}$playerName ${TextFormatting.RESET}popped a totem! (Total: $currentCount)",
                    5000L
                )
            }
        }
    }

    private fun getSound() = when (sound0) {
        soundtype.PING -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP
        soundtype.SPIDER -> SoundEvents.ENTITY_SPIDER_DEATH
        soundtype.SKELETON -> SoundEvents.ENTITY_SKELETON_DEATH
    }
    private fun loadMessages() {
        DefaultScope.launch(Dispatchers.IO) {
            ezMessages.clear()
            popMessagesList.clear()

            // Load EZ messages
            if (ezFile.exists()) {
                try {
                    ezFile.forEachLine { if (it.isNotBlank()) ezMessages.add(it.trim()) }
                    NoSpamMessage.sendMessage("$chatName Loaded AutoEZ messages!")
                } catch (e: Exception) {
                    NoSpamMessage.sendError("$chatName Failed loading AutoEZ messages, $e")
                }
            } else {
                ezFile.createNewFile()
                ezFile.writeText(
                    "Troll Hack on top! ez \$NAME\n" +
                            "You just got ez'd \$NAME\n" +
                            "gg, \$NAME\n" +
                            "Good fight \$NAME! Troll Hack owns me and all"
                )
                NoSpamMessage.sendMessage(
                    "$chatName Created AutoEZ message file with default messages!" +
                            " You can edit them in §7autoez.txt§f under the §7.minecraft/trollhack§f directory."
                )
            }

            // Load Pop messages
            if (popFile.exists()) {
                try {
                    popFile.forEachLine { if (it.isNotBlank()) popMessagesList.add(it.trim()) }
                    NoSpamMessage.sendMessage("$chatName Loaded AutoPop messages!")
                } catch (e: Exception) {
                    NoSpamMessage.sendError("$chatName Failed loading AutoPop messages, $e")
                }
            } else {
                popFile.createNewFile()
                popFile.writeText(
                    "LOL \$NAME just popped! (\$COUNT total)\n" +
                            "\$NAME is panicking! (\$COUNT totems gone)\n" +
                            "Totem goes pop! - \$NAME (\$COUNT)\n" +
                            "Another totem gone for \$NAME! (Total: \$COUNT)"
                )
                NoSpamMessage.sendMessage(
                    "$chatName Created AutoPop message file with default messages!" +
                            " You can edit them in §7autopop.txt§f under the §7.minecraft/trollhack§f directory."
                )
            }
        }
    }

    private fun SafeClientEvent.updateAttackedPlayer() {
        val attacked = player.lastAttackedEntity
        if (attacked is EntityPlayer && attacked.isEntityAlive && !attacked.isFakeOrSelf) {
            attackedPlayers[attacked] = player.lastAttackedEntityTime
        }
    }

    private fun SafeClientEvent.removeInvalidPlayers() {
        val removeTime = player.ticksExisted - 100L
        attackedPlayers.entries.removeIf {
            @Suppress("SENSELESS_COMPARISON")
            it.value < removeTime || connection.getPlayerInfo(it.key.uniqueID) == null
        }
    }

    private fun SafeClientEvent.sendEzMessage() {
        attackedPlayers.keys.find {
            !it.isEntityAlive
                    && player.distanceSqTo(it) <= 256.0
                    && (detectMode == DetectMode.HEALTH || confirmedKills.contains(it.entityId))
        }?.let {
            attackedPlayers.remove(it)
            confirmedKills.remove(it.entityId)
            popCountMap.remove(it.name) // Remove pop count when player dies

            if (ezMessages.isEmpty()) {
                NoSpamMessage.sendError("$chatName No AutoEZ messages loaded!")
                return@let
            }

            val randomMessage = ezMessages[Random.nextInt(ezMessages.size)]
            val replaced = randomMessage.replace(NAME_PLACEHOLDER, it.name)

            if (notification) {
                Notification.send("${TextFormatting.RED}${it.name} ${TextFormatting.RESET}was killed by you", 5000L)
            }

            sendServerMessage(replaced)
            mc.player.playSound(getSound(), 1.0f, 1.0f)
        }
    }

    private fun checkFilesEmpty() {
        if (timer.tickAndReset(5L)) {
            if (ezMessages.isEmpty()) {
                NoSpamMessage.sendError(
                    "$chatName AutoEZ file is empty!" +
                            ", please add messages in the §7autoez.txt§f under the §7.minecraft/trollhack§f directory." +
                            " Use $NAME_PLACEHOLDER for the player name."
                )
            }
            if (popMessagesList.isEmpty()) {
                NoSpamMessage.sendError(
                    "$chatName AutoPop file is empty!" +
                            ", please add messages in the §7autopop.txt§f under the §7.minecraft/trollhack§f directory." +
                            " Use $NAME_PLACEHOLDER for the player name and $COUNT_PLACEHOLDER for pop count."
                )
            }
        }
    }

    private fun reset() {
        attackedPlayers.clear()
        confirmedKills.clear()
        popCountMap.clear()
    }
}