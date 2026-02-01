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
            perms.hasPermission(player.commandSource, format.rankPermission)
        }
    }

    private fun buildDisplayName(player: ServerPlayerEntity, rank: RankFormat?): Text {
        val ctx = PlaceholderContext.of(player)

        val nickRaw = NicknameManager.getRaw(player)
        if (!nickRaw.isNullOrBlank()) {
            return Placeholders.parseText(
                CustomColorParser.processAllFormats(nickRaw),
                ctx
            )
        }

        val template = rank?.rankDisplayName?.takeIf { it.isNotBlank() }
            ?: ConfigManager.chatConfig.displayName

        val raw = template.replace("%player:name%", player.name.string)

        return Placeholders.parseText(
            CustomColorParser.processAllFormats(raw),
            ctx
        )
    }

    fun getChatMessage(player: ServerPlayerEntity, rawMessage: String): Text {
        if (ConfigManager.chatConfig.chatSystems.rankFormat) {
            val rankFormat = getRankFormat(player)
            if (rankFormat != null) {
                return getFormatted(player, rawMessage, rankFormat.rankChatFormat, rankFormat)
            }
        }
        return getFormatted(player, rawMessage, ConfigManager.chatConfig.chatFormat, null)
    }

    private fun getFormatted(player: ServerPlayerEntity, rawMessage: String, formatRaw: String, rank: RankFormat?): Text {
        val displayText = buildDisplayName(player, rank)
        val (beforeRaw, afterRaw) = splitOnDisplayToken(formatRaw)

        val before = Placeholders.parseText(
            CustomColorParser.processAllFormats(beforeRaw),
            PlaceholderContext.of(player)
        )

        val after = Placeholders.parseText(
            CustomColorParser.processAllFormats(replaceMessageTokens(afterRaw, rawMessage)),
            PlaceholderContext.of(player)
        )

        return Text.literal("")
            .append(before)
            .append(displayText)
            .append(after)
    }

    private fun splitOnDisplayToken(raw: String): Pair<String, String> {
        val tokens = listOf("\${display_name}", "\\\${display_name}")

        for (t in tokens) {
            val idx = raw.indexOf(t)
            if (idx >= 0) {
                val before = raw.substring(0, idx)
                val after = raw.substring(idx + t.length)
                return before to after
            }
        }

        val cleaned = raw
            .replace("\${display_name}", "")
            .replace("\\\${display_name}", "")

        return cleaned to ""
    }

    private fun replaceMessageTokens(raw: String, msg: String): String {
        return raw
            .replace("\${message}", msg)
            .replace("\\\${message}", msg)
    }

    fun getJoinMessage(player: ServerPlayerEntity): Text =
        parseFormat(ConfigManager.chatConfig.joinMessage, player)

    fun getLeaveMessage(player: ServerPlayerEntity): Text =
        parseFormat(ConfigManager.chatConfig.leaveMessage, player)

    private fun parseFormat(input: String, player: ServerPlayerEntity): Text {
        val withNick = input.replace("%player:name%", NicknameManager.getDisplayName(player))
        val colored = CustomColorParser.processAllFormats(withNick)
        return Placeholders.parseText(colored, PlaceholderContext.of(player))
    }

    fun getDeathMessage(player: ServerPlayerEntity, default: Text): Text {
        val raw = ConfigManager.chatConfig.baseDeath
            .replace("\${default_message}", default.string)
            .replace("\\\${default_message}", default.string)

        return parseFormat(raw, player)
    }
}