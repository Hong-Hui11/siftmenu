package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemStack.class})
public abstract class ItemStackConsumeMixin {
   @Inject(
      method = {"finishUsingItem"},
      at = {@At("HEAD")}
   )
   private void siftmenu$onFinishUsing(Level var1, LivingEntity var2, CallbackInfoReturnable<ItemStack> var3) {
      try {
         if (!(var2 instanceof ServerPlayer var4)) {
            return;
         }

         if (!(((ItemStack)(Object)this)).has(DataComponents.CONSUMABLE)) {
            return;
         }

         BountyService.onConsume(var4);
      } catch (Exception var5) {
      }
   }
}
