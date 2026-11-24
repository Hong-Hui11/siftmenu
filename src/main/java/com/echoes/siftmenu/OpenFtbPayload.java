package com.echoes.siftmenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.Identifier;

/// 服务端通知客户端打开某个 FTB 界面（teams = FTB 团队）。
public record OpenFtbPayload(String target) implements CustomPacketPayload {
   public static final Type<OpenFtbPayload> TYPE = new Type(Identifier.fromNamespaceAndPath("siftmenu", "open_ftb"));
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenFtbPayload> CODEC = StreamCodec.of(
      (var0, var1) -> var0.writeUtf(var1.target(), 32),
      var0 -> new OpenFtbPayload(var0.readUtf(32))
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
