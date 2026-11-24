package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class ItemPickupMixin {
   @Inject(
      method = {"take"},
      at = {@At("HEAD")}
   )
   private void siftmenu$onTake(Entity var1, int var2, CallbackInfo var3) {
      try {
         if (!(var1 instanceof ItemEntity) || var2 <= 0) {
            return;
         }

         if (((Object)this) instanceof ServerPlayer var4) {
            BountyService.onPickup(var4, var2);
         }
      } catch (Exception var6) {
      }
   }
}
