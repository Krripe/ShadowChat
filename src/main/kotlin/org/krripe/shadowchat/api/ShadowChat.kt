package org.krripe.shadowchat.api

import net.minecraft.server.MinecraftServer

object ShadowChat {
    @Volatile private var api: ShadowChatAPI? = null
    @Volatile private var serverRef: MinecraftServer? = null

    fun initialize(server: MinecraftServer, impl: ShadowChatAPI) {
        api = impl
        serverRef = server
    }

    fun isInitialized(): Boolean = api != null

    fun setServer(server: MinecraftServer) {
        if (serverRef == null) serverRef = server
    }

    fun get(): ShadowChatAPI {
        return api ?: throw IllegalStateException(
            "[ShadowChat] API not initialized yet. " + "Make sure the server is started and ShadowChat is loaded."
        )
    }
}
