package org.krripe.shadowchat.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"), cancellable = true)
    private void shadowchat$cancelVanillaJoinLeave(Text message, boolean overlay, CallbackInfo ci) {
        if (message.getContent() instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey();
            if (key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.left")) {
                ci.cancel();
            }
        }
    }
}
