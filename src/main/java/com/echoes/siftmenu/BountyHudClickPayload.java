package com.echoes.siftmenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record BountyHudClickPayload(int boardId) implements CustomPacketPayload {
   public static final Type<BountyHudClickPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("siftmenu", "bounty_hud_click"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BountyHudClickPayload> CODEC = StreamCodec.of(
      (var0, var1) -> var0.writeVarInt(var1.boardId()), var0 -> new BountyHudClickPayload(var0.readVarInt())
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
