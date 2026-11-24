package com.echoes.siftmenu;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.AnvilBlock;

public final class AnvilUnseal {
   private static final String TAG = "the_sift:charoite_used";

   private AnvilUnseal() {
   }

   public static void register() {
      UseBlockCallback.EVENT.register((UseBlockCallback)(var0, var1, var2, var3) -> {
         if (!var1.isClientSide() && var0 instanceof ServerPlayer var4) {
            if (!(var1.getBlockState(var3.getBlockPos()).getBlock() instanceof AnvilBlock)) {
               return InteractionResult.PASS;
            } else {
               if (unsealInventory(var4)) {
                  var4.sendSystemMessage(Component.literal("已解除紫龙晶附魔封印，可以正常使用附魔书").withStyle(ChatFormatting.GREEN), true);
               }

               return InteractionResult.PASS;
            }
         } else {
            return InteractionResult.PASS;
         }
      });
   }

   private static boolean unsealInventory(ServerPlayer var0) {
      boolean var1 = false;
      Inventory var2 = var0.getInventory();

      for (int var3 = 0; var3 < var2.getContainerSize(); var3++) {
         if (unseal(var2.getItem(var3))) {
            var1 = true;
         }
      }

      for (EquipmentSlot var6 : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND}) {
         if (unseal(var0.getItemBySlot(var6))) {
            var1 = true;
         }
      }

      return var1;
   }

   private static boolean unseal(ItemStack var0) {
      if (var0.isEmpty()) {
         return false;
      } else {
         CustomData var1 = (CustomData)var0.get(DataComponents.CUSTOM_DATA);
         if (var1 == null) {
            return false;
         } else {
            CompoundTag var2 = var1.copyTag();
            if (!var2.contains("the_sift:charoite_used")) {
               return false;
            } else {
               var2.remove("the_sift:charoite_used");
               var0.set(DataComponents.CUSTOM_DATA, CustomData.of(var2));
               return true;
            }
         }
      }
   }
}
