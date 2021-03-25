package net.alkalus.envirosound.mixin.client;

import net.minecraft.entity.EntityBase;
import net.minecraft.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public class MixinLevel {

    @Redirect(method = "playSound(Lnet/minecraft/entity/EntityBase;Ljava/lang/String;FF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityBase;standingEyeHeight:F", opcode = Opcodes.GETFIELD))
    private float getHeight(EntityBase entity) {
        return 0;
    }
}
