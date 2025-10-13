package org.krripe.shadowchat.manager

import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.utils.CustomColorParser
import java.util.UUID

object SpamManager {
    private val history  = mutableMapOf<UUID, ArrayDeque<Long>>()
    private val lastWarn = mutableMapOf<UUID, Long>()

    private val cfg get() = ConfigManager.chatConfig.spam

    fun shouldBlock(player: ServerPlayerEntity): Boolean {
        if (!ConfigManager.chatConfig.chatSystems.spam) return false

        val now = System.currentTimeMillis()
        val deque = history.getOrPut(player.uuid) { ArrayDeque() }

        while (deque.isNotEmpty() && now - deque.first() > cfg.windowMs) {
            deque.removeFirst()
        }

        deque.addLast(now)
        return deque.size > cfg.maxMessages
    }

    fun warn(player: ServerPlayerEntity) {
        if (!ConfigManager.chatConfig.chatSystems.spam) return

        val now = System.currentTimeMillis()
        val last = lastWarn[player.uuid] ?: 0L
        if (now - last < 1_000) return

        lastWarn[player.uuid] = now
        val msg = cfg.warnMessage.replace("%player%", player.name.string)
        player.sendMessage(CustomColorParser.processAllFormats(msg), false)
    }
}
