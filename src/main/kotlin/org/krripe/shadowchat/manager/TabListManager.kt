package org.krripe.shadowchat.manager

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.utils.CustomColorParser
import org.krripe.shadowchat.bridge.PlaceholderBridge
import org.krripe.shadowchat.config.RankFormat
import org.krripe.shadowchat.utils.PermissionsSystem
import java.util.EnumSet

object TabListManager {

    private fun isTabEnabled() = ConfigManager.chatConfig.chatSystems.tab

    private val perms = PermissionsSystem()

    private fun getRankFormat(p: ServerPlayerEntity): RankFormat? {
        if (!ConfigManager.chatConfig.chatSystems.rankFormat) return null
        return ConfigManager.chatConfig.rankFormats.firstOrNull { rf ->
            perms.hasPermission(p.commandSource, rf.rankPermission)
        }
    }

    private fun buildDisplayNameForTab(p: ServerPlayerEntity): Text {
        val ctx = PlaceholderContext.of(p)

        val nickRaw = NicknameManager.getRaw(p)
        if (!nickRaw.isNullOrBlank()) {
            val nick = nickRaw.replace("%player:name%", p.name.string)
            val bridged = PlaceholderBridge.apply(p, nick)
            return Placeholders.parseText(CustomColorParser.processAllFormats(bridged), ctx)
        }

        val rank = getRankFormat(p)
        val template = rank?.rankDisplayName?.takeIf { it.isNotBlank() } ?: ConfigManager.chatConfig.displayName

        val raw = template.replace("%player:name%", p.name.string)
        val bridged = PlaceholderBridge.apply(p, raw)

        return Placeholders.parseText(CustomColorParser.processAllFormats(bridged), ctx)
    }

    private fun applyBasicReplacements(p: ServerPlayerEntity, s: String): String {
        return s
            .replace("%nickname%", NicknameManager.getDisplayName(p))
            .replace("%display_name%", NicknameManager.getDisplayName(p)) // keep old behavior
            .replace("%name%", p.name.string)
    }

    private fun parse(lines: List<String>, ctx: PlaceholderContext): Text {
        val joined = lines.joinToString("\n")
        val withApi = PlaceholderBridge.apply(ctx.player as ServerPlayerEntity, joined)
        return Placeholders.parseText(
            CustomColorParser.processAllFormats(withApi),
            ctx
        )
    }

    fun entryText(p: ServerPlayerEntity): Text {
        val cfg = ConfigManager.chatConfig.tab
        val fmt = cfg.playerFormat

        val tokens = listOf(
            "\${displayname}",
            "\\\${displayname}",
            "\${display_name}",
            "\\\${display_name}",
            "%displayname%",
            "%display_name%"
        )

        val token = tokens.firstOrNull { fmt.contains(it) }

        if (token != null) {
            val idx = fmt.indexOf(token)
            val before0 = fmt.substring(0, idx)
            val after0 = fmt.substring(idx + token.length)

            val beforeStr = applyBasicReplacements(p, before0)
            val afterStr = applyBasicReplacements(p, after0)

            val before = PrefixManager.parseWithPrefixes(
                p,
                beforeStr
            )

            val after = PrefixManager.parseWithPrefixes(
                p,
                afterStr
            )

            val displayText = buildDisplayNameForTab(p)

            return Text.literal("")
                .append(before)
                .append(displayText)
                .append(after)
        }

        val raw = applyBasicReplacements(p, fmt)

        return PrefixManager.parseWithPrefixes(
            p,
            raw
        )
    }

    fun sendHeaderFooter(player: ServerPlayerEntity) {
        if (!isTabEnabled()) return

        val cfg = ConfigManager.chatConfig.tab
        val ctx = PlaceholderContext.of(player)

        player.networkHandler.sendPacket(
            PlayerListHeaderS2CPacket(
                parse(cfg.header, ctx),
                parse(cfg.footer, ctx)
            )
        )
    }

    fun pushDisplayNameUpdate(player: ServerPlayerEntity) {
        if (!isTabEnabled()) return

        val packet = PlayerListS2CPacket(
            EnumSet.of(
                PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME,
            ),
            listOf(player)
        )
        player.server.playerManager.sendToAll(packet)
    }

    fun refreshAll(server: MinecraftServer) {
        if (!isTabEnabled()) return
        server.playerManager.playerList.forEach {
            sendHeaderFooter(it)
            pushDisplayNameUpdate(it)
        }
    }
}
