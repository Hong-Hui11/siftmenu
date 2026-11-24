package com.echoes.siftmenu;

import com.mojang.math.Transformation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.Before;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class UltramarinePotFix {
   private static final String ENTITY_TAG = "siftmenu_potted_plant";
   private static final String ITEM_TAG_PREFIX = "siftmenu_potted_item:";
   private static final Map<String, Double> POT_RIM = Map.of(
      "ultramarine:blue_and_white_porcelain_flowerpot",
      1.0,
      "ultramarine:terracotta_flowerpot",
      1.0,
      "ultramarine:blue_porcelain_flowerpot",
      1.0,
      "ultramarine:black_iron_flowerpot",
      0.5
   );
   private static final double DEFAULT_RIM = 1.0;
   private static final double PLANT_SINK = 0.12;
   private static final double PLANT_TARGET_HEIGHT = 0.7;
   private static final double POT_INNER_WIDTH = 0.75;
   private static final List<String> POT_IDS = List.of(
      "ultramarine:blue_and_white_porcelain_flowerpot",
      "ultramarine:terracotta_flowerpot",
      "ultramarine:black_iron_flowerpot",
      "ultramarine:blue_porcelain_flowerpot"
   );
   private static final List<String> EXTRA_PLANTS = List.of(
      "minecraft:brown_mushroom",
      "minecraft:red_mushroom",
      "minecraft:crimson_fungus",
      "minecraft:warped_fungus",
      "minecraft:fern",
      "minecraft:dead_bush",
      "minecraft:short_grass",
      "minecraft:cactus",
      "minecraft:bamboo",
      "minecraft:azalea",
      "minecraft:flowering_azalea",
      "minecraft:torchflower",
      "minecraft:crimson_roots",
      "minecraft:warped_roots",
      "minecraft:nether_sprouts",
      "minecraft:mangrove_propagule"
   );
   private static Set<Block> potBlocks;
   private static Set<Item> extraPlants;
   private static int ticks;

   private UltramarinePotFix() {
   }

   public static void register() {
      if (FabricLoader.getInstance().isModLoaded("ultramarine")) {
         UseBlockCallback.EVENT.register(UltramarinePotFix::onUseBlock);
         PlayerBlockBreakEvents.BEFORE.register((Before)(var0, var1, var2, var3, var4) -> {
            if (var0 instanceof ServerLevel var5 && isPot(var3)) {
               BlockDisplay var6 = find(var5, var2);
               if (var6 == null) {
                  return true;
               }

               Item var7 = plantedItem(var6);
               var6.discard();
               if (var7 != null && !var1.isCreative()) {
                  Block.popResource(var5, var2, new ItemStack(var7));
               }

               return true;
            }

            return true;
         });
         ServerTickEvents.END_SERVER_TICK
            .register(
               (EndTick)var0 -> {
                  if (++ticks % 100 == 0) {
                     for (ServerPlayer var2 : var0.getPlayerList().getPlayers()) {
                        ServerLevel var4 = var2.level();
                        if (var4 instanceof ServerLevel) {
                           ServerLevel var3 = var4;

                           for (BlockDisplay var5 : var4.getEntitiesOfClass(
                              BlockDisplay.class, var2.getBoundingBox().inflate(24.0), var0x -> var0x.entityTags().contains("siftmenu_potted_plant")
                           )) {
                              if (!isPot(var3.getBlockState(var5.blockPosition()))) {
                                 var5.discard();
                              }
                           }
                        }
                     }
                  }
               }
            );
      }
   }

   private static InteractionResult onUseBlock(Player var0, Level var1, InteractionHand var2, BlockHitResult var3) {
      if (var2 != InteractionHand.MAIN_HAND || var0.isSpectator()) {
         return InteractionResult.PASS;
      } else if (var1.isClientSide()) {
         return InteractionResult.PASS;
      } else if (var1 instanceof ServerLevel var4) {
         BlockPos var5 = var3.getBlockPos();
         if (!isPot(var4.getBlockState(var5))) {
            return InteractionResult.PASS;
         } else {
            ItemStack var6 = var0.getItemInHand(var2);
            BlockDisplay var7 = find(var4, var5);
            if (!var6.isEmpty() && isPlantable(var6) && !var0.isShiftKeyDown()) {
               if (var7 != null) {
                  return InteractionResult.PASS;
               } else if (spawnPlant(var4, var5, var4.getBlockState(var5), ((BlockItem)var6.getItem()).getBlock().defaultBlockState(), var6.getItem())) {
                  if (!var0.isCreative()) {
                     var6.shrink(1);
                  }

                  var4.playSound(null, var5, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                  return InteractionResult.SUCCESS_SERVER;
               } else {
                  return InteractionResult.PASS;
               }
            } else if (var6.isEmpty() && var7 != null) {
               Item var8 = plantedItem(var7);
               var7.discard();
               if (var8 != null) {
                  ItemStack var9 = new ItemStack(var8);
                  if (!var0.getInventory().add(var9)) {
                     var0.drop(var9, false, Prediction.SERVER_ONLY);
                  }

                  var4.playSound(null, var5, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
               }

               return InteractionResult.SUCCESS_SERVER;
            } else {
               return InteractionResult.PASS;
            }
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private static boolean spawnPlant(ServerLevel var0, BlockPos var1, BlockState var2, BlockState var3, Item var4) {
      EntityType var5 = (EntityType)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("block_display"));
      if (var5 == null) {
         return false;
      } else {
         double var6 = 0.8;
         double var8 = 0.6;

         try {
            VoxelShape var10 = var3.getShape(var0, var1);
            if (!var10.isEmpty()) {
               var6 = Math.max(0.05, var10.max(Axis.Y));
               var8 = Math.max(0.1, Math.max(var10.max(Axis.X) - var10.min(Axis.X), var10.max(Axis.Z) - var10.min(Axis.Z)));
            }
         } catch (RuntimeException var21) {
         }

         double var22 = Math.min(0.7 / var6, 0.75 / var8);
         var22 = Math.max(0.35, Math.min(0.95, var22));
         double var12 = 1.0;
         Identifier var14 = BuiltInRegistries.BLOCK.getKey(var2.getBlock());
         if (var14 != null) {
            var12 = POT_RIM.getOrDefault(var14.toString(), 1.0);
         }

         CompoundTag var15 = new CompoundTag();
         var15.putString("id", "minecraft:block_display");
         var15.put("block_state", NbtUtils.writeBlockState(var3));
         Transformation var16 = new Transformation(
            new Vector3f((float)(-var22 / 2.0), 0.0F, (float)(-var22 / 2.0)),
            new Quaternionf(),
            new Vector3f((float)var22, (float)var22, (float)var22),
            new Quaternionf()
         );
         Tag var17 = (Tag)Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, var16).getOrThrow();
         var15.put("transformation", var17);
         ListTag var18 = new ListTag();
         var18.add(DoubleTag.valueOf((double)var1.getX() + 0.5));
         var18.add(DoubleTag.valueOf((double)var1.getY() + var12 - 0.12));
         var18.add(DoubleTag.valueOf((double)var1.getZ() + 0.5));
         var15.put("Pos", var18);
         if (EntityType.loadEntityRecursive(var5, var15, var0, EntitySpawnReason.COMMAND, EntityProcessor.NOP) instanceof BlockDisplay var20) {
            var20.addTag("siftmenu_potted_plant");
            var20.addTag("siftmenu_potted_item:" + BuiltInRegistries.ITEM.getKey(var4));
            return var0.addFreshEntity(var20);
         } else {
            return false;
         }
      }
   }

   private static BlockDisplay find(Level var0, BlockPos var1) {
      List var2 = var0.getEntitiesOfClass(
         BlockDisplay.class, new AABB(var1).inflate(0.5), var1x -> var1x.entityTags().contains("siftmenu_potted_plant") && var1x.blockPosition().equals(var1)
      );
      return var2.isEmpty() ? null : (BlockDisplay)var2.get(0);
   }

   private static Item plantedItem(BlockDisplay var0) {
      for (String var2 : var0.entityTags()) {
         if (var2.startsWith("siftmenu_potted_item:")) {
            Identifier var3 = Identifier.tryParse(var2.substring("siftmenu_potted_item:".length()));
            if (var3 != null) {
               Item var4 = (Item)BuiltInRegistries.ITEM.getValue(var3);
               return var4 != null && var4 != Items.AIR ? var4 : null;
            }
         }
      }

      return null;
   }

   private static boolean isPot(BlockState var0) {
      return potBlocks().contains(var0.getBlock());
   }

   private static Set<Block> potBlocks() {
      Set var0 = potBlocks;
      if (var0 != null) {
         return var0;
      } else {
         HashSet var1 = new HashSet();

         for (String var3 : POT_IDS) {
            Identifier var4 = Identifier.tryParse(var3);
            if (var4 != null) {
               Block var5 = (Block)BuiltInRegistries.BLOCK.getValue(var4);
               if (var5 != null && var5 != Blocks.AIR) {
                  var1.add(var5);
               }
            }
         }

         potBlocks = var1;
         return var1;
      }
   }

   private static boolean isPlantable(ItemStack var0) {
      if (!var0.isEmpty() && var0.getItem() instanceof BlockItem var1) {
         BlockState var3 = var1.getBlock().defaultBlockState();
         return !var3.is(BlockTags.SMALL_FLOWERS) && !var3.is(BlockTags.SAPLINGS) ? extraPlants().contains(var0.getItem()) : true;
      } else {
         return false;
      }
   }

   private static Set<Item> extraPlants() {
      Set var0 = extraPlants;
      if (var0 != null) {
         return var0;
      } else {
         HashSet var1 = new HashSet();

         for (String var3 : EXTRA_PLANTS) {
            Identifier var4 = Identifier.tryParse(var3);
            if (var4 != null) {
               Item var5 = (Item)BuiltInRegistries.ITEM.getValue(var4);
               if (var5 != null && var5 != Items.AIR) {
                  var1.add(var5);
               }
            }
         }

         extraPlants = var1;
         return var1;
      }
   }
}
