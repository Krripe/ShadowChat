package org.krripe.shadowchat.manager

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.config.ConfigManager
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object LogManager {
    private val chatLogDir: File = FabricLoader.getInstance().configDir.resolve("shadowchat/chat_logs").toFile()
    private val commandLogDir: File = FabricLoader.getInstance().configDir.resolve("shadowchat/command_logs").toFile()

    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        chatLogDir.mkdirs()
        commandLogDir.mkdirs()
    }

    private fun getChatLogFile(): File {
        val date = LocalDate.now().format(dateFormat)
        return chatLogDir.resolve("chat-$date.txt")
    }

    private fun getCommandLogFile(): File {
        val date = LocalDate.now().format(dateFormat)
        return commandLogDir.resolve("commands-$date.txt")
    }

    fun logChat(player: ServerPlayerEntity, message: String) {
        if (!ConfigManager.chatConfig.chatSystems.logMessages) return

        val time = LocalTime.now().format(timeFormat)
        val line = "[$time] [${player.name.string}] $message"
        writeLine(getChatLogFile(), line)
    }

    fun logPrivate(sender: String, target: String, message: String) {
        if (!ConfigManager.chatConfig.chatSystems.logMessages) return

        val time = LocalTime.now().format(timeFormat)
        val line = "[$time] [msg -> $sender -> $target] $message"
        writeLine(getChatLogFile(), line)
    }

    fun logCommand(source: ServerCommandSource, command: String) {
        if (!ConfigManager.chatConfig.chatSystems.commandLogs) return

        val cfg = ConfigManager.chatConfig.commandLogs

        val raw = command.trim().removePrefix("/")
        if (raw.isBlank()) return

        val base = raw.substringBefore(" ").lowercase()

        if (cfg.ignoredCommands.any { it.equals(base, ignoreCase = true) }) return

        val player = source.entity as? ServerPlayerEntity

        if (player == null && !cfg.logConsoleCommands) return

        val time = LocalTime.now().format(timeFormat)
        val actor = player?.name?.string ?: source.name
        val uuid = player?.uuidAsString ?: "console"
        val loggedCommand = if (cfg.logArguments) raw else base

        val line = "[$time] [$actor/$uuid] /$loggedCommand"
        writeLine(getCommandLogFile(), line)
    }

    private fun writeLine(file: File, text: String) {
        try {
            file.parentFile.mkdirs()
            file.appendText("$text\n")
        } catch (e: Exception) {
            println("[ShadowChat] Failed to write log: ${e.message}")
        }
    }
}