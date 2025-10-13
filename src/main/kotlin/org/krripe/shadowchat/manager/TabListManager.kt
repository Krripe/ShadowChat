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
import java.util.EnumSet

object TabListManager {

    private fun isTabEnabled() = ConfigManager.chatConfig.chatSystems.tab


    private fun parse(lines: List<String>, ctx: PlaceholderContext): Text {
        val joined = lines.joinToString("\n")
        val withApi = PlaceholderBridge.apply(ctx.player as ServerPlayerEntity, joined)
        return Placeholders.parseText(
            CustomColorParser.processAllFormats(withApi),
            ctx
        )
    }

    fun entryText(p: ServerPlayerEntity): Text {
        val cfg  = ConfigManager.chatConfig.tab
        val nick = NicknameManager.getDisplayName(p)

        var raw = cfg.playerFormat
            .replace("%nickname%",     nick)
            .replace("%display_name%", nick)
            .replace("%name%",         p.name.string)

        raw = PlaceholderBridge.apply(p, raw)

        return Placeholders.parseText(
            CustomColorParser.processAllFormats(raw),
            PlaceholderContext.of(p)
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
