package com.echoes.siftmenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record BingoClaimPayload(String token) implements CustomPacketPayload {
   public static final Type<BingoClaimPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("siftmenu", "bingo_claim"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BingoClaimPayload> CODEC = StreamCodec.of(
      (var0, var1) -> var0.writeUtf(var1.token()), var0 -> new BingoClaimPayload(var0.readUtf())
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
