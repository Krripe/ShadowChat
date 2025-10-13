package org.krripe.shadowchat.utils

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.manager.TabListManager
import org.krripe.shadowchat.manager.WeightManager

object TabAutoRefresher {
    private val REFRESH_TICKS get() = ConfigManager.chatConfig.tabSettings.autoRefreshTicks

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (server.ticks % REFRESH_TICKS == 0) {
                WeightManager.refreshAll(server)
                TabListManager.refreshAll(server)
            }
        }
    }
}