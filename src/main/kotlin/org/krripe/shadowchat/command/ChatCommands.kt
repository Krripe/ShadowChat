package org.krripe.shadowchat.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.config.MessageManager
import org.krripe.shadowchat.config.PrefixConfigManager
import org.krripe.shadowchat.manager.*
import org.krripe.shadowchat.utils.PermissionsSystem

object ChatCommands {
    private val perms = PermissionsSystem()

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("schat")
                    .then(
                        CommandManager.literal("reload")
                            .requires { source -> perms.hasPermission(source, "shadowchat.reload") }
                            .executes { ctx ->
                                ConfigManager.load()
                                MessageManager.load()
                                PrefixConfigManager.load()
                                BadWordManager.load()
                                PrefixManager.onConfigReload(ctx.source.server)
                                TabListManager.refreshAll(ctx.source.server)
                                WeightManager.refreshAll(ctx.source.server)
                                AnnouncementManager.onConfigReload(ctx.source.server)

                                try {
                                    val cfg = ConfigManager.chatConfig.joinMotd
                                    if (cfg.enabled && cfg.lines.isNotEmpty()) {
                                        for (p in ctx.source.server.playerManager.playerList) {
                                            JoinMotdManager.send(p)
                                        }
                                    }
                                } catch (_: Throwable) { }

                                ctx.source.sendFeedback({ MessageManager.get(ctx.source, "reloaded") }, false)
                                Command.SINGLE_SUCCESS
                            }
                    )
            )

            // /broadcast
            dispatcher.register(
                CommandManager.literal("broadcast")
                    .requires { src -> perms.hasPermission(src, "shadowchat.broadcast") }
                    .then(
                        CommandManager.argument("message", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val src = ctx.source
                                val msgArg = StringArgumentType.getString(ctx, "message")
                                val out = CommandFormatManager.formatBroadcast(src, msgArg)
                                src.server.playerManager.broadcast(out, false)
                                Command.SINGLE_SUCCESS
                            }
                    )
            )

            // /bc
            dispatcher.register(
                CommandManager.literal("bc")
                    .requires { src -> perms.hasPermission(src, "shadowchat.broadcast") }
                    .then(
                        CommandManager.argument("message", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val src = ctx.source
                                val msgArg = StringArgumentType.getString(ctx, "message")
                                val out = CommandFormatManager.formatBroadcast(src, msgArg)
                                src.server.playerManager.broadcast(out, false)
                                Command.SINGLE_SUCCESS
                            }
                    )
            )
        }
    }
}
