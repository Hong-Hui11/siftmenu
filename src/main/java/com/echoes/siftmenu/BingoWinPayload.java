package com.echoes.siftmenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record BingoWinPayload(String token) implements CustomPacketPayload {
   public static final Type<BingoWinPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("siftmenu", "bingo_win"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BingoWinPayload> CODEC = StreamCodec.of(
      (var0, var1) -> var0.writeUtf(var1.token()), var0 -> new BingoWinPayload(var0.readUtf())
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
