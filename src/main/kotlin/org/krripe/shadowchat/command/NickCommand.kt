package org.krripe.shadowchat.command

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.config.MessageManager
import org.krripe.shadowchat.manager.NicknameManager
import org.krripe.shadowchat.utils.CustomColorParser
import org.krripe.shadowchat.utils.PermissionsSystem

object NickCommand {

    private val perms = PermissionsSystem()

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->

            dispatcher.register(
                CommandManager.literal("nick")
                    .requires {
                        perms.hasPermission(
                            it,
                            "shadowchat.nick"
                        )
                    }
                    .then(
                        CommandManager.argument(
                            "nickname",
                            StringArgumentType.greedyString()
                        )
                            .executes { ctx ->
                                val player =
                                    ctx.source.playerOrThrow

                                val nickname =
                                    StringArgumentType.getString(
                                        ctx,
                                        "nickname"
                                    )

                                val maxLength =
                                    ConfigManager
                                        .chatConfig
                                        .nickSettings
                                        .maxLength

                                val visibleLength =
                                    CustomColorParser
                                        .processAllFormats(nickname)
                                        .string
                                        .length

                                if (
                                    maxLength > 0 &&
                                    visibleLength > maxLength
                                ) {
                                    ctx.source.sendFeedback(
                                        {
                                            MessageManager.get(
                                                ctx.source,
                                                "nickTooLong",
                                                "max" to maxLength.toString()
                                            )
                                        },
                                        false
                                    )

                                    return@executes 0
                                }

                                NicknameManager.setNickname(
                                    player,
                                    nickname
                                )

                                ctx.source.sendFeedback(
                                    {
                                        MessageManager.get(
                                            ctx.source,
                                            "nickSet",
                                            "nickname" to nickname
                                        )
                                    },
                                    false
                                )

                                1
                            }
                    )
                    .then(
                        CommandManager.literal("reset")
                            .requires {
                                perms.hasPermission(
                                    it,
                                    "shadowchat.nick.reset"
                                )
                            }
                            .executes { ctx ->
                                val player =
                                    ctx.source.playerOrThrow

                                NicknameManager.resetNickname(
                                    player
                                )

                                ctx.source.sendFeedback(
                                    {
                                        MessageManager.get(
                                            ctx.source,
                                            "nickReset"
                                        )
                                    },
                                    false
                                )

                                1
                            }
                    )
            )
        }
    }
}