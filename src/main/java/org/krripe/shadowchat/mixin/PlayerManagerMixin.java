package org.krripe.shadowchat.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
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

    @Shadow @Final private List<ServerPlayerEntity> players;

    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void shadowchat$interceptBroadcast(Text message, boolean overlay, CallbackInfo ci) {

        if (!(message.getContent() instanceof TranslatableTextContent translatable)) return;

        String key = translatable.getKey();
        if (key == null) return;

        if (key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.left")) {
            ci.cancel();
            return;
        }

        if (!key.startsWith("chat.type.advancement.")) return;

        ServerPlayerEntity owner = null;

        Object[] args = translatable.getArgs();
        Text who = null;
        if (args != null && args.length > 0 && args[0] instanceof Text t) {
            who = t;
        }

        if (who != null) {
            for (ServerPlayerEntity p : this.players) {
                if (who.equals(p.getDisplayName()) || who.equals(p.getName())) {
                    owner = p;
                    break;
                }
            }
        }

        if (owner == null && who != null) {
            String displayArg = who.getString();
            owner = findBestByLongestContains(displayArg);
        }

        if (owner == null) {
            String full = message.getString();
            owner = findBestByLongestContains(full);
        }

        if (owner == null) return;

        Text formatted = AdvancementFormatManager.INSTANCE.formatFromVanilla(owner, message);

        if (formatted.equals(message)) return;

        for (ServerPlayerEntity viewer : this.players) {
            viewer.sendMessage(formatted, overlay);
        }

        ci.cancel();
    }

    @Unique
    private ServerPlayerEntity findBestByLongestContains(String text) {
        if (text == null || text.isEmpty()) return null;

        ServerPlayerEntity best = null;
        int bestLen = -1;

        for (ServerPlayerEntity p : this.players) {
            String raw = p.getGameProfile().getName();
            if (raw == null || raw.isEmpty()) continue;

            if (text.contains(raw) && raw.length() > bestLen) {
                best = p;
                bestLen = raw.length();
            }
        }

        return best;
    }
}
