package org.krripe.shadowchat.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.krripe.shadowchat.api.ShadowChat;
import org.krripe.shadowchat.manager.ChatFormatManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @ModifyArg(
            method = "onDeath",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V"
            ),
            index = 0
    )
    private Text shadowchat$replaceDeathMessage(Text original) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        Text formatted = ChatFormatManager.INSTANCE.getDeathMessage(player, original);

        try {
            ShadowChat.INSTANCE.get().fireDeath(player, formatted);
        } catch (Throwable ignored) {}

        return formatted;
    }
}
