package net.alkalus.envirosound.mixin.client;

import net.alkalus.envirosound.EnviroSound;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

@Mixin(SoundManager.class)
@Environment(EnvType.CLIENT)
public class MixinSoundHelper {
    @Shadow private static SoundSystem soundSystem;
    
    @Inject(method = "method_2019()V", at = @At(value = "NEW", target = "paulscode.sound.SoundSystem", shift = At.Shift.AFTER))
    private void injectSetLibsAndCodecs(CallbackInfo ci) {
        EnviroSound.init();
    }

    @ModifyVariable(method = "method_2015(Ljava/lang/String;FFFFF)V", index = 5, at = @At(value = "INVOKE", target = "Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V", shift = At.Shift.BY, by = -5))
    private float modVolume1(float volume) {
        return volume * EnviroSound.globalVolumeMultiplier;
    }

    @ModifyVariable(method = "method_2009(Ljava/lang/String;FF)V", index = 2, at = @At(value = "INVOKE", target = "Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V", shift = At.Shift.BY, by = -5))
    private float modVolume2(float volume) {
        return volume * EnviroSound.globalVolumeMultiplier;
    }

    @Inject(method = "method_2015(Ljava/lang/String;FFFFF)V", at = @At(value = "INVOKE", target = "Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V", shift = At.Shift.AFTER))
    private void injectPlaySound(String s, float f, float f1, float f2, float f3, float f4, CallbackInfo ci) {
		EnviroSound.setLastSoundName(s);
    }

    @Inject(method = "method_2009(Ljava/lang/String;FF)V", at = @At(value = "INVOKE", target = "Lpaulscode/sound/SoundSystem;setVolume(Ljava/lang/String;F)V", shift = At.Shift.AFTER))
    private void injectPlaySound(String string, float f, float f1, CallbackInfo ci) {
        EnviroSound.setLastSoundName(string);
    }
}
