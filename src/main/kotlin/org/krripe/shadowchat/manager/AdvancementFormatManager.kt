package org.krripe.shadowchat.manager

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.text.TranslatableTextContent
import org.krripe.shadowchat.bridge.PlaceholderBridge
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.utils.CustomColorParser

object AdvancementFormatManager {

    fun formatFromVanilla(player: ServerPlayerEntity, vanilla: Text): Text {
        val cfg = ConfigManager.chatConfig.advancementFormats
        if (!cfg.enabled) return vanilla

        val content = vanilla.content
        if (content !is TranslatableTextContent) return vanilla

        val key = content.key
        val args = content.args

        val template = when {
            key.endsWith(".task") -> cfg.task
            key.endsWith(".goal") -> cfg.goal
            key.endsWith(".challenge") -> cfg.challenge
            else -> return vanilla
        }

        val advTitle = (args.getOrNull(1) as? Text) ?: Text.literal("")

        return buildFromTemplate(player, template, advTitle)
    }

    private fun buildFromTemplate(player: ServerPlayerEntity, rawTemplate: String, advancementTitle: Text): Text {
        val ctx = PlaceholderContext.of(player)

        fun tokenText(token: String): Text? = when (token) {
            "%player%" -> Text.literal(player.gameProfile.name)
            "%player_display%" -> player.displayName
            "%advancement%" -> advancementTitle
            else -> null
        }

        val tokens = listOf("%player_display%", "%player%", "%advancement%")

        fun parseSegment(segment: String): Text {
            if (segment.isEmpty()) return Text.empty()
            val bridged = PlaceholderBridge.apply(player, segment)
            val colored = CustomColorParser.processAllFormats(bridged)
            return Placeholders.parseText(colored, ctx)
        }

        var s = rawTemplate
        val out = Text.literal("")

        while (s.isNotEmpty()) {
            var bestToken: String? = null
            var bestIdx = Int.MAX_VALUE

            for (t in tokens) {
                val idx = s.indexOf(t)
                if (idx >= 0 && idx < bestIdx) {
                    bestIdx = idx
                    bestToken = t
                }
            }

            if (bestToken == null) {
                out.append(parseSegment(s))
                break
            }

            val before = s.substring(0, bestIdx)
            out.append(parseSegment(before))

            out.append(tokenText(bestToken) ?: Text.empty())

            s = s.substring(bestIdx + bestToken.length)
        }

        return out
    }
}