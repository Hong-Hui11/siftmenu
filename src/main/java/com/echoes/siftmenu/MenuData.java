package com.echoes.siftmenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;

public final class MenuData {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("siftmenu-data.json");
   private static MenuData instance;
   public Map<String, List<MenuData.Home>> homes = new HashMap<>();
   /// 精品位收益：key = 发布者UUID|访客UUID，value = 已产生金币（同一发布者总量封顶）
   public Map<String, Long> premiumContrib = new HashMap<>();
   public Map<String, List<MenuData.Death>> deaths = new HashMap<>();
   public Map<String, MenuData.Party> parties = new HashMap<>();
   public Map<String, String> playerParty = new HashMap<>();
   public Map<String, String> invites = new HashMap<>();
   public Set<String> bookGiven = new HashSet<>();
   public Map<String, Long> balances = new HashMap<>();
   public Integer currencyVersion;
   public String dailyDate = "";
   public Map<String, Long> dailySold = new HashMap<>();
   public Map<String, Long> dailyPlayerSold = new HashMap<>();
   public Map<String, Long> buyDynamicPrice = new HashMap<>();
   public Map<String, Double> buyPressure = new HashMap<>();
   public Map<String, Long> buyPressureAt = new HashMap<>();
   public Map<String, Integer> buyLowDays = new HashMap<>();
   public Map<String, String> buyLastDay = new HashMap<>();
   public Map<String, List<Long>> buyVolume7d = new HashMap<>();
   public Map<String, Long> buyLastSale = new HashMap<>();
   public Map<String, Long> buyLastPlayerSale = new HashMap<>();
   public long dailyCreated;
   public long dailySpent;
   public long dailyTax;
   public long dailyFee;
   public long dailyService;
   public List<MenuData.MarketListing> listings = new ArrayList<>();
   public Map<String, List<MenuData.PendingItem>> pendingItems = new HashMap<>();
   public List<MenuData.MarketOverride> sellAdmin = new ArrayList<>();
   public List<MenuData.MarketOverride> buyAdmin = new ArrayList<>();
   public Set<String> removedSell = new HashSet<>();
   public Set<String> removedBuy = new HashSet<>();
   public Map<String, MenuData.PlayerTitle> titles = new HashMap<>();
   public List<MenuData.Mail> mails = new ArrayList<>();
   public Map<String, List<String>> mailRead = new HashMap<>();
   public Map<String, List<String>> mailClaimed = new HashMap<>();
   public Map<String, List<String>> mailNotified = new HashMap<>();
   public Set<String> bingoClaims = new HashSet<>();
   public String bountyDay = "";
   public List<MenuData.BountyEntry> bountyBoard = new ArrayList<>();
   public Map<String, Long> bountyProgress = new HashMap<>();
   public Map<String, Integer> bountyDailyCount = new HashMap<>();
   public Map<String, Integer> bountyDailyExtreme = new HashMap<>();
   public Map<String, String> keptElytra = new HashMap<>();
   public Map<String, Long> firstSeen = new HashMap<>();

   public static MenuData get() {
      if (instance == null) {
         instance = load();
      }

      return instance;
   }

   private static MenuData load() {
      if (!Files.exists(FILE)) {
         return freshData();
      } else {
         MenuData var0 = null;

         try (BufferedReader var1 = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            var0 = (MenuData)GSON.fromJson(var1, MenuData.class);
         } catch (Exception var6) {
            SiftMenu.LOGGER.warn("读取菜单数据失败，将使用空数据", var6);
         }

         if (var0 == null) {
            return freshData();
         } else {
            var0.normalize();
            var0.migrateCurrency();
            return var0;
         }
      }
   }

   private static MenuData freshData() {
      MenuData var0 = new MenuData();
      var0.currencyVersion = 2;
      return var0;
   }

