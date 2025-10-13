package org.krripe.shadowchat.api.impl

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.krripe.shadowchat.api.*
import org.krripe.shadowchat.manager.*
import org.krripe.shadowchat.utils.CustomColorParser
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class DefaultShadowChatAPI(override val server: MinecraftServer) : ShadowChatAPI {
    private val listeners = CopyOnWriteArrayList<ShadowChatListener>()
    private val channels  = ConcurrentHashMap<String, ChatChannel>()

    override fun parseColors(raw: String): Text =
        CustomColorParser.processAllFormats(raw)

    override fun parseWithPlaceholders(source: ServerCommandSource, raw: String): Text {
        val colored = CustomColorParser.processAllFormats(raw)
        return Placeholders.parseText(colored, PlaceholderContext.of(source))
    }

    override fun formatPlayerChat(player: ServerPlayerEntity, rawMessage: String): Text =
        ChatFormatManager.getChatMessage(player, rawMessage)

    override fun getNicknameRaw(player: ServerPlayerEntity) = NicknameManager.getRaw(player)
    override fun getNicknameDisplay(player: ServerPlayerEntity) = NicknameManager.getNickname(player)

    override fun setNickname(player: ServerPlayerEntity, nicknameRaw: String?): Boolean {
        if (nicknameRaw.isNullOrBlank()) {
            NicknameManager.resetNickname(player)
            listeners.forEach { it.onNicknameChanged(player, null) }
            return true
        }
        NicknameManager.setNickname(player, nicknameRaw)
        listeners.forEach { it.onNicknameChanged(player, nicknameRaw) }
        return true
    }

    override fun refreshTabFor(player: ServerPlayerEntity) {
        TabListManager.sendHeaderFooter(player)
        TabListManager.pushDisplayNameUpdate(player)
        WeightManager.applyTeam(player)
    }

    override fun refreshTabAll() = TabListManager.refreshAll(server)

    override fun setNametagPrefix(player: ServerPlayerEntity, prefix: Text?) {
        NameTagManager.setOverridePrefix(player, prefix)
        WeightManager.applyTeam(player)
    }

    override fun setNametagSuffix(player: ServerPlayerEntity, suffix: Text?) {
        NameTagManager.setOverrideSuffix(player, suffix)
        WeightManager.applyTeam(player)
    }

    override fun isSpamming(player: ServerPlayerEntity) = SpamManager.shouldBlock(player)
    override fun warnSpam(player: ServerPlayerEntity) = SpamManager.warn(player)

    override fun broadcastSystem(text: Text) {
        server.playerManager.broadcast(text, false)
    }

    override fun sendStyledTo(player: ServerPlayerEntity, text: Text) {
        player.sendMessage(text, false)
    }

    override fun sendRawTo(player: ServerPlayerEntity, raw: String, ctx: ServerCommandSource?) {
        val src = ctx ?: player.commandSource
        sendStyledTo(player, parseWithPlaceholders(src, raw))
    }

    override fun sendRawToAll(raw: String, ctx: ServerCommandSource?) {
        val source = ctx ?: server.commandSource
        val parsed = parseWithPlaceholders(source, raw)
        broadcastSystem(parsed)
    }

    override fun sendChatFrom(player: ServerPlayerEntity, raw: String, bypassFilters: Boolean): Boolean {
        val pre = fireChatPre(ChatPreEvent(player, raw, bypassFilters = bypassFilters))
        if (pre.cancel) return false

        val formatted = pre.overrideFormatted
            ?: ChatFormatManager.getChatMessage(player, if (pre.bypassFilters) pre.message else
                org.krripe.shadowchat.utils.FormatPermissionFilter.filter(
                    BadWordManager.filterMessage(pre.message).first,
                    player.commandSource
                )
            )

        server.playerManager.broadcast(formatted, false)
        fireChatPost(player, formatted)
        LogManager.logChat(player, pre.message)
        return true
    }

    override fun placeholderContextOf(player: ServerPlayerEntity): PlaceholderContext =
        PlaceholderContext.of(player)

    override fun addListener(listener: ShadowChatListener) { listeners += listener }
    override fun removeListener(listener: ShadowChatListener) { listeners -= listener }

    override fun fireChatPre(ctx: ChatPreEvent): ChatPreEvent {
        listeners.forEach {
            try { it.onChatPre(ctx) } catch (_: Throwable) {}
        }
        return ctx
    }

    override fun fireChatPost(sender: ServerPlayerEntity, formatted: Text) {
        listeners.forEach {
            try { it.onChatPost(sender, formatted) } catch (_: Throwable) {}
        }
    }

    override fun firePrivateMessage(sender: ServerPlayerEntity, targets: List<ServerPlayerEntity>, content: String) {
        listeners.forEach {
            try { it.onPrivateMessage(sender, targets, content) } catch (_: Throwable) {}
        }
    }

    override fun fireBroadcast(source: ServerCommandSource, content: String, formatted: Text) {
        listeners.forEach {
            try { it.onBroadcast(source, content, formatted) } catch (_: Throwable) {}
        }
    }

    override fun fireJoin(player: ServerPlayerEntity, formatted: Text) {
        listeners.forEach {
            try { it.onJoin(player, formatted) } catch (_: Throwable) {}
        }
    }

    override fun fireLeave(player: ServerPlayerEntity, formatted: Text) {
        listeners.forEach {
            try { it.onLeave(player, formatted) } catch (_: Throwable) {}
        }
    }

    override fun fireDeath(player: ServerPlayerEntity, formatted: Text) {
        listeners.forEach {
            try { it.onDeath(player, formatted) } catch (_: Throwable) {}
        }
    }

    override fun registerChannel(channel: ChatChannel) { channels[channel.id.lowercase()] = channel }
    override fun unregisterChannel(id: String) { channels.remove(id.lowercase()) }

    override fun sendToChannel(id: String, sender: ServerPlayerEntity, content: String): Boolean {
        val ch = channels[id.lowercase()] ?: return false
        val perm = ch.permission
        if (perm != null && !org.krripe.shadowchat.utils.PermissionsSystem().hasPermission(sender.commandSource, perm)) {
            return false
        }
        val txt = ch.format(sender, content)
        for (p in ch.audience(server, sender)) p.sendMessage(txt, false)
        return true
    }
}
