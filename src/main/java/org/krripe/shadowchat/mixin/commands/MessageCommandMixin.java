package org.krripe.shadowchat.mixin.commands;

import kotlin.Pair;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.command.MessageCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.krripe.shadowchat.config.MessageManager;
import org.krripe.shadowchat.manager.*;
import org.krripe.shadowchat.utils.PermissionsSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
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

        String content = message.getContent().getString();
        String senderNick = sender != null ? NicknameManager.INSTANCE.getDisplayName(sender) : source.getName();

        PermissionsSystem perms = new PermissionsSystem();
        boolean bypassIgnore = sender != null && perms.hasPermission(sender.getCommandSource(), "shadowchat.ignore.bypass");

        List<ServerPlayerEntity> delivered = new ArrayList<>();

        for (ServerPlayerEntity target : targets) {

            if (sender != null && !bypassIgnore) {
                if (IgnoreManager.INSTANCE.isIgnoring(target.getUuid(), sender.getUuid())) {
                    sender.sendMessage(
                            MessageManager.INSTANCE.get(sender, "ignoreMsgBlockedByTarget",
                                    new Pair<>("target", target.getName().getString())),
                            false
                    );
                    continue;
                }
                if (IgnoreManager.INSTANCE.isIgnoring(sender.getUuid(), target.getUuid())) {
                    sender.sendMessage(
                            MessageManager.INSTANCE.get(sender, "ignoreMsgYouIgnoring",
                                    new Pair<>("target", target.getName().getString())),
                            false
                    );
                    continue;
                }
            }

            String targetNick = NicknameManager.INSTANCE.getDisplayName(target);

            target.sendMessage(
                    CommandFormatManager.INSTANCE.formatMsgReceived(source, senderNick, content),
                    false
            );

            LogManager.INSTANCE.logPrivate(senderNick, targetNick, content);

            source.sendFeedback(
                    () -> CommandFormatManager.INSTANCE.formatMsgSent(source, targetNick, content),
                    false
            );

            if (sender != null) {
                ReplyManager.INSTANCE.setLast(sender, target);
                ReplyManager.INSTANCE.setLast(target, sender);
            }

            delivered.add(target);
        }

        if (sender != null && !delivered.isEmpty() && SpyManager.INSTANCE.canBeSpied(sender)) {
            String targetList = delivered.stream()
                    .map(ServerPlayerEntity::getName)
                    .map(Text::getString)
                    .collect(Collectors.joining(", "));

            Text spyMsg = CommandFormatManager.INSTANCE.formatSpy(source, senderNick, targetList, content);

            for (ServerPlayerEntity online : Objects.requireNonNull(sender.getServer()).getPlayerManager().getPlayerList()) {
                if (!SpyManager.INSTANCE.isSpying(online)) continue;
                if (online == sender || delivered.contains(online)) continue;
                online.sendMessage(spyMsg, false);
            }
        }

        ci.cancel();
    }
}