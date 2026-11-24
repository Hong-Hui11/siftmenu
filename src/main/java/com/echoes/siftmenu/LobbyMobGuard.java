package com.echoes.siftmenu;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/// 主城大厅维度里禁止自然生成生物/怪物；已命名或持久化的实体保留，方便以后放大厅 NPC。
public final class LobbyMobGuard {
   private static final String LOBBY_DIM = "sift:lobby";

   private LobbyMobGuard() {
   }

   public static void register() {
      ServerEntityEvents.ENTITY_LOAD.register(LobbyMobGuard::onEntityLoad);
   }

   private static void onEntityLoad(Entity entity, ServerLevel level) {
      if (!LOBBY_DIM.equals(level.dimension().identifier().toString())) {
         return;
      }

      if (entity instanceof Player) {
         return;
      }

      if (!(entity instanceof Mob mob)) {
         return;
      }

      if (mob.isPersistenceRequired() || mob.hasCustomName()) {
         return;
      }

      mob.discard();
   }
}
