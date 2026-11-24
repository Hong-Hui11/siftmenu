package com.echoes.siftmenu;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/// 主城大厅维度 sift:lobby 内不允许把重生点设在该维度：
/// 床、睡袋或命令设置的重生点会在 1 秒内被回滚到世界出生点。
public final class LobbySpawnGuard {
   private static final String LOBBY = "sift:lobby";

   private LobbySpawnGuard() {
   }

   public static void tick(MinecraftServer server) {
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         ServerPlayer.RespawnConfig config = player.getRespawnConfig();
         if (config == null || !LOBBY.equals(config.respawnData().dimension().identifier().toString())) {
            continue;
         }

         ServerLevel overworld = server.overworld();
         player.setRespawnPosition(new ServerPlayer.RespawnConfig(overworld.getRespawnData(), false), true);
         player.sendSystemMessage(Component.literal("主城大厅内不能设置重生点，已重置为世界出生点。").withStyle(ChatFormatting.RED));
      }
   }
}
