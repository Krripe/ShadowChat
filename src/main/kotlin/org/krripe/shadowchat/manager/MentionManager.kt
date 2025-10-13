package org.krripe.shadowchat.manager

import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.utils.CustomColorParser

object MentionManager {
    fun handleMentions(message: String, sender: ServerPlayerEntity) {
        val server = sender.server
        val lowerMessage = message.lowercase()
        val format = ConfigManager.chatConfig.mentionFormats

        for (player in server.playerManager.playerList) {
            if (player == sender) continue
            val name = player.name.string

            if (lowerMessage.contains(name.lowercase())) {
                val line1 = format.mentionedTitle.replace("%sender%", sender.name.string)
                val line2 = format.mentionedMessage.replace("%message%", message)

                val msg1 = CustomColorParser.processAllFormats(line1)
                val msg2 = CustomColorParser.processAllFormats(line2)

                player.server.execute {
                    player.sendMessage(msg1, false)
                    player.sendMessage(msg2, false)
                }
            }
        }
    }
}
