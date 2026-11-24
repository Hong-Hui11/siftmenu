package com.echoes.siftmenu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class BingoHook {
   private BingoHook() {
   }

   public static void register() {
      try {
         Class var0 = Class.forName("me.jfenn.bingo.api.BingoEvents");
         Object var1 = var0.getField("GAME_ENDED").get(null);
         Method var2 = var1.getClass().getMethod("register", Consumer.class);
         var2.invoke(var1, (Consumer<Object>) BingoHook::onGameEnded);
         SiftMenu.LOGGER.info("[siftmenu] 已订阅 Bingo 结算事件");
      } catch (Throwable var3) {
         SiftMenu.LOGGER.warn("[siftmenu] 订阅 Bingo 结算事件失败，胜利奖励将不可用", var3);
      }
   }

   private static void onGameEnded(Object var0) {
      try {
         Object var1 = Class.forName("me.jfenn.bingo.api.BingoApi").getMethod("getINSTANCE").invoke(null);
         if (!(Class.forName("me.jfenn.bingo.api.IBingoApi").getMethod("getServer").invoke(var1) instanceof MinecraftServer var3)) {
            return;
         }

         Object var4 = Class.forName("me.jfenn.bingo.api.event.GameEndedEvent").getMethod("getId").invoke(var0);
         String var5 = String.valueOf(var4);
         var3.execute(() -> evaluate(var3, var1, var5));
      } catch (Throwable var6) {
         SiftMenu.LOGGER.warn("[siftmenu] 处理 Bingo 结算事件失败", var6);
      }
   }

   private static void evaluate(MinecraftServer var0, Object var1, String var2) {
      try {
         Class var3 = Class.forName("me.jfenn.bingo.api.data.IBingoTeam");
         Class var4 = Class.forName("me.jfenn.bingo.api.data.BingoTeamScore");
         if (!(Class.forName("me.jfenn.bingo.api.IBingoApi").getMethod("getTeams").invoke(var1) instanceof Iterable var6)) {
            return;
         }

         ArrayList var7 = new ArrayList();

         for (Object var9 : var6) {
            var7.add(var9);
         }

         int var24 = -1;
         int var25 = -1;
         int var10 = -1;

         for (Object var12 : var7) {
            Object var13 = var3.getMethod("getScore").invoke(var12);
            int var14 = (Integer)var4.getMethod("getLines").invoke(var13);
            int var15 = (Integer)var4.getMethod("getCards").invoke(var13);
            int var16 = (Integer)var4.getMethod("getItems").invoke(var13);
            if (var24 < 0 || better(var14, var15, var16, var24, var25, var10)) {
               var24 = var14;
               var25 = var15;
               var10 = var16;
            }
         }

         if (var24 <= 0 && var25 <= 0 && var10 <= 0) {
            return;
         }

         ArrayList var26 = new ArrayList();

         for (Object var29 : var7) {
            Object var31 = var3.getMethod("getScore").invoke(var29);
            int var33 = (Integer)var4.getMethod("getLines").invoke(var31);
            int var35 = (Integer)var4.getMethod("getCards").invoke(var31);
            int var17 = (Integer)var4.getMethod("getItems").invoke(var31);
            if (var33 == var24 && var35 == var25 && var17 == var10) {
               Object var18 = var3.getMethod("getPlayers").invoke(var29);
               if (var18 instanceof List) {
                  for (Object var21 : (List)var18) {
                     if (var21 instanceof UUID var22) {
                        var26.add(var22);
                     }
                  }
               }
            }
         }

         if (var26.isEmpty()) {
            return;
         }

         ArrayList var28 = new ArrayList();

         for (UUID var32 : (List<UUID>) var26) {
            ServerPlayer var34 = var0.getPlayerList().getPlayer(var32);
            if (var34 != null) {
               var28.add(var34.getGameProfile().name());
               BingoReward.recordWin(var34, var2);
            }
         }

         if (var28.isEmpty()) {
            return;
         }

         SiftMenu.LOGGER.info("[siftmenu] Bingo 结算：{} 获胜（连线 {} / 卡片 {} / 物品 {}）", new Object[]{var28, var24, var25, var10});
         var0.getPlayerList().broadcastSystemMessage(Component.literal("Bingo 对局结束：" + String.join("、", var28) + " 获胜！").withStyle(ChatFormatting.GOLD), false);
      } catch (Throwable var23) {
         SiftMenu.LOGGER.warn("[siftmenu] 判定 Bingo 获胜队伍失败", var23);
      }
   }

   private static boolean better(int var0, int var1, int var2, int var3, int var4, int var5) {
      if (var3 < 0) {
         return true;
      } else if (var0 != var3) {
         return var0 > var3;
      } else {
         return var1 != var4 ? var1 > var4 : var2 > var5;
      }
   }
}
