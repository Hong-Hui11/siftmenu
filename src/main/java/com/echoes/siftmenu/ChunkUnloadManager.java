package com.echoes.siftmenu;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.Load;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.Unload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;

public final class ChunkUnloadManager {
   private static final int CHECK_INTERVAL = 5;
   private static final int MARGIN = 1;
   private static final Map<String, LongSet> TRACKED = new HashMap<>();
   private static Field ticketStorageField;
   private static int counter;
   private static int lastPurged;

   private ChunkUnloadManager() {
   }

   public static void register() {
      ServerChunkEvents.CHUNK_LOAD.register((Load)(var0, var1, var2) -> tracked(var0).add(var1.getPos().pack()));
      ServerChunkEvents.CHUNK_UNLOAD.register((Unload)(var0, var1) -> tracked(var0).remove(var1.getPos().pack()));
      ServerTickEvents.END_SERVER_TICK.register(ChunkUnloadManager::tick);
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(var0, var1, var2) -> var0.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("chunkunload")
                           .then(
                              Commands.literal("status")
                                 .executes(
                                    var0x -> {
                                       MinecraftServer var1x = ((CommandSourceStack)var0x.getSource()).getServer();
                                       ServerPlayer var2x = ((CommandSourceStack)var0x.getSource()).getPlayerOrException();
                                       int var3 = viewDistance(var1x);
                                       int var4 = 0;

                                       for (LongSet var6 : TRACKED.values()) {
                                          var4 += var6.size();
                                       }

                                       var2x.sendSystemMessage(
                                          Component.literal("[强制卸载] 视距 " + var3 + " 区块，追踪中 " + var4 + " 个区块，上次清理 " + lastPurged + " 个")
                                             .withStyle(ChatFormatting.AQUA)
                                       );
                                       return 1;
                                    }
                                 )
                           ))
                        .then(
                           Commands.literal("check")
                              .executes(
                                 var0x -> {
                                    runCheck(((CommandSourceStack)var0x.getSource()).getServer());
                                    ((CommandSourceStack)var0x.getSource())
                                       .sendSuccess(() -> Component.literal("已执行一次强制卸载，清理 " + lastPurged + " 个区块").withStyle(ChatFormatting.GREEN), false);
                                    return 1;
                                 }
                              )
                        ))
                     .then(Commands.literal("view").then(Commands.argument("chunks", IntegerArgumentType.integer(2, 32)).executes(var0x -> {
                        int var1x = IntegerArgumentType.getInteger(var0x, "chunks");
                        ((CommandSourceStack)var0x.getSource()).getServer().getPlayerList().setViewDistance(var1x);
                        ((CommandSourceStack)var0x.getSource())
                           .sendSuccess(() -> Component.literal("服务器视距已设为 " + var1x + " 区块").withStyle(ChatFormatting.GREEN), true);
                        return 1;
                     })))
               )
         );
   }

   private static LongSet tracked(ServerLevel var0) {
      return TRACKED.computeIfAbsent(var0.dimension().identifier().toString(), var0x -> new LongOpenHashSet());
   }

   private static void tick(MinecraftServer var0) {
      if (++counter >= 5) {
         counter = 0;
         runCheck(var0);
      }
   }

   private static void runCheck(MinecraftServer var0) {
      int var1 = viewDistance(var0) + 1;
      int var2 = 0;

      for (ServerLevel var4 : var0.getAllLevels()) {
         var2 += purgeForced(var4);
         var2 += purgeFar(var4, var1);
      }

      lastPurged = var2;
   }

   private static int viewDistance(MinecraftServer var0) {
      return Math.max(2, var0.getPlayerList().getViewDistance());
   }

   private static int purgeForced(ServerLevel var0) {
      LongSet var1 = var0.getForceLoadedChunks();
      if (var1.isEmpty()) {
         return 0;
      } else {
         int var2 = 0;
         LongIterator var3 = new LongOpenHashSet(var1).iterator();

         while (var3.hasNext()) {
            long var4 = var3.nextLong();

            if (var0.setChunkForced(ChunkPos.getX(var4), ChunkPos.getZ(var4), false)) {
               var2++;
            }
         }

         return var2;
      }
   }

   private static int purgeFar(ServerLevel var0, int var1) {
      LongSet var2 = TRACKED.get(var0.dimension().identifier().toString());
      if (var2 != null && !var2.isEmpty()) {
         TicketStorage var3 = ticketStorage(var0);
         if (var3 == null) {
            return 0;
         } else {
            ArrayList var4 = new ArrayList();

            for (ServerPlayer var6 : var0.players()) {
               var4.add(var6.chunkPosition());
            }

            int var15 = 0;

            for (long var9 : var2.toLongArray()) {
               ChunkPos var11 = ChunkPos.unpack(var9);
               if (!nearAny(var11, var4, var1)) {
                  List var12 = var3.getTickets(var9);
                  if (var12 != null && !var12.isEmpty()) {
                     for (Object var13 : new ArrayList<>(var12)) {
                        Ticket var14 = (Ticket) var13;
                        var3.removeTicket(var14, var11);
                     }

                     var15++;
                  }
               }
            }

            return var15;
         }
      } else {
         return 0;
      }
   }

   private static boolean nearAny(ChunkPos var0, List<ChunkPos> var1, int var2) {
      for (ChunkPos var4 : var1) {
         int var5 = Math.abs(var0.x() - var4.x());
         int var6 = Math.abs(var0.z() - var4.z());
         if (Math.max(var5, var6) <= var2) {
            return true;
         }
      }

      return false;
   }

   private static TicketStorage ticketStorage(ServerLevel var0) {
      ServerChunkCache var1 = var0.getChunkSource();

      try {
         if (ticketStorageField == null) {
            Field var2 = ServerChunkCache.class.getDeclaredField("ticketStorage");
            var2.setAccessible(true);
            ticketStorageField = var2;
         }

         return (TicketStorage)ticketStorageField.get(var1);
      } catch (RuntimeException | ReflectiveOperationException var3) {
         return (TicketStorage)var0.getDataStorage().get(TicketStorage.TYPE);
      }
   }
}
