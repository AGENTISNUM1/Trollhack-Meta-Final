package dev.luna5ama.trollhack.module.modules.misc

import dev.luna5ama.trollhack.event.events.ConnectionEvent
import dev.luna5ama.trollhack.event.events.TickEvent
import dev.luna5ama.trollhack.event.listener
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.MovementUtils.isMoving
import dev.luna5ama.trollhack.util.text.MessageDetection
import dev.luna5ama.trollhack.util.text.MessageSendUtils
import dev.luna5ama.trollhack.util.text.MessageSendUtils.sendServerMessage
import dev.luna5ama.trollhack.util.text.NoSpamMessage

internal object LoginMessage : Module(
    name = "Login Message",
    description = "Sends a given message(s) to public chat on login.",
    category = Category.MISC,
    visible = false,
    modulePriority = 150
) {
    private val loginmessage by setting("Message", "Hello World!")
    private val sendAfterMoving by setting(
        "Send After Moving",
        false,
        description = "Wait until you have moved after logging in"
    )


    private var sent = false
    private var moved = false

    init {
        onEnable {
            if (loginmessage == "Hello World") {
                NoSpamMessage.sendMessage("$chatName set a custon loginmessage using the message setting")
            }
        }

        onDisable {
        }

        listener<ConnectionEvent.Disconnect> {
            sent = false
            moved = false
        }

        safeListener<TickEvent.Post> {
            if (!sent && (!sendAfterMoving || moved)) {
                    if (MessageDetection.Command.TROLL_HACK detect loginmessage) {
                        MessageSendUtils.sendTrollCommand(loginmessage)
                    } else {
                        LoginMessage.sendServerMessage(loginmessage)
                    }

                sent = true
            }

            if (!moved) moved = player.isMoving
        }
    }
}