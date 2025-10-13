package org.krripe.shadowchat.manager

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.config.RankFormat
import org.krripe.shadowchat.utils.CustomColorParser
import org.krripe.shadowchat.utils.PermissionsSystem

object ChatFormatManager {

    private val perms = PermissionsSystem()

    private fun getRankFormat(player: ServerPlayerEntity): RankFormat? {
        if (!ConfigManager.chatConfig.chatSystems.rankFormat) return null

        return ConfigManager.chatConfig.rankFormats.firstOrNull { format ->
            val hasPerm = perms.hasPermission(player.commandSource, format.rankPermission)
            hasPerm
        }
    }

    fun getChatMessage(player: ServerPlayerEntity, rawMessage: String): Text {

        return if (ConfigManager.chatConfig.chatSystems.rankFormat) {
            val rankFormat = getRankFormat(player)
            if (rankFormat != null) {
                getRankChatMessage(player, rawMessage, rankFormat)
            } else {
                getDefaultChatMessage(player, rawMessage)
            }
        } else {
            getDefaultChatMessage(player, rawMessage)
        }
    }

    private fun getRankChatMessage(
        player: ServerPlayerEntity,
        rawMessage: String,
        format: RankFormat
    ): Text {
        val displayText: Text = NicknameManager.getNickname(player)

        val parts = format.rankChatFormat.split("\${display_name}")
        val beforeRaw = parts.getOrNull(0) ?: ""
        val afterRaw  = (parts.getOrNull(1) ?: "").replace("\${message}", rawMessage)

        val before = Placeholders.parseText(
            CustomColorParser.processAllFormats(beforeRaw),
            PlaceholderContext.of(player)
        )
        val after  = Placeholders.parseText(
            CustomColorParser.processAllFormats(afterRaw),
            PlaceholderContext.of(player)
        )

        return Text.literal("")
            .append(before)
            .append(displayText)
            .append(after)
    }

    private fun getDefaultChatMessage(
        player: ServerPlayerEntity,
        rawMessage: String
    ): Text {
        val displayText: Text = NicknameManager.getNickname(player)

        val parts = ConfigManager.chatConfig.chatFormat.split("\${display_name}")
        val beforeRaw = parts.getOrNull(0) ?: ""
        val afterRaw  = (parts.getOrNull(1) ?: "").replace("\${message}", rawMessage)

        val before = Placeholders.parseText(
            CustomColorParser.processAllFormats(beforeRaw),
            PlaceholderContext.of(player)
        )
        val after  = Placeholders.parseText(
            CustomColorParser.processAllFormats(afterRaw),
            PlaceholderContext.of(player)
        )

        return Text.literal("")
            .append(before)
            .append(displayText)
            .append(after)
    }

    fun getJoinMessage(player: ServerPlayerEntity): Text {
        return parseFormat(ConfigManager.chatConfig.joinMessage, player)
    }

    fun getLeaveMessage(player: ServerPlayerEntity): Text {
        return parseFormat(ConfigManager.chatConfig.leaveMessage, player)
    }

    private fun parseFormat(input: String, player: ServerPlayerEntity): Text {
        val withNick = input.replace(
            "%player:name%",
            NicknameManager.getDisplayName(player)
        )

        val colored = CustomColorParser.processAllFormats(withNick)
        return Placeholders.parseText(colored, PlaceholderContext.of(player))
    }

    fun getDeathMessage(player: ServerPlayerEntity, default: Text): Text {
        val raw = ConfigManager.chatConfig.baseDeath
            .replace("\${default_message}", default.string)
        return parseFormat(raw, player)
    }
}
