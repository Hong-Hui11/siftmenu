package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FishingHook.class})
public abstract class FishingHookMixin {
   @Shadow
   private int nibble;

   @Inject(
      method = {"retrieve"},
      at = {@At("RETURN")}
   )
   private void siftmenu$onRetrieve(ItemStack var1, CallbackInfoReturnable<Integer> var2) {
      try {
         if (var2.getReturnValueI() != 1 || this.nibble <= 0) {
            return;
         }

         if ((((FishingHook)(Object)this)).getPlayerOwner() instanceof ServerPlayer var4) {
            BountyService.onFish(var4);
         }
      } catch (Exception var5) {
      }
   }
}
