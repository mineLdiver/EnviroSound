package net.alkalus.envirosound.mixin.client;

import net.alkalus.envirosound.EnviroSound;

import net.minecraft.client.sound.SoundHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import paulscode.sound.SoundSystem;

@Mixin(SoundHelper.class)
@Environment(EnvType.CLIENT)
public class MixinSoundHelper {
    @Shadow private static SoundSystem soundSystem;
    
    @Inject(method = "setLibsAndCodecs()V", at = @At(value = "NEW", target = "paulscode.sound.SoundSystem", shift = At.Shift.AFTER))
    private void injectSetLibsAndCodecs(CallbackInfo ci) {
        System.out.println("SOUND SYSTEM STARTING UP!!!");
        EnviroSound.init();
    }
    
    @Inject(method = "playSound(Ljava/lang/String;FFFFF)V", at = @At(value = "INVOKE", target = "Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V", shift = At.Shift.AFTER))
    private void injectPlaySound(String s, float f, float f1, float f2, float f3, float f4, CallbackInfo ci) {
        System.out.println("playSound()");
        f3 *= EnviroSound.globalVolumeMultiplier;
		EnviroSound.setLastSoundName(s);
    }
}
