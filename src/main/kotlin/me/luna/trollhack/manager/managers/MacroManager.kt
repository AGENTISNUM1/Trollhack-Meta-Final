package me.luna.trollhack.manager.managers

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import me.luna.trollhack.TrollHackMod
import me.luna.trollhack.command.CommandManager
import me.luna.trollhack.event.events.InputEvent
import me.luna.trollhack.event.listener
import me.luna.trollhack.manager.Manager
import me.luna.trollhack.util.ConfigUtils
import me.luna.trollhack.util.text.MessageSendUtils
import java.io.File
import java.io.FileWriter
import java.util.*

object MacroManager : Manager() {
    private var macroMap = TreeMap<Int, ArrayList<String>>()
    val isEmpty get() = macroMap.isEmpty()
    val macros: Map<Int, List<String>> get() = macroMap

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val type = object : TypeToken<TreeMap<Int, List<String>>>() {}.type
    private val file get() = File("${TrollHackMod.DIRECTORY}/macros.json")

    init {
        listener<InputEvent.Keyboard> {
            if (it.state) {
                sendMacro(it.key)
            }
        }
    }

    fun loadMacros(): Boolean {
        ConfigUtils.fixEmptyJson(file)

        return try {
            macroMap = gson.fromJson(file.readText(), type)
            TrollHackMod.logger.info("Macro loaded")
            true
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed loading macro", e)
            false
        }
    }

    fun saveMacros(): Boolean {
        return try {
            FileWriter(file, false).buffered().use {
                gson.toJson(macroMap, it)
            }
            TrollHackMod.logger.info("Macro saved")
            true
        } catch (e: Exception) {
            TrollHackMod.logger.warn("Failed saving macro", e)
            false
        }
    }

    /**
     * Sends the message or command, depending on which one it is
     * @param keyCode int keycode of the key the was pressed
     */
    private fun sendMacro(keyCode: Int) {
        val macros = getMacros(keyCode) ?: return
        for (macro in macros) {
            if (macro.startsWith(CommandManager.prefix)) { // this is done instead of just sending a chat packet so it doesn't add to the chat history
                MessageSendUtils.sendTrollCommand(macro) // ie, the false here
            } else {
                MessageManager.sendMessageDirect(macro)
            }
        }
    }

    fun getMacros(keycode: Int) = macroMap[keycode]

    fun setMacro(keycode: Int, macro: String) {
        macroMap.getOrPut(keycode, ::ArrayList).let {
            it.clear()
            it.add(macro)
        }
    }

    fun addMacro(keycode: Int, macro: String) {
        macroMap.getOrPut(keycode, ::ArrayList).add(macro)
    }

    fun removeMacro(keycode: Int) {
        macroMap.remove(keycode)
    }

}