   private void normalize() {
      if (this.homes == null) {
         this.homes = new HashMap<>();
      }

      if (this.premiumContrib == null) {
         this.premiumContrib = new HashMap<>();
      }

      if (this.deaths == null) {
         this.deaths = new HashMap<>();
      }

      if (this.parties == null) {
         this.parties = new HashMap<>();
      }

      if (this.playerParty == null) {
         this.playerParty = new HashMap<>();
      }

      if (this.invites == null) {
         this.invites = new HashMap<>();
      }

      if (this.bookGiven == null) {
         this.bookGiven = new HashSet<>();
      }

      if (this.balances == null) {
         this.balances = new HashMap<>();
      }

      if (this.dailyDate == null) {
         this.dailyDate = "";
      }

      if (this.dailySold == null) {
         this.dailySold = new HashMap<>();
      }

      if (this.dailyPlayerSold == null) {
         this.dailyPlayerSold = new HashMap<>();
      }

      if (this.buyDynamicPrice == null) {
         this.buyDynamicPrice = new HashMap<>();
      }

      if (this.buyPressure == null) {
         this.buyPressure = new HashMap<>();
      }

      if (this.buyPressureAt == null) {
         this.buyPressureAt = new HashMap<>();
      }

      if (this.buyLowDays == null) {
         this.buyLowDays = new HashMap<>();
      }

      if (this.buyLastDay == null) {
         this.buyLastDay = new HashMap<>();
      }

      if (this.buyVolume7d == null) {
         this.buyVolume7d = new HashMap<>();
      }

      if (this.buyLastSale == null) {
         this.buyLastSale = new HashMap<>();
      }

      if (this.buyLastPlayerSale == null) {
         this.buyLastPlayerSale = new HashMap<>();
      }

      if (this.listings == null) {
         this.listings = new ArrayList<>();
      }

      if (this.pendingItems == null) {
         this.pendingItems = new HashMap<>();
      }

      if (this.sellAdmin == null) {
         this.sellAdmin = new ArrayList<>();
      }

      if (this.buyAdmin == null) {
         this.buyAdmin = new ArrayList<>();
      }

      if (this.removedSell == null) {
         this.removedSell = new HashSet<>();
      }

      if (this.removedBuy == null) {
         this.removedBuy = new HashSet<>();
      }

      if (this.titles == null) {
         this.titles = new HashMap<>();
      }

      if (this.mails == null) {
         this.mails = new ArrayList<>();
      }

      if (this.mailRead == null) {
         this.mailRead = new HashMap<>();
      }

      if (this.mailClaimed == null) {
         this.mailClaimed = new HashMap<>();
      }

      if (this.mailNotified == null) {
         this.mailNotified = new HashMap<>();
      }

      if (this.bingoClaims == null) {
         this.bingoClaims = new HashSet<>();
      }

      if (this.bountyDay == null) {
         this.bountyDay = "";
      }

      if (this.bountyBoard == null) {
         this.bountyBoard = new ArrayList<>();
      }

      if (this.bountyProgress == null) {
         this.bountyProgress = new HashMap<>();
      }

      if (this.bountyDailyCount == null) {
         this.bountyDailyCount = new HashMap<>();
      }

      if (this.bountyDailyExtreme == null) {
         this.bountyDailyExtreme = new HashMap<>();
      }

      if (this.keptElytra == null) {
         this.keptElytra = new HashMap<>();
      }

      if (this.firstSeen == null) {
         this.firstSeen = new HashMap<>();
      }
   }

   private void migrateCurrency() {
      if (this.currencyVersion == null || this.currencyVersion < 2) {
         try {
            Files.copy(FILE, FILE.resolveSibling("siftmenu-data.pre-decimal.bak"), StandardCopyOption.REPLACE_EXISTING);
         } catch (IOException var3) {
            SiftMenu.LOGGER.warn("备份旧货币数据失败，继续迁移", var3);
         }

         if (this.balances != null) {
            this.balances.replaceAll((var0, var1) -> var1 == null ? 0L : var1 * 100L);
         }

         if (this.listings != null) {
            for (MenuData.MarketListing var2 : this.listings) {
               var2.unitPrice *= 100L;
            }
         }

         if (this.sellAdmin != null) {
            for (MenuData.MarketOverride var6 : this.sellAdmin) {
               var6.price *= 100L;
            }
         }

         if (this.buyAdmin != null) {
            for (MenuData.MarketOverride var7 : this.buyAdmin) {
               var7.price *= 100L;
            }
         }

         this.dailyCreated *= 100L;
         this.dailySpent *= 100L;
         this.dailyTax *= 100L;
         this.dailyFee *= 100L;
         this.currencyVersion = 2;
         this.save();
      }
   }

