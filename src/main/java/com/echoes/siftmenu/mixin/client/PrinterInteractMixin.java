package com.echoes.siftmenu.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
   targets = {"me.aleksilassila.litematica.printer.implementation.actions.InteractActionImpl"},
   remap = false
)
public abstract class PrinterInteractMixin {
   @WrapMethod(
      method = {"interact"},
      require = 0
   )
   private void siftmenu$sneakWhileInteracting(Minecraft var1, LocalPlayer var2, InteractionHand var3, BlockHitResult var4, Operation<Void> var5) {
      if (var2 == null) {
         var5.call(new Object[]{var1, var2, var3, var4});
      } else {
         Input var6 = var2.input.keyPresses;
         boolean var7 = var6.shift();
         Input var8 = new Input(var6.forward(), var6.backward(), var6.left(), var6.right(), var6.jump(), true, var6.sprint());
         var2.input.keyPresses = var8;
         var2.setShiftKeyDown(true);
         var2.connection.send(new ServerboundPlayerInputPacket(var8));

         try {
            var5.call(new Object[]{var1, var2, var3, var4});
         } finally {
            var2.input.keyPresses = var6;
            var2.setShiftKeyDown(var7);
            var2.connection.send(new ServerboundPlayerInputPacket(var6));
         }
      }
   }
}
