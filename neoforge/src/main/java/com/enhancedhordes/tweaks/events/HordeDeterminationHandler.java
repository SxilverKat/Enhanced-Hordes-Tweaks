package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.compat.GameStagesCompat;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.util.FeatureGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class HordeDeterminationHandler {

    private static final Map<UUID, DeterminationRecord> RECORDS = new ConcurrentHashMap<>();
    private static final Set<UUID> FORCED_PERSISTENCE = ConcurrentHashMap.newKeySet();
    private static final int PRUNE_INTERVAL_TICKS = 20 * 30;

    private record DeterminationRecord(UUID playerUuid, long startTick) {}

    public static UUID getFollowedPlayer(UUID mobUuid) {
        DeterminationRecord r = RECORDS.get(mobUuid);
        return r == null ? null : r.playerUuid;
    }

    public static void seedFrom(UUID observerUuid, UUID sourceMobUuid) {
        DeterminationRecord source = RECORDS.get(sourceMobUuid);
        if (source != null) {
            RECORDS.putIfAbsent(observerUuid, new DeterminationRecord(source.playerUuid, source.startTick));
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!EnhancedHordesTweaksConfig.enableHordeDetermination) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                level, EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation)) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;
        if (FeatureGate.blocked(mob)) return;

        long gameTime = level.getGameTime();
        int maxDistance = computeFollowDistance(level);
        int maxTimeMinutes = computeFollowTimeMinutes(level);
        long maxTicks = (long) maxTimeMinutes * 60L * 20L;

        LivingEntity target = mob.getTarget();

        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                mob.setTarget(null);
                RECORDS.remove(mob.getUUID());
                clearForcedPersistence(mob);
                return;
            }
            if (!GameStagesCompat.allows(player, EnhancedHordesTweaksConfig.hordeDeterminationStage)) {
                RECORDS.remove(mob.getUUID());
                clearForcedPersistence(mob);
                return;
            }
            RECORDS.compute(mob.getUUID(), (k, existing) -> {
                if (existing == null || !existing.playerUuid.equals(player.getUUID())) {
                    return new DeterminationRecord(player.getUUID(), gameTime);
                }
                return existing;
            });
            forcePersistence(mob);
            return;
        }

        DeterminationRecord record = RECORDS.get(mob.getUUID());
        if (record == null) {
            clearForcedPersistence(mob);
            return;
        }

        if (maxTimeMinutes > 0 && (gameTime - record.startTick) > maxTicks) {
            RECORDS.remove(mob.getUUID());
            clearForcedPersistence(mob);
            return;
        }

        Player player = level.getPlayerByUUID(record.playerUuid);
        if (player == null || player.isRemoved() || player.isSpectator()) {
            return;
        }
        if (player.isCreative()
                || !GameStagesCompat.allows(player, EnhancedHordesTweaksConfig.hordeDeterminationStage)) {
            RECORDS.remove(mob.getUUID());
            clearForcedPersistence(mob);
            return;
        }

        double distSqr = mob.distanceToSqr(player);
        if (distSqr > (double) maxDistance * maxDistance) {
            RECORDS.remove(mob.getUUID());
            clearForcedPersistence(mob);
            return;
        }

        mob.setTarget(player);
        forcePersistence(mob);
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity().isRemoved()) {
            RECORDS.remove(event.getEntity().getUUID());
            FORCED_PERSISTENCE.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % PRUNE_INTERVAL_TICKS != 0) return;
        if (RECORDS.isEmpty()) return;

        ServerLevel overworld = event.getServer().overworld();
        long gameTime = overworld.getGameTime();
        int maxTimeMinutes = computeFollowTimeMinutes(overworld);
        long maxTicks = (long) maxTimeMinutes * 60L * 20L;

        Iterator<Map.Entry<UUID, DeterminationRecord>> it = RECORDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DeterminationRecord> e = it.next();
            if (maxTimeMinutes > 0 && (gameTime - e.getValue().startTick) > maxTicks) {
                it.remove();
            }
        }
    }

    private static int computeFollowDistance(ServerLevel level) {
        int base = EnhancedHordesTweaksConfig.hordeDeterminationFollowDistance;
        if (!EnhancedHordesTweaksConfig.hordeDeterminationDistanceIncreaseOverTime) return base;
        long increments = incrementsSinceActivation(level,
                EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation,
                EnhancedHordesTweaksConfig.hordeDeterminationDistanceIncreaseIntervalDays);
        long distance = base + increments * (long) EnhancedHordesTweaksConfig.hordeDeterminationDistanceIncreaseAmount;
        return (int) Math.min(distance, 10000L);
    }

    private static int computeFollowTimeMinutes(ServerLevel level) {
        int base = EnhancedHordesTweaksConfig.hordeDeterminationFollowTimeMinutes;
        if (base <= 0) return base;
        if (!EnhancedHordesTweaksConfig.hordeDeterminationTimeIncreaseOverTime) return base;
        long increments = incrementsSinceActivation(level,
                EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation,
                EnhancedHordesTweaksConfig.hordeDeterminationTimeIncreaseIntervalDays);
        long minutes = base + increments * (long) EnhancedHordesTweaksConfig.hordeDeterminationTimeIncreaseAmount;
        return (int) Math.min(minutes, 1440L);
    }

    private static long incrementsSinceActivation(ServerLevel level, int thresholdDays, int intervalDays) {
        long daysElapsed = level.getGameTime() / 24000L;
        if (daysElapsed < thresholdDays) return 0L;
        int interval = Math.max(1, intervalDays);
        return (daysElapsed - thresholdDays) / interval;
    }

    private static void forcePersistence(Mob mob) {
        if (!mob.isPersistenceRequired()) {
            mob.setPersistenceRequired();
            FORCED_PERSISTENCE.add(mob.getUUID());
        }
    }

    private static void clearForcedPersistence(Mob mob) {
        if (FORCED_PERSISTENCE.remove(mob.getUUID())) {
            mob.persistenceRequired = false;
        }
    }
}