   public synchronized void save() {
      try {
         Files.createDirectories(FILE.getParent());

         try (BufferedWriter var1 = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
            GSON.toJson(this, var1);
         }
      } catch (IOException var6) {
         SiftMenu.LOGGER.warn("保存菜单数据失败", var6);
      }
   }

   public List<MenuData.Home> homes(UUID var1) {
      return this.homes.computeIfAbsent(var1.toString(), var0 -> new ArrayList<>());
   }

   public boolean addHome(UUID var1, MenuData.Home var2) {
      if ("sift:lobby".equals(var2.dim)) {
         return false;
      }

      List<MenuData.Home> var3 = this.homes(var1);
      if (var3.size() >= 64) {
         return false;
      } else if (var3.stream().anyMatch(var1x -> var1x.name.equalsIgnoreCase(var2.name))) {
         return false;
      } else {
         var3.add(var2);
         this.save();
         return true;
      }
   }

   public boolean removeHome(UUID var1, String var2) {
      boolean var3 = this.homes(var1).removeIf(var1x -> var1x.name.equalsIgnoreCase(var2));
      if (var3) {
         this.save();
      }

      return var3;
   }

   public MenuData.Home findHome(UUID var1, String var2) {
      for (MenuData.Home var4 : this.homes(var1)) {
         if (var4.name.equalsIgnoreCase(var2)) {
            return var4;
         }
      }

      return null;
   }

   public List<MenuData.PublicHome> publicHomes() {
      ArrayList var1 = new ArrayList();

      for (Entry<String, List<MenuData.Home>> var3 : this.homes.entrySet()) {
         for (MenuData.Home var5 : var3.getValue()) {
            if (var5.publicHome) {
               String var6 = var5.publicOwner != null && !var5.publicOwner.isBlank() ? var5.publicOwner : ((String)var3.getKey()).substring(0, 8);
               var1.add(new MenuData.PublicHome((String)var3.getKey(), var6, var5));
            }
         }
      }

      var1.sort(Comparator.<MenuData.PublicHome, String>comparing(var0 -> var0.ownerName()).thenComparing(var0 -> var0.home().name));
      return var1;
   }

   public boolean setHomePublic(UUID var1, String var2, String var3, boolean var4) {
      MenuData.Home var5 = this.findHome(var1, var3);
      if (var5 == null) {
         return false;
      } else {
         var5.publicHome = var4;
         var5.publicOwner = var4 ? var2 : null;
         if (!var4) {
            var5.premium = false;
            var5.premiumSince = 0L;
         }

         this.save();
         return true;
      }
   }

   /// 当前所有精品传送点，按上架时间从新到旧排序。
   public List<MenuData.Home> premiumHomes() {
      ArrayList<MenuData.Home> var1 = new ArrayList<>();

      for (List<MenuData.Home> var4 : this.homes.values()) {
         for (MenuData.Home var6 : var4) {
            if (var6.premium) {
               var1.add(var6);
            }
         }
      }

      var1.sort(Comparator.<MenuData.Home>comparingLong(var0 -> var0.premiumSince).reversed());
      return var1;
   }

   public int premiumCount() {
      int var1 = 0;

      for (List<MenuData.Home> var3 : this.homes.values()) {
         for (MenuData.Home var5 : var3) {
            if (var5.premium) {
               var1++;
            }
         }
      }

      return var1;
   }

   public boolean setHomePremium(UUID var1, String var2, boolean var3) {
      MenuData.Home var4 = this.findHome(var1, var2);
      if (var4 == null || !var4.publicHome) {
         return false;
      } else {
         var4.premium = var3;
         var4.premiumSince = var3 ? System.currentTimeMillis() : 0L;
         this.save();
         return true;
      }
   }

   /// 某位访客已经给这位发布者带来的收益（不分精品点数量）。
   public long premiumContributed(UUID var1, UUID var2) {
      if (var1 == null || var2 == null) {
         return 0L;
      }

      Long var3 = this.premiumContrib.get(var1 + "|" + var2);
      return var3 == null ? 0L : var3;
   }

   /// 记录访客给发布者带来的收益并保存。
   public void addPremiumContributed(UUID var1, UUID var2, long var3) {
      if (var1 == null || var2 == null) {
         return;
      }

      String var5 = var1 + "|" + var2;
      long var6 = this.premiumContrib.getOrDefault(var5, 0L);
      this.premiumContrib.put(var5, var6 + var3);
      this.save();
   }

