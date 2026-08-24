package org.krripe.shadowchat.events

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import org.krripe.shadowchat.api.ChatPreEvent
import org.krripe.shadowchat.api.ShadowChat
import org.krripe.shadowchat.compat.TriviaCompat
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.manager.*
import org.krripe.shadowchat.utils.FormatPermissionFilter
import org.krripe.shadowchat.utils.PermissionsSystem
import org.krripe.shadowchat.utils.RunLater

object ChatEventHandler {

    private val perms = PermissionsSystem()

    fun register() {

        ServerPlayConnectionEvents.JOIN.register { handler, _, server ->
            server.execute {
                val p = handler.player

                PrefixManager.ensureLoaded(p)

                val joinLeaveCfg = ConfigManager.chatConfig.joinLeaveMessages
                val customJoinEnabled =
                    joinLeaveCfg.enabled &&
                            ConfigManager.chatConfig.joinMessage.isNotBlank()

                val joinMsg = ChatFormatManager.getJoinMessage(p)

                val motdCfg = ConfigManager.chatConfig.joinMotd
                val motdEnabled = motdCfg.enabled && motdCfg.lines.any { it.isNotBlank() }
                val sendBefore = motdEnabled && customJoinEnabled && motdCfg.sendBeforeJoinMessage

                if (customJoinEnabled) {
                    if (sendBefore) {
                        for (online in server.playerManager.playerList) {
                            if (online !== p) online.sendMessage(joinMsg, false)
                        }
                    } else {
                        server.playerManager.broadcast(joinMsg, false)
                    }

                    try {
                        ShadowChat.get().fireJoin(p, joinMsg)
                    } catch (_: Throwable) { }
                }

                if (ConfigManager.chatConfig.chatSystems.tab) {
                    TabListManager.sendHeaderFooter(p)
                }

                WeightManager.applyTeam(p)

                if (ConfigManager.chatConfig.chatSystems.tab) {
                    TabListManager.pushDisplayNameUpdate(p)
                }

                try {
                    if (motdEnabled) {
                        val delay = motdCfg.sendAfterTicks.coerceAtLeast(0)
                        val sendBlock = {
                            JoinMotdManager.send(p)

                            if (sendBefore) {
                                p.sendMessage(joinMsg, false)
                            }
                        }

                        if (delay <= 0) sendBlock()
                        else RunLater.run(server, delay) { sendBlock() }
                    }
                } catch (_: Throwable) { }
            }

            NicknameManager.getRaw(handler.player)?.let {
                NicknameManager.setNickname(handler.player, it)

                try {
                    WeightManager.applyTeam(handler.player)
                    if (ConfigManager.chatConfig.chatSystems.tab) {
                        TabListManager.pushDisplayNameUpdate(handler.player)
                    }
                } catch (_: Throwable) { }
            }
        }

        ServerLifecycleEvents.SERVER_STARTED.register { srv ->

            WeightManager.refreshAll(srv)

            if (ConfigManager.chatConfig.chatSystems.tab) {
                TabListManager.refreshAll(srv)
            }
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            val player = handler.player

            val joinLeaveCfg = ConfigManager.chatConfig.joinLeaveMessages
            val customLeaveEnabled =
                joinLeaveCfg.enabled &&
                        ConfigManager.chatConfig.leaveMessage.isNotBlank()

            if (customLeaveEnabled) {
                val leaveMsg = ChatFormatManager.getLeaveMessage(player)
                server.playerManager.broadcast(leaveMsg, false)

                try {
                    ShadowChat.get().fireLeave(player, leaveMsg)
                } catch (_: Throwable) { }
            }

            PrefixManager.unload(player)
        }

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register { signed, sender, params ->

            val api = try { ShadowChat.get() } catch (_: Throwable) { null }

            val pre = api?.fireChatPre(ChatPreEvent(sender, signed.signedContent))
            if (pre?.cancel == true) {
                return@register false
            }

            val effectiveRaw = pre?.message ?: signed.signedContent
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

            val bypassIgnore = perms.hasPermission(sender.commandSource, "shadowchat.ignore.bypass")

            for (viewer in sender.server.playerManager.playerList) {
                if (viewer === sender) {
                    viewer.sendMessage(formatted, false)
                    continue
                }

                if (!bypassIgnore && IgnoreManager.isIgnoring(viewer.uuid, sender.uuid)) continue
                viewer.sendMessage(formatted, false)
            }

            try {
                api?.fireChatPost(sender, formatted)
            } catch (_: Throwable) { }

            try {
                ServerMessageEvents.CHAT_MESSAGE.invoker().onChatMessage(signed, sender, params)
            } catch (_: Throwable) { }

            false
        }
    }
}