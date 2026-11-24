package com.echoes.siftmenu;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TeleportRequestManager {
   private static final long TTL_MS = 60000L;
   private static final String LOBBY_DIM = "sift:lobby";
   private static final Map<UUID, TeleportRequestManager.Request> PENDING = new HashMap<>();

   private TeleportRequestManager() {
   }

   public static void request(ServerPlayer var0, ServerPlayer var1, boolean var2) {
      if (var1 == var0) {
         msg(var0, "不能对自己发起传送请求。", ChatFormatting.RED);
      } else {
         TeleportRequestManager.Request var3 = new TeleportRequestManager.Request();
         var3.from = var0.getUUID();
         var3.fromName = var0.getGameProfile().name();
         var3.bringTarget = var2;
         var3.expireAt = System.currentTimeMillis() + 60000L;
         PENDING.put(var1.getUUID(), var3);
         var0.closeContainer();
         MutableComponent var4 = Component.literal("[✓]")
            .withStyle(
               var1x -> var1x.withColor(ChatFormatting.GREEN)
                     .withBold(true)
                     .withClickEvent(new RunCommand("/siftmenu teleport accept " + var3.fromName))
                     .withHoverEvent(new ShowText(Component.literal("同意传送")))
            );
         MutableComponent var5 = Component.literal("[✕]")
            .withStyle(
               var1x -> var1x.withColor(ChatFormatting.RED)
                     .withBold(true)
                     .withClickEvent(new RunCommand("/siftmenu teleport deny " + var3.fromName))
                     .withHoverEvent(new ShowText(Component.literal("拒绝传送")))
            );
         String var6 = var2 ? var3.fromName + " 邀请你传送到他那里（60 秒内有效）" : var3.fromName + " 请求传送到你这里（60 秒内有效）";
         var1.sendSystemMessage(Component.literal(var6).withStyle(ChatFormatting.GOLD).append(var4).append(Component.literal(" ")).append(var5));
         msg(var0, "已向 " + var1.getGameProfile().name() + (var2 ? " 发送邀请，等待对方同意。" : " 发送传送请求，等待对方同意。"), ChatFormatting.GREEN);
      }
   }

   public static void accept(ServerPlayer var0, String var1) {
      TeleportRequestManager.Request var2 = take(var0, var1);
      if (var2 != null) {
         ServerPlayer var3 = var0.level().getServer().getPlayerList().getPlayer(var2.from);
         if (var3 == null) {
            msg(var0, "对方已离线，传送请求失效。", ChatFormatting.RED);
         } else {
            ServerPlayer var4 = var2.bringTarget ? var0 : var3;
            ServerPlayer var5 = var2.bringTarget ? var3 : var0;

            try {
               var4.closeContainer();
               // 被玩家传送到大厅时补记离开前的坐标，否则玩家没有 lobby_id，无法从大厅出去
               if (LOBBY_DIM.equals(var5.level().dimension().identifier().toString())
                  && !LOBBY_DIM.equals(var4.level().dimension().identifier().toString())) {
                  recordLobbyReturn(var4);
               }

               var4.teleportTo(var5.level(), var5.getX(), var5.getY(), var5.getZ(), Set.of(), var4.getYRot(), var4.getXRot(), true);
            } catch (Exception var7) {
               msg(var4, "传送失败：" + var7.getMessage(), ChatFormatting.RED);
               return;
            }

            msg(var4, "已传送到 " + var5.getGameProfile().name() + " 那里。", ChatFormatting.GREEN);
            msg(var5, var4.getGameProfile().name() + " 已接受传送请求。", ChatFormatting.GREEN);
            MenuManager.markPlayerTeleportDone(var4);
         }
      }
   }

   public static void deny(ServerPlayer var0, String var1) {
      TeleportRequestManager.Request var2 = take(var0, var1);
      if (var2 != null) {
         msg(var0, "已拒绝 " + var2.fromName + " 的传送请求。", ChatFormatting.GRAY);
         ServerPlayer var3 = var0.level().getServer().getPlayerList().getPlayer(var2.from);
         if (var3 != null) {
            msg(var3, var0.getGameProfile().name() + " 拒绝了你的传送请求。", ChatFormatting.GRAY);
         }
      }
   }

   private static TeleportRequestManager.Request take(ServerPlayer var0, String var1) {
      TeleportRequestManager.Request var2 = PENDING.get(var0.getUUID());
      if (var2 == null) {
         msg(var0, "没有待处理的传送请求。", ChatFormatting.RED);
         return null;
      } else if (System.currentTimeMillis() > var2.expireAt) {
         PENDING.remove(var0.getUUID());
         msg(var0, "传送请求已过期。", ChatFormatting.RED);
         return null;
      } else if (var1 != null && !var2.fromName.equalsIgnoreCase(var1)) {
         msg(var0, "没有来自该玩家的传送请求。", ChatFormatting.RED);
         return null;
      } else {
         PENDING.remove(var0.getUUID());
         return var2;
      }
   }

   private static void msg(ServerPlayer var0, String var1, ChatFormatting var2) {
      var0.sendSystemMessage(Component.literal(var1).withStyle(var2));
   }

   /// 让数据包把该玩家当前所在的坐标记到大厅返回点（对应 sift:lobby/record）
   private static void recordLobbyReturn(ServerPlayer var0) {
      MinecraftServer var1 = var0.level().getServer();
      if (var1 != null) {
         var1.getCommands()
            .performPrefixedCommand(
               var1.createCommandSourceStack().withSuppressedOutput(),
               "execute as " + var0.getGameProfile().name() + " run function sift:lobby/record"
            );
      }
   }

   private static final class Request {
      UUID from;
      String fromName;
      boolean bringTarget;
      long expireAt;
   }
}
