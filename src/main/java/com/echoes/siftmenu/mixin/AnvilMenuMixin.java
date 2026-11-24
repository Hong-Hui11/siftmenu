package com.echoes.siftmenu.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AnvilMenu.class})
public abstract class AnvilMenuMixin {
   @Inject(
      method = {"calculateIncreasedRepairCost"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void siftmenu$noPriorWorkPenalty(int var0, CallbackInfoReturnable<Integer> var1) {
      var1.setReturnValue(0);
   }
}
