package com.enhancedhordes.tweaks.config;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.util.List;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class EnhancedHordesTweaksConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final boolean GAME_STAGES_INSTALLED = ModList.get().isLoaded("gamestages");

    // -----------------------------------------------------------------------
    // General
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.EnumValue<DifficultyPreset> DIFFICULTY_PRESET = BUILDER
            .comment(
                "Difficulty preset applied on top of your config values.",
                "NORMAL leaves values untouched. EASY softens.",
                "A preset scales feature ranges, follow distances, over-time increases, mob block damage,",
                "and shifts day activation thresholds. It does not enable or disable features for you.",
                "Allowed values: EASY, NORMAL, HARD, NIGHTMARE"
            )
            .defineEnum("general.difficultyPreset", DifficultyPreset.NORMAL);

    private static final ModConfigSpec.BooleanValue GLOBAL_NIGHT_ONLY = BUILDER
            .comment(
                "Suppress Enhanced Hordes features during daytime."
            )
            .define("general.globalNightOnly", false);

    private static final ModConfigSpec.BooleanValue LIGHT_LEVEL_GATING = BUILDER
            .comment(
                "Only let horde mob features work where the light level is low enough."
            )
            .define("general.lightLevelGating", false);

    private static final ModConfigSpec.IntValue MAX_ACTIVE_LIGHT_LEVEL = BUILDER
            .comment(
                "Highest light level at which features stay active when lightLevelGating is true.",
                "Has no effect if lightLevelGating is false.",
                "Range: 0 ~ 15"
            )
            .defineInRange("general.maxActiveLightLevel", 7, 0, 15);

    // -----------------------------------------------------------------------
    // Grace Radius
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_GRACE_RADIUS = BUILDER
            .comment(
                "Suppress horde mob features near a protected point (world spawn or player beds).",
                "Affects Horde Determination, Heightened Sense, and Collective Understanding (checked at the mob).",
                "Also stops Horde Mentality from damaging any block inside a protected radius."
            )
            .define("graceRadius.enableGraceRadius", false);

    private static final ModConfigSpec.IntValue GRACE_RADIUS = BUILDER
            .comment(
                "Block radius around a protected point within which features are suppressed.",
                "Has no effect if enableGraceRadius is false.",
                "Range: 1 ~ 256"
            )
            .defineInRange("graceRadius.radius", 24, 1, 256);

    private static final ModConfigSpec.BooleanValue GRACE_USE_WORLD_SPAWN = BUILDER
            .comment(
                "Protect the area around the world spawn point.",
                "Has no effect if enableGraceRadius is false."
            )
            .define("graceRadius.useWorldSpawn", true);

    private static final ModConfigSpec.BooleanValue GRACE_USE_PLAYER_SPAWN = BUILDER
            .comment(
                "Protect the area around each player's bed or respawn point.",
                "Has no effect if enableGraceRadius is false."
            )
            .define("graceRadius.usePlayerSpawn", true);

    // -----------------------------------------------------------------------
    // Feature Toggles
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_HORDE_STACKING = BUILDER
            .comment(
                "Enable horde stacking, mobs climbing on top of each other to scale walls."
            )
            .define("features.enableHordeStacking", true);

    private static final ModConfigSpec.BooleanValue HORDE_FIRE_SPREAD = BUILDER
            .comment(
                "Allow horde mobs that are on fire to spread fire to nearby horde mobs on contact."
            )
            .define("features.hordeFireSpread", true);

    private static final ModConfigSpec.BooleanValue HORDE_FIRE_SPEED_BOOST = BUILDER
            .comment(
                "Burning horde mobs gain a speed boost."
            )
            .define("features.hordeFireSpeedBoost", true);

    private static final ModConfigSpec.BooleanValue HORDE_BABY_FIRE_SPEED_BOOST = BUILDER
            .comment(
                "Allow baby horde mobs to gain a speed boost when on fire.",
                "Has no effect if hordeFireSpeedBoost is false."
            )
            .define("features.hordeBabyFireSpeedBoost", false);

    private static final ModConfigSpec.IntValue HORDE_FIRE_SPEED_AMPLIFIER = BUILDER
            .comment(
                "Speed effect level for burning horde mobs (0=Speed I, 1=Speed II, 2=Speed III).",
                "Has no effect if hordeFireSpeedBoost is false.",
                "Range: 0 ~ 9"
            )
            .defineInRange("features.hordeFireSpeedAmplifier", 2, 0, 9);

    private static final ModConfigSpec.IntValue HORDE_FIRE_DAMAGE_RESISTANCE = BUILDER
            .comment(
                "Percentage of fire damage horde mobs resist (0=none, 100=immune).",
                "Range: 0 ~ 100"
            )
            .defineInRange("features.hordeFireDamageResistance", 0, 0, 100);

    private static final ModConfigSpec.BooleanValue HORDE_FALL_DAMAGE_IMMUNITY = BUILDER
            .comment(
                "Horde mobs take no fall damage."
            )
            .define("features.hordeFallDamageImmunity", false);

    private static final ModConfigSpec.BooleanValue HORDE_BABY_THROW = BUILDER
            .comment(
                "Baby horde mobs get launched during stacking behavior."
            )
            .define("features.hordeBabyThrow", true);

    private static final ModConfigSpec.BooleanValue HORDE_BABY_BLOCK_BREAKING = BUILDER
            .comment(
                "Allow baby horde mobs to break blocks from the hordeBreakableBlocks list while pathfinding.",
                "Has no effect if enableHordeBlockBreaking is false."
            )
            .define("features.hordeBabyBlockBreaking", false);

    private static final ModConfigSpec.BooleanValue ENABLE_HORDE_MULTIPLYING = BUILDER
            .comment(
                "Enable horde multiplying, mobs digging up additional reinforcements from the ground.",
                "This overrides the graveRobbers list."
            )
            .define("features.enableHordeMultiplying", true);

    private static final ModConfigSpec.BooleanValue ENABLE_INTELLIGENT_TEAMS = BUILDER
            .comment(
                "Enable intelligent team behavior.",
                "This overrides the intelligentTeamMobs list."
            )
            .define("features.enableIntelligentTeams", true);

    private static final ModConfigSpec.BooleanValue ENABLE_LEAPING_MOBS = BUILDER
            .comment(
                "Enable leaping behavior.",
                "This overrides the leapingMobs list."
            )
            .define("features.enableLeapingMobs", true);

    private static final ModConfigSpec.BooleanValue ENABLE_INTELLIGENT_PIGLINS = BUILDER
            .comment(
                "Enable intelligent behavior for piglins.",
                "This overrides the intelligentPiglins list."
            )
            .define("features.enableIntelligentPiglins", true);

    private static final ModConfigSpec.BooleanValue ENABLE_HIDDEN_ZOMBIES = BUILDER
            .comment(
                "Enable horde mobs burrowing and hiding under blocks.",
                "This overrides the hiddenZombieBlocks list.",
                "Note: Disabling this also prevents horde multiplying from triggering, even if enableHordeMultiplying is true."
            )
            .define("features.enableHiddenZombies", true);

    private static final ModConfigSpec.BooleanValue ENABLE_HORDE_BLOCK_BREAKING = BUILDER
            .comment(
                "Enable horde mobs breaking blocks while pathfinding. This overrides the hordeBreakableBlocks list. "
            )
            .define("features.enableHordeBlockBreaking", true);

    private static final ModConfigSpec.BooleanValue ENABLE_HORDE_SWIMMING = BUILDER
            .comment(
                "Allow horde mobs to float and swim across water instead of sinking."
            )
            .define("features.enableHordeSwimming", false);

    private static final ModConfigSpec.IntValue HORDE_MOB_CAP = BUILDER
            .comment(
                "Maximum number of horde mobs allowed in a single dimension.",
                "0 = vanilla mob cap.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("features.hordeMobCap", 0, 0, 10000);

    private static final ModConfigSpec.BooleanValue DISABLE_GAMERULE_COMMANDS = BUILDER
            .comment(
                "Prevent Enhanced Hordes game rules via /gamerule.",
                "When true, the config file becomes the only way to control these rules."
            )
            .define("features.disableGameruleCommands", false);

    private static final ModConfigSpec.IntValue FEATURES_DAYS_BEFORE_ACTIVATION = BUILDER
            .comment(
                "In-game day threshold before any Enhanced Hordes feature in this category becomes active.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("features.daysBeforeFeaturesActivate", 0, 0, 10000);

    // -----------------------------------------------------------------------
    // Intelligent Mob Behaviors
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_FRIENDLY_FIRE = BUILDER
            .comment(
                "Allow intelligent mobs to attack their own kind."
            )
            .define("intelligentMobBehaviors.enableFriendlyFire", false);

    private static final ModConfigSpec.BooleanValue ALLOW_FRIENDLY_FIRE_TO_HORDE_MOBS = BUILDER
            .comment(
                "Allow intelligent mobs to deal damage to horde mobs."
            )
            .define("intelligentMobBehaviors.allowFriendlyFireToHordeMobs", true);

    private static final ModConfigSpec.BooleanValue ENABLE_ZOMBIFIED_PIGLIN_CROSSBOW = BUILDER
            .comment(
                "Allow zombified piglins to fire inaccurate arrows at a distance.",
                "Has no effect if enableIntelligentPiglins is false."
            )
            .define("intelligentMobBehaviors.enableZombifiedPiglinCrossbow", true);

    private static final ModConfigSpec.IntValue INTELLIGENT_MOB_MIN_DASH_DISTANCE = BUILDER
            .comment(
                "Minimum distance (in blocks) an intelligent mob must be from its target before it is allowed to dash away.",
                "Dashes are suppressed while the mob is closer than this distance.",
                "Range: 1 ~ 32"
            )
            .defineInRange("intelligentMobBehaviors.intelligentMobMinDashDistance", 4, 1, 32);

    // -----------------------------------------------------------------------
    // Other Mobs
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_IRON_GOLEM_MULTI_HIT = BUILDER
            .comment(
                "Enable Iron Golems hitting multiple enemies at once with one swing."
            )
            .define("otherMobs.enableIronGolemMultiHit", true);

    private static final ModConfigSpec.BooleanValue ENABLE_IRON_GOLEM_FIRE_IMMUNITY = BUILDER
            .comment(
                "Enable Iron Golem fire immunity when not standing directly in a flame or lava."
            )
            .define("otherMobs.enableIronGolemFireImmunity", true);

    private static final ModConfigSpec.BooleanValue ENABLE_IRON_GOLEM_EFFECT_IMMUNITY = BUILDER
            .comment(
                "Enable Iron Golem immunity to Wither, Poison, and Levitation effects."
            )
            .define("otherMobs.enableIronGolemEffectImmunity", true);

    private static final ModConfigSpec.BooleanValue ENABLE_IRON_GOLEM_ARROW_RESISTANCE = BUILDER
            .comment(
                "Enable Iron Golem arrow resistance."
            )
            .define("otherMobs.enableIronGolemArrowResistance", true);

    private static final ModConfigSpec.IntValue IRON_GOLEM_ARROW_RESISTANCE_PERCENT = BUILDER
            .comment(
                "Percentage chance for an Iron Golem to block incoming arrow damage.",
                "Has no effect if enableIronGolemArrowResistance is false.",
                "Range: 0 ~ 100"
            )
            .defineInRange("otherMobs.ironGolemArrowResistancePercent", 60, 0, 100);

    private static final ModConfigSpec.BooleanValue ENABLE_IRON_GOLEM_HURRY = BUILDER
            .comment(
                "Enable Iron Golems receiving a speed boost when a Villager is being targeted.",
                "Note: If disabled, this will also prevent speed effects from potions and beacons on Iron Golems."
            )
            .define("otherMobs.enableIronGolemHurry", true);

    private static final ModConfigSpec.BooleanValue ENABLE_IRON_GOLEM_REGEN = BUILDER
            .comment(
                "Enable Iron Golem regeneration when not in combat.",
                "This overrides the ironGolemRegenPower game rule default."
            )
            .define("otherMobs.enableIronGolemRegen", true);

    private static final ModConfigSpec.IntValue IRON_GOLEM_REGEN_POWER = BUILDER
            .comment(
                "Default value for the 'ironGolemRegenPower' game rule.",
                "Sets the Regeneration effect level granted to Iron Golems when not in combat.",
                "0 disables regen. Default = 4."
            )
            .defineInRange("otherMobs.ironGolemRegenPower", 4, 0, 255);

    private static final ModConfigSpec.IntValue IRON_GOLEM_REGEN_COOLDOWN_SECONDS = BUILDER
            .comment(
                "Seconds after taking damage before an Iron Golem can begin regenerating.",
                "Has no effect if enableIronGolemRegen is false.",
                "Range: 0 ~ 300"
            )
            .defineInRange("otherMobs.ironGolemRegenCooldownSeconds", 10, 0, 300);

    private static final ModConfigSpec.BooleanValue ENABLE_IRON_GOLEM_VILLAGER_DEFENDER = BUILDER
            .comment(
                "Iron Golems will aggro on any mob that damages a nearby Villager."
            )
            .define("otherMobs.enableIronGolemVillagerDefender", false);

    private static final ModConfigSpec.BooleanValue ENABLE_WITHER_SKELETON_BOW_TACTICS = BUILDER
            .comment(
                "Enable Wither Skeleton bow tactics and dash behavior.",
                "Has no effect if enableIntelligentTeams is false."
            )
            .define("otherMobs.enableWitherSkeletonBowTactics", true);

    private static final ModConfigSpec.BooleanValue ENABLE_CREEPER_WALL_EXPLOSION = BUILDER
            .comment(
                "Creepers will ignite if they cannot path-reach their target player, are within",
                "creeperWallExplosionDistance blocks of the player, and are touching a wall in front of them."
            )
            .define("otherMobs.enableCreeperWallExplosion", false);

    private static final ModConfigSpec.IntValue CREEPER_WALL_EXPLOSION_DISTANCE = BUILDER
            .comment(
                "Maximum distance (in blocks) between a creeper and its target player for the creeper",
                "to be able to blow up a wall.",
                "Has no effect if enableCreeperWallExplosion is false.",
                "Range: 1 ~ 32"
            )
            .defineInRange("otherMobs.creeperWallExplosionDistance", 5, 1, 32);

    private static final ModConfigSpec.IntValue CREEPER_WALL_EXPLOSION_DAYS_BEFORE_ACTIVATION = BUILDER
            .comment(
                "In-game day threshold before Creeper Wall Explosion begins.",
                "Has no effect if enableCreeperWallExplosion is false.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("otherMobs.creeperWallExplosionDaysBeforeActivation", 0, 0, 10000);

    // -----------------------------------------------------------------------
    // Entity Tags
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HORDE_MOBS = BUILDER
            .comment(
                "Entity types that count as 'horde' mobs. Accepts entity IDs and entity tags.",
                "These mobs will stack on each other and multiply (dig up more mobs)."
            )
            .defineListAllowEmpty("entityTags.hordeMobs",
                    List.of(
                            "minecraft:zombie",
                            "minecraft:zombie_villager",
                            "minecraft:zombified_piglin",
                            "minecraft:husk",
                            "minecraft:drowned",
                            "minecraft:slime"
                    ),
                    obj -> obj instanceof String
            );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> INTELLIGENT_TEAM_MOBS = BUILDER
            .comment(
                "Entity types that form intelligent teams.",
                "These mobs can coordinate attacks and dash away.",
                "Accepts entity IDs and entity tags."
            )
            .defineListAllowEmpty("entityTags.intelligentTeamMobs",
                    List.of(
                            "minecraft:skeleton",
                            "minecraft:wither_skeleton",
                            "minecraft:stray"
                    ),
                    obj -> obj instanceof String
            );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> LEAPING_MOBS = BUILDER
            .comment(
                "Entity types that can leap at targets.",
                "Accepts entity IDs and entity tags."
            )
            .defineListAllowEmpty("entityTags.leapingMobs",
                    List.of(
                            "minecraft:spider",
                            "minecraft:cave_spider",
                            "minecraft:skeleton",
                            "minecraft:stray",
                            "minecraft:wither_skeleton"
                    ),
                    obj -> obj instanceof String
            );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> GRAVE_ROBBERS = BUILDER
            .comment(
                "Entity types that can dig up (spawn) other horde mobs from the ground.",
                "Accepts entity IDs and entity tags."
            )
            .defineListAllowEmpty("entityTags.graveRobbers",
                    List.of(
                            "minecraft:zombie",
                            "minecraft:husk",
                            "minecraft:drowned"
                    ),
                    obj -> obj instanceof String
            );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> INTELLIGENT_PIGLINS = BUILDER
            .comment(
                "Piglin entity types that receive intelligent behavior.",
                "Accepts entity IDs and entity tags."
            )
            .defineListAllowEmpty("entityTags.intelligentPiglins",
                    List.of(
                            "minecraft:piglin",
                            "minecraft:piglin_brute",
                            "minecraft:zombified_piglin"
                    ),
                    obj -> obj instanceof String
            );

    // -----------------------------------------------------------------------
    // Block Tags
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HIDDEN_ZOMBIE_BLOCKS = BUILDER
            .comment(
                "Blocks that horde mobs can hide (burrow) under.",
                "Horde mobs will sink into these blocks and ambush players.",
                "Accepts block IDs and block tags."
            )
            .defineListAllowEmpty("blockTags.hiddenZombieBlocks",
                    List.of(
                            "#minecraft:dirt",
                            "#c:sands",
                            "#c:gravels",
                            "minecraft:dirt_path",
                            "minecraft:mossy_cobblestone",
                            "minecraft:packed_mud"
                    ),
                    obj -> obj instanceof String
            );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HORDE_BREAKABLE_BLOCKS = BUILDER
            .comment(
                "Blocks that horde mobs can break while pathfinding.",
                "Accepts block IDs and block tags."
            )
            .defineListAllowEmpty("blockTags.hordeBreakableBlocks",
                    List.of(
                            "#minecraft:leaves",
                            "#minecraft:crops",
                            "minecraft:moss_carpet",
                            "minecraft:turtle_egg",
                            "minecraft:bamboo_sapling",
                            "minecraft:bamboo",
                            "minecraft:ice",
                            "minecraft:frosted_ice",
                            "minecraft:pointed_dripstone"
                    ),
                    obj -> obj instanceof String
            );

    // -----------------------------------------------------------------------
    // Block Regeneration
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_BLOCK_REGENERATION = BUILDER
            .comment(
                "Enable regeneration of blocks broken by horde mobs."
            )
            .define("blockRegeneration.enableBlockRegeneration", false);

    private static final ModConfigSpec.IntValue REGENERATION_DELAY_SECONDS = BUILDER
            .comment(
                "Time in seconds before a mob-broken block regenerates.",
                "Range: 1 ~ 3600"
            )
            .defineInRange("blockRegeneration.regenerationDelaySeconds", 60, 1, 3600);

    private static final ModConfigSpec.BooleanValue REQUIRE_NEARBY_PLAYERS = BUILDER
            .comment(
                "Only regenerate blocks if a player is within the check radius."
            )
            .define("blockRegeneration.requireNearbyPlayers", false);

    private static final ModConfigSpec.IntValue NEARBY_PLAYER_RADIUS = BUILDER
            .comment(
                "Block radius to check for nearby players before regenerating.",
                "Range: 1 ~ 64"
            )
            .defineInRange("blockRegeneration.nearbyPlayerRadius", 16, 1, 64);

    private static final ModConfigSpec.BooleanValue CHECK_ENTITY_COLLISION = BUILDER
            .comment(
                "Check if an entity is occupying the block space before regenerating.",
                "Prevents mobs getting trapped and players suffocating inside regenerated blocks.",
                "The block will keep trying to regenerate until the space is clear."
            )
            .define("blockRegeneration.checkEntityCollision", true);

    private static final ModConfigSpec.BooleanValue REQUIRE_MOBS_CLEARED_BEFORE_REGEN = BUILDER
            .comment(
                "Wait for all horde mobs to leave the area before starting the regeneration timer."
            )
            .define("blockRegeneration.requireMobsClearedBeforeRegen", true);

    private static final ModConfigSpec.IntValue MOB_CLEARED_RADIUS = BUILDER
            .comment(
                "Block radius to check for nearby horde mobs before starting the regeneration timer.",
                "Has no effect if requireMobsClearedBeforeRegen is false.",
                "Range: 1 ~ 512"
            )
            .defineInRange("blockRegeneration.mobClearedRadius", 15, 1, 512);

    private static final ModConfigSpec.BooleanValue CANCEL_REGEN_ON_MOB_RETURN = BUILDER
            .comment(
                "Cancel a block's regeneration and wait again if a horde mob re-enters the area."
            )
            .define("blockRegeneration.cancelRegenOnMobReturn", true);

    private static final ModConfigSpec.BooleanValue RESET_DELAY_ON_MOB_RETURN = BUILDER
            .comment(
                "When a mob returns and cancels regeneration:",
                "true  = fully restart the regeneration delay from the beginning.",
                "false = pause the timer and resume from where it left off when the mob leaves."
            )
            .define("blockRegeneration.resetDelayOnMobReturn", true);

    private static final ModConfigSpec.BooleanValue SHOW_PRE_REGEN_WARNING_PARTICLES = BUILDER
            .comment(
                "Show particles at a block's position 5 seconds before it regenerates,",
                "gives a heads-up that a block is about to be restored.",
                "In staggered mode, all blocks queued to regenerate will glow until placed."
            )
            .define("blockRegeneration.showPreRegenWarningParticles", true);

    private static final ModConfigSpec.BooleanValue STAGGERED_REGEN = BUILDER
            .comment(
                "When enabled, mob-broken blocks regenerate one at a time in a random order"
            )
            .define("blockRegeneration.staggeredRegen", true);

    private static final ModConfigSpec.IntValue STAGGERED_REGEN_INTERVAL_TICKS = BUILDER
            .comment(
                "Ticks between each block placement in staggered regeneration mode.",
                "Has no effect if staggeredRegen is false.",
                "Range: 1 ~ 200"
            )
            .defineInRange("blockRegeneration.staggeredRegenIntervalTicks", 7, 1, 200);

    private static final ModConfigSpec.BooleanValue SHOW_REGENERATION_PARTICLES = BUILDER
            .comment(
                "Show particles when a block regenerates."
            )
            .define("blockRegeneration.showRegenerationParticles", true);

    private static final ModConfigSpec.BooleanValue PLAY_REGENERATION_SOUND = BUILDER
            .comment(
                "Play the block's placement sound when it regenerates."
            )
            .define("blockRegeneration.playRegenerationSound", true);

    private static final ModConfigSpec.BooleanValue REGEN_DAYTIME_ONLY = BUILDER
            .comment(
                "Only regenerate blocks during the daytime."
            )
            .define("blockRegeneration.regenDaytimeOnly", false);

    private static final ModConfigSpec.BooleanValue REGEN_SCALE_DELAY_BY_HARDNESS = BUILDER
            .comment(
                "Scale the regeneration delay based on block hardness.",
                "Harder blocks will take longer to regenerate."
            )
            .define("blockRegeneration.regenScaleDelayByHardness", false);

    private static final ModConfigSpec.BooleanValue CANCEL_REGEN_ON_PLAYER_PLACE = BUILDER
            .comment(
                "Cancel pending block regeneration when a player places a block nearby."
            )
            .define("blockRegeneration.cancelRegenOnPlayerPlace", false);

    private static final ModConfigSpec.IntValue PLAYER_PLACE_CANCEL_RADIUS = BUILDER
            .comment(
                "Block radius around a player-placed block to cancel pending regeneration.",
                "Has no effect if cancelRegenOnPlayerPlace is false.",
                "Range: 1 ~ 16"
            )
            .defineInRange("blockRegeneration.playerPlaceCancelRadius", 4, 1, 16);

    // -----------------------------------------------------------------------
    // Horde Mentality
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_HORDE_MENTALITY = BUILDER
            .comment(
                "Enable horde mentality: grouped horde mobs can break harder blocks together."
            )
            .define("hordeMentality.enableHordeMentality", false);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_NIGHT_ONLY = BUILDER
            .comment(
                "Only allow horde mentality block breaking at night."
            )
            .define("hordeMentality.nightOnly", false);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_GROUP_RADIUS = BUILDER
            .comment(
                "Radius in blocks within which horde mobs count toward the same group.",
                "Range: 1 ~ 64"
            )
            .defineInRange("hordeMentality.groupRadius", 5, 1, 64);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_DAMAGE_RATE_PER_MOB = BUILDER
            .comment(
                "The amount of damage a horde mob does when hitting a block.",
                "Range: 1 ~ 100"
            )
            .defineInRange("hordeMentality.damageRatePerMob", 10, 1, 100);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_DAMAGE_INCREASE_PER_DAY = BUILDER
            .comment(
                "Increases horde mob block damage each day",
                "Range: 0 ~ 1000"
            )
            .defineInRange("hordeMentality.damageIncreasePerDay", 0, 0, 1000);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_DAMAGE_MULTIPLIER_PER_DAY = BUILDER
            .comment(
                "Multiplies horde mob block damage each day",
                "Range: 0 ~ 1000"
            )
            .defineInRange("hordeMentality.damageMultiplierPerDay", 0, 0, 1000);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_SCALE_WITH_DAMAGE = BUILDER
            .comment(
                "Scale a mob's block damage contribution by its attack damage attribute.",
                "damageRatePerMob acts as a multiplier on top of the attack damage value."
            )
            .define("hordeMentality.scaleWithDamage", false);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_TIER_DAMAGE_SCALING = BUILDER
            .comment(
                "Multiply block damage by the qualified tier number (Tier 2 group deals 2x damage)."
            )
            .define("hordeMentality.tierDamageScaling", false);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_PROGRESSIVE_SCALING = BUILDER
            .comment(
                "When the group exceeds the Tier 4 minimum, each additional mob adds bonus damage.",
                "Disabled by default."
            )
            .define("hordeMentality.progressiveScaling", false);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_PROGRESSIVE_SCALING_BONUS_PER_MOB = BUILDER
            .comment(
                "Bonus damage percent per mob above the Tier 4 threshold.",
                "Has no effect if progressiveScaling is false.",
                "Range: 1 ~ 100"
            )
            .defineInRange("hordeMentality.progressiveScalingBonusPerMob", 5, 1, 100);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_HARDNESS_SCALING = BUILDER
            .comment(
                "Scales block hardness into total damage points required to break it.",
                "For example, with a value of 100: a block with hardness 1.5 requires 150 damage to break.",
                "Range: 10 ~ 1000"
            )
            .defineInRange("hordeMentality.hardnessScaling", 100, 10, 1000);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_DAMAGE_LINGER_SECONDS = BUILDER
            .comment(
                "How long (in seconds) a block's visual crack stays after mobs stop hitting it.",
                "Accumulated damage also persists for this duration before resetting.",
                "Range: 1 ~ 3600"
            )
            .defineInRange("hordeMentality.damageLingerSeconds", 300, 1, 3600);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_INSTANT_BREAK = BUILDER
            .comment(
                "Break blocks instantly when a qualifying mob touches them, skipping progressive damage."
            )
            .define("hordeMentality.instantBreak", false);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_SWING_INTERVAL_TICKS = BUILDER
            .comment(
                "Ticks between each swing attempt by a horde mob.",
                "Range: 1 ~ 40"
            )
            .defineInRange("hordeMentality.swingIntervalTicks", 10, 1, 40);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_HIT_CHANCE_PERCENT = BUILDER
            .comment(
                "Percent chance that a mob's swing successfully deals damage.",
                "Range: 1 ~ 100"
            )
            .defineInRange("hordeMentality.hitChancePercent", 70, 1, 100);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_REQUIRE_PLAYER_TARGET = BUILDER
            .comment(
                "Require horde mobs to be actively pathfinding toward a player to deal block damage.",
                "WARNING: If disabled, large groups can dig massive tunnels on their own."
            )
            .define("hordeMentality.requirePlayerTarget", true);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_CHECK_PLAYER_PROXIMITY = BUILDER
            .comment(
                "Only allow horde mobs to damage blocks when a player is within playerProximityRadius."
            )
            .define("hordeMentality.checkPlayerProximity", true);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_PLAYER_PROXIMITY_RADIUS = BUILDER
            .comment(
                "Block radius that a player must be in for horde mobs to damage blocks.",
                "Has no effect if checkPlayerProximity is false.",
                "Range: 1 ~ 64"
            )
            .defineInRange("hordeMentality.playerProximityRadius", 10, 1, 64);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_REQUIRE_BLOCK_IN_DIRECTION = BUILDER
            .comment(
                "Horde mobs will only damage blocks that are roughly in the direction of their target player."
            )
            .define("hordeMentality.requireBlockInDirection", true);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_BABY_MOBS_CONTRIBUTE = BUILDER
            .comment(
                "Allow baby horde mobs to count toward group size."
            )
            .define("hordeMentality.babyMobsContribute", false);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_BABY_MOBS_CAN_BREAK = BUILDER
            .comment(
                "Allow baby horde mobs to damage blocks."
            )
            .define("hordeMentality.babyMobsCanBreak", false);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_DROP_BLOCK_ITEMS = BUILDER
            .comment(
                "Drop items when a block is broken by horde mentality.",
                "This option is disabled when Block Regeneration is enabled."
            )
            .define("hordeMentality.dropBlockItems", false);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_PROTECT_SUPPORTING_BLOCKS = BUILDER
            .comment(
                "Prevent horde mobs from breaking blocks that directly support another block.",
                "When enabled, blocks beneath objects like doors, beds, or any block that requires",
                "a solid surface to exist will be skipped."
            )
            .define("hordeMentality.protectSupportingBlocks", true);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_ALLOW_DIG_DOWN_TO_PLAYER = BUILDER
            .comment(
                "Allow horde mentality mobs to break the block beneath them when the player is underneath them"
            )
            .define("hordeMentality.allowDigDownToPlayer", true);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_DAYS_BEFORE_ACTIVATION = BUILDER
            .comment(
                "In-game day threshold before Horde Mentality block breaking begins.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("hordeMentality.daysBeforeActivation", 0, 0, 10000);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_DROP_TIER1_BLOCKS = BUILDER
            .comment("Allow Tier 1 blocks to drop items. Only applies when dropBlockItems is true.")
            .define("hordeMentality.dropTier1Blocks", true);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_DROP_TIER2_BLOCKS = BUILDER
            .comment("Allow Tier 2 blocks to drop items. Only applies when dropBlockItems is true.")
            .define("hordeMentality.dropTier2Blocks", true);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_DROP_TIER3_BLOCKS = BUILDER
            .comment("Allow Tier 3 blocks to drop items. Only applies when dropBlockItems is true.")
            .define("hordeMentality.dropTier3Blocks", true);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_DROP_TIER4_BLOCKS = BUILDER
            .comment("Allow Tier 4 blocks to drop items. Only applies when dropBlockItems is true.")
            .define("hordeMentality.dropTier4Blocks", true);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_SHOW_BREAK_PARTICLES = BUILDER
            .comment(
                "Spawn block break particles when a block is fully broken by horde mentality."
            )
            .define("hordeMentality.showBreakParticles", true);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_PLAY_BREAK_SOUND = BUILDER
            .comment(
                "Play the block's break sound when fully broken by horde mentality."
            )
            .define("hordeMentality.playBreakSound", true);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_PLAY_HIT_SOUND = BUILDER
            .comment(
                "Play the block's hit sound each time a mob successfully damages a block."
            )
            .define("hordeMentality.playHitSound", true);

    private static final ModConfigSpec.BooleanValue HORDE_MENTALITY_SHOW_HIT_PARTICLES = BUILDER
            .comment(
                "Spawn block chip particles each time a mob successfully damages a block."
            )
            .define("hordeMentality.showHitParticles", true);

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_TIER1_MIN_MOBS = BUILDER
            .comment(
                "Minimum group size to break Tier 1 blocks."
            )
            .defineInRange("hordeMentality.tier1MinMobs", 3, 1, 100);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HORDE_MENTALITY_TIER1_BLOCKS = BUILDER
            .comment(
                "Blocks breakable by a group of tier1MinMobs or more horde mobs.",
                "Accepts block IDs and block tags."
            )
            .defineListAllowEmpty("hordeMentality.tier1Blocks",
                    List.of(
                            "#minecraft:wooden_doors",
                            "#minecraft:wooden_trapdoors",
                            "#minecraft:wooden_fences",
                            "#minecraft:fence_gates",
                            "#minecraft:wool",
                            "#minecraft:wool_carpets",
                            "#minecraft:beds",
                            "#minecraft:flowers",
                            "#minecraft:flower_pots",
                            "#minecraft:wooden_pressure_plates",
                            "#minecraft:all_signs",
                            "#minecraft:candles",
                            "#c:glass_blocks",
                            "#c:glass_panes",
                            "#c:chests/wooden",
                            "#c:barrels/wooden",
                            "#minecraft:rails",
                            "#minecraft:banners",
                            "#minecraft:saplings",
                            "#minecraft:campfires",
                            "#minecraft:wooden_buttons",
                            "#minecraft:corals",
                            "#minecraft:coral_blocks",
                            "minecraft:bell",
                            "minecraft:daylight_detector",
                            "minecraft:lever",
                            "minecraft:pearlescent_froglight",
                            "minecraft:verdant_froglight",
                            "minecraft:ochre_froglight",
                            "minecraft:dried_kelp_block",
                            "minecraft:comparator",
                            "minecraft:repeater",
                            "minecraft:tripwire_hook",
                            "minecraft:tripwire",
                            "minecraft:tnt",
                            "minecraft:decorated_pot",
                            "minecraft:sponge",
                            "minecraft:wet_sponge",
                            "minecraft:slime_block",
                            "minecraft:honey_block",
                            "minecraft:redstone_torch",
                            "minecraft:redstone_lamp",
                            "minecraft:sea_lantern",
                            "minecraft:lantern",
                            "minecraft:soul_lantern",
                            "minecraft:small_amethyst_bud",
                            "minecraft:medium_amethyst_bud",
                            "minecraft:large_amethyst_bud",
                            "minecraft:amethyst_cluster",
                            "minecraft:chain",
                            "minecraft:short_grass",
                            "minecraft:tall_grass",
                            "minecraft:dead_bush",
                            "minecraft:cactus",
                            "minecraft:cocoa",
                            "minecraft:sugar_cane",
                            "minecraft:vine",
                            "minecraft:torch",
                            "minecraft:wall_torch",
                            "minecraft:soul_torch",
                            "minecraft:soul_wall_torch",
                            "minecraft:hay_block",
                            "minecraft:composter",
                            "minecraft:pumpkin",
                            "minecraft:carved_pumpkin",
                            "minecraft:jack_o_lantern",
                            "minecraft:melon",
                            "minecraft:ladder",
                            "minecraft:crafting_table"
                    ),
                    obj -> obj instanceof String
            );

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_TIER2_MIN_MOBS = BUILDER
            .comment(
                "Minimum group size to break Tier 2 blocks."
            )
            .defineInRange("hordeMentality.tier2MinMobs", 7, 1, 100);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HORDE_MENTALITY_TIER2_BLOCKS = BUILDER
            .comment(
                "Blocks breakable by a group of tier2MinMobs or more horde mobs.",
                "Accepts block IDs and block tags."
            )
            .defineListAllowEmpty("hordeMentality.tier2Blocks",
                    List.of(
                            "#minecraft:planks",
                            "#minecraft:wooden_slabs",
                            "#minecraft:wooden_stairs",
                            "#minecraft:logs",
                            "#minecraft:leaves",
                            "#minecraft:bamboo_blocks",
                            "#minecraft:pressure_plates",
                            "#minecraft:dirt",
                            "#c:sands",
                            "#c:gravels",
                            "#minecraft:snow",
                            "#minecraft:ice",
                            "#minecraft:beehives",
                            "minecraft:bookshelf",
                            "#minecraft:buttons",
                            "#minecraft:stone_pressure_plates",
                            "minecraft:lightning_rod",
                            "minecraft:stonecutter",
                            "minecraft:grindstone",
                            "minecraft:jukebox",
                            "minecraft:note_block",
                            "minecraft:loom",
                            "minecraft:fletching_table",
                            "minecraft:cartography_table",
                            "minecraft:brewing_stand",
                            "minecraft:lectern",
                            "minecraft:chiseled_bookshelf",
                            "minecraft:clay",
                            "minecraft:mud",
                            "minecraft:packed_mud",
                            "minecraft:dirt_path",
                            "minecraft:soul_sand",
                            "minecraft:soul_soil",
                            "minecraft:scaffolding",
                            "minecraft:cobweb",
                            "minecraft:brown_mushroom_block",
                            "minecraft:red_mushroom_block",
                            "minecraft:mushroom_stem"
                    ),
                    obj -> obj instanceof String
            );

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_TIER3_MIN_MOBS = BUILDER
            .comment(
                "Minimum group size to break Tier 3 blocks."
            )
            .defineInRange("hordeMentality.tier3MinMobs", 14, 1, 100);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HORDE_MENTALITY_TIER3_BLOCKS = BUILDER
            .comment(
                "Blocks breakable by a group of tier3MinMobs or more horde mobs.",
                "Accepts block IDs and block tags."
            )
            .defineListAllowEmpty("hordeMentality.tier3Blocks",
                    List.of(
                            "#c:cobblestones",
                            "#c:sandstone/blocks",
                            "#minecraft:walls",
                            "#minecraft:stairs",
                            "#minecraft:slabs",
                            "#minecraft:cauldrons",
                            "#minecraft:stone_bricks",
                            "#minecraft:pressure_plates",
                            "#minecraft:doors",
                            "#minecraft:trapdoors",
                            "minecraft:observer",
                            "minecraft:dropper",
                            "minecraft:dispenser",
                            "minecraft:sticky_piston",
                            "minecraft:piston",
                            "minecraft:hopper",
                            "minecraft:anvil",
                            "minecraft:chipped_anvil",
                            "minecraft:damaged_anvil",
                            "minecraft:blast_furnace",
                            "minecraft:furnace",
                            "minecraft:smoker",
                            "minecraft:smithing_table",
                            "minecraft:bricks",
                            "minecraft:nether_bricks",
                            "minecraft:cracked_nether_bricks",
                            "minecraft:chiseled_nether_bricks",
                            "minecraft:basalt",
                            "minecraft:smooth_basalt"
                    ),
                    obj -> obj instanceof String
            );

    private static final ModConfigSpec.IntValue HORDE_MENTALITY_TIER4_MIN_MOBS = BUILDER
            .comment(
                "Minimum group size to break Tier 4 blocks."
            )
            .defineInRange("hordeMentality.tier4MinMobs", 20, 1, 100);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HORDE_MENTALITY_TIER4_BLOCKS = BUILDER
            .comment(
                "Blocks breakable by a group of tier4MinMobs or more horde mobs.",
                "Accepts block IDs and block tags."
            )
            .defineListAllowEmpty("hordeMentality.tier4Blocks",
                    List.of(
                            "#c:stones",
                            "#c:ores",
                            "#c:storage_blocks",
                            "#c:end_stones",
                            "#c:netherracks",
                            "#minecraft:terracotta",
                            "#c:obsidians",
                            "minecraft:enchanting_table",
                            "minecraft:dripstone_block",
                            "minecraft:calcite",
                            "minecraft:amethyst_block",
                            "minecraft:budding_amethyst",
                            "minecraft:end_stone_bricks",
                            "minecraft:iron_bars",
                            "minecraft:purpur_block",
                            "minecraft:purpur_pillar",
                            "minecraft:prismarine",
                            "minecraft:prismarine_bricks",
                            "minecraft:dark_prismarine",
                            "minecraft:crying_obsidian"
                    ),
                    obj -> obj instanceof String
            );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HORDE_MENTALITY_BLACKLIST_BLOCKS = BUILDER
            .comment(
                "Blocks that horde mobs can NEVER break, even if they appear in a tier list.",
                "Accepts block IDs and block tags."
            )
            .defineListAllowEmpty("hordeMentality.blacklistBlocks",
                    List.of(),
                    obj -> obj instanceof String
            );

    // -----------------------------------------------------------------------
    // Universal Hostility
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_UNIVERSAL_HOSTILITY = BUILDER
            .comment(
                "Enable universal hostility: mobs listed in hostileMobs will attack mobs listed in hostilityTargetMobs on sight."
            )
            .define("universalHostility.enableUniversalHostility", false);

    private static final ModConfigSpec.BooleanValue ENABLE_HORDE_MENTALITY_WHEN_CHASING_TARGETS = BUILDER
            .comment(
                "Allow horde mentality block breaking when hostile mobs are pathfinding to a hostility target",
                "Has no effect if enableUniversalHostility is false."
            )
            .define("universalHostility.enableHordeMentalityWhenChasingTargets", false);

    private static final ModConfigSpec.BooleanValue ENABLE_HORDE_MENTALITY_DIG_DOWN_FOR_HOSTILITY = BUILDER
            .comment(
                "Allow horde mentality mobs chasing a hostility target to break the block beneath them",
                "when the target is below the mob.",
                "Has no effect if enableUniversalHostility or enableHordeMentalityWhenChasingTargets is false."
            )
            .define("universalHostility.allowHordeMentalityDigDownForHostility", false);

    private static final ModConfigSpec.IntValue UNIVERSAL_HOSTILITY_DAYS_BEFORE_ACTIVATION = BUILDER
            .comment(
                "In-game day threshold before Universal Hostility begins.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("universalHostility.daysBeforeActivation", 0, 0, 10000);

    private static final ModConfigSpec.BooleanValue ENABLE_PASSIVE_FEAR = BUILDER
            .comment(
                "Passive animals listed in hostilityTargetMobs will flee from mobs listed in hostileMobs.",
                "Has no effect if enableUniversalHostility is false."
            )
            .define("universalHostility.enablePassiveFear", true);

    private static final ModConfigSpec.BooleanValue ENABLE_NEUTRAL_FEAR = BUILDER
            .comment(
                "Neutral mobs listed in hostilityTargetMobs will flee from mobs listed in hostileMobs.",
                "Has no effect if enableUniversalHostility is false."
            )
            .define("universalHostility.enableNeutralFear", false);

    private static final ModConfigSpec.BooleanValue ENABLE_HOSTILE_FEAR = BUILDER
            .comment(
                "Hostile mobs listed in hostilityTargetMobs will flee from mobs listed in hostileMobs.",
                "Has no effect if enableUniversalHostility is false."
            )
            .define("universalHostility.enableHostileFear", false);

    private static final ModConfigSpec.BooleanValue PROTECT_TAMED_ANIMALS = BUILDER
            .comment(
                "Protects tamed animals from being targeted."
            )
            .define("universalHostility.protectTamedAnimals", false);

    private static final ModConfigSpec.BooleanValue PROTECT_NAMED_ENTITIES = BUILDER
            .comment(
                "Protects named entities from being targeted."
            )
            .define("universalHostility.protectNamedEntities", false);

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HOSTILE_MOBS = BUILDER
            .comment(
                "Mobs that will be universally hostile toward everything in hostilityTargetMobs.",
                "Accepts entity IDs and entity tags."
            )
            .defineListAllowEmpty("universalHostility.hostileMobs",
                    List.of(
                            "minecraft:zombie",
                            "minecraft:zombie_villager",
                            "minecraft:husk",
                            "minecraft:drowned"
                    ),
                    obj -> obj instanceof String
            );

    private static final ModConfigSpec.ConfigValue<List<? extends String>> HOSTILITY_TARGET_MOBS = BUILDER
            .comment(
                "Mobs that will be targeted by every mob listed in hostileMobs.",
                "Accepts entity IDs and entity tags."
            )
            .defineListAllowEmpty("universalHostility.hostilityTargetMobs",
                    List.of(
                            "minecraft:cow",
                            "minecraft:pig",
                            "minecraft:sheep",
                            "minecraft:chicken",
                            "minecraft:rabbit",
                            "minecraft:horse",
                            "minecraft:donkey",
                            "minecraft:mule",
                            "minecraft:llama",
                            "minecraft:trader_llama",
                            "minecraft:mooshroom",
                            "minecraft:cat",
                            "minecraft:ocelot",
                            "minecraft:wolf",
                            "minecraft:fox",
                            "minecraft:panda",
                            "minecraft:axolotl",
                            "minecraft:frog",
                            "minecraft:goat",
                            "minecraft:camel",
                            "minecraft:sniffer",
                            "minecraft:polar_bear"
                    ),
                    obj -> obj instanceof String
            );

    // -----------------------------------------------------------------------
    // Horde Determination
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_HORDE_DETERMINATION = BUILDER
            .comment(
                "Horde mobs will continue to pathfind toward a player across long distances"
            )
            .define("hordeDetermination.enableHordeDetermination", false);

    private static final ModConfigSpec.IntValue HORDE_DETERMINATION_FOLLOW_DISTANCE = BUILDER
            .comment(
                "Maximum distance (in blocks) a horde mob will follow its target before giving up.",
                "Range: 1 ~ 10000"
            )
            .defineInRange("hordeDetermination.followDistance", 300, 1, 10000);

    private static final ModConfigSpec.IntValue HORDE_DETERMINATION_FOLLOW_TIME_MINUTES = BUILDER
            .comment(
                "Maximum time (in minutes) a horde mob will continue following its target.",
                "Set to 0 for no time limit.",
                "Range: 0 ~ 1440"
            )
            .defineInRange("hordeDetermination.followTimeMinutes", 60, 0, 1440);

    private static final ModConfigSpec.IntValue HORDE_DETERMINATION_DAYS_BEFORE_ACTIVATION = BUILDER
            .comment(
                "In-game day threshold before Horde Determination begins.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("hordeDetermination.daysBeforeActivation", 0, 0, 10000);

    private static final ModConfigSpec.BooleanValue HORDE_DETERMINATION_DISTANCE_INCREASE_OVER_TIME = BUILDER
            .comment(
                "Should the horde mob follow distance increase over time?"
            )
            .define("hordeDetermination.followDistanceIncreaseOverTime", false);

    private static final ModConfigSpec.IntValue HORDE_DETERMINATION_DISTANCE_INCREASE_INTERVAL_DAYS = BUILDER
            .comment(
                "How many in-game days between each follow distance increase.",
                "Counting starts after daysBeforeActivation has elapsed.",
                "Has no effect if followDistanceIncreaseOverTime is false.",
                "Range: 1 ~ 10000"
            )
            .defineInRange("hordeDetermination.followDistanceIncreaseIntervalDays", 1, 1, 10000);

    private static final ModConfigSpec.IntValue HORDE_DETERMINATION_DISTANCE_INCREASE_AMOUNT = BUILDER
            .comment(
                "How many blocks to add to the follow distance on each increase.",
                "Has no effect if followDistanceIncreaseOverTime is false.",
                "Range: 1 ~ 10000"
            )
            .defineInRange("hordeDetermination.followDistanceIncreaseAmount", 50, 1, 10000);

    private static final ModConfigSpec.BooleanValue HORDE_DETERMINATION_TIME_INCREASE_OVER_TIME = BUILDER
            .comment(
                "Should the horde mob follow time increase over time?",
                "Has no effect if followTimeMinutes is 0 (no time limit)."
            )
            .define("hordeDetermination.followTimeIncreaseOverTime", false);

    private static final ModConfigSpec.IntValue HORDE_DETERMINATION_TIME_INCREASE_INTERVAL_DAYS = BUILDER
            .comment(
                "How many in-game days between each follow time increase.",
                "Counting starts after daysBeforeActivation has elapsed.",
                "Has no effect if followTimeIncreaseOverTime is false.",
                "Range: 1 ~ 10000"
            )
            .defineInRange("hordeDetermination.followTimeIncreaseIntervalDays", 1, 1, 10000);

    private static final ModConfigSpec.IntValue HORDE_DETERMINATION_TIME_INCREASE_AMOUNT = BUILDER
            .comment(
                "How many minutes to add to the follow time on each increase.",
                "Has no effect if followTimeIncreaseOverTime is false.",
                "Range: 1 ~ 1440"
            )
            .defineInRange("hordeDetermination.followTimeIncreaseAmount", 10, 1, 1440);

    // -----------------------------------------------------------------------
    // Horde Wandering
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_HORDE_WANDERING = BUILDER
            .comment(
                "Horde mobs will collect into groups that roam together. Groups merge when they meet and split apart when their members spread out. Incompatible with Zombie Awareness wandering hordes."
            )
            .define("hordeWandering.enableHordeWandering", false);

    private static final ModConfigSpec.IntValue MAX_HORDE_GROUP = BUILDER
            .comment(
                "Maximum number of mobs allowed in a single group.",
                "A horde mob that finds a group already at the cap will not join it.",
                "0 = no group size cap.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("hordeWandering.maxHordeGroup", 0, 0, 10000);

    private static final ModConfigSpec.IntValue HORDE_GROUP_MINIMUM = BUILDER
            .comment(
                "Minimum number of horde mobs required to form a group.",
                "Range: 2 ~ 10000"
            )
            .defineInRange("hordeWandering.hordeGroupMinimum", 5, 2, 10000);

    private static final ModConfigSpec.IntValue HORDE_WANDERING_DIRECTION_CHANGE_MINUTES = BUILDER
            .comment(
                "How many minutes a group wanders in the same direction before picking a new one.",
                "Groups that merge pick a fresh direction on merge regardless of this timer.",
                "Range: 1 ~ 1440"
            )
            .defineInRange("hordeWandering.directionChangeMinutes", 5, 1, 1440);

    private static final ModConfigSpec.IntValue HORDE_WANDERING_DAYS_BEFORE_ACTIVATION = BUILDER
            .comment(
                "In-game day threshold before Horde Wandering begins.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("hordeWandering.daysBeforeActivation", 0, 0, 10000);

    private static final ModConfigSpec.BooleanValue HORDE_WANDERING_AVOID_WATER = BUILDER
            .comment("Should wandering groups avoid water?")
            .define("hordeWandering.avoidWater", true);

    private static final ModConfigSpec.BooleanValue HORDE_WANDERING_AVOID_FALLS = BUILDER
            .comment("Should wandering groups avoid falls?")
            .define("hordeWandering.avoidFalls", true);

    // -----------------------------------------------------------------------
    // Collective Understanding
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_COLLECTIVE_UNDERSTANDING = BUILDER
            .comment(
                "A horde mob that sees another horde mob currently pursuing a player",
                "will pathfind to that same player."
            )
            .define("collectiveUnderstanding.enableCollectiveUnderstanding", false);

    private static final ModConfigSpec.IntValue COLLECTIVE_UNDERSTANDING_RANGE = BUILDER
            .comment(
                "Block range within which a horde mob will notice another horde mob pursuing a player.",
                "Range: 1 ~ 128"
            )
            .defineInRange("collectiveUnderstanding.range", 50, 1, 128);

    private static final ModConfigSpec.IntValue COLLECTIVE_UNDERSTANDING_DAYS_BEFORE_ACTIVATION = BUILDER
            .comment(
                "In-game day threshold before Collective Understanding begins.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("collectiveUnderstanding.daysBeforeActivation", 0, 0, 10000);

    private static final ModConfigSpec.BooleanValue COLLECTIVE_UNDERSTANDING_INCREASE_OVER_TIME = BUILDER
            .comment(
                "Should the notice range increase over time?"
            )
            .define("collectiveUnderstanding.increaseOverTime", false);

    private static final ModConfigSpec.IntValue COLLECTIVE_UNDERSTANDING_INCREASE_INTERVAL_DAYS = BUILDER
            .comment(
                "How many in-game days between each range increase.",
                "Counting starts after daysBeforeActivation has elapsed.",
                "Has no effect if increaseOverTime is false.",
                "Range: 1 ~ 10000"
            )
            .defineInRange("collectiveUnderstanding.increaseIntervalDays", 1, 1, 10000);

    private static final ModConfigSpec.IntValue COLLECTIVE_UNDERSTANDING_INCREASE_AMOUNT = BUILDER
            .comment(
                "How many blocks to add to the notice range on each increase.",
                "Has no effect if increaseOverTime is false.",
                "Range: 1 ~ 128"
            )
            .defineInRange("collectiveUnderstanding.increaseAmount", 5, 1, 128);

    // -----------------------------------------------------------------------
    // Heightened Sense
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_HEIGHTENED_SENSE = BUILDER
            .comment(
                "Horde mobs automatically know the location of any player or Universal Hostility target",
                "if enableUniversalHostility is true and they are in the configured range, even through walls"
            )
            .define("heightenedSense.enableHeightenedSense", false);

    private static final ModConfigSpec.IntValue HEIGHTENED_SENSE_RANGE = BUILDER
            .comment(
                "Block range within which a horde mob will sense a valid target.",
                "Range: 1 ~ 128"
            )
            .defineInRange("heightenedSense.range", 10, 1, 128);

    private static final ModConfigSpec.IntValue HEIGHTENED_SENSE_DAYS_BEFORE_ACTIVATION = BUILDER
            .comment(
                "In-game day threshold before Heightened Sense begins.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("heightenedSense.daysBeforeActivation", 0, 0, 10000);

    private static final ModConfigSpec.BooleanValue HEIGHTENED_SENSE_INCREASE_OVER_TIME = BUILDER
            .comment(
                "Should the sense range increase over time?"
            )
            .define("heightenedSense.increaseOverTime", false);

    private static final ModConfigSpec.IntValue HEIGHTENED_SENSE_INCREASE_INTERVAL_DAYS = BUILDER
            .comment(
                "How many in-game days between each sense range increase.",
                "Counting starts after daysBeforeActivation has elapsed.",
                "Has no effect if increaseOverTime is false.",
                "Range: 1 ~ 10000"
            )
            .defineInRange("heightenedSense.increaseIntervalDays", 1, 1, 10000);

    private static final ModConfigSpec.IntValue HEIGHTENED_SENSE_INCREASE_AMOUNT = BUILDER
            .comment(
                "How many blocks to add to the sense range on each increase.",
                "Has no effect if increaseOverTime is false.",
                "Range: 1 ~ 128"
            )
            .defineInRange("heightenedSense.increaseAmount", 5, 1, 128);

    // -----------------------------------------------------------------------
    // Horde Sight
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.IntValue HORDE_SIGHT_RANGE_BONUS = BUILDER
            .comment(
                "Blocks to add to a horde mob's default sight.",
                "0 = Vanilla.",
                "Range: 0 ~ 256"
            )
            .defineInRange("hordeSight.sightRangeBonus", 0, 0, 256);

    private static final ModConfigSpec.IntValue HORDE_SIGHT_DAYS_BEFORE_ACTIVATION = BUILDER
            .comment(
                "In-game day threshold before Horde Sight is applied.",
                "Range: 0 ~ 10000"
            )
            .defineInRange("hordeSight.daysBeforeActivation", 0, 0, 10000);

    private static final ModConfigSpec.BooleanValue HORDE_SIGHT_INCREASE_OVER_TIME = BUILDER
            .comment(
                "Should the horde mob sight range increase over time?"
            )
            .define("hordeSight.increaseOverTime", false);

    private static final ModConfigSpec.IntValue HORDE_SIGHT_INCREASE_INTERVAL_DAYS = BUILDER
            .comment(
                "How many in-game days between each sight range increase.",
                "Counting starts after daysBeforeActivation has elapsed.",
                "Has no effect if increaseOverTime is false.",
                "Range: 1 ~ 10000"
            )
            .defineInRange("hordeSight.increaseIntervalDays", 1, 1, 10000);

    private static final ModConfigSpec.IntValue HORDE_SIGHT_INCREASE_AMOUNT = BUILDER
            .comment(
                "How many blocks to add to the sight range on each increase.",
                "Has no effect if increaseOverTime is false.",
                "Range: 1 ~ 256"
            )
            .defineInRange("hordeSight.increaseAmount", 1, 1, 256);

    // -----------------------------------------------------------------------
    // Notifications
    // -----------------------------------------------------------------------

    private static final ModConfigSpec.BooleanValue ENABLE_ACTIVATION_NOTIFICATIONS = BUILDER
            .comment(
                "Announce when a feature reaches its day activation threshold while players are online."
            )
            .define("notifications.enableActivationNotifications", false);

    private static final ModConfigSpec.BooleanValue NOTIFICATION_PLAY_SOUND = BUILDER
            .comment(
                "Play a sound alongside each activation message.",
                "Has no effect if enableActivationNotifications is false."
            )
            .define("notifications.playSound", true);

    private static final ModConfigSpec.ConfigValue<String> NOTIFICATION_SOUND = BUILDER
            .comment(
                "Sound played on activation. Accepts a sound resource location.",
                "Has no effect if playSound is false."
            )
            .define("notifications.sound", "minecraft:entity.wither.spawn");

    private static final ModConfigSpec.ConfigValue<String> NOTIFY_FEATURES_MESSAGE = BUILDER
            .comment(
                "Message shown when the shared Features day threshold is reached.",
                "Blank = no message. Supports & formatting codes."
            )
            .define("notifications.featuresMessage", "");

    private static final ModConfigSpec.ConfigValue<String> NOTIFY_HORDE_MENTALITY_MESSAGE = BUILDER
            .comment("Message shown when Horde Mentality activates. Blank = none. Supports & formatting codes.")
            .define("notifications.hordeMentalityMessage", "");

    private static final ModConfigSpec.ConfigValue<String> NOTIFY_UNIVERSAL_HOSTILITY_MESSAGE = BUILDER
            .comment("Message shown when Universal Hostility activates. Blank = none. Supports & formatting codes.")
            .define("notifications.universalHostilityMessage", "");

    private static final ModConfigSpec.ConfigValue<String> NOTIFY_HORDE_DETERMINATION_MESSAGE = BUILDER
            .comment("Message shown when Horde Determination activates. Blank = none. Supports & formatting codes.")
            .define("notifications.hordeDeterminationMessage", "");

    private static final ModConfigSpec.ConfigValue<String> NOTIFY_HORDE_WANDERING_MESSAGE = BUILDER
            .comment("Message shown when Horde Wandering activates. Blank = none. Supports & formatting codes.")
            .define("notifications.hordeWanderingMessage", "");

    private static final ModConfigSpec.ConfigValue<String> NOTIFY_COLLECTIVE_UNDERSTANDING_MESSAGE = BUILDER
            .comment("Message shown when Collective Understanding activates. Blank = none. Supports & formatting codes.")
            .define("notifications.collectiveUnderstandingMessage", "");

    private static final ModConfigSpec.ConfigValue<String> NOTIFY_HEIGHTENED_SENSE_MESSAGE = BUILDER
            .comment("Message shown when Heightened Sense activates. Blank = none. Supports & formatting codes.")
            .define("notifications.heightenedSenseMessage", "");

    private static final ModConfigSpec.ConfigValue<String> NOTIFY_HORDE_SIGHT_MESSAGE = BUILDER
            .comment("Message shown when Horde Sight activates. Blank = none. Supports & formatting codes.")
            .define("notifications.hordeSightMessage", "");

    private static final ModConfigSpec.ConfigValue<String> NOTIFY_CREEPER_WALL_EXPLOSION_MESSAGE = BUILDER
            .comment("Message shown when Creeper Wall Explosion activates. Blank = none. Supports & formatting codes.")
            .define("notifications.creeperWallExplosionMessage", "");

    // -----------------------------------------------------------------------
    // Game Stages
    // -----------------------------------------------------------------------

    private static ModConfigSpec.BooleanValue ENABLE_GAME_STAGES;
    private static ModConfigSpec.ConfigValue<String> GAME_STAGES_HORDE_DETERMINATION_STAGE;
    private static ModConfigSpec.ConfigValue<String> GAME_STAGES_HEIGHTENED_SENSE_STAGE;
    private static ModConfigSpec.ConfigValue<String> GAME_STAGES_COLLECTIVE_UNDERSTANDING_STAGE;
    private static ModConfigSpec.ConfigValue<String> GAME_STAGES_HORDE_MENTALITY_STAGE;
    private static ModConfigSpec.ConfigValue<String> GAME_STAGES_HORDE_MULTIPLYING_STAGE;
    private static ModConfigSpec.ConfigValue<String> GAME_STAGES_UNIVERSAL_HOSTILITY_STAGE;

    static {
        if (GAME_STAGES_INSTALLED) {
            ENABLE_GAME_STAGES = BUILDER
                    .comment(
                        "Enable Game Stages integration.",
                        "Per-player features (Horde Determination, Heightened Sense, Collective Understanding)",
                        "only affect players who have the configured stage.",
                        "World features (Horde Mentality, Horde Multiplying, Universal Hostility)",
                        "stay active only while at least one online player has the configured stage.",
                        "Leave a stage blank to apply that feature to all players."
                    )
                    .define("gameStages.enableGameStages", false);

            GAME_STAGES_HORDE_DETERMINATION_STAGE = BUILDER
                    .comment(
                        "Game stage a player must have for Horde Determination to track them.",
                        "Blank = applies to all players. Has no effect if enableGameStages is false."
                    )
                    .define("gameStages.hordeDeterminationStage", "");

            GAME_STAGES_HEIGHTENED_SENSE_STAGE = BUILDER
                    .comment(
                        "Game stage a player must have to be sensed by Heightened Sense.",
                        "Blank = applies to all players. Has no effect if enableGameStages is false."
                    )
                    .define("gameStages.heightenedSenseStage", "");

            GAME_STAGES_COLLECTIVE_UNDERSTANDING_STAGE = BUILDER
                    .comment(
                        "Game stage a player must have for horde mobs to relay them through Collective Understanding.",
                        "Blank = applies to all players. Has no effect if enableGameStages is false."
                    )
                    .define("gameStages.collectiveUnderstandingStage", "");

            GAME_STAGES_HORDE_MENTALITY_STAGE = BUILDER
                    .comment(
                        "Game stage at least one online player must have for Horde Mentality to stay active.",
                        "Blank = always active. Has no effect if enableGameStages is false."
                    )
                    .define("gameStages.hordeMentalityStage", "");

            GAME_STAGES_HORDE_MULTIPLYING_STAGE = BUILDER
                    .comment(
                        "Game stage at least one online player must have for Horde Multiplying to stay active.",
                        "Blank = always active. Has no effect if enableGameStages is false."
                    )
                    .define("gameStages.hordeMultiplyingStage", "");

            GAME_STAGES_UNIVERSAL_HOSTILITY_STAGE = BUILDER
                    .comment(
                        "Game stage at least one online player must have for Universal Hostility to stay active.",
                        "Blank = always active. Has no effect if enableGameStages is false."
                    )
                    .define("gameStages.universalHostilityStage", "");
        }
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    // -----------------------------------------------------------------------
    // Resolved values (populated by onLoad)
    // -----------------------------------------------------------------------

    public static DifficultyPreset difficultyPreset;
    public static boolean globalNightOnly;
    public static boolean lightLevelGating;
    public static int maxActiveLightLevel;

    public static boolean enableGraceRadius;
    public static int graceRadius;
    public static boolean graceUseWorldSpawn;
    public static boolean graceUsePlayerSpawn;

    public static boolean enableActivationNotifications;
    public static boolean notificationPlaySound;
    public static String notificationSound;
    public static String notifyFeaturesMessage;
    public static String notifyHordeMentalityMessage;
    public static String notifyUniversalHostilityMessage;
    public static String notifyHordeDeterminationMessage;
    public static String notifyHordeWanderingMessage;
    public static String notifyCollectiveUnderstandingMessage;
    public static String notifyHeightenedSenseMessage;
    public static String notifyHordeSightMessage;
    public static String notifyCreeperWallExplosionMessage;

    public static boolean enableHordeStacking;
    public static boolean hordeFireSpread;
    public static boolean hordeFireSpeedBoost;
    public static int hordeFireSpeedAmplifier;
    public static int hordeFireDamageResistance;
    public static boolean hordeFallDamageImmunity;
    public static boolean hordeBabyThrow;
    public static boolean hordeBabyBlockBreaking;
    public static boolean hordeBabyFireSpeedBoost;
    public static boolean enableHordeMultiplying;
    public static boolean enableIntelligentTeams;
    public static boolean enableLeapingMobs;
    public static boolean enableIntelligentPiglins;
    public static boolean enableHiddenZombies;
    public static boolean enableHordeBlockBreaking;
    public static boolean disableGameruleCommands;
    public static int featuresDaysBeforeActivation;

    public static boolean enableFriendlyFire;
    public static boolean allowFriendlyFireToHordeMobs;
    public static boolean enableZombifiedPiglinCrossbow;

    public static boolean enableIronGolemMultiHit;
    public static boolean enableIronGolemFireImmunity;
    public static boolean enableIronGolemEffectImmunity;
    public static boolean enableIronGolemArrowResistance;
    public static boolean enableIronGolemHurry;
    public static boolean enableIronGolemRegen;
    public static int ironGolemRegenPower;
    public static boolean enableWitherSkeletonBowTactics;
    public static boolean enableCreeperWallExplosion;
    public static int creeperWallExplosionDistance;

    public static List<? extends String> hordeMobs;
    public static List<? extends String> intelligentTeamMobs;
    public static List<? extends String> leapingMobs;
    public static List<? extends String> graveRobbers;
    public static List<? extends String> intelligentPiglins;

    public static List<? extends String> hiddenZombieBlocks;
    public static List<? extends String> hordeBreakableBlocks;

    public static boolean enableBlockRegeneration;
    public static int regenerationDelaySeconds;
    public static boolean requireNearbyPlayers;
    public static int nearbyPlayerRadius;
    public static boolean checkEntityCollision;
    public static boolean requireMobsClearedBeforeRegen;
    public static int mobClearedRadius;
    public static boolean cancelRegenOnMobReturn;
    public static boolean resetDelayOnMobReturn;
    public static boolean showPreRegenWarningParticles;
    public static boolean staggeredRegen;
    public static int staggeredRegenIntervalTicks;
    public static boolean showRegenerationParticles;
    public static boolean playRegenerationSound;

    public static boolean enableHordeMentality;
    public static int hordeMentalityGroupRadius;
    public static int hordeMentalityDamageRatePerMob;
    public static int hordeMentalityDamageIncreasePerDay;
    public static int hordeMentalityDamageMultiplierPerDay;
    public static boolean hordeMentalityScaleWithDamage;
    public static int hordeMentalityHardnessScaling;
    public static int hordeMentalityDamageLingerSeconds;
    public static boolean hordeMentalityInstantBreak;
    public static int hordeMentalitySwingIntervalTicks;
    public static int hordeMentalityHitChancePercent;
    public static boolean hordeMentalityRequirePlayerTarget;
    public static boolean hordeMentalityCheckPlayerProximity;
    public static int hordeMentalityPlayerProximityRadius;
    public static boolean hordeMentalityRequireBlockInDirection;
    public static boolean hordeMentalityBabyMobsContribute;
    public static boolean hordeMentalityBabyMobsCanBreak;
    public static boolean hordeMentalityDropBlockItems;
    public static boolean hordeMentalityProtectSupportingBlocks;
    public static boolean hordeMentalityAllowDigDownToPlayer;
    public static int hordeMentalityDaysBeforeActivation;
    public static boolean hordeMentalityShowBreakParticles;
    public static boolean hordeMentalityPlayBreakSound;
    public static boolean hordeMentalityPlayHitSound;
    public static boolean hordeMentalityShowHitParticles;
    public static int hordeMentalityTier1MinMobs;
    public static List<? extends String> hordeMentalityTier1Blocks;
    public static int hordeMentalityTier2MinMobs;
    public static List<? extends String> hordeMentalityTier2Blocks;
    public static int hordeMentalityTier3MinMobs;
    public static List<? extends String> hordeMentalityTier3Blocks;
    public static int hordeMentalityTier4MinMobs;
    public static List<? extends String> hordeMentalityTier4Blocks;
    public static List<? extends String> hordeMentalityBlacklistBlocks;
    public static boolean hordeMentalityNightOnly;
    public static boolean hordeMentalityTierDamageScaling;
    public static boolean hordeMentalityProgressiveScaling;
    public static int hordeMentalityProgressiveScalingBonusPerMob;
    public static boolean hordeMentalityDropTier1Blocks;
    public static boolean hordeMentalityDropTier2Blocks;
    public static boolean hordeMentalityDropTier3Blocks;
    public static boolean hordeMentalityDropTier4Blocks;

    public static boolean regenDaytimeOnly;
    public static boolean regenScaleDelayByHardness;
    public static boolean cancelRegenOnPlayerPlace;
    public static int playerPlaceCancelRadius;

    public static int intelligentMobMinDashDistance;

    public static int ironGolemArrowResistancePercent;
    public static int ironGolemRegenCooldownSeconds;
    public static boolean enableIronGolemVillagerDefender;

    public static boolean enableUniversalHostility;
    public static boolean enableHordeMentalityWhenChasingTargets;
    public static boolean allowHordeMentalityDigDownForHostility;
    public static int universalHostilityDaysBeforeActivation;
    public static boolean protectTamedAnimals;
    public static boolean protectNamedEntities;
    public static List<? extends String> hostileMobs;
    public static List<? extends String> hostilityTargetMobs;

    public static boolean enableHordeDetermination;
    public static int hordeDeterminationFollowDistance;
    public static int hordeDeterminationFollowTimeMinutes;
    public static int hordeDeterminationDaysBeforeActivation;
    public static boolean hordeDeterminationDistanceIncreaseOverTime;
    public static int hordeDeterminationDistanceIncreaseIntervalDays;
    public static int hordeDeterminationDistanceIncreaseAmount;
    public static boolean hordeDeterminationTimeIncreaseOverTime;
    public static int hordeDeterminationTimeIncreaseIntervalDays;
    public static int hordeDeterminationTimeIncreaseAmount;

    public static boolean enableHordeWandering;
    public static int maxHordeGroup;
    public static int hordeGroupMinimum;
    public static int hordeWanderingDirectionChangeMinutes;
    public static int hordeWanderingDaysBeforeActivation;
    public static boolean hordeWanderingAvoidWater;
    public static boolean hordeWanderingAvoidFalls;

    public static boolean enableHordeSwimming;
    public static int hordeMobCap;
    public static int creeperWallExplosionDaysBeforeActivation;
    public static boolean enablePassiveFear;
    public static boolean enableNeutralFear;
    public static boolean enableHostileFear;

    public static boolean enableCollectiveUnderstanding;
    public static int collectiveUnderstandingRange;
    public static int collectiveUnderstandingDaysBeforeActivation;
    public static boolean collectiveUnderstandingIncreaseOverTime;
    public static int collectiveUnderstandingIncreaseIntervalDays;
    public static int collectiveUnderstandingIncreaseAmount;

    public static boolean enableHeightenedSense;
    public static int heightenedSenseRange;
    public static int heightenedSenseDaysBeforeActivation;
    public static boolean heightenedSenseIncreaseOverTime;
    public static int heightenedSenseIncreaseIntervalDays;
    public static int heightenedSenseIncreaseAmount;

    public static int hordeSightRangeBonus;
    public static int hordeSightDaysBeforeActivation;
    public static boolean hordeSightIncreaseOverTime;
    public static int hordeSightIncreaseIntervalDays;
    public static int hordeSightIncreaseAmount;

    public static boolean enableGameStages;
    public static String hordeDeterminationStage;
    public static String heightenedSenseStage;
    public static String collectiveUnderstandingStage;
    public static String hordeMentalityStage;
    public static String hordeMultiplyingStage;
    public static String universalHostilityStage;

    public static boolean daysElapsedReached(Level level, int threshold) {
        if (threshold <= 0) return true;
        return level.getGameTime() >= (long) threshold * 24000L;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (!event.getConfig().getModId().equals(EnhancedHordesTweaksMod.MODID)) return;

        difficultyPreset = DIFFICULTY_PRESET.get();
        globalNightOnly = GLOBAL_NIGHT_ONLY.get();
        lightLevelGating = LIGHT_LEVEL_GATING.get();
        maxActiveLightLevel = MAX_ACTIVE_LIGHT_LEVEL.get();

        enableGraceRadius = ENABLE_GRACE_RADIUS.get();
        graceRadius = GRACE_RADIUS.get();
        graceUseWorldSpawn = GRACE_USE_WORLD_SPAWN.get();
        graceUsePlayerSpawn = GRACE_USE_PLAYER_SPAWN.get();

        enableActivationNotifications = ENABLE_ACTIVATION_NOTIFICATIONS.get();
        notificationPlaySound = NOTIFICATION_PLAY_SOUND.get();
        notificationSound = NOTIFICATION_SOUND.get();
        notifyFeaturesMessage = NOTIFY_FEATURES_MESSAGE.get();
        notifyHordeMentalityMessage = NOTIFY_HORDE_MENTALITY_MESSAGE.get();
        notifyUniversalHostilityMessage = NOTIFY_UNIVERSAL_HOSTILITY_MESSAGE.get();
        notifyHordeDeterminationMessage = NOTIFY_HORDE_DETERMINATION_MESSAGE.get();
        notifyHordeWanderingMessage = NOTIFY_HORDE_WANDERING_MESSAGE.get();
        notifyCollectiveUnderstandingMessage = NOTIFY_COLLECTIVE_UNDERSTANDING_MESSAGE.get();
        notifyHeightenedSenseMessage = NOTIFY_HEIGHTENED_SENSE_MESSAGE.get();
        notifyHordeSightMessage = NOTIFY_HORDE_SIGHT_MESSAGE.get();
        notifyCreeperWallExplosionMessage = NOTIFY_CREEPER_WALL_EXPLOSION_MESSAGE.get();

        enableHordeStacking = ENABLE_HORDE_STACKING.get();
        hordeFireSpread = HORDE_FIRE_SPREAD.get();
        hordeFireSpeedBoost = HORDE_FIRE_SPEED_BOOST.get();
        hordeFireSpeedAmplifier = HORDE_FIRE_SPEED_AMPLIFIER.get();
        hordeFireDamageResistance = HORDE_FIRE_DAMAGE_RESISTANCE.get();
        hordeFallDamageImmunity = HORDE_FALL_DAMAGE_IMMUNITY.get();
        hordeBabyThrow = HORDE_BABY_THROW.get();
        hordeBabyBlockBreaking = HORDE_BABY_BLOCK_BREAKING.get();
        hordeBabyFireSpeedBoost = HORDE_BABY_FIRE_SPEED_BOOST.get();
        enableHordeMultiplying = ENABLE_HORDE_MULTIPLYING.get();
        enableIntelligentTeams = ENABLE_INTELLIGENT_TEAMS.get();
        enableLeapingMobs = ENABLE_LEAPING_MOBS.get();
        enableIntelligentPiglins = ENABLE_INTELLIGENT_PIGLINS.get();
        enableHiddenZombies = ENABLE_HIDDEN_ZOMBIES.get();
        enableHordeBlockBreaking = ENABLE_HORDE_BLOCK_BREAKING.get();
        disableGameruleCommands = DISABLE_GAMERULE_COMMANDS.get();
        featuresDaysBeforeActivation = FEATURES_DAYS_BEFORE_ACTIVATION.get();

        enableFriendlyFire = ENABLE_FRIENDLY_FIRE.get();
        allowFriendlyFireToHordeMobs = ALLOW_FRIENDLY_FIRE_TO_HORDE_MOBS.get();
        enableZombifiedPiglinCrossbow = ENABLE_ZOMBIFIED_PIGLIN_CROSSBOW.get();
        intelligentMobMinDashDistance = INTELLIGENT_MOB_MIN_DASH_DISTANCE.get();

        enableIronGolemMultiHit = ENABLE_IRON_GOLEM_MULTI_HIT.get();
        enableIronGolemFireImmunity = ENABLE_IRON_GOLEM_FIRE_IMMUNITY.get();
        enableIronGolemEffectImmunity = ENABLE_IRON_GOLEM_EFFECT_IMMUNITY.get();
        enableIronGolemArrowResistance = ENABLE_IRON_GOLEM_ARROW_RESISTANCE.get();
        ironGolemArrowResistancePercent = IRON_GOLEM_ARROW_RESISTANCE_PERCENT.get();
        enableIronGolemHurry = ENABLE_IRON_GOLEM_HURRY.get();
        enableIronGolemRegen = ENABLE_IRON_GOLEM_REGEN.get();
        ironGolemRegenPower = IRON_GOLEM_REGEN_POWER.get();
        ironGolemRegenCooldownSeconds = IRON_GOLEM_REGEN_COOLDOWN_SECONDS.get();
        enableIronGolemVillagerDefender = ENABLE_IRON_GOLEM_VILLAGER_DEFENDER.get();
        enableWitherSkeletonBowTactics = ENABLE_WITHER_SKELETON_BOW_TACTICS.get();
        enableCreeperWallExplosion = ENABLE_CREEPER_WALL_EXPLOSION.get();
        creeperWallExplosionDistance = CREEPER_WALL_EXPLOSION_DISTANCE.get();

        hordeMobs = HORDE_MOBS.get();
        intelligentTeamMobs = INTELLIGENT_TEAM_MOBS.get();
        leapingMobs = LEAPING_MOBS.get();
        graveRobbers = GRAVE_ROBBERS.get();
        intelligentPiglins = INTELLIGENT_PIGLINS.get();

        hiddenZombieBlocks = HIDDEN_ZOMBIE_BLOCKS.get();
        hordeBreakableBlocks = HORDE_BREAKABLE_BLOCKS.get();

        enableBlockRegeneration = ENABLE_BLOCK_REGENERATION.get();
        regenerationDelaySeconds = REGENERATION_DELAY_SECONDS.get();
        requireNearbyPlayers = REQUIRE_NEARBY_PLAYERS.get();
        nearbyPlayerRadius = NEARBY_PLAYER_RADIUS.get();
        checkEntityCollision = CHECK_ENTITY_COLLISION.get();
        requireMobsClearedBeforeRegen = REQUIRE_MOBS_CLEARED_BEFORE_REGEN.get();
        mobClearedRadius = MOB_CLEARED_RADIUS.get();
        cancelRegenOnMobReturn = CANCEL_REGEN_ON_MOB_RETURN.get();
        resetDelayOnMobReturn = RESET_DELAY_ON_MOB_RETURN.get();
        showPreRegenWarningParticles = SHOW_PRE_REGEN_WARNING_PARTICLES.get();
        staggeredRegen = STAGGERED_REGEN.get();
        staggeredRegenIntervalTicks = STAGGERED_REGEN_INTERVAL_TICKS.get();
        showRegenerationParticles = SHOW_REGENERATION_PARTICLES.get();
        playRegenerationSound = PLAY_REGENERATION_SOUND.get();
        regenDaytimeOnly = REGEN_DAYTIME_ONLY.get();
        regenScaleDelayByHardness = REGEN_SCALE_DELAY_BY_HARDNESS.get();
        cancelRegenOnPlayerPlace = CANCEL_REGEN_ON_PLAYER_PLACE.get();
        playerPlaceCancelRadius = PLAYER_PLACE_CANCEL_RADIUS.get();

        enableHordeMentality = ENABLE_HORDE_MENTALITY.get();
        hordeMentalityNightOnly = HORDE_MENTALITY_NIGHT_ONLY.get();
        hordeMentalityGroupRadius = HORDE_MENTALITY_GROUP_RADIUS.get();
        hordeMentalityDamageRatePerMob = HORDE_MENTALITY_DAMAGE_RATE_PER_MOB.get();
        hordeMentalityDamageIncreasePerDay = HORDE_MENTALITY_DAMAGE_INCREASE_PER_DAY.get();
        hordeMentalityDamageMultiplierPerDay = HORDE_MENTALITY_DAMAGE_MULTIPLIER_PER_DAY.get();
        hordeMentalityScaleWithDamage = HORDE_MENTALITY_SCALE_WITH_DAMAGE.get();
        hordeMentalityTierDamageScaling = HORDE_MENTALITY_TIER_DAMAGE_SCALING.get();
        hordeMentalityProgressiveScaling = HORDE_MENTALITY_PROGRESSIVE_SCALING.get();
        hordeMentalityProgressiveScalingBonusPerMob = HORDE_MENTALITY_PROGRESSIVE_SCALING_BONUS_PER_MOB.get();
        hordeMentalityDropTier1Blocks = HORDE_MENTALITY_DROP_TIER1_BLOCKS.get();
        hordeMentalityDropTier2Blocks = HORDE_MENTALITY_DROP_TIER2_BLOCKS.get();
        hordeMentalityDropTier3Blocks = HORDE_MENTALITY_DROP_TIER3_BLOCKS.get();
        hordeMentalityDropTier4Blocks = HORDE_MENTALITY_DROP_TIER4_BLOCKS.get();
        hordeMentalityHardnessScaling = HORDE_MENTALITY_HARDNESS_SCALING.get();
        hordeMentalityDamageLingerSeconds = HORDE_MENTALITY_DAMAGE_LINGER_SECONDS.get();
        hordeMentalityInstantBreak = HORDE_MENTALITY_INSTANT_BREAK.get();
        hordeMentalitySwingIntervalTicks = HORDE_MENTALITY_SWING_INTERVAL_TICKS.get();
        hordeMentalityHitChancePercent = HORDE_MENTALITY_HIT_CHANCE_PERCENT.get();
        hordeMentalityRequirePlayerTarget = HORDE_MENTALITY_REQUIRE_PLAYER_TARGET.get();
        hordeMentalityCheckPlayerProximity = HORDE_MENTALITY_CHECK_PLAYER_PROXIMITY.get();
        hordeMentalityPlayerProximityRadius = HORDE_MENTALITY_PLAYER_PROXIMITY_RADIUS.get();
        hordeMentalityRequireBlockInDirection = HORDE_MENTALITY_REQUIRE_BLOCK_IN_DIRECTION.get();
        hordeMentalityBabyMobsContribute = HORDE_MENTALITY_BABY_MOBS_CONTRIBUTE.get();
        hordeMentalityBabyMobsCanBreak = HORDE_MENTALITY_BABY_MOBS_CAN_BREAK.get();
        hordeMentalityDropBlockItems = HORDE_MENTALITY_DROP_BLOCK_ITEMS.get();
        hordeMentalityProtectSupportingBlocks = HORDE_MENTALITY_PROTECT_SUPPORTING_BLOCKS.get();
        hordeMentalityAllowDigDownToPlayer = HORDE_MENTALITY_ALLOW_DIG_DOWN_TO_PLAYER.get();
        hordeMentalityDaysBeforeActivation = HORDE_MENTALITY_DAYS_BEFORE_ACTIVATION.get();
        hordeMentalityShowBreakParticles = HORDE_MENTALITY_SHOW_BREAK_PARTICLES.get();
        hordeMentalityPlayBreakSound = HORDE_MENTALITY_PLAY_BREAK_SOUND.get();
        hordeMentalityPlayHitSound = HORDE_MENTALITY_PLAY_HIT_SOUND.get();
        hordeMentalityShowHitParticles = HORDE_MENTALITY_SHOW_HIT_PARTICLES.get();
        hordeMentalityTier1MinMobs = HORDE_MENTALITY_TIER1_MIN_MOBS.get();
        hordeMentalityTier1Blocks = HORDE_MENTALITY_TIER1_BLOCKS.get();
        hordeMentalityTier2MinMobs = HORDE_MENTALITY_TIER2_MIN_MOBS.get();
        hordeMentalityTier2Blocks = HORDE_MENTALITY_TIER2_BLOCKS.get();
        hordeMentalityTier3MinMobs = HORDE_MENTALITY_TIER3_MIN_MOBS.get();
        hordeMentalityTier3Blocks = HORDE_MENTALITY_TIER3_BLOCKS.get();
        hordeMentalityTier4MinMobs = HORDE_MENTALITY_TIER4_MIN_MOBS.get();
        hordeMentalityTier4Blocks = HORDE_MENTALITY_TIER4_BLOCKS.get();
        hordeMentalityBlacklistBlocks = HORDE_MENTALITY_BLACKLIST_BLOCKS.get();

        enableUniversalHostility = ENABLE_UNIVERSAL_HOSTILITY.get();
        enableHordeMentalityWhenChasingTargets = ENABLE_HORDE_MENTALITY_WHEN_CHASING_TARGETS.get();
        allowHordeMentalityDigDownForHostility = ENABLE_HORDE_MENTALITY_DIG_DOWN_FOR_HOSTILITY.get();
        universalHostilityDaysBeforeActivation = UNIVERSAL_HOSTILITY_DAYS_BEFORE_ACTIVATION.get();
        protectTamedAnimals = PROTECT_TAMED_ANIMALS.get();
        protectNamedEntities = PROTECT_NAMED_ENTITIES.get();
        hostileMobs = HOSTILE_MOBS.get();
        hostilityTargetMobs = HOSTILITY_TARGET_MOBS.get();

        enableHordeDetermination = ENABLE_HORDE_DETERMINATION.get();
        hordeDeterminationFollowDistance = HORDE_DETERMINATION_FOLLOW_DISTANCE.get();
        hordeDeterminationFollowTimeMinutes = HORDE_DETERMINATION_FOLLOW_TIME_MINUTES.get();
        hordeDeterminationDaysBeforeActivation = HORDE_DETERMINATION_DAYS_BEFORE_ACTIVATION.get();
        hordeDeterminationDistanceIncreaseOverTime = HORDE_DETERMINATION_DISTANCE_INCREASE_OVER_TIME.get();
        hordeDeterminationDistanceIncreaseIntervalDays = HORDE_DETERMINATION_DISTANCE_INCREASE_INTERVAL_DAYS.get();
        hordeDeterminationDistanceIncreaseAmount = HORDE_DETERMINATION_DISTANCE_INCREASE_AMOUNT.get();
        hordeDeterminationTimeIncreaseOverTime = HORDE_DETERMINATION_TIME_INCREASE_OVER_TIME.get();
        hordeDeterminationTimeIncreaseIntervalDays = HORDE_DETERMINATION_TIME_INCREASE_INTERVAL_DAYS.get();
        hordeDeterminationTimeIncreaseAmount = HORDE_DETERMINATION_TIME_INCREASE_AMOUNT.get();

        enableHordeWandering = ENABLE_HORDE_WANDERING.get();
        maxHordeGroup = MAX_HORDE_GROUP.get();
        hordeGroupMinimum = HORDE_GROUP_MINIMUM.get();
        hordeWanderingDirectionChangeMinutes = HORDE_WANDERING_DIRECTION_CHANGE_MINUTES.get();
        hordeWanderingDaysBeforeActivation = HORDE_WANDERING_DAYS_BEFORE_ACTIVATION.get();
        hordeWanderingAvoidWater = HORDE_WANDERING_AVOID_WATER.get();
        hordeWanderingAvoidFalls = HORDE_WANDERING_AVOID_FALLS.get();

        enableHordeSwimming = ENABLE_HORDE_SWIMMING.get();
        hordeMobCap = HORDE_MOB_CAP.get();
        creeperWallExplosionDaysBeforeActivation = CREEPER_WALL_EXPLOSION_DAYS_BEFORE_ACTIVATION.get();
        enablePassiveFear = ENABLE_PASSIVE_FEAR.get();
        enableNeutralFear = ENABLE_NEUTRAL_FEAR.get();
        enableHostileFear = ENABLE_HOSTILE_FEAR.get();

        enableCollectiveUnderstanding = ENABLE_COLLECTIVE_UNDERSTANDING.get();
        collectiveUnderstandingRange = COLLECTIVE_UNDERSTANDING_RANGE.get();
        collectiveUnderstandingDaysBeforeActivation = COLLECTIVE_UNDERSTANDING_DAYS_BEFORE_ACTIVATION.get();
        collectiveUnderstandingIncreaseOverTime = COLLECTIVE_UNDERSTANDING_INCREASE_OVER_TIME.get();
        collectiveUnderstandingIncreaseIntervalDays = COLLECTIVE_UNDERSTANDING_INCREASE_INTERVAL_DAYS.get();
        collectiveUnderstandingIncreaseAmount = COLLECTIVE_UNDERSTANDING_INCREASE_AMOUNT.get();

        enableHeightenedSense = ENABLE_HEIGHTENED_SENSE.get();
        heightenedSenseRange = HEIGHTENED_SENSE_RANGE.get();
        heightenedSenseDaysBeforeActivation = HEIGHTENED_SENSE_DAYS_BEFORE_ACTIVATION.get();
        heightenedSenseIncreaseOverTime = HEIGHTENED_SENSE_INCREASE_OVER_TIME.get();
        heightenedSenseIncreaseIntervalDays = HEIGHTENED_SENSE_INCREASE_INTERVAL_DAYS.get();
        heightenedSenseIncreaseAmount = HEIGHTENED_SENSE_INCREASE_AMOUNT.get();

        hordeSightRangeBonus = HORDE_SIGHT_RANGE_BONUS.get();
        hordeSightDaysBeforeActivation = HORDE_SIGHT_DAYS_BEFORE_ACTIVATION.get();
        hordeSightIncreaseOverTime = HORDE_SIGHT_INCREASE_OVER_TIME.get();
        hordeSightIncreaseIntervalDays = HORDE_SIGHT_INCREASE_INTERVAL_DAYS.get();
        hordeSightIncreaseAmount = HORDE_SIGHT_INCREASE_AMOUNT.get();

        if (GAME_STAGES_INSTALLED) {
            enableGameStages = ENABLE_GAME_STAGES.get();
            hordeDeterminationStage = GAME_STAGES_HORDE_DETERMINATION_STAGE.get();
            heightenedSenseStage = GAME_STAGES_HEIGHTENED_SENSE_STAGE.get();
            collectiveUnderstandingStage = GAME_STAGES_COLLECTIVE_UNDERSTANDING_STAGE.get();
            hordeMentalityStage = GAME_STAGES_HORDE_MENTALITY_STAGE.get();
            hordeMultiplyingStage = GAME_STAGES_HORDE_MULTIPLYING_STAGE.get();
            universalHostilityStage = GAME_STAGES_UNIVERSAL_HOSTILITY_STAGE.get();
        } else {
            enableGameStages = false;
            hordeDeterminationStage = "";
            heightenedSenseStage = "";
            collectiveUnderstandingStage = "";
            hordeMentalityStage = "";
            hordeMultiplyingStage = "";
            universalHostilityStage = "";
        }

        applyDifficultyPreset();

        ConfigCache.markDirty();
    }

    private static void applyDifficultyPreset() {
        if (difficultyPreset == null || !difficultyPreset.modifiesValues()) return;

        double range = difficultyPreset.rangeMultiplier;
        double days = difficultyPreset.daysMultiplier;
        double inc = difficultyPreset.increaseMultiplier;
        double dmg = difficultyPreset.damageMultiplier;

        hordeDeterminationFollowDistance = clampScale(hordeDeterminationFollowDistance, range, 1, 10000);
        if (hordeDeterminationFollowTimeMinutes > 0) {
            hordeDeterminationFollowTimeMinutes = clampScale(hordeDeterminationFollowTimeMinutes, range, 1, 1440);
        }
        heightenedSenseRange = clampScale(heightenedSenseRange, range, 1, 128);
        collectiveUnderstandingRange = clampScale(collectiveUnderstandingRange, range, 1, 128);
        hordeSightRangeBonus = clampScale(hordeSightRangeBonus, range, 0, 256);

        hordeMentalityDamageRatePerMob = clampScale(hordeMentalityDamageRatePerMob, dmg, 1, 100);

        hordeDeterminationDistanceIncreaseAmount = clampScale(hordeDeterminationDistanceIncreaseAmount, inc, 1, 10000);
        hordeDeterminationTimeIncreaseAmount = clampScale(hordeDeterminationTimeIncreaseAmount, inc, 1, 1440);
        heightenedSenseIncreaseAmount = clampScale(heightenedSenseIncreaseAmount, inc, 1, 128);
        collectiveUnderstandingIncreaseAmount = clampScale(collectiveUnderstandingIncreaseAmount, inc, 1, 128);
        hordeSightIncreaseAmount = clampScale(hordeSightIncreaseAmount, inc, 1, 256);

        featuresDaysBeforeActivation = clampScale(featuresDaysBeforeActivation, days, 0, 10000);
        hordeMentalityDaysBeforeActivation = clampScale(hordeMentalityDaysBeforeActivation, days, 0, 10000);
        universalHostilityDaysBeforeActivation = clampScale(universalHostilityDaysBeforeActivation, days, 0, 10000);
        creeperWallExplosionDaysBeforeActivation = clampScale(creeperWallExplosionDaysBeforeActivation, days, 0, 10000);
        hordeDeterminationDaysBeforeActivation = clampScale(hordeDeterminationDaysBeforeActivation, days, 0, 10000);
        hordeWanderingDaysBeforeActivation = clampScale(hordeWanderingDaysBeforeActivation, days, 0, 10000);
        collectiveUnderstandingDaysBeforeActivation = clampScale(collectiveUnderstandingDaysBeforeActivation, days, 0, 10000);
        heightenedSenseDaysBeforeActivation = clampScale(heightenedSenseDaysBeforeActivation, days, 0, 10000);
        hordeSightDaysBeforeActivation = clampScale(hordeSightDaysBeforeActivation, days, 0, 10000);
    }

    private static int clampScale(int base, double multiplier, int min, int max) {
        long scaled = Math.round(base * multiplier);
        if (scaled < min) scaled = min;
        if (scaled > max) scaled = max;
        return (int) scaled;
    }
}
