package com.guichaguri.pvptime.fabric.mixin;

import com.guichaguri.pvptime.fabric.CommandPerformCallback;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Inject(at = @At(value = "TAIL"), method = "performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V", cancellable = false)
    private void pvptime_onCommand(ParseResults<CommandSourceStack> command, String commandString, CallbackInfo info) {
        CommandPerformCallback.EVENT.invoker().onCommand();
    }

}
