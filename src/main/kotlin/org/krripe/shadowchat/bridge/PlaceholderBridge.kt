package org.krripe.shadowchat.bridge

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.network.ServerPlayerEntity

object PlaceholderBridge {
    private data class Bridge(
        val modId: String,
        val parser: (ServerPlayerEntity, String) -> String
    )

    private val bridges = listOf(
        Bridge("text_placeholder_api") { player, txt ->
            val cls  = Class.forName("eu.pb4.textplaceholderapi.TextPlaceholderAPI")
            val meth = cls.getMethod("render", ServerPlayerEntity::class.java, String::class.java)
            meth.invoke(null, player, txt) as String
        },
    )

    fun apply(player: ServerPlayerEntity, string: String): String {
        var out = string
        for (b in bridges) {
            if (FabricLoader.getInstance().isModLoaded(b.modId)) {
                try {
                    out = b.parser(player, out)
                } catch (t: Throwable) {
                }
            }
        }
        return out
    }
}