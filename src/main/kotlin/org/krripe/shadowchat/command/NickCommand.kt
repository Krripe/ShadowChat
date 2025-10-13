package org.krripe.shadowchat.command

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text
import org.krripe.shadowchat.manager.NicknameManager
import org.krripe.shadowchat.manager.TabListManager
import org.krripe.shadowchat.utils.PermissionsSystem

object NickCommand {
    private val perms = PermissionsSystem()

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("nick")
                    .requires { perms.hasPermission(it, "shadowchat.nick") }

                    .then(CommandManager.argument("nickname", StringArgumentType.greedyString())
                        .executes { ctx ->
                            val player = ctx.source.playerOrThrow
                            val nickname = StringArgumentType.getString(ctx, "nickname")
                            NicknameManager.setNickname(player, nickname)
                            TabListManager.pushDisplayNameUpdate(player)
                            ctx.source.sendFeedback({ Text.literal("§aNickname set to: $nickname") }, false)
                            1
                        })

                    .then(CommandManager.literal("reset")
                        .requires { perms.hasPermission(it, "shadowchat.nick.reset") }
                        .executes { ctx ->
                            val player = ctx.source.playerOrThrow
                            NicknameManager.resetNickname(player)
                            TabListManager.pushDisplayNameUpdate(player)
                            ctx.source.sendFeedback({ Text.literal("§cNickname reset.") }, false)
                            1
                        })
            )
        }
    }
}