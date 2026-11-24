package com.echoes.siftmenu.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
   targets = {"immersive_aircraft.entity.VehicleEntity"},
   remap = false
)
public class VehicleInterpolationMixin {
   protected InterpolationHandler createInterpolationHandler() {
      return LinearInterpolationHandler.create(((Entity)(Object)this), 3);
   }
}
