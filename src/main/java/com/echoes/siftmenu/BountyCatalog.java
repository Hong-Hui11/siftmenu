package com.echoes.siftmenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BountyCatalog {
   public static final String BLOCK_BREAK = "block_break";
   public static final String BLOCK_PLACE = "block_place";
   public static final String KILL = "kill";
   public static final String CRAFT = "craft";
   public static final String SMELT = "smelt";
   public static final String CONSUME = "consume";
   public static final String FISH = "fish";
   public static final String ENCHANT = "enchant";
   public static final String BREED = "breed";
   public static final String TAME = "tame";
   public static final String TRADE = "trade";
   public static final String TRAVEL = "travel";
   public static final String DAMAGE_DEALT = "damage_dealt";
   public static final String DAMAGE_TAKEN = "damage_taken";
   public static final String XP_GAIN = "xp_gain";
   public static final String PICKUP = "pickup";
   public static final String SLEEP = "sleep";
   public static final String PLAY_TIME = "play_time";
   public static final String SUBMIT_ITEM = "submit_item";
   public static final String BOSS_KILL = "boss_kill";
   public static final String ADVANCEMENT = "advancement";
   private static final int[] COUNT_BY_DIFFICULTY = new int[]{240, 180, 120, 60};
   private static final String[][] BLOCKS = new String[][]{
      {"minecraft:stone", "石头"},
      {"minecraft:deepslate", "深板岩"},
      {"minecraft:cobblestone", "圆石"},
      {"minecraft:mossy_cobblestone", "苔石"},
      {"minecraft:granite", "花岗岩"},
      {"minecraft:diorite", "闪长岩"},
      {"minecraft:andesite", "安山岩"},
      {"minecraft:tuff", "凝灰岩"},
      {"minecraft:calcite", "方解石"},
      {"minecraft:basalt", "玄武岩"},
      {"minecraft:blackstone", "黑石"},
      {"minecraft:netherrack", "下界岩"},
      {"minecraft:soul_sand", "灵魂沙"},
      {"minecraft:soul_soil", "灵魂土"},
      {"minecraft:magma_block", "岩浆块"},
      {"minecraft:glowstone", "萤石"},
      {"minecraft:obsidian", "黑曜石"},
      {"minecraft:crying_obsidian", "哭泣的黑曜石"},
      {"minecraft:end_stone", "末地石"},
      {"minecraft:purpur_block", "紫珀块"},
      {"minecraft:chorus_plant", "紫颂植株"},
      {"minecraft:ancient_debris", "远古残骸"},
      {"minecraft:sandstone", "砂岩"},
      {"minecraft:red_sand", "红沙"},
      {"minecraft:sand", "沙子"},
      {"minecraft:gravel", "沙砾"},
      {"minecraft:dirt", "泥土"},
      {"minecraft:grass_block", "草方块"},
      {"minecraft:clay", "黏土"},
      {"minecraft:snow_block", "雪块"},
      {"minecraft:ice", "冰"},
      {"minecraft:packed_ice", "浮冰"},
      {"minecraft:blue_ice", "蓝冰"},
      {"minecraft:bone_block", "骨块"},
      {"minecraft:sponge", "海绵"},
      {"minecraft:hay_block", "干草捆"},
      {"minecraft:melon", "西瓜"},
      {"minecraft:pumpkin", "南瓜"},
      {"minecraft:cactus", "仙人掌"},
      {"minecraft:sugar_cane", "甘蔗"},
      {"minecraft:bamboo", "竹子"},
      {"minecraft:moss_block", "苔藓块"},
      {"minecraft:sculk", "幽匿块"},
      {"minecraft:amethyst_block", "紫水晶块"},
      {"minecraft:quartz_block", "石英块"},
      {"minecraft:nether_bricks", "下界砖块"},
      {"minecraft:prismarine", "海晶石"},
      {"minecraft:sea_lantern", "海晶灯"},
      {"minecraft:terracotta", "陶瓦"},
      {"minecraft:white_wool", "白色羊毛"},
      {"minecraft:glass", "玻璃"},
      {"minecraft:bricks", "砖块"},
      {"minecraft:stone_bricks", "石砖"},
      {"minecraft:deepslate_bricks", "深板岩砖"},
      {"minecraft:polished_deepslate", "磨制深板岩"},
      {"minecraft:polished_granite", "磨制花岗岩"},
      {"minecraft:polished_diorite", "磨制闪长岩"},
      {"minecraft:polished_andesite", "磨制安山岩"},
      {"minecraft:smooth_stone", "平滑石头"},
      {"minecraft:mud", "泥巴"},
      {"minecraft:packed_mud", "泥坯"},
      {"minecraft:mud_bricks", "泥砖"},
      {"minecraft:dripstone_block", "滴水石块"},
      {"minecraft:pointed_dripstone", "滴水石锥"},
      {"minecraft:crimson_nylium", "绯红菌岩"},
      {"minecraft:warped_nylium", "诡异菌岩"},
      {"minecraft:nether_wart_block", "下界疣块"},
      {"minecraft:shroomlight", "菌光体"},
      {"minecraft:gilded_blackstone", "镶金黑石"},
      {"minecraft:smooth_basalt", "平滑玄武岩"},
      {"minecraft:cobbled_deepslate", "深板岩圆石"},
      {"minecraft:copper_block", "铜块"},
      {"minecraft:raw_iron_block", "粗铁块"},
      {"minecraft:raw_copper_block", "粗铜块"},
      {"minecraft:raw_gold_block", "粗金块"},
      {"minecraft:iron_block", "铁块"},
      {"minecraft:gold_block", "金块"},
      {"minecraft:diamond_block", "钻石块"},
      {"minecraft:emerald_block", "绿宝石块"},
      {"minecraft:lapis_block", "青金石块"},
      {"minecraft:redstone_block", "红石块"},
      {"minecraft:coal_block", "煤炭块"},
      {"minecraft:coal_ore", "煤矿石"},
      {"minecraft:deepslate_coal_ore", "深层煤矿石"},
      {"minecraft:iron_ore", "铁矿石"},
      {"minecraft:deepslate_iron_ore", "深层铁矿石"},
      {"minecraft:copper_ore", "铜矿石"},
      {"minecraft:deepslate_copper_ore", "深层铜矿石"},
      {"minecraft:gold_ore", "金矿石"},
      {"minecraft:deepslate_gold_ore", "深层金矿石"},
      {"minecraft:redstone_ore", "红石矿石"},
      {"minecraft:deepslate_redstone_ore", "深层红石矿石"},
      {"minecraft:lapis_ore", "青金石矿石"},
      {"minecraft:deepslate_lapis_ore", "深层青金石矿石"},
      {"minecraft:diamond_ore", "钻石矿石"},
      {"minecraft:deepslate_diamond_ore", "深层钻石矿石"},
      {"minecraft:emerald_ore", "绿宝石矿石"},
      {"minecraft:deepslate_emerald_ore", "深层绿宝石矿石"},
      {"minecraft:nether_quartz_ore", "下界石英矿石"},
      {"minecraft:nether_gold_ore", "下界金矿石"},
      {"minecraft:oak_log", "橡木原木"},
      {"minecraft:spruce_log", "云杉原木"},
      {"minecraft:birch_log", "白桦原木"},
      {"minecraft:jungle_log", "丛林原木"},
      {"minecraft:acacia_log", "金合欢原木"},
      {"minecraft:dark_oak_log", "深色橡木原木"},
      {"minecraft:mangrove_log", "红树原木"},
      {"minecraft:cherry_log", "樱花原木"},
      {"minecraft:pale_oak_log", "苍白橡木原木"},
      {"minecraft:crimson_stem", "绯红菌柄"},
      {"minecraft:warped_stem", "诡异菌柄"},
      {"minecraft:stripped_oak_log", "去皮橡木原木"}
   };
   private static final String[][] MOBS = new String[][]{
      {"minecraft:zombie", "僵尸"},
      {"minecraft:skeleton", "骷髅"},
      {"minecraft:creeper", "苦力怕"},
      {"minecraft:spider", "蜘蛛"},
      {"minecraft:cave_spider", "洞穴蜘蛛"},
      {"minecraft:enderman", "末影人"},
      {"minecraft:witch", "女巫"},
      {"minecraft:pillager", "掠夺者"},
      {"minecraft:vindicator", "卫道士"},
      {"minecraft:evoker", "唤魔者"},
      {"minecraft:zombie_villager", "僵尸村民"},
      {"minecraft:husk", "尸壳"},
      {"minecraft:stray", "流浪者"},
      {"minecraft:drowned", "溺尸"},
      {"minecraft:phantom", "幻翼"},
      {"minecraft:slime", "史莱姆"},
      {"minecraft:magma_cube", "岩浆怪"},
      {"minecraft:ghast", "恶魂"},
      {"minecraft:blaze", "烈焰人"},
      {"minecraft:wither_skeleton", "凋灵骷髅"},
      {"minecraft:piglin", "猪灵"},
      {"minecraft:piglin_brute", "猪灵蛮兵"},
      {"minecraft:hoglin", "疣猪兽"},
      {"minecraft:zoglin", "僵尸疣猪兽"},
      {"minecraft:guardian", "守卫者"},
      {"minecraft:elder_guardian", "远古守卫者"},
      {"minecraft:shulker", "潜影贝"},
      {"minecraft:silverfish", "蠹虫"},
      {"minecraft:endermite", "末影螨"},
      {"minecraft:ravager", "劫掠兽"},
      {"minecraft:warden", "监守者"},
      {"minecraft:breeze", "旋风人"},
      {"minecraft:bogged", "沼骸"},
      {"minecraft:vex", "恼鬼"}
   };
   private static final String[][] CRAFT_ITEMS = new String[][]{
      {"minecraft:crafting_table", "工作台"},
      {"minecraft:stick", "木棍"},
      {"minecraft:oak_planks", "橡木木板"},
      {"minecraft:torch", "火把"},
      {"minecraft:chest", "箱子"},
      {"minecraft:furnace", "熔炉"},
      {"minecraft:ladder", "梯子"},
      {"minecraft:paper", "纸"},
      {"minecraft:book", "书"},
      {"minecraft:bookshelf", "书架"},
      {"minecraft:bucket", "桶"},
      {"minecraft:glass_bottle", "玻璃瓶"},
      {"minecraft:bread", "面包"},
      {"minecraft:iron_pickaxe", "铁镐"},
      {"minecraft:iron_axe", "铁斧"},
      {"minecraft:iron_sword", "铁剑"},
      {"minecraft:iron_helmet", "铁头盔"},
      {"minecraft:iron_chestplate", "铁胸甲"},
      {"minecraft:iron_leggings", "铁护腿"},
      {"minecraft:iron_boots", "铁靴子"},
      {"minecraft:shield", "盾牌"},
      {"minecraft:bow", "弓"},
      {"minecraft:arrow", "箭"},
      {"minecraft:crossbow", "弩"},
      {"minecraft:hopper", "漏斗"},
      {"minecraft:piston", "活塞"},
      {"minecraft:observer", "侦测器"},
      {"minecraft:dispenser", "发射器"},
      {"minecraft:rail", "铁轨"},
      {"minecraft:minecart", "矿车"},
      {"minecraft:lantern", "灯笼"},
      {"minecraft:campfire", "营火"},
      {"minecraft:scaffolding", "脚手架"},
      {"minecraft:barrel", "木桶"}
   };
   private static final String[][] SMELT_ITEMS = new String[][]{
      {"minecraft:iron_ingot", "铁锭"},
      {"minecraft:gold_ingot", "金锭"},
      {"minecraft:copper_ingot", "铜锭"},
      {"minecraft:glass", "玻璃"},
      {"minecraft:charcoal", "木炭"},
      {"minecraft:stone", "石头"},
      {"minecraft:smooth_stone", "平滑石头"},
      {"minecraft:brick", "红砖"},
      {"minecraft:nether_brick", "下界砖"},
      {"minecraft:cooked_beef", "牛排"},
      {"minecraft:cooked_porkchop", "熟猪排"},
      {"minecraft:cooked_chicken", "熟鸡肉"},
      {"minecraft:cooked_mutton", "熟羊肉"},
      {"minecraft:cooked_cod", "熟鳕鱼"},
      {"minecraft:baked_potato", "烤马铃薯"},
      {"minecraft:dried_kelp", "干海带"},
      {"minecraft:terracotta", "陶瓦"},
      {"minecraft:cracked_stone_bricks", "裂纹石砖"},
      {"minecraft:sponge", "海绵"}
   };
   private static final String[][] SUBMIT_ITEMS = new String[][]{
      {"minecraft:iron_ingot", "铁锭"},
      {"minecraft:gold_ingot", "金锭"},
      {"minecraft:copper_ingot", "铜锭"},
      {"minecraft:redstone", "红石"},
      {"minecraft:lapis_lazuli", "青金石"},
      {"minecraft:diamond", "钻石"},
      {"minecraft:emerald", "绿宝石"},
      {"minecraft:coal", "煤炭"},
      {"minecraft:quartz", "下界石英"},
      {"minecraft:slime_ball", "粘液球"},
      {"minecraft:bone", "骨头"},
      {"minecraft:string", "线"},
      {"minecraft:gunpowder", "火药"},
      {"minecraft:wheat", "小麦"},
      {"minecraft:carrot", "胡萝卜"},
      {"minecraft:potato", "马铃薯"},
      {"minecraft:sugar_cane", "甘蔗"},
      {"minecraft:egg", "鸡蛋"},
      {"minecraft:leather", "皮革"},
      {"minecraft:white_wool", "白色羊毛"}
   };
   private static final String[][] BOSSES = new String[][]{
      {"minecraft:ender_dragon", "末影龙"},
      {"minecraft:wither", "凋灵"},
      {"minecraft:elder_guardian", "远古守卫者"},
      {"minecraft:warden", "监守者"},
      {"minecraft:ravager", "劫掠兽"},
      {"minecraft:evoker", "唤魔者"}
   };
   private static final String[][] ADVANCEMENTS = new String[][]{
      {"minecraft:adventure/adventuring_time", "探险时光"},
      {"minecraft:adventure/arbalistic", "弩神"},
      {"minecraft:adventure/blowback", "反弹"},
      {"minecraft:adventure/bullseye", "正中靶心"},
      {"minecraft:adventure/hero_of_the_village", "村庄英雄"},
      {"minecraft:adventure/kill_all_mobs", "怪物猎人"},
      {"minecraft:adventure/minecraft_trials_edition", "试炼之地"},
      {"minecraft:adventure/overoverkill", "过量击杀"},
      {"minecraft:adventure/revaulting", "再闯宝库"},
      {"minecraft:adventure/sniper_duel", "狙击对决"},
      {"minecraft:adventure/totem_of_undying", "死里逃生"},
      {"minecraft:adventure/trim_with_all_exclusive_armor_patterns", "风格锻造"},
      {"minecraft:adventure/two_birds_one_arrow", "一箭双雕"},
      {"minecraft:adventure/under_lock_and_key", "开启宝库"},
      {"minecraft:adventure/very_very_frightening", "惊悚万分"},
      {"minecraft:adventure/voluntary_exile", "自我放逐"},
      {"minecraft:adventure/who_needs_rockets", "无需火箭"},
      {"minecraft:adventure/whos_the_pillager_now", "反客为主"},
      {"minecraft:end/dragon_egg", "下一世代"},
      {"minecraft:end/elytra", "天空即极限"},
      {"minecraft:end/enter_end_gateway", "远程折返"},
      {"minecraft:end/find_end_city", "末地之城"},
      {"minecraft:end/kill_dragon", "解放末地"},
      {"minecraft:end/levitate", "高空视野"},
      {"minecraft:end/respawn_dragon", "末地再来一次"},
      {"minecraft:nether/all_effects", "怎么会变成这样"},
      {"minecraft:nether/all_potions", "狂乱鸡尾酒"},
      {"minecraft:nether/create_beacon", "带信标回家"},
      {"minecraft:nether/create_full_beacon", "信标学家"},
      {"minecraft:nether/explore_nether", "下界旅游胜地"},
      {"minecraft:nether/fast_travel", "亚空间气泡"},
      {"minecraft:nether/get_wither_skull", "骷髅头"},
      {"minecraft:nether/loot_bastion", "战猪"},
      {"minecraft:nether/netherite_armor", "残骸加身"},
      {"minecraft:nether/obtain_ancient_debris", "深藏不露"},
      {"minecraft:nether/return_to_sender", "以火还火"},
      {"minecraft:nether/summon_wither", "凋灵山庄"},
      {"minecraft:nether/uneasy_alliance", "脆弱的同盟"},
      {"minecraft:husbandry/balanced_diet", "均衡饮食"},
      {"minecraft:husbandry/bred_all_animals", "成双成对"},
      {"minecraft:husbandry/complete_catalogue", "猫类图鉴"},
      {"minecraft:husbandry/froglights", "蛙明灯"},
      {"minecraft:husbandry/leash_all_frog_variants", "蛙蛙小队"},
      {"minecraft:husbandry/obtain_netherite_hoe", "严肃的奉献"},
      {"minecraft:husbandry/whole_pack", "狼群"},
      {"minecraft:husbandry/allay_deliver_cake_to_note_block", "生日快乐"},
      {"minecraft:story/cure_zombie_villager", "僵尸医生"},
      {"minecraft:adventure/fall_from_world_height", "自由落体"},
      {"minecraft:adventure/kill_mob_near_sculk_catalyst", "幽匿蔓延"},
      {"minecraft:adventure/heart_transplanter", "心脏移植"},
      {"minecraft:nether/ride_strider_in_overworld_lava", "宾至如归"},
      {"minecraft:nether/charge_respawn_anchor", "充能重生锚"},
      {"minecraft:husbandry/obtain_sniffer_egg", "气味有趣"},
      {"minecraft:husbandry/place_dried_ghast_in_water", "保持水分"}
   };
   private static final String[][] EXTREME_TASKS = new String[][]{
      {"boss_kill", "minecraft:ender_dragon", "1", "屠龙令", "前往末地，亲手终结末影龙。", "1000"},
      {"boss_kill", "minecraft:wither", "1", "凋灵狩猎", "召唤并击败一只凋灵。", "1000"},
      {"boss_kill", "minecraft:elder_guardian", "1", "神殿守望者", "潜入海底神殿，击杀远古守卫者。", "800"},
      {"boss_kill", "minecraft:warden", "1", "静默猎手", "深入远古城市，击杀一只监守者。", "900"},
      {"boss_kill", "minecraft:ravager", "1", "劫掠兽围猎", "在袭击中击杀一只劫掠兽。", "900"},
      {"boss_kill", "minecraft:evoker", "1", "唤魔者的末日", "击杀一只唤魔者，小心尖牙与恼鬼。", "900"},
      {"advancement", "minecraft:adventure/adventuring_time", "1", "成就·探险时光", "走访主世界、下界、末地的全部生物群系。", "1500"},
      {"advancement", "minecraft:adventure/arbalistic", "1", "成就·弩神", "用一发弩箭击杀五种不同的生物。", "1300"},
      {"advancement", "minecraft:adventure/blowback", "1", "成就·反弹", "用反弹回来的旋风弹击杀旋风人。", "900"},
      {"advancement", "minecraft:adventure/bullseye", "1", "成就·正中靶心", "从 30 格开外命中标靶方块的红心。", "800"},
      {"advancement", "minecraft:adventure/hero_of_the_village", "1", "成就·村庄英雄", "成功守下一场村庄袭击。", "1000"},
      {"advancement", "minecraft:adventure/kill_all_mobs", "1", "成就·怪物猎人", "把每一种敌对生物都杀过一遍。", "1500"},
      {"advancement", "minecraft:adventure/minecraft_trials_edition", "1", "成就·试炼之地", "踏入任意一座试炼密室。", "700"},
      {"advancement", "minecraft:adventure/overoverkill", "1", "成就·过量击杀", "用重锤一击打出 50 颗心的伤害。", "1200"},
      {"advancement", "minecraft:adventure/revaulting", "1", "成就·再闯宝库", "用不祥试炼钥匙打开不祥宝库。", "1000"},
      {"advancement", "minecraft:adventure/sniper_duel", "1", "成就·狙击对决", "从 50 格开外射杀一只骷髅。", "1100"},
      {"advancement", "minecraft:adventure/totem_of_undying", "1", "成就·死里逃生", "用不死图腾挡下一次致命伤害。", "900"},
      {"advancement", "minecraft:adventure/trim_with_all_exclusive_armor_patterns", "1", "成就·风格锻造", "用尖塔、猪鼻、肋骨、监守、寂静、恼鬼、潮汐、向导八种模板各升级一次。", "1400"},
      {"advancement", "minecraft:adventure/two_birds_one_arrow", "1", "成就·一箭双雕", "用一支穿透箭串死两只幻翼。", "1000"},
      {"advancement", "minecraft:adventure/under_lock_and_key", "1", "成就·开启宝库", "用试炼钥匙打开一座宝库。", "800"},
      {"advancement", "minecraft:adventure/very_very_frightening", "1", "成就·惊悚万分", "用闪电劈中一名村民。", "900"},
      {"advancement", "minecraft:adventure/voluntary_exile", "1", "成就·自我放逐", "击杀一名袭击队长。", "800"},
      {"advancement", "minecraft:adventure/who_needs_rockets", "1", "成就·无需火箭", "用旋风弹把自己向上弹起 8 格。", "800"},
      {"advancement", "minecraft:adventure/whos_the_pillager_now", "1", "成就·反客为主", "用弩射杀一名掠夺者。", "800"},
      {"advancement", "minecraft:end/dragon_egg", "1", "成就·下一世代", "把龙蛋捧在手里。", "1100"},
      {"advancement", "minecraft:end/elytra", "1", "成就·天空即极限", "在末地城找到鞘翅。", "900"},
      {"advancement", "minecraft:end/enter_end_gateway", "1", "成就·远程折返", "穿过末地折跃门离开主岛。", "800"},
      {"advancement", "minecraft:end/find_end_city", "1", "成就·末地之城", "进入一座末地城。", "800"},
      {"advancement", "minecraft:end/kill_dragon", "1", "成就·解放末地", "在末地击败末影龙。", "1200"},
      {"advancement", "minecraft:end/levitate", "1", "成就·高空视野", "被潜影贝打上漂浮后升高 50 格。", "900"},
      {"advancement", "minecraft:end/respawn_dragon", "1", "成就·末地再来一次", "在末地重新召唤并再次击败末影龙。", "1200"},
      {"advancement", "minecraft:nether/all_effects", "1", "成就·怎么会变成这样", "同时拥有全部状态效果。", "1500"},
      {"advancement", "minecraft:nether/all_potions", "1", "成就·狂乱鸡尾酒", "同时拥有全部药水效果。", "1300"},
      {"advancement", "minecraft:nether/create_beacon", "1", "成就·带信标回家", "合成并放下一个信标。", "900"},
      {"advancement", "minecraft:nether/create_full_beacon", "1", "成就·信标学家", "把信标升到满级（四层金字塔）。", "1200"},
      {"advancement", "minecraft:nether/explore_nether", "1", "成就·下界旅游胜地", "走遍下界的全部生物群系。", "1300"},
      {"advancement", "minecraft:nether/fast_travel", "1", "成就·亚空间气泡", "借下界通道在主世界移动 7 公里。", "1000"},
      {"advancement", "minecraft:nether/get_wither_skull", "1", "成就·骷髅头", "拿到凋灵骷髅头颅。", "900"},
      {"advancement", "minecraft:nether/loot_bastion", "1", "成就·战猪", "在堡垒遗迹里搜刮一个箱子。", "900"},
      {"advancement", "minecraft:nether/netherite_armor", "1", "成就·残骸加身", "穿齐一整套下界合金盔甲。", "1300"},
      {"advancement", "minecraft:nether/obtain_ancient_debris", "1", "成就·深藏不露", "挖到远古残骸。", "1000"},
      {"advancement", "minecraft:nether/return_to_sender", "1", "成就·以火还火", "用恶魂的火球炸死恶魂。", "900"},
      {"advancement", "minecraft:nether/summon_wither", "1", "成就·凋灵山庄", "召唤一只凋灵。", "1000"},
      {"advancement", "minecraft:nether/uneasy_alliance", "1", "成就·脆弱的同盟", "把恶魂从下界带回主世界再击杀。", "1300"},
      {"advancement", "minecraft:husbandry/balanced_diet", "1", "成就·均衡饮食", "把游戏里所有能吃的东西都吃一遍。", "1400"},
      {"advancement", "minecraft:husbandry/bred_all_animals", "1", "成就·成双成对", "让每种可繁殖动物都繁殖一次。", "1300"},
      {"advancement", "minecraft:husbandry/complete_catalogue", "1", "成就·猫类图鉴", "驯服全部 11 种猫。", "1400"},
      {"advancement", "minecraft:husbandry/froglights", "1", "成就·蛙明灯", "集齐三种蛙明灯。", "1100"},
      {"advancement", "minecraft:husbandry/leash_all_frog_variants", "1", "成就·蛙蛙小队", "把三种颜色的青蛙同时拴在手上。", "900"},
      {"advancement", "minecraft:husbandry/obtain_netherite_hoe", "1", "成就·严肃的奉献", "用下界合金升级一把锄头。", "1200"},
      {"advancement", "minecraft:husbandry/whole_pack", "1", "成就·狼群", "驯服全部 9 种狼。", "1300"},
      {"advancement", "minecraft:husbandry/allay_deliver_cake_to_note_block", "1", "成就·生日快乐", "让悦灵把蛋糕丢在音符盒上。", "900"},
      {"advancement", "minecraft:story/cure_zombie_villager", "1", "成就·僵尸医生", "把僵尸村民治好。", "900"},
      {"advancement", "minecraft:adventure/fall_from_world_height", "1", "成就·自由落体", "从世界最高处自由落体到最底层并活下来。", "1000"},
      {"advancement", "minecraft:adventure/kill_mob_near_sculk_catalyst", "1", "成就·幽匿蔓延", "在幽匿催化体旁击杀一只生物。", "700"},
      {"advancement", "minecraft:adventure/heart_transplanter", "1", "成就·心脏移植", "在两根苍白橡木原木之间正确放置嘎枝之心。", "900"},
      {"advancement", "minecraft:nether/ride_strider_in_overworld_lava", "1", "成就·宾至如归", "骑着炽足兽在主世界的岩浆湖上跑一段长路。", "900"},
      {"advancement", "minecraft:nether/charge_respawn_anchor", "1", "成就·充能重生锚", "把重生锚充能到满。", "700"},
      {"advancement", "minecraft:husbandry/obtain_sniffer_egg", "1", "成就·气味有趣", "获得一枚嗅探兽蛋。", "900"},
      {"advancement", "minecraft:husbandry/place_dried_ghast_in_water", "1", "成就·保持水分", "把干燥的恶魂放进水里。", "700"}
   };
   private static final Map<String, String> NAMES = buildNames();
   private static final List<BountyCatalog.Bounty> ALL = buildAll();
   private static final Map<Integer, BountyCatalog.Bounty> BY_ID = buildIndex();

   private BountyCatalog() {
   }

   public static List<BountyCatalog.Bounty> all() {
      return ALL;
   }

   public static BountyCatalog.Bounty byId(int var0) {
      return BY_ID.get(var0);
   }

   public static List<BountyCatalog.Bounty> of(BountyCatalog.Difficulty var0) {
      ArrayList var1 = new ArrayList();

      for (BountyCatalog.Bounty var3 : ALL) {
         if (var3.difficulty() == var0) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public static String goalText(BountyCatalog.Bounty var0) {
      if (var0 == null) {
         return "";
      } else if (ADVANCEMENT.equals(var0.trigger())) {
         return "完成成就「" + nameOf(var0.match()) + "」";
      } else if (BOSS_KILL.equals(var0.trigger())) {
         return "击杀 " + var0.amount() + " 只" + nameOf(var0.match());
      } else {
         BountyCatalog.Trigger var1 = triggerOf(var0.trigger());
         if (var1 == null) {
            return var0.desc();
         } else {
            String var2 = nameOf(var0.match());
            StringBuilder var3 = new StringBuilder(var1.goalVerb).append(' ').append(var0.amount()).append(' ').append(var1.unit);
            if (!var2.isEmpty()) {
               var3.append(' ').append(var2);
            }

            return var3.toString();
         }
      }
   }

   public static boolean isBoss(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         for (String[] var3 : BOSSES) {
            if (var3[0].equals(var0)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static List<BountyCatalog.Bounty> buildAll() {
      ArrayList var0 = new ArrayList(600);
      BountyCatalog.Trigger[] var1 = BountyCatalog.Trigger.values();
      int[] var2 = new int[var1.length];
      int var3 = 1;

      for (BountyCatalog.Difficulty var7 : BountyCatalog.Difficulty.values()) {
         if (var7 == BountyCatalog.Difficulty.EXTREME) {
            for (String[] var18 : EXTREME_TASKS) {
               var0.add(
                  new BountyCatalog.Bounty(
                     var3++,
                     var7,
                     var18[0],
                     var18[1],
                     Long.parseLong(var18[2]),
                     Long.parseLong(var18[5]) * 100L,
                     var18[3],
                     var18[4]
                  )
               );
            }
            continue;
         }

         int[] var8 = spread(COUNT_BY_DIFFICULTY[var7.ordinal()], var1.length);

         for (int var9 = 0; var9 < var1.length; var9++) {
            BountyCatalog.Trigger var10 = var1[var9];
            String[][] var11 = tableFor(var10);

            for (int var12 = 0; var12 < var8[var9]; var12++) {
               String var13 = "";
               String var14 = "";
               if (var11 != null) {
                  String[] var15 = var11[var2[var9]++ % var11.length];
                  var13 = var15[0];
                  var14 = var15[1];
               }

               long var25 = var10.min[var7.ordinal()];
               long var17 = var10.max[var7.ordinal()];
               long var19 = spreadAmount(var3, var25, var17, var10.step);
               long var21 = spreadReward(var3, var7);
               String var23 = title(var3, var10, var14);
               String var24 = desc(var10, var14, var19);
               var0.add(new BountyCatalog.Bounty(var3, var7, var10.id, var13, var19, var21, var23, var24));
               var3++;
            }
         }
      }

      return List.copyOf(var0);
   }

   private static Map<Integer, BountyCatalog.Bounty> buildIndex() {
      HashMap var0 = new HashMap();

      for (BountyCatalog.Bounty var2 : ALL) {
         var0.put(var2.id(), var2);
      }

      return var0;
   }

   private static Map<String, String> buildNames() {
      HashMap var0 = new HashMap();

      for (String[][] var4 : new String[][][]{BLOCKS, MOBS, CRAFT_ITEMS, SMELT_ITEMS, SUBMIT_ITEMS, BOSSES, ADVANCEMENTS}) {
         for (String[] var8 : var4) {
            var0.put(var8[0], var8[1]);
         }
      }

      return var0;
   }

   private static String[][] tableFor(BountyCatalog.Trigger var0) {
      return switch (var0) {
         case BLOCK_BREAK, BLOCK_PLACE -> BLOCKS;
         case KILL -> MOBS;
         case CRAFT -> CRAFT_ITEMS;
         case SMELT -> SMELT_ITEMS;
         default -> null;
         case SUBMIT_ITEM -> SUBMIT_ITEMS;
      };
   }

   private static BountyCatalog.Trigger triggerOf(String var0) {
      for (BountyCatalog.Trigger var4 : BountyCatalog.Trigger.values()) {
         if (var4.id.equals(var0)) {
            return var4;
         }
      }

      return null;
   }

   private static String nameOf(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         String var1 = NAMES.get(var0);
         return var1 == null ? var0 : var1;
      } else {
         return "";
      }
   }

   private static String title(int var0, BountyCatalog.Trigger var1, String var2) {
      if (!var2.isEmpty()) {
         return var1.verbs[var0 % var1.verbs.length] + var2;
      } else {
         String[] var3 = new String[]{"委托", "任务", "订单"};
         return var1.titleNoun() + var3[var0 % var3.length];
      }
   }

   private static String desc(BountyCatalog.Trigger var0, String var1, long var2) {
      return switch (var0) {
         case BLOCK_BREAK -> "把 " + var2 + " 个" + var1 + "挖掉运回仓库。";
         case BLOCK_PLACE -> "在据点里放下 " + var2 + " 个" + var1 + "。";
         case KILL -> "清理 " + var2 + " 只" + var1 + "，让周边安全下来。";
         case CRAFT -> "在合成台做出 " + var2 + " 个" + var1 + "。";
         case SMELT -> "从熔炉取出 " + var2 + " 个" + var1 + "。";
         case CONSUME -> "进食或饮用 " + var2 + " 次，保持体力。";
         case FISH -> "在任意水域钓到 " + var2 + " 次鱼。";
         case ENCHANT -> "在附魔台完成 " + var2 + " 次附魔。";
         case BREED -> "让动物成功繁殖 " + var2 + " 次。";
         case TAME -> "驯服 " + var2 + " 只动物。";
         case TRADE -> "与村民完成 " + var2 + " 次交易。";
         case TRAVEL -> "累计移动 " + var2 + " 格（传送不计）。";
         case DAMAGE_DEALT -> "对生物累计造成 " + var2 + " 点伤害。";
         case DAMAGE_TAKEN -> "累计承受 " + var2 + " 点伤害。";
         case XP_GAIN -> "累计获得 " + var2 + " 点经验。";
         case PICKUP -> "累计拾取 " + var2 + " 个物品。";
         case SLEEP -> "成功入睡 " + var2 + " 次。";
         case PLAY_TIME -> "累计在线 " + var2 + " 分钟。";
         case SUBMIT_ITEM -> "把 " + var2 + " 个" + var1 + "交到悬赏处。";
      };
   }

   private static long spreadAmount(int var0, long var1, long var3, long var5) {
      long var7 = var3 - var1;
      long var9 = mix((long)var0 * 2654435761L + 17L) % 1001L;
      long var11 = var1 + var7 * var9 / 1000L;
      var11 = var1 + Math.round((double)(var11 - var1) / (double)var5) * var5;
      return Math.max(var1, Math.min(var3, var11));
   }

   private static long spreadReward(int var0, BountyCatalog.Difficulty var1) {
      int var2 = var1.coinsMax() - var1.coinsMin();
      long var3 = mix((long)var0 * 40503L + 97L) % 1001L;
      long var5 = (long)var1.coinsMin() + (long)var2 * var3 / 1000L;
      return var5 * 100L;
   }

   private static int[] spread(int var0, int var1) {
      int[] var2 = new int[var1];
      int var3 = var0 / var1;
      int var4 = var0 % var1;

      for (int var5 = 0; var5 < var1; var5++) {
         var2[var5] = var3 + (var5 < var4 ? 1 : 0);
      }

      return var2;
   }

   private static long mix(long var0) {
      long var2 = var0 * -7046029254386353131L;
      var2 ^= var2 >>> 30;
      var2 *= -4658895280553007687L;
      var2 ^= var2 >>> 27;
      var2 *= -7723592293110705685L;
      var2 ^= var2 >>> 31;
      return var2 & Long.MAX_VALUE;
   }

   public static record Bounty(
      int id, BountyCatalog.Difficulty difficulty, String trigger, String match, long amount, long rewardCents, String title, String desc
   ) {
      public String goalText() {
         return BountyCatalog.goalText(this);
      }
   }

   public static enum Difficulty {
      EASY("简单", 20, 40, 1L),
      NORMAL("普通", 45, 90, 2L),
      HARD("困难", 100, 200, 4L),
      EXTREME("极限", 700, 1500, 8L);

      private final String label;
      private final int coinsMin;
      private final int coinsMax;
      private final long scale;

      private Difficulty(String nullxx, int nullxxx, int nullxxxx, long nullxxxxx) {
         this.label = nullxx;
         this.coinsMin = nullxxx;
         this.coinsMax = nullxxxx;
         this.scale = nullxxxxx;
      }

      public String label() {
         return this.label;
      }

      public int coinsMin() {
         return this.coinsMin;
      }

      public int coinsMax() {
         return this.coinsMax;
      }

      public long scale() {
         return this.scale;
      }
   }

   private static enum Trigger {
      BLOCK_BREAK("block_break", new long[]{32L, 64L, 128L, 512L}, new long[]{128L, 256L, 512L, 2048L}, 8L, "个", "破坏", "清运", "开采", "收集"),
      BLOCK_PLACE("block_place", new long[]{32L, 64L, 128L, 512L}, new long[]{128L, 256L, 512L, 2048L}, 8L, "个", "放置", "搭建", "铺设", "置放"),
      KILL("kill", new long[]{8L, 16L, 32L, 128L}, new long[]{32L, 64L, 128L, 512L}, 1L, "只", "击杀", "猎杀", "讨伐", "清剿"),
      CRAFT("craft", new long[]{8L, 16L, 32L, 128L}, new long[]{16L, 32L, 64L, 256L}, 1L, "个", "合成", "制作", "打造", "备货"),
      SMELT("smelt", new long[]{8L, 16L, 32L, 128L}, new long[]{16L, 32L, 64L, 256L}, 1L, "个", "烧炼", "冶炼", "熔炼", "加工"),
      CONSUME("consume", new long[]{8L, 16L, 32L, 128L}, new long[]{16L, 32L, 64L, 256L}, 1L, "次", "进食或饮用", "补给", "品尝", "进食"),
      FISH("fish", new long[]{5L, 10L, 20L, 80L}, new long[]{8L, 16L, 32L, 128L}, 1L, "次鱼", "钓到", "垂钓", "打渔", "抛竿"),
      ENCHANT("enchant", new long[]{2L, 4L, 8L, 32L}, new long[]{4L, 8L, 16L, 64L}, 1L, "次", "附魔", "附魔", "铭刻", "强化"),
      BREED("breed", new long[]{2L, 4L, 8L, 32L}, new long[]{4L, 8L, 16L, 64L}, 1L, "次", "繁殖", "繁育", "配种", "育种"),
      TAME("tame", new long[]{1L, 2L, 4L, 16L}, new long[]{2L, 4L, 8L, 32L}, 1L, "只", "驯服", "驯服", "收服", "降服"),
      TRADE("trade", new long[]{2L, 4L, 8L, 32L}, new long[]{4L, 8L, 16L, 64L}, 1L, "次村民交易", "完成", "议价", "跑商", "交易"),
      TRAVEL("travel", new long[]{500L, 1000L, 2000L, 10000L}, new long[]{2500L, 5000L, 10000L, 50000L}, 100L, "格", "移动", "远行", "跋涉", "探索"),
      DAMAGE_DEALT("damage_dealt", new long[]{50L, 200L, 500L, 4000L}, new long[]{200L, 500L, 1500L, 15000L}, 10L, "点伤害", "累计造成", "出手", "压制", "输出", "重击"),
      DAMAGE_TAKEN("damage_taken", new long[]{50L, 200L, 500L, 4000L}, new long[]{200L, 500L, 1500L, 15000L}, 10L, "点伤害", "累计承受", "扛伤", "硬顶", "耐磨", "死撑"),
      XP_GAIN("xp_gain", new long[]{30L, 100L, 300L, 2000L}, new long[]{100L, 300L, 800L, 6000L}, 10L, "点经验", "累计获得", "积攒", "历练", "修行", "深造"),
      PICKUP("pickup", new long[]{64L, 256L, 512L, 2048L}, new long[]{256L, 512L, 1024L, 4096L}, 8L, "个物品", "拾取", "捡拾", "收拢", "归仓", "入库"),
      SLEEP("sleep", new long[]{1L, 2L, 4L, 12L}, new long[]{2L, 4L, 6L, 20L}, 1L, "次", "成功入睡", "安眠", "好梦", "歇息", "躺平"),
      PLAY_TIME("play_time", new long[]{10L, 30L, 60L, 240L}, new long[]{30L, 60L, 120L, 480L}, 5L, "分钟", "累计在线", "驻留", "值班", "守夜", "留守"),
      SUBMIT_ITEM("submit_item", new long[]{16L, 32L, 64L, 128L}, new long[]{64L, 128L, 256L, 512L}, 8L, "个", "提交", "缴纳", "上缴", "备货", "交付");

      private final String id;
      private final long[] min;
      private final long[] max;
      private final long step;
      private final String unit;
      private final String goalVerb;
      private final String[] verbs;

      private Trigger(String nullxx, long[] nullxxx, long[] nullxxxx, long nullxxxxx, String nullxxxxxx, String nullxxxxxxx, String... nullxxxxxxxx) {
         this.id = nullxx;
         this.min = nullxxx;
         this.max = nullxxxx;
         this.step = nullxxxxx;
         this.unit = nullxxxxxx;
         this.goalVerb = nullxxxxxxx;
         this.verbs = nullxxxxxxxx;
      }

      public String titleNoun() {
         return switch (this) {
            case CONSUME -> "进食";
            case FISH -> "钓鱼";
            case ENCHANT -> "附魔";
            case BREED -> "繁殖";
            case TAME -> "驯服";
            case TRADE -> "交易";
            case TRAVEL -> "远行";
            case DAMAGE_DEALT -> "试刀";
            case DAMAGE_TAKEN -> "耐磨";
            case XP_GAIN -> "历练";
            case PICKUP -> "拾荒";
            case SLEEP -> "安眠";
            case PLAY_TIME -> "驻留";
            case SUBMIT_ITEM -> "缴纳";
            default -> "";
         };
      }
   }
}
