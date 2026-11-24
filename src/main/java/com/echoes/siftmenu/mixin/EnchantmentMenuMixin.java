package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EnchantmentMenu.class})
public abstract class EnchantmentMenuMixin {
   @Inject(
      method = {"clickMenuButton"},
      at = {@At("RETURN")}
   )
   private void siftmenu$onEnchant(Player var1, int var2, CallbackInfoReturnable<Boolean> var3) {
      try {
         if (var3.getReturnValueZ() && var1 instanceof ServerPlayer var4) {
            BountyService.onEnchant(var4);
         }
      } catch (Exception var5) {
      }
   }
}
