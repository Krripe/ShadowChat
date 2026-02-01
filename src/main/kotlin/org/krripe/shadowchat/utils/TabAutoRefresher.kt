package org.krripe.shadowchat.utils

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.loader.api.FabricLoader
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.manager.TabListManager
import org.krripe.shadowchat.manager.WeightManager

object TabAutoRefresher {

    private fun isLuckPermsPresent(): Boolean =
        FabricLoader.getInstance().isModLoaded("luckperms")

    private fun refreshTicksSafe(): Int =
        ConfigManager.chatConfig.tabSettings.autoRefreshTicks.coerceAtLeast(1)

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (!ConfigManager.chatConfig.chatSystems.tab) return@register

            if (!isLuckPermsPresent()) return@register

            val interval = refreshTicksSafe()
            if (server.ticks % interval != 0) return@register

            WeightManager.refreshAll(server)
            TabListManager.refreshAll(server)
        }
    }
}
