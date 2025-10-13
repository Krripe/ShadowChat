package org.krripe.shadowchat.manager

import java.util.UUID
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.utils.CustomColorParser
import org.krripe.shadowchat.bridge.PlaceholderBridge

object NameTagManager {
    private val overridePrefix = mutableMapOf<UUID, Text?>()
    private val overrideSuffix = mutableMapOf<UUID, Text?>()

    fun setOverridePrefix(p: ServerPlayerEntity, value: Text?) {
        if (value == null) overridePrefix.remove(p.uuid) else overridePrefix[p.uuid] = value
    }
    fun setOverrideSuffix(p: ServerPlayerEntity, value: Text?) {
        if (value == null) overrideSuffix.remove(p.uuid) else overrideSuffix[p.uuid] = value
    }

    private fun parse(raw: String, p: ServerPlayerEntity): Text {
        var s = raw
            .replace("%nickname%", NicknameManager.getDisplayName(p))
            .replace("%display_name%", NicknameManager.getDisplayName(p))
            .replace("%name%", p.name.string)
        s = PlaceholderBridge.apply(p, s)
        val colored = CustomColorParser.processAllFormats(s)
        return Placeholders.parseText(colored, PlaceholderContext.of(p))
    }

    private fun fromConfigPrefix(p: ServerPlayerEntity): Text {
        if (!ConfigManager.chatConfig.chatSystems.nameTags) return Text.empty()
        val raw = ConfigManager.chatConfig.nameTag.prefix
        return if (raw.isBlank()) Text.empty() else parse(raw, p)
    }
    private fun fromConfigSuffix(p: ServerPlayerEntity): Text {
        if (!ConfigManager.chatConfig.chatSystems.nameTags) return Text.empty()
        val raw = ConfigManager.chatConfig.nameTag.suffix
        return if (raw.isBlank()) Text.empty() else parse(raw, p)
    }

    fun prefix(p: ServerPlayerEntity): Text = overridePrefix[p.uuid] ?: fromConfigPrefix(p)
    fun suffix(p: ServerPlayerEntity): Text = overrideSuffix[p.uuid] ?: fromConfigSuffix(p)
}
