package org.krripe.shadowchat.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.krripe.shadowchat.api.ShadowChat;
import org.krripe.shadowchat.config.ConfigManager;
import org.krripe.shadowchat.manager.ChatFormatManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Redirect(
            method = "onDeath",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"
            )
    )
    private void shadowchat$handleDeathMessage(PlayerManager playerManager, Text original, boolean overlay) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        var cfg = ConfigManager.INSTANCE.getChatConfig();
        var deathCfg = cfg.getDeathMessage();

        if (!deathCfg.getEnabled()) {
            return;
        }

        if (cfg.getBaseDeath().isBlank() && deathCfg.getUseVanillaIfBlank()) {
            playerManager.broadcast(original, overlay);
            return;
        }

        if (cfg.getBaseDeath().isBlank()) {
            return;
        }

        Text formatted = ChatFormatManager.INSTANCE.getDeathMessage(player, original);

        try {
            ShadowChat.INSTANCE.get().fireDeath(player, formatted);
        } catch (Throwable ignored) {}

        playerManager.broadcast(formatted, overlay);
    }
}