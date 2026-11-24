package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import net.minecraft.advancements.triggers.TameAnimalTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TameAnimalTrigger.class})
public abstract class TameAnimalTriggerMixin {
   @Inject(
      method = {"trigger"},
      at = {@At("HEAD")}
   )
   private void siftmenu$onTame(ServerPlayer var1, Animal var2, CallbackInfo var3) {
      try {
         if (var1 != null) {
            BountyService.onTame(var1);
         }
      } catch (Exception var5) {
      }
   }
}
