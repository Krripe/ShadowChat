package org.krripe.shadowchat.manager

import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.utils.CustomColorParser
import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.PlaceholderContext

object CommandFormatManager {
    fun formatMsgSent(source: ServerCommandSource, receiverName: String, message: String): Text {

        val senderNick = NicknameManager.getDisplayName(source.player!!)
        val raw = ConfigManager.chatConfig.commandFormats.msgSent
            .replace("%receiver%", receiverName)
            .replace("%sender%", senderNick)
            .replace("%message%", message)

        return parse(raw, source)
    }

    fun formatMsgReceived(source: ServerCommandSource, senderName: String, message: String): Text {

        val targetNick = NicknameManager.getDisplayName(source.player!!)
        val raw = ConfigManager.chatConfig.commandFormats.msgReceived
            .replace("%sender%", senderName)
            .replace("%receiver%", targetNick)
            .replace("%message%", message)

        return parse(raw, source)
    }

    fun formatSay(source: ServerCommandSource, message: String): Text {

        val raw = ConfigManager.chatConfig.commandFormats.say
            .replace("%player%", NicknameManager.getRaw(source.player!!) ?: source.name)
            .replace("%message%", message)

        return parse(raw, source)
    }

    fun formatMe(source: ServerCommandSource, message: String): Text {

        val raw = ConfigManager.chatConfig.commandFormats.me
            .replace("%player%", NicknameManager.getRaw(source.player!!) ?: source.name)
            .replace("%message%", message)

        return parse(raw, source)
    }

    fun formatSpy(
        source: ServerCommandSource,
        sender: String,
        targets: String,
        msg: String
    ): Text {
        val raw = ConfigManager.chatConfig.commandFormats.spy
            .replace("%sender%",  sender)
            .replace("%targets%", targets)
            .replace("%message%", msg)

        return parse(raw, source)
    }

    private fun parse(text: String, source: ServerCommandSource): Text {
        val colorParsed = CustomColorParser.processAllFormats(text)
        return Placeholders.parseText(colorParsed, PlaceholderContext.of(source))
    }

    fun formatBroadcast(source: ServerCommandSource, message: String): Text {
        val raw = ConfigManager.chatConfig.commandFormats.broadcast
            .replace("%message%", message)

        val colored = CustomColorParser.processAllFormats(raw)
        return Placeholders.parseText(colored, PlaceholderContext.of(source))
    }
}