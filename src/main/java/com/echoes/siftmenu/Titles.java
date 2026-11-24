package com.echoes.siftmenu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.scores.PlayerTeam;

public final class Titles {
   public static final int CUSTOM_LIMIT = 12;
   private static final double RAINBOW_SPEED = 0.09;
   private static final double RAINBOW_SATURATION = 0.72;
   private static final List<Titles.Def> DEFS = List.of(
      new Titles.Def("custom", "自定义头衔", "purple", null, "所有玩家可用，可自定义名称与颜色", true),
      new Titles.Def("diamond", "钻石猎人", "aqua", "minecraft:story/mine_diamond", "完成成就：钻石！(获得钻石)", false),
      new Titles.Def("enchanter", "附魔师", "purple", "minecraft:story/enchant_item", "完成成就：附魔师（给物品附魔）", false),
      new Titles.Def("blaze", "烈焰征服者", "gold", "minecraft:nether/obtain_blaze_rod", "完成成就：与火共舞（获得烈焰棒）", false),
      new Titles.Def("beacon", "信标工程师", "aqua", "minecraft:nether/create_full_beacon", "完成成就：带信标回家（激活满级信标）", false),
      new Titles.Def("debris", "远古探掘者", "blue", "minecraft:nether/obtain_ancient_debris", "完成成就：深入黑暗（获得远古残骸）", false),
      new Titles.Def("dragon", "屠龙者", "gold", "minecraft:end/kill_dragon", "完成成就：解放末地（击杀末影龙）", false),
      new Titles.Def("elytra", "天空旅人", "aqua", "minecraft:end/elytra", "完成成就：天空之城（找到鞘翅）", false),
      new Titles.Def("hero", "村庄英雄", "purple", "minecraft:adventure/hero_of_the_village", "完成成就：村庄英雄（击退一次袭击）", false),
      new Titles.Def("totem", "不死之身", "blue", "minecraft:adventure/totem_of_undying", "完成成就：死里逃生（使用不死图腾）", false),
      new Titles.Def("sniper", "神射手", "green", "minecraft:adventure/sniper_duel", "完成成就：狙击手对决（50 米外击杀骷髅）", false),
      new Titles.Def("trade", "行商伙伴", "yellow", "minecraft:adventure/trade", "完成成就：成交！（与村民交易）", false),
      new Titles.Def("gourmet", "美食家", "yellow", "minecraft:husbandry/balanced_diet", "完成成就：均衡饮食（吃遍所有食物）", false),
      new Titles.Def("rancher", "牧场主", "green", "minecraft:husbandry/breed_an_animal", "完成成就：我从哪来？（繁殖一对动物）", false)
   );

   private Titles() {
   }

   public static List<Titles.Def> defs() {
      return DEFS;
   }

