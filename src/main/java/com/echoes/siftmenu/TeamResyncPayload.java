package com.echoes.siftmenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

/// 客户端 -> 服务端：请求重发 FTB Teams 队伍同步包。
public record TeamResyncPayload() implements CustomPacketPayload {
   public static final Type<TeamResyncPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("siftmenu", "team_resync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, TeamResyncPayload> CODEC = StreamCodec.unit(new TeamResyncPayload());

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
