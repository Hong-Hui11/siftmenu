package com.echoes.siftmenu;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class MarketItems {
   private MarketItems() {
   }

   public static String id(Item var0) {
      Identifier var1 = BuiltInRegistries.ITEM.getKey(var0);
      return var1 == null ? "minecraft:air" : var1.toString();
   }

   public static Item item(String var0) {
      try {
         return (Item)BuiltInRegistries.ITEM.getOptional(Identifier.parse(var0)).orElse(null);
      } catch (Exception var2) {
         return null;
      }
   }

   public static String name(Item var0) {
      return var0 == null ? "未知物品" : new ItemStack(var0).getHoverName().getString();
   }

   public static Component displayName(Item var0) {
      return var0 == null ? Component.literal("未知物品") : Component.translatable(var0.getDescriptionId());
   }

   public static Component displayName(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return Component.literal("未知物品");
      } else if (!var0.has(DataComponents.ITEM_NAME) && !var0.has(DataComponents.CUSTOM_NAME)) {
         ItemEnchantments var1 = (ItemEnchantments)var0.get(DataComponents.STORED_ENCHANTMENTS);
         if (var1 != null && !var1.isEmpty()) {
            MutableComponent var2 = Component.translatable(var0.getItem().getDescriptionId()).copy();
            var2.append(Component.literal("（"));
            boolean var3 = true;

            for (Entry var5 : var1.entrySet()) {
               if (!var3) {
                  var2.append(Component.literal("、"));
               }

               var3 = false;
               var2.append(Enchantment.getFullname((Holder)var5.getKey(), var5.getIntValue()));
            }

            var2.append(Component.literal("）"));
            return var2;
         } else {
            return Component.translatable(var0.getItem().getDescriptionId());
         }
      } else {
         return var0.getHoverName();
      }
   }

   public static boolean plain(ItemStack var0) {
      return !var0.isEmpty() && var0.getComponentsPatch().isEmpty();
   }

   public static boolean marketable(ItemStack var0) {
      return !var0.isEmpty() && !SiftMenu.isMenuBook(var0);
   }

   public static String encode(ItemStack var0, RegistryAccess var1) {
      if (var0 != null && !var0.isEmpty()) {
         RegistryOps var2 = RegistryOps.create(NbtOps.INSTANCE, var1);
         return (String) ItemStack.CODEC.encodeStart(var2, var0.copy()).result().map(Object::toString).orElse("");
      } else {
         return "";
      }
   }

   public static ItemStack decode(String var0, Item var1, RegistryAccess var2) {
      if (var0 != null && !var0.isEmpty()) {
         try {
            CompoundTag var3 = TagParser.parseCompoundFully(var0);
            RegistryOps var4 = RegistryOps.create(NbtOps.INSTANCE, var2);
            ItemStack var5 = (ItemStack) ItemStack.CODEC.parse(var4, var3).result().orElse(ItemStack.EMPTY);
            if (!var5.isEmpty()) {
               return var5;
            }
         } catch (Exception var6) {
         }
      }

      return var1 == null ? ItemStack.EMPTY : new ItemStack(var1);
   }

   public static int count(Player var0, ItemStack var1) {
      if (!marketable(var1)) {
         return 0;
      } else {
         int var2 = 0;
         Inventory var3 = var0.getInventory();

         for (int var4 = 0; var4 < var3.getContainerSize(); var4++) {
            ItemStack var5 = var3.getItem(var4);
            if (ItemStack.isSameItemSameComponents(var5, var1)) {
               var2 += var5.getCount();
            }
         }

         return var2;
      }
   }

   public static int remove(Player var0, ItemStack var1, int var2) {
      int var3 = 0;
      Inventory var4 = var0.getInventory();

      for (int var5 = 0; var5 < var4.getContainerSize() && var3 < var2; var5++) {
         ItemStack var6 = var4.getItem(var5);
         if (ItemStack.isSameItemSameComponents(var6, var1)) {
            int var7 = Math.min(var2 - var3, var6.getCount());
            var4.removeItem(var5, var7);
            var3 += var7;
         }
      }

      return var3;
   }

   public static void give(ServerPlayer var0, ItemStack var1, int var2) {
      if (var1 != null && !var1.isEmpty() && var2 > 0) {
         int var3 = var2;
         int var4 = Math.max(1, var1.getMaxStackSize());

         while (var3 > 0) {
            int var5 = Math.min(var3, var4);
            ItemStack var6 = var1.copy();
            var6.setCount(var5);
            if (!var0.getInventory().add(var6)) {
               var0.drop(var6, false, Prediction.SERVER_ONLY);
            }

            var3 -= var5;
         }
      }
   }

   public static int count(Player var0, Item var1) {
      int var2 = 0;
      Inventory var3 = var0.getInventory();

      for (int var4 = 0; var4 < var3.getContainerSize(); var4++) {
         ItemStack var5 = var3.getItem(var4);
         if (!var5.isEmpty() && var5.getItem() == var1 && var5.getComponentsPatch().isEmpty()) {
            var2 += var5.getCount();
         }
      }

      return var2;
   }

   public static int remove(Player var0, Item var1, int var2) {
      int var3 = 0;
      Inventory var4 = var0.getInventory();

      for (int var5 = 0; var5 < var4.getContainerSize() && var3 < var2; var5++) {
         ItemStack var6 = var4.getItem(var5);
         if (!var6.isEmpty() && var6.getItem() == var1 && var6.getComponentsPatch().isEmpty()) {
            int var7 = Math.min(var2 - var3, var6.getCount());
            var4.removeItem(var5, var7);
            var3 += var7;
         }
      }

      return var3;
   }

   public static void give(ServerPlayer var0, Item var1, int var2) {
      if (var1 != null && var2 > 0) {
         int var3 = var2;
         int var4 = Math.max(1, var1.getDefaultMaxStackSize());

         while (var3 > 0) {
            int var5 = Math.min(var3, var4);
            ItemStack var6 = new ItemStack(var1, var5);
            if (!var0.getInventory().add(var6)) {
               var0.drop(var6, false, Prediction.SERVER_ONLY);
            }

            var3 -= var5;
         }
      }
   }

   public static boolean matchesOrder(ItemStack var0, ItemStack var1) {
      if (var0 != null && !var0.isEmpty() && var1 != null && !var1.isEmpty()) {
         if (var0.getItem() != var1.getItem()) {
            return false;
         } else if (SiftMenu.isMenuBook(var1)) {
            return false;
         } else if (!sameEnchantments(
            (ItemEnchantments)var0.get(DataComponents.STORED_ENCHANTMENTS), (ItemEnchantments)var1.get(DataComponents.STORED_ENCHANTMENTS)
         )) {
            return false;
         } else if (!sameEnchantments((ItemEnchantments)var0.get(DataComponents.ENCHANTMENTS), (ItemEnchantments)var1.get(DataComponents.ENCHANTMENTS))) {
            return false;
         } else {
            PotionContents var2 = (PotionContents)var0.get(DataComponents.POTION_CONTENTS);
            if (var2 != null) {
               PotionContents var3 = (PotionContents)var1.get(DataComponents.POTION_CONTENTS);
               if (!var2.equals(var3)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean sameEnchantments(ItemEnchantments var0, ItemEnchantments var1) {
      return var0 != null && !var0.isEmpty() ? var1 != null && var0.equals(var1) : true;
   }

   public static int countMatching(Player var0, ItemStack var1) {
      int var2 = 0;
      Inventory var3 = var0.getInventory();

      for (int var4 = 0; var4 < var3.getContainerSize(); var4++) {
         ItemStack var5 = var3.getItem(var4);
         if (matchesOrder(var1, var5)) {
            var2 += var5.getCount();
         }
      }

      return var2;
   }

   public static List<ItemStack> takeMatching(Player var0, ItemStack var1, int var2) {
      ArrayList var3 = new ArrayList();
      if (var2 <= 0) {
         return var3;
      } else {
         int var4 = var2;
         Inventory var5 = var0.getInventory();

         for (int var6 = 0; var6 < var5.getContainerSize() && var4 > 0; var6++) {
            ItemStack var7 = var5.getItem(var6);
            if (matchesOrder(var1, var7)) {
               int var8 = Math.min(var4, var7.getCount());
               ItemStack var9 = var7.copy();
               var9.setCount(var8);
               var3.add(var9);
               var5.removeItem(var6, var8);
               var4 -= var8;
            }
         }

         return var3;
      }
   }

   public static String encodeUnit(ItemStack var0, RegistryAccess var1) {
      if (var0 != null && !var0.isEmpty()) {
         ItemStack var2 = var0.copy();
         var2.setCount(1);
         return encode(var2, var1);
      } else {
         return "";
      }
   }
}
