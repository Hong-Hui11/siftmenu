package com.echoes.siftmenu;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class SleepSkip {
   private static final int REQUIRED = 3;
   private static final int CHECK_INTERVAL = 20;

   private SleepSkip() {
   }

   public static void register() {
      ServerTickEvents.END_SERVER_TICK.register(SleepSkip::tick);
   }

   private static void tick(MinecraftServer var0) {
      if (var0.getTickCount() % 20 == 0) {
         int var1 = var0.getPlayerList().getPlayerCount();
         if (var1 > 0) {
            int var2 = Math.min(3, var1);
            int var3 = 0;
            ServerLevel var4 = null;

            for (ServerLevel var6 : var0.getAllLevels()) {
               boolean var7 = var6.canSleepThroughNights();

               for (ServerPlayer var9 : var6.players()) {
                  if (var9.isSleeping()) {
                     var3++;
                     if (var7) {
                        var4 = var6;
                     }
                  }
               }
            }

            if (var4 != null && var3 >= var2) {
               skip(var0, var4, var3);
            }
         }
      }
   }

   private static void skip(MinecraftServer var0, ServerLevel var1, int var2) {
      CommandSourceStack var3 = var0.createCommandSourceStack().withSuppressedOutput();
      var0.getCommands().performPrefixedCommand(var3, "time set day");
      if (var1.isThundering()) {
         var0.getCommands().performPrefixedCommand(var3, "weather clear");
      }

      for (ServerLevel var5 : var0.getAllLevels()) {
         for (ServerPlayer var7 : var5.players()) {
            if (var7.isSleeping()) {
               var7.stopSleepInBed(true, true);
            }
         }
      }

      var0.getPlayerList().broadcastSystemMessage(Component.literal("[睡眠] " + var2 + " 名玩家已入睡，夜晚跳过").withStyle(ChatFormatting.GOLD), false);
   }
}
