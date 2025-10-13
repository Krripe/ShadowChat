package org.krripe.shadowchat.mixin.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.command.MeCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.krripe.shadowchat.manager.CommandFormatManager;
import org.krripe.shadowchat.manager.SpamManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.krripe.shadowchat.manager.LogManager;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(MeCommand.class)
public class MeCommandMixin {
    @Inject(method = "method_43645", at = @At("HEAD"), cancellable = true)
    private static void shadowchat$customMeFormat(CommandContext<ServerCommandSource> context, SignedMessage message, CallbackInfo ci) {
        ServerPlayerEntity sender = context.getSource().getPlayer();
        if (sender != null && SpamManager.INSTANCE.shouldBlock(sender)) {
            SpamManager.INSTANCE.warn(sender);
            ci.cancel();
            return;
        }

        String content = message.getContent().getString();
        var formatted = CommandFormatManager.INSTANCE.formatMe(context.getSource(), content);
        context.getSource().getServer().getPlayerManager().broadcast(formatted, false);
        if (context.getSource().getEntity() instanceof ServerPlayerEntity player) {
            LogManager.INSTANCE.logChat(player, content);
        }
        ci.cancel();
    }
}