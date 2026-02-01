package org.krripe.shadowchat.manager

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.utils.CustomColorParser

object JoinMotdManager {

    fun send(player: ServerPlayerEntity) {
        val cfg = ConfigManager.chatConfig.joinMotd
        if (!cfg.enabled) return

        val lines = cfg.lines.filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        val ctx = PlaceholderContext.of(player)

        for (raw in lines) {
            val withName = raw.replace("%player:name%", NicknameManager.getDisplayName(player))

            val colored = CustomColorParser.processAllFormats(withName)
            val parsed: Text = Placeholders.parseText(colored, ctx)

            player.sendMessage(parsed, false)
        }
    }
}