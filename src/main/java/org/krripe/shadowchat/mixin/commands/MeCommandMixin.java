package org.krripe.shadowchat.mixin.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.command.MeCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.krripe.shadowchat.manager.CommandFormatManager;
import org.krripe.shadowchat.manager.IgnoreManager;
import org.krripe.shadowchat.manager.LogManager;
import org.krripe.shadowchat.manager.SpamManager;
import org.krripe.shadowchat.utils.PermissionsSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MeCommand.class)
public class MeCommandMixin {

    @Inject(method = "method_43645", at = @At("HEAD"), cancellable = true)
    private static void shadowchat$customMeFormat(CommandContext<ServerCommandSource> context, SignedMessage message, CallbackInfo ci) {

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity sender = source.getPlayer();

        if (sender != null && SpamManager.INSTANCE.shouldBlock(sender)) {
            SpamManager.INSTANCE.warn(sender);
            ci.cancel();
            return;
        }

        String content = message.getContent().getString();
        var formatted = CommandFormatManager.INSTANCE.formatMe(source, content);

        PermissionsSystem perms = new PermissionsSystem();
        boolean bypassIgnore = sender != null && perms.hasPermission(sender.getCommandSource(), "shadowchat.ignore.bypass");

        for (ServerPlayerEntity viewer : source.getServer().getPlayerManager().getPlayerList()) {
            if (sender != null && !bypassIgnore && IgnoreManager.INSTANCE.isIgnoring(viewer.getUuid(), sender.getUuid())) continue;
            viewer.sendMessage(formatted, false);
        }

        if (source.getEntity() instanceof ServerPlayerEntity player) {
            LogManager.INSTANCE.logChat(player, content);
        }

        ci.cancel();
    }
}