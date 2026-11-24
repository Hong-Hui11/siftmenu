package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public abstract class SleepMixin {
   @Inject(
      method = {"startSleeping"},
      at = {@At("RETURN")}
   )
   private void siftmenu$onSleep(BlockPos var1, CallbackInfoReturnable<Boolean> var2) {
      try {
         if (!var2.getReturnValueZ()) {
            return;
         }

         if (((Object)this) instanceof ServerPlayer var3) {
            BountyService.onSleep(var3);
         }
      } catch (Exception var5) {
      }
   }
}
