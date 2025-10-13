package org.krripe.shadowchat.manager

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.config.BadWordsConfig
import org.krripe.shadowchat.utils.CustomColorParser
import java.util.*

object BadWordManager {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    private val configFile = FabricLoader.getInstance().configDir.resolve("shadowchat").resolve("badwords.json").toFile()
    private val playerOffenses = mutableMapOf<UUID, Int>()
    private var config = BadWordsConfig()

    fun loadConfig() {
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            configFile.writeText(gson.toJson(config))
        } else {
            try {
                config = gson.fromJson(configFile.readText(), BadWordsConfig::class.java)
            } catch (_: Exception) {}
        }
    }

    fun filterMessage(message: String): Triple<String, Int, String> {
        if (!config.enabled) return Triple(message, 0, message)

        var filtered = message
        var found = 0
        for (word in config.badWords) {
            val regex = Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE)
            val matches = regex.findAll(filtered).count()
            if (matches > 0) {
                filtered = filtered.replace(regex, config.censorWord)
                found += matches
            }
        }

        return Triple(filtered, found, message)
    }

    fun handleBadWords(player: ServerPlayerEntity, offenseCount: Int) {
        if (offenseCount <= 0) return

        val previousOffenses = playerOffenses.getOrDefault(player.uuid, 0)
        val newOffenseTotal = previousOffenses + offenseCount
        playerOffenses[player.uuid] = newOffenseTotal

        val offensesPerWarning = config.warnings.offensesPerWarning
        val maxWarnings = config.warnings.maxWarnings
        val currentWarningLevel = previousOffenses / offensesPerWarning
        val newWarningLevel = newOffenseTotal / offensesPerWarning

        if (newWarningLevel > currentWarningLevel && newWarningLevel <= maxWarnings) {
            val warnMsg = config.messages.warningMessage.replace("%player%", player.name.string)
            val parsed = CustomColorParser.processAllFormats(warnMsg)
            player.sendMessage(parsed, false)
        }

        if (newWarningLevel > maxWarnings) {
            val cmd = config.warnings.punishmentCommand.replace("%player%", player.name.string)
            player.server.commandManager.executeWithPrefix(player.server.commandSource, cmd)

            val broadcastMsg = config.messages.punishmentBroadcast.replace("%player%", player.name.string)
            val parsed = CustomColorParser.processAllFormats(broadcastMsg)
            player.server.playerManager.broadcast(parsed, false)

            playerOffenses[player.uuid] = 0
        }
    }
}