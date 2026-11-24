package com.echoes.siftmenu;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class BingoReward {
   public static final long AMOUNT = Money.fromCoins(1000L);
   private static final long WIN_MEMORY_MS = 1800000L;
   private static final long TOKEN_MAX_AGE_MS = 21600000L;
   private static final String SECRET = "sift-bingo-reward-2026-7f3c9a1e5d2b4c8a";
   private static final Map<UUID, BingoReward.Win> WINS = new HashMap<>();

   private BingoReward() {
   }

   public static void recordWin(ServerPlayer var0, String var1) {
      long var2 = System.currentTimeMillis();
      String var4 = sign(var0.getUUID(), var1, var2);
      WINS.put(var0.getUUID(), new BingoReward.Win(var4, var2));
      sendToken(var0, var4);
      var0.sendSystemMessage(Component.literal("你赢得了本局 Bingo！").withStyle(ChatFormatting.GOLD));
      var0.sendSystemMessage(
         Component.literal("[点击返回主服并领取 " + Economy.format(AMOUNT) + " 金币]")
            .withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withClickEvent(new RunCommand("/siftmenu bingowin")))
      );
   }

   public static boolean hasRecentWin(UUID var0) {
      BingoReward.Win var1 = WINS.get(var0);
      if (var1 == null) {
         return false;
      } else if (System.currentTimeMillis() - var1.time() > 1800000L) {
         WINS.remove(var0);
         return false;
      } else {
         return true;
      }
   }

   public static void returnToMain(ServerPlayer var0) {
      BingoReward.Win var1 = WINS.get(var0.getUUID());
      if (var1 != null) {
         sendToken(var0, var1.token());
      }

      var0.sendSystemMessage(Component.literal("正在返回主服领取 " + Economy.format(AMOUNT) + " 金币…").withStyle(ChatFormatting.GOLD));
      MenuManager.returnToMainServer(var0);
   }

   public static void handleClaim(ServerPlayer var0, String var1) {
      if (!MenuManager.isBingoServer()) {
         String[] var2 = verify(var1);
         if (var2 == null) {
            ack(var0, false, "Bingo 奖励凭证无效或已过期。");
         } else {
            UUID var3 = UUID.fromString(var2[0]);
            String var4 = var2[1];
            if (!var3.equals(var0.getUUID())) {
               ack(var0, false, "这张奖励凭证不属于你。");
            } else {
               MenuData var5 = MenuData.get();
               String var6 = var3 + "|" + var4;
               if (var5.isBingoClaimed(var6)) {
                  ack(var0, false, "本局 Bingo 奖励已经领取过了。");
               } else {
                  var5.markBingoClaimed(var6);
                  Economy.ensureAccount(var0);
                  Economy.add(var0, AMOUNT);
                  var5.save();
                  MinecraftServer var7 = var0.level().getServer();
                  String var8 = var0.getGameProfile().name();
                  var7.getPlayerList()
                     .broadcastSystemMessage(
                        Component.literal("★ " + var8 + " 在 Bingo 中获胜，获得 " + Economy.format(AMOUNT) + " 金币！").withStyle(ChatFormatting.GOLD), false
                     );
                  var0.sendSystemMessage(Component.literal("已领取 Bingo 胜利奖励：" + Economy.format(AMOUNT) + " 金币").withStyle(ChatFormatting.GREEN));
                  ack(var0, true, "");
               }
            }
         }
      }
   }

   private static void sendToken(ServerPlayer var0, String var1) {
      if (ServerPlayNetworking.canSend(var0, BingoWinPayload.TYPE)) {
         ServerPlayNetworking.send(var0, new BingoWinPayload(var1));
      }
   }

   private static void ack(ServerPlayer var0, boolean var1, String var2) {
      if (ServerPlayNetworking.canSend(var0, BingoClaimResultPayload.TYPE)) {
         ServerPlayNetworking.send(var0, new BingoClaimResultPayload(var1, var2));
      }

      if (var2 != null && !var2.isBlank()) {
         var0.sendSystemMessage(Component.literal(var2).withStyle(ChatFormatting.RED));
      }
   }

   private static String sign(UUID var0, String var1, long var2) {
      String var4 = var0 + "|" + var1 + "|" + var2;
      return encode(var4.getBytes(StandardCharsets.UTF_8)) + "." + hmac(var4);
   }

   private static String[] verify(String var0) {
      if (var0 == null) {
         return null;
      } else {
         int var1 = var0.lastIndexOf(46);
         if (var1 > 0 && var1 < var0.length() - 1) {
            String var2;
            try {
               var2 = new String(Base64.getUrlDecoder().decode(var0.substring(0, var1)), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException var12) {
               return null;
            }

            byte[] var3 = hmac(var2).getBytes(StandardCharsets.UTF_8);
            byte[] var4 = var0.substring(var1 + 1).getBytes(StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(var3, var4)) {
               return null;
            } else {
               String[] var5 = var2.split("\\|");
               if (var5.length != 3) {
                  return null;
               } else {
                  try {
                     UUID var6 = UUID.fromString(var5[0]);
                     long var7 = Long.parseLong(var5[2]);
                     long var9 = System.currentTimeMillis() - var7;
                     return var9 >= -60000L && var9 <= 21600000L ? new String[]{var6.toString(), var5[1]} : null;
                  } catch (IllegalArgumentException var11) {
                     return null;
                  }
               }
            }
         } else {
            return null;
         }
      }
   }

   private static String hmac(String var0) {
      try {
         Mac var1 = Mac.getInstance("HmacSHA256");
         var1.init(new SecretKeySpec("sift-bingo-reward-2026-7f3c9a1e5d2b4c8a".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
         return encode(var1.doFinal(var0.getBytes(StandardCharsets.UTF_8)));
      } catch (Exception var2) {
         throw new IllegalStateException("HMAC 初始化失败", var2);
      }
   }

   private static String encode(byte[] var0) {
      return Base64.getUrlEncoder().withoutPadding().encodeToString(var0);
   }

   private static record Win(String token, long time) {
   }
}
