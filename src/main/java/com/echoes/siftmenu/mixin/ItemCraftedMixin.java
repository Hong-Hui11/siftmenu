package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Item.class})
public abstract class ItemCraftedMixin {
   @Inject(
      method = {"onCraftedBy"},
      at = {@At("HEAD")}
   )
   private void siftmenu$onCraftedBy(ItemStack var1, Player var2, CallbackInfo var3) {
      try {
         if (!(var2 instanceof ServerPlayer var4)) {
            return;
         }

         if (var1 == null || var1.isEmpty()) {
            return;
         }

         if (var2.containerMenu instanceof AbstractCraftingMenu) {
            BountyService.onCraft(var4, BuiltInRegistries.ITEM.getKey(var1.getItem()).toString(), Math.max(1, var1.getCount()));
         } else if (var2.containerMenu instanceof MerchantMenu) {
            BountyService.onTrade(var4);
         }
      } catch (Exception var5) {
      }
   }
}
