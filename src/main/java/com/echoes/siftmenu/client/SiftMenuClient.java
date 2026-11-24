package com.echoes.siftmenu.client;

import com.echoes.siftmenu.BingoClaimPayload;
import com.echoes.siftmenu.BingoClaimResultPayload;
import com.echoes.siftmenu.BingoWinPayload;
import com.echoes.siftmenu.BountyHudPayload;
import com.echoes.siftmenu.OpenMenuPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.resources.Identifier;

public final class SiftMenuClient implements ClientModInitializer {
   private static final Category CATEGORY = Category.register(Identifier.fromNamespaceAndPath("siftmenu", "main"));
   private static boolean claimSent;

   public void onInitializeClient() {
      BountyHudPanel.register();
      ClientPlayConnectionEvents.JOIN.register((Join)(var0, var1x, var2) -> {
         claimSent = false;
         BountyHudPanel.reset();
         TeamClientDiag.onJoin();
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(var0, var1x) -> {
         claimSent = false;
         BountyHudPanel.reset();
         TeamClientDiag.onDisconnect();
      });
      ClientPlayNetworking.registerGlobalReceiver(
         BountyHudPayload.TYPE, (var0, var1x) -> var1x.client().execute(() -> BountyHudPanel.setEntries(var0.entries()))
      );
      ClientPlayNetworking.registerGlobalReceiver(BingoWinPayload.TYPE, (var0, var1x) -> var1x.client().execute(() -> BingoClaimStore.save(var0.token())));
      ClientPlayNetworking.registerGlobalReceiver(BingoClaimResultPayload.TYPE, (var0, var1x) -> var1x.client().execute(BingoClaimStore::clear));
      ClientPlayNetworking.registerGlobalReceiver(
         com.echoes.siftmenu.OpenFtbPayload.TYPE,
         (var0, var1x) -> var1x.client().execute(() -> {
            String var2 = var0.target();
            if ("teams".equals(var2)) {
               dev.ftb.mods.ftbteams.net.OpenGUIMessage.sendToServer();
            }
         })
      );
      KeyMapping var1 = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.siftmenu.open", 71, CATEGORY));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)var1x -> {
         TeamClientDiag.tick(var1x);
         if (!claimSent && var1x.player != null) {
            String var2 = BingoClaimStore.token();
            if (var2 != null && !var2.isEmpty() && ClientPlayNetworking.canSend(BingoClaimPayload.TYPE)) {
               ClientPlayNetworking.send(new BingoClaimPayload(var2));
               claimSent = true;
            }
         }

         boolean var3 = InputConstants.isKeyDown(292);

         while (var1.consumeClick()) {
            if (!var3 && var1x.player != null) {
               ClientPlayNetworking.send(new OpenMenuPayload());
            }
         }
      });
   }
}
