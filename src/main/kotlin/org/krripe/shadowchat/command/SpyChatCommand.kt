package org.krripe.shadowchat.command

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.*
import net.minecraft.text.Text
import org.krripe.shadowchat.manager.SpyManager
import org.krripe.shadowchat.utils.PermissionsSystem

object SpyChatCommand {
    private val perms = PermissionsSystem()

    fun register() {
        CommandRegistrationCallback.EVENT.register { d, _, _ ->
            d.register(
                literal("spychat")
                    .requires { perms.hasPermission(it, "shadowchat.spy") }
                    .then(argument("state", StringArgumentType.word())
                        .suggests { _, b -> b.suggest("on").suggest("off").buildFuture() }
                        .executes { ctx ->
                            val p = ctx.source.playerOrThrow
                            val wantOn = StringArgumentType.getString(ctx, "state").equals("on", true)
                            val nowOn = SpyManager.isSpying(p)
                            if (wantOn != nowOn) SpyManager.toggle(p)
                            ctx.source.sendFeedback(
                                { Text.literal("§eSpyChat ${if (wantOn) "enabled" else "disabled"}") },
                                false
                            )
                            1
                        })
                    .executes { ctx ->
                        val p = ctx.source.playerOrThrow
                        val enabled = SpyManager.toggle(p)
                        ctx.source.sendFeedback(
                            { Text.literal("§eSpyChat ${if (enabled) "enabled" else "disabled"}") },
                            false
                        )
                        1
                    }
            )
        }
    }
}