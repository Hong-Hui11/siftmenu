package com.echoes.siftmenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record BingoClaimResultPayload(boolean granted, String message) implements CustomPacketPayload {
   public static final Type<BingoClaimResultPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("siftmenu", "bingo_claim_result"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BingoClaimResultPayload> CODEC = StreamCodec.of((var0, var1) -> {
      var0.writeBoolean(var1.granted());
      var0.writeUtf(var1.message() == null ? "" : var1.message());
   }, var0 -> new BingoClaimResultPayload(var0.readBoolean(), var0.readUtf()));

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
