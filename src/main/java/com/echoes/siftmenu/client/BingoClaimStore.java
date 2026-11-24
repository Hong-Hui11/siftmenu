package com.echoes.siftmenu.client;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class BingoClaimStore {
   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("siftmenu-bingo-claim.txt");
   private static String token;
   private static boolean loaded;

   private BingoClaimStore() {
   }

   public static String token() {
      load();
      return token;
   }

   public static void save(String var0) {
      if (var0 != null && !var0.isBlank()) {
         loaded = true;
         token = var0;

         try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, var0, StandardCharsets.UTF_8);
         } catch (Exception var2) {
         }
      }
   }

   public static void clear() {
      loaded = true;
      token = null;

      try {
         Files.deleteIfExists(FILE);
      } catch (Exception var1) {
      }
   }

   private static void load() {
      if (!loaded) {
         loaded = true;

         try {
            if (Files.exists(FILE)) {
               String var0 = Files.readString(FILE, StandardCharsets.UTF_8).trim();
               if (!var0.isEmpty()) {
                  token = var0;
               }
            }
         } catch (Exception var1) {
         }
      }
   }
}
