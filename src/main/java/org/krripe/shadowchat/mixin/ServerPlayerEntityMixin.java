package org.krripe.shadowchat.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.krripe.shadowchat.manager.ChatFormatManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void shadowchat$customDeathMessage(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Text vanillaMessage = player.getDamageTracker().getDeathMessage();
        Text formatted = ChatFormatManager.INSTANCE.getDeathMessage(player, vanillaMessage);

        MinecraftServer server = player.getServer();
        if (server != null) {
            server.getPlayerManager().broadcast(formatted, false);
        }

        ci.cancel();
    }
}
