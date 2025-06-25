package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.SafeClientEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.events.combat.TotemPopEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.manager.managers.FriendManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.EntityUtils.isFakeOrSelf
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.math.vector.distanceSqTo
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minecraft.entity.player.EntityPlayer
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

internal object TotemPopMessage : Module(
    name = "AutoEz+",
    description = "too ez",
    category = Category.META
) {
    private val greentext by setting("Greentext", false)
    private val friends by setting("Friends", false)
    private val deathMessages by setting("Death Messages", true)
    private val delay by setting("Delay", 2, 0..60, 1, description = "Delay in seconds between messages")
    private val file = File("${TrollHackMod.DIRECTORY}/totem_messages")
    private val deathFile = File("${TrollHackMod.DIRECTORY}/death_messages")
    private var messages: List<String> = emptyList()
    private var deathMessagesList: List<String> = emptyList()
    private var lastMessageTime = 0L // Tracks the last time a message was sent
    private val attackedPlayers = WeakHashMap<EntityPlayer, Int>().synchronized()

    init {
        onEnable {
            loadMessages()
            loadDeathMessages()
        }

        safeListener<TotemPopEvent.Pop> {
            if (it.name != player.name) {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastMessage = currentTime - lastMessageTime
                val delayMillis = TimeUnit.SECONDS.toMillis(delay.toLong())

                if (timeSinceLastMessage >= delayMillis) {

                    val message = getRandomMessage(it.name, it.count)
                    if (friends && FriendManager.isFriend(it.name)) {
                        if (greentext) sendServerMessage("> My friend ${it.name} popped ${it.count} totem${if (it.count == 1) "s!" else "!"}")
                        else sendServerMessage("My friend ${it.name} popped ${it.count} totem${if (it.count == 1) "s!" else "!"}")
                    } else {
                        if (!FriendManager.isFriend(it.name)) {
                            if (greentext) sendServerMessage("> $message")
                            else sendServerMessage(message)
                        }
                    }
                    lastMessageTime = currentTime
                } else {
                    NoSpamMessage.sendMessage("$chatName Message skipped. Delay not yet passed.")
                }
            }
        }

        safeListener<TickEvent.Post> {
            if (deathMessages) {
                updateAttackedPlayer()
                removeInvalidPlayers()
                sendDeathMessage()
            }
        }
    }

    private fun loadMessages() {
        runBlocking {
            launch(Dispatchers.IO) {
                if (!file.exists()) {
                    file.createNewFile()
                    file.writeText(
                        """
                        helen keller is better at this game than you, {name}! EZ {count} pop{s}
                        I almost feel bad for {name}... Just kidding! {count} pop{s}!
                        Your lack of skill bores me {name}. {count} pop{s}
                        I expected more of you, {name}! {count} pop{s}!
                        Just /kill at this point, you popped {count} time{s}.
                        Is there something wrong with your config? You popped {count} time{s}.
                        {name} popped faster than bubble wrap under a fat guy. EZ {count} POP{s}!
                        {name} really though this was a lore smp. EZ {count} POP{s}!
                        That wasn't a fight, {name}, that was a tutorial. {count} pop{s}
                        {name} just donated {count} totem{s} to the cause
                        I just used {name} as a training dummy. {count} pop{s}!
                        {name} hitting new lows in PvP history. {count} pop{s}
                        {count} pop{s}, and not one person cared enough to save you, {name}
                        I've seen less suffering in a war crime tribunal! {count} pop{s}, {name}
                        {name} popped so much I thought it was a school shooting! {count} pop{s}
                        {name}'s skill issues make terminal illness look like a buff. {count} pop{s}!
                        {name} has the survival rate of a hamster in a microwave. {count} pop{s}!
                        Your skill level makes Darwin proud. {count} pop{s}, {name}!
                        {name} handles pressure like a 2008 stockbroker. {count} pop{s}
                        You're the reason your bloodline ends, {name}! {count} pop{s}!
                        {name} dies more often than hope in a foster home. {count} pop{s}!
                        That wasn't a pop — that was a eulogy. {count} time{s}, {name}!
                        You're not a player, {name}. You're a case study in failure. {count} pop{s}!
                        You're not dying anymore, {name}. You're decaying. {count} pop{s}!
                        God killed your game, {name}. {count} pop{s} was your crash log.
                        That wasn't a battle. That was divine punishment. {count} pop{s}, {name}!
                        You pop like a Columbine yearbook. {count} pop{s}, {name}!
                        That wasn't PvP. That was a domestic incident. ez {count} pop{s}, {name}!
                        {name} fights like their parents wanted the miscarriage. {count} pop{s}!
                        Even the UN issued a ceasefire after {count} pop{s}, {name}!
                        You fight like your mom locked the fridge. EZ {count} pop{s}, {name}!
                        {count} pop{s}? You've got more deaths than people who stayed in your life.
                        """.trimIndent()
                    )
                    NoSpamMessage.sendMessage("$chatName Created new totem_messages file with default content!")
                }
                messages = file.readLines().filter { it.isNotBlank() }
                NoSpamMessage.sendMessage("$chatName Loaded ${messages.size} totem pop messages!")
            }
        }
    }

    private fun loadDeathMessages() {
        runBlocking {
            launch(Dispatchers.IO) {
                if (!deathFile.exists()) {
                    deathFile.createNewFile()
                    deathFile.writeText(
                        """
                       {name} died the way they lived — unprepared and unwanted.
                       {name} returned to the void that raised them.
                       {name} finally found something stronger than their denial.
                       {name} died wishing this was the first time it hurt.
                       {name} died — not suddenly, but inevitably.
                       {name} met the same end as every broken thing.
                       {name} fell faster than the Twin Towers
                       {name} is the punchline to a joke their family never finished
                       {name}, you bring new meaning to the term 'quick drop'
                       You’re the human version of a participation trophy, {name}.
                       Even {name}'s shadow is ashamed to follow them.
                       {name} is the reason the tutorial exists.
                       {name}'s skill peaked at 'once upon a time.'
                       {name} only exists to lower the average.
                       {name} is the failure everyone else warns about but never expects.
                       {name} brings new meaning to the word 'pathetic.'
                       {name} is the embodiment of 'too far gone.'
                       {name} is a walking condom ad.
                       {name} is the reason abortions were invented.
                       {name} is a liability in every single pixel.
                        """.trimIndent()
                    )
                    NoSpamMessage.sendMessage("$chatName Created new death_messages file with default content!")
                }
                deathMessagesList = deathFile.readLines().filter { it.isNotBlank() }
                NoSpamMessage.sendMessage("$chatName Loaded ${deathMessagesList.size} death messages!")
            }
        }
    }

    private fun getRandomMessage(name: String, count: Int): String {
        val s = if (count > 1) "s" else ""
        val message = if (messages.isNotEmpty()) messages.random() else "EZZZZZ POP, {name}!  {count} pop{s}!"
        return message
            .replace("{name}", name)
            .replace("{count}", count.toString())
            .replace("{s}", s)
    }

    private fun getRandomDeathMessage(name: String): String {
        val message = if (deathMessagesList.isNotEmpty()) deathMessagesList.random() else "RIP {name}! EZ kill!"
        return message.replace("{name}", name)
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

    private fun SafeClientEvent.sendDeathMessage() {
        attackedPlayers.keys.find {
            !it.isEntityAlive
                    && player.distanceSqTo(it) <= 256.0
        }?.let { deadPlayer ->
            attackedPlayers.remove(deadPlayer)
            
            val currentTime = System.currentTimeMillis()
            val timeSinceLastMessage = currentTime - lastMessageTime
            val delayMillis = TimeUnit.SECONDS.toMillis(delay.toLong())
            
            if (timeSinceLastMessage >= delayMillis) {
                val message = getRandomDeathMessage(deadPlayer.name)

                if (!FriendManager.isFriend(deadPlayer.name)) {
                    if (greentext) sendServerMessage("> $message")
                        else sendServerMessage(message)
                }

                lastMessageTime = currentTime
            } else {
                NoSpamMessage.sendMessage("$chatName Death message skipped. Delay not yet passed.")
            }
        }
    }
}
