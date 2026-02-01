package org.krripe.shadowchat.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import org.krripe.shadowchat.config.ConfigManager
import org.krripe.shadowchat.config.MessageManager
import org.krripe.shadowchat.manager.*
import org.krripe.shadowchat.utils.PermissionsSystem

object ReplyCommand {

    private val perms = PermissionsSystem()

    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            fun registerAlias(name: String) {
                dispatcher.register(
                    CommandManager.literal(name)
                        .requires { src -> perms.hasPermission(src, "shadowchat.reply") }
                        .then(
                            CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes { ctx ->
                                    val src = ctx.source
                                    val sender = src.player

                                    if (sender == null) {
                                        src.sendFeedback({ MessageManager.get(src, "onlyPlayers") }, false)
                                        return@executes 0
                                    }

                                    if (ConfigManager.chatConfig.chatSystems.spam && SpamManager.shouldBlock(sender)) {
                                        SpamManager.warn(sender)
                                        return@executes 0
                                    }

                                    val msg = StringArgumentType.getString(ctx, "message")
                                    val lastUuid = ReplyManager.getLastUuid(sender)

                                    if (lastUuid == null) {
                                        sender.sendMessage(MessageManager.get(sender, "replyNoOne"), false)
                                        return@executes 0
                                    }

                                    val target = src.server.playerManager.getPlayer(lastUuid)
                                    if (target == null) {
                                        sender.sendMessage(MessageManager.get(sender, "replyTargetOffline"), false)
                                        ReplyManager.clearPlayer(sender.uuid)
                                        return@executes 0
                                    }

                                    sendPrivate(src, sender, target, msg)
                                    Command.SINGLE_SUCCESS
                                }
                        )
                )
            }

            registerAlias("r")
            registerAlias("reply")
            registerAlias("replay")
        }
    }

    private fun sendPrivate(source: ServerCommandSource, sender: ServerPlayerEntity, target: ServerPlayerEntity, content: String) {
        val bypassIgnore = perms.hasPermission(sender.commandSource, "shadowchat.ignore.bypass")
        if (!bypassIgnore) {
            if (IgnoreManager.isIgnoring(target.uuid, sender.uuid)) {
                sender.sendMessage(
                    MessageManager.get(sender, "ignoreMsgBlockedByTarget", "target" to target.name.string),
                    false
                )
                return
            }
            if (IgnoreManager.isIgnoring(sender.uuid, target.uuid)) {
                sender.sendMessage(
                    MessageManager.get(sender, "ignoreMsgYouIgnoring", "target" to target.name.string),
                    false
                )
                return
            }
        }

        val senderNick = NicknameManager.getDisplayName(sender)
        val targetNick = NicknameManager.getDisplayName(target)

        target.sendMessage(CommandFormatManager.formatMsgReceived(source, senderNick, content), false)
        source.sendFeedback({ CommandFormatManager.formatMsgSent(source, targetNick, content) }, false)

        LogManager.logPrivate(senderNick, targetNick, content)
        ReplyManager.setLast(sender, target)
        ReplyManager.setLast(target, sender)

        if (SpyManager.canBeSpied(sender)) {
            val spyMsg = CommandFormatManager.formatSpy(source, senderNick, target.name.string, content)
            for (online in sender.server.playerManager.playerList) {
                if (!SpyManager.isSpying(online)) continue
                if (online === sender || online === target) continue
                online.sendMessage(spyMsg, false)
            }
        }
    }
}