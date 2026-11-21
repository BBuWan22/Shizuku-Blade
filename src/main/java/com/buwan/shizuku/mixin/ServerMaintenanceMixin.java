package com.buwan.shizuku.mixin;

import com.buwan.shizuku.sa.Ymsa;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class ServerMaintenanceMixin {
    @Inject(method = "tickServer", at = @At("RETURN"))
    private void ymsa$serverTickEnd(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        Ymsa.handleServerTickEnd((MinecraftServer)(Object)this);
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void ymsa$serverStop(CallbackInfo ci) {
        Ymsa.clearAllSAState();
    }
}
