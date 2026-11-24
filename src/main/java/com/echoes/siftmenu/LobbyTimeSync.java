package com.echoes.siftmenu;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.Level;

/// 主城大厅维度 sift:lobby 使用独立时钟 sift:lobby，并跟随服务器本地现实时间。
public final class LobbyTimeSync {
   private static final ResourceKey<Level> LOBBY_DIMENSION = ResourceKey.create(
      Registries.DIMENSION, Identifier.fromNamespaceAndPath("sift", "lobby")
   );
   private static final ResourceKey<WorldClock> LOBBY_CLOCK = ResourceKey.create(
      Registries.WORLD_CLOCK, Identifier.fromNamespaceAndPath("sift", "lobby")
   );

   private LobbyTimeSync() {
   }

   public static void tick(MinecraftServer server) {
      ServerLevel lobby = server.getLevel(LOBBY_DIMENSION);
      if (lobby == null) {
         return;
      }

      Registry<WorldClock> clocks = server.registryAccess().lookup(Registries.WORLD_CLOCK).orElse(null);
      if (clocks == null) {
         return;
      }

      Optional<Holder.Reference<WorldClock>> clock = clocks.get(LOBBY_CLOCK);
      if (clock.isEmpty()) {
         return;
      }

      // 现实时间秒数换算成 Minecraft 日间时间：0 tick = 06:00，6000 = 正午，18000 = 午夜
      long seconds = LocalTime.now(ZoneId.systemDefault()).toSecondOfDay();
      long ticks = Math.floorMod(seconds - 21600L, 86400L) * 24000L / 86400L;
      ServerClockManager manager = server.clockManager();
      manager.setPaused(clock.get(), true);
      manager.setTotalTicks(clock.get(), ticks);
   }
}
