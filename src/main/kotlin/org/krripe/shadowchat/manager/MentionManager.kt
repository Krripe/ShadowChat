package org.krripe.shadowchat.manager

import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.utils.CustomColorParser
import org.krripe.shadowchat.utils.PermissionsSystem

object MentionManager {
    private val perms = PermissionsSystem()

    fun handleMentions(message: String, sender: ServerPlayerEntity) {
        val server = sender.server
        val lowerMessage = message.lowercase()
        val format = ConfigManager.chatConfig.mentionFormats
        val bypassIgnore = perms.hasPermission(sender.commandSource, "shadowchat.ignore.bypass")

        for (player in server.playerManager.playerList) {
            if (player == sender) continue

            if (!bypassIgnore && IgnoreManager.isIgnoring(player.uuid, sender.uuid)) continue

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