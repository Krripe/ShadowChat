package org.krripe.shadowchat.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.krripe.shadowchat.config.ConfigManager;
import org.krripe.shadowchat.manager.AdvancementFormatManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Shadow
    @Final
    private List<ServerPlayerEntity> players;

    @Inject(
            method = "broadcast(Lnet/minecraft/text/Text;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void shadowchat$interceptBroadcast(
            Text message,
            boolean overlay,
            CallbackInfo ci
    ) {
        if (!(message.getContent() instanceof TranslatableTextContent translatable)) {
            return;
        }

        String key = translatable.getKey();
        if (key == null) {
            return;
        }

        if (
                key.equals("multiplayer.player.joined")
                        || key.equals("multiplayer.player.left")
        ) {
            var cfg = ConfigManager.INSTANCE.getChatConfig();
            var joinLeave = cfg.getJoinLeaveMessages();

            boolean isJoin = key.equals("multiplayer.player.joined");

            String customMessage = isJoin
                    ? cfg.getJoinMessage()
                    : cfg.getLeaveMessage();

            boolean shouldCancelVanilla =
                    !joinLeave.getEnabled()
                            || !customMessage.isBlank()
                            || !joinLeave.getUseVanillaIfBlank();

            if (shouldCancelVanilla) {
                ci.cancel();
            }

            return;
        }

        if (!key.startsWith("chat.type.advancement.")) {
            return;
        }

        ServerPlayerEntity owner = null;

        Object[] args = translatable.getArgs();
        Text who = null;

        if (args != null && args.length > 0 && args[0] instanceof Text text) {
            who = text;
        }

        if (who != null) {
            for (ServerPlayerEntity player : this.players) {
                if (
                        who.equals(player.getDisplayName())
                                || who.equals(player.getName())
                ) {
                    owner = player;
                    break;
                }
            }
        }

        if (owner == null && who != null) {
            owner = shadowchat$findBestByLongestContains(who.getString());
        }

        if (owner == null) {
            owner = shadowchat$findBestByLongestContains(message.getString());
        }

        if (owner == null) {
            return;
        }

        Text formatted =
                AdvancementFormatManager.INSTANCE.formatFromVanilla(owner, message);

        if (formatted.equals(message)) {
            return;
        }

        for (ServerPlayerEntity viewer : this.players) {
            viewer.sendMessage(formatted, overlay);
        }

        ci.cancel();
    }

    @Unique
    private ServerPlayerEntity shadowchat$findBestByLongestContains(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        ServerPlayerEntity best = null;
        int bestLength = -1;

        for (ServerPlayerEntity player : this.players) {
            String rawName = player.getGameProfile().getName();

            if (rawName == null || rawName.isEmpty()) {
                continue;
            }

            if (text.contains(rawName) && rawName.length() > bestLength) {
                best = player;
                bestLength = rawName.length();
            }
        }

        return best;
    }
}