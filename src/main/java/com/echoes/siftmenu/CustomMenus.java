package com.echoes.siftmenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * 数据驱动的自定义菜单系统。
 *
 * <p>服务器管理员可以在 {@code config/siftmenu/menus.json} 中新增页面，
 * 或在内置页面（如 MAIN、HOMES）的指定槽位覆盖 / 追加按钮，
 * 无需修改任何 Java 代码。执行 {@code /siftmenu menu reload} 即可热重载。</p>
 *
 * <p>配置结构见仓库 docs/ 文档站与 docs/examples/menus.example.json。</p>
 */
public final class CustomMenus {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("siftmenu").resolve("menus.json");
   private static final int SLOT_COUNT = 54;
   private static final int BACK_SLOT = 45;
   private static final int CLOSE_SLOT = 49;

   private static final Map<String, PageDef> CUSTOM_PAGES = new LinkedHashMap<>();
   private static final List<PageDef> OVERLAYS = new ArrayList<>();
   private static boolean loaded;

   private CustomMenus() {
   }

   // ------------------------------------------------------------------
   // 对外接口（供 MenuManager 调用）
   // ------------------------------------------------------------------

   /** 单独的自定义页面：填充整页内容。 */
   public static void render(SimpleContainer container, ServerPlayer player, String pageId) {
      ensureLoaded();
      PageDef def = pageId == null ? null : CUSTOM_PAGES.get(pageId);
      if (def == null) {
         container.setItem(22, simple(Items.BARRIER, "&c页面不存在：" + pageId));
         container.setItem(BACK_SLOT, simple(Items.ARROW, "&f返回主菜单"));
         return;
      }

      if (def.isAdminOnly() && !Titles.isAdmin(player)) {
         container.setItem(22, simple(Items.BARRIER, "&c你没有权限查看这个页面"));
         container.setItem(BACK_SLOT, simple(Items.ARROW, "&f返回主菜单"));
         return;
      }

      fillItems(container, player, def, 0);
      if (container.getItem(BACK_SLOT).isEmpty()) {
         container.setItem(BACK_SLOT, simple(Items.ARROW, "&f返回主菜单"));
      }
      if (container.getItem(CLOSE_SLOT).isEmpty()) {
         container.setItem(CLOSE_SLOT, simple(Items.BARRIER, "&c关闭"));
      }
   }

   /** 在内置页面上叠加配置里的按钮（在页面自身内容之后执行，所以会覆盖同槽位）。 */
   public static void applyOverlay(SimpleContainer container, ServerPlayer player, MenuManager.Page page, int pageIndex) {
      if (page == null || page == MenuManager.Page.CUSTOM) {
         return;
      }
      ensureLoaded();
      for (PageDef def : OVERLAYS) {
         if (!def.base.equals(page.name())) {
            continue;
         }
         if (!def.allPages && def.pageIndex != pageIndex) {
            continue;
         }
         if (def.isAdminOnly() && !Titles.isAdmin(player)) {
            continue;
         }
         fillItems(container, player, def, pageIndex);
      }
   }

   /**
    * 处理菜单点击。
    *
    * @return true 表示这次点击已经被自定义菜单消费，内置逻辑不再处理
    */
   public static boolean handleClick(ServerPlayer player, MenuManager.Page page, String customId, int pageIndex, int slot, boolean rightClick) {
      ensureLoaded();
      if (page == MenuManager.Page.CUSTOM) {
         PageDef def = customId == null ? null : CUSTOM_PAGES.get(customId);
         if (def != null && !(def.isAdminOnly() && !Titles.isAdmin(player))) {
            ItemDef item = itemAt(def, slot, pageIndex, player);
            if (item != null) {
               runAction(player, item, rightClick);
               return true;
            }
         }
         // 页面自带的返回 / 关闭按钮
         if (slot == BACK_SLOT) {
            MenuManager.openPage(player, MenuManager.Page.MAIN);
         } else if (slot == CLOSE_SLOT) {
            player.closeContainer();
         }
         return true;
      }

      for (PageDef def : OVERLAYS) {
         if (!def.base.equals(page.name()) || (!def.allPages && def.pageIndex != pageIndex)) {
            continue;
         }
         if (def.isAdminOnly() && !Titles.isAdmin(player)) {
            continue;
         }
         ItemDef item = itemAt(def, slot, pageIndex, player);
         if (item != null) {
            runAction(player, item, rightClick);
            return true;
         }
      }
      return false;
   }

   /** 打开一个自定义页面（配置里的 id）。 */
   public static void open(ServerPlayer player, String pageId) {
      ensureLoaded();
      MenuManager.openCustom(player, pageId);
   }

