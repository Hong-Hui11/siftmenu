package com.echoes.siftmenu;

import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import dev.ftb.mods.ftbteams.net.SyncTeamsMessage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/// 部分客户端登录时收不到 FTB Teams 的同步包，这里在进服 3 秒 / 12 秒各补发一次。
public final class TeamSyncFixer {
   private static final Map<UUID, Integer> PENDING = new HashMap<>();
   private static final Set<UUID> SENT_ONCE = new HashSet<>();

   private TeamSyncFixer() {
   }

   public static void onJoin(ServerPlayer player) {
      PENDING.put(player.getUUID(), 60);
      SENT_ONCE.remove(player.getUUID());
   }

   public static void onLeave(ServerPlayer player) {
      PENDING.remove(player.getUUID());
      SENT_ONCE.remove(player.getUUID());
   }

   public static void tick(MinecraftServer server) {
      if (PENDING.isEmpty()) {
         return;
      }

      Iterator<Map.Entry<UUID, Integer>> iterator = PENDING.entrySet().iterator();
      while (iterator.hasNext()) {
         Map.Entry<UUID, Integer> entry = iterator.next();
         int left = entry.getValue() - 1;
         if (left > 0) {
            entry.setValue(left);
            continue;
         }

         ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
         if (player == null) {
            iterator.remove();
            SENT_ONCE.remove(entry.getKey());
            continue;
         }

         resync(player);
         if (SENT_ONCE.add(entry.getKey())) {
            entry.setValue(180);
         } else {
            iterator.remove();
            SENT_ONCE.remove(entry.getKey());
         }
      }
   }

   public static void resyncNow(ServerPlayer player) {
      resync(player);
   }

   private static void resync(ServerPlayer player) {
      try {
         TeamManagerImpl manager = (TeamManagerImpl) FTBTeamsAPI.api().getManager();
         Team team = manager.getTeamForPlayer(player).orElse(null);
         if (team == null) {
            SiftMenu.LOGGER.warn("[siftmenu] {} 没有 FTB Teams 队伍数据，跳过同步包补发", player.getGameProfile().name());
            return;
         }

         ClientTeamManagerImpl data = ClientTeamManagerImpl.forSyncing(manager, manager.getTeamMap().values());
         boolean canSend = ServerPlayNetworking.canSend(player, SyncTeamsMessage.TYPE);
         SiftMenu.LOGGER.info(
            "[siftmenu] 向 {} 补发 FTB Teams 同步包：通道可用={}，队伍数={}",
            player.getGameProfile().name(),
            canSend,
            manager.getTeamMap().size()
         );
         if (!canSend) {
            return;
         }

         Server2PlayNetworking.send(player, new SyncTeamsMessage(data.setSelfTeamId(team.getId()), team.getId(), true));
         SiftMenu.LOGGER.info("[siftmenu] 已向 {} 补发 FTB Teams 同步包", player.getGameProfile().name());
      } catch (Throwable var3) {
         SiftMenu.LOGGER.warn("[siftmenu] 补发 FTB Teams 同步包失败", var3);
      }
   }
}
