package net.alkalus.envirosound.mixin.client;

import net.alkalus.envirosound.EnviroSound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import paulscode.sound.Source;
import paulscode.sound.SoundBuffer;
import paulscode.sound.Channel;
import paulscode.sound.Vector3D;
import paulscode.sound.libraries.SourceLWJGLOpenAL;
import paulscode.sound.libraries.ChannelLWJGLOpenAL;

@Mixin(SourceLWJGLOpenAL.class)
@Environment(EnvType.CLIENT)
public class MixinSourceLWJGLOpenAL extends Source {
    @Shadow(remap = false) private ChannelLWJGLOpenAL channelOpenAL;
    
    public MixinSourceLWJGLOpenAL(Source s1, SoundBuffer s2) {
        super(s1, s2);
    }
    
    @Inject(method = "play(Lpaulscode/sound/Channel;)V", at = @At(value = "INVOKE", target = "Lpaulscode/sound/Channel;play()V"), remap = false)
    private void injectPlay(Channel channel, CallbackInfo ci) {
        System.out.println("play() from paulscode");
        System.out.println(channelOpenAL.ALSource.get(0));
        EnviroSound.onPlaySound(position.x, position.y, position.z, channelOpenAL.ALSource.get(0));
    }
}
