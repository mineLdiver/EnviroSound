package net.alkalus.envirosound.mixin.client;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public class MixinLevel {

    @Redirect(
            method = "method_191(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/entity/Entity;field_1631:F",
                    opcode = Opcodes.GETFIELD
            )
    )
    private float getHeight(Entity entity) {
        return 0;
    }
}
