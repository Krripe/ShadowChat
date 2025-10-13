package org.krripe.shadowchat.api

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.server.MinecraftServer

data class ChatPreEvent(
    val sender: ServerPlayerEntity,
    var message: String,
    var cancel: Boolean = false,
    var overrideFormatted: Text? = null,
    var bypassFilters: Boolean = false
)

data class ChatPostEvent(
    val sender: ServerPlayerEntity,
    val formatted: Text
)

interface ChatChannel {
    val id: String
    val permission: String? get() = null
    fun format(sender: ServerPlayerEntity, content: String): Text
    fun audience(server: MinecraftServer, sender: ServerPlayerEntity): Collection<ServerPlayerEntity>
}
