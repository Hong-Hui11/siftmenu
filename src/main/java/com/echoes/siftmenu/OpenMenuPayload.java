package com.echoes.siftmenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

public record OpenMenuPayload() implements CustomPacketPayload {
   public static final Type<OpenMenuPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("siftmenu", "open"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenMenuPayload> CODEC = StreamCodec.unit(new OpenMenuPayload());

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
