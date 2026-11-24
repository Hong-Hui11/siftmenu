package com.echoes.siftmenu;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class BountyService {
   public static final int BOARD_SIZE = 56;
   public static final int DAILY_ACCEPT_LIMIT = 5;
   public static final int DAILY_EXTREME_LIMIT = 2;
   private static final double TRAVEL_MAX_STEP = 64.0;
   private static final int TICKS_PER_MINUTE = 1200;
   public static final String OPEN = "OPEN";
   public static final String TAKEN = "TAKEN";
   public static final String DONE = "DONE";
   public static final String FAILED = "FAILED";
   private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
   private static final String BOARD_VERSION = "bounty-v3";
   private static boolean dirty;
   private static final Map<UUID, BountyService.TravelState> TRAVEL = new HashMap<>();
   private static final Map<UUID, Integer> PLAY_TICKS = new HashMap<>();
   private static final Map<UUID, BountyHudPayload> LAST_HUD = new HashMap<>();

   private BountyService() {
   }

   public static boolean ensureBoard(MenuData var0) {
      String var1 = todayText();
      if (var1.equals(var0.bountyDay) && var0.bountyBoard.size() == 56) {
         return false;
      } else {
         rollBoard(var0, var1);
         return true;
      }
   }

   public static void tick(MinecraftServer var0) {
      if (var0 != null) {
         MenuData var1 = MenuData.get();
         if (ensureBoard(var1)) {
            for (ServerPlayer var3 : var0.getPlayerList().getPlayers()) {
               var3.sendSystemMessage(Component.literal("悬赏板已刷新，今天有 56 个新悬赏。").withStyle(ChatFormatting.GOLD));
            }

            var1.save();
         }

         if (dirty) {
            dirty = false;
            var1.save();
         }

         tickAdvancements(var0);
         pushHudAll(var0);
      }
   }

   public static void tickAdvancements(MinecraftServer var0) {
      if (var0 != null) {
         MenuData var1 = MenuData.get();
         for (ServerPlayer var3 : var0.getPlayerList().getPlayers()) {
            UUID var4 = var3.getUUID();
            for (MenuData.BountyEntry var6 : var1.bountyBoard) {
               if ("TAKEN".equals(var6.state) && var4.toString().equals(var6.acceptedBy)) {
                  BountyCatalog.Bounty var7 = BountyCatalog.byId(var6.bountyId);
                  if (var7 != null
                     && BountyCatalog.ADVANCEMENT.equals(var7.trigger())
                     && progress(var4, var7.id()) < var7.amount()
                     && advancementDone(var3, var7.match())) {
                     advance(var3, BountyCatalog.ADVANCEMENT, var7.match(), 1L);
                  }
               }
            }
         }
      }
   }

   private static boolean advancementDone(ServerPlayer var0, String var1) {
      MinecraftServer var2 = var0.level().getServer();
      if (var2 == null) {
         return false;
      } else {
         Identifier var3 = Identifier.tryParse(var1);
         if (var3 == null) {
            return false;
         } else {
            AdvancementHolder var4 = var2.getAdvancements().get(var3);
            return var4 != null && var0.getAdvancements().getOrStartProgress(var4).isDone();
         }
      }
   }

   public static void reset(MenuData var0) {
      rollBoard(var0, todayText());
   }

   public static MenuData.BountyEntry entry(MenuData var0, int var1) {
      for (MenuData.BountyEntry var3 : var0.bountyBoard) {
         if (var3.boardId == var1) {
            return var3;
         }
      }

      return null;
   }

   public static long progress(UUID var0, int var1) {
      Long var2 = MenuData.get().bountyProgress.get(progressKey(var0, var1));
      return var2 == null ? 0L : var2;
   }

   public static boolean isMine(MenuData.BountyEntry var0, UUID var1) {
      return var0 != null && "TAKEN".equals(var0.state) && var0.acceptedBy != null && var0.acceptedBy.equals(var1.toString());
   }

   public static List<MenuData.BountyEntry> takenBy(MenuData var0, UUID var1) {
      String var2 = var1.toString();
      ArrayList var3 = new ArrayList();

      for (MenuData.BountyEntry var5 : var0.bountyBoard) {
         if ("TAKEN".equals(var5.state) && var2.equals(var5.acceptedBy)) {
            var3.add(var5);
         }
      }

      return var3;
   }

   public static boolean completed(UUID var0, BountyCatalog.Bounty var1) {
      return var1 != null && progress(var0, var1.id()) >= var1.amount();
   }

   public static void pushHud(ServerPlayer var0) {
      if (var0 != null) {
         if (ServerPlayNetworking.canSend(var0, BountyHudPayload.TYPE)) {
            BountyHudPayload var1 = buildHud(var0);
            if (!var1.equals(LAST_HUD.put(var0.getUUID(), var1))) {
               ServerPlayNetworking.send(var0, var1);
            }
         }
      }
   }

   public static void pushHudAll(MinecraftServer var0) {
      if (var0 != null) {
         for (ServerPlayer var2 : var0.getPlayerList().getPlayers()) {
            pushHud(var2);
         }
      }
   }

   private static BountyHudPayload buildHud(ServerPlayer var0) {
      MenuData var1 = MenuData.get();
      String var2 = var0.getUUID().toString();
      ArrayList var3 = new ArrayList();

      for (MenuData.BountyEntry var5 : var1.bountyBoard) {
         if ("TAKEN".equals(var5.state) && var2.equals(var5.acceptedBy)) {
            BountyCatalog.Bounty var6 = BountyCatalog.byId(var5.bountyId);
            if (var6 != null) {
               long var7 = Math.min(progress(var0.getUUID(), var6.id()), var6.amount());
               var3.add(
                  new BountyHudPayload.Entry(
                     var5.boardId,
                     var6.title(),
                     var6.goalText(),
                     (int)var7,
                     (int)var6.amount(),
                     (int)var6.rewardCents(),
                     var7 >= var6.amount(),
                     var6.difficulty().ordinal()
                  )
               );
            }
         }
      }

      return new BountyHudPayload(List.copyOf(var3));
   }

   public static String statusText(MenuData.BountyEntry var0, UUID var1) {
      if (var0 == null) {
         return "可接取";
      } else if ("DONE".equals(var0.state)) {
         return "已完成";
      } else if ("FAILED".equals(var0.state)) {
         return "已失败";
      } else if (!"TAKEN".equals(var0.state)) {
         return "可接取";
      } else {
         return var1 != null && var1.toString().equals(var0.acceptedBy) ? "我的进行中" : "已被接取";
      }
   }

   public static String acceptedName(MenuData.BountyEntry var0, MinecraftServer var1) {
      if (var0 == null || var0.acceptedBy == null || var0.acceptedBy.isEmpty()) {
         return "";
      } else if (var0.acceptedByName != null && !var0.acceptedByName.isEmpty()) {
         return var0.acceptedByName;
      } else {
         if (var1 != null) {
            try {
               ServerPlayer var2 = var1.getPlayerList().getPlayer(UUID.fromString(var0.acceptedBy));
               if (var2 != null) {
                  var0.acceptedByName = var2.getGameProfile().name();
                  return var0.acceptedByName;
               }
            } catch (IllegalArgumentException var3) {
            }
         }

         return "未知玩家";
      }
   }

   public static String statusDetail(MenuData.BountyEntry var0, ServerPlayer var1) {
      UUID var2 = var1 == null ? null : var1.getUUID();
      String var3 = statusText(var0, var2);
      if (var0 != null && "TAKEN".equals(var0.state) && (var2 == null || !var2.toString().equals(var0.acceptedBy))) {
         String var4 = acceptedName(var0, var1 == null ? null : var1.level().getServer());
         if (!var4.isEmpty()) {
            return var3 + "（" + var4 + "）";
         }
      }

      return var3;
   }

   public static boolean available(MenuData.BountyEntry var0) {
      return var0 == null ? false : "OPEN".equals(var0.state) || "FAILED".equals(var0.state);
   }

   public static boolean accept(ServerPlayer var0, int var1) {
      MenuData var2 = MenuData.get();
      ensureBoard(var2);
      MenuData.BountyEntry var3 = entry(var2, var1);
      if (var3 == null) {
         fail(var0, "这个悬赏已经不存在了。");
         return false;
      } else {
         BountyCatalog.Bounty var4 = BountyCatalog.byId(var3.bountyId);
         if (var4 == null) {
            fail(var0, "这个悬赏已经不存在了。");
            return false;
         } else {
            String var5 = var0.getUUID().toString();
            if ("DONE".equals(var3.state)) {
               fail(var0, "这个悬赏已经完成了。");
               return false;
            } else if ("TAKEN".equals(var3.state)) {
               fail(var0, var5.equals(var3.acceptedBy) ? "你已经接取了这个悬赏。" : "这个悬赏已经被其他玩家接取了。");
               return false;
            } else {
               if (BountyCatalog.ADVANCEMENT.equals(var4.trigger()) && advancementDone(var0, var4.match())) {
                  fail(var0, "这个成就你已经完成过了，换一个悬赏吧。");
                  return false;
               }

               int var6 = var2.bountyDailyCount.getOrDefault(var5, 0);
               if (var6 >= 5) {
                  fail(var0, "今天最多接取 5 个悬赏，明天再来吧。");
                  return false;
               } else {
                  boolean var7 = var4.difficulty() == BountyCatalog.Difficulty.EXTREME;
                  int var8 = var2.bountyDailyExtreme.getOrDefault(var5, 0);
                  if (var7 && var8 >= DAILY_EXTREME_LIMIT) {
                     fail(var0, "极限悬赏每天最多接取 " + DAILY_EXTREME_LIMIT + " 个，明天再来吧。");
                     return false;
                  } else {
                     var3.acceptedBy = var5;
                     var3.acceptedByName = var0.getGameProfile().name();
                     var3.state = "TAKEN";
                     var2.bountyDailyCount.put(var5, var6 + 1);
                     if (var7) {
                        var2.bountyDailyExtreme.put(var5, var8 + 1);
                     }

                     var2.bountyProgress.remove(progressKey(var0.getUUID(), var3.bountyId));
                     var2.save();
                     var0.sendSystemMessage(
                        Component.literal("已接取悬赏 #" + var1 + "（" + var4.difficulty().label() + "）：" + var4.title()).withStyle(ChatFormatting.GREEN)
                     );
                     if (var7) {
                        var0.sendSystemMessage(Component.literal("极限悬赏：死亡即失败，当日接取次数不退还。").withStyle(ChatFormatting.RED));
                     }

                     pushHud(var0);
                     return true;
                  }
               }
            }
         }
      }
   }

   public static boolean abandon(ServerPlayer var0, int var1) {
      MenuData var2 = MenuData.get();
      MenuData.BountyEntry var3 = entry(var2, var1);
      if (var3 != null && isMine(var3, var0.getUUID())) {
         var3.acceptedBy = "";
         var3.acceptedByName = "";
         var3.state = "OPEN";
         var2.bountyProgress.remove(progressKey(var0.getUUID(), var3.bountyId));
         var2.save();
         var0.sendSystemMessage(Component.literal("已放弃悬赏 #" + var1 + "，任务已释放回悬赏板（当日接取次数不退还）。").withStyle(ChatFormatting.YELLOW));
         pushHud(var0);
         return true;
      } else {
         fail(var0, "这不是你接取的悬赏。");
         return false;
      }
   }

   public static boolean claim(ServerPlayer var0, int var1) {
      MenuData var2 = MenuData.get();
      MenuData.BountyEntry var3 = entry(var2, var1);
      if (var3 != null && isMine(var3, var0.getUUID())) {
         BountyCatalog.Bounty var4 = BountyCatalog.byId(var3.bountyId);
         if (var4 == null) {
            fail(var0, "这个悬赏已经不存在了。");
            return false;
         } else {
            long var5 = progress(var0.getUUID(), var4.id());
            if (var5 < var4.amount()) {
               fail(var0, "目标还没完成：" + var5 + " / " + var4.amount());
               return false;
            } else {
               var3.state = "DONE";
               var2.bountyProgress.put(progressKey(var0.getUUID(), var3.bountyId), var4.amount());
               Economy.add(var0, var4.rewardCents());
               var2.save();
               var0.sendSystemMessage(Component.literal("已领取悬赏 #" + var1 + " 奖励：" + Economy.format(var4.rewardCents()) + " 金币").withStyle(ChatFormatting.GOLD));
               pushHud(var0);
               return true;
            }
         }
      } else {
         fail(var0, "这不是你接取的悬赏。");
         return false;
      }
   }

   public static boolean submit(ServerPlayer var0, int var1) {
      MenuData var2 = MenuData.get();
      ensureBoard(var2);
      MenuData.BountyEntry var3 = entry(var2, var1);
      if (var3 != null && isMine(var3, var0.getUUID())) {
         BountyCatalog.Bounty var4 = BountyCatalog.byId(var3.bountyId);
         if (var4 == null) {
            fail(var0, "这个悬赏已经不存在了。");
            return false;
         } else if (!"submit_item".equals(var4.trigger())) {
            fail(var0, "这个悬赏不需要提交物品。");
            return false;
         } else {
            UUID var5 = var0.getUUID();
            long var6 = progress(var5, var4.id());
            if (var6 >= var4.amount()) {
               fail(var0, "物品已经交齐了，直接领取奖励即可。");
               return false;
            } else {
               Item var8 = MarketItems.item(var4.match());
               if (var8 == null) {
                  fail(var0, "这个悬赏的物品无法提交。");
                  return false;
               } else {
                  int var9 = (int)(var4.amount() - var6);
                  int var10 = MarketItems.count(var0, var8);
                  if (var10 < var9) {
                     fail(var0, "还差 " + (var9 - var10) + " 个" + MarketItems.name(var8) + "。");
                     return false;
                  } else {
                     int var11 = MarketItems.remove(var0, new ItemStack(var8), var9);
                     if (var11 < var9) {
                        fail(var0, "提交失败，物品数量不足。");
                        return false;
                     } else {
                        var2.bountyProgress.put(progressKey(var5, var4.id()), var4.amount());
                        var2.save();
                        var0.sendSystemMessage(
                           Component.literal("已提交 " + var11 + " 个" + MarketItems.name(var8) + "，悬赏 #" + var1 + " 可以领取奖励了。").withStyle(ChatFormatting.GREEN)
                        );
                        pushHud(var0);
                        return true;
                     }
                  }
               }
            }
         }
      } else {
         fail(var0, "这不是你接取的悬赏。");
         return false;
      }
   }

   public static void onBlockBreak(ServerPlayer var0, String var1) {
      advance(var0, "block_break", var1);
   }

   public static void onBlockPlace(ServerPlayer var0, String var1) {
      advance(var0, "block_place", var1);
   }

   public static void onKill(ServerPlayer var0, String var1) {
      advance(var0, "kill", var1);
      if (BountyCatalog.isBoss(var1)) {
         advance(var0, BountyCatalog.BOSS_KILL, var1);
      }
   }

   public static void onCraft(ServerPlayer var0, String var1, int var2) {
      advance(var0, "craft", var1, (long)var2);
   }

   public static void onSmelt(ServerPlayer var0, String var1) {
      advance(var0, "smelt", var1);
   }

   public static void onConsume(ServerPlayer var0) {
      advance(var0, "consume", "");
   }

   public static void onFish(ServerPlayer var0) {
      advance(var0, "fish", "");
   }

   public static void onEnchant(ServerPlayer var0) {
      advance(var0, "enchant", "");
   }

   public static void onBreed(ServerPlayer var0) {
      advance(var0, "breed", "");
   }

   public static void onTame(ServerPlayer var0) {
      advance(var0, "tame", "");
   }

   public static void onTrade(ServerPlayer var0) {
      advance(var0, "trade", "");
   }

   public static void onDamageDealt(ServerPlayer var0, double var1) {
      long var3 = Math.round(var1);
      if (var3 > 0L) {
         advance(var0, "damage_dealt", "", var3);
      }
   }

   public static void onDamageTaken(ServerPlayer var0, double var1) {
      long var3 = Math.round(var1);
      if (var3 > 0L) {
         advance(var0, "damage_taken", "", var3);
      }
   }

   public static void onXpGain(ServerPlayer var0, int var1) {
      if (var1 > 0) {
         advance(var0, "xp_gain", "", (long)var1);
      }
   }

   public static void onPickup(ServerPlayer var0, int var1) {
      if (var1 > 0) {
         advance(var0, "pickup", "", (long)var1);
      }
   }

   public static void onSleep(ServerPlayer var0) {
      advance(var0, "sleep", "");
   }

   public static void tickPlayTime(MinecraftServer var0) {
      if (var0 != null) {
         for (ServerPlayer var2 : var0.getPlayerList().getPlayers()) {
            UUID var3 = var2.getUUID();
            int var4 = PLAY_TICKS.getOrDefault(var3, 0) + 1;
            if (var4 < 1200) {
               PLAY_TICKS.put(var3, var4);
            } else {
               PLAY_TICKS.put(var3, var4 - 1200);
               advance(var2, "play_time", "", 1L);
            }
         }
      }
   }

   public static void tickTravel(MinecraftServer var0) {
      if (var0 != null) {
         for (ServerPlayer var2 : var0.getPlayerList().getPlayers()) {
            String var3 = var2.level().dimension().identifier().toString();
            BountyService.TravelState var4 = TRAVEL.get(var2.getUUID());
            if (var4 != null && var3.equals(var4.dim)) {
               double var5 = var2.getX() - var4.x;
               double var7 = var2.getY() - var4.y;
               double var9 = var2.getZ() - var4.z;
               var4.x = var2.getX();
               var4.y = var2.getY();
               var4.z = var2.getZ();
               double var11 = Math.sqrt(var5 * var5 + var7 * var7 + var9 * var9);
               if (!(var11 <= 0.0) && !(var11 > 64.0)) {
                  var4.pending += var11;
                  long var13 = (long)var4.pending;
                  if (var13 > 0L) {
                     var4.pending -= (double)var13;
                     advance(var2, "travel", "", var13);
                  }
               }
            } else {
               TRAVEL.put(var2.getUUID(), new BountyService.TravelState(var2.getX(), var2.getY(), var2.getZ(), var3));
            }
         }
      }
   }

   public static void onDeath(ServerPlayer var0) {
      if (var0 != null) {
         MenuData var1 = MenuData.get();
         ensureBoard(var1);
         String var2 = var0.getUUID().toString();
         ArrayList var3 = new ArrayList();
         boolean var4 = false;

         for (MenuData.BountyEntry var5 : var1.bountyBoard) {
            if ("TAKEN".equals(var5.state) && var2.equals(var5.acceptedBy)) {
               BountyCatalog.Bounty var6 = BountyCatalog.byId(var5.bountyId);
               if (var6 != null && var6.difficulty() == BountyCatalog.Difficulty.EXTREME) {
                  if (BountyCatalog.ADVANCEMENT.equals(var6.trigger()) && advancementDone(var0, var6.match())) {
                     var1.bountyProgress.put(progressKey(var0.getUUID(), var5.bountyId), var6.amount());
                     var4 = true;
                  } else {
                     var5.state = "FAILED";
                     var5.acceptedBy = "";
                     var5.acceptedByName = "";
                     var1.bountyProgress.remove(progressKey(var0.getUUID(), var5.bountyId));
                     var3.add(var5.boardId);
                  }
               }
            }
         }

         if (!var3.isEmpty() || var4) {
            var1.save();
         }

         if (!var3.isEmpty()) {
            for (int var8 : (Iterable<Integer>) var3) {
               var0.sendSystemMessage(Component.literal("你在极限悬赏 #" + var8 + " 中死亡，任务已失败并释放回悬赏板（当日接取次数不退还）。").withStyle(ChatFormatting.RED));
            }
         }
      }
   }

   private static void advance(ServerPlayer var0, String var1, String var2) {
      advance(var0, var1, var2, 1L);
   }

   private static void advance(ServerPlayer var0, String var1, String var2, long var3) {
      if (var0 != null && var3 > 0L) {
         MenuData var5 = MenuData.get();
         ensureBoard(var5);
         String var6 = var0.getUUID().toString();
         boolean var7 = false;

         for (MenuData.BountyEntry var9 : var5.bountyBoard) {
            if ("TAKEN".equals(var9.state) && var6.equals(var9.acceptedBy)) {
               BountyCatalog.Bounty var10 = BountyCatalog.byId(var9.bountyId);
               if (var10 != null && var1.equals(var10.trigger()) && (var10.match().isEmpty() || var10.match().equals(var2))) {
                  String var11 = progressKey(var0.getUUID(), var10.id());
                  long var12 = var5.bountyProgress.getOrDefault(var11, 0L);
                  if (var12 < var10.amount()) {
                     long var14 = Math.min(var10.amount(), var12 + var3);
                     if (var14 != var12) {
                        var5.bountyProgress.put(var11, var14);
                        var7 = true;
                     }
                  }
               }
            }
         }

         if (var7) {
            dirty = true;
         }
      }
   }

   private static void rollBoard(MenuData var0, String var1) {
      Random var2 = new Random(daySeed(var1));
      int var3 = 5 + var2.nextInt(11);
      int var4 = 56 - var3;
      int[] var5 = allocate(var4, new int[]{16, 29, 55});
      int[] var6 = new int[]{var5[2], var5[1], var5[0], var3};
      ArrayList var7 = new ArrayList(56);
      BountyCatalog.Difficulty[] var8 = BountyCatalog.Difficulty.values();

      for (BountyCatalog.Difficulty var12 : var8) {
         int var13 = Math.min(var6[var12.ordinal()], countOf(var12));
         List var14 = BountyCatalog.of(var12);
         Collections.shuffle(var14, new Random(daySeed(var1) + 1013L * (long)(var12.ordinal() + 1)));

         for (int var15 = 0; var15 < var13; var15++) {
            var7.add(((BountyCatalog.Bounty)var14.get(var15)).id());
         }
      }

      Collections.shuffle(var7, new Random(daySeed(var1) ^ 24301L));
      var0.bountyBoard.clear();

      for (int var16 = 0; var16 < var7.size(); var16++) {
         MenuData.BountyEntry var17 = new MenuData.BountyEntry();
         var17.boardId = var16 + 1;
         var17.bountyId = (Integer)var7.get(var16);
         var17.acceptedBy = "";
         var17.state = "OPEN";
         var0.bountyBoard.add(var17);
      }

      var0.bountyDay = var1;
      var0.bountyProgress.clear();
      var0.bountyDailyCount.clear();
      var0.bountyDailyExtreme.clear();
      var0.save();
   }

   private static int countOf(BountyCatalog.Difficulty var0) {
      return BountyCatalog.of(var0).size();
   }

   private static int[] allocate(int var0, int[] var1) {
      int var2 = var1.length;
      int[] var3 = new int[var2];
      double[] var4 = new double[var2];
      int var5 = 0;

      for (int var9 : var1) {
         var5 += var9;
      }

      int var10 = 0;

      for (int var11 = 0; var11 < var2; var11++) {
         double var14 = (double)var0 * (double)var1[var11] / (double)var5;
         var3[var11] = Math.max(1, (int)Math.floor(var14));
         var4[var11] = var14 - Math.floor(var14);
         var10 += var3[var11];
      }

      while (var10 > var0) {
         int var12 = -1;

         for (int var15 = 0; var15 < var2; var15++) {
            if (var3[var15] > 1 && (var12 < 0 || var3[var15] > var3[var12] || var3[var15] == var3[var12] && var4[var15] < var4[var12])) {
               var12 = var15;
            }
         }

         if (var12 < 0) {
            break;
         }

         var3[var12]--;
         var10--;
      }

      while (var10 < var0) {
         int var13 = 0;

         for (int var16 = 1; var16 < var2; var16++) {
            if (var4[var16] > var4[var13]) {
               var13 = var16;
            }
         }

         var3[var13]++;
         var4[var13]--;
         var10++;
      }

      return var3;
   }

   private static long daySeed(String var0) {
      return (long)var0.hashCode() * 31L + 20260101L;
   }

   private static String progressKey(UUID var0, int var1) {
      return var0 + "|" + var1;
   }

   private static void fail(ServerPlayer var0, String var1) {
      var0.sendSystemMessage(Component.literal(var1).withStyle(ChatFormatting.RED));
   }

   private static String todayText() {
      return BOARD_VERSION + "|" + LocalDate.now().format(DAY_FORMAT);
   }

   private static final class TravelState {
      private double x;
      private double y;
      private double z;
      private final String dim;
      private double pending;

      private TravelState(double var1, double var3, double var5, String var7) {
         this.x = var1;
         this.y = var3;
         this.z = var5;
         this.dim = var7;
      }
   }
}
