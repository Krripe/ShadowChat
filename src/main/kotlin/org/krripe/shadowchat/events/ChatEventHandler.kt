package org.krripe.shadowchat.events

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.krripe.shadowchat.api.ShadowChat
import org.krripe.shadowchat.api.ChatPreEvent
import org.krripe.shadowchat.compat.TriviaCompat
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.manager.*
import org.krripe.shadowchat.utils.FormatPermissionFilter

object ChatEventHandler {
    fun registerEvents() {
        ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
            server.execute {
                val p = handler.player
                val joinMsg = ChatFormatManager.getJoinMessage(p)
                server.playerManager.broadcast(joinMsg, false)

                try {
                    ShadowChat.get().fireJoin(p, joinMsg)
                } catch (_: Throwable) {  }

                if (ConfigManager.chatConfig.chatSystems.tab) {
                    TabListManager.sendHeaderFooter(p)
                    TabListManager.pushDisplayNameUpdate(p)
                }
                if (ConfigManager.chatConfig.chatSystems.weights) {
                    WeightManager.applyTeam(p)
                }
            }

            NicknameManager.getRaw(handler.player)?.let {
                NicknameManager.setNickname(handler.player, it)
            }
        }

        ServerLifecycleEvents.SERVER_STARTED.register { srv ->
            if (ConfigManager.chatConfig.chatSystems.weights) {
                WeightManager.refreshAll(srv)
            }
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            val leaveMsg = ChatFormatManager.getLeaveMessage(handler.player)
            server.playerManager.broadcast(leaveMsg, false)

            try {
                ShadowChat.get().fireLeave(handler.player, leaveMsg)
            } catch (_: Throwable) { }
        }

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { message, sender, _ ->

            val api = try { ShadowChat.get() } catch (_: Throwable) { null }

            val pre = api?.fireChatPre(ChatPreEvent(sender, message.signedContent))
            if (pre?.cancel == true) {
                return@register false
            }

            val effectiveRaw = pre?.message ?: message.signedContent
            val bypass = pre?.bypassFilters == true

            if (!bypass && ConfigManager.chatConfig.chatSystems.spam && SpamManager.shouldBlock(sender)) {
                SpamManager.warn(sender)
                return@register false
            }

            TriviaCompat.tryAnswerTrivia(sender, effectiveRaw)

            val (censored, offenseCount, original) =
                if (bypass) Triple(effectiveRaw, 0, effectiveRaw)
                else BadWordManager.filterMessage(effectiveRaw)

            if (!bypass) {
                BadWordManager.handleBadWords(sender, offenseCount)
            }

            val colorSafe =
                if (bypass) censored
                else FormatPermissionFilter.filter(censored, sender.commandSource)

            val formatted = pre?.overrideFormatted
                ?: ChatFormatManager.getChatMessage(sender, colorSafe)

            MentionManager.handleMentions(censored, sender)
            LogManager.logChat(sender, original)

            sender.server.playerManager.broadcast(formatted, false)

            try {
                api?.fireChatPost(sender, formatted)
            } catch (_: Throwable) { }

            false
        }
    }
}