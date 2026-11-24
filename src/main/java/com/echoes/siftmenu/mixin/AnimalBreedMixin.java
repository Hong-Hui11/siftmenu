package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Animal.class})
public abstract class AnimalBreedMixin {
   @Inject(
      method = {"finalizeSpawnChildFromBreeding"},
      at = {@At("HEAD")}
   )
   private void siftmenu$onBreed(ServerLevel var1, Animal var2, AgeableMob var3, CallbackInfo var4) {
      try {
         ServerPlayer var5 = (((Animal)(Object)this)).getLoveCause();
         if (var5 == null && var2 != null) {
            var5 = var2.getLoveCause();
         }

         if (var5 != null) {
            BountyService.onBreed(var5);
         }
      } catch (Exception var6) {
      }
   }
}
