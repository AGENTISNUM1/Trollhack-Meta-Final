package dev.luna5ama.trollhack.module.modules.meta

import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.misc.Spammer
import dev.luna5ama.trollhack.util.extension.synchronized
import dev.luna5ama.trollhack.util.text.MessageDetection
import dev.luna5ama.trollhack.util.text.MessageSendUtils
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import dev.luna5ama.trollhack.util.threads.DefaultScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random
import kotlin.text.isNotBlank
import kotlin.text.trim

internal object AutoSpamPlus : Module(
    name = "AutoSpamPlus",
    category = Category.META,
    visible = true,
    description = "Better AutoSpam with multiple modes"
) {
    private val spamMode = setting("Spam Mode", SpamMode.AUTOCOPE)
    private val modeSetting = setting("Order", Mode.RANDOM_ORDER)

    private val delay = setting("Delay", 10, 1..180, 1, description = "Delay between messages, in seconds")

    private val spammer = ArrayList<String>().synchronized()
    private val timer = TickTimer(TimeUnit.SECONDS)
    private var currentLine = 0

    private enum class Mode {
        IN_ORDER, RANDOM_ORDER
    }

    private enum class SpamMode(val fileName: String) {
        AUTOCOPE("autocope.txt"),
        PKBAIT("pkbait.txt"),
        WHATIS("whatis.txt"),
        BULLY("bully.txt"),
        EGOSPAM("egospam.txt")
    }

    private val defaultMessages = mapOf(
        SpamMode.AUTOCOPE to listOf(
            "cope harder",
            "imagine coping this hard",
            "you're literally coping rn",
            "stop coping and get good",
            "that’s a lot of cope for one hit",
            "cope detected, stay mad",
            "you coped so hard you lagged",
            "keep typing, it fuels me",
            "cope harder, the logs aren't helping",
            "you’re deflecting harder than your aim",
            "take a screenshot of this cope",
            "is your keyboard okay from all that coping?",
            "cope so strong it's impressive",
            "massive cope incoming",
            "your keyboard is 90% excuses",
            "I win, you cope — balance",
            "drown in your own cope",
            "that’s not a strat, it’s just coping",
            "copium"
        ),
        SpamMode.PKBAIT to listOf(
            "LETS GO PK VC LOL",
            "LETS BLAZE NN",
            "WHO ARE YOU",
            "IM IN HYPERION VC",
            "JQQ IS IN VC LETS GO",
            "JQQ IS IN VC LOL",
            "WHO ARE YOU",
            "LOL THIS KID KNOWS SO MUCH ABOUT ME",
            "BUT HE WONT GO INTO VC LEL",
            "ARENT YOU RATTED BY SN0WHOOK LOL",
            "LEL I HAVE ALL UR LOGS",
            "SKITTY IS IN VC",
            "I JUST TOLD ETHAN THAT UR TRYING TO BLAZE LOL",
            "LOL WHO ARE YOU",
            "DIDNT YOU RUN SN0W LOL",
            "UR HARMLESS",
            "LEL",
            "LETS GO BLAZE LELASAUCE",
            "LOL I HEARD YOU RAN KAMI SN0W LOL",
            "I HEARD UR TRYING TO BLAZE BUT UR GRAILED",
            "LOL UR RATTED AS FUCK",
            "LOL LETS BLAZE IN HYPERION VC",
            "LEL YOU NN",
            "WHO ARE YOU",
            "LETS GO BLAZE IN HYPERION VC",
            "LETS BLAZE IN PK VC LOL",
            "LETS GO PK VC LOL",
            "WHO ARE YOU",
            "COMPARE LUIGIHACK UID",
            "EZZZZZZ",
            "NO SKILL LELELLEEL",
            "JAP CLIENT USER",
            "EZZZZZZZZZ",
            "DIDNT U BEG OXY FOR PICS LOLOLOL",
            "NO SEXMASTER-CC?",
            "WHO ARE YOU LMAOOOO",
            "FREE QD LMAOOOO",
            "U MAD?",
            "BROS MAD XDDDDDDDDDD",
            "SEXMASTER-CC OWNS U"
        ),
        SpamMode.WHATIS to listOf(
            "what is this gameplay",
            "what is this aim",
            "what is this movement",
            "what is this strategy",
            "what is this decision making",
            "what is this positioning",
            "what is this combo attempt",
            "what is this defense",
            "what is this ping abuse",
            "what is this walking simulator",
            "what is this reaction time",
            "what is this hotbar setup",
            "what is this reach attempt",
            "what is this mouse control",
            "what is this laggy mess",
            "what is this fear-based PvP",
            "what is this pack you're using",
            "what is this glass jaw defense",
            "what is this low effort duel",
            "what is this excuse for PvP"
        ),
        SpamMode.BULLY to listOf(
            "you're actually so bad",
            "uninstall the game",
            "go back to creative mode",
            "you shouldn't be playing this",
            "embarrassing gameplay",
            "did you even try?",
            "bro plays like a bot",
            "get good, seriously",
            "this is painful to watch",
            "was that supposed to be a combo?",
            "0 braincells were used",
            "you're just free kills",
            "you dropped faster than my fps",
            "get rolled, noob",
            "does your mouse even work?",
            "touch grass and uninstall",
            "I’ve seen villagers PvP better",
            "you play like it’s your first day",
            "this ain’t creative mode, clown",
            "imagine losing that hard",
            "you move like a fridge",
            "PvPing you is like fighting a training dummy",
            "are you lagging or just bad?",
            "you got outplayed by someone half-asleep",
            "if bad was a crime, you'd get life",
            "I beat you with one hand, literally",
            "it's like you're trying to lose",
            "stop before you embarrass yourself more",
            "I thought this was gonna be a challenge"
        ),
        SpamMode.EGOSPAM to listOf(
            "IM THE TOP PVPER",
            "IM BETTER",
            "IM THE KING OF HVH",
            "IM THE #1 PVPER",
            "IM ACTUALLY SO GOOD",
            "REASON WHY I OWN THIS SERVER",
            "IS ANYONE EVEN GOOD HERE",
            "WHY AM I SO GOOD",
            "EVERYONE IS SHIT COMPARED TO ME",
            "MY SKILLS ARE JUST SO GOOD",
            "WHY AM I THIS GOOD AT THIS GAME",
            "IM HIM",
            "I MAKE THE RULES HERE",
            "YALL FIGHTING ME LIKE IT’S FAIR",
            "I COULD 1V10 AND WIN",
            "YOU SHOULD BE HONORED I LOGGED IN",
            "THE SERVER RUNS BECAUSE I SAY SO",
            "MY CLIENT > YOUR ENTIRE TEAM",
            "YOU’RE FIGHTING A LEGEND",
            "I CAME, I FOUGHT, I FARMED"
        )
    )

    private fun getCurrentFile(): File {
        return File("${TrollHackMod.DIRECTORY}/${spamMode.value.fileName}")
    }

    private fun createFileWithDefaults(file: File, mode: SpamMode) {
        if (!file.exists()) {
            file.createNewFile()
            val defaultContent = defaultMessages[mode] ?: listOf("Default message for ${mode.name}")
            file.writeText(defaultContent.joinToString("\n"))
            NoSpamMessage.sendMessage("$chatName Created ${mode.fileName} with default messages!")
        }
    }

    init {
        onEnable {
            spammer.clear()


            DefaultScope.launch(Dispatchers.IO) {
                val currentFile = getCurrentFile()

                // Create file with defaults if it doesn't exist
                createFileWithDefaults(currentFile, spamMode.value)

                try {
                    currentFile.forEachLine {
                        if (it.isNotBlank()) spammer.add(it.trim())
                    }
                    NoSpamMessage.sendMessage("$chatName Loaded ${spammer.size} messages for ${spamMode.value.name} mode!")

                    if (spammer.isEmpty()) {
                        NoSpamMessage.sendError("$chatName No valid messages found in ${spamMode.value.fileName}!")
                        disable()
                    }
                } catch (e: Exception) {
                    NoSpamMessage.sendError("$chatName Failed loading ${spamMode.value.fileName}, $e")
                    disable()
                }
            }
        }

        // Reload messages when spam mode changes
        spamMode.listeners.add {
            if (isEnabled) {
                disable()
                enable()
            }
        }

        safeListener<TickEvent.Post> {
            if (spammer.isEmpty() || !timer.tickAndReset(delay.value)) return@safeListener

            val message = if (modeSetting.value == Mode.IN_ORDER) getOrdered() else getRandom()
            if (MessageDetection.Command.TROLL_HACK detect message) {
                MessageSendUtils.sendTrollCommand(message)
            } else {
                Spammer.sendServerMessage(message)
            }
        }
    }

    private fun getOrdered(): String {
        currentLine %= spammer.size
        return spammer[currentLine++]
    }

    private fun getRandom(): String {
        val prevLine = currentLine
        // Avoids sending the same message
        while (spammer.size != 1 && currentLine == prevLine) {
            currentLine = Random.nextInt(spammer.size)
        }
        return spammer[currentLine]
    }
}