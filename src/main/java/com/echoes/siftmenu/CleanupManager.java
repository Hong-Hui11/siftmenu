package com.echoes.siftmenu;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.AllowLoad;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.Load;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class CleanupManager {
   private static final long PERIOD_SECONDS = 1800L;
   private static final long WARN_BEFORE_SECONDS = 10L;
   private static final long CLEANUP_WINDOW_SECONDS = 5L;
   private static final int MIN_AGE_TICKS = 600;
   private static long lastWarnSlot = Long.MIN_VALUE;
   private static long lastCleanupSlot = Long.MIN_VALUE;

   private CleanupManager() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register(CleanupManager::tick);
      ServerEntityEvents.ALLOW_LOAD.register((AllowLoad)(var0, var1, var2, var3) -> {
         if (!var3) {
            return true;
         } else {
            return var0 instanceof ItemEntity var4 ? var4.getAge() < 600 : true;
         }
      });
      ServerEntityEvents.ENTITY_LOAD.register((Load)(var0, var1) -> {
         if (var0 instanceof ItemEntity var2) {
            if (var2.getAge() >= 600) {
               MinecraftServer var3 = var1.getServer();
               if (var3 != null) {
                  var3.execute(() -> {
                     if (!var2.isRemoved() && var2.getAge() >= 600) {
                        var2.discard();
                     }
                  });
               }
            }
         }
      });
   }

   private static void tick(MinecraftServer var0) {
      long var1 = System.currentTimeMillis() / 1000L;
      long var3 = Math.floorDiv(var1, 1800L);
      long var5 = Math.floorMod(var1, 1800L);
      if (var5 >= 1790L && lastWarnSlot != var3) {
         lastWarnSlot = var3;
         broadcast(var0, Component.literal("[清理] 服务器将在 10 秒后清理掉落物，请及时捡起重要物品！").withStyle(ChatFormatting.YELLOW));
      }

      if (var5 < 5L && lastCleanupSlot != var3) {
         lastCleanupSlot = var3;
         int var7 = cleanup(var0);
         broadcast(var0, Component.literal("[清理] 已清理 " + var7 + " 个掉落物。").withStyle(ChatFormatting.GREEN));
      }
   }

   private static int cleanup(MinecraftServer var0) {
      int var1 = 0;

      for (ServerLevel var3 : var0.getAllLevels()) {
         try {
            ArrayList var4 = new ArrayList();
            var3.getAllEntities().forEach(var4::add);

            for (Entity var6 : (List<Entity>) var4) {
               if (var6 instanceof ItemEntity) {
                  ItemEntity var7 = (ItemEntity)var6;
                  if (!var7.isRemoved() && var7.getAge() >= 600) {
                     var7.discard();
                     var1++;
                  }
               }
            }
         } catch (RuntimeException var8) {
            SiftMenu.LOGGER.warn("[清理] 清理 {} 的掉落物时出错，已跳过本次", var3.dimension().identifier(), var8);
         }
      }

      return var1;
   }

   private static void broadcast(MinecraftServer var0, Component var1) {
      if (var0.getPlayerList().getPlayerCount() > 0) {
         var0.getPlayerList().broadcastSystemMessage(var1, false);
      }
   }
}
