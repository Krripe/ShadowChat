package org.krripe.shadowchat.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.krripe.shadowchat.config.ConfigManager;
import org.krripe.shadowchat.manager.TabListManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerTabNameMixin {

    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    private void shadowchat$customTabName(CallbackInfoReturnable<Text> cir) {
        if (!ConfigManager.INSTANCE.getChatConfig().getChatSystems().getTab()) {
            return;
        }
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        cir.setReturnValue(TabListManager.INSTANCE.entryText(self));
    }
}
