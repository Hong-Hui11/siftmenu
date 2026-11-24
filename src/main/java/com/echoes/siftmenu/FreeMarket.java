package com.echoes.siftmenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FreeMarket {
   public static final String SELL = "SELL";
   public static final String BUY = "BUY";
   public static final long DURATION = 604800000L;
   public static final int MAX_SELL_QUANTITY = 576;
   public static final int MAX_BUY_QUANTITY = 64000;
   public static final long MAX_UNIT_PRICE = Money.fromCoins(100000L);
   public static final long LISTING_FEE = Money.fromCoins(10L);
   public static final int TAX_PERCENT = 5;
   public static final long TAX_MIN_TOTAL = Money.fromCoins(200L);

   private FreeMarket() {
   }

   public static long taxOf(long var0) {
      return var0 < TAX_MIN_TOTAL ? 0L : Math.max(0L, var0 * 5L / 100L);
   }

   private static boolean chargeListingFee(ServerPlayer var0, MenuData var1) {
      if (!var1.takeBalance(var0.getUUID(), LISTING_FEE)) {
         var0.sendSystemMessage(Component.literal("金币不足，发布挂单需要 " + Economy.format(LISTING_FEE) + " 金币挂单费").withStyle(ChatFormatting.RED));
         return false;
      } else {
         var1.dailyFee = var1.dailyFee + LISTING_FEE;
         return true;
      }
   }

   private static void refundListingFee(MenuData var0, UUID var1) {
      var0.dailyFee = Math.max(0L, var0.dailyFee - LISTING_FEE);
      var0.addBalance(var1, LISTING_FEE);
   }

   public static int maxQuantity(String var0) {
      return "BUY".equals(var0) ? 64000 : 576;
   }

   public static List<MenuData.MarketListing> activeListings(String var0) {
      return MenuData.get().activeListings(var0);
   }

   public static List<MenuData.MarketListing> myListings(UUID var0) {
      ArrayList var1 = new ArrayList();
      long var2 = System.currentTimeMillis();

      for (MenuData.MarketListing var5 : MenuData.get().listings) {
         if (var5.quantity > 0 && var5.expires > var2 && var5.owner.equals(var0.toString())) {
            var1.add(var5);
         }
      }

      var1.sort(Comparator.<MenuData.MarketListing>comparingLong(var0x -> var0x.created).reversed());
      return var1;
   }

   public static boolean updatePrice(ServerPlayer var0, String var1, long var2) {
      MenuData var4 = MenuData.get();
      MenuData.MarketListing var5 = var4.listing(var1);
      if (!ownedAndActive(var0, var5)) {
         return false;
      } else if (var2 >= 1L && var2 <= MAX_UNIT_PRICE) {
         long var6 = var5.unitPrice;
         if ("BUY".equals(var5.type)) {
            long var8 = (var2 - var6) * (long)var5.quantity;
            if (var8 > 0L && !var4.takeBalance(var0.getUUID(), var8)) {
               var0.sendSystemMessage(Component.literal("金币不足，改价需要补 " + Economy.format(var8) + " 金币").withStyle(ChatFormatting.RED));
               return false;
            }

            if (var8 < 0L) {
               var4.addBalance(var0.getUUID(), -var8);
            }
         }

         var5.unitPrice = var2;
         var4.save();
         var0.sendSystemMessage(Component.literal("已修改单价：" + Economy.format(var6) + " → " + Economy.format(var2) + " 金币/个").withStyle(ChatFormatting.GREEN));
         return true;
      } else {
         var0.sendSystemMessage(Component.literal("单价需在 1-" + Economy.format(MAX_UNIT_PRICE) + " 金币之间").withStyle(ChatFormatting.RED));
         return false;
      }
   }

   public static boolean updateQuantity(ServerPlayer var0, String var1, int var2) {
      MenuData var3 = MenuData.get();
      MenuData.MarketListing var4 = var3.listing(var1);
      if (!ownedAndActive(var0, var4)) {
         return false;
      } else {
         int var5 = maxQuantity(var4.type);
         if (var2 >= 1 && var2 <= var5) {
            int var6 = var4.quantity;
            if ("SELL".equals(var4.type)) {
               ItemStack var7 = stackOf(var4, var0.level().registryAccess());
               if (var7.isEmpty()) {
                  var0.sendSystemMessage(Component.literal("物品不存在，无法修改数量").withStyle(ChatFormatting.RED));
                  return false;
               }

               if (var2 > var6) {
                  int var8 = var2 - var6;
                  if (MarketItems.count(var0, var7) < var8) {
                     var0.sendSystemMessage(Component.literal("背包里的物品数量不足，无法增加数量").withStyle(ChatFormatting.RED));
                     return false;
                  }

                  int var9 = MarketItems.remove(var0, var7, var8);
                  if (var9 < var8) {
                     notifyOverflow(var0, giveOrStore(var0, var7, var9));
                     var0.sendSystemMessage(Component.literal("扣除物品失败，数量未修改").withStyle(ChatFormatting.RED));
                     return false;
                  }
               } else if (var2 < var6) {
                  notifyOverflow(var0, giveOrStore(var0, var7, var6 - var2));
               }
            } else if ("BUY".equals(var4.type)) {
               long var10 = (long)(var2 - var6) * var4.unitPrice;
               if (var10 > 0L && !var3.takeBalance(var0.getUUID(), var10)) {
                  var0.sendSystemMessage(Component.literal("金币不足，增加数量需要补 " + Economy.format(var10) + " 金币").withStyle(ChatFormatting.RED));
                  return false;
               }

               if (var10 < 0L) {
                  var3.addBalance(var0.getUUID(), -var10);
               }
            }

            var4.quantity = var2;
            var3.save();
            var0.sendSystemMessage(Component.literal("已修改数量：" + var6 + " → " + var2).withStyle(ChatFormatting.GREEN));
            return true;
         } else {
            var0.sendSystemMessage(Component.literal("数量需在 1-" + var5 + " 之间").withStyle(ChatFormatting.RED));
            return false;
         }
      }
   }

   public static boolean cancel(ServerPlayer var0, String var1) {
      MenuData var2 = MenuData.get();
      MenuData.MarketListing var3 = var2.listing(var1);
      if (!ownedAndActive(var0, var3)) {
         return false;
      } else {
         ItemStack var4 = stackOf(var3, var0.level().registryAccess());
         String var5 = var4.isEmpty() ? var3.item : MarketItems.name(var4.getItem());
         if ("SELL".equals(var3.type)) {
            int var6 = 0;
            if (!var4.isEmpty()) {
               var6 = giveOrStore(var0, var4, var3.quantity);
            }

            notifyOverflow(var0, var6);
            var0.sendSystemMessage(Component.literal("已下架出售挂单：" + var5 + " x" + var3.quantity + "，物品已退回背包").withStyle(ChatFormatting.YELLOW));
         } else {
            long var8 = var3.remainingEscrow();
            var2.addBalance(var0.getUUID(), var8);
            var0.sendSystemMessage(Component.literal("已下架收购挂单：" + var5 + "，退还 " + Economy.format(var8) + " 金币").withStyle(ChatFormatting.YELLOW));
         }

         var2.listings.remove(var3);
         var2.save();
         return true;
      }
   }

   private static boolean ownedAndActive(ServerPlayer var0, MenuData.MarketListing var1) {
      if (var1 == null || var1.expires <= System.currentTimeMillis() || var1.quantity <= 0) {
         var0.sendSystemMessage(Component.literal("该挂单已失效。").withStyle(ChatFormatting.RED));
         return false;
      } else if (!var1.owner.equals(var0.getUUID().toString())) {
         var0.sendSystemMessage(Component.literal("这不是你的挂单。").withStyle(ChatFormatting.RED));
         return false;
      } else {
         return true;
      }
   }

   public static boolean createSell(ServerPlayer var0, int var1, long var2) {
      ItemStack var4 = var0.getMainHandItem();
      if (!validSample(var4)) {
         var0.sendSystemMessage(Component.literal("请手持要出售的物品（不能是菜单书）").withStyle(ChatFormatting.RED));
         return false;
      } else {
         ItemStack var5 = var4.copy();
         var5.setCount(1);
         if (var1 < 1 || var1 > 576) {
            var0.sendSystemMessage(Component.literal("出售数量需在 1-576 之间（最多 9 组）").withStyle(ChatFormatting.RED));
            return false;
         } else if (var2 >= 1L && var2 <= MAX_UNIT_PRICE) {
            String var6 = MarketItems.encodeUnit(var5, var0.level().registryAccess());
            if (var6.isEmpty()) {
               var0.sendSystemMessage(Component.literal("该物品的数据无法保存，暂时无法上架").withStyle(ChatFormatting.RED));
               return false;
            } else if (MarketItems.count(var0, var5) < var1) {
               var0.sendSystemMessage(Component.literal("背包里的物品数量不足").withStyle(ChatFormatting.RED));
               return false;
            } else {
               MenuData var7 = MenuData.get();
               var7.dailyReset();
               if (!chargeListingFee(var0, var7)) {
                  return false;
               } else {
                  int var8 = MarketItems.remove(var0, var5, var1);
                  if (var8 != var1) {
                     refundListingFee(var7, var0.getUUID());
                     notifyOverflow(var0, giveOrStore(var0, var5, var8));
                     var0.sendSystemMessage(Component.literal("出售挂单创建失败，物品与挂单费已退回").withStyle(ChatFormatting.RED));
                     return false;
                  } else {
                     MenuData.MarketListing var9 = newListing(var0, "SELL", var5, var6, var1, var2);
                     var7.listings.add(var9);
                     var7.save();
                     broadcast(var0.level().getServer(), var9);
                     var0.sendSystemMessage(
                        Component.literal("出售挂单已发布：" + var1 + "x ")
                           .append(MarketItems.displayName(var5))
                           .append(Component.literal("，单价 " + Economy.format(var2) + " 金币"))
                           .withStyle(ChatFormatting.GREEN)
                     );
                     var0.sendSystemMessage(
                        Component.literal("已收挂单费 " + Economy.format(LISTING_FEE) + " 金币；成交税 5%（单笔低于 " + Economy.format(TAX_MIN_TOTAL) + " 金币免税）")
                           .withStyle(ChatFormatting.GRAY)
                     );
                     return true;
                  }
               }
            }
         } else {
            var0.sendSystemMessage(Component.literal("单价需在 1-" + Economy.format(MAX_UNIT_PRICE) + " 金币之间").withStyle(ChatFormatting.RED));
            return false;
         }
      }
   }

   public static boolean createBuy(ServerPlayer var0, int var1, long var2) {
      ItemStack var4 = var0.getMainHandItem();
      if (!validSample(var4)) {
         var0.sendSystemMessage(Component.literal("请手持想收购的物品作为样品（不能是菜单书）").withStyle(ChatFormatting.RED));
         return false;
      } else {
         ItemStack var5 = var4.copy();
         var5.setCount(1);
         if (var1 < 1 || var1 > 64000) {
            var0.sendSystemMessage(Component.literal("收购数量需在 1-64000 之间（最多 1000 组，每组 64 个）").withStyle(ChatFormatting.RED));
            return false;
         } else if (var2 >= 1L && var2 <= MAX_UNIT_PRICE) {
            String var6 = MarketItems.encodeUnit(var5, var0.level().registryAccess());
            if (var6.isEmpty()) {
               var0.sendSystemMessage(Component.literal("该物品的数据无法保存，暂时无法发布收购").withStyle(ChatFormatting.RED));
               return false;
            } else {
               long var7 = (long)var1 * var2;
               long var9 = var7 + LISTING_FEE;
               MenuData var11 = MenuData.get();
               var11.dailyReset();
               if (!var11.takeBalance(var0.getUUID(), var9)) {
                  var0.sendSystemMessage(
                     Component.literal("金币不足，需要 " + Economy.format(var9) + " 金币（含挂单费 " + Economy.format(LISTING_FEE) + "）").withStyle(ChatFormatting.RED)
                  );
                  return false;
               } else {
                  var11.dailyFee = var11.dailyFee + LISTING_FEE;
                  MenuData.MarketListing var12 = newListing(var0, "BUY", var5, var6, var1, var2);
                  var11.listings.add(var12);
                  var11.save();
                  broadcast(var0.level().getServer(), var12);
                  var0.sendSystemMessage(
                     Component.literal("收购挂单已发布：" + var1 + "x ")
                        .append(MarketItems.displayName(var5))
                        .append(Component.literal("，单价 " + Economy.format(var2) + " 金币"))
                        .withStyle(ChatFormatting.GREEN)
                  );
                  return true;
               }
            }
         } else {
            var0.sendSystemMessage(Component.literal("单价需在 1-" + Economy.format(MAX_UNIT_PRICE) + " 金币之间").withStyle(ChatFormatting.RED));
            return false;
         }
      }
   }

   public static void buySellListing(ServerPlayer var0, String var1, boolean var2) {
      MenuData var3 = MenuData.get();
      var3.dailyReset();
      MenuData.MarketListing var4 = var3.listing(var1);
      if (var4 == null || !"SELL".equals(var4.type) || var4.expires <= System.currentTimeMillis() || var4.quantity <= 0) {
         var0.sendSystemMessage(Component.literal("该挂单已失效。").withStyle(ChatFormatting.RED));
      } else if (var4.owner.equals(var0.getUUID().toString())) {
         var0.sendSystemMessage(Component.literal("不能购买自己的挂单。").withStyle(ChatFormatting.RED));
      } else if (var4.unitPrice > 0L) {
         int var5 = var2 ? var4.quantity : 1;
         long var6 = Economy.balance(var0) / var4.unitPrice;
         var5 = (int)Math.min((long)var5, Math.min((long)var4.quantity, var6));
         if (var5 <= 0) {
            var0.sendSystemMessage(Component.literal("金币不足，无法购买。").withStyle(ChatFormatting.RED));
         } else {
            ItemStack var8 = stackOf(var4, var0.level().registryAccess());
            if (var8.isEmpty()) {
               var0.sendSystemMessage(Component.literal("物品不存在，挂单已失效。").withStyle(ChatFormatting.RED));
            } else {
               Item var9 = var8.getItem();
               long var10 = (long)var5 * var4.unitPrice;
               if (var3.takeBalance(var0.getUUID(), var10)) {
                  long var12 = taxOf(var10);
                  long var14 = var10 - var12;
                  var3.addBalance(UUID.fromString(var4.owner), var14);
                  var3.dailyTax += var12;
                  var4.quantity -= var5;
                  if (var4.quantity <= 0) {
                     var3.listings.remove(var4);
                  }

                  var3.save();
                  int var16 = giveOrStore(var0, var8, var5);
                  var0.sendSystemMessage(
                     Component.literal("已购买 " + var5 + "x ")
                        .append(MarketItems.displayName(var8))
                        .append(Component.literal("，花费 " + Economy.format(var10) + " 金币"))
                        .withStyle(ChatFormatting.GREEN)
                  );
                  notifyOverflow(var0, var16);
                  ServerPlayer var17 = var0.level().getServer().getPlayerList().getPlayer(UUID.fromString(var4.owner));
                  if (var17 != null) {
                     var17.sendSystemMessage(
                        Component.literal("你在自由市场卖出了 " + var5 + "x ")
                           .append(MarketItems.displayName(var8))
                           .append(Component.literal("，获得 " + Economy.format(var14) + " 金币" + (var12 > 0L ? "（成交税 " + Economy.format(var12) + "）" : "")))
                           .withStyle(ChatFormatting.GOLD)
                     );
                  }
               }
            }
         }
      }
   }

   public static void fulfillBuyOrder(ServerPlayer var0, String var1, boolean var2) {
      MenuData var3 = MenuData.get();
      var3.dailyReset();
      MenuData.MarketListing var4 = var3.listing(var1);
      if (var4 == null || !"BUY".equals(var4.type) || var4.expires <= System.currentTimeMillis() || var4.quantity <= 0) {
         var0.sendSystemMessage(Component.literal("该挂单已失效。").withStyle(ChatFormatting.RED));
      } else if (var4.owner.equals(var0.getUUID().toString())) {
         var0.sendSystemMessage(Component.literal("不能履约自己的收购挂单。").withStyle(ChatFormatting.RED));
      } else {
         Item var5 = MarketItems.item(var4.item);
         if (var5 == null) {
            var0.sendSystemMessage(Component.literal("物品不存在，挂单已失效。").withStyle(ChatFormatting.RED));
         } else {
            ItemStack var6 = stackOf(var4, var0.level().registryAccess());
            if (var6.isEmpty()) {
               var6 = new ItemStack(var5);
            }

            int var7 = MarketItems.countMatching(var0, var6);
            int var8 = var2 ? Math.min(var4.quantity, var7) : Math.min(1, var7);
            if (var8 <= 0) {
               var0.sendSystemMessage(Component.literal("背包里没有该物品。").withStyle(ChatFormatting.RED));
            } else {
               List<ItemStack> var9 = MarketItems.takeMatching(var0, var6, var8);
               int var10 = 0;

               for (ItemStack var12 : var9) {
                  var10 += var12.getCount();
               }

               if (var10 > 0) {
                  long var22 = (long)var10 * var4.unitPrice;
                  long var13 = taxOf(var22);
                  long var15 = var22 - var13;
                  var4.quantity -= var10;
                  if (var4.quantity <= 0) {
                     var3.listings.remove(var4);
                  }

                  var3.addBalance(var0.getUUID(), var15);
                  var3.dailyTax += var13;
                  var3.save();
                  UUID var17 = UUID.fromString(var4.owner);
                  ServerPlayer var18 = var0.level().getServer().getPlayerList().getPlayer(var17);
                  if (var18 != null) {
                     int var19 = 0;

                     for (ItemStack var21 : var9) {
                        var19 += giveOrStore(var18, var21, var21.getCount());
                     }

                     var18.sendSystemMessage(Component.literal("你在自由市场收购的 " + var10 + " 件物品已到账").withStyle(ChatFormatting.GOLD));
                     notifyOverflow(var18, var19);
                  } else {
                     for (ItemStack var24 : var9) {
                        var3.addPendingItem(
                           var17, MarketItems.id(var24.getItem()), MarketItems.encodeUnit(var24, var0.level().registryAccess()), var24.getCount()
                        );
                     }
                  }

                  var0.sendSystemMessage(
                     Component.literal("已履约 " + var10 + " 件物品，获得 " + Economy.format(var15) + " 金币" + (var13 > 0L ? "（成交税 " + Economy.format(var13) + "）" : ""))
                        .withStyle(ChatFormatting.GREEN)
                  );
               }
            }
         }
      }
   }

   public static void cleanup(MinecraftServer var0) {
      MenuData var1 = MenuData.get();
      long var2 = System.currentTimeMillis();
      boolean var4 = false;
      Iterator var5 = var1.listings.iterator();

      while (var5.hasNext()) {
         MenuData.MarketListing var6 = (MenuData.MarketListing)var5.next();
         if (var6.expires <= var2) {
            UUID var7 = UUID.fromString(var6.owner);
            ServerPlayer var8 = var0.getPlayerList().getPlayer(var7);
            if ("SELL".equals(var6.type)) {
               ItemStack var9 = stackOf(var6, var0.registryAccess());
               if (var8 != null) {
                  int var10 = 0;
                  if (!var9.isEmpty()) {
                     var10 = giveOrStore(var8, var9, var6.quantity);
                  }

                  notifyOverflow(var8, var10);
                  var8.sendSystemMessage(Component.literal("你的自由市场出售挂单已到期，物品已退回").withStyle(ChatFormatting.YELLOW));
               } else {
                  var1.addPendingItem(var7, var6.item, var6.itemData, var6.quantity);
               }
            } else if ("BUY".equals(var6.type)) {
               long var11 = var6.remainingEscrow();
               var1.addBalance(var7, var11);
               if (var8 != null) {
                  var8.sendSystemMessage(Component.literal("你的自由市场收购挂单已到期，已退还 " + Economy.format(var11) + " 金币").withStyle(ChatFormatting.YELLOW));
               }
            }

            var5.remove();
            var4 = true;
         }
      }

      if (var4) {
         var1.save();
      }
   }

   public static void deliverPending(ServerPlayer var0) {
      MenuData var1 = MenuData.get();
      List var2 = var1.pendingItems(var0.getUUID());
      if (!var2.isEmpty()) {
         int var3 = 0;
         int var4 = 0;

         for (int var5 = var2.size() - 1; var5 >= 0; var5--) {
            MenuData.PendingItem var6 = (MenuData.PendingItem)var2.get(var5);
            ItemStack var7 = MarketItems.decode(var6.itemData, MarketItems.item(var6.item), var0.level().registryAccess());
            if (!var7.isEmpty() && var6.count > 0) {
               int var8 = addToInventory(var0, var7, var6.count);
               var3 += var6.count - var8;
               if (var8 <= 0) {
                  var1.removePendingAt(var0.getUUID(), var5);
               } else {
                  var1.setPendingCount(var0.getUUID(), var5, var8);
                  var4 += var8;
               }
            } else {
               var1.removePendingAt(var0.getUUID(), var5);
            }
         }

         if (var3 > 0) {
            var0.sendSystemMessage(Component.literal("自由市场待领取物品已发放：" + var3 + " 件").withStyle(ChatFormatting.GOLD));
         }

         if (var4 > 0) {
            var0.sendSystemMessage(Component.literal("背包已满，剩余 " + var4 + " 件可在「自由市场 - 待领取物品」领取").withStyle(ChatFormatting.YELLOW));
         }
      }
   }

   public static void claimPending(ServerPlayer var0, int var1) {
      MenuData var2 = MenuData.get();
      MenuData.PendingItem var3 = var2.pendingAt(var0.getUUID(), var1);
      if (var3 != null) {
         ItemStack var4 = MarketItems.decode(var3.itemData, MarketItems.item(var3.item), var0.level().registryAccess());
         if (var4.isEmpty()) {
            var2.removePendingAt(var0.getUUID(), var1);
            var0.sendSystemMessage(Component.literal("该待领取物品数据已失效，已移除").withStyle(ChatFormatting.RED));
         } else {
            int var5 = Math.max(1, var3.count);
            int var6 = addToInventory(var0, var4, var5);
            int var7 = var5 - var6;
            if (var6 <= 0) {
               var2.removePendingAt(var0.getUUID(), var1);
            } else {
               var2.setPendingCount(var0.getUUID(), var1, var6);
            }

            if (var7 > 0) {
               var0.sendSystemMessage(Component.literal("已领取 " + var7 + " 件 ").append(MarketItems.displayName(var4)).withStyle(ChatFormatting.GREEN));
            }

            if (var6 > 0) {
               var0.sendSystemMessage(Component.literal("背包已满，剩余 " + var6 + " 件仍存放在这里").withStyle(ChatFormatting.YELLOW));
            }
         }
      }
   }

   public static int giveOrStore(ServerPlayer var0, ItemStack var1, int var2) {
      if (var1 != null && !var1.isEmpty() && var2 > 0) {
         int var3 = addToInventory(var0, var1, var2);
         if (var3 > 0) {
            MenuData.get().addPendingItem(var0.getUUID(), MarketItems.id(var1.getItem()), MarketItems.encodeUnit(var1, var0.level().registryAccess()), var3);
         }

         return var3;
      } else {
         return 0;
      }
   }

   public static void notifyOverflow(ServerPlayer var0, int var1) {
      if (var1 > 0) {
         var0.sendSystemMessage(Component.literal("背包已满，" + var1 + " 件物品已存入「自由市场 - 待领取物品」").withStyle(ChatFormatting.YELLOW));
      }
   }

   private static int addToInventory(ServerPlayer var0, ItemStack var1, int var2) {
      int var3 = var2;
      int var4 = Math.max(1, var1.getMaxStackSize());

      while (var3 > 0) {
         int var5 = Math.min(var3, var4);
         ItemStack var6 = var1.copy();
         var6.setCount(var5);
         if (!var0.getInventory().add(var6)) {
            int var7 = var5 - var6.getCount();
            var3 -= var7;
            if (var7 <= 0) {
               break;
            }
         } else {
            var3 -= var5;
         }
      }

      return var3;
   }

   private static boolean validSample(ItemStack var0) {
      return MarketItems.marketable(var0);
   }

   public static ItemStack stackOf(MenuData.MarketListing var0, RegistryAccess var1) {
      return MarketItems.decode(var0.itemData, MarketItems.item(var0.item), var1);
   }

   private static MenuData.MarketListing newListing(ServerPlayer var0, String var1, ItemStack var2, String var3, int var4, long var5) {
      long var7 = System.currentTimeMillis();
      MenuData.MarketListing var9 = new MenuData.MarketListing();
      var9.id = UUID.randomUUID().toString();
      var9.type = var1;
      var9.owner = var0.getUUID().toString();
      var9.ownerName = var0.getGameProfile().name();
      var9.item = MarketItems.id(var2.getItem());
      var9.itemData = var3;
      var9.quantity = var4;
      var9.unitPrice = var5;
      var9.created = var7;
      var9.expires = var7 + 604800000L;
      return var9;
   }

   private static void broadcast(MinecraftServer var0, MenuData.MarketListing var1) {
      String var2 = "SELL".equals(var1.type) ? "出售" : "收购";
      String var3 = "SELL".equals(var1.type) ? "/siftmenu free sell" : "/siftmenu free buy";
      MutableComponent var4 = Component.literal("[查看]")
         .withStyle(
            var1x -> var1x.withColor(ChatFormatting.AQUA)
                  .withBold(true)
                  .withClickEvent(new RunCommand(var3))
                  .withHoverEvent(new ShowText(Component.literal("打开自由市场")))
         );
      MutableComponent var5 = Component.literal("[自由市场] " + var1.ownerName + " " + var2 + " " + var1.quantity + "x ")
         .append(MarketItems.displayName(stackOf(var1, var0.registryAccess())))
         .append(Component.literal("，单价 " + Economy.format(var1.unitPrice) + " 金币  "))
         .withStyle(ChatFormatting.GOLD)
         .append(var4);

      for (ServerPlayer var7 : var0.getPlayerList().getPlayers()) {
         var7.sendSystemMessage(var5);
      }
   }

   public static String remainingText(long var0) {
      long var2 = var0 - System.currentTimeMillis();
      if (var2 <= 0L) {
         return "已过期";
      } else {
         long var4 = var2 / 3600000L;
         long var6 = var4 / 24L;
         long var8 = var4 % 24L;
         return var6 > 0L ? var6 + "天" + var8 + "小时" : Math.max(1L, var4) + "小时";
      }
   }
}
