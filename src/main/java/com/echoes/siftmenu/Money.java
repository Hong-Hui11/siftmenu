package com.echoes.siftmenu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
   public static final long SCALE = 100L;

   private Money() {
   }

   public static long fromCoins(long var0) {
      return var0 * 100L;
   }

   public static long parse(String var0) {
      try {
         BigDecimal var1 = new BigDecimal(var0.trim()).setScale(2, RoundingMode.HALF_UP);
         return var1.movePointRight(2).longValueExact();
      } catch (RuntimeException var2) {
         throw new NumberFormatException("无效金额：" + var0);
      }
   }

   public static String format(long var0) {
      NumberFormat var2 = NumberFormat.getNumberInstance(Locale.CHINA);
      var2.setMinimumFractionDigits(2);
      var2.setMaximumFractionDigits(2);
      return var2.format((double)var0 / 100.0);
   }
}