   /// 通过实体维度 + 名字找到归属者 UUID（用于精品位收益）。
   public UUID ownerOf(MenuData.Home var1) {
      for (Entry<String, List<MenuData.Home>> var3 : this.homes.entrySet()) {
         for (MenuData.Home var5 : var3.getValue()) {
            if (var5 == var1) {
               try {
                  return UUID.fromString(var3.getKey());
               } catch (IllegalArgumentException var6) {
                  return null;
               }
            }
         }
      }

      return null;
   }

   public int unpublishPublicHomesOutside(String var1) {
      int var2 = 0;

      for (List<MenuData.Home> var4 : this.homes.values()) {
         for (MenuData.Home var6 : var4) {
            if (var6.publicHome && !var1.equals(var6.dim)) {
               var6.publicHome = false;
               var6.publicOwner = null;
               var6.premium = false;
               var6.premiumSince = 0L;
               var2++;
            }
         }
      }

      if (var2 > 0) {
         this.save();
      }

      return var2;
   }

   public List<MenuData.Death> deaths(UUID var1) {
      return this.deaths.computeIfAbsent(var1.toString(), var0 -> new ArrayList<>());
   }

   public void addDeath(UUID var1, MenuData.Death var2) {
      List var3 = this.deaths(var1);
      var3.add(0, var2);

      while (var3.size() > 5) {
         var3.remove(var3.size() - 1);
      }

      this.save();
   }

   public MenuData.Party partyOf(UUID var1) {
      String var2 = this.playerParty.get(var1.toString());
      return var2 == null ? null : this.parties.get(var2);
   }

   public MenuData.Party createParty(UUID var1, String var2) {
      MenuData.Party var3 = new MenuData.Party();
      var3.id = UUID.randomUUID().toString();
      var3.name = var2;
      var3.leader = var1.toString();
      var3.createdAt = System.currentTimeMillis();
      var3.members.add(var1.toString());
      this.parties.put(var3.id, var3);
      this.playerParty.put(var1.toString(), var3.id);
      this.invites.remove(var1.toString());
      this.save();
      return var3;
   }

   public void removeParty(String var1) {
      MenuData.Party var2 = this.parties.remove(var1);
      if (var2 != null) {
         for (String var4 : var2.members) {
            this.playerParty.remove(var4);
         }

         this.invites.values().removeIf(var1x -> var1x.equals(var1));
         this.save();
      }
   }

   public void leaveParty(UUID var1) {
      MenuData.Party var2 = this.partyOf(var1);
      if (var2 != null) {
         var2.members.remove(var1.toString());
         this.playerParty.remove(var1.toString());
         if (var2.members.isEmpty()) {
            this.parties.remove(var2.id);
         } else if (var2.leader.equals(var1.toString())) {
            var2.leader = var2.members.get(0);
         }

         this.save();
      }
   }

   public boolean hasAccount(UUID var1) {
      return this.balances.containsKey(var1.toString());
   }

   public long balance(UUID var1) {
      Long var2 = this.balances.get(var1.toString());
      return var2 == null ? 0L : var2;
   }

   public void setBalance(UUID var1, long var2) {
      this.balances.put(var1.toString(), Math.max(0L, var2));
      this.save();
   }

   public void addBalance(UUID var1, long var2) {
      this.setBalance(var1, this.balance(var1) + var2);
   }

   public boolean takeBalance(UUID var1, long var2) {
      if (var2 < 0L) {
         return false;
      } else {
         long var4 = this.balance(var1);
         if (var4 < var2) {
            return false;
         } else {
            this.setBalance(var1, var4 - var2);
            return true;
         }
      }
   }

   public long totalMoney() {
      long var1 = 0L;

      for (Long var4 : this.balances.values()) {
         if (var4 != null) {
            var1 += var4;
         }
      }

      for (MenuData.MarketListing var6 : this.listings) {
         if ("BUY".equals(var6.type)) {
            var1 += var6.remainingEscrow();
         }
      }

      return var1;
   }

