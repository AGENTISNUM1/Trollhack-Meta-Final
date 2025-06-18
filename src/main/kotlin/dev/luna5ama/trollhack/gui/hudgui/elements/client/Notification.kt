package dev.luna5ama.trollhack.gui.hudgui.elements.client

import dev.luna5ama.trollhack.util.text.NoSpamMessage

/**
 * Notification system for the TrollHack client
 * Provides a unified interface for sending notifications to the user
 */
object Notification {
    /**
     * Send a notification message
     * @param message The message to send
     */
    fun send(message: String) {
        NoSpamMessage.sendMessage(message)
    }

    /**
     * Send a notification message with a specific identifier
     * @param identifier The identifier for spam filtering
     * @param message The message to send
     */
    fun send(identifier: Any, message: String) {
        NoSpamMessage.sendMessage(identifier, message)
    }

    /**
     * Send a notification message with a specific identifier and timeout
     * @param identifier The identifier for spam filtering
     * @param message The message to send
     * @param timeout The timeout in milliseconds (ignored for compatibility)
     */
    @Suppress("UNUSED_PARAMETER")
    fun send(identifier: Any, message: String, timeout: Long) {
        NoSpamMessage.sendMessage(identifier, message)
    }

    /**
     * Send a warning notification
     * @param message The warning message to send
     */
    fun sendWarning(message: String) {
        NoSpamMessage.sendWarning(message)
    }

    /**
     * Send a warning notification with a specific identifier
     * @param identifier The identifier for spam filtering
     * @param message The warning message to send
     */
    fun sendWarning(identifier: Any, message: String) {
        NoSpamMessage.sendWarning(identifier, message)
    }

    /**
     * Send a warning notification with a specific identifier and timeout
     * @param identifier The identifier for spam filtering
     * @param message The warning message to send
     * @param timeout The timeout in milliseconds (ignored for compatibility)
     */
    @Suppress("UNUSED_PARAMETER")
    fun sendWarning(identifier: Any, message: String, timeout: Long) {
        NoSpamMessage.sendWarning(identifier, message)
    }

    /**
     * Send an error notification
     * @param message The error message to send
     */
    fun sendError(message: String) {
        NoSpamMessage.sendError(message)
    }

    /**
     * Send an error notification with a specific identifier
     * @param identifier The identifier for spam filtering
     * @param message The error message to send
     */
    fun sendError(identifier: Any, message: String) {
        NoSpamMessage.sendError(identifier, message)
    }

    /**
     * Send an error notification with a specific identifier and timeout
     * @param identifier The identifier for spam filtering
     * @param message The error message to send
     * @param timeout The timeout in milliseconds (ignored for compatibility)
     */
    @Suppress("UNUSED_PARAMETER")
    fun sendError(identifier: Any, message: String, timeout: Long) {
        NoSpamMessage.sendError(identifier, message)
    }
}

