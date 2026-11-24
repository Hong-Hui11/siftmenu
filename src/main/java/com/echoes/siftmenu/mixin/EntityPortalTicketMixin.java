package com.echoes.siftmenu.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class EntityPortalTicketMixin {
   @Inject(
      method = {"placePortalTicket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void siftmenu$skipPortalTicketWithoutPlayers(BlockPos var1, CallbackInfo var2) {
      Entity var3 = ((Entity)(Object)this);
      if (var3.level() instanceof ServerLevel var4) {
         int var12 = var4.getServer().getPlayerList().getViewDistance();
         ChunkPos var6 = ChunkPos.containing(var1);

         for (ServerPlayer var8 : var4.players()) {
            ChunkPos var9 = var8.chunkPosition();
            int var10 = var9.x() - var6.x();
            int var11 = var9.z() - var6.z();
            if (var10 * var10 + var11 * var11 <= var12 * var12) {
               return;
            }
         }

         var2.cancel();
      }
   }
}