   public void dailyReset() {
      String var1 = LocalDate.now().toString();
      if (!var1.equals(this.dailyDate)) {
         String var2 = this.dailyDate;
         HashMap var3 = new HashMap<>(this.dailySold);
         this.dailyDate = var1;
         this.dailySold.clear();
         this.dailyPlayerSold.clear();
         this.dailyCreated = 0L;
         this.dailySpent = 0L;
         this.dailyTax = 0L;
         this.dailyFee = 0L;
         this.dailyService = 0L;
         this.save();
         if (var2 != null && !var2.isEmpty()) {
            GlobalMarket.settleDay(var2, var3);
         }
      }
   }

   public long dailySold(String var1) {
      Long var2 = this.dailySold.get(var1);
      return var2 == null ? 0L : var2;
   }

   public void addDailySold(String var1, long var2) {
      this.dailySold.merge(var1, var2, Long::sum);
      this.save();
   }

   public long playerDailySold(UUID var1, String var2) {
      Long var3 = this.dailyPlayerSold.get(var1 + "|" + var2);
      return var3 == null ? 0L : var3;
   }

   public void addPlayerDailySold(UUID var1, String var2, long var3) {
      this.dailyPlayerSold.merge(var1 + "|" + var2, var3, Long::sum);
      this.save();
   }

   public List<MenuData.MarketListing> activeListings(String var1) {
      long var2 = System.currentTimeMillis();
      ArrayList var4 = new ArrayList();

      for (MenuData.MarketListing var6 : this.listings) {
         if (var1.equals(var6.type) && var6.quantity > 0 && var6.expires > var2) {
            var4.add(var6);
         }
      }

      var4.sort(Comparator.<MenuData.MarketListing>comparingLong(var0 -> var0.created).reversed());
      return var4;
   }

