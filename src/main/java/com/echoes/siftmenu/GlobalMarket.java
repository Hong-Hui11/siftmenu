package com.echoes.siftmenu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GlobalMarket {
   private static final Set<Item> PRECIOUS_BUY_ITEMS = Set.of(Items.IRON_BLOCK, Items.GOLD_BLOCK, Items.DIAMOND, Items.NETHERITE_BLOCK);
   private static final String ADMIN_FEATURED = "管理员精选物品";
   private static final long MIN_BUY_PRICE = 1L;
   private static final double PRICE_FLOOR_RATE = 0.05;
   private static final double PRICE_DROP_RATE = 0.85;
   private static final double PRICE_CURVE = 0.75;
   private static final double PRESSURE_RECOVER_HOURS = 8.0;
   private static final double PRESSURE_PER_QUOTA = 1.0;
   private static final double PRECIOUS_ELASTICITY = 0.5;
   private static final double PURCHASE_RELIEF = 0.5;
   private static final int VOLUME_WINDOW_DAYS = 7;
   private static final long BURST_WINDOW_MS = 60000L;
   private static final int BURST_FREE_SALES = 2;
   private static final double BURST_PENALTY = 0.9;
   private static final Map<String, long[]> BUY_BURST = new HashMap<>();
   private static List<GlobalMarket.Category> baseCategories;
   private static List<GlobalMarket.BuyEntry> baseBuyEntries;
   private static List<GlobalMarket.Category> categories;
   private static List<GlobalMarket.BuyEntry> buyEntries;
   private static RegistryAccess registryAccess;
   private static boolean dirty = true;
   private static final String[][] WOOD_GROUPS = new String[][]{
      {"dark_oak", "深色橡木"},
      {"pale_oak", "苍白橡木"},
      {"oak", "橡木"},
      {"spruce", "云杉木"},
      {"birch", "白桦木"},
      {"jungle", "丛林木"},
      {"acacia", "金合欢木"},
      {"mangrove", "红树木"},
      {"cherry", "樱花木"},
      {"bamboo", "竹制"},
      {"crimson", "绯红木"},
      {"warped", "诡异木"}
   };
   private static final String[] COLORS = new String[]{
      "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
   };

   private GlobalMarket() {
   }

   public static synchronized List<GlobalMarket.Category> categories() {
      rebuild();
      return categories;
   }

   public static synchronized List<GlobalMarket.BuyEntry> buyEntries() {
      rebuild();
      return buyEntries;
   }

   public static synchronized void invalidate() {
      dirty = true;
   }

   public static synchronized void prepare(RegistryAccess var0) {
      if (var0 != null && var0 != registryAccess) {
         registryAccess = var0;
         baseCategories = null;
         baseBuyEntries = null;
         invalidate();
      }
   }

   private static void rebuild() {
      if (dirty || categories == null || buyEntries == null) {
         if (baseCategories == null) {
            buildBase();
         }

         MenuData var0 = MenuData.get();
         Map var1 = index(var0.sellAdmin);
         ArrayList var2 = new ArrayList();
         HashSet var3 = new HashSet();

         for (GlobalMarket.Category var5 : baseCategories) {
            ArrayList var6 = new ArrayList();

            for (GlobalMarket.Entry var8 : var5.entries()) {
               var3.add(var8.id());
               if (!var0.removedSell.contains(var8.id())) {
                  MenuData.MarketOverride var9 = (MenuData.MarketOverride)var1.get(var8.id());
                  var6.add(var9 == null ? var8 : new GlobalMarket.Entry(var8.item(), var8.id(), Math.max(1L, var9.price), var8.stack()));
               }
            }

            if (!var6.isEmpty()) {
               var2.add(new GlobalMarket.Category(var5.name(), var5.icon(), var6));
            }
         }

         LinkedHashMap<String, List<GlobalMarket.Entry>> var11 = new LinkedHashMap<>();

         for (MenuData.MarketOverride var16 : var0.sellAdmin) {
            if (var16 != null && var16.id != null && !var3.contains(var16.id) && !var0.removedSell.contains(var16.id)) {
               Item var20 = MarketItems.item(var16.id);
               if (var20 != null) {
                  String var24 = var16.category != null && !var16.category.isBlank() ? var16.category : "管理员精选物品";
                  var11.computeIfAbsent(var24, var0x -> new ArrayList<>()).add(new GlobalMarket.Entry(var20, var16.id, Math.max(1L, var16.price)));
               }
            }
         }

         for (int var13 = 0; var13 < var2.size(); var13++) {
            GlobalMarket.Category var17 = (GlobalMarket.Category)var2.get(var13);
            List var21 = (List)var11.remove(var17.name());
            if (var21 != null && !var21.isEmpty()) {
               ArrayList var25 = new ArrayList<>(var17.entries());
               var25.addAll(var21);
               var25.sort(Comparator.comparing(GlobalMarket.Entry::id));
               var2.set(var13, new GlobalMarket.Category(var17.name(), var17.icon(), var25));
            }
         }

         for (Map.Entry<String, List<GlobalMarket.Entry>> var18 : var11.entrySet()) {
            List<GlobalMarket.Entry> var22 = var18.getValue();
            var22.sort(Comparator.comparing(GlobalMarket.Entry::id));
            Item var26 = "管理员精选物品".equals(var18.getKey()) ? Items.NETHER_STAR : ((GlobalMarket.Entry)var22.get(0)).item();
            var2.add(new GlobalMarket.Category((String)var18.getKey(), var26, var22));
         }

         categories = var2;
         Map var15 = index(var0.buyAdmin);
         ArrayList var19 = new ArrayList();
         HashSet var23 = new HashSet();

         for (GlobalMarket.BuyEntry var29 : baseBuyEntries) {
            var23.add(var29.id());
            if (!var0.removedBuy.contains(var29.id())) {
               MenuData.MarketOverride var10 = (MenuData.MarketOverride)var15.get(var29.id());
               var19.add(
                  var10 == null
                     ? var29
                     : new GlobalMarket.BuyEntry(
                        var29.item(), Math.max(1L, var10.price), Math.max(1, var10.dailyStacks), Math.max(1, var10.dailyStacksPerPlayer)
                     )
               );
            }
         }

         for (MenuData.MarketOverride var30 : var0.buyAdmin) {
            if (var30 != null && var30.id != null && !var23.contains(var30.id) && !var0.removedBuy.contains(var30.id)) {
               Item var31 = MarketItems.item(var30.id);
               if (var31 != null) {
                  var19.add(
                     new GlobalMarket.BuyEntry(var31, Math.max(1L, var30.price), Math.max(1, var30.dailyStacks), Math.max(1, var30.dailyStacksPerPlayer))
                  );
               }
            }
         }

         buyEntries = var19;
         dirty = false;
      }
   }

   private static Map<String, MenuData.MarketOverride> index(List<MenuData.MarketOverride> var0) {
      HashMap var1 = new HashMap();

      for (MenuData.MarketOverride var3 : var0) {
         if (var3 != null && var3.id != null) {
            var1.put(var3.id, var3);
         }
      }

      return var1;
   }

   private static void buildBase() {
      baseCategories = new ArrayList<>();
      ArrayList<GlobalMarket.BuyEntry> sample = new ArrayList<>(
         List.of(
            new GlobalMarket.BuyEntry(Items.IRON_BLOCK, Money.fromCoins(12L), 6, 2),
            new GlobalMarket.BuyEntry(Items.GOLD_BLOCK, Money.fromCoins(15L), 5, 2),
            new GlobalMarket.BuyEntry(Items.DIAMOND, Money.fromCoins(45L), 3, 1),
            new GlobalMarket.BuyEntry(Items.NETHERITE_BLOCK, Money.fromCoins(120L), 1, 1),
            new GlobalMarket.BuyEntry(item("minecraft", "copper_block"), Money.fromCoins(3L), 8, 3),
            new GlobalMarket.BuyEntry(Items.HAY_BLOCK, 40L, 12, 4),
            new GlobalMarket.BuyEntry(Items.SUGAR_CANE, 3L, 40, 4),
            new GlobalMarket.BuyEntry(Items.WHEAT, 5L, 40, 4),
            new GlobalMarket.BuyEntry(Items.CARROT, 4L, 40, 4),
            new GlobalMarket.BuyEntry(Items.POTATO, 4L, 40, 4),
            new GlobalMarket.BuyEntry(Items.BEETROOT, 4L, 30, 3),
            new GlobalMarket.BuyEntry(Items.EGG, 2L, 20, 2),
            new GlobalMarket.BuyEntry(Items.COCOA_BEANS, 4L, 20, 2),
            new GlobalMarket.BuyEntry(Items.PUMPKIN, 6L, 30, 3),
            new GlobalMarket.BuyEntry(Items.MELON, 6L, 30, 3),
            new GlobalMarket.BuyEntry(Items.NETHER_WART, 5L, 20, 2),
            new GlobalMarket.BuyEntry(Items.LEATHER, 8L, 20, 2),
            new GlobalMarket.BuyEntry((Item)Items.WOOL.pick(DyeColor.WHITE), 6L, 30, 3)
         )
      );
      ArrayList<GlobalMarket.BuyEntry> scaled = new ArrayList<>(sample.size());

      for (GlobalMarket.BuyEntry var3 : sample) {
         boolean var4 = PRECIOUS_BUY_ITEMS.contains(var3.item());
         double var5 = var4 ? 1.5 : 2.25;
         int var7 = (int)Math.max(1L, Math.round((double)var3.dailyStacks() * var5));
         int var8 = (int)Math.max(1L, Math.round((double)var3.dailyStacksPerPlayer() * var5));
         long var9 = Math.max(1L, Math.round((double)var3.price() * (var4 ? 1.0 : 1.25)));
         scaled.add(new GlobalMarket.BuyEntry(var3.item(), var9, var7, var8));
      }

      baseBuyEntries = scaled;
      baseCategories.add(new GlobalMarket.Category("种子", Items.WHEAT_SEEDS, seedEntries()));
      baseCategories.add(new GlobalMarket.Category("树苗", Items.OAK_SAPLING, saplingEntries()));
      baseCategories.add(new GlobalMarket.Category("基础农产品", Items.WHEAT, cropEntries()));
      baseCategories.add(new GlobalMarket.Category("基础畜牧产品", Items.LEATHER, livestockEntries()));
      baseCategories.add(new GlobalMarket.Category("建筑·基础方块", Items.STONE_BRICKS, baseBuildingEntries()));
      baseCategories.add(new GlobalMarket.Category("建筑·木材", Items.OAK_LOG, woodBuildingEntries()));
      baseCategories.add(new GlobalMarket.Category("建筑·混凝土", item("minecraft", "white_concrete"), concreteEntries()));
      baseCategories.add(new GlobalMarket.Category("建筑·陶瓦", Items.TERRACOTTA, terracottaEntries()));
      baseCategories.add(new GlobalMarket.Category("建筑·玻璃", Items.GLASS, glassBuildingEntries()));
      baseCategories.add(new GlobalMarket.Category("冰雪", Items.BLUE_ICE, iceEntries()));
      baseCategories.add(
         new GlobalMarket.Category(
            "矿产资源",
            Items.RAW_IRON,
            List.of(
               entry(Items.COAL, 3L),
               entry(Items.RAW_COPPER, 4L),
               entry(Items.RAW_IRON, 6L),
               entry(Items.RAW_GOLD, 8L),
               entry(Items.DIAMOND, 120L),
               entry(Items.ANCIENT_DEBRIS, 180L),
               entry(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 300L)
            )
         )
      );
      baseCategories.add(new GlobalMarket.Category("基础资源", Items.STRING, baseResourceEntries()));
   }

   private static List<GlobalMarket.Entry> baseBuildingEntries() {
      LinkedHashMap var0 = new LinkedHashMap();
      add(var0, Items.STONE, 2L);
      add(var0, Items.COBBLESTONE, 1L);
      add(var0, Items.STONE_BRICKS, 2L);
      add(var0, Items.DIRT, 1L);
      add(var0, Items.GRASS_BLOCK, 1L);
      add(var0, Items.SAND, 1L);
      add(var0, Items.SANDSTONE, 2L);
      add(var0, Items.GRAVEL, 1L);
      add(var0, Items.DEEPSLATE, 2L);
      add(var0, Items.DEEPSLATE_BRICKS, 2L);
      add(var0, Items.TUFF, 1L);
      add(var0, Items.CALCITE, 2L);
      add(var0, Items.ANDESITE, 1L);
      add(var0, Items.DIORITE, 1L);
      add(var0, Items.GRANITE, 1L);
      add(var0, Items.BRICKS, 3L);
      add(var0, Items.MUD_BRICKS, 3L);
      add(var0, Items.PACKED_MUD, 2L);
      add(var0, Items.COBBLED_DEEPSLATE, 2L);
      add(var0, Items.POLISHED_DEEPSLATE, 2L);
      add(var0, Items.DEEPSLATE_TILES, 2L);
      add(var0, Items.CHISELED_DEEPSLATE, 2L);
      add(var0, Items.CRACKED_DEEPSLATE_BRICKS, 2L);
      add(var0, Items.MOSSY_COBBLESTONE, 1L);
      add(var0, Items.MOSSY_STONE_BRICKS, 2L);
      add(var0, Items.CRACKED_STONE_BRICKS, 2L);
      add(var0, Items.CHISELED_STONE_BRICKS, 2L);
      add(var0, Items.SMOOTH_STONE, 2L);
      add(var0, Items.TUFF_BRICKS, 2L);
      add(var0, Items.DRIPSTONE_BLOCK, 2L);
      add(var0, Items.BASALT, 1L);
      add(var0, Items.SMOOTH_BASALT, 2L);
      add(var0, Items.BLACKSTONE, 1L);
      add(var0, Items.POLISHED_BLACKSTONE, 2L);
      add(var0, Items.POLISHED_BLACKSTONE_BRICKS, 2L);
      add(var0, Items.MUD, 1L);
      add(var0, Items.RED_SAND, 1L);
      add(var0, Items.RED_SANDSTONE, 2L);
      return toEntries(var0);
   }

   private static List<GlobalMarket.Entry> woodBuildingEntries() {
      LinkedHashMap var0 = new LinkedHashMap();

      for (String[] var4 : WOOD_GROUPS) {
         if ("bamboo".equals(var4[0])) {
            add(var0, item("minecraft", "bamboo_block"), 2L);
         } else {
            add(var0, item("minecraft", var4[0] + "_log"), 2L);
         }

         add(var0, item("minecraft", var4[0] + "_planks"), 1L);
      }

      return toEntries(var0);
   }

   private static List<GlobalMarket.Entry> concreteEntries() {
      LinkedHashMap var0 = new LinkedHashMap();

      for (String var4 : COLORS) {
         add(var0, item("minecraft", var4 + "_concrete"), 4L);
      }

      return toEntries(var0);
   }

   private static List<GlobalMarket.Entry> terracottaEntries() {
      LinkedHashMap var0 = new LinkedHashMap();
      add(var0, Items.TERRACOTTA, 3L);

      for (String var4 : COLORS) {
         add(var0, item("minecraft", var4 + "_terracotta"), 4L);
      }

      return toEntries(var0);
   }

   private static List<GlobalMarket.Entry> glassBuildingEntries() {
      LinkedHashMap var0 = new LinkedHashMap();
      add(var0, Items.GLASS, 2L);
      add(var0, item("minecraft", "glass_pane"), 2L);

      for (String var4 : COLORS) {
         add(var0, item("minecraft", var4 + "_stained_glass"), 3L);
      }

      return toEntries(var0);
   }

   private static List<GlobalMarket.Entry> iceEntries() {
      ArrayList var0 = new ArrayList();
      var0.add(new GlobalMarket.Entry(Items.ICE, MarketItems.id(Items.ICE), 50L));
      var0.add(new GlobalMarket.Entry(Items.PACKED_ICE, MarketItems.id(Items.PACKED_ICE), 100L));
      var0.add(new GlobalMarket.Entry(Items.BLUE_ICE, MarketItems.id(Items.BLUE_ICE), 500L));
      var0.add(new GlobalMarket.Entry(Items.SNOW_BLOCK, MarketItems.id(Items.SNOW_BLOCK), 50L));
      var0.add(new GlobalMarket.Entry(Items.SNOWBALL, MarketItems.id(Items.SNOWBALL), 50L, new ItemStack(Items.SNOWBALL, 16)));
      var0.add(new GlobalMarket.Entry(Items.POWDER_SNOW_BUCKET, MarketItems.id(Items.POWDER_SNOW_BUCKET), 300L));
      addIce(var0, "cookingforblockheads", "ice_cubes", 60L);
      addIce(var0, "betterend", "emerald_ice", 300L);
      addIce(var0, "betterend", "dense_emerald_ice", 500L);
      addIce(var0, "betterend", "ancient_emerald_ice", 800L);
      var0.sort(Comparator.comparing(GlobalMarket.Entry::id));
      return var0;
   }

   private static void addIce(List<GlobalMarket.Entry> var0, String var1, String var2, long var3) {
      Item var5 = item(var1, var2);
      if (var5 != Items.AIR) {
         var0.add(new GlobalMarket.Entry(var5, MarketItems.id(var5), var3));
      }
   }

   private static List<GlobalMarket.Entry> baseResourceEntries() {
      LinkedHashMap var0 = new LinkedHashMap();
      add(var0, Items.STRING, 1L);
      add(var0, Items.STICK, 1L);
      add(var0, Items.PAPER, 1L);
      add(var0, Items.FLINT, 2L);
      add(var0, Items.CLAY_BALL, 1L);
      add(var0, Items.BRICK, 2L);
      add(var0, Items.CHARCOAL, 2L);
      add(var0, Items.GUNPOWDER, 2L);
      add(var0, Items.SUGAR, 2L);
      add(var0, Items.GLASS_BOTTLE, 2L);
      add(var0, Items.BOWL, 2L);
      add(var0, Items.TORCH, 2L);
      add(var0, item("minecraft", "white_wool"), 3L);
      add(var0, Items.LEATHER, 5L);
      add(var0, Items.FEATHER, 2L);
      add(var0, Items.BONE, 2L);
      add(var0, Items.SLIME_BALL, 4L);
      add(var0, Items.MAGMA_CREAM, 6L);
      add(var0, Items.BLAZE_POWDER, 4L);
      add(var0, Items.ENDER_PEARL, 12L);
      add(var0, Items.GHAST_TEAR, 15L);
      add(var0, Items.HONEYCOMB, 5L);
      add(var0, Items.NAUTILUS_SHELL, 12L);
      add(var0, Items.PRISMARINE_SHARD, 3L);
      add(var0, Items.PRISMARINE_CRYSTALS, 5L);
      add(var0, Items.PHANTOM_MEMBRANE, 6L);
      add(var0, Items.RABBIT_FOOT, 8L);
      add(var0, Items.SPIDER_EYE, 2L);
      add(var0, Items.REDSTONE, 3L);
      add(var0, Items.GLOWSTONE_DUST, 4L);
      add(var0, Items.QUARTZ, 4L);
      add(var0, Items.AMETHYST_SHARD, 5L);
      add(var0, item("minecraft", "resin_clump"), 4L);
      add(var0, item("minecraft", "resin_brick"), 5L);
      ArrayList var1 = new ArrayList<>(toEntries(var0));
      var1.add(new GlobalMarket.Entry(Items.INK_SAC, MarketItems.id(Items.INK_SAC), 170L));
      var1.add(new GlobalMarket.Entry(Items.GLOW_INK_SAC, MarketItems.id(Items.GLOW_INK_SAC), 335L));
      var1.add(new GlobalMarket.Entry(Items.NETHER_BRICK, MarketItems.id(Items.NETHER_BRICK), 500L));
      var1.add(new GlobalMarket.Entry(Items.BLAZE_ROD, MarketItems.id(Items.BLAZE_ROD), 1000L));
      var1.add(new GlobalMarket.Entry(Items.TURTLE_SCUTE, MarketItems.id(Items.TURTLE_SCUTE), 1500L));
      var1.add(new GlobalMarket.Entry(Items.ARMADILLO_SCUTE, MarketItems.id(Items.ARMADILLO_SCUTE), 1170L));
      var1.add(new GlobalMarket.Entry(Items.HONEY_BOTTLE, MarketItems.id(Items.HONEY_BOTTLE), 250L));
      var1.add(new GlobalMarket.Entry(Items.DRAGON_BREATH, MarketItems.id(Items.DRAGON_BREATH), 2000L));
      var1.add(new GlobalMarket.Entry(Items.EXPERIENCE_BOTTLE, MarketItems.id(Items.EXPERIENCE_BOTTLE), 3335L));
      var1.add(new GlobalMarket.Entry(Items.SHULKER_SHELL, MarketItems.id(Items.SHULKER_SHELL), 4170L));
      var1.add(new GlobalMarket.Entry(Items.COPPER_INGOT, MarketItems.id(Items.COPPER_INGOT), 250L));
      var1.add(new GlobalMarket.Entry(Items.ECHO_SHARD, MarketItems.id(Items.ECHO_SHARD), 6670L));
      var1.add(new GlobalMarket.Entry(Items.HONEY_BLOCK, MarketItems.id(Items.HONEY_BLOCK), 670L));
      var1.sort(Comparator.comparing(GlobalMarket.Entry::id));
      return var1;
   }

   public static List<GlobalMarket.EntryRef> allSellEntries() {
      ArrayList var0 = new ArrayList();

      for (GlobalMarket.Category var2 : categories()) {
         for (GlobalMarket.Entry var4 : var2.entries()) {
            var0.add(new GlobalMarket.EntryRef(var2.name(), var4));
         }
      }

      return var0;
   }

   public static String baseCategoryOf(String var0) {
      rebuild();

      for (GlobalMarket.Category var2 : baseCategories) {
         for (GlobalMarket.Entry var4 : var2.entries()) {
            if (var4.id().equals(var0)) {
               return var2.name();
            }
         }
      }

      return "管理员精选物品";
   }

   public static void setSellPrice(String var0, long var1) {
      setSellPrice(var0, var1, (String)null);
   }

   public static void setSellPrice(String var0, long var1, String var3) {
      MenuData var4 = MenuData.get();
      var4.removedSell.remove(var0);
      MenuData.MarketOverride var5 = find(var4.sellAdmin, var0);
      if (var5 == null) {
         var5 = new MenuData.MarketOverride();
         var5.id = var0;
         var5.category = var3 != null && !var3.isBlank() ? var3.trim() : baseCategoryOf(var0);
         var4.sellAdmin.add(var5);
      } else if (var3 != null && !var3.isBlank()) {
         var5.category = var3.trim();
      }

      var5.price = Math.max(1L, var1);
      var4.save();
      invalidate();
   }

   public static void addSellItem(String var0, long var1) {
      setSellPrice(var0, var1);
   }

   public static void removeSellItem(String var0) {
      MenuData var1 = MenuData.get();
      var1.sellAdmin.removeIf(var1x -> var0.equals(var1x.id));
      var1.removedSell.add(var0);
      var1.save();
      invalidate();
   }

   public static boolean buyEntryExists(String var0) {
      for (GlobalMarket.BuyEntry var2 : buyEntries()) {
         if (var2.id().equals(var0)) {
            return true;
         }
      }

      return false;
   }

   public static void setBuy(String var0, long var1, int var3, int var4) {
      MenuData var5 = MenuData.get();
      var5.removedBuy.remove(var0);
      MenuData.MarketOverride var6 = find(var5.buyAdmin, var0);
      if (var6 == null) {
         var6 = new MenuData.MarketOverride();
         var6.id = var0;
         var5.buyAdmin.add(var6);
      }

      var6.price = Math.max(1L, var1);
      var6.dailyStacks = Math.max(1, var3);
      var6.dailyStacksPerPlayer = Math.max(1, var4);
      var5.save();
      invalidate();
   }

   public static void removeBuyItem(String var0) {
      MenuData var1 = MenuData.get();
      var1.buyAdmin.removeIf(var1x -> var0.equals(var1x.id));
      var1.removedBuy.add(var0);
      var1.save();
      invalidate();
   }

   private static MenuData.MarketOverride find(List<MenuData.MarketOverride> var0, String var1) {
      for (MenuData.MarketOverride var3 : var0) {
         if (var3 != null && var1.equals(var3.id)) {
            return var3;
         }
      }

      return null;
   }

   private static List<GlobalMarket.Entry> seedEntries() {
      LinkedHashMap var0 = new LinkedHashMap();
      collectTag(var0, "c", "seeds", 1L);
      add(var0, Items.WHEAT_SEEDS, 1L);
      add(var0, Items.BEETROOT_SEEDS, 1L);
      add(var0, Items.MELON_SEEDS, 1L);
      add(var0, Items.PUMPKIN_SEEDS, 1L);
      add(var0, Items.TORCHFLOWER_SEEDS, 2L);
      add(var0, Items.PITCHER_POD, 2L);
      add(var0, item("the_sift", "sculkflower_seeds"), 5L);
      add(var0, item("kaleidoscope_end", "chorus_seed"), 5L);
      remove(var0, item("farmersdelight", "rice"));
      remove(var0, item("kaleidoscope_cookery", "rice"));
      remove(var0, item("kaleidoscope_cookery", "wild_rice"));
      remove(var0, item("rusticdelight", "coffee_beans"));
      return toEntries(var0);
   }

   private static List<GlobalMarket.Entry> saplingEntries() {
      LinkedHashMap var0 = new LinkedHashMap();
      collectTag(var0, "minecraft", "saplings", 2L);
      collectTag(var0, "c", "saplings", 2L);
      add(var0, Items.OAK_SAPLING, 2L);
      add(var0, Items.SPRUCE_SAPLING, 2L);
      add(var0, Items.BIRCH_SAPLING, 2L);
      add(var0, Items.JUNGLE_SAPLING, 2L);
      add(var0, Items.ACACIA_SAPLING, 2L);
      add(var0, Items.DARK_OAK_SAPLING, 2L);
      add(var0, Items.CHERRY_SAPLING, 3L);
      add(var0, Items.PALE_OAK_SAPLING, 3L);
      add(var0, Items.MANGROVE_PROPAGULE, 3L);
      add(var0, item("the_sift", "overgrown_willow_sapling"), 4L);
      return toEntries(var0);
   }

   private static List<GlobalMarket.Entry> cropEntries() {
      LinkedHashMap var0 = new LinkedHashMap();
      collectTag(var0, "c", "crops", 3L);
      collectTag(var0, "c", "fruits", 3L);
      add(var0, Items.WHEAT, 2L);
      add(var0, Items.CARROT, 2L);
      add(var0, Items.POTATO, 2L);
      add(var0, Items.BEETROOT, 2L);
      add(var0, Items.APPLE, 3L);
      add(var0, Items.MELON_SLICE, 2L);
      add(var0, Items.MELON, 3L);
      add(var0, Items.SWEET_BERRIES, 2L);
      add(var0, Items.GLOW_BERRIES, 3L);
      add(var0, Items.PUMPKIN, 3L);
      add(var0, Items.SUGAR_CANE, 2L);
      add(var0, Items.BAMBOO, 1L);
      add(var0, Items.COCOA_BEANS, 2L);
      add(var0, Items.NETHER_WART, 3L);
      add(var0, item("kaleidoscope_tavern", "grape"), 4L);
      add(var0, item("kaleidoscope_tavern", "green_grape"), 4L);
      add(var0, item("kaleidoscope_tavern", "gold_grape"), 5L);
      add(var0, item("kaleidoscope_tavern", "ice_grape"), 5L);
      add(var0, item("kaleidoscope_tavern", "grapevine"), 3L);
      add(var0, item("farmersdelight", "rice"), 2L);
      add(var0, item("kaleidoscope_cookery", "rice"), 2L);
      add(var0, item("kaleidoscope_cookery", "wild_rice"), 2L);
      add(var0, item("rusticdelight", "coffee_beans"), 4L);
      add(var0, item("ubesdelight", "ube"), 4L);
      add(var0, item("kaleidoscope_nether", "soul_pepper"), 5L);
      add(var0, item("kaleidoscope_end", "dream_berry"), 5L);
      add(var0, item("kaleidoscope_nether", "crimson_fruit"), 4L);
      add(var0, item("kaleidoscope_nether", "warped_fruit"), 4L);
      add(var0, item("kaleidoscope_nether", "poisonous_fruit"), 4L);
      add(var0, item("minecraft", "chorus_fruit"), 4L);
      add(var0, item("the_sift", "sculkflower"), 4L);
      add(var0, item("the_sift", "overgrown_chard"), 3L);
      add(var0, item("the_sift", "overgrown_fronds"), 3L);
      add(var0, item("the_sift", "overgrown_stalks"), 3L);
      add(var0, item("the_sift", "overgrown_lotus"), 4L);
      add(var0, item("the_sift", "healthy_sculk_sprouts"), 3L);
      add(var0, item("the_sift", "dry_healthy_sculk_sprouts"), 3L);
      remove(var0, item("ubesdelight", "garlic_chop"));
      remove(var0, item("ubesdelight", "ginger_chop"));
      remove(var0, item("minecraft", "chorus_flower"));
      return toEntries(var0);
   }

   private static List<GlobalMarket.Entry> livestockEntries() {
      LinkedHashMap var0 = new LinkedHashMap();
      collectTag(var0, "c", "foods/raw_meat", 4L);
      collectTag(var0, "c", "foods/cooked_meat", 6L);
      collectTag(var0, "c", "leathers", 5L);
      collectTag(var0, "c", "eggs", 2L);
      collectTag(var0, "c", "feathers", 2L);
      collectTag(var0, "c", "bones", 2L);
      add(var0, Items.LEATHER, 5L);
      add(var0, Items.BEEF, 4L);
      add(var0, Items.PORKCHOP, 4L);
      add(var0, Items.CHICKEN, 4L);
      add(var0, Items.MUTTON, 4L);
      add(var0, Items.RABBIT, 4L);
      add(var0, Items.COOKED_BEEF, 6L);
      add(var0, Items.COOKED_PORKCHOP, 6L);
      add(var0, Items.COOKED_CHICKEN, 6L);
      add(var0, Items.COOKED_MUTTON, 6L);
      add(var0, Items.COOKED_RABBIT, 6L);
      add(var0, Items.EGG, 2L);
      add(var0, Items.TURTLE_EGG, 3L);
      add(var0, Items.FEATHER, 2L);
      add(var0, Items.BONE, 2L);
      add(var0, Items.RABBIT_HIDE, 2L);
      add(var0, item("the_sift", "raw_sifter_meat"), 6L);
      add(var0, item("the_sift", "cooked_sifter_meat"), 8L);
      add(var0, item("kaleidoscope_nether", "raw_ghast_meat"), 10L);
      add(var0, item("kaleidoscope_nether", "cooked_ghast_meat"), 12L);
      add(var0, item("kaleidoscope_nether", "raw_strider_meat"), 10L);
      add(var0, item("kaleidoscope_nether", "cooked_strider_meat"), 12L);
      add(var0, item("kaleidoscope_end", "raw_ender_dragon_meat"), 12L);
      add(var0, item("kaleidoscope_end", "cooked_ender_dragon_meat"), 15L);
      add(var0, item("kaleidoscope_end", "raw_endermite_meat"), 5L);
      add(var0, item("kaleidoscope_end", "roasted_endermite_meat"), 7L);
      add(var0, item("kaleidoscope_end", "shulker_shell_meat"), 8L);
      add(var0, item("kaleidoscope_cookery", "raw_cut_small_meats"), 4L);
      add(var0, item("kaleidoscope_cookery", "cooked_cut_small_meats"), 6L);
      add(var0, item("kaleidoscope_cookery", "raw_donkey_meat"), 4L);
      add(var0, item("kaleidoscope_cookery", "cooked_donkey_meat"), 6L);
      add(var0, item("farmersdelight", "smoked_ham"), 8L);
      return toEntries(var0);
   }

   private static void collectTag(Map<Item, Long> var0, String var1, String var2, long var3) {
      TagKey<Item> var5 = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(var1, var2));

      for (Holder<Item> var7 : BuiltInRegistries.ITEM.getTagOrEmpty(var5)) {
         var0.putIfAbsent((Item)var7.value(), var3);
      }
   }

   private static void add(Map<Item, Long> var0, Item var1, long var2) {
      if (var1 != null && var1 != Items.AIR) {
         var0.put(var1, var2);
      }
   }

   private static void remove(Map<Item, Long> var0, Item var1) {
      if (var1 != null && var1 != Items.AIR) {
         var0.remove(var1);
      }
   }

   private static Item item(String var0, String var1) {
      return BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(var0, var1)).orElse(Items.AIR);
   }

   private static List<GlobalMarket.Entry> toEntries(Map<Item, Long> var0) {
      ArrayList var1 = new ArrayList();

      for (Map.Entry var3 : var0.entrySet()) {
         var1.add(entry((Item)var3.getKey(), (Long)var3.getValue()));
      }

      var1.sort(Comparator.comparing(GlobalMarket.Entry::id));
      return var1;
   }

   private static GlobalMarket.Entry entry(Item var0, long var1) {
      return new GlobalMarket.Entry(var0, MarketItems.id(var0), Money.fromCoins(var1));
   }

   public static void buyFromServer(ServerPlayer var0, GlobalMarket.Entry var1, int var2) {
      int var3 = Math.max(1, var1.item().getDefaultMaxStackSize());
      int var4 = Math.max(1, Math.min(var2, var3));
      long var5 = (long)var4 * var1.price();
      if (Economy.balance(var0) < var5) {
         var0.sendSystemMessage(Component.literal("金币不足，需要 " + Economy.format(var5) + " 金币").withStyle(ChatFormatting.RED));
      } else if (!Economy.spend(var0, var5)) {
         var0.sendSystemMessage(Component.literal("购买失败，请重试。").withStyle(ChatFormatting.RED));
      } else {
         MenuData var7 = MenuData.get();
         var7.dailyReset();
         var7.dailySpent += var5;
         relievePressure(var1.id(), var4);
         var7.save();
         MarketItems.give(var0, var1.stack(), var4);
         var0.sendSystemMessage(
            Component.literal("已购买 " + var4 + "x ")
               .append(MarketItems.displayName(var1.stack()))
               .append(Component.literal("，花费 " + Economy.format(var5) + " 金币"))
               .withStyle(ChatFormatting.GREEN)
         );
      }
   }

   public static void sellToServer(ServerPlayer var0, GlobalMarket.BuyEntry var1, int var2) {
      MenuData var3 = MenuData.get();
      var3.dailyReset();
      long var4 = (long)var1.dailyStacks() * 64L;
      long var6 = Math.max(0L, var4 - var3.dailySold(var1.id()));
      if (var6 <= 0L) {
         var0.sendSystemMessage(Component.literal("今日全服收购额度已用完，请明天再来。").withStyle(ChatFormatting.RED));
      } else {
         long var8 = (long)var1.dailyStacksPerPlayer() * 64L;
         long var10 = Math.max(0L, var8 - var3.playerDailySold(var0.getUUID(), var1.id()));
         if (var10 <= 0L) {
            var0.sendSystemMessage(Component.literal("你今天的个人出售额度已用完，请明天再来。").withStyle(ChatFormatting.RED));
         } else {
            int var12 = MarketItems.count(var0, var1.item());
            int var13 = (int)Math.min(Math.min(var6, var10), 2147483647L);
            int var14 = var2 == Integer.MAX_VALUE ? var12 : Math.min(var2, var12);
            int var15 = Math.min(var14, var13);
            if (var15 <= 0) {
               var0.sendSystemMessage(Component.literal("背包里没有可出售的 ").append(MarketItems.displayName(var1.item())).withStyle(ChatFormatting.RED));
            } else {
               int var16 = MarketItems.remove(var0, var1.item(), var15);
               if (var16 > 0) {
                  long var17 = Math.max(1L, var1.price());
                  double var19 = pressureOf(var3, var1.id(), var17);
                  double var21 = salePressure(var1, var16);
                  double var23 = Math.min(1.0, var19 + var21);
                  long var25 = averagePrice(var1, var17, var19, var23);
                  if (isBurstSale(var0.getUUID(), var1.id())) {
                     var25 = Math.max(1L, Math.round((double)var25 * 0.9));
                  }

                  long var27 = (long)var16 * var25;
                  var3.addDailySold(var1.id(), (long)var16);
                  var3.addPlayerDailySold(var0.getUUID(), var1.id(), (long)var16);
                  Economy.add(var0.getUUID(), var27);
                  addPressure(var1.id(), var17, var21);
                  var3.save();
                  long var29 = Math.max(0L, var4 - var3.dailySold(var1.id()));
                  long var31 = Math.max(0L, var8 - var3.playerDailySold(var0.getUUID(), var1.id()));
                  long var33 = currentBuyPrice(var1);
                  MutableComponent var35 = Component.literal("已出售 " + var16 + "x ").append(MarketItems.displayName(var1.item()));
                  var35.append(
                     Component.literal(
                        "，单价 "
                           + Money.format(var25)
                           + " 金币，获得 "
                           + Money.format(var27)
                           + " 金币；全服剩余 "
                           + var29
                           + " 个（"
                           + var29 / 64L
                           + " 组），个人剩余 "
                           + var31
                           + " 个（"
                           + var31 / 64L
                           + " 组）；当前收购价 "
                           + Money.format(var33)
                           + " 金币"
                     )
                  );
                  var0.sendSystemMessage(var35.withStyle(ChatFormatting.GREEN));
               }
            }
         }
      }
   }

   public static synchronized long currentBuyPrice(GlobalMarket.BuyEntry var0) {
      long var1 = Math.max(1L, var0.price());
      return priceFor(var0.id(), var1, pressureOf(MenuData.get(), var0.id(), var1));
   }

   public static synchronized int marketPressurePercent(GlobalMarket.BuyEntry var0) {
      long var1 = Math.max(1L, var0.price());
      return (int)Math.round(Math.min(1.0, pressureOf(MenuData.get(), var0.id(), var1)) * 100.0);
   }

   public static synchronized int pricePercent(GlobalMarket.BuyEntry var0) {
      long var1 = Math.max(1L, var0.price());
      return (int)Math.round((double)currentBuyPrice(var0) * 100.0 / (double)var1);
   }

   private static long sellPriceOf(String var0, long var1) {
      for (GlobalMarket.Category var4 : categories()) {
         for (GlobalMarket.Entry var6 : var4.entries()) {
            if (var6.id().equals(var0)) {
               return Math.max(1L, var6.price());
            }
         }
      }

      return Math.max(1L, var1);
   }

   private static long priceFloor(long var0) {
      return Math.max(1L, Math.round((double)var0 * 0.05));
   }

   private static long priceCap(String var0, long var1) {
      long var3 = Math.min(var1, sellPriceOf(var0, var1));
      return Math.max(priceFloor(var1), var3);
   }

   private static long clampPrice(String var0, long var1, long var3) {
      long var5 = priceFloor(var1);
      long var7 = priceCap(var0, var1);
      return Math.max(var5, Math.min(var7, var3));
   }

   private static long priceFor(String var0, long var1, double var3) {
      double var5 = Math.min(1.0, Math.max(0.0, var3));
      double var7 = 0.85 * Math.pow(var5, 0.75);
      long var9 = Math.round((double)var1 * (1.0 - var7));
      return clampPrice(var0, var1, var9);
   }

   private static long averagePrice(GlobalMarket.BuyEntry var0, long var1, double var3, double var5) {
      double var7 = var5 - var3;
      double var9;
      if (var7 <= 1.0E-9) {
         var9 = Math.min(1.0, Math.max(0.0, var3));
      } else {
         double var11 = 1.75;
         var9 = (Math.pow(var5, var11) - Math.pow(var3, var11)) / (var11 * var7);
      }

      double var15 = 0.85 * var9;
      long var13 = Math.round((double)var1 * (1.0 - var15));
      return clampPrice(var0.id(), var1, var13);
   }

   private static double pressureOf(MenuData var0, String var1, long var2) {
      Double var4 = var0.buyPressure.get(var1);
      Long var5;
      double var6;
      if (var4 != null) {
         var6 = var4;
         var5 = var0.buyPressureAt.get(var1);
      } else {
         Long var8 = var0.buyDynamicPrice.get(var1);
         if (var8 != null && var2 > 0L) {
            double var9 = (double)(var2 - Math.max(1L, var8)) / ((double)var2 * 0.85);
            var9 = Math.max(0.0, Math.min(1.0, var9));
            var6 = Math.pow(var9, 1.3333333333333333);
         } else {
            var6 = 0.0;
         }

         var5 = null;
      }

      if (var5 == null) {
         var5 = var0.buyLastSale.get(var1);
      }

      if (var5 != null && var6 > 0.0) {
         double var11 = (double)Math.max(0L, System.currentTimeMillis() / 1000L - var5) / 3600.0;
         var6 *= Math.exp(-var11 / 8.0);
      }

      return Math.max(0.0, Math.min(1.0, var6));
   }

   private static synchronized void addPressure(String var0, long var1, double var3) {
      if (var3 != 0.0) {
         MenuData var5 = MenuData.get();
         double var6 = pressureOf(var5, var0, var1) + var3;
         var6 = Math.max(0.0, Math.min(1.0, var6));
         long var8 = System.currentTimeMillis() / 1000L;
         var5.buyPressure.put(var0, var6);
         var5.buyPressureAt.put(var0, var8);
         var5.buyLastSale.put(var0, var8);
         var5.buyDynamicPrice.put(var0, priceFor(var0, var1, var6));
      }
   }

   private static double salePressure(GlobalMarket.BuyEntry var0, int var1) {
      long var2 = Math.max(1L, (long)var0.dailyStacks() * 64L);
      double var4 = PRECIOUS_BUY_ITEMS.contains(var0.item()) ? 0.5 : 1.0;
      return (double)var1 / (double)var2 * 1.0 * var4;
   }

   private static void relievePressure(String var0, int var1) {
      if (var1 > 0) {
         for (GlobalMarket.BuyEntry var3 : buyEntries()) {
            if (var3.id().equals(var0)) {
               long var4 = Math.max(1L, (long)var3.dailyStacks() * 64L);
               addPressure(var0, Math.max(1L, var3.price()), -((double)var1) / (double)var4 * 0.5);
               return;
            }
         }
      }
   }

   private static double averageDaily(MenuData var0, String var1) {
      List<Long> var2 = var0.buyVolume7d.get(var1);
      long var3 = 0L;
      int var5 = 0;
      if (var2 != null) {
         for (Long var7 : var2) {
            if (var7 != null) {
               var3 += var7;
               var5++;
            }
         }
      }

      return (double)(var3 + var0.dailySold(var1)) / (double)(var5 + 1);
   }

   public static synchronized double recentVolumeRatio(GlobalMarket.BuyEntry var0) {
      long var1 = Math.max(1L, (long)var0.dailyStacks() * 64L);
      return Math.min(1.0, averageDaily(MenuData.get(), var0.id()) / (double)var1);
   }

   public static synchronized int recentVolumePercent(GlobalMarket.BuyEntry var0) {
      return (int)Math.round(recentVolumeRatio(var0) * 100.0);
   }

   public static synchronized long recentDailyVolume(GlobalMarket.BuyEntry var0) {
      return Math.round(averageDaily(MenuData.get(), var0.id()));
   }

   private static boolean isBurstSale(UUID var0, String var1) {
      MenuData var2 = MenuData.get();
      String var3 = var0 + "|" + var1;
      long var4 = System.currentTimeMillis();
      long[] var6 = BUY_BURST.get(var3);
      if (var6 == null || var4 - var6[1] > 60000L) {
         var6 = new long[]{0L, var4};
         BUY_BURST.put(var3, var6);
      }

      var6[0]++;
      var2.buyLastPlayerSale.put(var3, var4 / 1000L);
      return var6[0] > 2L;
   }

   public static synchronized void settleDay(String var0, Map<String, Long> var1) {
      MenuData var2 = MenuData.get();

      for (GlobalMarket.BuyEntry var4 : buyEntries()) {
         String var5 = var4.id();
         Long var6 = var1 == null ? null : (Long)var1.get(var5);
         List var7 = var2.buyVolume7d.computeIfAbsent(var5, var0x -> new ArrayList<>());
         var7.add(var6 == null ? 0L : var6);

         while (var7.size() > 7) {
            var7.remove(0);
         }

         var2.buyLastDay.put(var5, var0);
      }

      var2.save();
   }

   public static long remainingItems(GlobalMarket.BuyEntry var0) {
      MenuData var1 = MenuData.get();
      var1.dailyReset();
      long var2 = (long)var0.dailyStacks() * 64L;
      return Math.max(0L, var2 - var1.dailySold(var0.id()));
   }

   public static long remainingPlayerItems(ServerPlayer var0, GlobalMarket.BuyEntry var1) {
      MenuData var2 = MenuData.get();
      var2.dailyReset();
      long var3 = (long)var1.dailyStacksPerPlayer() * 64L;
      return Math.max(0L, var3 - var2.playerDailySold(var0.getUUID(), var1.id()));
   }

   public static record BuyEntry(Item item, long price, int dailyStacks, int dailyStacksPerPlayer) {
      public String id() {
         return MarketItems.id(this.item);
      }
   }

   public static record Category(String name, Item icon, List<GlobalMarket.Entry> entries) {
   }

   public static record Entry(Item item, String id, long price, ItemStack stack) {
      public Entry(Item var1, String var2, long var3) {
         this(var1, var2, var3, var1 != null && var1 != Items.AIR ? new ItemStack(var1) : ItemStack.EMPTY);
      }
   }

   public static record EntryRef(String category, GlobalMarket.Entry entry) {
   }
}
