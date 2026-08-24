package org.krripe.shadowchat.manager

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.krripe.shadowchat.bridge.PlaceholderBridge
import org.krripe.shadowchat.config.ConfigManager
import java.util.UUID

object NameTagManager {
    private val overridePrefix = mutableMapOf<UUID, Text?>()
    private val overrideSuffix = mutableMapOf<UUID, Text?>()

    fun setOverridePrefix(p: ServerPlayerEntity, value: Text?) {
        if (value == null) overridePrefix.remove(p.uuid) else overridePrefix[p.uuid] = value
    }

    fun setOverrideSuffix(p: ServerPlayerEntity, value: Text?) {
        if (value == null) overrideSuffix.remove(p.uuid) else overrideSuffix[p.uuid] = value
    }

    private fun parse(
        raw: String,
        p: ServerPlayerEntity
    ): Text {
        val value = raw
            .replace("%player:name%", p.name.string)
            .replace("%name%", p.name.string)
            .replace("%nickname%", NicknameManager.getDisplayName(p))
            .replace("%displayname%", NicknameManager.getDisplayName(p))
            .replace("%display_name%", NicknameManager.getDisplayName(p))

        return PrefixManager.parseWithPrefixes(
            p,
            value
        )
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

    fun prefix(p: ServerPlayerEntity): Text =
        overridePrefix[p.uuid] ?: fromConfigPrefix(p)

    fun suffix(p: ServerPlayerEntity): Text =
        overrideSuffix[p.uuid] ?: fromConfigSuffix(p)

    fun nameColor(p: ServerPlayerEntity): Formatting {
        if (!ConfigManager.chatConfig.chatSystems.nameTags) return Formatting.WHITE

        val rawCfg = ConfigManager.chatConfig.nameTag.nameColor
        if (rawCfg.isBlank()) return Formatting.WHITE

        val resolved = PlaceholderBridge.apply(p, rawCfg).trim()

        return parseLegacyColorToFormatting(resolved) ?: Formatting.WHITE
    }

    private fun parseLegacyColorToFormatting(s: String): Formatting? {
        val idx = s.indexOf('§').takeIf { it >= 0 } ?: s.indexOf('&').takeIf { it >= 0 }
        if (idx != null && idx + 1 < s.length) {
            val code = s[idx + 1].lowercaseChar()
            return when (code) {
                '0' -> Formatting.BLACK
                '1' -> Formatting.DARK_BLUE
                '2' -> Formatting.DARK_GREEN
                '3' -> Formatting.DARK_AQUA
                '4' -> Formatting.DARK_RED
                '5' -> Formatting.DARK_PURPLE
                '6' -> Formatting.GOLD
                '7' -> Formatting.GRAY
                '8' -> Formatting.DARK_GRAY
                '9' -> Formatting.BLUE
                'a' -> Formatting.GREEN
                'b' -> Formatting.AQUA
                'c' -> Formatting.RED
                'd' -> Formatting.LIGHT_PURPLE
                'e' -> Formatting.YELLOW
                'f' -> Formatting.WHITE
                'r' -> Formatting.WHITE
                else -> null
            }
        }

        val byName = Formatting.byName(s.lowercase())
        return byName?.takeIf { it.isColor }
    }
}