   public static Component pageTitle(String pageId) {
      ensureLoaded();
      PageDef def = pageId == null ? null : CUSTOM_PAGES.get(pageId);
      if (def == null || def.title == null || def.title.isBlank()) {
         return Component.literal(pageId == null ? "自定义菜单" : pageId).withStyle(ChatFormatting.GOLD);
      }
      return text(def.title);
   }

   /** 挂在 /siftmenu 下的 menu 子命令。 */
   public static LiteralArgumentBuilder<CommandSourceStack> command() {
      return Commands.literal("menu")
         .then(
            Commands.literal("reload")
               .requires(source -> Titles.isAdmin(source))
               .executes(ctx -> {
                  boolean ok = reload();
                  if (ok) {
                     ctx.getSource()
                        .sendSuccess(
                           () -> Component.literal(
                                    "[siftmenu] 自定义菜单已重载：独立页面 " + CUSTOM_PAGES.size() + " 个，内置页覆盖 " + OVERLAYS.size() + " 条"
                                 )
                                 .withStyle(ChatFormatting.GREEN),
                           true
                        );
                  } else {
                     ctx.getSource()
                        .sendFailure(Component.literal("[siftmenu] 重载失败，已保留上一份可用配置，详情见服务端日志").withStyle(ChatFormatting.RED));
                  }
                  return ok ? 1 : 0;
               })
         )
         .then(
            Commands.literal("list").executes(ctx -> {
               ensureLoaded();
               String text = CUSTOM_PAGES.isEmpty() ? "（没有配置独立页面）" : String.join(", ", CUSTOM_PAGES.keySet());
               ctx.getSource().sendSuccess(() -> Component.literal("自定义页面：" + text).withStyle(ChatFormatting.GRAY), false);
               String overlayText = OVERLAYS.isEmpty()
                  ? "（没有内置页覆盖）"
                  : String.join(", ", OVERLAYS.stream().map(def -> def.base + "/" + def.id).toList());
               ctx.getSource().sendSuccess(() -> Component.literal("内置页覆盖：" + overlayText).withStyle(ChatFormatting.GRAY), false);
               return 1;
            })
         )
         .then(
            Commands.literal("open")
               .then(
                  Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                     ServerPlayer player = ctx.getSource().getPlayerOrException();
                     open(player, StringArgumentType.getString(ctx, "id"));
                     return 1;
                  })
               )
         );
   }

   /** 重新读取配置文件；解析失败时保留上一次可用的配置。返回是否成功。 */
   public static boolean reload() {
      List<PageDef> raw;
      try {
         raw = readFile();
      } catch (Exception e) {
         SiftMenu.LOGGER.warn("[siftmenu] 读取自定义菜单配置失败，继续使用旧配置", e);
         loaded = true;
         return false;
      }

      Map<String, PageDef> pages = new LinkedHashMap<>();
      List<PageDef> overlays = new ArrayList<>();
      int skippedItems = 0;

      for (PageDef def : raw) {
         if (def == null || def.id == null || def.id.isBlank()) {
            SiftMenu.LOGGER.warn("[siftmenu] 忽略一个缺少 id 的自定义页面");
            continue;
         }
         def.id = def.id.trim();
         if (def.base == null || def.base.isBlank()) {
            def.base = "CUSTOM";
         }
         def.base = def.base.trim().toUpperCase(Locale.ROOT);
         if (def.pageIndex == null) {
            def.pageIndex = 0;
         }
         if (def.allPages == null) {
            def.allPages = false;
         }
         if (def.items == null) {
            def.items = new ArrayList<>();
         }

         List<ItemDef> valid = new ArrayList<>();
         for (ItemDef item : def.items) {
            if (item == null || item.slot == null || item.slot < 0 || item.slot >= SLOT_COUNT) {
               SiftMenu.LOGGER.warn("[siftmenu] 页面 {} 有一个按钮 slot 缺失或越界，已跳过", def.id);
               skippedItems++;
               continue;
            }
            if (item.item == null || MarketItems.item(item.item) == null) {
               SiftMenu.LOGGER.warn("[siftmenu] 页面 {} 槽位 {} 的物品 id 无效：{}，已跳过", def.id, item.slot, item.item);
               skippedItems++;
               continue;
            }
            valid.add(item);
         }
         def.items = valid;

         if ("CUSTOM".equals(def.base)) {
            if (pages.containsKey(def.id)) {
               SiftMenu.LOGGER.warn("[siftmenu] 自定义页面 id 重复：{}，后者被忽略", def.id);
               continue;
            }
            pages.put(def.id, def);
         } else {
            overlays.add(def);
         }
      }

      CUSTOM_PAGES.clear();
      CUSTOM_PAGES.putAll(pages);
      OVERLAYS.clear();
      OVERLAYS.addAll(overlays);
      loaded = true;
      SiftMenu.LOGGER.info(
         "[siftmenu] 自定义菜单已加载：独立页面 {} 个，内置页覆盖 {} 条，跳过无效按钮 {} 个", CUSTOM_PAGES.size(), OVERLAYS.size(), skippedItems
      );
      return true;
   }

   // ------------------------------------------------------------------
   // 渲染
   // ------------------------------------------------------------------

   private static void fillItems(SimpleContainer container, ServerPlayer player, PageDef def, int pageIndex) {
      for (ItemDef item : def.items) {
         int effectivePage = item.pageIndex == null ? def.pageIndex : item.pageIndex;
         if (effectivePage >= 0 && effectivePage != pageIndex) {
            continue;
         }
         if (item.isAdminOnly() && !Titles.isAdmin(player)) {
            continue;
         }
         container.setItem(item.slot, itemStack(item));
      }
   }

   private static ItemStack itemStack(ItemDef def) {
      Item item = MarketItems.item(def.item);
      ItemStack stack = new ItemStack(item == null ? Items.STONE : item, def.count == null ? 1 : Math.max(1, Math.min(64, def.count)));
      if (def.name != null && !def.name.isBlank()) {
         stack.set(DataComponents.CUSTOM_NAME, text(def.name));
      }
      if (def.lore != null && !def.lore.isEmpty()) {
         List<Component> lore = new ArrayList<>();
         for (String line : def.lore) {
            lore.add(line == null ? Component.empty() : text(line));
         }
         stack.set(DataComponents.LORE, new ItemLore(lore));
      }
      if (def.glint) {
         stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
      }
      return stack;
   }

   private static ItemStack simple(Item item, String name) {
      ItemStack stack = new ItemStack(item);
      stack.set(DataComponents.CUSTOM_NAME, text(name));
      return stack;
   }

   // ------------------------------------------------------------------
   // 点击动作
   // ------------------------------------------------------------------

   private static ItemDef itemAt(PageDef def, int slot, int pageIndex, ServerPlayer player) {
      for (ItemDef item : def.items) {
         if (item.slot == null || item.slot != slot) {
            continue;
         }
         int effectivePage = item.pageIndex == null ? def.pageIndex : item.pageIndex;
         if (effectivePage >= 0 && effectivePage != pageIndex) {
            continue;
         }
         if (item.isAdminOnly() && !Titles.isAdmin(player)) {
            return null;
         }
         return item;
      }
      return null;
   }

   private static void runAction(ServerPlayer player, ItemDef item, boolean rightClick) {
      ActionDef action = rightClick ? item.right : item.left;
      if (action == null || action.type == null || action.type.isBlank()) {
         return;
      }
      switch (action.type.trim().toLowerCase(Locale.ROOT)) {
         case "open" -> openTarget(player, action.page);
         case "command" -> runCommand(player, action);
         case "message" -> player.sendSystemMessage(text(action.text == null ? "" : action.text));
         case "transfer" -> transfer(player, action);
         case "close" -> player.closeContainer();
         case "main", "back" -> MenuManager.openPage(player, MenuManager.Page.MAIN);
         default -> SiftMenu.LOGGER.warn("[siftmenu] 未知的自定义菜单动作类型：{}", action.type);
      }
   }

   private static void openTarget(ServerPlayer player, String target) {
      if (target == null || target.isBlank()) {
         return;
      }
      String id = target.trim();
      if (CUSTOM_PAGES.containsKey(id)) {
         MenuManager.openCustom(player, id);
         return;
      }
      try {
         MenuManager.Page page = MenuManager.Page.valueOf(id.toUpperCase(Locale.ROOT));
         if (page != MenuManager.Page.CUSTOM) {
            MenuManager.openPage(player, page);
            return;
         }
      } catch (IllegalArgumentException ignored) {
         // 不是内置页面名，继续往下报错
      }
      player.sendSystemMessage(text("&c找不到目标页面：" + target));
   }

   private static void runCommand(ServerPlayer player, ActionDef action) {
      if (action.command == null || action.command.isBlank()) {
         return;
      }
      String command = action.command.trim();
      while (command.startsWith("/")) {
         command = command.substring(1);
      }
      boolean console = "console".equalsIgnoreCase(action.as);
      if (console && !Titles.isAdmin(player)) {
         player.sendSystemMessage(text("&c该按钮需要管理员权限"));
         return;
      }
      var server = player.level().getServer();
      if (server == null) {
         return;
      }
      if (console) {
         server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
      } else {
         server.getCommands()
            .performPrefixedCommand(
               server.createCommandSourceStack().withSuppressedOutput(),
               "execute as " + player.getScoreboardName() + " at @s run " + command
            );
      }
   }

   private static void transfer(ServerPlayer player, ActionDef action) {
      if (action.host == null || action.host.isBlank()) {
         player.sendSystemMessage(text("&c该按钮没有配置服务器地址"));
         return;
      }
      int port = action.port == null || action.port <= 0 ? 25565 : action.port;
      player.closeContainer();
      player.connection.send(new ClientboundTransferPacket(action.host.trim(), port));
   }

   // ------------------------------------------------------------------
   // 配置读取
   // ------------------------------------------------------------------

   private static void ensureLoaded() {
      if (!loaded) {
         reload();
      }
   }

   private static List<PageDef> readFile() throws IOException {
      Path dir = FILE.getParent();
      if (dir != null) {
         Files.createDirectories(dir);
      }
      if (!Files.exists(FILE)) {
         writeDefault();
         return new ArrayList<>();
      }
      try (BufferedReader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
         Root root = GSON.fromJson(reader, Root.class);
         if (root == null || root.pages == null) {
            return new ArrayList<>();
         }
         return root.pages;
      }
   }

   private static void writeDefault() throws IOException {
      Root root = new Root();
      root.schema = 1;
      root.pages = new ArrayList<>();
      try (BufferedWriter writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
         GSON.toJson(root, writer);
      }
      SiftMenu.LOGGER.info("[siftmenu] 已生成默认自定义菜单配置：{}", FILE);
   }

   // ------------------------------------------------------------------
   // 文本颜色解析：支持 &0-&f、&k-&o、&r
   // ------------------------------------------------------------------

   public static MutableComponent text(String raw) {
      MutableComponent result = Component.empty();
      if (raw == null || raw.isEmpty()) {
         return result;
      }
      StringBuilder buffer = new StringBuilder();
      ChatFormatting style = null;
      for (int i = 0; i < raw.length(); i++) {
         char c = raw.charAt(i);
         if ((c == '&' || c == '\u00a7') && i + 1 < raw.length()) {
            ChatFormatting next = formattingOf(raw.charAt(i + 1));
            char code = Character.toLowerCase(raw.charAt(i + 1));
            if (next != null || code == 'r') {
               if (!buffer.isEmpty()) {
                  result.append(styled(buffer.toString(), style));
                  buffer.setLength(0);
               }
               style = code == 'r' ? null : next;
               i++;
               continue;
            }
         }
         buffer.append(c);
      }
      if (!buffer.isEmpty()) {
         result.append(styled(buffer.toString(), style));
      }
      return result;
   }

   private static MutableComponent styled(String value, ChatFormatting style) {
      MutableComponent part = Component.literal(value);
      return style == null ? part : part.withStyle(style);
   }

   private static ChatFormatting formattingOf(char code) {
      return switch (Character.toLowerCase(code)) {
         case '0' -> ChatFormatting.BLACK;
         case '1' -> ChatFormatting.DARK_BLUE;
         case '2' -> ChatFormatting.DARK_GREEN;
         case '3' -> ChatFormatting.DARK_AQUA;
         case '4' -> ChatFormatting.DARK_RED;
         case '5' -> ChatFormatting.DARK_PURPLE;
         case '6' -> ChatFormatting.GOLD;
         case '7' -> ChatFormatting.GRAY;
         case '8' -> ChatFormatting.DARK_GRAY;
         case '9' -> ChatFormatting.BLUE;
         case 'a' -> ChatFormatting.GREEN;
         case 'b' -> ChatFormatting.AQUA;
         case 'c' -> ChatFormatting.RED;
         case 'd' -> ChatFormatting.LIGHT_PURPLE;
         case 'e' -> ChatFormatting.YELLOW;
         case 'f' -> ChatFormatting.WHITE;
         case 'k' -> ChatFormatting.OBFUSCATED;
         case 'l' -> ChatFormatting.BOLD;
         case 'm' -> ChatFormatting.STRIKETHROUGH;
         case 'n' -> ChatFormatting.UNDERLINE;
         case 'o' -> ChatFormatting.ITALIC;
         default -> null;
      };
   }

   // ------------------------------------------------------------------
   // 配置模型（字段名与 menus.json 一一对应）
   // ------------------------------------------------------------------

   private static final class Root {
      int schema;
      List<PageDef> pages;
   }

   static final class PageDef {
      String id;
      String title;
      String base;
      String permission;
      Boolean allPages;
      Integer pageIndex;
      List<ItemDef> items;

      boolean isAdminOnly() {
         return "admin".equalsIgnoreCase(permission);
      }
   }

   static final class ItemDef {
      Integer slot;
      String item;
      Integer count;
      String name;
      List<String> lore;
      boolean glint;
      String permission;
      Integer pageIndex;
      ActionDef left;
      ActionDef right;

      boolean isAdminOnly() {
         return "admin".equalsIgnoreCase(permission);
      }
   }

   static final class ActionDef {
      String type;
      String page;
      String command;
      String as;
      String text;
      String host;
      Integer port;
   }
}
