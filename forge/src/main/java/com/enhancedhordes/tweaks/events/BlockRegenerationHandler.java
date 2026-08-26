package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
//? if >=1.19.2 {
import net.minecraftforge.event.level.BlockEvent;
//?} else {
/*import net.minecraftforge.event.world.BlockEvent;*/
//?}
//? if >=1.19.2 {
import net.minecraftforge.event.level.LevelEvent;
//?} else {
/*import net.minecraftforge.event.world.WorldEvent;*/
//?}
//? if >=1.19.2 {
import net.minecraftforge.event.level.PistonEvent;
//?} else {
/*import net.minecraftforge.event.world.PistonEvent;*/
//?}
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlockRegenerationHandler {

    private static final Random RANDOM = new Random();

    private static final int SCAN_INTERVAL = 20;
    private static final int SCAN_RADIUS = 4;
    private static final int MAX_PLAYER_BROKEN = 2000;

    private static final Map<ResourceKey<Level>, LevelRegenData> levelData = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!EnhancedHordesTweaksConfig.enableBlockRegeneration) return;
        if (event.isCanceled()) return;
        //? if >=1.19.2 {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        //?} else {
        /*if (!(event.getWorld() instanceof ServerLevel level)) return;*/
        //?}

        BlockPos pos = event.getPos().immutable();
        LevelRegenData data = getOrCreate(level.dimension());
        data.regenQueue.remove(pos);
        data.waitingForClear.remove(pos);
        data.pausedRegen.remove(pos);
        data.playerBroken.add(pos);

        while (data.playerBroken.size() > MAX_PLAYER_BROKEN) {
            Iterator<BlockPos> it = data.playerBroken.iterator();
            if (!it.hasNext()) break;
            it.next();
            it.remove();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!EnhancedHordesTweaksConfig.enableBlockRegeneration) return;
        if (!EnhancedHordesTweaksConfig.cancelRegenOnPlayerPlace) return;
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        //? if >=1.19.2 {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        //?} else {
        /*if (!(event.getWorld() instanceof ServerLevel level)) return;*/
        //?}

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
    public static void onPistonMove(PistonEvent.Pre event) {
        if (!EnhancedHordesTweaksConfig.enableBlockRegeneration) return;
        //? if >=1.19.2 {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        //?} else {
        /*if (!(event.getWorld() instanceof ServerLevel level)) return;*/
        //?}
        LevelRegenData data = levelData.get(level.dimension());
        if (data == null) return;

        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null) return;

        Direction pushDirection = resolver.getPushDirection();
        for (BlockPos pos : resolver.getToPush()) {
            forgetPosition(data, pos);
            forgetPosition(data, pos.relative(pushDirection));
        }
        for (BlockPos pos : resolver.getToDestroy()) {
            forgetPosition(data, pos);
        }
    }

    private static void forgetPosition(LevelRegenData data, BlockPos pos) {
        data.prevSnapshot.remove(pos);
        data.regenQueue.remove(pos);
        data.waitingForClear.remove(pos);
        data.pausedRegen.remove(pos);
    }

    @SubscribeEvent
    //? if >=1.19.2 {
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
    //?} else {
    /*public static void onLevelTick(TickEvent.WorldTickEvent event) {*/
    //?}
        if (event.phase != TickEvent.Phase.END) return;
        //? if >=1.19.2 {
        if (!(event.level instanceof ServerLevel level)) return;
        //?} else {
        /*if (!(event.world instanceof ServerLevel level)) return;*/
        //?}
        if (!EnhancedHordesTweaksConfig.enableBlockRegeneration) return;

        LevelRegenData data = getOrCreate(level.dimension());
        data.ticksSinceLastScan++;
        boolean scanTick = data.ticksSinceLastScan >= SCAN_INTERVAL;

        if (scanTick) {
            data.ticksSinceLastScan = 0;
            List<BlockPos> mobPositions = collectHordeMobPositions(level);
            scanForBreaks(level, data, mobPositions);
            processWaitingForClear(level, data, mobPositions);
            processPausedRegen(level, data, mobPositions);
            processRegenQueue(level, data, mobPositions);
            return;
        }

        processRegenQueue(level, data, null);
    }

    @SubscribeEvent
    //? if >=1.19.2 {
    public static void onLevelUnload(LevelEvent.Unload event) {
    //?} else {
    /*public static void onLevelUnload(WorldEvent.Unload event) {*/
    //?}
        //? if >=1.19.2 {
        if (event.getLevel() instanceof ServerLevel level) {
        //?} else {
        /*if (event.getWorld() instanceof ServerLevel level) {*/
        //?}
            levelData.remove(level.dimension());
        }
    }

    public static void scheduleRegen(ServerLevel level, BlockPos pos, BlockState state) {
        if (!EnhancedHordesTweaksConfig.enableBlockRegeneration) return;
        LevelRegenData data = getOrCreate(level.dimension());

        if (data.playerBroken.contains(pos)) return;
        if (data.regenQueue.containsKey(pos) || data.waitingForClear.containsKey(pos)) return;

        enqueue(level, data, pos, state);
        scheduleCompanion(level, data, pos, state);
    }

    private static void scanForBreaks(ServerLevel level, LevelRegenData data, List<BlockPos> mobPositions) {
        for (Map.Entry<BlockPos, BlockState> entry : data.prevSnapshot.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState prevState = entry.getValue();
            BlockState currentState = level.getBlockState(pos);

            if (currentState.isAir() && !prevState.isAir()) {
                if (data.playerBroken.remove(pos)) {
                    continue;
                } else if (!data.regenQueue.containsKey(pos) && !data.waitingForClear.containsKey(pos)) {
                    enqueue(level, data, pos, prevState);
                    scheduleCompanion(level, data, pos, prevState);
                }
            }
        }

        Map<BlockPos, BlockState> newSnapshot = new HashMap<>();
        if (mobPositions != null) {
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (BlockPos mobPos : mobPositions) {
                for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                            cursor.set(mobPos.getX() + dx, mobPos.getY() + dy, mobPos.getZ() + dz);
                            if (newSnapshot.containsKey(cursor)) continue;
                            BlockState state = level.getBlockState(cursor);
                            if (!state.isAir() && ConfigCache.isHordeBreakable(state)) {
                                newSnapshot.put(cursor.immutable(), state);
                            }
                        }
                    }
                }
            }
        }

        data.prevSnapshot = newSnapshot;
    }

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

    private static void cancelCompanionFromAllQueues(LevelRegenData data, BlockPos companionPos) {
        data.regenQueue.remove(companionPos);
        data.waitingForClear.remove(companionPos);
        data.pausedRegen.remove(companionPos);
    }

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
        if (hardness < 0) return baseDelay;
        float multiplier = Math.max(0.1f, Math.min(5.0f, hardness / 1.5f));
        return (long)(baseDelay * multiplier);
    }

    private static List<BlockPos> collectHordeMobPositions(ServerLevel level) {
        List<BlockPos> positions = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity.isRemoved()) continue;
            if (ConfigCache.isHordeMob(entity.getType())) {
                positions.add(entity.blockPosition());
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

    private static void processWaitingForClear(ServerLevel level, LevelRegenData data, List<BlockPos> mobPositions) {
        if (data.waitingForClear.isEmpty()) return;

        double radiusSq = (double) EnhancedHordesTweaksConfig.mobClearedRadius
                * EnhancedHordesTweaksConfig.mobClearedRadius;

        List<BlockPos> companionsToCancel = new ArrayList<>();

        Iterator<Map.Entry<BlockPos, BlockState>> iter = data.waitingForClear.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<BlockPos, BlockState> entry = iter.next();
            BlockPos pos = entry.getKey();

            if (!level.getBlockState(pos).isAir()) {
                iter.remove();
                BlockPos companion = getCompanionPos(entry.getValue(), pos);
                if (companion != null) companionsToCancel.add(companion.immutable());
                continue;
            }

            if (mobPositions == null || !isMobNearby(pos, mobPositions, radiusSq)) {
                long regenTime = level.getGameTime() + computeRegenDelay(level, pos, entry.getValue());
                data.regenQueue.put(pos, new PendingRegen(entry.getValue(), regenTime));
                iter.remove();
            }
        }

        for (BlockPos cPos : companionsToCancel) {
            cancelCompanionFromAllQueues(data, cPos);
        }
    }

    private static void processPausedRegen(ServerLevel level, LevelRegenData data, List<BlockPos> mobPositions) {
        if (data.pausedRegen.isEmpty()) return;

        double radiusSq = (double) EnhancedHordesTweaksConfig.mobClearedRadius
                * EnhancedHordesTweaksConfig.mobClearedRadius;

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

            if (mobPositions == null || !isMobNearby(pos, mobPositions, radiusSq)) {
                long regenTime = level.getGameTime() + entry.getValue().remainingTicks();
                data.regenQueue.put(pos, new PendingRegen(entry.getValue().blockState(), regenTime));
                iter.remove();
            }
        }

        for (BlockPos cPos : companionsToCancel) {
            cancelCompanionFromAllQueues(data, cPos);
        }
    }

    private static void processRegenQueue(ServerLevel level, LevelRegenData data, List<BlockPos> mobPositions) {
        if (data.regenQueue.isEmpty()) return;

        long currentTime = level.getGameTime();
        boolean requirePlayers = EnhancedHordesTweaksConfig.requireNearbyPlayers;
        double playerRadius = EnhancedHordesTweaksConfig.nearbyPlayerRadius;
        boolean staggered = EnhancedHordesTweaksConfig.staggeredRegen;
        boolean warningParticles = EnhancedHordesTweaksConfig.showPreRegenWarningParticles;
        int staggerInterval = EnhancedHordesTweaksConfig.staggeredRegenIntervalTicks;

        boolean checkMobReturn = EnhancedHordesTweaksConfig.cancelRegenOnMobReturn && mobPositions != null;
        double cancelRadiusSq = (double) EnhancedHordesTweaksConfig.mobClearedRadius
                * EnhancedHordesTweaksConfig.mobClearedRadius;

        List<BlockPos> readyToPlace = staggered ? new ArrayList<>() : null;
        List<BlockPos> companionsToCancel = new ArrayList<>();
        List<BlockPos> companionsToPlace = staggered ? null : new ArrayList<>();

        Iterator<Map.Entry<BlockPos, PendingRegen>> iter = data.regenQueue.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<BlockPos, PendingRegen> entry = iter.next();
            BlockPos pos = entry.getKey();
            PendingRegen regen = entry.getValue();

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

            if (warningParticles && !staggered
                    && currentTime >= regen.regenTime() - 100L
                    && currentTime < regen.regenTime()
                    && currentTime % 10 == 0) {
                level.sendParticles(ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        5, 0.3, 0.3, 0.3, 0.05);
            }

            if (currentTime < regen.regenTime()) continue;

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
                BlockPos companion = getCompanionPos(regen.blockState(), pos);
                if (companion != null) companionsToPlace.add(companion.immutable());
            }
        }

        for (BlockPos cPos : companionsToCancel) {
            cancelCompanionFromAllQueues(data, cPos);
        }

        if (companionsToPlace != null) {
            for (BlockPos cPos : companionsToPlace) {
                placeCompanionFromQueues(level, data, cPos);
            }
        }

        if (staggered && readyToPlace != null && !readyToPlace.isEmpty()) {
            if (warningParticles && currentTime % 10 == 0) {
                for (BlockPos pos : readyToPlace) {
                    level.sendParticles(ParticleTypes.CRIT,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            5, 0.3, 0.3, 0.3, 0.05);
                }
            }

            if (currentTime - data.lastStaggerTick >= staggerInterval) {
                Collections.shuffle(readyToPlace, RANDOM);
                for (BlockPos chosen : readyToPlace) {
                    PendingRegen regen = data.regenQueue.get(chosen);
                    if (regen == null) continue;
                    if (isUpperHalf(regen.blockState())) {
                        BlockPos lower = getCompanionPos(regen.blockState(), chosen);
                        if (lower != null) {
                            BlockPos lowerKey = lower.immutable();
                            if (data.regenQueue.containsKey(lowerKey)
                                    || data.waitingForClear.containsKey(lowerKey)
                                    || data.pausedRegen.containsKey(lowerKey)) {
                                continue;
                            }
                        }
                    }
                    data.regenQueue.remove(chosen);
                    placeBlock(level, data, chosen, regen.blockState());
                    data.lastStaggerTick = currentTime;
                    BlockPos companion = getCompanionPos(regen.blockState(), chosen);
                    if (companion != null) placeCompanionFromQueues(level, data, companion.immutable());
                    break;
                }
            }
        }
    }

    private static void placeBlock(ServerLevel level, LevelRegenData data,
                                   BlockPos pos, BlockState state) {
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

    private static LevelRegenData getOrCreate(ResourceKey<Level> dim) {
        return levelData.computeIfAbsent(dim, k -> new LevelRegenData());
    }

    private static class LevelRegenData {
        final Map<BlockPos, PendingRegen> regenQueue = new HashMap<>();
        final Set<BlockPos> playerBroken = new LinkedHashSet<>();
        final Map<BlockPos, BlockState> waitingForClear = new HashMap<>();
        final Map<BlockPos, PausedRegen> pausedRegen = new HashMap<>();
        Map<BlockPos, BlockState> prevSnapshot = new HashMap<>();
        int ticksSinceLastScan = 0;
        long lastStaggerTick = 0L;
    }

    private record PendingRegen(BlockState blockState, long regenTime) {}

    private record PausedRegen(BlockState blockState, long remainingTicks) {}
}
