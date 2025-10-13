package org.krripe.shadowchat

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.krripe.shadowchat.api.ShadowChat
import org.krripe.shadowchat.api.impl.DefaultShadowChatAPI
import org.krripe.shadowchat.command.ChatCommands
import org.krripe.shadowchat.command.NickCommand
import org.krripe.shadowchat.command.SpyChatCommand
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.events.ChatEventHandler
import org.krripe.shadowchat.manager.AnnouncementManager
import org.krripe.shadowchat.manager.BadWordManager
import org.krripe.shadowchat.manager.NicknameManager
import org.krripe.shadowchat.utils.TabAutoRefresher
class Shadowchat : ModInitializer {
    override fun onInitialize() {
        ConfigManager.load()
        BadWordManager.loadConfig()
        ChatCommands.registerCommands()
        ChatEventHandler.registerEvents()
        NicknameManager.load()
        NickCommand.register()
        SpyChatCommand.register()

        TabAutoRefresher.init()
        AnnouncementManager.init()

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            ShadowChat.initialize(server, DefaultShadowChatAPI(server)
            )
        }

        ServerLifecycleEvents.SERVER_STOPPED.register {
            ConfigManager.save()
        }
    }
}
