package org.krripe.shadowchat.manager

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import org.krripe.shadowchat.bridge.PlaceholderBridge
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.config.RankFormat
import org.krripe.shadowchat.utils.CustomColorParser
import org.krripe.shadowchat.utils.PermissionsSystem

object ChatFormatManager {

    private val perms = PermissionsSystem()

    private fun getRankFormat(player: ServerPlayerEntity): RankFormat? {
        if (!ConfigManager.chatConfig.chatSystems.rankFormat) return null

        return ConfigManager.chatConfig.rankFormats.firstOrNull { format ->
            perms.hasPermission(
                player.commandSource,
                format.rankPermission
            )
        }
    }

    private fun buildDisplayName(
        player: ServerPlayerEntity,
        rank: RankFormat?
    ): Text {
        val ctx = PlaceholderContext.of(player)

        val nickRaw = NicknameManager.getRaw(player)

        val displayText = if (!nickRaw.isNullOrBlank()) {
            Placeholders.parseText(
                CustomColorParser.processAllFormats(nickRaw),
                ctx
            )
        } else {
            val template =
                rank?.rankDisplayName
                    ?.takeIf { it.isNotBlank() }
                    ?: ConfigManager.chatConfig.displayName

            val raw = template.replace(
                "%player:name%",
                player.name.string
            )

            Placeholders.parseText(
                CustomColorParser.processAllFormats(raw),
                ctx
            )
        }

        return applyDisplayNameHover(
            player,
            displayText
        )
    }

    private fun applyDisplayNameHover(
        player: ServerPlayerEntity,
        displayText: Text
    ): Text {
        val cfg = ConfigManager.chatConfig.displayNameHover

        if (!cfg.enabled) {
            return displayText
        }

        var style = Style.EMPTY

        if (cfg.lines.isNotEmpty()) {
            val hoverText = Text.literal("")

            cfg.lines.forEachIndexed { index, line ->
                if (index > 0) {
                    hoverText.append(Text.literal("\n"))
                }

                hoverText.append(
                    PrefixManager.parseWithPrefixes(
                        player,
                        line
                    )
                )
            }

            style = style.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    hoverText
                )
            )
        }

        if (cfg.clickable && cfg.clickValue.isNotBlank()) {
            val clickValue = resolveClickValue(
                player,
                cfg.clickValue
            )

            val clickEvent = when (
                cfg.clickAction.trim().lowercase()
            ) {
                "run_command" -> ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    clickValue
                )

                "open_url" -> ClickEvent(
                    ClickEvent.Action.OPEN_URL,
                    clickValue
                )

                "copy_to_clipboard" -> ClickEvent(
                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                    clickValue
                )

                else -> ClickEvent(
                    ClickEvent.Action.SUGGEST_COMMAND,
                    clickValue
                )
            }

            style = style.withClickEvent(clickEvent)
        }

        return Text.literal("")
            .setStyle(style)
            .append(displayText)
    }

    private fun resolveClickValue(
        player: ServerPlayerEntity,
        raw: String
    ): String {
        var value = raw
            .replace(
                "%player:name%",
                player.name.string
            )
            .replace(
                "%name%",
                player.name.string
            )
            .replace(
                "%nickname%",
                NicknameManager.getDisplayName(player)
            )

        value = PlaceholderBridge.apply(
            player,
            value
        )

        return value
    }

    fun getChatMessage(
        player: ServerPlayerEntity,
        rawMessage: String
    ): Text {
        if (ConfigManager.chatConfig.chatSystems.rankFormat) {
            val rankFormat = getRankFormat(player)

            if (rankFormat != null) {
                return getFormatted(
                    player,
                    rawMessage,
                    rankFormat.rankChatFormat,
                    rankFormat
                )
            }
        }

        return getFormatted(
            player,
            rawMessage,
            ConfigManager.chatConfig.chatFormat,
            null
        )
    }

    private fun getFormatted(
        player: ServerPlayerEntity,
        rawMessage: String,
        formatRaw: String,
        rank: RankFormat?
    ): Text {
        val displayText = buildDisplayName(
            player,
            rank
        )

        val (beforeRaw, afterRaw) =
            splitOnDisplayToken(formatRaw)

        val before = PrefixManager.parseWithPrefixes(
            player,
            beforeRaw
        )

        val after = PrefixManager.parseWithPrefixes(
            player,
            replaceMessageTokens(
                afterRaw,
                rawMessage
            )
        )

        return Text.literal("")
            .append(before)
            .append(displayText)
            .append(after)
    }

    private fun splitOnDisplayToken(
        raw: String
    ): Pair<String, String> {
        val tokens = listOf(
            "\${display_name}",
            "\\\${display_name}"
        )

        for (token in tokens) {
            val index = raw.indexOf(token)

            if (index >= 0) {
                val before =
                    raw.substring(0, index)

                val after =
                    raw.substring(
                        index + token.length
                    )

                return before to after
            }
        }

        val cleaned = raw
            .replace("\${display_name}", "")
            .replace("\\\${display_name}", "")

        return cleaned to ""
    }

    private fun replaceMessageTokens(
        raw: String,
        message: String
    ): String {
        return raw
            .replace("\${message}", message)
            .replace("\\\${message}", message)
    }

    fun getJoinMessage(
        player: ServerPlayerEntity
    ): Text =
        parseFormat(
            ConfigManager.chatConfig.joinMessage,
            player
        )

    fun getLeaveMessage(
        player: ServerPlayerEntity
    ): Text =
        parseFormat(
            ConfigManager.chatConfig.leaveMessage,
            player
        )

    private fun parseFormat(
        input: String,
        player: ServerPlayerEntity
    ): Text {
        val withNick = input.replace(
            "%player:name%",
            NicknameManager.getDisplayName(player)
        )

        return PrefixManager.parseWithPrefixes(
            player,
            withNick
        )
    }

    fun getDeathMessage(
        player: ServerPlayerEntity,
        default: Text
    ): Text {
        val raw = ConfigManager.chatConfig.baseDeath
            .replace(
                "\${default_message}",
                default.string
            )
            .replace(
                "\\\${default_message}",
                default.string
            )

        return parseFormat(
            raw,
            player
        )
    }
}