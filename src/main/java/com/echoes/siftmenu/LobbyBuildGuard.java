package com.echoes.siftmenu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/// 主城大厅维度：只有管理员能放置 / 破坏方块，其他玩家只能与实体（NPC）交互。
public final class LobbyBuildGuard {
   private static final String LOBBY = "sift:lobby";
   private static final long NOTIFY_INTERVAL = 3000L;
   private static final Map<UUID, Long> LAST_NOTIFY = new HashMap<>();

   private LobbyBuildGuard() {
   }

   public static void register() {
      PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
         if (player instanceof ServerPlayer serverPlayer && isBlocked(serverPlayer)) {
            notify(serverPlayer, "主城大厅内只有管理员可以破坏方块。");
            return false;
         }

         return true;
      });

      UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
         if (world.isClientSide()) {
            return InteractionResult.PASS;
         }

         if (player instanceof ServerPlayer serverPlayer && isBlocked(serverPlayer)) {
            notify(serverPlayer, "主城大厅内只有管理员可以使用方块。");
            return InteractionResult.FAIL;
         }

         return InteractionResult.PASS;
      });
   }

   private static boolean isBlocked(ServerPlayer var0) {
      return LOBBY.equals(var0.level().dimension().identifier().toString()) && !Titles.isAdmin(var0);
   }

   private static void notify(ServerPlayer var0, String var1) {
      long var2 = System.currentTimeMillis();
      Long var4 = LAST_NOTIFY.get(var0.getUUID());
      if (var4 == null || var2 - var4 > NOTIFY_INTERVAL) {
         LAST_NOTIFY.put(var0.getUUID(), var2);
         var0.sendSystemMessage(Component.literal(var1).withStyle(ChatFormatting.RED));
      }
   }
}
