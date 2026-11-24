package com.echoes.siftmenu.mixin.client;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ItemModelResolver.class})
public abstract class ItemModelResolverMixin {
   @Redirect(
      method = {"updateForLiving"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getId()I"
      )
   )
   private int siftmenu$safeEntityId(LivingEntity var1) {
      try {
         return var1.getId();
      } catch (IllegalStateException var3) {
         return 0;
      }
   }
}
