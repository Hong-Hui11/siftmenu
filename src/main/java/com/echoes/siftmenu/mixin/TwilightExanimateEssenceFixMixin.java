package com.echoes.siftmenu.mixin;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"twilightforest.item.ExanimateEssenceItem"}
)
public abstract class TwilightExanimateEssenceFixMixin {
   private static Map<?, ?> siftmenu$candleMap;
   private static boolean siftmenu$candleMapLoaded;

   @Inject(
      method = {"useOn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void siftmenu$skipBrokenCandle(UseOnContext var1, CallbackInfoReturnable<InteractionResult> var2) {
      try {
         Level var3 = var1.getLevel();
         BlockPos var4 = var1.getClickedPos();
         BlockState var5 = var3.getBlockState(var4);
         if (!(var5.getBlock() instanceof CandleBlock)) {
            return;
         }

         Map var6 = siftmenu$candleMap();
         if (var6 == null) {
            return;
         }

         Block var7 = var5.getBlock();
         if (var6.containsKey(var7) && var6.get(var7) == null) {
            var2.setReturnValue(InteractionResult.PASS);
         }
      } catch (Throwable var8) {
      }
   }

   private static Map<?, ?> siftmenu$candleMap() {
      if (siftmenu$candleMapLoaded) {
         return siftmenu$candleMap;
      } else {
         siftmenu$candleMapLoaded = true;

         try {
            Class var0 = Class.forName("twilightforest.block.OminousCandleBlock");
            Field var1 = var0.getField("CANDLE_MAP");
            if (var1.get(null) instanceof Map var3) {
               siftmenu$candleMap = var3;
            }
         } catch (Throwable var4) {
            siftmenu$candleMap = null;
         }

         return siftmenu$candleMap;
      }
   }
}
