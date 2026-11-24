package com.echoes.siftmenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * 服务器级配置：跨服传送地址与赞助链接。
 *
 * <p>首次启动会在 {@code config/siftmenu/config.json} 生成默认配置，所有地址默认为空。
 * 地址留空时，对应的跨服入口会提示「未配置」，不会把任何服务器地址硬编码进模组。</p>
 */
public final class MenuConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("siftmenu").resolve("config.json");
   private static Root root;
   private static boolean loaded;

   private MenuConfig() {
   }

   /** 读取（或生成）配置文件；失败时回退到空配置。 */
   public static synchronized void reload() {
      loaded = true;
      root = null;
      try {
         Path dir = FILE.getParent();
         if (dir != null) {
            Files.createDirectories(dir);
         }
         if (!Files.exists(FILE)) {
            writeDefault();
            return;
         }
         try (BufferedReader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            root = GSON.fromJson(reader, Root.class);
         }
      } catch (Exception e) {
         SiftMenu.LOGGER.warn("[siftmenu] 读取服务器配置失败，跨服传送与赞助链接将不可用", e);
      }
      if (root == null) {
         root = new Root();
      }
      root.normalize();
   }

   public static Path file() {
      return FILE;
   }

   public static String mainHost() {
      return host(root().servers.main);
   }

   public static int mainPort() {
      return port(root().servers.main);
   }

   public static String bingoHost() {
      return host(root().servers.bingo);
   }

   public static int bingoPort() {
      return port(root().servers.bingo);
   }

   public static String bedwarsHost() {
      return host(root().servers.bedwars);
   }

   public static int bedwarsPort() {
      return port(root().servers.bedwars);
   }

   public static String sponsorUrl() {
      Root value = root();
      return value.sponsorUrl == null ? "" : value.sponsorUrl.trim();
   }

   public static boolean configured(String host, int port) {
      return host != null && !host.isBlank() && port > 0;
   }

   private static Root root() {
      if (!loaded || root == null) {
         reload();
      }
      return root == null ? new Root() : root;
   }

   private static String host(Server server) {
      return server == null || server.host == null ? "" : server.host.trim();
   }

   private static int port(Server server) {
      if (server == null || server.port == null) {
         return 0;
      }
      return server.port > 0 && server.port <= 65535 ? server.port : 0;
   }

   private static void writeDefault() throws IOException {
      Root value = new Root();
      value.normalize();
      try (BufferedWriter writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
         GSON.toJson(value, writer);
      }
      root = value;
      SiftMenu.LOGGER.info("[siftmenu] 已生成默认服务器配置：{}（跨服地址与赞助链接留空，可按需填写）", FILE);
   }

   private static final class Root {
      int schema = 1;
      Servers servers = new Servers();
      String sponsorUrl = "";

      void normalize() {
         if (servers == null) {
            servers = new Servers();
         }
         servers.normalize();
         if (sponsorUrl == null) {
            sponsorUrl = "";
         }
      }
   }

   private static final class Servers {
      Server main = new Server();
      Server bingo = new Server();
      Server bedwars = new Server();

      void normalize() {
         if (main == null) {
            main = new Server();
         }
         if (bingo == null) {
            bingo = new Server();
         }
         if (bedwars == null) {
            bedwars = new Server();
         }
      }
   }

   private static final class Server {
      String host = "";
      Integer port = 0;
   }
}
