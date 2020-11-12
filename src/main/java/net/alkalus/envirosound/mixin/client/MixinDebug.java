package net.alkalus.envirosound.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import paulscode.sound.SoundSystemLogger;

@Mixin(SoundSystemLogger.class)
@Environment(EnvType.CLIENT)
public class MixinDebug {
    @Inject(method = "errorMessage(Ljava/lang/String;Ljava/lang/String;I)V", at = @At(value = "RETURN"), remap = false)
    private void injectErrorMessage(String classname, String error, int indent, CallbackInfo ci) {
        try {
            throw new Exception("Stack Trace");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