   public static List<Titles.Def> achievementDefs(ServerPlayer var0) {
      ArrayList var1 = new ArrayList();

      for (Titles.Def var3 : DEFS) {
         if (!var3.custom()) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public static Titles.Def byId(String var0) {
      if (var0 == null) {
         return null;
      } else {
         for (Titles.Def var2 : DEFS) {
            if (var2.id().equals(var0)) {
               return var2;
            }
         }

         return null;
      }
   }

   public static boolean isAdmin(ServerPlayer var0) {
      return var0.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
   }

   public static boolean isAdmin(CommandSourceStack var0) {
      return var0.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
   }

   public static boolean unlocked(ServerPlayer var0, Titles.Def var1) {
      if (var1.custom()) {
         return true;
      } else if (var1.advancement() == null) {
         return false;
      } else {
         AdvancementHolder var2 = var0.level().getServer().getAdvancements().get(Identifier.parse(var1.advancement()));
         return var2 != null && var0.getAdvancements().getOrStartProgress(var2).isDone();
      }
   }

   public static boolean dragonUnlocked(ServerPlayer var0) {
      Titles.Def var1 = byId("dragon");
      return var1 != null && unlocked(var0, var1);
   }

   public static String customText(MenuData.PlayerTitle var0) {
      return var0 != null && var0.customText != null && !var0.customText.isBlank() ? var0.customText : "自定义";
   }

   public static String colorOf(MenuData.PlayerTitle var0, Titles.Def var1) {
      if (!var1.custom()) {
         return var1.color();
      } else {
         return var0 != null && var0.color != null ? var0.color : "purple";
      }
   }

   public static Component colored(String var0, String var1) {
      if (!"rainbow".equals(var1)) {
         return Component.literal(var0).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb(var1))).withBold(true));
      } else {
         double var2 = (double)System.currentTimeMillis() * 0.09 % 360.0;
         int[] var4 = var0.codePoints().toArray();
         int var5 = Math.max(1, var4.length);
         double var6 = 360.0 / (double)var5;
         MutableComponent var8 = Component.empty();

         for (int var9 = 0; var9 < var4.length; var9++) {
            double var10 = (var2 + (double)var9 * var6) % 360.0;
            var8.append(
               Component.literal(new String(Character.toChars(var4[var9])))
                  .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rainbowRgb(var10))).withBold(true))
            );
         }

         return var8;
      }
   }

   private static int rainbowRgb(double var0) {
      double var2 = 0.72;
      double var4 = var2 * (1.0 - Math.abs(var0 / 60.0 % 2.0 - 1.0));
      double var6 = 1.0 - var2;
      double var8;
      double var10;
      double var12;
      if (var0 < 60.0) {
         var8 = var2;
         var10 = var4;
         var12 = 0.0;
      } else if (var0 < 120.0) {
         var8 = var4;
         var10 = var2;
         var12 = 0.0;
      } else if (var0 < 180.0) {
         var8 = 0.0;
         var10 = var2;
         var12 = var4;
      } else if (var0 < 240.0) {
         var8 = 0.0;
         var10 = var4;
         var12 = var2;
      } else if (var0 < 300.0) {
         var8 = var4;
         var10 = 0.0;
         var12 = var2;
      } else {
         var8 = var2;
         var10 = 0.0;
         var12 = var4;
      }

      int var14 = (int)Math.round((var8 + var6) * 255.0);
      int var15 = (int)Math.round((var10 + var6) * 255.0);
      int var16 = (int)Math.round((var12 + var6) * 255.0);
      return var14 << 16 | var15 << 8 | var16;
   }

   private static int rgb(String var0) {
      String var1 = var0 == null ? "" : var0;

      return switch (var1) {
         case "purple" -> 11032055;
         case "blue" -> 4890367;
         case "gold" -> 16766720;
         case "aqua" -> 5636095;
         case "green" -> 5635925;
         case "yellow" -> 16777045;
         case "dark_red" -> 11141120;
         case "red" -> 16731469;
         default -> 16777215;
      };
   }

   public static Component display(ServerPlayer var0) {
      MenuData.PlayerTitle var1 = MenuData.get().title(var0.getUUID());
      if (var1 == null) {
         return null;
      } else {
         Titles.Def var2 = byId(var1.id);
         if (var2 != null && unlocked(var0, var2)) {
            String var3 = var2.custom() ? customText(var1) : var2.name();
            return colored(var3, colorOf(var1, var2));
         } else {
            return null;
         }
      }
   }

   public static boolean isRainbow(ServerPlayer var0) {
      MenuData.PlayerTitle var1 = MenuData.get().title(var0.getUUID());
      if (var1 == null) {
         return false;
      } else {
         Titles.Def var2 = byId(var1.id);
         return var2 == null ? false : "rainbow".equals(colorOf(var1, var2));
      }
   }

   public static void refreshName(ServerPlayer var0) {
      ServerScoreboard var1 = var0.level().getServer().getScoreboard();
      String var2 = teamName(var0);
      String var3 = var0.getScoreboardName();
      PlayerTeam var4 = var1.getPlayersTeam(var3);
      Component var5 = display(var0);
      if (var5 == null) {
         if (var4 != null && var4.getName().equals(var2)) {
            var1.removePlayerFromTeam(var3, var4);
         }
      } else if (var4 == null || var4.getName().equals(var2)) {
         PlayerTeam var6 = var1.getPlayerTeam(var2);
         if (var6 == null) {
            var6 = var1.addPlayerTeam(var2);
         }

         MutableComponent var7 = Component.empty();
         var7.append(var5).append(Component.literal(" "));
         var6.setPlayerPrefix(var7);
         var1.addPlayerToTeam(var3, var6);
      }
   }

   public static void tickNames(MinecraftServer var0) {
      for (ServerPlayer var2 : var0.getPlayerList().getPlayers()) {
         if (isRainbow(var2)) {
            refreshName(var2);
         }
      }
   }

   private static String teamName(ServerPlayer var0) {
      String var1 = var0.getUUID().toString().replace("-", "");
      return "sft_" + var1.substring(0, 12);
   }

   public static void equip(ServerPlayer var0, String var1) {
      Titles.Def var2 = byId(var1);
      if (var2 != null) {
         if (!unlocked(var0, var2)) {
            var0.sendSystemMessage(Component.literal("该头衔尚未获得。").withStyle(ChatFormatting.RED));
         } else {
            MenuData.PlayerTitle var3 = MenuData.get().title(var0.getUUID());
            String var4 = var3 != null && var1.equals(var3.id) ? var3.customText : null;
            String var5 = var3 != null && var1.equals(var3.id) ? var3.color : null;
            if (var2.custom()) {
               if (var4 == null || var4.isBlank()) {
                  var4 = "自定义";
               }

               if (var5 == null) {
                  var5 = "purple";
               }
            }

            MenuData.get().setTitle(var0.getUUID(), var1, var4, var5);
            refreshName(var0);
            var0.sendSystemMessage(Component.literal("已佩戴头衔：").append(display(var0)).withStyle(ChatFormatting.GREEN));
         }
      }
   }

   public static void equipCustom(ServerPlayer var0) {
      equip(var0, "custom");
   }

   public static void clear(ServerPlayer var0) {
      MenuData.get().setTitle(var0.getUUID(), null, null, null);
      refreshName(var0);
      var0.sendSystemMessage(Component.literal("已取消佩戴头衔。").withStyle(ChatFormatting.GRAY));
   }

   public static boolean setCustomText(ServerPlayer var0, String var1) {
      String var2 = var1 == null ? "" : var1.trim();
      if (var2.isEmpty() || var2.length() > 12) {
         var0.sendSystemMessage(Component.literal("头衔长度需在 1-12 个字符之间。").withStyle(ChatFormatting.RED));
         return false;
      } else if (var2.contains("§")) {
         var0.sendSystemMessage(Component.literal("头衔不能包含颜色代码。").withStyle(ChatFormatting.RED));
         return false;
      } else {
         MenuData.PlayerTitle var3 = MenuData.get().title(var0.getUUID());
         String var4 = var3 != null && var3.color != null ? var3.color : "purple";
         MenuData.get().setTitle(var0.getUUID(), "custom", var2, var4);
         refreshName(var0);
         var0.sendSystemMessage(Component.literal("自定义头衔已更新。").withStyle(ChatFormatting.GREEN));
         return true;
      }
   }

   public static boolean setColor(ServerPlayer var0, String var1) {
      if ("red".equals(var1) && !isAdmin(var0)) {
         var0.sendSystemMessage(Component.literal("红色头衔仅管理员可用。").withStyle(ChatFormatting.RED));
         return false;
      } else if (!List.of("purple", "blue", "rainbow", "red").contains(var1)) {
         return false;
      } else if ("rainbow".equals(var1) && !dragonUnlocked(var0)) {
         var0.sendSystemMessage(Component.literal("击败末影龙后才能解锁彩色头衔。").withStyle(ChatFormatting.RED));
         return false;
      } else {
         MenuData.PlayerTitle var2 = MenuData.get().title(var0.getUUID());
         String var3 = var2 == null ? "自定义" : var2.customText;
         if (var3 == null || var3.isBlank()) {
            var3 = "自定义";
         }

         MenuData.get().setTitle(var0.getUUID(), "custom", var3, var1);
         refreshName(var0);
         var0.sendSystemMessage(Component.literal("头衔颜色已更新。").withStyle(ChatFormatting.GREEN));
         return true;
      }
   }

   public static record Def(String id, String name, String color, String advancement, String requirement, boolean custom) {
   }
}
