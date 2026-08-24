package org.krripe.shadowchat.mixin;

import com.mojang.brigadier.ParseResults;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.krripe.shadowchat.manager.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class CommandManagerMixin {

    @Inject(
            method = "execute(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V",
            at = @At("HEAD")
    )
    private void shadowchat$logCommand(
            ParseResults<ServerCommandSource> parseResults,
            String command,
            CallbackInfo ci
    ) {
        ServerCommandSource source = parseResults.getContext().getSource();
        LogManager.INSTANCE.logCommand(source, command);
    }
}