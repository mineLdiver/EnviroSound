package net.alkalus.envirosound.mixin.client;

import net.alkalus.envirosound.EnviroSound;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import paulscode.sound.FilenameURL;
import paulscode.sound.ICodec;
import paulscode.sound.SoundBuffer;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

@Mixin(LibraryLWJGLOpenAL.class)
@Environment(EnvType.CLIENT)
public class MixinLibraryLWJGLOpenAL {
    @Inject(method = "loadSound(Lpaulscode/sound/FilenameURL;)Z", at = @At(value = "INVOKE", target = "Lpaulscode/sound/ICodec;cleanup()V", shift = At.Shift.AFTER), remap = false, locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectLoadSound(FilenameURL filenameURL, CallbackInfoReturnable cir, ICodec codec, SoundBuffer buffer) {
        buffer = EnviroSound.onLoadSound(buffer, filenameURL.getFilename());
    }

    @Redirect(method = "init()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/AL;create()V"), remap = false)
    private void stopCreate() { }
    
    /*@Inject(method = "loadSound(Lpaulscode/sound/SoundBuffer;Ljava/lang/String;)Z", at = @At(value = "INVOKE", target = "Lpaulscode/sound/AudioFormat;getChannels()V", by = -12), remap = false)
    private void injectLoadSound2(SoundBuffer buffer, String s, CallbackInfoReturnable cir) {
        System.out.println("loadSound(SoundBuffer, String) from paulscode");
        //ALOAD_0
        //ALOAD_1
        //INVOKESTATIC net/alkalus/envirosound/EnviroSound onLoadSound (Lpaulscode/sound/SoundBuffer;Ljava/lang/String;)Lpaulscode/sound/SoundBuffer;
		//ASTORE 0
    }*/
}
