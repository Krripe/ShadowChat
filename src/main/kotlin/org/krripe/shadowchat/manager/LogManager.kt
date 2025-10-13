package org.krripe.shadowchat.manager

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.config.ConfigManager
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object LogManager {
    private val logDir: File = FabricLoader.getInstance().configDir.resolve("shadowchat/chat_logs").toFile()
    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        logDir.mkdirs()
    }

    private fun getLogFile(): File {
        val date = LocalDate.now().format(dateFormat)
        return logDir.resolve("chat-$date.txt")
    }

    fun logChat(player: ServerPlayerEntity, message: String) {
        if (!ConfigManager.chatConfig.chatSystems.logMessages) return

        val time = LocalTime.now().format(timeFormat)
        val line = "[$time] [${player.name.string}] $message"
        writeLine(line)
    }

    fun logPrivate(sender: String, target: String, message: String) {
        if (!ConfigManager.chatConfig.chatSystems.logMessages) return

        val time = LocalTime.now().format(timeFormat)
        val line = "[$time] [msg -> $sender -> $target] $message"
        writeLine(line)
    }

    private fun writeLine(text: String) {
        try {
            getLogFile().appendText("$text\n")
        } catch (e: Exception) {
            println("[ShadowChat] Failed to write chat log: ${e.message}")
        }
    }
}
