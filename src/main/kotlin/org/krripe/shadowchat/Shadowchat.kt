package org.krripe.shadowchat

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.krripe.shadowchat.api.ShadowChat
import org.krripe.shadowchat.api.impl.DefaultShadowChatAPI
import org.krripe.shadowchat.command.ChatCommands
import org.krripe.shadowchat.command.IgnoreCommand
import org.krripe.shadowchat.command.NickCommand
import org.krripe.shadowchat.command.ReplyCommand
import org.krripe.shadowchat.command.SpyChatCommand
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.config.MessageManager
import org.krripe.shadowchat.events.ChatEventHandler
import org.krripe.shadowchat.manager.AnnouncementManager
import org.krripe.shadowchat.manager.BadWordManager
import org.krripe.shadowchat.manager.NicknameManager
import org.krripe.shadowchat.utils.RunLater
import org.krripe.shadowchat.utils.TabAutoRefresher

class Shadowchat : ModInitializer {
    override fun onInitialize() {
        ConfigManager.load()
        MessageManager.load()
        NicknameManager.load()
        BadWordManager.load()

        ChatCommands.register()
        ChatEventHandler.register()
        NickCommand.register()
        SpyChatCommand.register()
        ReplyCommand.register()

        TabAutoRefresher.init()
        AnnouncementManager.init()
        RunLater.init()

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            IgnoreCommand.register(dispatcher)
        }

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            ShadowChat.initialize(server, DefaultShadowChatAPI(server))
        }

        ServerLifecycleEvents.SERVER_STOPPED.register {
            ConfigManager.save()
        }
    }
}