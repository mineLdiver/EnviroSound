package net.alkalus.envirosound.mixin.client;

import net.minecraft.util.maths.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.*;

@Mixin(Vec3f.class)
public interface Vec3fAccessor {

    @Accessor
    static List<Vec3f> getField_1588() {
        throw new AssertionError("Mixin!");
    }
}
