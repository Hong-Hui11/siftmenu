package com.echoes.siftmenu.mixin;

import com.echoes.siftmenu.BountyService;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractFurnaceBlockEntity.class})
public abstract class FurnaceSmeltProgressMixin {
   @Unique
   private static final Map<AbstractFurnaceBlockEntity, Integer> SIFTMENU$LAST_OUTPUT = new WeakHashMap<>();
   @Unique
   private static final Map<AbstractFurnaceBlockEntity, UUID> SIFTMENU$OWNER = new WeakHashMap<>();

   @Inject(
      method = {"serverTick"},
      at = {@At("TAIL")}
   )
   private static void siftmenu$countProduced(ServerLevel var0, BlockPos var1, BlockState var2, AbstractFurnaceBlockEntity var3, CallbackInfo var4) {
      try {
         ServerPlayer var5 = siftmenu$viewer(var0, var3);
         if (var5 != null) {
            SIFTMENU$OWNER.put(var3, var5.getUUID());
         }

         ItemStack var7 = var3.getItem(2);
         int var8 = var7.isEmpty() ? 0 : var7.getCount();
         Integer var9 = SIFTMENU$LAST_OUTPUT.put(var3, var8);
         if (var9 == null || var8 <= var9) {
            return;
         }

         UUID var10 = SIFTMENU$OWNER.get(var3);
         if (var10 == null || var0.getServer() == null) {
            return;
         }

         ServerPlayer var11 = var0.getServer().getPlayerList().getPlayer(var10);
         if (var11 == null) {
            return;
         }

         String var12 = BuiltInRegistries.ITEM.getKey(var7.getItem()).toString();
         int var13 = Math.min(var8 - var9, 64);

         for (int var14 = 0; var14 < var13; var14++) {
            BountyService.onSmelt(var11, var12);
         }
      } catch (Exception var15) {
      }
   }

   @Unique
   private static ServerPlayer siftmenu$viewer(ServerLevel var0, AbstractFurnaceBlockEntity var1) {
      for (Player var3 : var0.players()) {
         if (var3 instanceof ServerPlayer var4 && var4.containerMenu instanceof AbstractFurnaceMenu var5 && var5.getSlot(2).container == var1) {
            return var4;
         }
      }

      return null;
   }
}
