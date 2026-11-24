package com.echoes.siftmenu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ChatInput {
   private static final Map<UUID, ChatInput.Pending> PENDING = new HashMap<>();

   private ChatInput() {
   }

   public static void requestHome(ServerPlayer var0) {
      if ("sift:lobby".equals(var0.level().dimension().identifier().toString())) {
         var0.closeContainer();
         var0.sendSystemMessage(Component.literal("主城大厅内不能设置家。").withStyle(ChatFormatting.RED));
         return;
      }

      MenuData.Home var1 = new MenuData.Home();
      var1.x = var0.getX();
      var1.y = var0.getY();
      var1.z = var0.getZ();
      var1.yaw = var0.getYRot();
      var1.pitch = var0.getXRot();
      var1.dim = var0.level().dimension().identifier().toString();
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.HOME_NAME, var1, null));
      var0.closeContainer();
      var0.sendSystemMessage(Component.literal("请在聊天栏输入家的名称（输入 cancel 取消）。").withStyle(ChatFormatting.YELLOW));
   }

   public static void requestParty(ServerPlayer var0) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.PARTY_NAME, null, null));
      var0.closeContainer();
      var0.sendSystemMessage(Component.literal("请在聊天栏输入队伍名称（输入 cancel 取消）。").withStyle(ChatFormatting.YELLOW));
   }

   public static void requestFreeSell(ServerPlayer var0) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.FREE_SELL, null, null));
      var0.closeContainer();
      var0.sendSystemMessage(
         Component.literal("请手持要出售的物品，然后在聊天栏输入：数量（个） 单价（例如 64 100.00）。单价支持两位小数；数量最多 576（9 组）。输入 cancel 取消。").withStyle(ChatFormatting.YELLOW)
      );
   }

   public static void requestFreeBuy(ServerPlayer var0) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.FREE_BUY, null, null));
      var0.closeContainer();
      var0.sendSystemMessage(
         Component.literal("请手持想收购的物品作为样品，然后在聊天栏输入：数量（个） 单价（例如 6400 100.00）。单价支持两位小数；数量最多 64000（1000 组，每组 64 个）。输入 cancel 取消。")
            .withStyle(ChatFormatting.YELLOW)
      );
   }

   public static void requestFreePrice(ServerPlayer var0, String var1) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.FREE_EDIT_PRICE, null, var1));
      var0.closeContainer();
      var0.sendSystemMessage(
         Component.literal("请输入该挂单的新单价（金币，支持两位小数），例如 120.00；不要超过 " + Economy.format(FreeMarket.MAX_UNIT_PRICE) + "。输入 cancel 取消。")
            .withStyle(ChatFormatting.YELLOW)
      );
   }

   public static void requestFreeQuantity(ServerPlayer var0, String var1) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.FREE_EDIT_QUANTITY, null, var1));
      var0.closeContainer();
      MenuData.MarketListing var2 = MenuData.get().listing(var1);
      String var3 = var2 == null ? "SELL" : var2.type;
      var0.sendSystemMessage(Component.literal("请输入该挂单的新数量（1-" + FreeMarket.maxQuantity(var3) + "）。输入 cancel 取消。").withStyle(ChatFormatting.YELLOW));
   }

   public static void requestTitleText(ServerPlayer var0) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.TITLE_TEXT, null, null));
      var0.closeContainer();
      var0.sendSystemMessage(Component.literal("请在聊天栏输入自定义头衔，最多 12 个字符。输入 cancel 取消。").withStyle(ChatFormatting.YELLOW));
   }

   public static void requestMailAnnouncement(ServerPlayer var0) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.MAIL_ANNOUNCEMENT, null, null));
      var0.closeContainer();
      var0.sendSystemMessage(Component.literal("请输入公告内容：标题|正文（例如「维护公告|今晚 22:00 停机维护」）。输入 cancel 取消。").withStyle(ChatFormatting.YELLOW));
   }

   public static void requestMailCompensation(ServerPlayer var0) {
      if (!Titles.isAdmin(var0)) {
         var0.sendSystemMessage(Component.literal("你没有管理员权限。").withStyle(ChatFormatting.RED));
      } else {
         ItemStack var1 = var0.getMainHandItem();
         if (!var1.isEmpty() && !SiftMenu.isMenuBook(var1)) {
            String var2 = MarketItems.encode(var1, var0.level().registryAccess());
            PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.MAIL_COMPENSATION, null, null, var2));
            var0.closeContainer();
            var0.sendSystemMessage(
               Component.literal("已记录手持物品，请在聊天栏输入：领取范围 数量 说明（例如 全服 1 服务器维护补偿）。范围可填：玩家名 / 多个玩家名（逗号分隔）/ 全服 / 老玩家 / 新玩家。说明可省略，输入 cancel 取消。")
                  .withStyle(ChatFormatting.YELLOW)
            );
         } else {
            var0.sendSystemMessage(Component.literal("请先手持要作为补偿的物品，再点击「发送补偿」。").withStyle(ChatFormatting.RED));
         }
      }
   }

   public static void requestAdminSellPrice(ServerPlayer var0, String var1, long var2) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.ADMIN_SELL_PRICE, null, var1));
      var0.closeContainer();
      var0.sendSystemMessage(
         Component.literal("请输入 " + var1 + " 的新出售价格（支持两位小数）；可加空格指定分类，例如「20.00 建筑材料」（当前 " + Economy.format(var2) + "）。输入 cancel 取消。")
            .withStyle(ChatFormatting.YELLOW)
      );
   }

   public static void requestAdminSellAdd(ServerPlayer var0, String var1) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.ADMIN_SELL_ADD, null, var1));
      var0.closeContainer();
      var0.sendSystemMessage(
         Component.literal("请输入 " + var1 + " 的出售价格（支持两位小数），可加空格指定分类（例如「20.00 建筑材料」）；不填分类则放入「管理员精选物品」。输入 cancel 取消。").withStyle(ChatFormatting.YELLOW)
      );
   }

   public static void requestAdminBuyEdit(ServerPlayer var0, String var1, String var2) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.ADMIN_BUY_EDIT, null, var1));
      var0.closeContainer();
      var0.sendSystemMessage(
         Component.literal("请输入 " + var1 + " 的收购设置：价格 全服组数 个人组数（1 组 = 64 个，价格支持两位小数）" + (var2 == null ? "" : "（当前 " + var2 + "）") + "。输入 cancel 取消。")
            .withStyle(ChatFormatting.YELLOW)
      );
   }

   public static void requestAdminBuyAdd(ServerPlayer var0, String var1) {
      PENDING.put(var0.getUUID(), new ChatInput.Pending(ChatInput.Type.ADMIN_BUY_ADD, null, var1));
      var0.closeContainer();
      var0.sendSystemMessage(Component.literal("请输入 " + var1 + " 的收购设置：价格 全服组数 个人组数（1 组 = 64 个，价格支持两位小数）。输入 cancel 取消。").withStyle(ChatFormatting.YELLOW));
   }

   public static boolean handle(ServerPlayer var0, String var1) {
      ChatInput.Pending var2 = PENDING.remove(var0.getUUID());
      if (var2 == null) {
         return false;
      } else {
         String var3 = var1.trim();
         if (var3.equalsIgnoreCase("cancel")) {
            var0.sendSystemMessage(Component.literal("已取消。").withStyle(ChatFormatting.GRAY));
            return true;
         } else if (var3.isEmpty()) {
            var0.sendSystemMessage(Component.literal("内容不能为空。").withStyle(ChatFormatting.RED));
            return true;
         } else if (var2.type == ChatInput.Type.FREE_SELL || var2.type == ChatInput.Type.FREE_BUY) {
            String[] var23 = var3.split("\\s+");
            if (var23.length != 2) {
               retry(var0, var2, "格式错误，请输入：数量（个） 单价（例如 64 100.00）");
               return true;
            } else {
               int var28;
               long var30;
               try {
                  var28 = Integer.parseInt(var23[0]);
                  var30 = Money.parse(var23[1]);
               } catch (NumberFormatException var15) {
                  retry(var0, var2, "数量必须是整数，单价支持两位小数（例如 100.00）。");
                  return true;
               }

               boolean var33 = var2.type == ChatInput.Type.FREE_SELL ? FreeMarket.createSell(var0, var28, var30) : FreeMarket.createBuy(var0, var28, var30);
               if (var33) {
                  MenuManager.openFreeMarket(var0);
               } else {
                  PENDING.put(var0.getUUID(), var2);
               }

               return true;
            }
         } else if (var2.type != ChatInput.Type.FREE_EDIT_PRICE && var2.type != ChatInput.Type.FREE_EDIT_QUANTITY) {
            if (var2.type != ChatInput.Type.ADMIN_SELL_PRICE && var2.type != ChatInput.Type.ADMIN_SELL_ADD) {
               if (var2.type != ChatInput.Type.ADMIN_BUY_EDIT && var2.type != ChatInput.Type.ADMIN_BUY_ADD) {
                  if (var2.type == ChatInput.Type.MAIL_ANNOUNCEMENT) {
                     if (!Titles.isAdmin(var0)) {
                        var0.sendSystemMessage(Component.literal("你没有管理员权限。").withStyle(ChatFormatting.RED));
                        return true;
                     } else {
                        String[] var22 = var3.split("\\|", 2);
                        String var27 = var22[0].trim();
                        String var29 = var22.length > 1 ? var22[1].trim() : "";
                        if (Mailbox.sendAnnouncement(var0, var27, var29)) {
                           MenuManager.openMailAdmin(var0);
                        } else {
                           PENDING.put(var0.getUUID(), var2);
                        }

                        return true;
                     }
                  } else if (var2.type == ChatInput.Type.MAIL_COMPENSATION) {
                     if (!Titles.isAdmin(var0)) {
                        var0.sendSystemMessage(Component.literal("你没有管理员权限。").withStyle(ChatFormatting.RED));
                        return true;
                     } else {
                        String[] var21 = var3.split("\\s+", 3);
                        if (var21.length < 2) {
                           retry(var0, var2, "格式：玩家名 数量 说明（说明可省略）");
                           return true;
                        } else {
                           int var26;
                           try {
                              var26 = Integer.parseInt(var21[1]);
                           } catch (NumberFormatException var10) {
                              retry(var0, var2, "数量必须是整数。格式：玩家名 数量 说明");
                              return true;
                           }

                           String var6 = var21.length > 2 ? var21[2].trim() : "";
                           if (Mailbox.sendCompensation(var0, var21[0], var26, var2.itemData, var6)) {
                              MenuManager.openMailAdmin(var0);
                           } else {
                              PENDING.put(var0.getUUID(), var2);
                           }

                           return true;
                        }
                     }
                  } else if (var2.type == ChatInput.Type.TITLE_TEXT) {
                     if (Titles.setCustomText(var0, var3)) {
                        MenuManager.openTitles(var0);
                     } else {
                        PENDING.put(var0.getUUID(), var2);
                     }

                     return true;
                  } else if (var3.length() > 16) {
                     var0.sendSystemMessage(Component.literal("名称长度需在 1-16 个字符之间，请重新打开菜单操作。").withStyle(ChatFormatting.RED));
                     return true;
                  } else {
                     if (var2.type == ChatInput.Type.HOME_NAME) {
                        MenuData.Home var19 = var2.home;
                        var19.name = var3;
                        MenuData var25 = MenuData.get();
                        if (var25.homes(var0.getUUID()).size() >= 64) {
                           var0.sendSystemMessage(Component.literal("家的数量已达上限（64 个）。").withStyle(ChatFormatting.RED));
                        } else if (var25.findHome(var0.getUUID(), var3) != null) {
                           var0.sendSystemMessage(Component.literal("已存在同名的家。").withStyle(ChatFormatting.RED));
                        } else {
                           var25.addHome(var0.getUUID(), var19);
                           var0.sendSystemMessage(Component.literal("已创建家：" + var3).withStyle(ChatFormatting.GREEN));
                        }

                        MenuManager.openHomes(var0);
                     } else {
                        MenuData var20 = MenuData.get();
                        if (var20.partyOf(var0.getUUID()) != null) {
                           var0.sendSystemMessage(Component.literal("你已经在一个队伍中了。").withStyle(ChatFormatting.RED));
                        } else {
                           var20.createParty(var0.getUUID(), var3);
                           var0.sendSystemMessage(Component.literal("已创建队伍：" + var3).withStyle(ChatFormatting.GREEN));
                        }

                        MenuManager.openParty(var0);
                     }

                     return true;
                  }
               } else if (!Titles.isAdmin(var0)) {
                  var0.sendSystemMessage(Component.literal("你没有管理员权限。").withStyle(ChatFormatting.RED));
                  return true;
               } else {
                  String[] var18 = var3.split("\\s+");
                  if (var18.length != 3) {
                     retry(var0, var2, "格式错误，请输入：价格 全服组数 个人组数（例如 20.00 100 20）");
                     return true;
                  } else {
                     long var24;
                     int var31;
                     int var32;
                     try {
                        var24 = Money.parse(var18[0]);
                        var31 = Integer.parseInt(var18[1]);
                        var32 = Integer.parseInt(var18[2]);
                     } catch (NumberFormatException var11) {
                        retry(var0, var2, "价格支持两位小数，组数必须是整数。");
                        return true;
                     }

                     if (var24 >= 1L && var31 >= 1 && var32 >= 1) {
                        GlobalMarket.setBuy(var2.key, var24, var31, var32);
                        var0.sendSystemMessage(Component.literal("已更新 " + var2.key + " 的收购设置。").withStyle(ChatFormatting.GREEN));
                        MenuManager.openAdminBuy(var0);
                        return true;
                     } else {
                        retry(var0, var2, "价格和额度都必须大于 0。");
                        return true;
                     }
                  }
               }
            } else if (!Titles.isAdmin(var0)) {
               var0.sendSystemMessage(Component.literal("你没有管理员权限。").withStyle(ChatFormatting.RED));
               return true;
            } else {
               String[] var17 = var3.split("\\s+", 2);

               long var5;
               try {
                  var5 = Money.parse(var17[0]);
               } catch (NumberFormatException var12) {
                  retry(var0, var2, "价格支持两位小数（例如 20.00）。格式：价格 分类名（分类可省略）");
                  return true;
               }

               if (var5 < 1L) {
                  retry(var0, var2, "价格必须大于 0。");
                  return true;
               } else {
                  String var7 = var17.length > 1 ? var17[1].trim() : null;
                  GlobalMarket.setSellPrice(var2.key, var5, var7);
                  String var8 = var7 != null && !var7.isBlank() ? var7 : GlobalMarket.baseCategoryOf(var2.key);
                  var0.sendSystemMessage(
                     Component.literal("已更新 " + var2.key + "，分类：" + var8 + "，售价 " + Economy.format(var5) + " 金币。").withStyle(ChatFormatting.GREEN)
                  );
                  MenuManager.openAdminSell(var0);
                  return true;
               }
            }
         } else {
            if (var2.type == ChatInput.Type.FREE_EDIT_PRICE) {
               long var4;
               try {
                  var4 = Money.parse(var3);
               } catch (NumberFormatException var14) {
                  retry(var0, var2, "请输入价格，支持两位小数（例如 120.00）。");
                  return true;
               }

               if (var4 < 1L) {
                  retry(var0, var2, "价格必须大于 0。");
                  return true;
               }

               FreeMarket.updatePrice(var0, var2.key, var4);
            } else {
               int var16;
               try {
                  var16 = Integer.parseInt(var3);
               } catch (NumberFormatException var13) {
                  retry(var0, var2, "数量必须是整数。");
                  return true;
               }

               if (var16 < 1) {
                  retry(var0, var2, "数量必须大于 0。");
                  return true;
               }

               FreeMarket.updateQuantity(var0, var2.key, var16);
            }

            MenuManager.openFreeManage(var0);
            return true;
         }
      }
   }

   private static void retry(ServerPlayer var0, ChatInput.Pending var1, String var2) {
      PENDING.put(var0.getUUID(), var1);
      var0.sendSystemMessage(Component.literal(var2).withStyle(ChatFormatting.RED));
   }

   private static record Pending(ChatInput.Type type, MenuData.Home home, String key, String itemData) {
      private Pending(ChatInput.Type var1, MenuData.Home var2, String var3) {
         this(var1, var2, var3, null);
      }
   }

   private static enum Type {
      HOME_NAME,
      PARTY_NAME,
      FREE_SELL,
      FREE_BUY,
      FREE_EDIT_PRICE,
      FREE_EDIT_QUANTITY,
      TITLE_TEXT,
      ADMIN_SELL_PRICE,
      ADMIN_SELL_ADD,
      ADMIN_BUY_EDIT,
      ADMIN_BUY_ADD,
      MAIL_ANNOUNCEMENT,
      MAIL_COMPENSATION;
   }
}
