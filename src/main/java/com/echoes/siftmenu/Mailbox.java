package com.echoes.siftmenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class Mailbox {
   public static final String ANNOUNCEMENT = "ANNOUNCEMENT";
   public static final String COMPENSATION = "COMPENSATION";
   public static final String AUDIENCE_ALL = "ALL";
   public static final String AUDIENCE_OLD = "OLD";
   public static final String AUDIENCE_NEW = "NEW";
   public static final String AUDIENCE_PLAYERS = "PLAYERS";
   public static final long NEW_PLAYER_WINDOW = 604800000L;
   public static final int MAX_TITLE = 24;
   public static final int MAX_BODY = 220;
   public static final int MAX_ITEMS = 576;
   public static final int MAX_RECIPIENTS = 32;
   private static final Comparator<MenuData.Mail> NEWEST_FIRST = Comparator.<MenuData.Mail>comparingLong(var0 -> var0.created).reversed();

   private Mailbox() {
   }

   public static boolean isAnnouncement(MenuData.Mail var0) {
      return "ANNOUNCEMENT".equals(var0.kind);
   }

   public static String audienceOf(MenuData.Mail var0) {
      return var0.audience != null && !var0.audience.isBlank() ? var0.audience : "ALL";
   }

   public static String audienceText(MenuData.Mail var0) {
      if (isAnnouncement(var0)) {
         return "全体玩家";
      } else {
         String var1 = audienceOf(var0);

         return switch (var1) {
            case "OLD" -> "老玩家（7 天前进过服）";
            case "NEW" -> "新玩家（最近 7 天进服）";
            case "PLAYERS" -> "指定玩家：" + String.join("、", var0.recipientNames);
            default -> "全服玩家";
         };
      }
   }

   public static boolean isTargeted(MenuData.Mail var0) {
      return "PLAYERS".equals(audienceOf(var0));
   }

   public static boolean visibleTo(MenuData.Mail var0, UUID var1, String var2) {
      if (isAnnouncement(var0)) {
         return true;
      } else {
         String var3 = audienceOf(var0);

         return switch (var3) {
            case "OLD" -> isOldPlayer(var0, var1);
            case "NEW" -> !isOldPlayer(var0, var1);
            case "PLAYERS" -> matchesPlayer(var0, var1, var2);
            default -> true;
         };
      }
   }

   private static boolean matchesPlayer(MenuData.Mail var0, UUID var1, String var2) {
      if (var0.recipientIds.contains(var1.toString())) {
         return true;
      } else if (var2 == null) {
         return false;
      } else {
         for (String var4 : var0.recipientNames) {
            if (var4.equalsIgnoreCase(var2)) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean isOldPlayer(MenuData.Mail var0, UUID var1) {
      long var2 = MenuData.get().firstSeen(var1);
      return var2 <= 0L ? false : var2 <= var0.created - 604800000L;
   }

   public static List<MenuData.Mail> forPlayer(UUID var0, String var1) {
      ArrayList var2 = new ArrayList();

      for (MenuData.Mail var4 : MenuData.get().mails()) {
         if (visibleTo(var4, var0, var1)) {
            var2.add(var4);
         }
      }

      var2.sort(NEWEST_FIRST);
      return var2;
   }

   public static List<MenuData.Mail> all() {
      ArrayList var0 = new ArrayList<>(MenuData.get().mails());
      var0.sort(NEWEST_FIRST);
      return var0;
   }

   public static int unread(UUID var0, String var1) {
      MenuData var2 = MenuData.get();
      int var3 = 0;

      for (MenuData.Mail var5 : forPlayer(var0, var1)) {
         if (isAnnouncement(var5)) {
            if (!var2.isMailRead(var0, var5.id)) {
               var3++;
            }
         } else if (!var2.isMailClaimed(var0, var5.id)) {
            var3++;
         }
      }

      return var3;
   }

   private static void showPrompt(ServerPlayer var0, String var1, String var2, ChatFormatting var3) {
      var0.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
      var0.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(var2).withStyle(ChatFormatting.WHITE)));
      var0.connection.send(new ClientboundSetTitleTextPacket(Component.literal(var1).withStyle(var3)));
   }

   private static void notifyMail(ServerPlayer var0, MenuData.Mail var1) {
      boolean var2 = isAnnouncement(var1);
      showPrompt(var0, var2 ? "新服务器公告" : "收到补偿邮件", var1.title == null ? "" : var1.title, var2 ? ChatFormatting.GOLD : ChatFormatting.AQUA);
   }

   public static void notifyPending(ServerPlayer var0) {
      MenuData var1 = MenuData.get();
      UUID var2 = var0.getUUID();
      ArrayList<MenuData.Mail> var3 = new ArrayList<>();

      for (MenuData.Mail var5 : forPlayer(var2, var0.getGameProfile().name())) {
         if (!var1.isMailNotified(var2, var5.id)) {
            var3.add(var5);
         }
      }

      if (!var3.isEmpty()) {
         for (MenuData.Mail var10 : var3) {
            var1.markMailNotified(var2, var10.id);
         }

         var1.save();
         if (var3.size() == 1) {
            notifyMail(var0, (MenuData.Mail)var3.get(0));
         } else {
            int var9 = 0;
            int var11 = 0;

            for (MenuData.Mail var7 : var3) {
               if (isAnnouncement(var7)) {
                  var9++;
               } else {
                  var11++;
               }
            }

            String var12;
            if (var9 > 0 && var11 > 0) {
               var12 = "公告 " + var9 + " 封 · 补偿 " + var11 + " 封（菜单-邮箱查看）";
            } else if (var9 > 0) {
               var12 = "公告 " + var9 + " 封（菜单-邮箱查看）";
            } else {
               var12 = "补偿 " + var11 + " 封（菜单-邮箱领取）";
            }

            showPrompt(var0, "你有 " + var3.size() + " 封新邮件", var12, ChatFormatting.YELLOW);
         }
      }
   }

   public static boolean sendAnnouncement(ServerPlayer var0, String var1, String var2) {
      return sendAnnouncement(var0.level().getServer(), var0.getGameProfile().name(), var0::sendSystemMessage, var1, var2);
   }

   public static boolean sendAnnouncement(MinecraftServer var0, String var1, Consumer<Component> var2, String var3, String var4) {
      String var5 = var3 == null ? "" : var3.trim();
      String var6 = var4 == null ? "" : var4.trim();
      if (var5.isEmpty() || var5.length() > 24) {
         var2.accept(Component.literal("公告标题需在 1-24 个字符之间。").withStyle(ChatFormatting.RED));
         return false;
      } else if (var6.length() > 220) {
         var2.accept(Component.literal("公告内容不能超过 220 个字符。").withStyle(ChatFormatting.RED));
         return false;
      } else {
         MenuData.Mail var7 = new Mailbox.MailBuilder().kind("ANNOUNCEMENT").audience("ALL").title(var5).body(var6).sender(var1).build();
         MenuData var8 = MenuData.get();
         var8.mails().add(var7);
         var8.save();

         for (ServerPlayer var10 : var0.getPlayerList().getPlayers()) {
            var10.sendSystemMessage(Component.literal("[服务器公告] " + var5 + "（点击菜单邮箱查看全文）").withStyle(ChatFormatting.GOLD));
            notifyMail(var10, var7);
            var8.markMailNotified(var10.getUUID(), var7.id);
         }

         var8.save();
         var2.accept(Component.literal("公告已发送：" + var5).withStyle(ChatFormatting.GREEN));
         return true;
      }
   }

   public static boolean sendCompensation(ServerPlayer var0, String var1, int var2, String var3, String var4) {
      ItemStack var5 = validCompensationItem(var0, var2, var3);
      return var5 == null ? false : sendCompensation(var0.level().getServer(), var0.getGameProfile().name(), var0::sendSystemMessage, var1, var2, var5, var4);
   }

   public static boolean sendCompensation(MinecraftServer var0, String var1, Consumer<Component> var2, String var3, int var4, ItemStack var5, String var6) {
      String var7 = var3 == null ? "" : var3.trim();
      if (var7.isEmpty()) {
         var2.accept(Component.literal("请填写领取范围：玩家名、多个玩家名（逗号分隔）、全服、老玩家或新玩家。").withStyle(ChatFormatting.RED));
         return false;
      } else if (var4 >= 1 && var4 <= 576) {
         if (var5 != null && !var5.isEmpty()) {
            var5.setCount(1);
            ArrayList<String> var10 = new ArrayList<>();
            ArrayList var11 = new ArrayList();
            String var8;
            String var9;
            if (isKeyword(var7, "全服") || var7.equals("*") || var7.equalsIgnoreCase("all")) {
               var8 = "ALL";
               var9 = "全服补偿";
            } else if (isKeyword(var7, "老玩家")) {
               var8 = "OLD";
               var9 = "老玩家补偿";
            } else if (isKeyword(var7, "新玩家")) {
               var8 = "NEW";
               var9 = "新玩家补偿";
            } else {
               var8 = "PLAYERS";
               var9 = "系统补偿";

               for (String var15 : var7.split("[,，、\\s]+")) {
                  String var16 = var15.trim();
                  if (!var16.isEmpty()) {
                     var10.add(var16);
                  }
               }

               if (var10.isEmpty()) {
                  var2.accept(Component.literal("没有解析到有效的玩家名。").withStyle(ChatFormatting.RED));
                  return false;
               }

               if (var10.size() > 32) {
                  var2.accept(Component.literal("一次最多指定 32 名玩家。").withStyle(ChatFormatting.RED));
                  return false;
               }

               for (String var19 : var10) {
                  ServerPlayer var21 = var0.getPlayerList().getPlayerByName(var19);
                  if (var21 != null) {
                     var11.add(var21.getUUID().toString());
                  }
               }
            }

            MenuData.Mail var18 = new Mailbox.MailBuilder()
               .kind("COMPENSATION")
               .audience(var8)
               .title(var9)
               .body(var6 == null ? "" : var6.trim())
               .sender(var1)
               .build();
            var18.recipientNames.addAll(var10);
            var18.recipientIds.addAll(var11);
            var18.items.add(new MenuData.PendingItem(MarketItems.id(var5.getItem()), MarketItems.encode(var5, var0.registryAccess()), var4));
            MenuData var20 = MenuData.get();
            var20.mails().add(var18);
            var20.save();
            String var22 = MarketItems.name(var5.getItem());
            var2.accept(Component.literal("已发送补偿：" + var4 + "x " + var22 + "，范围：" + audienceText(var18)).withStyle(ChatFormatting.GREEN));

            for (ServerPlayer var24 : var0.getPlayerList().getPlayers()) {
               if (visibleTo(var18, var24.getUUID(), var24.getGameProfile().name())) {
                  var24.sendSystemMessage(Component.literal("你收到一封补偿邮件，请在菜单邮箱中领取。").withStyle(ChatFormatting.GOLD));
                  notifyMail(var24, var18);
                  var20.markMailNotified(var24.getUUID(), var18.id);
               }
            }

            var20.save();
            return true;
         } else {
            var2.accept(Component.literal("物品无效，无法发送补偿。").withStyle(ChatFormatting.RED));
            return false;
         }
      } else {
         var2.accept(Component.literal("数量需在 1-576 之间。").withStyle(ChatFormatting.RED));
         return false;
      }
   }

   private static boolean isKeyword(String var0, String var1) {
      return var0 != null && var0.equalsIgnoreCase(var1);
   }

   private static ItemStack validCompensationItem(ServerPlayer var0, int var1, String var2) {
      if (var1 < 1 || var1 > 576) {
         var0.sendSystemMessage(Component.literal("数量需在 1-576 之间。").withStyle(ChatFormatting.RED));
         return null;
      } else if (var2 != null && !var2.isBlank()) {
         ItemStack var3 = MarketItems.decode(var2, null, var0.level().registryAccess());
         if (var3.isEmpty()) {
            var0.sendSystemMessage(Component.literal("无法读取手持物品，请重试。").withStyle(ChatFormatting.RED));
            return null;
         } else {
            var3.setCount(1);
            return var3;
         }
      } else {
         var0.sendSystemMessage(Component.literal("请手持要补偿的物品后再点击发送补偿。").withStyle(ChatFormatting.RED));
         return null;
      }
   }

   public static void read(ServerPlayer var0, String var1) {
      MenuData var2 = MenuData.get();
      MenuData.Mail var3 = var2.mail(var1);
      if (var3 != null && visibleTo(var3, var0.getUUID(), var0.getGameProfile().name())) {
         if (isAnnouncement(var3) && !var2.isMailRead(var0.getUUID(), var1)) {
            var2.markMailRead(var0.getUUID(), var1);
         }

         String var4 = isAnnouncement(var3) ? "服务器公告" : "系统补偿";
         var0.sendSystemMessage(Component.literal("===== " + var4 + "：" + var3.title + " =====").withStyle(ChatFormatting.GOLD));
         var0.sendSystemMessage(Component.literal("发送者：" + (var3.sender == null ? "系统" : var3.sender)).withStyle(ChatFormatting.GRAY));
         if (!isAnnouncement(var3)) {
            var0.sendSystemMessage(Component.literal("领取范围：" + audienceText(var3)).withStyle(ChatFormatting.GRAY));
         }

         if (var3.body != null && !var3.body.isBlank()) {
            for (String var8 : var3.body.split("\\R")) {
               if (!var8.isBlank()) {
                  var0.sendSystemMessage(Component.literal(var8).withStyle(ChatFormatting.WHITE));
               }
            }
         }

         if (!isAnnouncement(var3)) {
            boolean var9 = var2.isMailClaimed(var0.getUUID(), var1);
            var0.sendSystemMessage(Component.literal(var9 ? "状态：已领取" : "状态：未领取，左键此邮件即可领取").withStyle(ChatFormatting.AQUA));
         }
      }
   }

   public static boolean claim(ServerPlayer var0, String var1) {
      MenuData var2 = MenuData.get();
      MenuData.Mail var3 = var2.mail(var1);
      if (var3 != null && !isAnnouncement(var3) && visibleTo(var3, var0.getUUID(), var0.getGameProfile().name())) {
         if (var2.isMailClaimed(var0.getUUID(), var1)) {
            var0.sendSystemMessage(Component.literal("这封补偿已经领取过了。").withStyle(ChatFormatting.GRAY));
            return false;
         } else {
            int var4 = 0;

            for (MenuData.PendingItem var6 : var3.items) {
               ItemStack var7 = MarketItems.decode(var6.itemData, MarketItems.item(var6.item), var0.level().registryAccess());
               if (!var7.isEmpty() && var6.count > 0) {
                  MarketItems.give(var0, var7, var6.count);
                  var4 += var6.count;
               }
            }

            var2.markMailClaimed(var0.getUUID(), var1);
            var0.sendSystemMessage(Component.literal("已领取补偿：共 " + var4 + " 件物品").withStyle(ChatFormatting.GREEN));
            return true;
         }
      } else {
         return false;
      }
   }

   public static boolean remove(String var0) {
      MenuData var1 = MenuData.get();
      MenuData.Mail var2 = var1.mail(var0);
      if (var2 == null) {
         return false;
      } else {
         var1.mails().remove(var2);
         var1.save();
         return true;
      }
   }

   private static final class MailBuilder {
      private final MenuData.Mail mail = new MenuData.Mail();

      private MailBuilder() {
         this.mail.id = UUID.randomUUID().toString();
         this.mail.created = System.currentTimeMillis();
         this.mail.title = "";
         this.mail.body = "";
      }

      private Mailbox.MailBuilder kind(String var1) {
         this.mail.kind = var1;
         return this;
      }

      private Mailbox.MailBuilder audience(String var1) {
         this.mail.audience = var1;
         return this;
      }

      private Mailbox.MailBuilder title(String var1) {
         this.mail.title = var1;
         return this;
      }

      private Mailbox.MailBuilder body(String var1) {
         this.mail.body = var1;
         return this;
      }

      private Mailbox.MailBuilder sender(ServerPlayer var1) {
         return this.sender(var1.getGameProfile().name());
      }

      private Mailbox.MailBuilder sender(String var1) {
         this.mail.sender = var1;
         return this;
      }

      private MenuData.Mail build() {
         return this.mail;
      }
   }
}
