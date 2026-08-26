package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.compat.GameStagesCompat;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.util.BlockSupportUtil;
import com.enhancedhordes.tweaks.util.FeatureGate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.*;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class HordeMentalityHandler {

    private static final Random RANDOM = new Random();

    private static final int MAX_BLOCK_DAMAGE = 100_000_000;

    private static final Map<ResourceKey<Level>, LevelMentalityData> levelData = new HashMap<>();

    private static boolean suppressDrops = false;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        boolean active = EnhancedHordesTweaksConfig.enableHordeMentality
                && EnhancedHordesTweaksConfig.daysElapsedReached(
                        level, EnhancedHordesTweaksConfig.hordeMentalityDaysBeforeActivation)
                && !FeatureGate.nightBlocked(level)
                && GameStagesCompat.anyPlayerHasStage(level, EnhancedHordesTweaksConfig.hordeMentalityStage);
        if (!active) {
            LevelMentalityData existing = levelData.get(level.dimension());
            if (existing != null) clearAll(level, existing);
            return;
        }

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
                for (Map.Entry<BlockPos, Integer> e : data.breakerIds.entrySet()) {
                    level.destroyBlockProgress(e.getValue(), e.getKey(), -1);
                }
            }
        }
    }

    private static void processHordeMentality(ServerLevel level, LevelMentalityData data) {
        boolean nightBlocked = EnhancedHordesTweaksConfig.hordeMentalityNightOnly && level.isDay();

        if (!ConfigCache.hasHordeMobs()) return;

        long gameTime = level.getGameTime();
        int groupRadius = EnhancedHordesTweaksConfig.hordeMentalityGroupRadius;
        double groupRadiusSq = (double)(groupRadius * groupRadius);
        int swingInterval = EnhancedHordesTweaksConfig.hordeMentalitySwingIntervalTicks;
        long daysElapsed = gameTime / 24000L;
        long dayDamageBonus = (long) EnhancedHordesTweaksConfig.hordeMentalityDamageIncreasePerDay * daysElapsed;
        long dayDamageMultiplier = 1L + (long) EnhancedHordesTweaksConfig.hordeMentalityDamageMultiplierPerDay * daysElapsed;
        int[] tierMinMobs = {
                EnhancedHordesTweaksConfig.hordeMentalityTier1MinMobs,
                EnhancedHordesTweaksConfig.hordeMentalityTier2MinMobs,
                EnhancedHordesTweaksConfig.hordeMentalityTier3MinMobs,
                EnhancedHordesTweaksConfig.hordeMentalityTier4MinMobs,
        };

        List<Entity> hordeMobs = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity.isRemoved()) continue;
            if (ConfigCache.isHordeMob(entity.getType())) {
                hordeMobs.add(entity);
            }
        }

        int count = hordeMobs.size();
        Set<UUID> activeMobUUIDs = new HashSet<>();
        double[] px = new double[count];
        double[] py = new double[count];
        double[] pz = new double[count];
        boolean[] contributes = new boolean[count];
        for (int i = 0; i < count; i++) {
            Entity e = hordeMobs.get(i);
            activeMobUUIDs.add(e.getUUID());
            px[i] = e.getX();
            py[i] = e.getY();
            pz[i] = e.getZ();
            contributes[i] = EnhancedHordesTweaksConfig.hordeMentalityBabyMobsContribute
                    || !(e instanceof Mob m && m.isBaby());
        }

        Map<BlockPos, Integer> damageThisTick = new HashMap<>();
        Map<BlockPos, Integer> tierThisTick = new HashMap<>();

        for (Entity entity : hordeMobs) {
            if (!(entity instanceof Mob mob)) continue;
            if (nightBlocked) continue;

            if (!EnhancedHordesTweaksConfig.hordeMentalityBabyMobsCanBreak && mob.isBaby()) continue;

            LivingEntity currentTarget = mob.getTarget();
            boolean playerTarget = currentTarget instanceof Player;
            boolean hostilityChase = !playerTarget
                    && currentTarget != null
                    && EnhancedHordesTweaksConfig.enableUniversalHostility
                    && EnhancedHordesTweaksConfig.enableHordeMentalityWhenChasingTargets
                    && UniversalHostilityHandler.isHostilityTarget(currentTarget);

            if (EnhancedHordesTweaksConfig.hordeMentalityRequirePlayerTarget) {
                if (!playerTarget && !hostilityChase) continue;
            }

            Player nearestPlayer = null;
            if (EnhancedHordesTweaksConfig.hordeMentalityCheckPlayerProximity && !hostilityChase) {
                nearestPlayer = level.getNearestPlayer(mob,
                        EnhancedHordesTweaksConfig.hordeMentalityPlayerProximityRadius);
                if (nearestPlayer == null) continue;
            } else if (playerTarget) {
                nearestPlayer = (Player) currentTarget;
            }

            if (nearestPlayer == null && !hostilityChase
                    && EnhancedHordesTweaksConfig.hordeMentalityRequireBlockInDirection) {
                nearestPlayer = level.getNearestPlayer(mob, 64.0);
            }

            long lastSwing = data.mobLastSwingTick.getOrDefault(mob.getUUID(), -swingInterval - 1L);
            if (gameTime - lastSwing < swingInterval) continue;

            double mobX = mob.getX();
            double mobY = mob.getY();
            double mobZ = mob.getZ();
            int groupSize = 0;
            for (int i = 0; i < count; i++) {
                if (!contributes[i]) continue;
                double dx = px[i] - mobX;
                double dy = py[i] - mobY;
                double dz = pz[i] - mobZ;
                if (dx * dx + dy * dy + dz * dz <= groupRadiusSq) {
                    groupSize++;
                }
            }

            int qualifiedTier = 0;
            for (int t = tierMinMobs.length - 1; t >= 0; t--) {
                if (groupSize >= tierMinMobs[t]) {
                    qualifiedTier = t + 1;
                    break;
                }
            }
            if (qualifiedTier == 0) continue;

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
                        if (!state.isAir() && ConfigCache.isBreakableAtTier(state, qualifiedTier)
                                && !ConfigCache.isMentalityBlacklisted(state)
                                && !FeatureGate.lightBlocked(level, pos)
                                && !FeatureGate.graceBlocked(level, pos)) {
                            touchingBreakable.add(pos);
                        }
                    }
                }
            }

            if (touchingBreakable.isEmpty()) continue;

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

            if (EnhancedHordesTweaksConfig.hordeMentalityRequireBlockInDirection) {
                Vec3 lookDir = mob.getLookAngle();
                Vec3 mobOrigin = mob.position();
                touchingBreakable.removeIf(pos -> {
                    Vec3 mobToBlock = Vec3.atCenterOf(pos).subtract(mobOrigin).normalize();
                    return lookDir.dot(mobToBlock) < 0.3;
                });
            }

            if (touchingBreakable.isEmpty()) continue;

            data.mobLastSwingTick.put(mob.getUUID(), gameTime);

            if (RANDOM.nextInt(100) >= EnhancedHordesTweaksConfig.hordeMentalityHitChancePercent) {
                continue;
            }

            BlockPos target = touchingBreakable.get(RANDOM.nextInt(touchingBreakable.size()));

            if (EnhancedHordesTweaksConfig.hordeMentalityInstantBreak) {
                BlockState state = level.getBlockState(target);
                if (!state.isAir() && breakBlock(level, target, state, qualifiedTier)) {
                    data.blockDamage.remove(target);
                    data.lastDamagedTick.remove(target);
                    clearProgress(level, data, target);
                }
                continue;
            }

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
            long total = ((long) contribution + dayDamageBonus) * dayDamageMultiplier;
            contribution = (int) Math.min(MAX_BLOCK_DAMAGE, total);
            damageThisTick.merge(target, contribution, (a, b) -> (int) Math.min(MAX_BLOCK_DAMAGE, (long) a + b));
            tierThisTick.merge(target, qualifiedTier, Math::max);
        }

        for (Map.Entry<BlockPos, Integer> entry : damageThisTick.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) continue;

            int hp = Math.max(1, (int)(hardness * EnhancedHordesTweaksConfig.hordeMentalityHardnessScaling));
            int newDamage = (int) Math.min(MAX_BLOCK_DAMAGE, (long) data.blockDamage.getOrDefault(pos, 0) + entry.getValue());

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
                if (breakBlock(level, pos, state, tierThisTick.getOrDefault(pos, 4))) {
                    data.blockDamage.remove(pos);
                    data.lastDamagedTick.remove(pos);
                    clearProgress(level, data, pos);
                } else {
                    data.blockDamage.put(pos, hp);
                    data.lastDamagedTick.put(pos, gameTime);
                    showProgress(level, data, pos, 9);
                }
            } else {
                data.blockDamage.put(pos, newDamage);
                data.lastDamagedTick.put(pos, gameTime);
                int crackStage = (int)((newDamage / (float) hp) * 9);
                showProgress(level, data, pos, crackStage);
            }
        }

        Iterator<Map.Entry<BlockPos, Integer>> refreshIt = data.blockDamage.entrySet().iterator();
        while (refreshIt.hasNext()) {
            Map.Entry<BlockPos, Integer> e = refreshIt.next();
            BlockPos pos = e.getKey();
            if (damageThisTick.containsKey(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                clearProgress(level, data, pos);
                data.lastDamagedTick.remove(pos);
                refreshIt.remove();
                continue;
            }
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) continue;
            int hp = Math.max(1, (int)(hardness * EnhancedHordesTweaksConfig.hordeMentalityHardnessScaling));
            int crackStage = (int)((e.getValue() / (float) hp) * 9);
            showProgress(level, data, pos, crackStage);
        }

        long lingerTicks = EnhancedHordesTweaksConfig.hordeMentalityDamageLingerSeconds * 20L;
        data.blockDamage.entrySet().removeIf(e -> {
            BlockPos pos = e.getKey();
            if (damageThisTick.containsKey(pos)) return false;
            long lastTick = data.lastDamagedTick.getOrDefault(pos, 0L);
            if (gameTime - lastTick >= lingerTicks) {
                clearProgress(level, data, pos);
                data.lastDamagedTick.remove(pos);
                return true;
            }
            return false;
        });

        data.mobLastSwingTick.keySet().removeIf(uuid -> !activeMobUUIDs.contains(uuid));
    }

    private static boolean breakBlock(ServerLevel level, BlockPos pos, BlockState state, int qualifiedTier) {
        if (EnhancedHordesTweaksConfig.hordeMentalityProtectSupportingBlocks
                && BlockSupportUtil.wouldOrphanNeighbor(level, pos, state)) {
            return false;
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

        if (state.hasBlockEntity()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) {
                Containers.dropContents(level, pos, container);
            }
        }

        boolean dropAllowed = (switch (qualifiedTier) {
            case 1 -> EnhancedHordesTweaksConfig.hordeMentalityDropTier1Blocks;
            case 2 -> EnhancedHordesTweaksConfig.hordeMentalityDropTier2Blocks;
            case 3 -> EnhancedHordesTweaksConfig.hordeMentalityDropTier3Blocks;
            default -> EnhancedHordesTweaksConfig.hordeMentalityDropTier4Blocks;
        }) && EnhancedHordesTweaksConfig.hordeMentalityDropBlockItems
                && !EnhancedHordesTweaksConfig.enableBlockRegeneration;

        if (dropAllowed) {
            Block.dropResources(state, level, pos, null);
        }

        BlockPos partnerPos = BlockSupportUtil.getDoubleBlockPartner(state, pos);
        BlockState partnerState = partnerPos != null ? level.getBlockState(partnerPos) : null;

        BlockRegenerationHandler.scheduleRegen(level, pos, state);
        if (partnerState != null && !partnerState.isAir()) {
            BlockRegenerationHandler.scheduleRegen(level, partnerPos, partnerState);
        }

        int removeFlags = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
        suppressDrops = true;
        try {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), removeFlags);
            if (partnerPos != null && !level.getBlockState(partnerPos).isAir()) {
                level.setBlock(partnerPos, Blocks.AIR.defaultBlockState(), removeFlags);
            }
        } finally {
            suppressDrops = false;
        }
        return true;
    }

    private static LevelMentalityData getOrCreate(ResourceKey<Level> dim) {
        return levelData.computeIfAbsent(dim, k -> new LevelMentalityData());
    }

    private static void clearAll(ServerLevel level, LevelMentalityData data) {
        for (Map.Entry<BlockPos, Integer> e : data.breakerIds.entrySet()) {
            level.destroyBlockProgress(e.getValue(), e.getKey(), -1);
        }
        levelData.remove(level.dimension());
    }

    private static int breakerId(LevelMentalityData data, BlockPos pos) {
        Integer id = data.breakerIds.get(pos);
        if (id == null) {
            id = data.nextBreakerId++;
            if (data.nextBreakerId == Integer.MAX_VALUE) data.nextBreakerId = 1;
            data.breakerIds.put(pos.immutable(), id);
        }
        return id;
    }

    private static void showProgress(ServerLevel level, LevelMentalityData data, BlockPos pos, int stage) {
        Integer shown = data.shownStage.get(pos);
        if (shown != null && shown == stage) return;
        int id = breakerId(data, pos);
        data.shownStage.put(pos.immutable(), stage);
        level.destroyBlockProgress(id, pos, stage);
    }

    private static void clearProgress(ServerLevel level, LevelMentalityData data, BlockPos pos) {
        Integer id = data.breakerIds.remove(pos);
        data.shownStage.remove(pos);
        if (id == null) return;
        level.destroyBlockProgress(id, pos, -1);
    }

    private static class LevelMentalityData {
        final Map<BlockPos, Integer> blockDamage = new HashMap<>();
        final Map<BlockPos, Long> lastDamagedTick = new HashMap<>();
        final Map<UUID, Long> mobLastSwingTick = new HashMap<>();
        final Map<BlockPos, Integer> breakerIds = new HashMap<>();
        final Map<BlockPos, Integer> shownStage = new HashMap<>();
        int nextBreakerId = 1;
    }
}
