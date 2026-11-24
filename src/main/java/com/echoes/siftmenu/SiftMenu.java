package com.echoes.siftmenu;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AfterDamage;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AfterDeath;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDamage;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AllowDeath;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AfterRespawn;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.CopyFrom;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.After;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents.AllowChatMessage;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Disconnect;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SiftMenu implements ModInitializer {
   public static final String MOD_ID = "siftmenu";
   public static final Logger LOGGER = LoggerFactory.getLogger("siftmenu");
   private static final Map<UUID, ItemStack> KEPT_ELYTRA = new HashMap<>();
   private static int tickCounter;

   public void onInitialize() {
      PayloadTypeRegistry.serverboundPlay().register(OpenMenuPayload.TYPE, OpenMenuPayload.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(BingoWinPayload.TYPE, BingoWinPayload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(BingoClaimPayload.TYPE, BingoClaimPayload.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(BingoClaimResultPayload.TYPE, BingoClaimResultPayload.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(BountyHudPayload.TYPE, BountyHudPayload.CODEC);
      PayloadTypeRegistry.clientboundPlay().register(OpenFtbPayload.TYPE, OpenFtbPayload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(BountyHudClickPayload.TYPE, BountyHudClickPayload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(TeamResyncPayload.TYPE, TeamResyncPayload.CODEC);
      PayloadTypeRegistry.serverboundPlay().register(TeamStatusPayload.TYPE, TeamStatusPayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(TeamStatusPayload.TYPE, (var0, var1) -> {
         ServerPlayer var2 = var1.player();
         LOGGER.info(
            "[siftmenu] {} 客户端队伍数据状态（第 {} 次检查）：{}",
            var2.getGameProfile().name(),
            var0.stage(),
            var0.synced() ? "已同步" : "未同步"
         );
      });
      ServerPlayNetworking.registerGlobalReceiver(TeamResyncPayload.TYPE, (var0, var1) -> {
         ServerPlayer var2 = var1.player();
         var1.server().execute(() -> TeamSyncFixer.resyncNow(var2));
      });
      ServerPlayNetworking.registerGlobalReceiver(OpenMenuPayload.TYPE, (var0, var1) -> {
         ServerPlayer var2 = var1.player();
         var1.server().execute(() -> MenuManager.openMain(var2));
      });
      ServerPlayNetworking.registerGlobalReceiver(BountyHudClickPayload.TYPE, (var0, var1) -> {
         ServerPlayer var2 = var1.player();
         var1.server().execute(() -> MenuManager.openBountyDetail(var2, var0.boardId()));
      });
      ServerPlayNetworking.registerGlobalReceiver(
         BingoClaimPayload.TYPE, (var0, var1) -> var1.server().execute(() -> BingoReward.handleClaim(var1.player(), var0.token()))
      );
      if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && MenuManager.isBingoServer()) {
         BingoHook.register();
      }

      UltramarinePotFix.register();
      CommandRegistrationCallback.EVENT
         .register(
            (CommandRegistrationCallback)(var0, var1, var2) -> var0.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                               "siftmenu"
                                                            )
                                                            .executes(var0x -> {
                                                               MenuManager.openMain(((CommandSourceStack)var0x.getSource()).getPlayerOrException());
                                                               return 1;
                                                            }))
                                                         .then(Commands.literal("book").executes(var0x -> {
                                                            giveBook(((CommandSourceStack)var0x.getSource()).getPlayerOrException());
                                                            return 1;
                                                         })))
                                                      .then(Commands.literal("global").executes(var0x -> {
                                                         if (rejectEconomyOnBingo((CommandSourceStack)var0x.getSource())) {
                                                            return 0;
                                                         } else {
                                                            MenuManager.openGlobalMarket(((CommandSourceStack)var0x.getSource()).getPlayerOrException());
                                                            return 1;
                                                         }
                                                      })))
                                                   .then(((LiteralArgumentBuilder)Commands.literal("bounty").executes(var0x -> {
                                                      if (rejectEconomyOnBingo((CommandSourceStack)var0x.getSource())) {
                                                         return 0;
                                                      } else {
                                                         MenuManager.openBounty(((CommandSourceStack)var0x.getSource()).getPlayerOrException());
                                                         return 1;
                                                      }
                                                   })).then(
                                                      ((LiteralArgumentBuilder)Commands.literal("reset").requires(var0x -> Titles.isAdmin(var0x)))
                                                         .executes(var0x -> {
                                                            ServerPlayer var1x = ((CommandSourceStack)var0x.getSource()).getPlayerOrException();
                                                            BountyService.reset(MenuData.get());
                                                            var1x.sendSystemMessage(Component.literal("已强制重刷悬赏板，并清空当日接取计数。").withStyle(ChatFormatting.GREEN));
                                                            MenuManager.openBounty(var1x);
                                                            return 1;
                                                         })
                                                   )))
                                                .then(Commands.literal("quest").executes(var0x -> {
                                                   if (rejectEconomyOnBingo((CommandSourceStack)var0x.getSource())) {
                                                      return 0;
                                                   } else {
                                                      MenuManager.openBounty(((CommandSourceStack)var0x.getSource()).getPlayerOrException());
                                                      return 1;
                                                   }
                                                })))
                                             .then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("free").executes(var0x -> {
                                                if (rejectEconomyOnBingo((CommandSourceStack)var0x.getSource())) {
                                                   return 0;
                                                } else {
                                                   MenuManager.openFreeMarket(((CommandSourceStack)var0x.getSource()).getPlayerOrException());
                                                   return 1;
                                                }
                                             })).then(Commands.literal("sell").executes(var0x -> {
                                                if (rejectEconomyOnBingo((CommandSourceStack)var0x.getSource())) {
                                                   return 0;
                                                } else {
                                                   MenuManager.openFreeSell(((CommandSourceStack)var0x.getSource()).getPlayerOrException());
                                                   return 1;
                                                }
                                             }))).then(Commands.literal("buy").executes(var0x -> {
                                                if (rejectEconomyOnBingo((CommandSourceStack)var0x.getSource())) {
                                                   return 0;
                                                } else {
                                                   MenuManager.openFreeBuy(((CommandSourceStack)var0x.getSource()).getPlayerOrException());
                                                   return 1;
                                                }
                                             }))))
                                          .then(Commands.literal("balance").executes(var0x -> {
                                             if (rejectEconomyOnBingo((CommandSourceStack)var0x.getSource())) {
                                                return 0;
                                             } else {
                                                ServerPlayer var1x = ((CommandSourceStack)var0x.getSource()).getPlayerOrException();
                                                var1x.sendSystemMessage(
                                                   Component.literal("我的金币：" + Economy.format(Economy.balance(var1x))).withStyle(ChatFormatting.GOLD)
                                                );
                                                return 1;
                                             }
                                          })))
                                       .then(Commands.literal("titles").executes(var0x -> {
                                          MenuManager.openTitles(((CommandSourceStack)var0x.getSource()).getPlayerOrException());
                                          return 1;
                                       })))
                                    .then(Commands.literal("bingowin").executes(var0x -> {
                                       ServerPlayer var1x = ((CommandSourceStack)var0x.getSource()).getPlayerOrException();
                                       if (!BingoReward.hasRecentWin(var1x.getUUID())) {
                                          var1x.sendSystemMessage(Component.literal("你最近没有赢下 Bingo 对局。").withStyle(ChatFormatting.RED));
                                          return 0;
                                       } else {
                                          BingoReward.returnToMain(var1x);
                                          return 1;
                                       }
                                    })))
                                 .then(((LiteralArgumentBuilder)Commands.literal("admin").requires(var0x -> Titles.isAdmin(var0x))).executes(var0x -> {
                                    MenuManager.openAdmin(((CommandSourceStack)var0x.getSource()).getPlayerOrException());
                                    return 1;
                                 })))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("pubhome").requires(var0x -> Titles.isAdmin(var0x)))
                                    .then(Commands.literal("purge").executes(var0x -> {
                                       int var1x = MenuData.get().unpublishPublicHomesOutside("minecraft:overworld");
                                       ((CommandSourceStack)var0x.getSource()).sendSuccess(() -> Component.literal("已移除主世界之外的公共传送点：" + var1x + " 个"), true);
                                       return var1x;
                                    }))
                              ))
                           .then(mailCommand(var1)))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("party")
                                 .then(Commands.literal("accept").then(Commands.argument("player", StringArgumentType.word()).executes(var0x -> {
                                    PartyManager.accept(
                                       ((CommandSourceStack)var0x.getSource()).getPlayerOrException(), StringArgumentType.getString(var0x, "player")
                                    );
                                    return 1;
                                 }))))
                              .then(Commands.literal("deny").then(Commands.argument("player", StringArgumentType.word()).executes(var0x -> {
                                 PartyManager.deny(
                                    ((CommandSourceStack)var0x.getSource()).getPlayerOrException(), StringArgumentType.getString(var0x, "player")
                                 );
                                 return 1;
                              })))
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("teleport")
                              .then(Commands.literal("accept").then(Commands.argument("player", StringArgumentType.word()).executes(var0x -> {
                                 TeleportRequestManager.accept(
                                    ((CommandSourceStack)var0x.getSource()).getPlayerOrException(), StringArgumentType.getString(var0x, "player")
                                 );
                                 return 1;
                              }))))
                           .then(Commands.literal("deny").then(Commands.argument("player", StringArgumentType.word()).executes(var0x -> {
                              TeleportRequestManager.deny(
                                 ((CommandSourceStack)var0x.getSource()).getPlayerOrException(), StringArgumentType.getString(var0x, "player")
                              );
                              return 1;
                           })))
                     )
                     .then(CustomMenus.command())
               )
         );
      ServerLivingEntityEvents.AFTER_DEATH.register((AfterDeath)(var0, var1) -> {
         if (var0 instanceof ServerPlayer var2) {
            MenuData.Death var3 = new MenuData.Death();
            var3.x = var2.getX();
            var3.y = var2.getY();
            var3.z = var2.getZ();
            var3.dim = var2.level().dimension().identifier().toString();
            var3.time = System.currentTimeMillis();
            MenuData.get().addDeath(var2.getUUID(), var3);
            BountyService.onDeath(var2);
         } else {
            ServerPlayer var4 = killerOf(var1);
            if (var4 != null) {
               BountyService.onKill(var4, BuiltInRegistries.ENTITY_TYPE.getKey(var0.getType()).toString());
            }
         }
      });
      ServerLivingEntityEvents.AFTER_DAMAGE.register((AfterDamage)(var0, var1, var2, var3, var4) -> {
         if (!var4 && !(var3 <= 0.0F)) {
            if (var0 instanceof ServerPlayer var5) {
               BountyService.onDamageTaken(var5, (double)var3);
            }

            ServerPlayer var6 = killerOf(var1);
            if (var6 != null && var6 != var0) {
               BountyService.onDamageDealt(var6, (double)var3);
            }
         }
      });
      PlayerBlockBreakEvents.AFTER.register((After)(var0, var1, var2, var3, var4) -> {
         if (var1 instanceof ServerPlayer var5) {
            String var6 = BuiltInRegistries.BLOCK.getKey(var3.getBlock()).toString();
            BountyService.onBlockBreak(var5, var6);
            Item var7 = var3.getBlock().asItem();
            if (var7 != Items.AIR) {
               String var8 = BuiltInRegistries.ITEM.getKey(var7).toString();
               if (!var8.equals(var6)) {
                  BountyService.onBlockBreak(var5, var8);
               }
            }
         }
      });
      ServerLivingEntityEvents.ALLOW_DEATH.register((AllowDeath)(var0, var1, var2) -> {
         if (var0 instanceof ServerPlayer var3) {
            ItemStack var4 = var3.getItemBySlot(EquipmentSlot.BODY);
            if (!var4.isEmpty()) {
               var3.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
               String var5 = MarketItems.encodeUnit(var4, var3.level().registryAccess());
               if (!var5.isEmpty()) {
                  MenuData.get().setKeptElytra(var3.getUUID(), var5);
               } else {
                  KEPT_ELYTRA.put(var3.getUUID(), var4.copy());
                  LOGGER.warn("[siftmenu] {} 的鞘翅槽物品无法序列化，暂存内存（重启会丢失）", var3.getName().getString());
               }

               LOGGER.info("[siftmenu] 收存 {} 的鞘翅槽物品 {}，等待复活/重连归还", var3.getName().getString(), var4.getItem());
            }
         }

         return true;
      });
      ServerPlayerEvents.COPY_FROM.register((CopyFrom)(var0, var1, var2) -> {
         if (var0 != var1) {
            ItemStack var3 = var0.getItemBySlot(EquipmentSlot.BODY);
            if (!var3.isEmpty() && var1.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
               var1.setItemSlot(EquipmentSlot.BODY, var3.copy());
            }
         }
      });
      ServerPlayerEvents.AFTER_RESPAWN.register((AfterRespawn)(var0, var1, var2) -> restoreKeptElytra(var1));
      ServerLivingEntityEvents.ALLOW_DAMAGE.register((AllowDamage)(var0, var1, var2) -> {
         if (var0 instanceof ServerPlayer var3 && var1.getEntity() instanceof ServerPlayer var4 && var4 != var3) {
            MenuData.Party var6 = MenuData.get().partyOf(var3.getUUID());
            if (var6 != null && !var6.pvp && var6.members.contains(var4.getUUID().toString())) {
               return false;
            }
         }

         return true;
      });
      ServerPlayConnectionEvents.JOIN.register((Join)(var0, var1, var2) -> {
         ServerPlayer var3 = var0.player;
         var2.execute(() -> {
            MenuData.get().recordFirstSeen(var3.getUUID());
            boolean var1x = MenuManager.isBingoServer();
            if (!var1x) {
               Economy.ensureAccount(var3);
               FreeMarket.deliverPending(var3);
               BountyService.ensureBoard(MenuData.get());
               BountyService.pushHud(var3);
            }

            if (MenuData.get().bookGiven.add(var3.getUUID().toString())) {
               MenuData.get().save();
               giveBook(var3);
            }

            restoreKeptElytra(var3);
            Mailbox.notifyPending(var3);
            Titles.refreshName(var3);
            TeamSyncFixer.onJoin(var3);
         });
      });
      ServerPlayConnectionEvents.DISCONNECT.register((Disconnect)(var0, var1) -> TeamSyncFixer.onLeave(var0.player));
      ServerTickEvents.END_SERVER_TICK.register((EndTick)var0 -> {
         tickCounter++;
         TeamSyncFixer.tick(var0);
         if (tickCounter % 6000 == 0) {
            FreeMarket.cleanup(var0);
         }

         if (tickCounter % 4 == 0) {
            Titles.tickNames(var0);
         }

         if (!MenuManager.isBingoServer()) {
            BountyService.tickTravel(var0);
            BountyService.tickPlayTime(var0);
         }

         if (tickCounter % 20 == 0 && !MenuManager.isBingoServer()) {
            BountyService.tick(var0);
         }

         if (tickCounter % 20 == 0) {
            LobbyTimeSync.tick(var0);
            LobbySpawnGuard.tick(var0);
         }
      });
      UseItemCallback.EVENT.register((UseItemCallback)(var0, var1, var2) -> {
         ItemStack var3 = var0.getItemInHand(var2);
         if (!var1.isClientSide() && isMenuBook(var3) && var0 instanceof ServerPlayer var5) {
            MenuManager.openMain(var5);
            return InteractionResult.SUCCESS_SERVER;
         }

         return InteractionResult.PASS;
      });
      ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((AllowChatMessage)(var0, var1, var2) -> !ChatInput.handle(var1, var0.signedContent()));
      ServerLifecycleEvents.SERVER_STARTED.register((ServerStarted)var0 -> {
         FreeMarket.cleanup(var0);
         MenuConfig.reload();
         CustomMenus.reload();
         var0.getGameRules().set(GameRules.KEEP_INVENTORY, true, var0);
      });
      ServerLifecycleEvents.SERVER_STOPPING.register((ServerStopping)var0 -> MenuData.get().save());
      CleanupManager.register();
      AnvilUnseal.register();
      ChunkUnloadManager.register();
      SleepSkip.register();
      LobbyBuildGuard.register();
      LobbyMobGuard.register();
   }

   private static boolean rejectEconomyOnBingo(CommandSourceStack var0) {
      if (!MenuManager.isBingoServer()) {
         return false;
      } else {
         var0.sendFailure(Component.literal("Bingo 服没有经济系统。"));
         return true;
      }
   }

   private static LiteralArgumentBuilder<CommandSourceStack> mailCommand(CommandBuildContext var0) {
      LiteralArgumentBuilder var1 = (LiteralArgumentBuilder)Commands.literal("announce")
         .then(
            Commands.argument("title", StringArgumentType.string())
               .then(
                  Commands.argument("body", StringArgumentType.greedyString())
                     .executes(
                        var0x -> {
                           Mailbox.sendAnnouncement(
                              ((CommandSourceStack)var0x.getSource()).getServer(),
                              ((CommandSourceStack)var0x.getSource()).getTextName(),
                              ((CommandSourceStack)var0x.getSource())::sendSystemMessage,
                              StringArgumentType.getString(var0x, "title"),
                              StringArgumentType.getString(var0x, "body")
                           );
                           return 1;
                        }
                     )
               )
         );
      ArgumentBuilder var2 = Commands.argument("note", StringArgumentType.greedyString())
         .executes(var0x -> compensateMail(var0x, StringArgumentType.getString(var0x, "note")));
      ArgumentBuilder var3 = ((RequiredArgumentBuilder)Commands.argument("item", ItemArgument.item(var0)).executes(var0x -> compensateMail(var0x, "")))
         .then(var2);
      ArgumentBuilder var4 = Commands.argument("count", IntegerArgumentType.integer(1, 576)).then(var3);
      ArgumentBuilder var5 = Commands.argument("target", StringArgumentType.string()).then(var4);
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("mail")
               .requires(var0x -> var0x.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)))
            .then(var1))
         .then(Commands.literal("compensate").then(var5));
   }

   private static int compensateMail(CommandContext<CommandSourceStack> var0, String var1) {
      try {
         ItemStack var2 = ItemArgument.getItem(var0, "item").createItemStack(1);
         Mailbox.sendCompensation(
            ((CommandSourceStack)var0.getSource()).getServer(),
            ((CommandSourceStack)var0.getSource()).getTextName(),
            ((CommandSourceStack)var0.getSource())::sendSystemMessage,
            StringArgumentType.getString(var0, "target"),
            IntegerArgumentType.getInteger(var0, "count"),
            var2,
            var1
         );
      } catch (CommandSyntaxException var3) {
         ((CommandSourceStack)var0.getSource()).sendFailure(Component.literal("物品格式无效：" + var3.getMessage()));
      }

      return 1;
   }

   private static ServerPlayer killerOf(DamageSource var0) {
      Entity var1 = var0.getEntity();
      if (var1 instanceof ServerPlayer) {
         return (ServerPlayer)var1;
      } else {
         if (var1 instanceof Projectile var2) {
            Entity var4 = var2.getOwner();
            if (var4 instanceof ServerPlayer) {
               return (ServerPlayer)var4;
            }
         }

         return null;
      }
   }

   private static void restoreKeptElytra(ServerPlayer var0) {
      ItemStack var1 = KEPT_ELYTRA.remove(var0.getUUID());
      if (var1 == null || var1.isEmpty()) {
         String var2 = MenuData.get().takeKeptElytra(var0.getUUID());
         if (var2 == null || var2.isEmpty()) {
            return;
         }

         var1 = MarketItems.decode(var2, null, var0.level().registryAccess());
      }

      if (var1.isEmpty()) {
         LOGGER.warn("[siftmenu] {} 的待归还鞘翅数据无法恢复", var0.getName().getString());
      } else {
         if (var0.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
            var0.setItemSlot(EquipmentSlot.BODY, var1);
         } else if (!var0.getInventory().add(var1)) {
            String var3 = MarketItems.encodeUnit(var1, var0.level().registryAccess());
            if (!var3.isEmpty()) {
               MenuData.get().setKeptElytra(var0.getUUID(), var3);
            } else {
               KEPT_ELYTRA.put(var0.getUUID(), var1.copy());
            }

            var0.sendSystemMessage(Component.literal("背包已满，鞘翅已为你保留，清理背包后重新进服即可领取").withStyle(ChatFormatting.YELLOW));
            LOGGER.info("[siftmenu] {} 背包已满，鞘翅继续保留", var0.getName().getString());
            return;
         }

         var0.sendSystemMessage(Component.literal("已为你保留鞘翅").withStyle(ChatFormatting.GREEN));
         LOGGER.info("[siftmenu] 已归还 {} 的鞘翅槽物品 {}", var0.getName().getString(), var1.getItem());
      }
   }

   public static void giveBook(ServerPlayer var0) {
      ItemStack var1 = new ItemStack(Items.BOOK);
      var1.set(DataComponents.CUSTOM_NAME, Component.literal("筛界菜单").withStyle(ChatFormatting.GOLD));
      var1.set(
         DataComponents.LORE,
         new ItemLore(
            List.of(Component.literal("右键打开菜单").withStyle(ChatFormatting.GRAY), Component.literal("也可以按快捷键打开（默认 G 键）").withStyle(ChatFormatting.DARK_GRAY))
         )
      );
      CompoundTag var2 = new CompoundTag();
      var2.putBoolean("siftmenu_book", true);
      var1.set(DataComponents.CUSTOM_DATA, CustomData.of(var2));
      if (!var0.getInventory().add(var1)) {
         var0.drop(var1, false, Prediction.SERVER_ONLY);
      }

      var0.sendSystemMessage(Component.literal("已获得《筛界菜单》，右键打开或按 G 键。").withStyle(ChatFormatting.GOLD));
   }

   public static boolean isMenuBook(ItemStack var0) {
      if (!var0.is(Items.BOOK)) {
         return false;
      } else {
         CustomData var1 = (CustomData)var0.get(DataComponents.CUSTOM_DATA);
         return var1 != null && var1.copyTag().contains("siftmenu_book");
      }
   }
}
