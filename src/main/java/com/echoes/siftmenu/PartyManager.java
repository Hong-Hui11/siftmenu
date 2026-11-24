package com.echoes.siftmenu;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.server.level.ServerPlayer;

public final class PartyManager {
   private PartyManager() {
   }

   public static void invite(ServerPlayer var0, String var1) {
      MenuData var2 = MenuData.get();
      MenuData.Party var3 = var2.partyOf(var0.getUUID());
      if (var3 == null) {
         msg(var0, "你还没有队伍。", ChatFormatting.RED);
      } else {
         ServerPlayer var4 = var0.level().getServer().getPlayerList().getPlayerByName(var1);
         if (var4 == null) {
            msg(var0, "找不到在线玩家：" + var1, ChatFormatting.RED);
         } else if (var4 == var0) {
            msg(var0, "不能邀请自己。", ChatFormatting.RED);
         } else if (var2.partyOf(var4.getUUID()) != null) {
            msg(var0, var4.getGameProfile().name() + " 已经在队伍中。", ChatFormatting.RED);
         } else if (var3.members.size() >= 16) {
            msg(var0, "队伍人数已达上限（16 人）。", ChatFormatting.RED);
         } else {
            var2.invites.put(var4.getUUID().toString(), var3.id);
            var2.save();
            MutableComponent var5 = Component.literal("[✓]")
               .withStyle(
                  var1x -> var1x.withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new RunCommand("/siftmenu party accept " + var0.getGameProfile().name()))
                        .withHoverEvent(new ShowText(Component.literal("同意加入队伍")))
               );
            MutableComponent var6 = Component.literal("[✕]")
               .withStyle(
                  var1x -> var1x.withColor(ChatFormatting.RED)
                        .withBold(true)
                        .withClickEvent(new RunCommand("/siftmenu party deny " + var0.getGameProfile().name()))
                        .withHoverEvent(new ShowText(Component.literal("拒绝邀请")))
               );
            var4.sendSystemMessage(
               Component.literal("队伍邀请：" + var3.name + "  ").withStyle(ChatFormatting.GOLD).append(var5).append(Component.literal("  ")).append(var6)
            );
            msg(var0, "已向 " + var4.getGameProfile().name() + " 发送邀请。", ChatFormatting.GREEN);
         }
      }
   }

   public static void accept(ServerPlayer var0, String var1) {
      MenuData var2 = MenuData.get();
      String var3 = var2.invites.get(var0.getUUID().toString());
      if (var3 == null) {
         msg(var0, "没有待处理的队伍邀请。", ChatFormatting.RED);
      } else {
         MenuData.Party var4 = var2.parties.get(var3);
         if (var4 == null) {
            var2.invites.remove(var0.getUUID().toString());
            var2.save();
            msg(var0, "该队伍已不存在。", ChatFormatting.RED);
         } else {
            ServerPlayer var5 = var0.level().getServer().getPlayerList().getPlayer(UUID.fromString(var4.leader));
            String var6 = var5 == null ? var1 : var5.getGameProfile().name();
            if (!var6.equalsIgnoreCase(var1)) {
               msg(var0, "邀请已失效。", ChatFormatting.RED);
            } else {
               if (var2.partyOf(var0.getUUID()) != null) {
                  var2.leaveParty(var0.getUUID());
               }

               var4.members.add(var0.getUUID().toString());
               var2.playerParty.put(var0.getUUID().toString(), var4.id);
               var2.invites.remove(var0.getUUID().toString());
               var2.save();
               notifyParty(var0, var4, var0.getGameProfile().name() + " 加入了队伍。", ChatFormatting.GREEN);
               MenuManager.openParty(var0);
            }
         }
      }
   }

   public static void deny(ServerPlayer var0, String var1) {
      MenuData var2 = MenuData.get();
      if (var2.invites.remove(var0.getUUID().toString()) != null) {
         var2.save();
         msg(var0, "已拒绝队伍邀请。", ChatFormatting.GRAY);
         ServerPlayer var3 = var0.level().getServer().getPlayerList().getPlayerByName(var1);
         if (var3 != null) {
            msg(var3, var0.getGameProfile().name() + " 拒绝了你的邀请。", ChatFormatting.GRAY);
         }
      } else {
         msg(var0, "没有待处理的队伍邀请。", ChatFormatting.RED);
      }
   }

   public static void kick(ServerPlayer var0, String var1) {
      MenuData var2 = MenuData.get();
      MenuData.Party var3 = var2.partyOf(var0.getUUID());
      if (var3 == null || !var3.leader.equals(var0.getUUID().toString())) {
         msg(var0, "只有队长可以踢人。", ChatFormatting.RED);
      } else if (var1.equals(var3.leader)) {
         msg(var0, "不能踢出自己。", ChatFormatting.RED);
      } else {
         if (var3.members.remove(var1)) {
            var2.playerParty.remove(var1);
            var2.save();
            ServerPlayer var4 = var0.level().getServer().getPlayerList().getPlayer(UUID.fromString(var1));
            String var5 = var4 == null ? var1 : var4.getGameProfile().name();
            notifyParty(var0, var3, var5 + " 被移出了队伍。", ChatFormatting.YELLOW);
            if (var4 != null) {
               msg(var4, "你已被移出队伍。", ChatFormatting.YELLOW);
            }

            MenuManager.openParty(var0);
         }
      }
   }

   public static void transfer(ServerPlayer var0, String var1) {
      MenuData var2 = MenuData.get();
      MenuData.Party var3 = var2.partyOf(var0.getUUID());
      if (var3 == null || !var3.leader.equals(var0.getUUID().toString())) {
         msg(var0, "只有队长可以转让队长。", ChatFormatting.RED);
      } else if (!var3.members.contains(var1)) {
         msg(var0, "该玩家不在队伍中。", ChatFormatting.RED);
      } else {
         var3.leader = var1;
         var2.save();
         ServerPlayer var4 = var0.level().getServer().getPlayerList().getPlayer(UUID.fromString(var1));
         String var5 = var4 == null ? var1 : var4.getGameProfile().name();
         notifyParty(var0, var3, var5 + " 成为了新队长。", ChatFormatting.GOLD);
         MenuManager.openParty(var0);
      }
   }

   public static void togglePvp(ServerPlayer var0) {
      MenuData var1 = MenuData.get();
      MenuData.Party var2 = var1.partyOf(var0.getUUID());
      if (var2 != null && var2.leader.equals(var0.getUUID().toString())) {
         var2.pvp = !var2.pvp;
         var1.save();
         notifyParty(var0, var2, "队伍队友伤害已" + (var2.pvp ? "开启" : "关闭") + "。", ChatFormatting.YELLOW);
         MenuManager.openParty(var0);
      } else {
         msg(var0, "只有队长可以修改队伍设置。", ChatFormatting.RED);
      }
   }

   public static void leave(ServerPlayer var0) {
      MenuData var1 = MenuData.get();
      MenuData.Party var2 = var1.partyOf(var0.getUUID());
      if (var2 == null) {
         msg(var0, "你不在任何队伍中。", ChatFormatting.RED);
      } else if (var2.leader.equals(var0.getUUID().toString())) {
         disband(var0);
      } else {
         var1.leaveParty(var0.getUUID());
         notifyParty(var0, var2, var0.getGameProfile().name() + " 离开了队伍。", ChatFormatting.YELLOW);
         var0.closeContainer();
         msg(var0, "已退出队伍。", ChatFormatting.GRAY);
      }
   }

   public static void disband(ServerPlayer var0) {
      MenuData var1 = MenuData.get();
      MenuData.Party var2 = var1.partyOf(var0.getUUID());
      if (var2 == null) {
         msg(var0, "你不在任何队伍中。", ChatFormatting.RED);
      } else if (!var2.leader.equals(var0.getUUID().toString())) {
         msg(var0, "只有队长可以解散队伍。", ChatFormatting.RED);
      } else {
         notifyParty(var0, var2, "队伍已被队长解散。", ChatFormatting.RED);
         var1.removeParty(var2.id);
         var0.closeContainer();
      }
   }

   public static void notifyParty(ServerPlayer var0, MenuData.Party var1, String var2, ChatFormatting var3) {
      for (String var5 : var1.members) {
         ServerPlayer var6 = var0.level().getServer().getPlayerList().getPlayer(UUID.fromString(var5));
         if (var6 != null) {
            var6.sendSystemMessage(Component.literal(var2).withStyle(var3));
         }
      }
   }

   private static void msg(ServerPlayer var0, String var1, ChatFormatting var2) {
      var0.sendSystemMessage(Component.literal(var1).withStyle(var2));
   }
}
