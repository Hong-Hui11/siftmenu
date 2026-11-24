package com.echoes.siftmenu.mixin.client;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({AnvilScreen.class})
public abstract class AnvilScreenMixin {
   @Redirect(
      method = {"extractLabels"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"
      )
   )
   private boolean siftmenu$hideTooExpensiveLabel(LocalPlayer var1) {
      return true;
   }
}