   public MenuData.MarketListing listing(String var1) {
      for (MenuData.MarketListing var3 : this.listings) {
         if (var3.id.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public void addPendingItem(UUID var1, String var2, String var3, int var4) {
      this.pendingItems.computeIfAbsent(var1.toString(), var0 -> new ArrayList<>()).add(new MenuData.PendingItem(var2, var3, var4));
      this.save();
   }

   public List<MenuData.PendingItem> takePendingItems(UUID var1) {
      List var2 = this.pendingItems.remove(var1.toString());
      if (var2 == null) {
         return List.of();
      } else {
         this.save();
         return var2;
      }
   }

   public List<MenuData.PendingItem> pendingItems(UUID var1) {
      List var2 = this.pendingItems.get(var1.toString());
      return var2 == null ? List.of() : var2;
   }

   public MenuData.PendingItem pendingAt(UUID var1, int var2) {
      List var3 = this.pendingItems.get(var1.toString());
      return var3 != null && var2 >= 0 && var2 < var3.size() ? (MenuData.PendingItem)var3.get(var2) : null;
   }

   public void removePendingAt(UUID var1, int var2) {
      List var3 = this.pendingItems.get(var1.toString());
      if (var3 != null && var2 >= 0 && var2 < var3.size()) {
         var3.remove(var2);
         if (var3.isEmpty()) {
            this.pendingItems.remove(var1.toString());
         }

         this.save();
      }
   }

   public void setPendingCount(UUID var1, int var2, int var3) {
      List var4 = this.pendingItems.get(var1.toString());
      if (var4 != null && var2 >= 0 && var2 < var4.size()) {
         if (var3 <= 0) {
            var4.remove(var2);
            if (var4.isEmpty()) {
               this.pendingItems.remove(var1.toString());
            }
         } else {
            ((MenuData.PendingItem)var4.get(var2)).count = var3;
         }

         this.save();
      }
   }

   public MenuData.PlayerTitle title(UUID var1) {
      return this.titles.get(var1.toString());
   }

   public void setTitle(UUID var1, String var2, String var3, String var4) {
      if (var2 == null) {
         this.titles.remove(var1.toString());
      } else {
         MenuData.PlayerTitle var5 = new MenuData.PlayerTitle();
         var5.id = var2;
         var5.customText = var3;
         var5.color = var4;
         this.titles.put(var1.toString(), var5);
      }

      this.save();
   }

   public void recordFirstSeen(UUID var1) {
      String var2 = var1.toString();
      if (!this.firstSeen.containsKey(var2)) {
         this.firstSeen.put(var2, System.currentTimeMillis());
         this.save();
      }
   }

   public long firstSeen(UUID var1) {
      Long var2 = this.firstSeen.get(var1.toString());
      return var2 == null ? 0L : var2;
   }

   public List<MenuData.Mail> mails() {
      if (this.mails == null) {
         this.mails = new ArrayList<>();
      }

      return this.mails;
   }

   public MenuData.Mail mail(String var1) {
      for (MenuData.Mail var3 : this.mails()) {
         if (var3.id.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public boolean isMailRead(UUID var1, String var2) {
      List var3 = this.mailRead.get(var1.toString());
      return var3 != null && var3.contains(var2);
   }

   public void markMailRead(UUID var1, String var2) {
      this.mailRead.computeIfAbsent(var1.toString(), var0 -> new ArrayList<>()).add(var2);
      this.save();
   }

   public boolean isMailClaimed(UUID var1, String var2) {
      List var3 = this.mailClaimed.get(var1.toString());
      return var3 != null && var3.contains(var2);
   }

   public void markMailClaimed(UUID var1, String var2) {
      this.mailClaimed.computeIfAbsent(var1.toString(), var0 -> new ArrayList<>()).add(var2);
      this.save();
   }

   public boolean isMailNotified(UUID var1, String var2) {
      List var3 = this.mailNotified.get(var1.toString());
      return var3 != null && var3.contains(var2);
   }

   public void markMailNotified(UUID var1, String var2) {
      List var3 = this.mailNotified.computeIfAbsent(var1.toString(), var0 -> new ArrayList<>());
      if (!var3.contains(var2)) {
         var3.add(var2);
      }
   }

   public boolean isBingoClaimed(String var1) {
      return this.bingoClaims.contains(var1);
   }

   public void markBingoClaimed(String var1) {
      this.bingoClaims.add(var1);
   }

   public void setKeptElytra(UUID var1, String var2) {
      if (var2 != null && !var2.isEmpty()) {
         this.keptElytra.put(var1.toString(), var2);
         this.save();
      }
   }

   public String takeKeptElytra(UUID var1) {
      String var2 = this.keptElytra.remove(var1.toString());
      if (var2 != null) {
         this.save();
      }

      return var2;
   }

   public int mailClaimCount(String var1) {
      int var2 = 0;

      for (List var4 : this.mailClaimed.values()) {
         if (var4 != null && var4.contains(var1)) {
            var2++;
         }
      }

      return var2;
   }

   public static final class BountyEntry {
      public int boardId;
      public int bountyId;
      public String acceptedBy;
      public String acceptedByName;
      public String state;
   }

   public static final class Death {
      public double x;
      public double y;
      public double z;
      public String dim;
      public long time;
   }

   public static final class Home {
      public String name;
      public double x;
      public double y;
      public double z;
      public float yaw;
      public float pitch;
      public String dim;
      public boolean publicHome;
      public String publicOwner;
      /// 是否为精品传送点（必须先是公共传送点）
      public boolean premium;
      /// 上架精品位的时间戳
      public long premiumSince;
   }

   public static final class Mail {
      public String id;
      public String kind;
      public String audience;
      public String title;
      public String body;
      public String sender;
      public long created;
      public List<String> recipientIds = new ArrayList<>();
      public List<String> recipientNames = new ArrayList<>();
      public List<MenuData.PendingItem> items = new ArrayList<>();
   }

   public static final class MarketListing {
      public String id;
      public String type;
      public String owner;
      public String ownerName;
      public String item;
      public String itemData;
      public int quantity;
      public long unitPrice;
      public long created;
      public long expires;

      public long remainingEscrow() {
         return (long)this.quantity * this.unitPrice;
      }
   }

   public static final class MarketOverride {
      public String id;
      public long price;
      public int dailyStacks;
      public int dailyStacksPerPlayer;
      public String category;
   }

   public static final class Party {
      public String id;
      public String name;
      public String leader;
      public List<String> members = new ArrayList<>();
      public boolean pvp = false;
      public long createdAt;
   }

   public static final class PendingItem {
      public String item;
      public String itemData;
      public int count;

      public PendingItem() {
      }

      public PendingItem(String var1, String var2, int var3) {
         this.item = var1;
         this.itemData = var2;
         this.count = var3;
      }
   }

   public static final class PlayerTitle {
      public String id;
      public String customText;
      public String color;
   }

   public static record PublicHome(String ownerId, String ownerName, MenuData.Home home) {
   }
}
