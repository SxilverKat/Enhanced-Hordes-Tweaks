package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.util.BlockSupportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HordeMentalityHandler {

    private static final Random RANDOM = new Random();

    private static final Map<ResourceKey<Level>, LevelMentalityData> levelData = new HashMap<>();

    // Set true during level.removeBlock() calls so we can intercept any item entities
    // spawned by partner-half removal (doors, beds, tall plants) and suppress them,
    // keeping item drops fully under hordeMentalityDropBlockItems's control.
    private static boolean suppressDrops = false;

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!EnhancedHordesTweaksConfig.enableHordeMentality) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                level, EnhancedHordesTweaksConfig.hordeMentalityDaysBeforeActivation)) return;

        processHordeMentality(level, getOrCreate(level.dimension()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (suppressDrops && event.getEntity() instanceof ItemEntity) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LevelMentalityData data = levelData.remove(level.dimension());
            if (data != null) {
                for (BlockPos pos : data.blockDamage.keySet()) {
                    level.destroyBlockProgress(pos.hashCode(), pos, -1);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Core logic
    // -----------------------------------------------------------------------

    private static void processHordeMentality(ServerLevel level, LevelMentalityData data) {
        if (EnhancedHordesTweaksConfig.hordeMentalityNightOnly && level.isDay()) return;

        List<? extends String> hordeMobIds = EnhancedHordesTweaksConfig.hordeMobs;
        if (hordeMobIds == null || hordeMobIds.isEmpty()) return;

        long gameTime = level.getGameTime();
        int groupRadius = EnhancedHordesTweaksConfig.hordeMentalityGroupRadius;
        TierData[] tiers = buildTiers();
        int swingInterval = EnhancedHordesTweaksConfig.hordeMentalitySwingIntervalTicks;

        // Collect all active horde mob entities
        List<Entity> hordeMobs = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity.isRemoved()) continue;
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityId != null && hordeMobIds.contains(entityId.toString())) {
                hordeMobs.add(entity);
            }
        }

        Set<UUID> activeMobUUIDs = new HashSet<>();
        for (Entity mob : hordeMobs) activeMobUUIDs.add(mob.getUUID());

        // Damage accumulated this tick per block position
        Map<BlockPos, Integer> damageThisTick = new HashMap<>();
        // Highest qualified tier that contributed damage to each block this tick
        Map<BlockPos, Integer> tierThisTick = new HashMap<>();

        for (Entity entity : hordeMobs) {
            if (!(entity instanceof Mob mob)) continue;

            // Skip baby mobs from swinging if configured
            if (!EnhancedHordesTweaksConfig.hordeMentalityBabyMobsCanBreak && mob.isBaby()) continue;

            // Target classification: player target is always valid; a hostility target
            // (cow, villager, etc.) only counts when the universal-hostility integration
            // is on. When chasing a hostility target the player-presence gates below
            // are bypassed, because the whole point of this path is to let block
            // breaking fire without a player nearby.
            LivingEntity currentTarget = mob.getTarget();
            boolean playerTarget = currentTarget instanceof Player;
            boolean hostilityChase = !playerTarget
                    && currentTarget != null
                    && EnhancedHordesTweaksConfig.enableUniversalHostility
                    && EnhancedHordesTweaksConfig.enableHordeMentalityWhenChasingTargets
                    && UniversalHostilityHandler.isHostilityTarget(currentTarget);

            // Require pathfinding toward a player (or hostility target) if configured
            if (EnhancedHordesTweaksConfig.hordeMentalityRequirePlayerTarget) {
                if (!playerTarget && !hostilityChase) continue;
            }

            // Player proximity check — find nearest player within radius.
            // When chasing a hostility target we bypass this: no player is expected.
            Player nearestPlayer = null;
            if (EnhancedHordesTweaksConfig.hordeMentalityCheckPlayerProximity && !hostilityChase) {
                nearestPlayer = level.getNearestPlayer(mob,
                        EnhancedHordesTweaksConfig.hordeMentalityPlayerProximityRadius);
                if (nearestPlayer == null) continue; // no player in range
            } else if (playerTarget) {
                nearestPlayer = (Player) currentTarget;
            }

            // Fallback: if the direction check is enabled but we still have no player
            // reference (neither proximity nor target checks resolved one), find any
            // nearby player so requireBlockInDirection is not silently skipped.
            if (nearestPlayer == null && !hostilityChase
                    && EnhancedHordesTweaksConfig.hordeMentalityRequireBlockInDirection) {
                nearestPlayer = level.getNearestPlayer(mob, 64.0);
            }

            // Swing cooldown — each mob swings at most once per swingInterval ticks
            long lastSwing = data.mobLastSwingTick.getOrDefault(mob.getUUID(), -swingInterval - 1L);
            if (gameTime - lastSwing < swingInterval) continue;

            // Count group size (optionally excluding baby mobs)
            BlockPos mobPos = mob.blockPosition();
            int groupSize = 0;
            for (Entity other : hordeMobs) {
                if (!EnhancedHordesTweaksConfig.hordeMentalityBabyMobsContribute
                        && other instanceof Mob otherMob && otherMob.isBaby()) continue;
                if (mobPos.distSqr(other.blockPosition()) <= (double)(groupRadius * groupRadius)) {
                    groupSize++;
                }
            }

            // Find the highest tier this group qualifies for
            int qualifiedTier = 0;
            for (int t = tiers.length - 1; t >= 0; t--) {
                if (groupSize >= tiers[t].minMobs()) {
                    qualifiedTier = t + 1;
                    break;
                }
            }
            if (qualifiedTier == 0) continue;

            // Build the breakable set for all qualifying tiers
            Set<String> breakableIds = new HashSet<>();
            Set<TagKey<Block>> breakableTags = new HashSet<>();
            for (int t = 0; t < qualifiedTier; t++) {
                collectTierBlocks(tiers[t], breakableIds, breakableTags);
            }

            // Find all breakable blocks the mob's hitbox is currently touching
            List<BlockPos> touchingBreakable = new ArrayList<>();
            AABB mobBox = mob.getBoundingBox().inflate(0.1);
            int minX = Mth.floor(mobBox.minX);
            int maxX = Mth.floor(mobBox.maxX);
            int minY = Mth.floor(mobBox.minY);
            int maxY = Mth.floor(mobBox.maxY);
            int minZ = Mth.floor(mobBox.minZ);
            int maxZ = Mth.floor(mobBox.maxZ);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z).immutable();
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && isBreakable(state, breakableIds, breakableTags)
                                && !isBlacklisted(state)) {
                            touchingBreakable.add(pos);
                        }
                    }
                }
            }

            if (touchingBreakable.isEmpty()) continue;

            // Exclude blocks strictly below the mob's feet — prevents floor digging by
            // default (mobs would otherwise dig holes instead of breaking walls).
            // Exception: when the target is genuinely below the mob AND the relevant
            // dig-down toggle is on, allow downward digging so mobs can tunnel toward
            // the target. The 1-block margin ignores minor elevation differences like
            // stairs or slabs.
            final int mobFeetY = mob.blockPosition().getY();
            boolean targetBelow = false;
            if (playerTarget
                    && EnhancedHordesTweaksConfig.hordeMentalityAllowDigDownToPlayer
                    && nearestPlayer != null
                    && nearestPlayer.getY() < mob.getY() - 1.0) {
                targetBelow = true;
            } else if (hostilityChase
                    && EnhancedHordesTweaksConfig.allowHordeMentalityDigDownForHostility
                    && currentTarget != null
                    && currentTarget.getY() < mob.getY() - 1.0) {
                targetBelow = true;
            }
            if (!targetBelow) {
                touchingBreakable.removeIf(pos -> pos.getY() < mobFeetY);
            }

            // Direction filter — keep only blocks the mob is actually facing.
            // Using getLookAngle() instead of mob-to-player vector prevents mobs from
            // targeting floor/ceiling blocks when the player is at a different height.
            if (EnhancedHordesTweaksConfig.hordeMentalityRequireBlockInDirection) {
                Vec3 lookDir = mob.getLookAngle();
                Vec3 mobOrigin = mob.position();
                touchingBreakable.removeIf(pos -> {
                    Vec3 mobToBlock = Vec3.atCenterOf(pos).subtract(mobOrigin).normalize();
                    return lookDir.dot(mobToBlock) < 0.3;
                });
            }

            if (touchingBreakable.isEmpty()) continue;

            // Record the swing attempt (counts even on a miss to enforce rhythm)
            data.mobLastSwingTick.put(mob.getUUID(), gameTime);

            // Roll hit chance
            if (RANDOM.nextInt(100) >= EnhancedHordesTweaksConfig.hordeMentalityHitChancePercent) {
                continue; // miss
            }

            // Pick ONE random block from the candidates this mob is touching
            BlockPos target = touchingBreakable.get(RANDOM.nextInt(touchingBreakable.size()));

            // ----- Instant break mode -----
            if (EnhancedHordesTweaksConfig.hordeMentalityInstantBreak) {
                BlockState state = level.getBlockState(target);
                if (!state.isAir()) {
                    data.blockDamage.remove(target);
                    data.lastDamagedTick.remove(target);
                    level.destroyBlockProgress(target.hashCode(), target, -1);
                    breakBlock(level, target, state, qualifiedTier);
                }
                continue;
            }

            // ----- Progressive damage mode -----
            int contribution = EnhancedHordesTweaksConfig.hordeMentalityDamageRatePerMob;
            if (EnhancedHordesTweaksConfig.hordeMentalityScaleWithDamage) {
                double attackDamage = mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
                contribution = Math.max(1, (int)(attackDamage * contribution));
            }
            if (EnhancedHordesTweaksConfig.hordeMentalityTierDamageScaling) {
                contribution *= qualifiedTier;
            }
            if (EnhancedHordesTweaksConfig.hordeMentalityProgressiveScaling
                    && qualifiedTier == 4
                    && groupSize > EnhancedHordesTweaksConfig.hordeMentalityTier4MinMobs) {
                int overage = groupSize - EnhancedHordesTweaksConfig.hordeMentalityTier4MinMobs;
                double multiplier = 1.0 + overage * (EnhancedHordesTweaksConfig.hordeMentalityProgressiveScalingBonusPerMob / 100.0);
                contribution = (int) Math.ceil(contribution * multiplier);
            }
            damageThisTick.merge(target, contribution, Integer::sum);
            tierThisTick.merge(target, qualifiedTier, Math::max);
        }

        // Apply accumulated progressive damage — effects fire once per block per swing cycle
        for (Map.Entry<BlockPos, Integer> entry : damageThisTick.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) continue; // indestructible

            int hp = Math.max(1, (int)(hardness * EnhancedHordesTweaksConfig.hordeMentalityHardnessScaling));
            int newDamage = data.blockDamage.getOrDefault(pos, 0) + entry.getValue();

            if (EnhancedHordesTweaksConfig.hordeMentalityPlayHitSound) {
                var soundType = state.getSoundType();
                level.playSound(null, pos, soundType.getHitSound(), SoundSource.BLOCKS,
                        soundType.getVolume() * 0.3f, soundType.getPitch() * 0.8f);
            }
            if (EnhancedHordesTweaksConfig.hordeMentalityShowHitParticles) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        3, 0.3, 0.3, 0.3, 0.05);
            }

            if (newDamage >= hp) {
                data.blockDamage.remove(pos);
                data.lastDamagedTick.remove(pos);
                level.destroyBlockProgress(pos.hashCode(), pos, -1);
                breakBlock(level, pos, state, tierThisTick.getOrDefault(pos, 4));
            } else {
                data.blockDamage.put(pos, newDamage);
                data.lastDamagedTick.put(pos, gameTime);
                int crackStage = (int)((newDamage / (float) hp) * 9);
                level.destroyBlockProgress(pos.hashCode(), pos, crackStage);
            }
        }

        // Refresh crack visuals for lingering blocks not hit this tick.
        // Without this, Minecraft's client-side crack animation times out (~400 ticks)
        // long before the configured linger window expires.
        for (Map.Entry<BlockPos, Integer> e : data.blockDamage.entrySet()) {
            BlockPos pos = e.getKey();
            if (damageThisTick.containsKey(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) continue;
            int hp = Math.max(1, (int)(hardness * EnhancedHordesTweaksConfig.hordeMentalityHardnessScaling));
            int crackStage = (int)((e.getValue() / (float) hp) * 9);
            level.destroyBlockProgress(pos.hashCode(), pos, crackStage);
        }

        // Decay block damage that hasn't been hit within the linger window.
        // NOTE: This always runs even when no mobs are found, so cracks are never
        // cleared prematurely (e.g. when mobs briefly despawn or unload).
        long lingerTicks = EnhancedHordesTweaksConfig.hordeMentalityDamageLingerSeconds * 20L;
        data.blockDamage.entrySet().removeIf(e -> {
            BlockPos pos = e.getKey();
            if (damageThisTick.containsKey(pos)) return false;
            long lastTick = data.lastDamagedTick.getOrDefault(pos, 0L);
            if (gameTime - lastTick >= lingerTicks) {
                level.destroyBlockProgress(pos.hashCode(), pos, -1);
                data.lastDamagedTick.remove(pos);
                return true;
            }
            return false;
        });

        // Clean up swing timers for mobs no longer present
        data.mobLastSwingTick.keySet().removeIf(uuid -> !activeMobUUIDs.contains(uuid));
    }

    // -----------------------------------------------------------------------
    // Block breaking
    // -----------------------------------------------------------------------

    private static void breakBlock(ServerLevel level, BlockPos pos, BlockState state, int qualifiedTier) {
        if (EnhancedHordesTweaksConfig.hordeMentalityProtectSupportingBlocks
                && BlockSupportUtil.wouldOrphanNeighbor(level, pos, state)) {
            return;
        }
        if (EnhancedHordesTweaksConfig.hordeMentalityShowBreakParticles
                && EnhancedHordesTweaksConfig.hordeMentalityPlayBreakSound) {
            level.levelEvent(2001, pos, Block.getId(state));
        } else if (EnhancedHordesTweaksConfig.hordeMentalityPlayBreakSound) {
            var soundType = state.getSoundType();
            level.playSound(null, pos, soundType.getBreakSound(), SoundSource.BLOCKS,
                    soundType.getVolume(), soundType.getPitch());
        } else if (EnhancedHordesTweaksConfig.hordeMentalityShowBreakParticles) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    12, 0.3, 0.3, 0.3, 0.1);
        }

        // Determine whether drops are allowed for this tier
        boolean dropAllowed = switch (qualifiedTier) {
            case 1 -> EnhancedHordesTweaksConfig.hordeMentalityDropTier1Blocks;
            case 2 -> EnhancedHordesTweaksConfig.hordeMentalityDropTier2Blocks;
            case 3 -> EnhancedHordesTweaksConfig.hordeMentalityDropTier3Blocks;
            default -> EnhancedHordesTweaksConfig.hordeMentalityDropTier4Blocks;
        } && EnhancedHordesTweaksConfig.hordeMentalityDropBlockItems;

        // Explicit drop for the broken block — only when configured.
        if (dropAllowed) {
            Block.dropResources(state, level, pos, null);
        }

        // Resolve the partner half of any two-block object (door, bed, tall plant)
        BlockPos partnerPos = BlockSupportUtil.getDoubleBlockPartner(state, pos);
        BlockState partnerState = partnerPos != null ? level.getBlockState(partnerPos) : null;

        // Register with the regen system before removing the block so the pre-break
        // state is captured. The snapshot diff system won't detect horde mentality
        // breaks because tier blocks aren't in hordeBreakableBlocks.
        BlockRegenerationHandler.scheduleRegen(level, pos, state);
        if (partnerState != null && !partnerState.isAir()) {
            BlockRegenerationHandler.scheduleRegen(level, partnerPos, partnerState);
        }

        // Use UPDATE_SUPPRESS_DROPS (flag 32) so the vanilla update chain does not
        // drop items when the partner's updateShape resolves to AIR: Block.updateOrDestroy
        // would otherwise call level.destroyBlock(partnerPos, true) which drops a full
        // copy of the item (doors, beds, tall plants). The event-level suppressDrops
        // ThreadLocal stays as a belt-and-suspenders guard against any drops that
        // slip past the flag (e.g. modded blocks with custom removal paths).
        int removeFlags = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
        suppressDrops = true;
        try {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), removeFlags);
            // Explicitly remove the partner half in case vanilla's updateShape didn't
            // already resolve it (modded two-block objects that don't self-destruct).
            if (partnerPos != null && !level.getBlockState(partnerPos).isAir()) {
                level.setBlock(partnerPos, Blocks.AIR.defaultBlockState(), removeFlags);
            }
        } finally {
            suppressDrops = false;
        }
    }

    // -----------------------------------------------------------------------
    // Tier helpers
    // -----------------------------------------------------------------------

    private record TierData(int minMobs, List<? extends String> blocks) {}

    private static TierData[] buildTiers() {
        return new TierData[] {
                new TierData(EnhancedHordesTweaksConfig.hordeMentalityTier1MinMobs,
                             EnhancedHordesTweaksConfig.hordeMentalityTier1Blocks),
                new TierData(EnhancedHordesTweaksConfig.hordeMentalityTier2MinMobs,
                             EnhancedHordesTweaksConfig.hordeMentalityTier2Blocks),
                new TierData(EnhancedHordesTweaksConfig.hordeMentalityTier3MinMobs,
                             EnhancedHordesTweaksConfig.hordeMentalityTier3Blocks),
                new TierData(EnhancedHordesTweaksConfig.hordeMentalityTier4MinMobs,
                             EnhancedHordesTweaksConfig.hordeMentalityTier4Blocks),
        };
    }

    private static void collectTierBlocks(TierData tier, Set<String> ids, Set<TagKey<Block>> tags) {
        if (tier.blocks() == null) return;
        for (String entry : tier.blocks()) {
            if (entry.startsWith("#")) {
                tags.add(TagKey.create(Registries.BLOCK, new ResourceLocation(entry.substring(1))));
            } else {
                ids.add(entry);
            }
        }
    }

    private static boolean isBreakable(BlockState state, Set<String> ids, Set<TagKey<Block>> tags) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId != null && ids.contains(blockId.toString())) return true;
        for (TagKey<Block> tag : tags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    private static boolean isBlacklisted(BlockState state) {
        List<? extends String> blacklist = EnhancedHordesTweaksConfig.hordeMentalityBlacklistBlocks;
        if (blacklist == null || blacklist.isEmpty()) return false;
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId != null && blacklist.contains(blockId.toString())) return true;
        for (String entry : blacklist) {
            if (entry.startsWith("#")) {
                TagKey<Block> tag = TagKey.create(Registries.BLOCK, new ResourceLocation(entry.substring(1)));
                if (state.is(tag)) return true;
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static LevelMentalityData getOrCreate(ResourceKey<Level> dim) {
        return levelData.computeIfAbsent(dim, k -> new LevelMentalityData());
    }

    // -----------------------------------------------------------------------
    // Data classes
    // -----------------------------------------------------------------------

    private static class LevelMentalityData {
        final Map<BlockPos, Integer> blockDamage = new HashMap<>();
        final Map<BlockPos, Long> lastDamagedTick = new HashMap<>();
        final Map<UUID, Long> mobLastSwingTick = new HashMap<>();
    }
}
