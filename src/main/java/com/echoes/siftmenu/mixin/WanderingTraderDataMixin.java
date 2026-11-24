package com.echoes.siftmenu.mixin;

import net.minecraft.world.level.saveddata.WanderingTraderData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({WanderingTraderData.class})
public abstract class WanderingTraderDataMixin {
   private static final int SPAWN_DELAY_DIVISOR = 3;

   @ModifyVariable(
      method = {"<init>(II)V"},
      at = @At("HEAD"),
      argsOnly = true,
      index = 1
   )
   private static int siftmenu$shortenLoadedDelay(int var0) {
      return siftmenu$shorten(var0);
   }

   @ModifyVariable(
      method = {"setSpawnDelay(I)V"},
      at = @At("HEAD"),
      argsOnly = true,
      index = 1
   )
   private int siftmenu$shortenSpawnDelay(int var1) {
      return siftmenu$shorten(var1);
   }

   private static int siftmenu$shorten(int var0) {
      return var0 == 24000 ? 8000 : var0;
   }
}
