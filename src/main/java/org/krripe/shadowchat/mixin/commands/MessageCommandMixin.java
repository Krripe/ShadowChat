package org.krripe.shadowchat.mixin.commands;

import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.command.MessageCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.krripe.shadowchat.manager.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@Mixin(MessageCommand.class)
public class MessageCommandMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void shadowchat$customMsg(ServerCommandSource source, Collection<ServerPlayerEntity> targets, SignedMessage message, CallbackInfo ci) {

        ServerPlayerEntity sender = source.getPlayer();
        if (sender != null && SpamManager.INSTANCE.shouldBlock(sender)) {
            SpamManager.INSTANCE.warn(sender);
            ci.cancel();
            return;
        }

        String senderNick = NicknameManager.INSTANCE.getDisplayName(source.getPlayer());
        String content = message.getContent().getString();

        for (ServerPlayerEntity target : targets) {
            String targetNick = NicknameManager.INSTANCE.getDisplayName(target);
            target.sendMessage(
                    CommandFormatManager.INSTANCE.formatMsgReceived(source, senderNick, content)
            );

            LogManager.INSTANCE.logPrivate(senderNick, targetNick, content);

            source.sendFeedback(
                    () -> CommandFormatManager.INSTANCE.formatMsgSent(source, targetNick, content),
                    false
            );
        }

        if (SpyManager.INSTANCE.canBeSpied(sender)) {
            String targetList = targets.stream()
                    .map(ServerPlayerEntity::getName)
                    .map(Text::getString)
                    .collect(Collectors.joining(", "));

            Text spyMsg = CommandFormatManager.INSTANCE
                    .formatSpy(source, senderNick, targetList, content);

            assert sender != null;
            for (ServerPlayerEntity online : Objects.requireNonNull(sender.getServer()).getPlayerManager().getPlayerList()) {
                if (!SpyManager.INSTANCE.isSpying(online)) continue;
                if (online == sender || targets.contains(online)) continue;
                online.sendMessage(spyMsg, false);
            }
        }

        ci.cancel();
    }
}
