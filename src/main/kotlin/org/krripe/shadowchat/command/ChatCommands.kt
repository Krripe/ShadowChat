package org.krripe.shadowchat.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.manager.AnnouncementManager
import org.krripe.shadowchat.manager.CommandFormatManager
import org.krripe.shadowchat.manager.TabListManager
import org.krripe.shadowchat.manager.WeightManager
import org.krripe.shadowchat.utils.PermissionsSystem

object ChatCommands {
    private val perms = PermissionsSystem()

    fun registerCommands() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("schat")
                    .then(
                        CommandManager.literal("reload")
                            .requires { source -> perms.hasPermission(source, "shadowchat.reload") }
                            .executes { ctx ->
                                ConfigManager.load()
                                TabListManager.refreshAll(ctx.source.server)
                                WeightManager.refreshAll(ctx.source.server)
                                AnnouncementManager.onConfigReload(ctx.source.server)
                                ctx.source.sendFeedback({Text.literal("§8[§6ShadowChat§8] §aconfig reloaded.")}, false)
                                Command.SINGLE_SUCCESS
                            }
                    )
            )

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
