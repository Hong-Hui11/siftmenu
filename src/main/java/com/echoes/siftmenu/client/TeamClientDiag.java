package com.echoes.siftmenu.client;

import com.echoes.siftmenu.SiftMenu;
import com.echoes.siftmenu.TeamResyncPayload;
import com.echoes.siftmenu.TeamStatusPayload;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/// 客户端自检：进服 5 秒 / 30 秒各检查一次 FTB Teams 队伍数据，未同步就请求服务器补发，并把结果上报服务器。
public final class TeamClientDiag {
   private static int stage;
   private static int countdown;

   private TeamClientDiag() {
   }

   public static void onJoin() {
      stage = 1;
      countdown = 100;
   }

   public static void onDisconnect() {
      stage = 0;
      countdown = 0;
   }

   public static void tick(Minecraft client) {
      if (stage == 0 || client.player == null) {
         return;
      }

      if (--countdown > 0) {
         return;
      }

      boolean synced = isSynced();
      report(client, stage, synced);
      if (!synced && ClientPlayNetworking.canSend(TeamResyncPayload.TYPE)) {
         ClientPlayNetworking.send(new TeamResyncPayload());
      }

      if (stage == 1) {
         stage = 2;
         countdown = 600;
      } else {
         stage = 0;
      }
   }

   private static boolean isSynced() {
      try {
         return FTBTeamsAPI.api().isClientManagerLoaded();
      } catch (Throwable var1) {
         return false;
      }
   }

   private static void report(Minecraft client, int stage, boolean synced) {
      if (ClientPlayNetworking.canSend(TeamStatusPayload.TYPE)) {
         ClientPlayNetworking.send(new TeamStatusPayload(stage, synced));
      }

      String text = stage == 1
         ? (synced ? "FTB 队伍数据已同步" : "未收到 FTB 队伍数据，已请求服务器补发")
         : (synced ? "FTB 队伍数据已同步（30 秒复查）" : "30 秒复查仍未同步 FTB 队伍数据");
      ChatFormatting color = synced ? ChatFormatting.GREEN : (stage == 1 ? ChatFormatting.YELLOW : ChatFormatting.RED);
      try {
         if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("[筛界] " + text).withStyle(color));
         }
      } catch (Throwable var6) {
         SiftMenu.LOGGER.warn("[siftmenu-client] 无法显示提示：{}", var6.toString());
      }

      SiftMenu.LOGGER.info("[siftmenu-client] 第 {} 次检查：{}", stage, text);
   }
}
