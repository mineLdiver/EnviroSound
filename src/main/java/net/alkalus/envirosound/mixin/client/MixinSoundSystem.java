package net.alkalus.envirosound.mixin.client;

import net.alkalus.envirosound.EnviroSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import paulscode.sound.SoundSystem;

@Mixin(SoundSystem.class)
public class MixinSoundSystem {

    @ModifyVariable(method = "newSource(ZLjava/lang/String;Ljava/net/URL;Ljava/lang/String;ZFFFIF)V", index = 9, at = @At("HEAD"), remap = false)
    private int modAttenuation(int attenuation) {
        return EnviroSound.attenuationModel;
    }

    @ModifyVariable(method = "newSource(ZLjava/lang/String;Ljava/net/URL;Ljava/lang/String;ZFFFIF)V", index = 10, at = @At("HEAD"), remap = false)
    private float modRolloffFactor(float rolloffFactor) {
        return EnviroSound.globalRolloffFactor;
    }
}
