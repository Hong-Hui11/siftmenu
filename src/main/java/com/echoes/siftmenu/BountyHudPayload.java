package com.echoes.siftmenu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record BountyHudPayload(List<BountyHudPayload.Entry> entries) implements CustomPacketPayload {
   public static final Type<BountyHudPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("siftmenu", "bounty_hud"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BountyHudPayload> CODEC = StreamCodec.of(
      (var0, var1) -> {
         var0.writeVarInt(var1.entries().size());

         for (BountyHudPayload.Entry var3 : var1.entries()) {
            var0.writeVarInt(var3.boardId());
            var0.writeUtf(var3.title(), 64);
            var0.writeUtf(var3.goal(), 128);
            var0.writeVarInt(var3.progress());
            var0.writeVarInt(var3.amount());
            var0.writeVarInt(var3.rewardCents());
            var0.writeBoolean(var3.done());
            var0.writeVarInt(var3.difficulty());
         }
      },
      var0 -> {
         int var1 = var0.readVarInt();
         ArrayList var2 = new ArrayList(var1);

         for (int var3 = 0; var3 < var1; var3++) {
            var2.add(
               new BountyHudPayload.Entry(
                  var0.readVarInt(),
                  var0.readUtf(64),
                  var0.readUtf(128),
                  var0.readVarInt(),
                  var0.readVarInt(),
                  var0.readVarInt(),
                  var0.readBoolean(),
                  var0.readVarInt()
               )
            );
         }

         return new BountyHudPayload(List.copyOf(var2));
      }
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static record Entry(int boardId, String title, String goal, int progress, int amount, int rewardCents, boolean done, int difficulty) {
   }
}
