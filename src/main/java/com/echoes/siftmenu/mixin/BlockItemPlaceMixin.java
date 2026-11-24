package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockItem.class})
public abstract class BlockItemPlaceMixin {
   @Inject(
      method = {"placeBlock"},
      at = {@At("RETURN")}
   )
   private void siftmenu$onBlockPlaced(BlockPlaceContext var1, BlockState var2, CallbackInfoReturnable<Boolean> var3) {
      try {
         if (!var3.getReturnValueZ()) {
            return;
         }

         if (!(var1.getPlayer() instanceof ServerPlayer var4)) {
            return;
         }

         String var9 = BuiltInRegistries.BLOCK.getKey(var2.getBlock()).toString();
         BountyService.onBlockPlace(var4, var9);
         Item var6 = var2.getBlock().asItem();
         if (var6 != Items.AIR) {
            String var7 = BuiltInRegistries.ITEM.getKey(var6).toString();
            if (!var7.equals(var9)) {
               BountyService.onBlockPlace(var4, var7);
            }
         }
      } catch (Exception var8) {
      }
   }
}
