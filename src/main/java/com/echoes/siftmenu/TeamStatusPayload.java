package com.echoes.siftmenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

/// 客户端 -> 服务端：上报本地 FTB Teams 队伍数据是否已同步（stage 1 = 5 秒，2 = 30 秒）。
public record TeamStatusPayload(int stage, boolean synced) implements CustomPacketPayload {
   public static final Type<TeamStatusPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("siftmenu", "team_status"));
   public static final StreamCodec<RegistryFriendlyByteBuf, TeamStatusPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, TeamStatusPayload::stage, ByteBufCodecs.BOOL, TeamStatusPayload::synced, TeamStatusPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
