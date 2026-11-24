package com.echoes.siftmenu;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class MenuManager {
   private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
   private static final int GRID_COLUMNS = 7;
   private static final int GRID_ROWS = 4;
   private static final int CATEGORY_PER_PAGE = 28;
   private static final int SELL_ITEM_PER_PAGE = 28;
   private static final int PUBLIC_PER_PAGE = 27;
   private static final int TITLE_SLOT_COUNT = 13;
   private static final int MAIL_ADMIN_PER_PAGE = 21;
   private static final long RANDOM_COOLDOWN_MS = 300000L;
   private static final long HOME_COOLDOWN_MS = 180000L;
   private static final long PUBLIC_COOLDOWN_MS = 180000L;
   private static final long BINGO_RANDOM_COOLDOWN_MS = 600000L;
   private static final long BINGO_PLAYER_COOLDOWN_MS = 300000L;
   private static final long LOBBY_COOLDOWN_MS = 300000L;
   private static final long FEE_HOME = 0L;
   private static final long FEE_DEATH = 0L;
   private static final long FEE_RANDOM = 0L;
   /// 精品传送点：上架 2000 金币，取消退 1000，传送一次归属者得 100，全服最多 7 个
   private static final long PREMIUM_COST = Money.fromCoins(2000L);
   private static final long PREMIUM_REFUND = Money.fromCoins(1000L);
   private static final long PREMIUM_REWARD = Money.fromCoins(100L);
   /// 同一位访客对同一个精品点最多能为发布者带来 500 金币收益
   private static final long PREMIUM_VISITOR_CAP = Money.fromCoins(500L);
   private static final int PREMIUM_SLOTS = 7;
   private static final String LOBBY_DIM = "sift:lobby";
   private static final int[] PREMIUM_SLOTS_MAIN = new int[]{19, 20, 21, 22, 23, 24, 25};
   private static final Map<UUID, Long> RANDOM_COOLDOWNS = new HashMap<>();
   private static final Map<UUID, Long> HOME_COOLDOWNS = new HashMap<>();
   private static final Map<UUID, Long> PUBLIC_COOLDOWNS = new HashMap<>();
   private static final Map<UUID, Long> LOBBY_COOLDOWNS = new HashMap<>();
   private static final Map<UUID, Long> BINGO_PLAYER_COOLDOWNS = new HashMap<>();

   private static ItemStack filler() {
      return button((Item)Items.STAINED_GLASS_PANE.pick(DyeColor.GRAY), " ", ChatFormatting.DARK_GRAY);
   }

   private MenuManager() {
   }

   public static boolean isBingoServer() {
      return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && FabricLoader.getInstance().isModLoaded("yet-another-minecraft-bingo");
   }

   public static void openMain(ServerPlayer var0) {
      open(var0, isBingoServer() ? MenuManager.Page.BINGO : MenuManager.Page.MAIN, 0);
   }

   public static void openHomes(ServerPlayer var0) {
      open(var0, MenuManager.Page.HOMES, 0);
   }

   public static void openDeaths(ServerPlayer var0) {
      open(var0, MenuManager.Page.DEATHS, 0);
   }

   public static void openRandom(ServerPlayer var0) {
      open(var0, MenuManager.Page.RANDOM, 0);
   }

   public static void openPlayers(ServerPlayer var0) {
      open(var0, MenuManager.Page.PLAYERS, 0);
   }

   public static void openParty(ServerPlayer var0) {
      openFtb(var0, "teams");
   }

   /// 关闭当前容器并让客户端打开对应的 FTB 界面。
   public static void openFtb(ServerPlayer var0, String var1) {
      var0.closeContainer();
      ServerPlayNetworking.send(var0, new OpenFtbPayload(var1));
   }

   public static void openGlobalMarket(ServerPlayer var0) {
      open(var0, MenuManager.Page.GLOBAL_MARKET, 0);
   }

   public static void openBountyHub(ServerPlayer var0) {
      BountyService.ensureBoard(MenuData.get());
      open(var0, MenuManager.Page.BOUNTY_HUB, 0);
   }

   public static void openBounty(ServerPlayer var0) {
      BountyService.ensureBoard(MenuData.get());
      open(var0, MenuManager.Page.BOUNTY, 0);
   }

   public static void openBountyDetail(ServerPlayer var0, int var1) {
      open(var0, MenuManager.Page.BOUNTY_DETAIL, 0, var1);
   }

   public static void openMyBounty(ServerPlayer var0) {
      BountyService.ensureBoard(MenuData.get());
      open(var0, MenuManager.Page.MY_BOUNTY, 0);
   }

   public static void openFreeMarket(ServerPlayer var0) {
      FreeMarket.cleanup(var0.level().getServer());
      open(var0, MenuManager.Page.FREE_MARKET, 0);
   }

   public static void openFreeSell(ServerPlayer var0) {
      FreeMarket.cleanup(var0.level().getServer());
      open(var0, MenuManager.Page.FREE_SELL_LIST, 0);
   }

   public static void openFreeBuy(ServerPlayer var0) {
      FreeMarket.cleanup(var0.level().getServer());
      open(var0, MenuManager.Page.FREE_BUY_LIST, 0);
   }

   public static void openFreeManage(ServerPlayer var0) {
      FreeMarket.cleanup(var0.level().getServer());
      open(var0, MenuManager.Page.FREE_MANAGE, 0);
   }

   public static void openFreePending(ServerPlayer var0) {
      open(var0, MenuManager.Page.PENDING, 0);
   }

   public static void openTitles(ServerPlayer var0) {
      open(var0, MenuManager.Page.TITLES, 0);
   }

   public static void openMinigames(ServerPlayer var0) {
      open(var0, MenuManager.Page.MINIGAMES, 0);
   }

   private static void openSponsor(ServerPlayer var0) {
      String var1 = MenuConfig.sponsorUrl();
      if (var1.isBlank()) {
         var0.sendSystemMessage(Component.literal("服务器赞助链接准备中，敬请期待。").withStyle(ChatFormatting.GOLD));
      } else {
         var0.sendSystemMessage(Component.literal("服务器赞助：" + var1).withStyle(ChatFormatting.GOLD));
      }
   }

   private static void transferTo(ServerPlayer var0, String var1, int var2) {
      if (var1 != null && !var1.isBlank() && var2 > 0) {
         var0.closeContainer();
         var0.connection.send(new ClientboundTransferPacket(var1, var2));
      } else {
         var0.sendSystemMessage(Component.literal("该小游戏服尚未配置地址。").withStyle(ChatFormatting.GRAY));
      }
   }

   public static void returnToMainServer(ServerPlayer var0) {
      String var1 = MenuConfig.mainHost();
      int var2 = MenuConfig.mainPort();
      if (!MenuConfig.configured(var1, var2)) {
         var0.sendSystemMessage(Component.literal("未配置主服地址，请联系管理员在 config/siftmenu/config.json 中填写。").withStyle(ChatFormatting.GRAY));
         return;
      }

      var0.sendSystemMessage(Component.literal("正在返回主服…").withStyle(ChatFormatting.GRAY));
      transferTo(var0, var1, var2);
   }

   public static void openMailbox(ServerPlayer var0) {
      open(var0, MenuManager.Page.MAILBOX, 0);
   }

   public static void openMailAdmin(ServerPlayer var0) {
      if (!Titles.isAdmin(var0)) {
         var0.sendSystemMessage(Component.literal("你没有管理员权限。").withStyle(ChatFormatting.RED));
      } else {
         open(var0, MenuManager.Page.MAIL_ADMIN, 0);
      }
   }

   public static void openAdmin(ServerPlayer var0) {
      if (!Titles.isAdmin(var0)) {
         var0.sendSystemMessage(Component.literal("你没有管理员权限。").withStyle(ChatFormatting.RED));
      } else {
         open(var0, MenuManager.Page.ADMIN_MAIN, 0);
      }
   }

   public static void openAdminSell(ServerPlayer var0) {
      if (Titles.isAdmin(var0)) {
         open(var0, MenuManager.Page.ADMIN_SELL, 0);
      }
   }

   public static void openAdminBuy(ServerPlayer var0) {
      if (Titles.isAdmin(var0)) {
         open(var0, MenuManager.Page.ADMIN_BUY, 0);
      }
   }

   private static void open(ServerPlayer var0, MenuManager.Page var1, int var2) {
      open(var0, var1, var2, 0);
   }

   private static void open(ServerPlayer var0, MenuManager.Page var1, int var2, int var3) {
      open(var0, var1, var2, var3, null);
   }

   private static void open(ServerPlayer var0, MenuManager.Page var1, int var2, int var3, String var4) {
      int var5 = Math.max(0, var2);
      GlobalMarket.prepare(var0.level().registryAccess());
      var0.closeContainer();
      var0.openMenu(new SimpleMenuProvider((var6, var7, var8) -> new MenuManager.SiftMenuContainer(var6, var7, var0, var1, var5, var3, var4), title(var1, var3, var4)));
   }

   /// 打开配置驱动的自定义页面（config/siftmenu/menus.json）。
   public static void openCustom(ServerPlayer var0, String var1) {
      open(var0, MenuManager.Page.CUSTOM, 0, 0, var1);
   }

   /// 按枚举打开内置页面，供自定义按钮的 open 动作使用。
   public static void openPage(ServerPlayer var0, MenuManager.Page var1) {
      open(var0, var1, 0);
   }

   private static Component title(MenuManager.Page var0, int var1, String varCustom) {
      return switch (var0) {
         case CUSTOM -> CustomMenus.pageTitle(varCustom);
         case MAIN -> Component.literal("筛界菜单").withStyle(ChatFormatting.GOLD);
         case BINGO -> Component.literal("Bingo 菜单").withStyle(ChatFormatting.GOLD);
         case HOMES -> Component.literal("我的家").withStyle(ChatFormatting.GREEN);
         case DEATHS -> Component.literal("死亡地点传送").withStyle(ChatFormatting.RED);
         case RANDOM -> Component.literal("世界传送").withStyle(ChatFormatting.LIGHT_PURPLE);
         case PLAYERS -> Component.literal("玩家传送").withStyle(ChatFormatting.AQUA);
         case PARTY -> Component.literal("队伍").withStyle(ChatFormatting.YELLOW);
         case PARTY_INVITE -> Component.literal("邀请玩家").withStyle(ChatFormatting.YELLOW);
         case PARTY_MEMBERS -> Component.literal("队伍成员").withStyle(ChatFormatting.YELLOW);
         case GLOBAL_MARKET -> Component.literal("全球市场").withStyle(ChatFormatting.GREEN);
         case GLOBAL_SELL -> Component.literal("全球市场 - 购买物资").withStyle(ChatFormatting.GREEN);
         case GLOBAL_SELL_ITEMS -> {
            List var2 = GlobalMarket.categories();
            String var3 = var1 >= 0 && var1 < var2.size() ? ((GlobalMarket.Category)var2.get(var1)).name() : "物品";
            yield Component.literal("全球市场 - " + var3).withStyle(ChatFormatting.GREEN);
         }
         case GLOBAL_BUY -> Component.literal("全球市场 - 收购物资").withStyle(ChatFormatting.YELLOW);
         case BOUNTY_HUB -> Component.literal("悬赏任务").withStyle(ChatFormatting.YELLOW);
         case BOUNTY -> Component.literal("悬赏任务 - 列表").withStyle(ChatFormatting.YELLOW);
         case BOUNTY_DETAIL -> Component.literal("悬赏任务 - 详情").withStyle(ChatFormatting.YELLOW);
         case MY_BOUNTY -> Component.literal("我的悬赏").withStyle(ChatFormatting.YELLOW);
         case FREE_MARKET -> Component.literal("自由市场").withStyle(ChatFormatting.GOLD);
         case FREE_SELL_LIST -> Component.literal("自由市场 - 出售挂单").withStyle(ChatFormatting.AQUA);
         case FREE_BUY_LIST -> Component.literal("自由市场 - 收购挂单").withStyle(ChatFormatting.YELLOW);
         case FREE_MANAGE -> Component.literal("自由市场 - 商品管理").withStyle(ChatFormatting.GOLD);
         case PENDING -> Component.literal("自由市场 - 待领取物品").withStyle(ChatFormatting.LIGHT_PURPLE);
         case TITLES -> Component.literal("我的头衔").withStyle(ChatFormatting.LIGHT_PURPLE);
         case TITLE_COLOR -> Component.literal("头衔颜色").withStyle(ChatFormatting.LIGHT_PURPLE);
         case MINIGAMES -> Component.literal("小游戏").withStyle(ChatFormatting.AQUA);
         case MAILBOX -> Component.literal("我的邮箱").withStyle(ChatFormatting.GOLD);
         case MAIL_ADMIN -> Component.literal("邮箱管理").withStyle(ChatFormatting.DARK_RED);
         case ADMIN_MAIN -> Component.literal("管理员后台").withStyle(ChatFormatting.DARK_RED);
         case ADMIN_SELL -> Component.literal("后台 - 出售物品").withStyle(ChatFormatting.DARK_RED);
         case ADMIN_BUY -> Component.literal("后台 - 收购物品").withStyle(ChatFormatting.DARK_RED);
      };
   }

   private static void handleClick(MenuManager.SiftMenuContainer var0, ServerPlayer var1, int var2, boolean var3) {
      if (CustomMenus.handleClick(var1, var0.page, var0.customId, var0.pageIndex, var2, var3)) {
         return;
      }

      MenuManager.Page var4 = var0.page;
      int var5 = var0.pageIndex;
      int var6 = var0.category;
      MenuData var7 = MenuData.get();
      UUID var8 = var1.getUUID();
      switch (var4) {
         case MAIN:
            switch (var2) {
               case 4:
                  if (var3 && Titles.isAdmin(var1)) {
                     openAdmin(var1);
                  }

                  return;
               case 5:
               case 6:
               case 7:
               case 8:
               case 9:
               case 10:
               case 11:
               case 12:
               case 14:
               case 15:
               case 16:
               case 17:
               case 18:
               case 20:
               case 22:
               case 24:
               case 26:
               case 27:
               case 28:
               case 30:
               case 32:
               case 34:
               case 35:
               case 36:
               case 38:
               case 40:
               case 42:
               case 44:
               case 45:
               case 46:
               case 47:
               case 48:
               default:
                  return;
               case 13:
                  openMailbox(var1);
                  return;
               case 19:
                  openHomes(var1);
                  return;
               case 21:
                  openDeaths(var1);
                  return;
               case 23:
                  openRandom(var1);
                  return;
               case 25:
                  openPlayers(var1);
                  return;
               case 29:
                  openGlobalMarket(var1);
                  return;
               case 31:
                  openFreeMarket(var1);
                  return;
               case 33:
                  openParty(var1);
                  return;
               case 37:
                  openBountyHub(var1);
                  return;
               case 39:
                  openTitles(var1);
                  return;
               case 41:
                  openMinigames(var1);
                  return;
               case 43:
                  openSponsor(var1);
                  return;
               case 49:
                  var1.closeContainer();
                  return;
            }
         case BINGO:
            switch (var2) {
               case 20:
                  if (!checkCooldown(RANDOM_COOLDOWNS, var1, 600000L, "世界随机传送")) {
                     return;
                  }

                  if (randomTeleport(var1)) {
                     startCooldown(RANDOM_COOLDOWNS, var1);
                  }

                  return;
               case 22:
                  openPlayers(var1);
                  return;
               case 24:
                  if (BingoReward.hasRecentWin(var1.getUUID())) {
                     BingoReward.returnToMain(var1);
                  } else {
                     var1.sendSystemMessage(Component.literal("已放弃本局比赛（弃权）。").withStyle(ChatFormatting.RED));
                     returnToMainServer(var1);
                  }

                  return;
               case 49:
                  var1.closeContainer();
                  return;
               default:
                  return;
            }
         case HOMES:
            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 49) {
               ChatInput.requestHome(var1);
               return;
            }

            List var37 = var7.homes(var8);
            int var56 = pages(var37.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var56 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            if (var2 >= 0 && var2 < 45) {
               int var73 = var5 * 45 + var2;
               if (var73 >= var37.size()) {
                  return;
               }

               MenuData.Home var84 = (MenuData.Home)var37.get(var73);
               if (var3) {
                  boolean var86 = !var84.publicHome;
                  var7.setHomePublic(var8, var1.getGameProfile().name(), var84.name, var86);
                  var1.sendSystemMessage(
                     Component.literal(var86 ? "已设为公共传送点：" + var84.name + "（其他玩家可在「世界传送」看到，右键可下架）" : "已下架公共传送点：" + var84.name + "（现在仅自己可见）")
                        .withStyle(var86 ? ChatFormatting.AQUA : ChatFormatting.GRAY)
                  );
                  var0.setPage(var5);
               } else {
                  if (!checkCooldown(HOME_COOLDOWNS, var1, 180000L, "家传送")) {
                     return;
                  }

                  if (teleport(var1, var84)) {
                     startCooldown(HOME_COOLDOWNS, var1);
                  }
               }
            }
            break;
         case DEATHS:
            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 >= 11 && var2 <= 15) {
               int var36 = var2 - 11;
               List var55 = recentDeaths(var8);
               if (var36 < var55.size()) {
                  teleport(var1, (MenuData.Death)var55.get(var36));
               }
            }
            break;
         case RANDOM:
            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 11) {
               if (!checkCooldown(RANDOM_COOLDOWNS, var1, 300000L, "随机传送")) {
                  return;
               }

               if (randomTeleport(var1)) {
                  startCooldown(RANDOM_COOLDOWNS, var1);
               }

               return;
            }

            if (var2 == 13) {
               boolean var36 = LOBBY_DIM.equals(var1.level().dimension().identifier().toString());
               if (var36) {
                  var1.closeContainer();
                  runAsPlayer(var1, "function sift:lobby/exit");
                  return;
               }

               if (!checkCooldown(LOBBY_COOLDOWNS, var1, LOBBY_COOLDOWN_MS, "大厅传送")) {
                  return;
               }

               var1.closeContainer();
               runAsPlayer(var1, "function sift:lobby/enter");
               startCooldown(LOBBY_COOLDOWNS, var1);
               return;
            }

            if (var2 == 15) {
               var1.sendSystemMessage(
                  Component.literal("精品传送点：全服每周最多 " + PREMIUM_SLOTS + " 个，直接显示在主菜单；上架 2000 金币，取消退 1000 金币；有人传送时归属者得 100 金币（不限次数）。")
                     .withStyle(ChatFormatting.LIGHT_PURPLE)
               );
               return;
            }

            List var35 = var7.publicHomes();
            int var54 = Math.max(1, (var35.size() + 18 - 1) / 18);
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var54 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            if (var2 >= 19 && var2 <= 25) {
               teleportPremium(var1, var2);
               return;
            }

            if (var2 >= 27 && var2 < 45) {
               int var72 = var5 * 18 + (var2 - 27);
               if (var72 < var35.size()) {
                  if (!checkCooldown(PUBLIC_COOLDOWNS, var1, 180000L, "公共传送点")) {
                     return;
                  }

                  if (teleport(var1, ((MenuData.PublicHome)var35.get(var72)).home())) {
                     startCooldown(PUBLIC_COOLDOWNS, var1);
                     rewardPremiumOwner(var1, ((MenuData.PublicHome)var35.get(var72)).home());
                  }
               }
            }
            break;
         case PLAYERS:
            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 >= 0 && var2 < 45) {
               List var34 = onlinePlayers(var1);
               if (var2 < var34.size()) {
                  ServerPlayer var53 = (ServerPlayer)var34.get(var2);
                  if (isBingoServer()) {
                     if (!checkCooldown(BINGO_PLAYER_COOLDOWNS, var1, 300000L, "玩家传送")) {
                        return;
                     }

                     TeleportRequestManager.request(var1, var53, var3);
                  } else {
                     TeleportRequestManager.request(var1, var53, var3);
                  }
               }
            }
            break;
         case PARTY:
            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            MenuData.Party var33 = var7.partyOf(var8);
            if (var33 == null) {
               if (var2 == 22) {
                  ChatInput.requestParty(var1);
               }

               return;
            }

            switch (var2) {
               case 20:
                  open(var1, MenuManager.Page.PARTY_INVITE, 0);
                  return;
               case 22:
                  open(var1, MenuManager.Page.PARTY_MEMBERS, 0);
                  return;
               case 24:
                  PartyManager.togglePvp(var1);
                  return;
               case 30:
                  if (var33.leader.equals(var8.toString())) {
                     PartyManager.disband(var1);
                  } else {
                     PartyManager.leave(var1);
                  }

                  return;
               default:
                  return;
            }
         case PARTY_INVITE:
            if (var2 == 45) {
               openParty(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 >= 0 && var2 < 45) {
               List var32 = inviteCandidates(var1);
               if (var2 < var32.size()) {
                  PartyManager.invite(var1, ((ServerPlayer)var32.get(var2)).getGameProfile().name());
                  open(var1, MenuManager.Page.PARTY_INVITE, 0);
               }
            }
            break;
         case PARTY_MEMBERS:
            if (var2 == 45) {
               openParty(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            MenuData.Party var31 = var7.partyOf(var8);
            if (var31 == null || var2 < 0 || var2 >= var31.members.size()) {
               return;
            }

            if (!var31.leader.equals(var8.toString())) {
               return;
            }

            String var52 = var31.members.get(var2);
            if (var52.equals(var8.toString())) {
               return;
            }

            if (var3) {
               PartyManager.kick(var1, var52);
            } else {
               PartyManager.transfer(var1, var52);
            }
            break;
         case GLOBAL_MARKET:
            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 21) {
               open(var1, MenuManager.Page.GLOBAL_SELL, 0);
            } else if (var2 == 23) {
               open(var1, MenuManager.Page.GLOBAL_BUY, 0);
            }
            break;
         case GLOBAL_SELL:
            if (var2 == 45) {
               openGlobalMarket(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            int var30 = categoryPages(GlobalMarket.categories().size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var30 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            int var51 = gridIndex(var2);
            if (var51 < 0) {
               return;
            }

            int var71 = var5 * 28 + var51;
            if (var71 < GlobalMarket.categories().size()) {
               open(var1, MenuManager.Page.GLOBAL_SELL_ITEMS, 0, var71);
            }
            break;
         case GLOBAL_SELL_ITEMS:
            if (var2 == 45) {
               open(var1, MenuManager.Page.GLOBAL_SELL, 0);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            List var29 = GlobalMarket.categories();
            if (var6 < 0 || var6 >= var29.size()) {
               return;
            }

            List var50 = ((GlobalMarket.Category)var29.get(var6)).entries();
            int var70 = sellItemPages(var50.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var70 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            int var83 = gridIndex(var2);
            if (var83 < 0) {
               return;
            }

            int var85 = var5 * 28 + var83;
            if (var85 < var50.size()) {
               GlobalMarket.Entry var14 = (GlobalMarket.Entry)var50.get(var85);
               int var15 = var3 ? Math.max(1, var14.item().getDefaultMaxStackSize()) : 1;
               GlobalMarket.buyFromServer(var1, var14, var15);
            }
            break;
         case GLOBAL_BUY:
            if (var2 == 45) {
               openGlobalMarket(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            List var28 = GlobalMarket.buyEntries();
            int var49 = categoryPages(var28.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var49 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            int var69 = gridIndex(var2);
            if (var69 < 0) {
               return;
            }

            int var82 = var5 * 28 + var69;
            if (var82 < var28.size()) {
               GlobalMarket.sellToServer(var1, (GlobalMarket.BuyEntry)var28.get(var82), var3 ? 64 : 1);
               var0.setPage(var5);
            }
            break;
         case BOUNTY_HUB:
            if (var2 == 21) {
               openBounty(var1);
               return;
            }

            if (var2 == 23) {
               openMyBounty(var1);
               return;
            }

            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
            }
            break;
         case BOUNTY:
            if (var2 == 45) {
               openBountyHub(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            List var27 = var7.bountyBoard;
            int var48 = bountyPages(var27.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var48 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            int var68 = gridIndex(var2);
            if (var68 < 0) {
               return;
            }

            int var81 = var5 * 28 + var68;
            if (var81 < var27.size()) {
               openBountyDetail(var1, ((MenuData.BountyEntry)var27.get(var81)).boardId);
            }
            break;
         case BOUNTY_DETAIL:
            if (var2 == 45) {
               openBounty(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            MenuData.BountyEntry var47 = BountyService.entry(var7, var6);
            if (var47 == null) {
               openBounty(var1);
               return;
            }

            if (var2 == 21) {
               BountyCatalog.Bounty var67 = BountyCatalog.byId(var47.bountyId);
               if (BountyService.isMine(var47, var8) && BountyService.completed(var8, var67)) {
                  BountyService.claim(var1, var6);
               } else {
                  BountyService.accept(var1, var6);
               }

               openBountyDetail(var1, var6);
               return;
            }

            if (var2 == 24 && BountyService.abandon(var1, var6)) {
               openBountyDetail(var1, var6);
            }

            if (var2 == 22) {
               BountyCatalog.Bounty var66 = BountyCatalog.byId(var47.bountyId);
               if (var66 != null && BountyService.isMine(var47, var8) && "submit_item".equals(var66.trigger()) && !BountyService.completed(var8, var66)) {
                  BountyService.submit(var1, var6);
                  openBountyDetail(var1, var6);
               }
            }
            break;
         case MY_BOUNTY:
            if (var2 == 45) {
               openBountyHub(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            List var26 = BountyService.takenBy(var7, var8);
            int var46 = gridIndex(var2);
            if (var46 < 0) {
               return;
            }

            int var65 = var5 * 28 + var46;
            if (var65 < var26.size()) {
               openBountyDetail(var1, ((MenuData.BountyEntry)var26.get(var65)).boardId);
            }
            break;
         case FREE_MARKET:
            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            switch (var2) {
               case 19:
                  openFreeSell(var1);
                  return;
               case 20:
               case 22:
               case 24:
               case 26:
               case 27:
               case 28:
               case 30:
               default:
                  return;
               case 21:
                  openFreeBuy(var1);
                  return;
               case 23:
                  ChatInput.requestFreeSell(var1);
                  return;
               case 25:
                  ChatInput.requestFreeBuy(var1);
                  return;
               case 29:
                  openFreeManage(var1);
                  return;
               case 31:
                  openFreePending(var1);
                  return;
            }
         case FREE_SELL_LIST:
            if (var2 == 45) {
               openFreeMarket(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            List var25 = FreeMarket.activeListings("SELL");
            int var45 = pages(var25.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var45 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            if (var2 >= 0 && var2 < 45) {
               int var64 = var5 * 45 + var2;
               if (var64 < var25.size()) {
                  FreeMarket.buySellListing(var1, ((MenuData.MarketListing)var25.get(var64)).id, var3);
                  int var80 = pages(FreeMarket.activeListings("SELL").size());
                  var0.setPage(Math.max(0, Math.min(var5, var80 - 1)));
               }
            }
            break;
         case FREE_BUY_LIST:
            if (var2 == 45) {
               openFreeMarket(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            List var24 = FreeMarket.activeListings("BUY");
            int var44 = pages(var24.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var44 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            if (var2 >= 0 && var2 < 45) {
               int var63 = var5 * 45 + var2;
               if (var63 < var24.size()) {
                  FreeMarket.fulfillBuyOrder(var1, ((MenuData.MarketListing)var24.get(var63)).id, var3);
                  int var79 = pages(FreeMarket.activeListings("BUY").size());
                  var0.setPage(Math.max(0, Math.min(var5, var79 - 1)));
               }
            }
            break;
         case FREE_MANAGE:
            if (var2 == 45) {
               openFreeMarket(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            List var23 = FreeMarket.myListings(var8);
            int var43 = pages(var23.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var43 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            if (var2 >= 0 && var2 < 45) {
               int var62 = var5 * 45 + var2;
               if (var62 >= var23.size()) {
                  return;
               }

               MenuData.MarketListing var78 = (MenuData.MarketListing)var23.get(var62);
               if (var3) {
                  ChatInput.requestFreeQuantity(var1, var78.id);
               } else {
                  ChatInput.requestFreePrice(var1, var78.id);
               }
            }
            break;
         case PENDING:
            if (var2 == 45) {
               openFreeMarket(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            List var22 = MenuData.get().pendingItems(var8);
            int var42 = pages(var22.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var42 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            if (var2 >= 0 && var2 < 45) {
               int var61 = var5 * 45 + var2;
               if (var61 >= var22.size()) {
                  return;
               }

               FreeMarket.claimPending(var1, var61);
               int var77 = pages(MenuData.get().pendingItems(var8).size());
               var0.setPage(Math.max(0, Math.min(var5, var77 - 1)));
            }
            break;
         case TITLES:
            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 49) {
               Titles.clear(var1);
               var0.setPage(0);
               return;
            }

            if (var2 == 40) {
               open(var1, MenuManager.Page.TITLE_COLOR, 0);
               return;
            }

            int var21 = titleIndex(var2);
            if (var21 < 0) {
               return;
            }

            if (var21 == 0) {
               if (var3) {
                  ChatInput.requestTitleText(var1);
               } else {
                  Titles.equipCustom(var1);
                  var0.setPage(0);
               }

               return;
            }

            List var41 = Titles.achievementDefs(var1);
            int var60 = var21 - 1;
            if (var60 < var41.size()) {
               Titles.equip(var1, ((Titles.Def)var41.get(var60)).id());
               var0.setPage(0);
            }
            break;
         case TITLE_COLOR:
            if (var2 == 45) {
               openTitles(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 20) {
               Titles.setColor(var1, "purple");
               openTitles(var1);
            } else if (var2 == 22) {
               Titles.setColor(var1, "blue");
               openTitles(var1);
            } else if (var2 == 24) {
               if (Titles.setColor(var1, "rainbow")) {
                  openTitles(var1);
               }
            } else if (var2 == 31 && Titles.isAdmin(var1) && Titles.setColor(var1, "red")) {
               openTitles(var1);
            }
            break;
         case MINIGAMES:
            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 21) {
               transferTo(var1, MenuConfig.bedwarsHost(), MenuConfig.bedwarsPort());
               return;
            }

            if (var2 == 23) {
               transferTo(var1, MenuConfig.bingoHost(), MenuConfig.bingoPort());
               return;
            }
            break;
         case MAILBOX:
            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 49 && Titles.isAdmin(var1)) {
               openMailAdmin(var1);
               return;
            }

            List var20 = Mailbox.forPlayer(var8, var1.getGameProfile().name());
            int var40 = pages(var20.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var40 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            if (var2 >= 0 && var2 < 45) {
               int var59 = var5 * 45 + var2;
               if (var59 >= var20.size()) {
                  return;
               }

               MenuData.Mail var76 = (MenuData.Mail)var20.get(var59);
               Mailbox.read(var1, var76.id);
               if (!Mailbox.isAnnouncement(var76) && !var7.isMailClaimed(var8, var76.id)) {
                  Mailbox.claim(var1, var76.id);
               }

               var0.setPage(var5);
            }
            break;
         case MAIL_ADMIN:
            if (!Titles.isAdmin(var1)) {
               var1.closeContainer();
               return;
            }

            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 11) {
               ChatInput.requestMailCompensation(var1);
               return;
            }

            if (var2 == 13) {
               ChatInput.requestMailAnnouncement(var1);
               return;
            }

            List var19 = Mailbox.all();
            int var39 = mailAdminPages(var19.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var39 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            int var58 = mailAdminIndex(var2);
            if (var58 >= 0) {
               int var75 = var5 * 21 + var58;
               if (var75 < var19.size()) {
                  Mailbox.read(var1, ((MenuData.Mail)var19.get(var75)).id);
               }
            }
            break;
         case ADMIN_MAIN:
            if (!Titles.isAdmin(var1)) {
               var1.closeContainer();
               return;
            }

            if (var2 == 45) {
               openMain(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 19) {
               openAdminSell(var1);
            } else if (var2 == 21) {
               openAdminBuy(var1);
            } else if (var2 == 23) {
               openMailAdmin(var1);
            } else if (var2 == 25) {
               Economy.report(var1);
            }
            break;
         case ADMIN_SELL:
            if (!Titles.isAdmin(var1)) {
               var1.closeContainer();
               return;
            }

            if (var2 == 45) {
               openAdmin(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 49) {
               ItemStack var18 = var1.getMainHandItem();
               if (!var18.isEmpty()) {
                  ChatInput.requestAdminSellAdd(var1, MarketItems.id(var18.getItem()));
               } else {
                  var1.sendSystemMessage(Component.literal("请先手持要添加或修改的物品。").withStyle(ChatFormatting.RED));
               }

               return;
            }

            List var17 = GlobalMarket.allSellEntries();
            int var38 = pages(var17.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var38 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            if (var2 >= 0 && var2 < 45) {
               int var57 = var5 * 45 + var2;
               if (var57 >= var17.size()) {
                  return;
               }

               GlobalMarket.Entry var74 = ((GlobalMarket.EntryRef)var17.get(var57)).entry();
               if (var3) {
                  GlobalMarket.removeSellItem(var74.id());
                  var1.sendSystemMessage(Component.literal("已从出售列表移除：" + var74.id()).withStyle(ChatFormatting.GRAY));
                  var0.setPage(Math.min(var5, pages(GlobalMarket.allSellEntries().size()) - 1));
               } else {
                  ChatInput.requestAdminSellPrice(var1, var74.id(), var74.price());
               }
            }
            break;
         case ADMIN_BUY:
            if (!Titles.isAdmin(var1)) {
               var1.closeContainer();
               return;
            }

            if (var2 == 45) {
               openAdmin(var1);
               return;
            }

            if (var2 == 53) {
               var1.closeContainer();
               return;
            }

            if (var2 == 49) {
               ItemStack var16 = var1.getMainHandItem();
               if (!var16.isEmpty()) {
                  ChatInput.requestAdminBuyAdd(var1, MarketItems.id(var16.getItem()));
               } else {
                  var1.sendSystemMessage(Component.literal("请先手持要收购的物品。").withStyle(ChatFormatting.RED));
               }

               return;
            }

            List var9 = GlobalMarket.buyEntries();
            int var10 = pages(var9.size());
            if (var2 == 46 && var5 > 0) {
               var0.setPage(var5 - 1);
               return;
            }

            if (var2 == 50 && var5 < var10 - 1) {
               var0.setPage(var5 + 1);
               return;
            }

            if (var2 >= 0 && var2 < 45) {
               int var11 = var5 * 45 + var2;
               if (var11 >= var9.size()) {
                  return;
               }

               GlobalMarket.BuyEntry var12 = (GlobalMarket.BuyEntry)var9.get(var11);
               if (var3) {
                  GlobalMarket.removeBuyItem(var12.id());
                  var1.sendSystemMessage(Component.literal("已从收购列表移除：" + var12.id()).withStyle(ChatFormatting.GRAY));
                  var0.setPage(Math.min(var5, pages(GlobalMarket.buyEntries().size()) - 1));
               } else {
                  String var13 = Economy.format(var12.price()) + " " + var12.dailyStacks() + " " + var12.dailyStacksPerPlayer();
                  ChatInput.requestAdminBuyEdit(var1, var12.id(), var13);
               }
            }
      }
   }

   private static void deleteHome(MenuManager.SiftMenuContainer var0, ServerPlayer var1, int var2) {
      if (var2 >= 0 && var2 < 45) {
         MenuData var3 = MenuData.get();
         List var4 = var3.homes(var1.getUUID());
         int var5 = var0.pageIndex * 45 + var2;
         if (var5 < var4.size()) {
            MenuData.Home var6 = (MenuData.Home)var4.get(var5);
            var3.removeHome(var1.getUUID(), var6.name);
            var1.sendSystemMessage(Component.literal("已删除家：" + var6.name + (var6.publicHome ? "（已同时从世界传送中下架）" : "")).withStyle(ChatFormatting.GRAY));
            int var7 = pages(var3.homes(var1.getUUID()).size());
            var0.setPage(Math.max(0, Math.min(var0.pageIndex, var7 - 1)));
         }
      }
   }

   private static void handleShiftClick(MenuManager.SiftMenuContainer var0, ServerPlayer var1, int var2, boolean var3) {
      if (CustomMenus.handleClick(var1, var0.page, var0.customId, var0.pageIndex, var2, true)) {
         return;
      }

      if (var3 && var2 >= 0 && var2 < 45) {
         if (var0.page == MenuManager.Page.HOMES) {
            togglePremium(var0, var1, var2);
         }

         return;
      }

      if (!var3 && var2 >= 0 && var2 < 45) {
         if (var0.page == MenuManager.Page.GLOBAL_BUY) {
            int var4 = gridIndex(var2);
            if (var4 >= 0) {
               int var5 = var0.pageIndex * 28 + var4;
               List var6 = GlobalMarket.buyEntries();
               if (var5 < var6.size()) {
                  GlobalMarket.sellToServer(var1, (GlobalMarket.BuyEntry)var6.get(var5), Integer.MAX_VALUE);
                  var0.setPage(var0.pageIndex);
               }
            }
         } else {
            if (var0.page == MenuManager.Page.HOMES) {
               deleteHome(var0, var1, var2);
            } else if (var0.page == MenuManager.Page.FREE_MANAGE) {
               cancelManagedListing(var0, var1, var2);
            } else if (var0.page == MenuManager.Page.MAIL_ADMIN) {
               deleteMail(var0, var1, var2);
            }
         }
      }
   }

   private static void deleteMail(MenuManager.SiftMenuContainer var0, ServerPlayer var1, int var2) {
      if (Titles.isAdmin(var1)) {
         int var3 = mailAdminIndex(var2);
         if (var3 >= 0) {
            List var4 = Mailbox.all();
            int var5 = var0.pageIndex * 21 + var3;
            if (var5 < var4.size()) {
               MenuData.Mail var6 = (MenuData.Mail)var4.get(var5);
               Mailbox.remove(var6.id);
               var1.sendSystemMessage(Component.literal("已删除邮件：" + var6.title).withStyle(ChatFormatting.GRAY));
               int var7 = mailAdminPages(Mailbox.all().size());
               var0.setPage(Math.max(0, Math.min(var0.pageIndex, var7 - 1)));
            }
         }
      }
   }

   private static void cancelManagedListing(MenuManager.SiftMenuContainer var0, ServerPlayer var1, int var2) {
      List var3 = FreeMarket.myListings(var1.getUUID());
      int var4 = var0.pageIndex * 45 + var2;
      if (var4 < var3.size()) {
         FreeMarket.cancel(var1, ((MenuData.MarketListing)var3.get(var4)).id);
         int var5 = pages(FreeMarket.myListings(var1.getUUID()).size());
         var0.setPage(Math.max(0, Math.min(var0.pageIndex, var5 - 1)));
      }
   }

   private static boolean teleport(ServerPlayer var0, MenuData.Home var1) {
      try {
         ServerLevel var2 = levelOf(var0, var1.dim);
         if (var2 == null) {
            var0.sendSystemMessage(Component.literal("目标维度不存在：" + var1.dim).withStyle(ChatFormatting.RED));
            return false;
         } else if (!canAffordTravel(var0, 0L, "家传送")) {
            return false;
         } else {
            var0.closeContainer();
            var0.teleportTo(var2, var1.x, var1.y, var1.z, Set.of(), var1.yaw, var1.pitch, true);
            chargeTravel(var0, 0L);
            var0.sendSystemMessage(Component.literal("已传送到家：" + var1.name + feeSuffix(var0, 0L)).withStyle(ChatFormatting.GREEN));
            return true;
         }
      } catch (Exception var3) {
         var0.sendSystemMessage(Component.literal("传送失败：" + var3.getMessage()).withStyle(ChatFormatting.RED));
         return false;
      }
   }

   /// 主页精品位：传送（免费，归属者获得收益）
   private static void teleportPremium(ServerPlayer var0, int var1) {
      int var2 = premiumSlotIndex(var1);
      List<MenuData.Home> var3 = MenuData.get().premiumHomes();
      if (var2 < 0 || var2 >= var3.size()) {
         var0.sendSystemMessage(Component.literal("这个精品位还是空的。在「我的家」对已设为公共的家 Shift+右键即可上架。").withStyle(ChatFormatting.GRAY));
         return;
      }

      MenuData.Home var4 = var3.get(var2);
      if (!checkCooldown(PUBLIC_COOLDOWNS, var0, PUBLIC_COOLDOWN_MS, "传送")) {
         return;
      }

      if (teleport(var0, var4)) {
         startCooldown(PUBLIC_COOLDOWNS, var0);
         rewardPremiumOwner(var0, var4);
      }
   }

   private static int premiumSlotIndex(int var0) {
      for (int var1 = 0; var1 < PREMIUM_SLOTS_MAIN.length; var1++) {
         if (PREMIUM_SLOTS_MAIN[var1] == var0) {
            return var1;
         }
      }

      return -1;
   }

   /// 精品位图标（含当前玩家已经贡献的收益）
   private static ItemStack premiumEntry(ServerPlayer var0, MenuData.Home var1) {
      List<String> var2 = new ArrayList<>();
      var2.add("归属者：" + (var1.publicOwner == null ? "未知" : var1.publicOwner));
      var2.add("世界：" + dimName(var1.dim));
      var2.add("坐标：" + (int)var1.x + ", " + (int)var1.y + ", " + (int)var1.z);
      long var3 = MenuData.get().premiumContributed(MenuData.get().ownerOf(var1), var0.getUUID());
      var2.add("你已为发布者带来收益：" + Economy.format(var3) + " / " + Economy.format(PREMIUM_VISITOR_CAP));
      var2.add("左键：免费传送（归属者得 " + Economy.format(PREMIUM_REWARD) + " 金币）");
      ItemStack var5 = button(Items.LODESTONE, "精品：" + var1.name, ChatFormatting.LIGHT_PURPLE, var2.toArray());
      var5.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
      return var5;
   }

   /// 有人传送到精品点，给归属者发金币（不限次数，自己传自己不给）
   private static void rewardPremiumOwner(ServerPlayer var0, MenuData.Home var1) {
      if (!var1.premium || isBingoServer()) {
         return;
      }

      UUID var2 = MenuData.get().ownerOf(var1);
      if (var2 == null || var2.equals(var0.getUUID())) {
         return;
      }

      MenuData var5 = MenuData.get();
      long var6 = var5.premiumContributed(var2, var0.getUUID());
      if (var6 >= PREMIUM_VISITOR_CAP) {
         var0.sendSystemMessage(
            Component.literal("你已经为这位发布者带来满 " + Economy.format(PREMIUM_VISITOR_CAP) + " 金币收益，之后的传送不再产生收益。").withStyle(ChatFormatting.GRAY)
         );
         return;
      }

      long var8 = Math.min(PREMIUM_REWARD, PREMIUM_VISITOR_CAP - var6);
      Economy.add(var2, var8);
      var5.addPremiumContributed(var2, var0.getUUID(), var8);
      var var3 = var0.level().getServer();
      if (var3 != null) {
         ServerPlayer var4 = var3.getPlayerList().getPlayer(var2);
         if (var4 != null) {
            var4.sendSystemMessage(
               Component.literal(var0.getGameProfile().name() + " 传送到了你的精品点「" + var1.name + "」，获得 " + Economy.format(var8) + " 金币")
                  .withStyle(ChatFormatting.GOLD)
            );
         }
      }
   }

   /// 我的家页面 Shift+右键：上架 / 下架精品位
   private static void togglePremium(MenuManager.SiftMenuContainer var0, ServerPlayer var1, int var2) {
      MenuData var3 = MenuData.get();
      List<MenuData.Home> var4 = var3.homes(var1.getUUID());
      int var5 = var0.pageIndex * 45 + var2;
      if (var5 >= var4.size()) {
         return;
      }

      MenuData.Home var6 = var4.get(var5);
      if (!var6.publicHome) {
         var1.sendSystemMessage(Component.literal("请先把「" + var6.name + "」设为公共传送点（右键），才能上架精品位。").withStyle(ChatFormatting.RED));
         return;
      }

      if (var6.premium) {
         var3.setHomePremium(var1.getUUID(), var6.name, false);
         Economy.add(var1, PREMIUM_REFUND);
         var1.sendSystemMessage(
            Component.literal("已下架精品位：" + var6.name + "，退还 " + Economy.format(PREMIUM_REFUND) + " 金币").withStyle(ChatFormatting.YELLOW)
         );
      } else {
         if (var3.premiumCount() >= PREMIUM_SLOTS) {
            var1.sendSystemMessage(Component.literal("精品位已满（" + PREMIUM_SLOTS + " / " + PREMIUM_SLOTS + "），等有人下架后再试。").withStyle(ChatFormatting.RED));
            return;
         }

         if (!Economy.chargeService(var1, PREMIUM_COST, "上架精品位")) {
            return;
         }

         var3.setHomePremium(var1.getUUID(), var6.name, true);
         var1.sendSystemMessage(
            Component.literal("已上架精品位：" + var6.name + "（主页可见，每次有人传送你得 " + Economy.format(PREMIUM_REWARD) + " 金币）")
               .withStyle(ChatFormatting.LIGHT_PURPLE)
         );
      }

      var0.setPage(var0.pageIndex);
   }

   /// 用管理员权限执行一条指令，@s 指向玩家（复用于大厅进出函数）
   private static void runAsPlayer(ServerPlayer var0, String var1) {
      var var2 = var0.level().getServer();
      if (var2 == null) {
         return;
      }

      var2.getCommands()
         .performPrefixedCommand(
            var2.createCommandSourceStack().withSuppressedOutput(),
            "execute as " + var0.getScoreboardName() + " at @s run " + var1
         );
   }

   private static boolean canAffordTravel(ServerPlayer var0, long var1, String var3) {
      if (!isBingoServer() && var1 > 0L) {
         if (Economy.balance(var0) >= var1) {
            return true;
         } else {
            var0.sendSystemMessage(Component.literal(var3 + "需要 " + Economy.format(var1) + " 金币，余额不足。").withStyle(ChatFormatting.RED));
            return false;
         }
      } else {
         return true;
      }
   }

   private static void chargeTravel(ServerPlayer var0, long var1) {
      if (!isBingoServer() && var1 > 0L) {
         Economy.chargeService(var0, var1, "传送");
      }
   }

   private static String feeSuffix(ServerPlayer var0, long var1) {
      return !isBingoServer() && var1 > 0L ? "（-" + Economy.format(var1) + " 金币）" : "";
   }

   private static boolean checkCooldown(Map<UUID, Long> var0, ServerPlayer var1, long var2, String var4) {
      long var5 = var0.getOrDefault(var1.getUUID(), 0L) + var2 - System.currentTimeMillis();
      if (var5 <= 0L) {
         return true;
      } else {
         long var7 = (var5 + 999L) / 1000L;
         var1.sendSystemMessage(Component.literal(var4 + "冷却中，还需等待 " + var7 / 60L + " 分 " + var7 % 60L + " 秒").withStyle(ChatFormatting.RED));
         return false;
      }
   }

   private static void startCooldown(Map<UUID, Long> var0, ServerPlayer var1) {
      var0.put(var1.getUUID(), System.currentTimeMillis());
   }

   public static void markPlayerTeleportDone(ServerPlayer var0) {
      if (isBingoServer()) {
         startCooldown(BINGO_PLAYER_COOLDOWNS, var0);
      }
   }

   private static List<MenuData.Death> recentDeaths(UUID var0) {
      ArrayList var1 = new ArrayList<>(MenuData.get().deaths(var0));
      var1.sort(Comparator.<MenuData.Death>comparingLong(var0x -> var0x.time).reversed());

      while (var1.size() > 5) {
         var1.remove(var1.size() - 1);
      }

      return var1;
   }

   private static void teleport(ServerPlayer var0, MenuData.Death var1) {
      try {
         ServerLevel var2 = levelOf(var0, var1.dim);
         if (var2 == null) {
            var0.sendSystemMessage(Component.literal("目标维度不存在：" + var1.dim).withStyle(ChatFormatting.RED));
            return;
         }

         if (!canAffordTravel(var0, 0L, "死亡地点传送")) {
            return;
         }

         var0.closeContainer();
         var0.teleportTo(var2, var1.x, var1.y, var1.z, Set.of(), var0.getYRot(), var0.getXRot(), true);
         chargeTravel(var0, 0L);
         var0.sendSystemMessage(Component.literal("已传送到死亡地点" + feeSuffix(var0, 0L)).withStyle(ChatFormatting.GREEN));
      } catch (Exception var3) {
         var0.sendSystemMessage(Component.literal("传送失败：" + var3.getMessage()).withStyle(ChatFormatting.RED));
      }
   }

   private static boolean randomTeleport(ServerPlayer var0) {
      ServerLevel var1 = var0.level();
      RandomSource var2 = var1.getRandom();

      for (int var3 = 0; var3 < 16; var3++) {
         int var4 = var0.getBlockX() + var2.nextInt(10001) - 5000;
         int var5 = var0.getBlockZ() + var2.nextInt(10001) - 5000;
         var1.getChunk(var4 >> 4, var5 >> 4);
         int var6 = var1.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, var4, var5);
         if (!var1.dimension().equals(Level.NETHER) || var6 < 120) {
            BlockPos var7 = new BlockPos(var4, var6, var5);
            BlockState var8 = var1.getBlockState(var7.below());
            BlockState var9 = var1.getBlockState(var7);
            BlockState var10 = var1.getBlockState(var7.above());
            if (var8.isSolid() && var9.isAir() && var10.isAir()) {
               if (!canAffordTravel(var0, 0L, "随机传送")) {
                  return false;
               }

               var0.closeContainer();
               var0.teleportTo(var1, (double)var4 + 0.5, (double)var6, (double)var5 + 0.5, Set.of(), var0.getYRot(), var0.getXRot(), true);
               chargeTravel(var0, 0L);
               var0.sendSystemMessage(
                  Component.literal("已随机传送到 (" + var4 + ", " + var6 + ", " + var5 + ")" + feeSuffix(var0, 0L)).withStyle(ChatFormatting.GREEN)
               );
               return true;
            }
         }
      }

      var0.sendSystemMessage(Component.literal("没有找到合适的位置，请再试一次。").withStyle(ChatFormatting.RED));
      return false;
   }

   private static ServerLevel levelOf(ServerPlayer var0, String var1) {
      ResourceKey var2 = ResourceKey.create(Registries.DIMENSION, Identifier.parse(var1));
      return var0.level().getServer().getLevel(var2);
   }

   private static int pages(int var0) {
      return Math.max(1, (var0 + 44) / 45);
   }

   private static int sellItemPages(int var0) {
      return Math.max(1, (var0 + 28 - 1) / 28);
   }

   private static int gridSlot(int var0) {
      int var1 = var0 / 7;
      int var2 = var0 % 7;
      return 9 * (var1 + 1) + var2 + 1;
   }

   private static int gridIndex(int var0) {
      int var1 = var0 / 9;
      int var2 = var0 % 9;
      return var1 >= 1 && var1 <= 4 && var2 >= 1 && var2 <= 7 ? (var1 - 1) * 7 + (var2 - 1) : -1;
   }

   private static int titleSlot(int var0) {
      if (var0 == 0) {
         return 19;
      } else {
         return var0 <= 6 ? 19 + var0 : 27 + (var0 - 6);
      }
   }

   private static int titleIndex(int var0) {
      if (var0 == 19) {
         return 0;
      } else if (var0 >= 20 && var0 <= 25) {
         return var0 - 19;
      } else {
         return var0 >= 28 && var0 <= 34 ? var0 - 28 + 7 : -1;
      }
   }

   private static int categoryPages(int var0) {
      return Math.max(1, (var0 + 28 - 1) / 28);
   }

   private static int bountyPages(int var0) {
      return Math.max(2, (var0 + 28 - 1) / 28);
   }

   private static ChatFormatting statusColor(String var0) {
      return switch (var0) {
         case "已完成" -> ChatFormatting.DARK_GRAY;
         case "已失败" -> ChatFormatting.RED;
         case "已被接取" -> ChatFormatting.RED;
         case "我的进行中" -> ChatFormatting.YELLOW;
         default -> ChatFormatting.GREEN;
      };
   }

   private static ChatFormatting difficultyColor(BountyCatalog.Difficulty var0) {
      return switch (var0) {
         case EASY -> ChatFormatting.WHITE;
         case NORMAL -> ChatFormatting.GREEN;
         case HARD -> ChatFormatting.GOLD;
         case EXTREME -> ChatFormatting.RED;
      };
   }

   private static int mailAdminPages(int var0) {
      return Math.max(1, (var0 + 21 - 1) / 21);
   }

   private static int mailAdminSlot(int var0) {
      if (var0 < 7) {
         return 19 + var0;
      } else {
         return var0 < 14 ? 28 + (var0 - 7) : 37 + (var0 - 14);
      }
   }

   private static int mailAdminIndex(int var0) {
      if (var0 >= 19 && var0 <= 25) {
         return var0 - 19;
      } else if (var0 >= 28 && var0 <= 34) {
         return var0 - 28 + 7;
      } else {
         return var0 >= 37 && var0 <= 43 ? var0 - 37 + 14 : -1;
      }
   }

   private static String colorName(String var0) {
      String var1 = var0 == null ? "" : var0;

      return switch (var1) {
         case "purple" -> "紫色";
         case "blue" -> "蓝色";
         case "rainbow" -> "彩色（炫彩）";
         case "gold" -> "金色";
         case "aqua" -> "青色";
         case "green" -> "绿色";
         case "yellow" -> "黄色";
         case "dark_red" -> "暗红色";
         case "red" -> "红色";
         default -> "白色";
      };
   }

   private static ChatFormatting colorFor(String var0) {
      String var1 = var0 == null ? "" : var0;

      return switch (var1) {
         case "purple" -> ChatFormatting.LIGHT_PURPLE;
         case "blue" -> ChatFormatting.BLUE;
         case "rainbow" -> ChatFormatting.GOLD;
         case "gold" -> ChatFormatting.GOLD;
         case "aqua" -> ChatFormatting.AQUA;
         case "green" -> ChatFormatting.GREEN;
         case "yellow" -> ChatFormatting.YELLOW;
         case "dark_red" -> ChatFormatting.DARK_RED;
         case "red" -> ChatFormatting.RED;
         default -> ChatFormatting.WHITE;
      };
   }

   private static List<ServerPlayer> onlinePlayers(ServerPlayer var0) {
      List<ServerPlayer> var1 = new ArrayList<>(var0.level().getServer().getPlayerList().getPlayers());
      var1.removeIf(var1x -> var1x == var0);
      var1.sort(Comparator.comparing((ServerPlayer var0x) -> var0x.getGameProfile().name(), String.CASE_INSENSITIVE_ORDER));
      return var1;
   }

   private static List<ServerPlayer> inviteCandidates(ServerPlayer var0) {
      MenuData.Party var1 = MenuData.get().partyOf(var0.getUUID());
      List<ServerPlayer> var2 = onlinePlayers(var0);
      if (var1 != null) {
         var2.removeIf(var1x -> var1.members.contains(var1x.getUUID().toString()));
      }

      return var2;
   }

   private static String dimName(String var0) {
      return switch (var0) {
         case "minecraft:overworld" -> "主世界";
         case "minecraft:the_nether" -> "下界";
         case "minecraft:the_end" -> "末地";
         case "twilightforest:twilight_forest" -> "暮色森林";
         case "sift_origins:the_sift" -> "筛界";
         case "sift:lobby" -> "主城大厅";
         default -> var0;
      };
   }

   private static ItemStack button(Item var0, String var1, ChatFormatting var2, Object... var3) {
      return button(var0, Component.literal(var1), var2, var3);
   }

   private static ItemStack button(Item var0, MutableComponent var1, ChatFormatting var2, Object... var3) {
      ItemStack var4 = new ItemStack(var0);
      var4.set(DataComponents.CUSTOM_NAME, var1.withStyle(var2));
      if (var3.length > 0) {
         ArrayList<Component> var5 = new ArrayList<>();

         for (Object var9 : var3) {
            var5.add(Component.literal(String.valueOf(var9)).withStyle(ChatFormatting.GRAY));
         }

         var4.set(DataComponents.LORE, new ItemLore(var5));
      }

      return var4;
   }

   private static Component loreLine(String var0, ChatFormatting var1) {
      return Component.literal(var0).withStyle(var1);
   }

   private static ItemStack itemWithLore(Item var0, String var1, ChatFormatting var2, List<Component> var3) {
      ItemStack var4 = new ItemStack(var0);
      var4.set(DataComponents.CUSTOM_NAME, Component.literal(var1).withStyle(var2));
      if (!var3.isEmpty()) {
         var4.set(DataComponents.LORE, new ItemLore(var3));
      }

      return var4;
   }

   private static ItemStack marketItem(Item var0, String... var1) {
      return marketItem(new ItemStack(var0), var1);
   }

   private static ItemStack marketItem(ItemStack var0, String... var1) {
      ItemStack var2 = var0.copy();
      if (var1.length > 0) {
         ArrayList var3 = new ArrayList();

         for (String var7 : var1) {
            var3.add(Component.literal(var7).withStyle(ChatFormatting.GRAY));
         }

         var2.set(DataComponents.LORE, new ItemLore(var3));
      }

      return var2;
   }

   private static ItemStack head(ServerPlayer var0, String var1, ChatFormatting var2, Object... var3) {
      ItemStack var4 = button(Items.PLAYER_HEAD, var1, var2, var3);
      var4.set(DataComponents.PROFILE, ResolvableProfile.createResolved(var0.getGameProfile()));
      return var4;
   }

   private static ItemStack buyButton(ServerPlayer var0, GlobalMarket.BuyEntry var1) {
      long var2 = GlobalMarket.remainingItems(var1);
      long var4 = GlobalMarket.remainingPlayerItems(var0, var1);
      long var6 = GlobalMarket.currentBuyPrice(var1);
      MutableComponent var8 = Component.literal("收购：").append(MarketItems.displayName(var1.item()));
      return button(
         var1.item(),
         var8,
         ChatFormatting.YELLOW,
         "当前收购价：" + Money.format(var6) + " 金币/个（基础价的 " + GlobalMarket.pricePercent(var1) + "%）",
         "每组（64 个）：" + Money.format(var6 * 64L) + " 金币",
         "基础收购价：" + Money.format(var1.price()) + " 金币/个",
         "市场饱和：" + GlobalMarket.marketPressurePercent(var1) + "%",
         "全服剩余：" + amountText(var2) + "（每日 " + var1.dailyStacks() + " 组）",
         "个人剩余：" + amountText(var4) + "（每日上限 " + var1.dailyStacksPerPlayer() + " 组）",
         "集中抛售会立刻压价，停售后价格逐步回升（约 8 小时恢复六成）",
         "1 组 = 64 个",
         "左键出售 1 个",
         "右键出售 1 组（64 个）",
         "Shift + 左键出售背包全部"
      );
   }

   private static String amountText(long var0) {
      long var2 = var0 / 64L;
      long var4 = var0 % 64L;
      return var4 == 0L ? var2 + " 组" : var2 + " 组 " + var4 + " 个";
   }

   private static String quantityText(int var0) {
      int var1 = var0 / 64;
      int var2 = var0 % 64;
      String var3 = String.format(Locale.ROOT, "%,d", var0) + " 个";
      if (var1 <= 0) {
         return var3;
      } else {
         return var2 == 0 ? var3 + "（" + var1 + " 组）" : var3 + "（" + var1 + " 组 + " + var2 + " 个）";
      }
   }

   private static ItemStack listingItem(ServerPlayer var0, MenuData.MarketListing var1, boolean var2) {
      ItemStack var3 = FreeMarket.stackOf(var1, var0.level().registryAccess());
      if (var3.isEmpty()) {
         var3 = new ItemStack(Items.BARRIER);
      } else {
         var3 = var3.copy();
      }

      long var4 = (long)var1.quantity * var1.unitPrice;
      MutableComponent var6 = Component.literal(var2 ? "出售：" : "收购：").append(MarketItems.displayName(var3)).append(Component.literal(" x" + var1.quantity));
      var3.set(DataComponents.CUSTOM_NAME, var6.withStyle(var2 ? ChatFormatting.AQUA : ChatFormatting.YELLOW));
      ArrayList var7 = new ArrayList();
      var7.add(Component.literal((var2 ? "卖家：" : "买家：") + (var1.ownerName == null ? "未知" : var1.ownerName)).withStyle(ChatFormatting.GRAY));
      var7.add(Component.literal("数量：" + quantityText(var1.quantity)).withStyle(ChatFormatting.GRAY));
      var7.add(Component.literal("单价：" + Economy.format(var1.unitPrice) + " 金币").withStyle(ChatFormatting.GRAY));
      var7.add(Component.literal("总价：" + Economy.format(var4) + " 金币").withStyle(ChatFormatting.GRAY));
      var7.add(Component.literal("剩余：" + FreeMarket.remainingText(var1.expires)).withStyle(ChatFormatting.GRAY));
      if (!var2) {
         var7.add(Component.literal("按物品类型匹配（背包/附魔内容不限）").withStyle(ChatFormatting.DARK_GRAY));
      }

      var7.add(Component.literal(var2 ? "左键买 1 个 / 右键买全部" : "左键卖 1 个 / 右键卖全部").withStyle(ChatFormatting.GREEN));
      var3.set(DataComponents.LORE, new ItemLore(var7));
      return var3;
   }

   private static ItemStack manageListingItem(ServerPlayer var0, MenuData.MarketListing var1) {
      boolean var2 = "SELL".equals(var1.type);
      ItemStack var3 = FreeMarket.stackOf(var1, var0.level().registryAccess());
      if (var3.isEmpty()) {
         var3 = new ItemStack(Items.BARRIER);
      } else {
         var3 = var3.copy();
         var3.setCount(Math.min(var1.quantity, Math.max(1, var3.getMaxStackSize())));
      }

      long var4 = (long)var1.quantity * var1.unitPrice;
      MutableComponent var6 = Component.literal(var2 ? "出售：" : "收购：").append(MarketItems.displayName(var3)).append(Component.literal(" x" + var1.quantity));
      var3.set(DataComponents.CUSTOM_NAME, var6.withStyle(var2 ? ChatFormatting.AQUA : ChatFormatting.YELLOW));
      ArrayList var7 = new ArrayList();
      var7.add(Component.literal("类型：" + (var2 ? "出售挂单" : "收购挂单")).withStyle(ChatFormatting.GRAY));
      var7.add(Component.literal("数量：" + quantityText(var1.quantity)).withStyle(ChatFormatting.GRAY));
      var7.add(Component.literal("单价：" + Economy.format(var1.unitPrice) + " 金币").withStyle(ChatFormatting.GRAY));
      var7.add(Component.literal("总价：" + Economy.format(var4) + " 金币").withStyle(ChatFormatting.GRAY));
      var7.add(Component.literal("剩余：" + FreeMarket.remainingText(var1.expires)).withStyle(ChatFormatting.GRAY));
      var7.add(Component.literal("左键：修改价格").withStyle(ChatFormatting.GREEN));
      var7.add(Component.literal("右键：修改数量").withStyle(ChatFormatting.GREEN));
      var7.add(Component.literal(var2 ? "Shift+左键：下架（物品退回背包）" : "Shift+左键：下架（退还冻结金币）").withStyle(ChatFormatting.RED));
      var3.set(DataComponents.LORE, new ItemLore(var7));
      return var3;
   }

   private static void fill(SimpleContainer var0) {
      for (int var1 = 0; var1 < 54; var1++) {
         var0.setItem(var1, filler());
      }
   }

   private static ItemStack mailItem(ServerPlayer var0, MenuData.Mail var1) {
      MenuData var2 = MenuData.get();
      boolean var3 = Mailbox.isAnnouncement(var1);
      boolean var4 = var2.isMailRead(var0.getUUID(), var1.id);
      ArrayList<String> var5 = new ArrayList<>();
      var5.add("类型：" + (var3 ? "服务器公告" : "系统补偿"));
      var5.add("发送者：" + (var1.sender != null && !var1.sender.isBlank() ? var1.sender : "系统"));
      var5.add("时间：" + TIME_FORMAT.format(Instant.ofEpochMilli(var1.created)));
      if (var3) {
         var5.add("状态：" + (var4 ? "已读" : "未读"));
         var5.add("左键：阅读公告全文");
      } else {
         boolean var6 = var2.isMailClaimed(var0.getUUID(), var1.id);
         var5.add("领取范围：" + Mailbox.audienceText(var1));
         var5.add("状态：" + (var6 ? "已领取" : "未领取"));

         for (MenuData.PendingItem var8 : var1.items) {
            var5.add("物品：" + MarketItems.name(MarketItems.item(var8.item)) + " x" + var8.count);
         }

         var5.add(var6 ? "已领取，无法重复领取" : "左键：领取补偿");
         if (var1.body != null && !var1.body.isBlank()) {
            var5.add("留言：" + var1.body);
         }
      }

      boolean var9 = var3 ? !var4 : !var2.isMailClaimed(var0.getUUID(), var1.id);
      return button(var3 ? Items.PAPER : Items.CHEST, var1.title, var9 ? ChatFormatting.GOLD : ChatFormatting.GRAY, var5.toArray(new String[0]));
   }

   private static ItemStack adminMailItem(ServerPlayer var0, MenuData.Mail var1) {
      boolean var2 = Mailbox.isAnnouncement(var1);
      ArrayList var3 = new ArrayList();
      var3.add("类型：" + (var2 ? "服务器公告" : "系统补偿"));
      if (!var2) {
         var3.add("领取范围：" + Mailbox.audienceText(var1));
         int var4 = MenuData.get().mailClaimCount(var1.id);
         var3.add(Mailbox.isTargeted(var1) ? "已领取：" + var4 + " / " + var1.recipientNames.size() + " 人" : "已领取人数：" + var4);

         for (MenuData.PendingItem var6 : var1.items) {
            var3.add("物品：" + MarketItems.name(MarketItems.item(var6.item)) + " x" + var6.count);
         }
      }

      var3.add("发送者：" + (var1.sender != null && !var1.sender.isBlank() ? var1.sender : "系统"));
      var3.add("时间：" + TIME_FORMAT.format(Instant.ofEpochMilli(var1.created)));
      var3.add("左键：查看内容");
      var3.add("Shift+左键：删除");
      return button(var2 ? Items.PAPER : Items.CHEST, var1.title, var2 ? ChatFormatting.GOLD : ChatFormatting.GREEN, var3.toArray(new String[0]));
   }

   public static enum Page {
      /// 由 config/siftmenu/menus.json 定义的独立页面
      CUSTOM,
      MAIN,
      BINGO,
      HOMES,
      DEATHS,
      RANDOM,
      PLAYERS,
      PARTY,
      PARTY_INVITE,
      PARTY_MEMBERS,
      GLOBAL_MARKET,
      GLOBAL_SELL,
      GLOBAL_SELL_ITEMS,
      GLOBAL_BUY,
      BOUNTY_HUB,
      BOUNTY,
      BOUNTY_DETAIL,
      MY_BOUNTY,
      FREE_MARKET,
      FREE_SELL_LIST,
      FREE_BUY_LIST,
      FREE_MANAGE,
      PENDING,
      TITLES,
      TITLE_COLOR,
      MINIGAMES,
      MAILBOX,
      MAIL_ADMIN,
      ADMIN_MAIN,
      ADMIN_SELL,
      ADMIN_BUY;
   }

   private static final class SiftMenuContainer extends AbstractContainerMenu {
      private final MenuManager.Page page;
      private final int category;
      private final SimpleContainer container;
      private final ServerPlayer owner;
      private final String customId;
      private int pageIndex;

      private SiftMenuContainer(int var1, Inventory var2, ServerPlayer var3, MenuManager.Page var4, int var5, int var6) {
         this(var1, var2, var3, var4, var5, var6, null);
      }

      private SiftMenuContainer(int var1, Inventory var2, ServerPlayer var3, MenuManager.Page var4, int var5, int var6, String var7) {
         super(MenuType.GENERIC_9x6, var1);
         this.page = var4;
         this.pageIndex = var5;
         this.category = var6;
         this.owner = var3;
         this.customId = var7;
         this.container = new SimpleContainer(54);

         for (int var7x = 0; var7x < 6; var7x++) {
            for (int var8 = 0; var8 < 9; var8++) {
               this.addSlot(new Slot(this.container, var8 + var7x * 9, 8 + var8 * 18, 18 + var7x * 18));
            }
         }

         this.addStandardInventorySlots(var2, 8, 139);
         MenuManager.fill(this.container);
         build(this.container, var3, var4, var5, var6, var7);
      }

      private void setPage(int var1) {
         if (var1 >= 0 && var1 != this.pageIndex) {
            this.pageIndex = var1;
            MenuManager.fill(this.container);
            build(this.container, this.owner, this.page, this.pageIndex, this.category, this.customId);
            if (this.owner.containerMenu == this) {
               this.broadcastChanges();
            }
         } else {
            if (var1 == this.pageIndex) {
               MenuManager.fill(this.container);
               build(this.container, this.owner, this.page, this.pageIndex, this.category, this.customId);
               if (this.owner.containerMenu == this) {
                  this.broadcastChanges();
               }
            }
         }
      }

      public void clicked(int var1, int var2, ContainerInput var3, Player var4) {
         if (var4 instanceof ServerPlayer var5 && var1 >= 0 && var1 < 54) {
            if (var3 == ContainerInput.PICKUP) {
               MenuManager.handleClick(this, var5, var1, var2 == 1);
            } else if (var3 == ContainerInput.QUICK_MOVE) {
               MenuManager.handleShiftClick(this, var5, var1, var2 == 1);
            }
         }

         if (var4.containerMenu == this) {
            this.broadcastChanges();
         }
      }

      public ItemStack quickMoveStack(Player var1, int var2) {
         return ItemStack.EMPTY;
      }

      public boolean stillValid(Player var1) {
         return true;
      }

      private static void build(SimpleContainer var0, ServerPlayer var1, MenuManager.Page var2, int var3, int var4) {
         build(var0, var1, var2, var3, var4, null);
      }

      private static void build(SimpleContainer var0, ServerPlayer var1, MenuManager.Page var2, int var3, int var4, String varCustom) {
         MenuData var5 = MenuData.get();
         UUID var6 = var1.getUUID();
         switch (var2) {
            case CUSTOM:
               CustomMenus.render(var0, var1, varCustom);
               break;
            case MAIN:
               if (Titles.isAdmin(var1)) {
                  var0.setItem(4, MenuManager.button(Items.BOOK, "筛界菜单", ChatFormatting.GOLD, "快捷打开：按快捷键（默认 G 键）", "右键菜单书", "右键此书本打开管理员后台"));
               } else {
                  var0.setItem(4, MenuManager.button(Items.BOOK, "筛界菜单", ChatFormatting.GOLD, "快捷打开：按快捷键（默认 G 键）", "或者右键菜单书"));
               }

               int var43 = Mailbox.unread(var6, var1.getGameProfile().name());
               var0.setItem(
                  13,
                  MenuManager.button(Items.CHEST, "邮箱", ChatFormatting.GOLD, var43 > 0 ? "有 " + var43 + " 封未读 / 未领取邮件" : "暂无未读邮件", "服务器公告与补偿都在这里", "点击查看邮件")
               );
               var0.setItem(19, MenuManager.button(Items.GRASS_BLOCK, "创建家 / 我的家", ChatFormatting.GREEN, "创建、传送、管理你的家", "可把家设为公共传送点", "最多可以创建 64 个家"));
               var0.setItem(21, MenuManager.button(Items.SKELETON_SKULL, "死亡地点传送", ChatFormatting.RED, "查看最近 5 次死亡地点", "点击即可传送过去"));
               var0.setItem(23, MenuManager.button(Items.ENDER_PEARL, "世界传送", ChatFormatting.LIGHT_PURPLE, "随机传送 / 大厅传送 / 精品传送点 / 公共传送点", "公共点可在「我的家」中设置"));
               var0.setItem(25, MenuManager.button(Items.PLAYER_HEAD, "玩家之间传送", ChatFormatting.AQUA, "列出当前在线玩家", "点击即可传送到对方身边"));
               var0.setItem(
                  37, MenuManager.button(Items.PAPER, "悬赏任务", ChatFormatting.YELLOW, "全服共享悬赏板，每天 0 点刷新 56 个任务", "每人每天最多接取 5 个", "左侧：悬赏任务　右侧：我的悬赏", "完成目标后领取金币")
               );
               var0.setItem(29, MenuManager.button(Items.EMERALD, "全球市场", ChatFormatting.GREEN, "服务器出售与收购物资", "收购每日有全服限额"));
               var0.setItem(31, MenuManager.button(Items.GOLD_INGOT, "自由市场", ChatFormatting.YELLOW, "玩家之间发布收购和出售挂单", "挂单持续 7 天"));
               var0.setItem(33, MenuManager.button((Item)Items.BANNER.pick(DyeColor.WHITE), "创建队伍", ChatFormatting.YELLOW, "创建、邀请、管理你的队伍", "队友伤害可自由开关"));
               var0.setItem(39, MenuManager.button(Items.NAME_TAG, "我的头衔", ChatFormatting.LIGHT_PURPLE, "设置自定义头衔与颜色", "完成成就可解锁更多头衔", "佩戴后聊天名字前会显示"));
               var0.setItem(41, MenuManager.button(Items.MAP, "小游戏", ChatFormatting.AQUA, "服务器娱乐玩法入口", "Bingo、起床战争等", "点击查看小游戏列表"));
               String sponsorHint = MenuConfig.sponsorUrl().isBlank() ? "赞助链接准备中，敬请期待" : "点击查看赞助链接";
               var0.setItem(43, MenuManager.button(Items.CLOCK, "服务器赞助", ChatFormatting.GOLD, "支持服务器运营", sponsorHint));
               var0.setItem(49, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case BINGO:
               var0.setItem(
                  4,
                  MenuManager.button(
                     Items.BOOK,
                     "Bingo 玩法介绍",
                     ChatFormatting.GOLD,
                     "目标：在 5×5 卡片上率先连成一条线（横 / 竖 / 斜）",
                     "卡片只包含原版物品与原版成就（无模组内容）",
                     "收集到目标物品后会自动点亮对应格子",
                     "达成成就后对应格子也会自动点亮",
                     "比赛中可以使用本菜单的三个功能",
                     "本服没有金币与市场系统"
                  )
               );
               var0.setItem(20, MenuManager.button(Items.ENDER_PEARL, "世界随机传送", ChatFormatting.LIGHT_PURPLE, "点击立即在当前维度随机传送", "范围：以你为中心 5000 格", "冷却时间：10 分钟"));
               var0.setItem(22, MenuManager.button(Items.PLAYER_HEAD, "玩家之间传送", ChatFormatting.AQUA, "查看当前在线玩家", "点击即可传送到对方身边", "冷却时间：5 分钟"));
               if (BingoReward.hasRecentWin(var1.getUUID())) {
                  var0.setItem(
                     24,
                     MenuManager.button(
                        Items.GOLD_INGOT, "胜利返回主服", ChatFormatting.GOLD, "你赢下了本局 Bingo", "回到主服可领取 " + Economy.format(BingoReward.AMOUNT) + " 金币", "点击后立即传送"
                     )
                  );
               } else {
                  var0.setItem(24, MenuManager.button(Items.OAK_DOOR, "返回主服（弃权）", ChatFormatting.RED, "返回主服即视为放弃比赛", "本局按弃权处理", "点击后立即传送"));
               }

               var0.setItem(49, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case HOMES:
               List<MenuData.Home> var42 = var5.homes(var6);
               int var65 = MenuManager.pages(var42.size());
               int var86 = var3 * 45;
               long var102 = var42.stream().filter(var0x -> var0x.publicHome).count();

               for (int var129 = 0; var129 < 45; var129++) {
                  int var140 = var86 + var129;
                  if (var140 >= var42.size()) {
                     break;
                  }

                  MenuData.Home var147 = (MenuData.Home)var42.get(var140);
                  boolean var152 = var147.publicHome;
                  List<String> var158 = new ArrayList<>();
                  var158.add("世界：" + MenuManager.dimName(var147.dim));
                  var158.add("坐标：" + (int)var147.x + ", " + (int)var147.y + ", " + (int)var147.z);
                  var158.add(var152 ? "状态：已是公共传送点（其他玩家可在「世界传送」看到）" : "状态：私有，仅自己可见");
                  var158.add(
                     var147.premium
                        ? "精品位：已上架（主页可见，每次被传送得 100 金币）"
                        : (var152 ? "精品位：未上架（Shift+右键上架，2000 金币）" : "精品位：需先设为公共传送点")
                  );
                  var158.add(var152 ? "右键：下架传送点" : "右键：设为公共传送点（设为公共后床会变成青色）");
                  if (var152) {
                     var158.add(var147.premium ? "Shift+右键：下架精品位（退 1000 金币）" : "Shift+右键：上架精品位（2000 金币）");
                  }

                  var158.add("左键：传送");
                  var158.add("Shift+左键：删除");
                  ItemStack var157 = MenuManager.button(
                     (Item)Items.BED.pick(var152 ? DyeColor.CYAN : DyeColor.RED),
                     var147.name,
                     var152 ? ChatFormatting.AQUA : ChatFormatting.GREEN,
                     var158.toArray()
                  );
                  if (var152) {
                     var157.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                  }

                  var0.setItem(var129, var157);
               }

               if (var42.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "还没有创建任何家", ChatFormatting.GRAY, "点击下方「创建家」开始"));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(
                  48,
                  MenuManager.button(
                     Items.PAPER,
                     "第 " + (var3 + 1) + " / " + var65 + " 页",
                     ChatFormatting.WHITE,
                     "共 " + var42.size() + " 个家（上限 64）",
                     "其中公共传送点：" + var102 + " 个"
                  )
               );
               var0.setItem(49, MenuManager.button(Items.EMERALD, "创建家", ChatFormatting.GREEN, "记录当前位置为新的家", "点击后在聊天栏输入名称"));
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var65 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(51, MenuManager.button(Items.BOOK, "使用说明", ChatFormatting.WHITE, "左键：传送", "右键：设为 / 下架公共传送点", "Shift+右键：上架 / 下架精品位", "Shift+左键：删除家", "公共点会变成青色床并出现在「世界传送」"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case DEATHS:
               List var41 = MenuManager.recentDeaths(var6);

               for (int var64 = 0; var64 < var41.size(); var64++) {
                  MenuData.Death var85 = (MenuData.Death)var41.get(var64);
                  var0.setItem(
                     11 + var64,
                     MenuManager.button(
                        Items.SKELETON_SKULL,
                        "死亡地点 " + (var64 + 1),
                        ChatFormatting.RED,
                        "世界：" + MenuManager.dimName(var85.dim),
                        "坐标：" + (int)var85.x + ", " + (int)var85.y + ", " + (int)var85.z,
                        "时间：" + MenuManager.TIME_FORMAT.format(Instant.ofEpochMilli(var85.time)),
                        "点击传送"
                     )
                  );
               }

               if (var41.isEmpty()) {
                  var0.setItem(13, MenuManager.button(Items.BARRIER, "暂无死亡记录", ChatFormatting.GRAY, "死亡后会自动记录最近 5 次"));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case RANDOM:
               List var40 = var5.publicHomes();
               int var63 = Math.max(1, (var40.size() + 18 - 1) / 18);
               int var84 = var3 * 18;
               var0.setItem(4, MenuManager.button(Items.BOOK, "世界传送", ChatFormatting.GOLD, "左侧：随机传送（当前维度）", "中间：主城大厅传送", "中排：精品传送点（7 个）", "下方：玩家创建的公共传送点"));
               ItemStack var101 = MenuManager.button(
                  Items.ENDER_PEARL,
                  "随机传送",
                  ChatFormatting.LIGHT_PURPLE,
                  "当前维度：" + MenuManager.dimName(var1.level().dimension().identifier().toString()),
                  "在以你为中心的 5000 格内传送",
                  "冷却 5 分钟",
                  "点击后立即传送"
               );
               var101.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
               var0.setItem(11, var101);
               boolean inLobby = LOBBY_DIM.equals(var1.level().dimension().identifier().toString());
               var0.setItem(
                  13,
                  inLobby
                     ? MenuManager.button(Items.DARK_OAK_DOOR, "离开主城大厅", ChatFormatting.GOLD, "返回进入大厅前的位置", "点击后立即传送")
                     : MenuManager.button(Items.BELL, "主城大厅传送", ChatFormatting.GOLD, "传送到服务器主城大厅", "离开时回到进入前的坐标", "大厅内禁止建造、禁止生物生成", "冷却 5 分钟")
               );
               List<MenuData.Home> premiumNow = var5.premiumHomes();
               var0.setItem(
                  15,
                  MenuManager.button(
                     Items.BEACON,
                     "精品传送点 " + premiumNow.size() + " / " + PREMIUM_SLOTS,
                     ChatFormatting.LIGHT_PURPLE,
                     "每页上方 7 个精品位",
                     "上架 2000 金币，取消退 1000 金币",
                     "每次有人传送，归属者得 100 金币",
                     "同一访客最多为发布者带来 500 金币（不分精品点数量）"
                  )
               );

               for (int premiumIndex = 0; premiumIndex < PREMIUM_SLOTS && premiumIndex < premiumNow.size(); premiumIndex++) {
                  var0.setItem(PREMIUM_SLOTS_MAIN[premiumIndex], premiumEntry(var1, premiumNow.get(premiumIndex)));
               }

               for (int var118 = 0; var118 < 18; var118++) {
                  int var128 = var84 + var118;
                  if (var128 >= var40.size()) {
                     break;
                  }

                  MenuData.PublicHome var139 = (MenuData.PublicHome)var40.get(var128);
                  MenuData.Home var146 = var139.home();
                  ItemStack var151 = MenuManager.button(
                     Items.LODESTONE,
                     "公共点：" + var146.name,
                     ChatFormatting.AQUA,
                     "创建者：" + var139.ownerName(),
                     "世界：" + MenuManager.dimName(var146.dim),
                     "坐标：" + (int)var146.x + ", " + (int)var146.y + ", " + (int)var146.z,
                     "左键传送"
                  );
                  var0.setItem(27 + var118, var151);
               }

               if (var40.isEmpty()) {
                  var0.setItem(35, MenuManager.button(Items.BARRIER, "暂无公共传送点", ChatFormatting.GRAY, "在「我的家」中右键自己的家", "即可设为公共传送点"));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(
                  48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var63 + " 页", ChatFormatting.WHITE, "共 " + var40.size() + " 个公共传送点")
               );
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var63 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case PLAYERS:
               List var39 = MenuManager.onlinePlayers(var1);

               for (int var62 = 0; var62 < Math.min(45, var39.size()); var62++) {
                  ServerPlayer var83 = (ServerPlayer)var39.get(var62);
                  var0.setItem(
                     var62,
                     MenuManager.head(
                        var83,
                        var83.getGameProfile().name(),
                        ChatFormatting.AQUA,
                        "世界：" + MenuManager.dimName(var83.level().dimension().identifier().toString()),
                        "左键：传送到他身边（对方需同意）",
                        "右键：邀请他传送到你这里（对方需同意）"
                     )
                  );
               }

               if (var39.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "当前没有其他在线玩家", ChatFormatting.GRAY));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case PARTY:
               buildParty(var0, var1, var5);
               break;
            case PARTY_INVITE:
               List var38 = MenuManager.inviteCandidates(var1);

               for (int var61 = 0; var61 < Math.min(45, var38.size()); var61++) {
                  ServerPlayer var82 = (ServerPlayer)var38.get(var61);
                  var0.setItem(var61, MenuManager.head(var82, var82.getGameProfile().name(), ChatFormatting.YELLOW, "点击发送队伍邀请", "对方可在聊天栏点击 [✓] 或 [✕]"));
               }

               if (var38.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "没有可邀请的玩家", ChatFormatting.GRAY));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回队伍菜单", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case PARTY_MEMBERS:
               MenuData.Party var37 = var5.partyOf(var6);
               if (var37 == null) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "你还没有队伍", ChatFormatting.GRAY));
               } else {
                  boolean var60 = var37.leader.equals(var6.toString());

                  for (int var81 = 0; var81 < Math.min(45, var37.members.size()); var81++) {
                     String var100 = var37.members.get(var81);
                     ServerPlayer var117 = var1.level().getServer().getPlayerList().getPlayer(UUID.fromString(var100));
                     String var127 = var117 != null ? var117.getGameProfile().name() : var100.substring(0, 8);
                     boolean var138 = var100.equals(var37.leader);
                     boolean var145 = var100.equals(var6.toString());
                     ArrayList<String> var150 = new ArrayList<>();
                     var150.add(var138 ? "队长" : "成员");
                     if (var117 == null) {
                        var150.add("离线");
                     }

                     if (var60 && !var145) {
                        var150.add("左键转让队长");
                        var150.add("右键踢出队伍");
                     }

                     ItemStack var156 = var117 != null
                        ? MenuManager.head(var117, var127, ChatFormatting.YELLOW, var150.toArray(new String[0]))
                        : MenuManager.button(Items.PLAYER_HEAD, var127, ChatFormatting.YELLOW, var150.toArray(new String[0]));
                     var0.setItem(var81, var156);
                  }
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回队伍菜单", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case GLOBAL_MARKET:
               var0.setItem(
                  4, MenuManager.button(Items.BOOK, "全球市场", ChatFormatting.GOLD, "购买物资：左键买 1 个，右键买 1 组", "收购物资：左键卖背包全部，右键卖 1 组", "收购每日有全服限额", "统计板位置：按 H 键调整")
               );
               var0.setItem(21, MenuManager.button(Items.EMERALD, "购买物资", ChatFormatting.GREEN, "从服务器购买物资", "点击选择分类", "左键买 1 个 / 右键买 1 组"));
               var0.setItem(23, MenuManager.button(Items.HOPPER, "收购物资", ChatFormatting.YELLOW, "向服务器出售物资", "点击查看价格和剩余额度", "左键卖全部 / 右键卖 1 组"));
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case GLOBAL_SELL:
               List var36 = GlobalMarket.categories();
               int var59 = MenuManager.categoryPages(var36.size());
               int var80 = var3 * 28;

               for (int var99 = 0; var99 < 28; var99++) {
                  int var116 = var80 + var99;
                  if (var116 >= var36.size()) {
                     break;
                  }

                  GlobalMarket.Category var126 = (GlobalMarket.Category)var36.get(var116);
                  var0.setItem(
                     MenuManager.gridSlot(var99),
                     MenuManager.button(var126.icon(), var126.name(), ChatFormatting.GREEN, "共 " + var126.entries().size() + " 种物品", "点击查看并购买")
                  );
               }

               if (var36.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "暂无出售分类", ChatFormatting.GRAY));
               }

               var0.setItem(
                  4, MenuManager.button(Items.BOOK, "选择分类", ChatFormatting.GOLD, "共 " + var36.size() + " 个分类", "家具按木材与材质细分", "建筑材料含混凝土、陶瓦、玻璃等", "点击分类查看物品与售价")
               );
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回全球市场", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var59 + " 页", ChatFormatting.WHITE, "共 " + var36.size() + " 个分类"));
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var59 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case GLOBAL_SELL_ITEMS:
               List var35 = GlobalMarket.categories();
               if (var4 >= 0 && var4 < var35.size()) {
                  GlobalMarket.Category var58 = (GlobalMarket.Category)var35.get(var4);
                  List var79 = var58.entries();
                  int var98 = MenuManager.sellItemPages(var79.size());
                  int var115 = var3 * 28;

                  for (int var125 = 0; var125 < 28; var125++) {
                     int var137 = var115 + var125;
                     if (var137 >= var79.size()) {
                        break;
                     }

                     GlobalMarket.Entry var144 = (GlobalMarket.Entry)var79.get(var137);
                     var0.setItem(
                        MenuManager.gridSlot(var125),
                        MenuManager.marketItem(var144.stack(), "售价：" + Economy.format(var144.price()) + " 金币/个", "左键购买 1 个", "右键购买 1 组")
                     );
                  }

                  if (var79.isEmpty()) {
                     var0.setItem(22, MenuManager.button(Items.BARRIER, "暂无物品", ChatFormatting.GRAY));
                  }

                  var0.setItem(49, MenuManager.button(Items.BOOK, var58.name(), ChatFormatting.GREEN, "共 " + var79.size() + " 种物品", "左键购买 1 个 / 右键购买 1 组"));
                  var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
                  var0.setItem(
                     48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var98 + " 页", ChatFormatting.WHITE, "共 " + var79.size() + " 种物品")
                  );
                  var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var98 - 1 ? "点击翻页" : "已经是最后一页"));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回分类", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case GLOBAL_BUY:
               List var34 = GlobalMarket.buyEntries();
               int var57 = MenuManager.categoryPages(var34.size());
               int var78 = var3 * 28;

               for (int var97 = 0; var97 < 28; var97++) {
                  int var114 = var78 + var97;
                  if (var114 >= var34.size()) {
                     break;
                  }

                  var0.setItem(MenuManager.gridSlot(var97), MenuManager.buyButton(var1, (GlobalMarket.BuyEntry)var34.get(var114)));
               }

               if (var34.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "暂无可收购物品", ChatFormatting.GRAY, "管理员还没有设置收购项目"));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回全球市场", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var57 + " 页", ChatFormatting.WHITE, "共 " + var34.size() + " 种收购物品"));
               var0.setItem(
                  49, MenuManager.button(Items.BOOK, "收购说明", ChatFormatting.GOLD, "服务器每天限量收购以下物品", "全服和个人都有每日上限，0 点重置", "左键：出售背包里的全部同类物品", "右键：出售 1 组（64 个）")
               );
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var57 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case BOUNTY_HUB:
               int var33 = BountyService.takenBy(var5, var6).size();
               var0.setItem(21, MenuManager.button(Items.PAPER, "悬赏任务", ChatFormatting.YELLOW, "全服共享悬赏板，每天 0 点刷新 56 个任务", "每人每天最多接取 5 个", "点击查看全部悬赏"));
               var0.setItem(23, MenuManager.button(Items.WRITABLE_BOOK, "我的悬赏", ChatFormatting.YELLOW, "进行中：" + var33 + " 个", "查看进度、奖励与剩余目标", "点击打开"));
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case BOUNTY:
               BountyService.ensureBoard(var5);
               List var32 = var5.bountyBoard;
               int var56 = MenuManager.bountyPages(var32.size());

               for (int var77 = 0; var77 < 28; var77++) {
                  int var96 = var3 * 28 + var77;
                  if (var96 >= var32.size()) {
                     break;
                  }

                  MenuData.BountyEntry var113 = (MenuData.BountyEntry)var32.get(var96);
                  BountyCatalog.Bounty var124 = BountyCatalog.byId(var113.bountyId);
                  if (var124 != null) {
                     String var136 = BountyService.statusText(var113, var6);
                     long var143 = Math.min(BountyService.progress(var6, var124.id()), var124.amount());
                     ArrayList var155 = new ArrayList();
                     var155.add(MenuManager.loreLine("难度：" + var124.difficulty().label(), ChatFormatting.GRAY));
                     var155.add(MenuManager.loreLine("目标：" + var124.goalText(), ChatFormatting.GRAY));
                     var155.add(MenuManager.loreLine("奖励：" + Economy.format(var124.rewardCents()) + " 金币", ChatFormatting.GRAY));
                     var155.add(MenuManager.loreLine("状态：" + BountyService.statusDetail(var113, var1), MenuManager.statusColor(var136)));
                     if (BountyService.isMine(var113, var6)) {
                        var155.add(MenuManager.loreLine("我的进度：" + var143 + " / " + var124.amount(), ChatFormatting.GRAY));
                     }

                     var155.add(MenuManager.loreLine("点击查看详情", ChatFormatting.GRAY));
                     var0.setItem(
                        MenuManager.gridSlot(var77),
                        MenuManager.itemWithLore(Items.PAPER, "#" + var113.boardId + " " + var124.title(), MenuManager.statusColor(var136), var155)
                     );
                  }
               }

               var0.setItem(4, MenuManager.button(Items.PAPER, "全服悬赏板", ChatFormatting.GOLD, "全服共用一块悬赏板，每天 0 点整板刷新 56 个悬赏", "每人每天最多接取 5 个", "极限悬赏每天最多 2 个"));
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回悬赏", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var56 + " 页", ChatFormatting.WHITE, "共 " + var32.size() + " 个悬赏"));
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var56 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case BOUNTY_DETAIL:
               BountyService.ensureBoard(var5);
               MenuData.BountyEntry var31 = BountyService.entry(var5, var4);
               BountyCatalog.Bounty var55 = var31 == null ? null : BountyCatalog.byId(var31.bountyId);
               if (var31 != null && var55 != null) {
                  long var76 = Math.min(BountyService.progress(var6, var55.id()), var55.amount());
                  String var112 = BountyService.statusText(var31, var6);
                  boolean var123 = BountyService.isMine(var31, var6);
                  boolean var135 = var123 && "submit_item".equals(var55.trigger()) && !BountyService.completed(var6, var55);
                  ArrayList var142 = new ArrayList();
                  var142.add(MenuManager.loreLine("编号：#" + var31.boardId, ChatFormatting.GRAY));
                  var142.add(MenuManager.loreLine("难度：" + var55.difficulty().label(), ChatFormatting.GRAY));
                  var142.add(MenuManager.loreLine("目标：" + var55.goalText(), ChatFormatting.GRAY));
                  var142.add(MenuManager.loreLine("说明：" + var55.desc(), ChatFormatting.GRAY));
                  var142.add(MenuManager.loreLine("进度：" + var76 + " / " + var55.amount(), ChatFormatting.GRAY));
                  var142.add(MenuManager.loreLine("奖励：" + Economy.format(var55.rewardCents()) + " 金币", ChatFormatting.GRAY));
                  var142.add(MenuManager.loreLine("状态：" + BountyService.statusDetail(var31, var1), MenuManager.statusColor(var112)));
                  var0.setItem(23, MenuManager.itemWithLore(Items.PAPER, "悬赏 #" + var31.boardId, MenuManager.difficultyColor(var55.difficulty()), var142));
                  if (var135) {
                     Item var149 = MarketItems.item(var55.match());
                     String var154 = var149 == null ? var55.match() : MarketItems.name(var149);
                     int var158 = var149 == null ? 0 : MarketItems.count(var1, var149);
                     var0.setItem(
                        22,
                        MenuManager.button(
                           Items.HOPPER, "提交物品", ChatFormatting.AQUA, "需要：" + var55.amount() + " 个" + var154, "当前持有：" + var158 + " 个", "一次性交齐后即可领取奖励", "点击提交"
                        )
                     );
                  }

                  if (var123 && BountyService.completed(var6, var55)) {
                     var0.setItem(
                        21, MenuManager.button(Items.GOLD_INGOT, "领取奖励", ChatFormatting.GOLD, "奖励：" + Economy.format(var55.rewardCents()) + " 金币", "点击领取")
                     );
                  } else if (BountyService.available(var31)) {
                     var0.setItem(21, MenuManager.button(Items.EMERALD, "接取悬赏", ChatFormatting.GREEN, "接取后开始累计进度", "每人每天最多 5 个", "点击接取"));
                  } else if (var123) {
                     var0.setItem(21, MenuManager.button(Items.CLOCK, "目标未完成", ChatFormatting.YELLOW, "进度：" + var76 + " / " + var55.amount(), "完成后回来领取奖励"));
                  }

                  if (var123) {
                     var0.setItem(24, MenuManager.button(Items.BARRIER, "放弃悬赏", ChatFormatting.RED, "放弃后任务回到板上，可被其他玩家接取", "已消耗的当日接取次数不退还", "点击放弃"));
                  }
               } else {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "悬赏已失效", ChatFormatting.GRAY, "这个悬赏已经不在今天的板上了"));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回列表", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case MY_BOUNTY:
               BountyService.ensureBoard(var5);
               List var30 = BountyService.takenBy(var5, var6);

               for (int var54 = 0; var54 < 28 && var54 < var30.size(); var54++) {
                  MenuData.BountyEntry var75 = (MenuData.BountyEntry)var30.get(var54);
                  BountyCatalog.Bounty var95 = BountyCatalog.byId(var75.bountyId);
                  if (var95 != null) {
                     long var111 = Math.min(BountyService.progress(var6, var95.id()), var95.amount());
                     long var134 = Math.max(0L, var95.amount() - var111);
                     boolean var148 = var111 >= var95.amount();
                     ArrayList var153 = new ArrayList();
                     var153.add("难度：" + var95.difficulty().label());
                     var153.add("目标：" + var95.goalText());
                     var153.add("进度：" + var111 + " / " + var95.amount());
                     var153.add(var148 ? "剩余目标：已完成，可领取奖励" : "剩余目标：" + var134);
                     var153.add("奖励：" + Economy.format(var95.rewardCents()) + " 金币");
                     var153.add("状态：" + (var148 ? "可领取" : "进行中"));
                     var153.add("点击查看详情 / 放弃");
                     var0.setItem(
                        MenuManager.gridSlot(var54),
                        MenuManager.button(
                           Items.MAP,
                           "#" + var75.boardId + " " + var95.title(),
                           var148 ? ChatFormatting.GOLD : MenuManager.difficultyColor(var95.difficulty()),
                           var153.toArray(new String[0])
                        )
                     );
                  }
               }

               if (var30.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "还没有进行中的悬赏", ChatFormatting.GRAY, "去悬赏板接取任务吧", "每人每天最多接取 5 个"));
               }

               var0.setItem(
                  4,
                  MenuManager.button(Items.WRITABLE_BOOK, "我的悬赏", ChatFormatting.YELLOW, "进行中：" + var30.size() + " 个", "每人每天最多接取 5 个（极限 2 个）", "完成后在详情页领取奖励")
               );
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回悬赏", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case FREE_MARKET:
               int var29 = FreeMarket.activeListings("SELL").size();
               int var53 = FreeMarket.activeListings("BUY").size();
               var0.setItem(
                  4,
                  MenuManager.button(
                     Items.BOOK,
                     "自由市场",
                     ChatFormatting.GOLD,
                     "玩家之间发布收购/出售挂单",
                     "挂单持续 7 天",
                     "发布时请手持物品并在聊天栏输入 数量 单价",
                     "每单挂单费 " + Economy.format(FreeMarket.LISTING_FEE) + " 金币",
                     "成交税 5%（单笔低于 " + Economy.format(FreeMarket.TAX_MIN_TOTAL) + " 免税）",
                     "成交后自动结算"
                  )
               );
               var0.setItem(19, MenuManager.button(Items.CHEST, "出售中的物品", ChatFormatting.AQUA, "当前 " + var29 + " 条挂单", "点击浏览玩家出售的物品", "左键买 1 个 / 右键买全部"));
               var0.setItem(21, MenuManager.button(Items.HOPPER, "收购中的物品", ChatFormatting.YELLOW, "当前 " + var53 + " 条挂单", "点击浏览玩家的收购需求", "左键卖 1 个 / 右键卖全部"));
               var0.setItem(
                  23,
                  MenuManager.button(
                     Items.WRITABLE_BOOK,
                     "发布出售挂单",
                     ChatFormatting.GREEN,
                     "出售背包中多余的物资",
                     "包括附魔书等带属性的物品",
                     "手持物品后点击，输入 数量 单价",
                     "收取 " + Economy.format(FreeMarket.LISTING_FEE) + " 金币挂单费",
                     "单次最多 9 组（576 个）"
                  )
               );
               var0.setItem(
                  25,
                  MenuManager.button(
                     Items.PAPER,
                     "发布收购挂单",
                     ChatFormatting.YELLOW,
                     "发布你想收购的物品",
                     "手持样品后点击，输入 数量 单价",
                     "单次最多 1000 组（64,000 个）",
                     "挂单费 " + Economy.format(FreeMarket.LISTING_FEE) + " 金币，另冻结货款"
                  )
               );
               var0.setItem(29, MenuManager.button(Items.ENDER_CHEST, "商品管理", ChatFormatting.GOLD, "管理自己发布的全部挂单", "出售与收购挂单都能改价、改数量、下架", "下架的出售物品退回背包，收购金币退还"));
               int var74 = MenuData.get().pendingItems(var6).size();
               var0.setItem(
                  31,
                  MenuManager.button(
                     Items.SHULKER_BOX,
                     "待领取物品",
                     ChatFormatting.LIGHT_PURPLE,
                     var74 > 0 ? "有 " + var74 + " 项物品待领取" : "暂无待领取物品",
                     "背包满时成交的物品会暂存在这里",
                     "点击领取，装不下的会继续保留"
                  )
               );
               var0.setItem(
                  33,
                  MenuManager.button(
                     Items.BOOK,
                     "自由市场规则",
                     ChatFormatting.GRAY,
                     "挂单持续 7 天，到期自动退回",
                     "每单挂单费 " + Economy.format(FreeMarket.LISTING_FEE) + " 金币，不退还",
                     "成交税 5%，单笔低于 " + Economy.format(FreeMarket.TAX_MIN_TOTAL) + " 金币免税",
                     "出售挂单到期退回物品",
                     "收购挂单到期退还金币",
                     "出售单最多 9 组，收购单最多 1000 组（64,000 个）",
                     "收购按物品类型匹配，背包内容不同也能提交",
                     "成交时优先放进背包，背包满则存入「待领取物品」"
                  )
               );
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case FREE_SELL_LIST:
               List var28 = FreeMarket.activeListings("SELL");
               int var52 = MenuManager.pages(var28.size());
               int var73 = var3 * 45;

               for (int var94 = 0; var94 < 45; var94++) {
                  int var110 = var73 + var94;
                  if (var110 >= var28.size()) {
                     break;
                  }

                  var0.setItem(var94, MenuManager.listingItem(var1, (MenuData.MarketListing)var28.get(var110), true));
               }

               if (var28.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "暂无出售挂单", ChatFormatting.GRAY, "点击下方返回后发布出售挂单"));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回自由市场", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var52 + " 页", ChatFormatting.WHITE, "共 " + var28.size() + " 条挂单"));
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var52 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case FREE_BUY_LIST:
               List var27 = FreeMarket.activeListings("BUY");
               int var51 = MenuManager.pages(var27.size());
               int var72 = var3 * 45;

               for (int var93 = 0; var93 < 45; var93++) {
                  int var109 = var72 + var93;
                  if (var109 >= var27.size()) {
                     break;
                  }

                  var0.setItem(var93, MenuManager.listingItem(var1, (MenuData.MarketListing)var27.get(var109), false));
               }

               if (var27.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "暂无收购挂单", ChatFormatting.GRAY, "点击下方返回后发布收购挂单"));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回自由市场", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var51 + " 页", ChatFormatting.WHITE, "共 " + var27.size() + " 条挂单"));
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var51 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case FREE_MANAGE:
               List var26 = FreeMarket.myListings(var6);
               int var50 = MenuManager.pages(var26.size());
               int var71 = var3 * 45;

               for (int var92 = 0; var92 < 45; var92++) {
                  int var108 = var71 + var92;
                  if (var108 >= var26.size()) {
                     break;
                  }

                  var0.setItem(var92, MenuManager.manageListingItem(var1, (MenuData.MarketListing)var26.get(var108)));
               }

               if (var26.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "你还没有挂单", ChatFormatting.GRAY, "先发布出售或收购挂单，再回来管理"));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回自由市场", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(
                  48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var50 + " 页", ChatFormatting.WHITE, "共 " + var26.size() + " 条自己的挂单")
               );
               var0.setItem(49, MenuManager.button(Items.BOOK, "管理说明", ChatFormatting.WHITE, "左键：修改价格", "右键：修改数量", "Shift+左键：下架", "出售挂单下架退回物品，收购挂单下架退还金币"));
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var50 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case PENDING:
               List var25 = MenuData.get().pendingItems(var6);
               int var49 = MenuManager.pages(var25.size());
               int var70 = var3 * 45;

               for (int var91 = 0; var91 < 45; var91++) {
                  int var107 = var70 + var91;
                  if (var107 >= var25.size()) {
                     break;
                  }

                  MenuData.PendingItem var122 = (MenuData.PendingItem)var25.get(var107);
                  ItemStack var132 = MarketItems.decode(var122.itemData, MarketItems.item(var122.item), var1.level().registryAccess());
                  if (var132.isEmpty()) {
                     var0.setItem(var91, MenuManager.button(Items.BARRIER, "未知物品", ChatFormatting.RED, "数据已失效，点击清除"));
                  } else {
                     var132 = var132.copy();
                     var132.setCount(Math.max(1, Math.min(var122.count, var132.getMaxStackSize())));
                     ArrayList var141 = new ArrayList();
                     var141.add(Component.literal("待领取：" + var122.count + " 件").withStyle(ChatFormatting.GRAY));
                     var141.add(Component.literal("左键：领取到背包").withStyle(ChatFormatting.GREEN));
                     var132.set(DataComponents.LORE, new ItemLore(var141));
                     var0.setItem(var91, var132);
                  }
               }

               if (var25.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "暂无待领取物品", ChatFormatting.GRAY, "背包满时成交的物品会出现在这里"));
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回自由市场", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var49 + " 页", ChatFormatting.WHITE, "共 " + var25.size() + " 项待领取"));
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var49 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case TITLES:
               MenuData.PlayerTitle var24 = var5.title(var6);
               String var48 = var24 == null ? null : var24.id;
               var0.setItem(4, MenuManager.button(Items.NAME_TAG, "我的头衔", ChatFormatting.LIGHT_PURPLE, "佩戴后聊天名字前会显示头衔", "自定义头衔额度：1 个，最多 12 字", "击败末影龙可解锁彩色头衔"));
               String var69 = Titles.customText(var24);
               String var90 = var24 != null && var24.color != null ? var24.color : "purple";
               var0.setItem(
                  19,
                  MenuManager.button(
                     Items.NAME_TAG,
                     "自定义头衔：" + var69,
                     "custom".equals(var48) ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE,
                     "状态：" + ("custom".equals(var48) ? "佩戴中" : "点击佩戴"),
                     "当前颜色：" + MenuManager.colorName(var90),
                     "左键佩戴",
                     "右键修改文字（最多 12 字）"
                  )
               );
               List var106 = Titles.achievementDefs(var1);

               for (int var120 = 0; var120 < var106.size() && var120 < 13; var120++) {
                  Titles.Def var130 = (Titles.Def)var106.get(var120);
                  boolean var14 = Titles.unlocked(var1, var130);
                  boolean var15 = var130.id().equals(var48);
                  ArrayList var16 = new ArrayList();
                  var16.add(var14 ? (var15 ? "状态：佩戴中" : "状态：已获得") : "状态：未获得");
                  var16.add("颜色：" + MenuManager.colorName(var130.color()));
                  var16.add("获取条件：" + var130.requirement());
                  var16.add(var14 ? "点击佩戴" : "完成后自动解锁，暂不可佩戴");
                  ItemStack var17 = var14
                     ? MenuManager.button(Items.NAME_TAG, var130.name(), MenuManager.colorFor(var130.color()), var16.toArray(new String[0]))
                     : MenuManager.button((Item)Items.DYE.pick(DyeColor.GRAY), var130.name(), ChatFormatting.DARK_GRAY, var16.toArray(new String[0]));
                  var0.setItem(MenuManager.titleSlot(var120 + 1), var17);
               }

               boolean var121 = Titles.dragonUnlocked(var1);
               String var131 = "可选：紫色 / 蓝色" + (var121 ? " / 彩色" : "") + (Titles.isAdmin(var1) ? " / 红色（管理员）" : "");
               var0.setItem(
                  40,
                  MenuManager.button(
                     (Item)Items.DYE.pick(DyeColor.PURPLE),
                     "自定义头衔颜色",
                     ChatFormatting.LIGHT_PURPLE,
                     "当前：" + MenuManager.colorName(var90),
                     var131,
                     var121 ? "点击选择颜色" : "击败末影龙后解锁彩色"
                  )
               );
               var0.setItem(49, MenuManager.button(Items.BARRIER, "取消佩戴", ChatFormatting.RED, "点击后聊天不再显示头衔"));
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case TITLE_COLOR:
               MenuData.PlayerTitle var23 = var5.title(var6);
               String var47 = var23 != null && var23.color != null ? var23.color : "purple";
               var0.setItem(4, MenuManager.button(Items.BOOK, "头衔颜色", ChatFormatting.LIGHT_PURPLE, "仅影响自定义头衔", "当前：" + MenuManager.colorName(var47)));
               var0.setItem(
                  20,
                  MenuManager.button(
                     (Item)Items.DYE.pick(DyeColor.PURPLE), "紫色", ChatFormatting.LIGHT_PURPLE, "柔和亮紫，适合大多数头衔", "purple".equals(var47) ? "当前使用中" : "点击选择"
                  )
               );
               var0.setItem(
                  22,
                  MenuManager.button((Item)Items.DYE.pick(DyeColor.LIGHT_BLUE), "蓝色", ChatFormatting.AQUA, "明亮的天蓝色", "blue".equals(var47) ? "当前使用中" : "点击选择")
               );
               if (Titles.dragonUnlocked(var1)) {
                  var0.setItem(
                     24,
                     MenuManager.button(
                        Items.NETHER_STAR, "彩色（炫彩）", ChatFormatting.GOLD, "击败末影龙的专属奖励", "文字呈幻彩渐变，随时间流动", "rainbow".equals(var47) ? "当前使用中" : "点击选择"
                     )
                  );
               } else {
                  var0.setItem(
                     24, MenuManager.button((Item)Items.DYE.pick(DyeColor.GRAY), "彩色（未解锁）", ChatFormatting.DARK_GRAY, "获取条件：完成成就「解放末地」", "击败末影龙后自动解锁")
                  );
               }

               if (Titles.isAdmin(var1)) {
                  var0.setItem(
                     31,
                     MenuManager.button(
                        (Item)Items.DYE.pick(DyeColor.RED), "红色（管理员）", ChatFormatting.RED, "管理员专属颜色", "普通玩家看不到这个选项", "red".equals(var47) ? "当前使用中" : "点击选择"
                     )
                  );
               }

               var0.setItem(45, MenuManager.button(Items.ARROW, "返回头衔", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case MINIGAMES:
               var0.setItem(4, MenuManager.button(Items.CLOCK, "小游戏", ChatFormatting.AQUA, "服务器娱乐玩法入口", "按玩法分类展示，边框留空保持整洁"));
               boolean bedwarsReady = MenuConfig.configured(MenuConfig.bedwarsHost(), MenuConfig.bedwarsPort());
               var0.setItem(
                  21,
                  MenuManager.button(
                     (Item)Items.BED.pick(DyeColor.RED),
                     bedwarsReady ? "起床战争" : "起床战争（未配置）",
                     bedwarsReady ? ChatFormatting.RED : ChatFormatting.DARK_GRAY,
                     "分类：竞技对战 / PVP",
                     bedwarsReady ? "点击进入起床战争服" : "在 config/siftmenu/config.json 配置地址后开放"
                  )
               );
               boolean bingoReady = MenuConfig.configured(MenuConfig.bingoHost(), MenuConfig.bingoPort());
               var0.setItem(
                  23,
                  MenuManager.button(
                     Items.MAP,
                     bingoReady ? "Bingo" : "Bingo（未配置）",
                     bingoReady ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY,
                     "分类：休闲收集 / 团队对抗",
                     "5×5 物品卡，连成一条线即获胜",
                     bingoReady ? "点击进入 Bingo 服（最多 6 人）" : "在 config/siftmenu/config.json 配置地址后开放"
                  )
               );
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case MAILBOX:
               List var21 = Mailbox.forPlayer(var6, var1.getGameProfile().name());
               int var46 = MenuManager.pages(var21.size());
               int var68 = var3 * 45;

               for (int var89 = 0; var89 < 45; var89++) {
                  int var105 = var68 + var89;
                  if (var105 >= var21.size()) {
                     break;
                  }

                  var0.setItem(var89, MenuManager.mailItem(var1, (MenuData.Mail)var21.get(var105)));
               }

               if (var21.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "邮箱是空的", ChatFormatting.GRAY, "服务器公告与补偿都会出现在这里"));
               }

               var0.setItem(
                  47,
                  MenuManager.button(
                     Items.BOOK,
                     "我的邮箱",
                     ChatFormatting.GOLD,
                     "未读 / 未领取：" + Mailbox.unread(var6, var1.getGameProfile().name()) + " 封",
                     "最新的邮件永远在第 1 格",
                     "公告：左键阅读",
                     "补偿：左键领取",
                     "已领取的补偿不能重复领取"
                  )
               );
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var46 + " 页", ChatFormatting.WHITE, "共 " + var21.size() + " 封邮件"));
               if (Titles.isAdmin(var1)) {
                  var0.setItem(49, MenuManager.button(Items.COMMAND_BLOCK, "邮箱管理", ChatFormatting.DARK_RED, "发送公告与补偿", "仅管理员可见"));
               }

               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var46 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case MAIL_ADMIN:
               List var20 = Mailbox.all();
               int var45 = MenuManager.mailAdminPages(var20.size());
               int var67 = var3 * 21;

               for (int var88 = 0; var88 < 21; var88++) {
                  int var104 = var67 + var88;
                  if (var104 >= var20.size()) {
                     break;
                  }

                  var0.setItem(MenuManager.mailAdminSlot(var88), MenuManager.adminMailItem(var1, (MenuData.Mail)var20.get(var104)));
               }

               if (var20.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "还没有任何邮件", ChatFormatting.GRAY, "用上方按钮发送公告或补偿"));
               }

               var0.setItem(4, MenuManager.button(Items.BOOK, "邮箱管理", ChatFormatting.DARK_RED, "共 " + var20.size() + " 封邮件", "左键邮件：查看内容", "Shift+左键：删除邮件"));
               var0.setItem(
                  11,
                  MenuManager.button(
                     Items.WRITABLE_BOOK,
                     "发送补偿",
                     ChatFormatting.GREEN,
                     "先手持要补偿的物品，再点击",
                     "输入：领取范围 数量 说明",
                     "范围：玩家名 / 多玩家逗号分隔",
                     "　　　全服 / 老玩家 / 新玩家",
                     "玩家可在邮箱中各自领取"
                  )
               );
               var0.setItem(13, MenuManager.button(Items.PAPER, "发送服务器公告", ChatFormatting.YELLOW, "点击后输入：标题|内容", "所有玩家都会收到公告邮件", "在线玩家会立刻收到提示"));
               var0.setItem(15, MenuManager.button(Items.MAP, "邮箱说明", ChatFormatting.GRAY, "下方 21 格为已发送邮件", "每页 21 封", "左键查看 / Shift+左键删除"));
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var45 + " 页", ChatFormatting.WHITE, "共 " + var20.size() + " 封邮件"));
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var45 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case ADMIN_MAIN:
               MenuData var19 = MenuData.get();
               var19.dailyReset();
               var0.setItem(4, MenuManager.button(Items.COMMAND_BLOCK, "管理员后台", ChatFormatting.DARK_RED, "仅管理员可进入", "修改全球市场的物品与价格", "普通玩家无法看到此入口"));
               var0.setItem(19, MenuManager.button(Items.EMERALD, "出售物品管理", ChatFormatting.GREEN, "修改售价、下架物品", "手持物品可上架新物品", "左键编辑 / 右键下架"));
               var0.setItem(21, MenuManager.button(Items.HOPPER, "收购物品管理", ChatFormatting.YELLOW, "修改收购价与每日额度", "手持物品可新增收购项", "左键编辑 / 右键下架"));
               var0.setItem(23, MenuManager.button(Items.CHEST, "邮箱管理", ChatFormatting.GOLD, "发送服务器公告与补偿", "查看并删除已发送的邮件"));
               var0.setItem(
                  25,
                  MenuManager.button(
                     Items.GOLD_BLOCK,
                     "经济面板",
                     ChatFormatting.GOLD,
                     "总金币：" + Economy.format(var19.totalMoney()),
                     "今日产出：" + Economy.format(var19.dailyCreated),
                     "今日回收：" + Economy.format(var19.dailySpent),
                     "今日税收：" + Economy.format(var19.dailyTax),
                     "今日挂单费：" + Economy.format(var19.dailyFee),
                     "今日服务费：" + Economy.format(var19.dailyService),
                     "点击在聊天栏查看完整明细"
                  )
               );
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case ADMIN_SELL:
               List var18 = GlobalMarket.allSellEntries();
               int var44 = MenuManager.pages(var18.size());
               int var66 = var3 * 45;

               for (int var87 = 0; var87 < 45; var87++) {
                  int var103 = var66 + var87;
                  if (var103 >= var18.size()) {
                     break;
                  }

                  GlobalMarket.EntryRef var119 = (GlobalMarket.EntryRef)var18.get(var103);
                  GlobalMarket.Entry var13 = var119.entry();
                  var0.setItem(
                     var87,
                     MenuManager.button(
                        var13.item(),
                        MarketItems.name(var13.item()),
                        ChatFormatting.GREEN,
                        "分类：" + var119.category(),
                        "物品 ID：" + var13.id(),
                        "售价：" + Economy.format(var13.price()) + " 金币/个",
                        "左键修改价格",
                        "右键从市场下架"
                     )
                  );
               }

               if (var18.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "暂无出售物品", ChatFormatting.GRAY));
               }

               var0.setItem(47, MenuManager.button(Items.BOOK, "出售物品管理", ChatFormatting.GREEN, "共 " + var18.size() + " 种物品", "左键：修改价格", "右键：下架", "下方按钮：手持物品上架"));
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回后台", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var44 + " 页", ChatFormatting.WHITE, "共 " + var18.size() + " 种物品"));
               var0.setItem(
                  49, MenuManager.button(Items.EMERALD, "上架 / 改价", ChatFormatting.GREEN, "请先手持要上架的物品", "点击后在聊天栏输入：价格 分类名", "例如：20 建筑材料", "不填分类则放入「管理员精选物品」")
               );
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var44 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
               break;
            case ADMIN_BUY:
               List var7 = GlobalMarket.buyEntries();
               int var8 = MenuManager.pages(var7.size());
               int var9 = var3 * 45;

               for (int var10 = 0; var10 < 45; var10++) {
                  int var11 = var9 + var10;
                  if (var11 >= var7.size()) {
                     break;
                  }

                  GlobalMarket.BuyEntry var12 = (GlobalMarket.BuyEntry)var7.get(var11);
                  var0.setItem(
                     var10,
                     MenuManager.button(
                        var12.item(),
                        MarketItems.name(var12.item()),
                        ChatFormatting.YELLOW,
                        "物品 ID：" + var12.id(),
                        "当前收购价：" + Money.format(GlobalMarket.currentBuyPrice(var12)) + " 金币/个（基础价的 " + GlobalMarket.pricePercent(var12) + "%）",
                        "基础收购价：" + Money.format(var12.price()) + " 金币/个",
                        "市场饱和：" + GlobalMarket.marketPressurePercent(var12) + "%",
                        "7 日均量：" + Economy.format(GlobalMarket.recentDailyVolume(var12)) + " 个/天（额度 " + GlobalMarket.recentVolumePercent(var12) + "%）",
                        "全服额度：" + var12.dailyStacks() + " 组/天（" + (long)var12.dailyStacks() * 64L + " 个）",
                        "个人额度：" + var12.dailyStacksPerPlayer() + " 组/天（" + (long)var12.dailyStacksPerPlayer() * 64L + " 个）",
                        "1 组 = 64 个",
                        "左键修改设置",
                        "右键从收购列表移除"
                     )
                  );
               }

               if (var7.isEmpty()) {
                  var0.setItem(22, MenuManager.button(Items.BARRIER, "暂无收购物品", ChatFormatting.GRAY));
               }

               var0.setItem(
                  47, MenuManager.button(Items.BOOK, "收购物品管理", ChatFormatting.YELLOW, "共 " + var7.size() + " 种物品", "左键：修改价格与额度", "右键：移除", "下方按钮：手持物品新增收购项")
               );
               var0.setItem(45, MenuManager.button(Items.ARROW, "返回后台", ChatFormatting.WHITE));
               var0.setItem(46, MenuManager.button(Items.ARROW, "上一页", ChatFormatting.WHITE, var3 > 0 ? "点击翻页" : "已经是第一页"));
               var0.setItem(48, MenuManager.button(Items.PAPER, "第 " + (var3 + 1) + " / " + var8 + " 页", ChatFormatting.WHITE, "共 " + var7.size() + " 种物品"));
               var0.setItem(49, MenuManager.button(Items.EMERALD, "新增收购物品", ChatFormatting.YELLOW, "请先手持要收购的物品", "点击后输入：价格 全服组数 个人组数"));
               var0.setItem(50, MenuManager.button(Items.ARROW, "下一页", ChatFormatting.WHITE, var3 < var8 - 1 ? "点击翻页" : "已经是最后一页"));
               var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
         }

         CustomMenus.applyOverlay(var0, var1, var2, var3);
      }

      private static void buildParty(SimpleContainer var0, ServerPlayer var1, MenuData var2) {
         MenuData.Party var3 = var2.partyOf(var1.getUUID());
         if (var3 == null) {
            var0.setItem(22, MenuManager.button((Item)Items.BANNER.pick(DyeColor.WHITE), "创建队伍", ChatFormatting.YELLOW, "点击后在聊天栏输入队伍名称", "创建后可以邀请其他玩家"));
            var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
            var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
         } else {
            boolean var4 = var3.leader.equals(var1.getUUID().toString());
            ServerPlayer var5 = var1.level().getServer().getPlayerList().getPlayer(UUID.fromString(var3.leader));
            String var6 = var5 != null ? var5.getGameProfile().name() : var3.leader.substring(0, 8);
            String var7 = formatPartyCreated(var3.createdAt);
            var0.setItem(
               32,
               MenuManager.button(
                  Items.BOOK,
                  "队伍信息",
                  ChatFormatting.GOLD,
                  "队伍名称：" + var3.name,
                  "队长：" + var6,
                  "创建时间：" + var7,
                  "成员：" + var3.members.size() + " / 16",
                  "队友伤害：" + (var3.pvp ? "开启" : "关闭")
               )
            );
            var0.setItem(20, MenuManager.button(Items.WRITABLE_BOOK, "邀请玩家", ChatFormatting.YELLOW, "打开在线玩家列表", "点击玩家即可发送邀请"));
            var0.setItem(22, MenuManager.button(Items.PLAYER_HEAD, "队伍成员", ChatFormatting.YELLOW, "查看队伍成员", var4 ? "左键转让队长 / 右键踢人" : "只有队长可以管理成员"));
            var0.setItem(24, MenuManager.button(Items.IRON_SWORD, "队友伤害：" + (var3.pvp ? "开启" : "关闭"), ChatFormatting.RED, var4 ? "点击切换" : "只有队长可以修改"));
            if (var4) {
               var0.setItem(30, MenuManager.button(Items.TNT, "解散队伍", ChatFormatting.DARK_RED, "点击后立即解散队伍"));
            } else {
               var0.setItem(30, MenuManager.button(Items.OAK_DOOR, "退出队伍", ChatFormatting.YELLOW, "点击后立即退出队伍"));
            }

            var0.setItem(45, MenuManager.button(Items.ARROW, "返回主菜单", ChatFormatting.WHITE));
            var0.setItem(53, MenuManager.button(Items.BARRIER, "关闭", ChatFormatting.RED, "关闭菜单"));
         }
      }

      private static String formatPartyCreated(long var0) {
         return var0 <= 0L ? "未知（旧队伍）" : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(var0));
      }
   }
}
