package net.alkalus.envirosound.mixin.client;

import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Vec3d.class)
public interface Vec3fAccessor {

    @Accessor("cache")
    static List<Vec3d> getField_1588() {
        throw new AssertionError("Mixin!");
    }
}
