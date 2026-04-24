package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlockRegenerationHandler {

    private static final Random RANDOM = new Random();

    // How often (in ticks) to scan for mob-broken blocks
    private static final int SCAN_INTERVAL = 20;
    // Block radius around each horde mob to watch for breakable blocks
    private static final int SCAN_RADIUS = 4;

    // Per-level state: regen queues, player-break exclusions, block snapshots
    private static final Map<ResourceKey<Level>, LevelRegenData> levelData = new HashMap<>();

    // -----------------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!EnhancedHordesTweaksConfig.enableBlockRegeneration) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos().immutable();
        LevelRegenData data = getOrCreate(level.dimension());
        data.regenQueue.remove(pos);
        data.waitingForClear.remove(pos);
        data.pausedRegen.remove(pos);
        data.playerBroken.add(pos);

        if (data.playerBroken.size() > 500) {
            data.playerBroken.clear();
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!EnhancedHordesTweaksConfig.enableBlockRegeneration) return;
        if (!EnhancedHordesTweaksConfig.cancelRegenOnPlayerPlace) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos placePos = event.getPos().immutable();
        int radius = EnhancedHordesTweaksConfig.playerPlaceCancelRadius;
        double radiusSq = (double) radius * radius;
        LevelRegenData data = getOrCreate(level.dimension());

        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos pos : data.regenQueue.keySet()) {
            if (placePos.distSqr(pos) <= radiusSq) toRemove.add(pos);
        }
        for (BlockPos pos : data.waitingForClear.keySet()) {
            if (placePos.distSqr(pos) <= radiusSq) toRemove.add(pos);
        }
        for (BlockPos pos : data.pausedRegen.keySet()) {
            if (placePos.distSqr(pos) <= radiusSq) toRemove.add(pos);
        }
        for (BlockPos pos : toRemove) {
            data.regenQueue.remove(pos);
            data.waitingForClear.remove(pos);
            data.pausedRegen.remove(pos);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!EnhancedHordesTweaksConfig.enableBlockRegeneration) return;

        LevelRegenData data = getOrCreate(level.dimension());
        data.ticksSinceLastScan++;

        if (data.ticksSinceLastScan >= SCAN_INTERVAL) {
            data.ticksSinceLastScan = 0;
            scanForBreaks(level, data);
            processWaitingForClear(level, data);
            processPausedRegen(level, data);
        }

        processRegenQueue(level, data);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            levelData.remove(level.dimension());
        }
    }

    // -----------------------------------------------------------------------
    // Direct registration — called by HordeMentalityHandler when it breaks a block
    // -----------------------------------------------------------------------

    public static void scheduleRegen(ServerLevel level, BlockPos pos, BlockState state) {
        if (!EnhancedHordesTweaksConfig.enableBlockRegeneration) return;
        LevelRegenData data = getOrCreate(level.dimension());

        if (data.playerBroken.contains(pos)) return;
        if (data.regenQueue.containsKey(pos) || data.waitingForClear.containsKey(pos)) return;

        enqueue(level, data, pos, state);
        scheduleCompanion(level, data, pos, state);
    }

    // -----------------------------------------------------------------------
    // Break detection (snapshot diff — for EH's own block breaking)
    // -----------------------------------------------------------------------

    private static void scanForBreaks(ServerLevel level, LevelRegenData data) {
        Set<String> breakableIds = buildBreakableIds();
        Set<TagKey<Block>> breakableTags = buildBreakableTags();

        for (Map.Entry<BlockPos, BlockState> entry : data.prevSnapshot.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState prevState = entry.getValue();
            BlockState currentState = level.getBlockState(pos);

            if (currentState.isAir() && !prevState.isAir()) {
                if (data.playerBroken.remove(pos)) {
                    // Player broke this — don't regenerate
                } else if (!data.regenQueue.containsKey(pos) && !data.waitingForClear.containsKey(pos)) {
                    enqueue(level, data, pos, prevState);
                    scheduleCompanion(level, data, pos, prevState);
                }
            }
        }

        List<? extends String> hordeMobIds = EnhancedHordesTweaksConfig.hordeMobs;
        Map<BlockPos, BlockState> newSnapshot = new HashMap<>();

        if (hordeMobIds != null) {
            for (Entity entity : level.getAllEntities()) {
                if (entity.isRemoved()) continue;
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                if (entityId == null || !hordeMobIds.contains(entityId.toString())) continue;

                BlockPos mobPos = entity.blockPosition();
                for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                            BlockPos pos = mobPos.offset(dx, dy, dz).immutable();
                            if (newSnapshot.containsKey(pos)) continue;
                            BlockState state = level.getBlockState(pos);
                            if (isBreakableBlock(state, breakableIds, breakableTags)) {
                                newSnapshot.put(pos, state);
                            }
                        }
                    }
                }
            }
        }

        data.prevSnapshot = newSnapshot;
    }

    // -----------------------------------------------------------------------
    // Double-block companion handling
    // -----------------------------------------------------------------------

    private static boolean isUpperHalf(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)) {
            return state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
        }
        return false;
    }

    @Nullable
    private static BlockPos getCompanionPos(BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (block instanceof DoorBlock) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            return half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        }
        if (block instanceof BedBlock) {
            BedPart part = state.getValue(BlockStateProperties.BED_PART);
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return part == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
        }
        if (block instanceof DoublePlantBlock) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            return half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        }
        return null;
    }

    @Nullable
    private static BlockState reconstructCompanionState(BlockState brokenState) {
        Block block = brokenState.getBlock();
        if (block instanceof DoorBlock) {
            DoubleBlockHalf half = brokenState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            return brokenState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                    half == DoubleBlockHalf.LOWER ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
        }
        if (block instanceof BedBlock) {
            BedPart part = brokenState.getValue(BlockStateProperties.BED_PART);
            return brokenState
                    .setValue(BlockStateProperties.BED_PART, part == BedPart.FOOT ? BedPart.HEAD : BedPart.FOOT)
                    .setValue(BlockStateProperties.OCCUPIED, false);
        }
        if (block instanceof DoublePlantBlock) {
            DoubleBlockHalf half = brokenState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            return brokenState.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                    half == DoubleBlockHalf.LOWER ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
        }
        return null;
    }

    private static void scheduleCompanion(ServerLevel level, LevelRegenData data,
                                          BlockPos pos, BlockState state) {
        BlockPos companionPos = getCompanionPos(state, pos);
        if (companionPos == null) return;
        companionPos = companionPos.immutable();

        if (data.playerBroken.contains(companionPos)) return;
        if (data.regenQueue.containsKey(companionPos) || data.waitingForClear.containsKey(companionPos)) return;

        BlockState companionState = level.getBlockState(companionPos);
        if (companionState.isAir()) {
            companionState = reconstructCompanionState(state);
        }
        if (companionState != null && !companionState.isAir()) {
            enqueue(level, data, companionPos, companionState);
        }
    }

    /**
     * Cancels the companion half of a double-block from all queues.
     * Called when the primary half's spot was filled externally (e.g. player placed a block).
     */
    private static void cancelCompanionFromAllQueues(LevelRegenData data, BlockPos companionPos) {
        data.regenQueue.remove(companionPos);
        data.waitingForClear.remove(companionPos);
        data.pausedRegen.remove(companionPos);
    }

    /**
     * Immediately places the companion half of a double-block by pulling it from whichever
     * queue it currently lives in. Called right after the primary half is placed so both
     * halves appear in the same tick.
     */
    private static void placeCompanionFromQueues(ServerLevel level, LevelRegenData data,
                                                 BlockPos companionPos) {
        if (!level.getBlockState(companionPos).isAir()) return;

        PendingRegen fromRegen = data.regenQueue.remove(companionPos);
        if (fromRegen != null) {
            placeBlock(level, data, companionPos, fromRegen.blockState());
            return;
        }
        BlockState fromWaiting = data.waitingForClear.remove(companionPos);
        if (fromWaiting != null) {
            placeBlock(level, data, companionPos, fromWaiting);
            return;
        }
        PausedRegen fromPaused = data.pausedRegen.remove(companionPos);
        if (fromPaused != null) {
            placeBlock(level, data, companionPos, fromPaused.blockState());
        }
    }

    // -----------------------------------------------------------------------
    // Queue helpers
    // -----------------------------------------------------------------------

    private static void enqueue(ServerLevel level, LevelRegenData data, BlockPos pos, BlockState state) {
        if (EnhancedHordesTweaksConfig.requireMobsClearedBeforeRegen) {
            data.waitingForClear.put(pos, state);
        } else {
            long regenTime = level.getGameTime() + computeRegenDelay(level, pos, state);
            data.regenQueue.put(pos, new PendingRegen(state, regenTime));
        }
    }

    private static long computeRegenDelay(ServerLevel level, BlockPos pos, BlockState state) {
        long baseDelay = EnhancedHordesTweaksConfig.regenerationDelaySeconds * 20L;
        if (!EnhancedHordesTweaksConfig.regenScaleDelayByHardness) return baseDelay;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) return baseDelay; // indestructible — use base
        float multiplier = Math.max(0.1f, Math.min(5.0f, hardness / 1.5f));
        return (long)(baseDelay * multiplier);
    }

    private static Set<String> buildBreakableIds() {
        Set<String> ids = new HashSet<>();
        for (String entry : EnhancedHordesTweaksConfig.hordeBreakableBlocks) {
            if (!entry.startsWith("#")) ids.add(entry);
        }
        return ids;
    }

    private static Set<TagKey<Block>> buildBreakableTags() {
        Set<TagKey<Block>> tags = new HashSet<>();
        for (String entry : EnhancedHordesTweaksConfig.hordeBreakableBlocks) {
            if (entry.startsWith("#")) {
                tags.add(TagKey.create(Registries.BLOCK, new ResourceLocation(entry.substring(1))));
            }
        }
        return tags;
    }

    private static boolean isBreakableBlock(BlockState state, Set<String> ids, Set<TagKey<Block>> tags) {
        if (state.isAir()) return false;
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId != null && ids.contains(blockId.toString())) return true;
        for (TagKey<Block> tag : tags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Shared mob position helpers
    // -----------------------------------------------------------------------

    private static List<BlockPos> collectHordeMobPositions(ServerLevel level) {
        List<? extends String> hordeMobIds = EnhancedHordesTweaksConfig.hordeMobs;
        List<BlockPos> positions = new ArrayList<>();
        if (hordeMobIds != null) {
            for (Entity entity : level.getAllEntities()) {
                if (entity.isRemoved()) continue;
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                if (entityId != null && hordeMobIds.contains(entityId.toString())) {
                    positions.add(entity.blockPosition());
                }
            }
        }
        return positions;
    }

    private static boolean isMobNearby(BlockPos pos, List<BlockPos> mobPositions, double radiusSq) {
        for (BlockPos mobPos : mobPositions) {
            if (pos.distSqr(mobPos) <= radiusSq) return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Waiting-for-clear processing
    // -----------------------------------------------------------------------

    private static void processWaitingForClear(ServerLevel level, LevelRegenData data) {
        if (data.waitingForClear.isEmpty()) return;

        double radiusSq = (double) EnhancedHordesTweaksConfig.mobClearedRadius
                * EnhancedHordesTweaksConfig.mobClearedRadius;
        List<BlockPos> mobPositions = collectHordeMobPositions(level);

        List<BlockPos> companionsToCancel = new ArrayList<>();

        Iterator<Map.Entry<BlockPos, BlockState>> iter = data.waitingForClear.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<BlockPos, BlockState> entry = iter.next();
            BlockPos pos = entry.getKey();

            // Spot was filled externally — cancel this and its companion
            if (!level.getBlockState(pos).isAir()) {
                iter.remove();
                BlockPos companion = getCompanionPos(entry.getValue(), pos);
                if (companion != null) companionsToCancel.add(companion.immutable());
                continue;
            }

            if (!isMobNearby(pos, mobPositions, radiusSq)) {
                long regenTime = level.getGameTime() + computeRegenDelay(level, pos, entry.getValue());
                data.regenQueue.put(pos, new PendingRegen(entry.getValue(), regenTime));
                iter.remove();
            }
        }

        for (BlockPos cPos : companionsToCancel) {
            cancelCompanionFromAllQueues(data, cPos);
        }
    }

    // -----------------------------------------------------------------------
    // Paused regen processing
    // -----------------------------------------------------------------------

    private static void processPausedRegen(ServerLevel level, LevelRegenData data) {
        if (data.pausedRegen.isEmpty()) return;

        double radiusSq = (double) EnhancedHordesTweaksConfig.mobClearedRadius
                * EnhancedHordesTweaksConfig.mobClearedRadius;
        List<BlockPos> mobPositions = collectHordeMobPositions(level);

        List<BlockPos> companionsToCancel = new ArrayList<>();

        Iterator<Map.Entry<BlockPos, PausedRegen>> iter = data.pausedRegen.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<BlockPos, PausedRegen> entry = iter.next();
            BlockPos pos = entry.getKey();

            if (!level.getBlockState(pos).isAir()) {
                iter.remove();
                BlockPos companion = getCompanionPos(entry.getValue().blockState(), pos);
                if (companion != null) companionsToCancel.add(companion.immutable());
                continue;
            }

            if (!isMobNearby(pos, mobPositions, radiusSq)) {
                long regenTime = level.getGameTime() + entry.getValue().remainingTicks();
                data.regenQueue.put(pos, new PendingRegen(entry.getValue().blockState(), regenTime));
                iter.remove();
            }
        }

        for (BlockPos cPos : companionsToCancel) {
            cancelCompanionFromAllQueues(data, cPos);
        }
    }

    // -----------------------------------------------------------------------
    // Regeneration
    // -----------------------------------------------------------------------

    private static void processRegenQueue(ServerLevel level, LevelRegenData data) {
        if (data.regenQueue.isEmpty()) return;

        long currentTime = level.getGameTime();
        boolean requirePlayers = EnhancedHordesTweaksConfig.requireNearbyPlayers;
        double playerRadius = EnhancedHordesTweaksConfig.nearbyPlayerRadius;
        boolean staggered = EnhancedHordesTweaksConfig.staggeredRegen;
        boolean warningParticles = EnhancedHordesTweaksConfig.showPreRegenWarningParticles;
        int staggerInterval = EnhancedHordesTweaksConfig.staggeredRegenIntervalTicks;

        boolean checkMobReturn = EnhancedHordesTweaksConfig.cancelRegenOnMobReturn;
        double cancelRadiusSq = (double) EnhancedHordesTweaksConfig.mobClearedRadius
                * EnhancedHordesTweaksConfig.mobClearedRadius;
        List<BlockPos> mobPositions = checkMobReturn ? collectHordeMobPositions(level) : null;

        // Collected outside the iterator to avoid ConcurrentModificationException
        // when we need to touch the map for companion blocks.
        List<BlockPos> readyToPlace = staggered ? new ArrayList<>() : null;
        List<BlockPos> companionsToCancel = new ArrayList<>();
        // For non-staggered: companion positions to place after the loop
        List<BlockPos> companionsToPlace = staggered ? null : new ArrayList<>();

        Iterator<Map.Entry<BlockPos, PendingRegen>> iter = data.regenQueue.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<BlockPos, PendingRegen> entry = iter.next();
            BlockPos pos = entry.getKey();
            PendingRegen regen = entry.getValue();

            // If a horde mob has returned, cancel or pause this regen.
            if (checkMobReturn && isMobNearby(pos, mobPositions, cancelRadiusSq)) {
                if (EnhancedHordesTweaksConfig.resetDelayOnMobReturn) {
                    data.waitingForClear.put(pos, regen.blockState());
                } else {
                    long remaining = Math.max(0L, regen.regenTime() - currentTime);
                    data.pausedRegen.put(pos, new PausedRegen(regen.blockState(), remaining));
                }
                iter.remove();
                continue;
            }

            // Warning particles (non-staggered only — fixed regen time known in advance).
            if (warningParticles && !staggered
                    && currentTime >= regen.regenTime() - 100L
                    && currentTime < regen.regenTime()
                    && currentTime % 10 == 0) {
                level.sendParticles(ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        5, 0.3, 0.3, 0.3, 0.05);
            }

            if (currentTime < regen.regenTime()) continue;

            // Spot was filled externally — cancel this and its companion.
            if (!level.getBlockState(pos).isAir()) {
                iter.remove();
                BlockPos companion = getCompanionPos(regen.blockState(), pos);
                if (companion != null) companionsToCancel.add(companion.immutable());
                continue;
            }

            if (requirePlayers) {
                if (level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(playerRadius)).isEmpty()) {
                    continue;
                }
            }

            if (EnhancedHordesTweaksConfig.checkEntityCollision) {
                if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) {
                    continue;
                }
            }

            if (EnhancedHordesTweaksConfig.regenDaytimeOnly && !level.isDay()) continue;

            if (staggered) {
                readyToPlace.add(pos);
            } else {
                // If this is the upper half of a two-block object and its lower companion is
                // still pending, skip it here. The lower half will place us via
                // placeCompanionFromQueues, ensuring lower is always placed first.
                // Placing upper before lower triggers DoorBlock/BedBlock.neighborChanged which
                // pops the upper half off and drops its item in a repair loop.
                if (isUpperHalf(regen.blockState())) {
                    BlockPos lower = getCompanionPos(regen.blockState(), pos);
                    if (lower != null) {
                        BlockPos lowerKey = lower.immutable();
                        if (data.regenQueue.containsKey(lowerKey)
                                || data.waitingForClear.containsKey(lowerKey)
                                || data.pausedRegen.containsKey(lowerKey)) {
                            continue;
                        }
                    }
                }
                placeBlock(level, data, pos, regen.blockState());
                iter.remove();
                // Queue companion for after-loop placement (safe — outside this iterator)
                BlockPos companion = getCompanionPos(regen.blockState(), pos);
                if (companion != null) companionsToPlace.add(companion.immutable());
            }
        }

        // Cancel companions of externally-filled spots (safe — outside iterator)
        for (BlockPos cPos : companionsToCancel) {
            cancelCompanionFromAllQueues(data, cPos);
        }

        // Place companions for non-staggered placements (safe — outside iterator)
        if (companionsToPlace != null) {
            for (BlockPos cPos : companionsToPlace) {
                placeCompanionFromQueues(level, data, cPos);
            }
        }

        // Staggered: show warning particles on all ready blocks; place one per interval.
        if (staggered && readyToPlace != null && !readyToPlace.isEmpty()) {
            if (warningParticles && currentTime % 10 == 0) {
                for (BlockPos pos : readyToPlace) {
                    level.sendParticles(ParticleTypes.CRIT,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            5, 0.3, 0.3, 0.3, 0.05);
                }
            }

            if (currentTime - data.lastStaggerTick >= staggerInterval) {
                // Shuffle and find the first candidate still in the queue
                // (companions placed in earlier ticks may have been removed).
                Collections.shuffle(readyToPlace, RANDOM);
                for (BlockPos chosen : readyToPlace) {
                    PendingRegen regen = data.regenQueue.get(chosen);
                    if (regen == null) continue; // already placed as a companion
                    // Prefer lower halves — skip upper if its lower companion is also ready.
                    if (isUpperHalf(regen.blockState())) {
                        BlockPos lower = getCompanionPos(regen.blockState(), chosen);
                        if (lower != null && data.regenQueue.containsKey(lower.immutable())) {
                            continue;
                        }
                    }
                    data.regenQueue.remove(chosen);
                    placeBlock(level, data, chosen, regen.blockState());
                    data.lastStaggerTick = currentTime;
                    // Place companion immediately so both halves appear in the same tick
                    BlockPos companion = getCompanionPos(regen.blockState(), chosen);
                    if (companion != null) placeCompanionFromQueues(level, data, companion.immutable());
                    break;
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Block placement
    // -----------------------------------------------------------------------

    private static void placeBlock(ServerLevel level, LevelRegenData data,
                                   BlockPos pos, BlockState state) {
        // Recompute connection properties (fences, walls, glass panes, etc.) based on
        // current neighbours rather than the stale stored state.
        BlockState correctedState = Block.updateFromNeighbourShapes(state, level, pos);
        level.setBlock(pos, correctedState, Block.UPDATE_ALL);
        data.prevSnapshot.put(pos, state);

        if (EnhancedHordesTweaksConfig.showRegenerationParticles) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.3, 0.3, 0.3, 0.0);
        }

        if (EnhancedHordesTweaksConfig.playRegenerationSound) {
            var sound = state.getSoundType();
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    sound.getVolume(), sound.getPitch());
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static LevelRegenData getOrCreate(ResourceKey<Level> dim) {
        return levelData.computeIfAbsent(dim, k -> new LevelRegenData());
    }

    // -----------------------------------------------------------------------
    // Data classes
    // -----------------------------------------------------------------------

    private static class LevelRegenData {
        final Map<BlockPos, PendingRegen> regenQueue = new HashMap<>();
        final Set<BlockPos> playerBroken = new HashSet<>();
        final Map<BlockPos, BlockState> waitingForClear = new HashMap<>();
        final Map<BlockPos, PausedRegen> pausedRegen = new HashMap<>();
        Map<BlockPos, BlockState> prevSnapshot = new HashMap<>();
        int ticksSinceLastScan = 0;
        long lastStaggerTick = 0L;
    }

    private record PendingRegen(BlockState blockState, long regenTime) {}

    private record PausedRegen(BlockState blockState, long remainingTicks) {}
}
