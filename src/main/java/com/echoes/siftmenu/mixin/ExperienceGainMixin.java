package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Player.class})
public abstract class ExperienceGainMixin {
   @Inject(
      method = {"giveExperiencePoints"},
      at = {@At("HEAD")}
   )
   private void siftmenu$onExperience(int var1, CallbackInfo var2) {
      try {
         if (var1 <= 0) {
            return;
         }

         if (((Object)this) instanceof ServerPlayer var3) {
            BountyService.onXpGain(var3, var1);
         }
      } catch (Exception var5) {
      }
   }
}
