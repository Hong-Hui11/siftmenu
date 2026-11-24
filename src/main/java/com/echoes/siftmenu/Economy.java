package com.echoes.siftmenu;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Economy {
   public static final long STARTING_BALANCE = Money.fromCoins(500L);

   private Economy() {
   }

   public static long balance(ServerPlayer var0) {
      return MenuData.get().balance(var0.getUUID());
   }

   public static long total() {
      return MenuData.get().totalMoney();
   }

   public static void ensureAccount(ServerPlayer var0) {
      MenuData var1 = MenuData.get();
      if (!var1.hasAccount(var0.getUUID())) {
         var1.setBalance(var0.getUUID(), STARTING_BALANCE);
         var0.sendSystemMessage(Component.literal("已获得初始金币：" + format(STARTING_BALANCE)).withStyle(ChatFormatting.GOLD));
      }
   }

   public static boolean spend(ServerPlayer var0, long var1) {
      if (var1 <= 0L) {
         return true;
      } else {
         MenuData var3 = MenuData.get();
         return var3.takeBalance(var0.getUUID(), var1);
      }
   }

   public static void add(UUID var0, long var1) {
      if (var1 > 0L) {
         MenuData var3 = MenuData.get();
         var3.dailyReset();
         var3.addBalance(var0, var1);
         var3.dailyCreated += var1;
      }
   }

   public static boolean chargeService(ServerPlayer var0, long var1, String var3) {
      if (var1 <= 0L) {
         return true;
      } else {
         MenuData var4 = MenuData.get();
         if (!var4.takeBalance(var0.getUUID(), var1)) {
            var0.sendSystemMessage(Component.literal(var3 + "需要 " + format(var1) + " 金币，余额不足。").withStyle(ChatFormatting.RED));
            return false;
         } else {
            var4.dailyReset();
            var4.dailyService += var1;
            var4.save();
            return true;
         }
      }
   }

   public static void add(ServerPlayer var0, long var1) {
      add(var0.getUUID(), var1);
   }

   public static String format(long var0) {
      return Money.format(var0);
   }

   public static void report(ServerPlayer var0) {
      MenuData var1 = MenuData.get();
      var1.dailyReset();
      long var2 = var1.totalMoney();
      long var4 = 0L;

      for (Long var7 : var1.dailySold.values()) {
         if (var7 != null) {
            var4 += var7;
         }
      }

      int var8 = var1.activeListings("SELL").size();
      int var9 = var1.activeListings("BUY").size();
      var0.sendSystemMessage(Component.literal("—— 筛界经济面板 · " + var1.dailyDate + " ——").withStyle(ChatFormatting.GOLD));
      var0.sendSystemMessage(Component.literal("总金币：" + format(var2) + "（" + var1.balances.size() + " 个账户）").withStyle(ChatFormatting.WHITE));
      var0.sendSystemMessage(Component.literal("今日产出：" + format(var1.dailyCreated) + "（悬赏 / Bingo / 收购，其中收购 " + var4 + " 件）").withStyle(ChatFormatting.GREEN));
      var0.sendSystemMessage(Component.literal("今日回收：" + format(var1.dailySpent) + "（玩家从服务器购买）").withStyle(ChatFormatting.AQUA));
      var0.sendSystemMessage(
         Component.literal("今日成交税：" + format(var1.dailyTax) + "（5%，单笔低于 " + format(FreeMarket.TAX_MIN_TOTAL) + " 免税）").withStyle(ChatFormatting.YELLOW)
      );
      var0.sendSystemMessage(
         Component.literal("今日挂单费：" + format(var1.dailyFee) + "（每单 " + format(FreeMarket.LISTING_FEE) + "）").withStyle(ChatFormatting.YELLOW)
      );
      var0.sendSystemMessage(Component.literal("今日服务费：" + format(var1.dailyService) + "（传送消耗）").withStyle(ChatFormatting.YELLOW));
      var0.sendSystemMessage(Component.literal("有效挂单：出售 " + var8 + " / 收购 " + var9).withStyle(ChatFormatting.GRAY));
   }
}
