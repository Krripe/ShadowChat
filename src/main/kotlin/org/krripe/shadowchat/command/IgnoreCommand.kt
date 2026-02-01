package org.krripe.shadowchat.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.config.MessageManager
import org.krripe.shadowchat.manager.IgnoreManager
import org.krripe.shadowchat.utils.PermissionsSystem

object IgnoreCommand {

    private val perms = PermissionsSystem()

    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        dispatcher.register(
            literal("ignore")
                .requires { src ->
                    src.entity is ServerPlayerEntity && perms.hasPermission(src, "shadowchat.ignore")
                }

                // /ignore <player> toggle
                .then(
                    argument("player", EntityArgumentType.player())
                        .executes { ctx ->
                            val viewer = ctx.source.player!!
                            val target = EntityArgumentType.getPlayer(ctx, "player")

                            if (target.uuid == viewer.uuid) {
                                viewer.sendMessage(MessageManager.get(viewer, "ignoreCannotSelf"), false)
                                return@executes 0
                            }

                            if (
                                perms.hasPermission(target.commandSource, "shadowchat.ignore.immune") &&
                                !perms.hasPermission(viewer.commandSource, "shadowchat.ignore.override")
                            ) {
                                viewer.sendMessage(
                                    MessageManager.get(viewer, "ignoreCannotIgnore", "target" to target.name.string),
                                    false
                                )
                                return@executes 0
                            }

                            val nowIgnored = IgnoreManager.toggleIgnore(
                                viewer = viewer.uuid,
                                target = target.uuid,
                                targetName = target.name.string
                            )

                            IgnoreManager.saveAsync(ctx.source.server, viewer.uuid)

                            if (nowIgnored) {
                                viewer.sendMessage(
                                    MessageManager.get(viewer, "ignoreOn", "target" to target.name.string),
                                    false
                                )
                            } else {
                                viewer.sendMessage(
                                    MessageManager.get(viewer, "ignoreOff", "target" to target.name.string),
                                    false
                                )
                            }
                            1
                        }
                )

                // /ignore list
                .then(
                    literal("list")
                        .requires { src -> perms.hasPermission(src, "shadowchat.ignore.list") }
                        .executes { ctx ->
                            val viewer = ctx.source.player!!
                            val ignored = IgnoreManager.getIgnored(viewer.uuid)

                            if (ignored.isEmpty()) {
                                viewer.sendMessage(MessageManager.get(viewer, "ignoreListEmpty"), false)
                                return@executes 1
                            }

                            val names = ignored.map { uuid ->
                                ctx.source.server.playerManager.getPlayer(uuid)?.name?.string
                                    ?: IgnoreManager.getLastKnownName(viewer.uuid, uuid)
                                    ?: uuid.toString().substring(0, 8)
                            }.sorted()

                            viewer.sendMessage(
                                MessageManager.get(
                                    viewer,
                                    "ignoreListHeader",
                                    "count" to names.size.toString(),
                                    "list" to names.joinToString(", ")
                                ),
                                false
                            )
                            1
                        }
                )

                // /ignore clear (self)
                .then(
                    literal("clear")
                        .requires { src -> perms.hasPermission(src, "shadowchat.ignore.clear") }
                        .executes { ctx ->
                            val viewer = ctx.source.player!!
                            IgnoreManager.clear(viewer.uuid)
                            IgnoreManager.saveAsync(ctx.source.server, viewer.uuid)

                            viewer.sendMessage(MessageManager.get(viewer, "ignoreClearedSelf"), false)
                            1
                        }

                        // /ignore clear <player> (other)
                        .then(
                            argument("player", EntityArgumentType.player())
                                .requires { src -> perms.hasPermission(src, "shadowchat.ignore.clear.other") }
                                .executes { ctx ->
                                    val admin = ctx.source.player!!
                                    val target = EntityArgumentType.getPlayer(ctx, "player")

                                    IgnoreManager.clear(target.uuid)
                                    IgnoreManager.saveAsync(ctx.source.server, target.uuid)

                                    admin.sendMessage(
                                        MessageManager.get(admin, "ignoreClearedOther", "target" to target.name.string),
                                        false
                                    )
                                    target.sendMessage(
                                        MessageManager.get(target, "ignoreClearedNotified"),
                                        false
                                    )
                                    1
                                }
                        )
                )
        )
    }
}
