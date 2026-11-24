package com.echoes.siftmenu.client;

import com.echoes.siftmenu.BountyHudClickPayload;
import com.echoes.siftmenu.BountyHudPayload;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterExtract;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.AllowMouseClick;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

public final class BountyHudPanel {
   private static final Identifier ID = Identifier.fromNamespaceAndPath("siftmenu", "bounty_panel");
   private static final int LINE = 11;
   private static final int PAD = 4;
   private static final int GAP = 3;
   private static final int MARGIN = 4;
   private static final int MAX_TEXT = 148;
   private static final int COLOR_TITLE = -22016;
   private static final int COLOR_LABEL = -8090;
   private static final int COLOR_VALUE = -1;
   private static final int COLOR_MONEY = -14003;
   private static final int COLOR_DONE = -10027162;
   private static final int COLOR_SEP = -13382452;
   private static final int COLOR_HOVER = 1090507341;
   private static final int[] DIFFICULTY_COLORS = new int[]{-10027162, -8090, -25027, -43691};
   private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.CHINA);
   private static volatile List<BountyHudPayload.Entry> entries = List.of();
   private static final Set<Integer> ANNOUNCED = new HashSet<>();
   private static boolean primed;

   private BountyHudPanel() {
   }

   public static void register() {
      HudElementRegistry.attachElementAfter(VanillaHudElements.PLAYER_LIST, ID, BountyHudPanel::renderHud);
      ScreenEvents.AFTER_INIT.register((AfterInit)(var0, var1, var2, var3) -> {
         if (var1 instanceof AbstractContainerScreen) {
            ScreenEvents.afterExtract(var1).register((AfterExtract)(var1x, var2x, var3x, var4, var5) -> draw(var2x, var0, var3x, var4));
            ScreenMouseEvents.allowMouseClick(var1).register((AllowMouseClick)(var0x, var1x) -> !handleClick(var1x));
         }
      });
   }

   public static void setEntries(List<BountyHudPayload.Entry> var0) {
      entries = var0;
      if (!primed) {
         primed = true;

         for (BountyHudPayload.Entry var6 : var0) {
            if (var6.done()) {
               ANNOUNCED.add(var6.boardId());
            }
         }
      } else {
         boolean var1 = false;
         HashSet var2 = new HashSet();

         for (BountyHudPayload.Entry var4 : var0) {
            var2.add(var4.boardId());
            if (var4.done() && ANNOUNCED.add(var4.boardId())) {
               var1 = true;
            }
         }

         ANNOUNCED.retainAll(var2);
         if (var1) {
            Minecraft var7 = Minecraft.getInstance();
            var7.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F));
         }
      }
   }

   public static void reset() {
      entries = List.of();
      ANNOUNCED.clear();
      primed = false;
   }

   private static void renderHud(GuiGraphicsExtractor var0, DeltaTracker var1) {
      Minecraft var2 = Minecraft.getInstance();
      if (var2.gui.screen() == null) {
         draw(var0, var2, -1, -1);
      }
   }

   private static void draw(GuiGraphicsExtractor var0, Minecraft var1, int var2, int var3) {
      List var4 = entries;
      if (!var4.isEmpty() && var1.player != null && var1.level != null) {
         BountyHudPanel.Panel var5 = layout(var1, var4);
         int var6 = var5.x() + 4;
         int var7 = var5.y() + 4;
         var0.text(var1.font, "悬赏任务", var6, var7, -22016, true);
         var7 += 11;
         var0.fill(var6, var7, var5.x() + var5.width() - 4, var7 + 1, -13382452);
         var7 += 6;

         for (int var8 = 0; var8 < var4.size(); var8++) {
            BountyHudPayload.Entry var9 = (BountyHudPayload.Entry)var4.get(var8);
            int var10 = blockHeight(var9);
            if (var5.indexAt((double)var2, (double)var3) == var8) {
               var0.fill(var5.x(), var7 - 1, var5.x() + var5.width(), var7 + var10 - 2, 1090507341);
            }

            int var11 = var9.done() ? -10027162 : difficultyColor(var9.difficulty());
            var0.text(var1.font, fit(var1, "#" + var9.boardId() + " " + var9.title(), 148), var6, var7, var11, true);
            var7 += 11;
            var0.text(var1.font, fit(var1, var9.goal(), 148), var6, var7, -1, true);
            var7 += 11;
            String var12 = "进度 ";
            var0.text(var1.font, var12, var6, var7, -8090, true);
            var0.text(var1.font, var9.progress() + " / " + var9.amount(), var6 + var1.font.width(var12), var7, var9.done() ? -10027162 : -1, true);
            var7 += 11;
            String var13 = "奖励 ";
            var0.text(var1.font, var13, var6, var7, -8090, true);
            var0.text(var1.font, MONEY.format((double)var9.rewardCents() / 100.0) + " 金币", var6 + var1.font.width(var13), var7, -14003, true);
            var7 += 11;
            if (var9.done()) {
               var0.text(var1.font, "* 已完成 · 点击领取", var6, var7, -10027162, true);
               var7 += 11;
            }

            var7 += 3;
         }
      }
   }

   private static boolean handleClick(MouseButtonEvent var0) {
      if (var0.button() != 0) {
         return false;
      } else {
         List var1 = entries;
         if (var1.isEmpty()) {
            return false;
         } else {
            Minecraft var2 = Minecraft.getInstance();
            int var3 = layout(var2, var1).indexAt(var0.x(), var0.y());
            if (var3 < 0) {
               return false;
            } else {
               int var4 = ((BountyHudPayload.Entry)var1.get(var3)).boardId();
               if (ClientPlayNetworking.canSend(BountyHudClickPayload.TYPE)) {
                  ClientPlayNetworking.send(new BountyHudClickPayload(var4));
               }

               return true;
            }
         }
      }
   }

   private static BountyHudPanel.Panel layout(Minecraft var0, List<BountyHudPayload.Entry> var1) {
      int var2 = var0.font.width("悬赏任务");
      int var3 = 21;
      int[] var4 = new int[var1.size()];
      int[] var5 = new int[var1.size()];
      int var6 = var3;

      for (int var7 = 0; var7 < var1.size(); var7++) {
         BountyHudPayload.Entry var8 = (BountyHudPayload.Entry)var1.get(var7);
         var2 = Math.max(var2, var0.font.width(fit(var0, "#" + var8.boardId() + " " + var8.title(), 148)));
         var2 = Math.max(var2, var0.font.width(fit(var0, var8.goal(), 148)));
         var2 = Math.max(var2, var0.font.width("进度 " + var8.progress() + " / " + var8.amount()));
         var2 = Math.max(var2, var0.font.width("奖励 " + MONEY.format((double)var8.rewardCents() / 100.0) + " 金币"));
         if (var8.done()) {
            var2 = Math.max(var2, var0.font.width("* 已完成 · 点击领取"));
         }

         var4[var7] = var6;
         var6 += blockHeight(var8);
         var5[var7] = var6 - 1;
         var6 += 3;
      }

      var3 = var6 + 4;
      int var15 = var2 + 8;
      int var16 = var0.getWindow().getGuiScaledHeight();
      int var9 = Math.max(4, (var16 - var3) / 2);
      return new BountyHudPanel.Panel(4, var9, var15, var4, var5);
   }

   private static int blockHeight(BountyHudPayload.Entry var0) {
      return (var0.done() ? 5 : 4) * 11;
   }

   private static int difficultyColor(int var0) {
      return var0 >= 0 && var0 < DIFFICULTY_COLORS.length ? DIFFICULTY_COLORS[var0] : -1;
   }

   private static String fit(Minecraft var0, String var1, int var2) {
      if (var1 != null && !var1.isEmpty()) {
         if (var0.font.width(var1) <= var2) {
            return var1;
         } else {
            StringBuilder var3 = new StringBuilder();

            for (char var7 : var1.toCharArray()) {
               if (var0.font.width(var3.toString() + var7 + "…") > var2) {
                  break;
               }

               var3.append(var7);
            }

            return var3 + "…";
         }
      } else {
         return "";
      }
   }

   static {
      MONEY.setMinimumFractionDigits(2);
      MONEY.setMaximumFractionDigits(2);
   }

   private static record Panel(int x, int y, int width, int[] tops, int[] bottoms) {
      private int indexAt(double var1, double var3) {
         if (!(var1 < (double)this.x) && !(var1 > (double)(this.x + this.width))) {
            for (int var5 = 0; var5 < this.tops.length; var5++) {
               if (var3 >= (double)(this.y + this.tops[var5]) && var3 <= (double)(this.y + this.bottoms[var5])) {
                  return var5;
               }
            }

            return -1;
         } else {
            return -1;
         }
      }
   }
}
