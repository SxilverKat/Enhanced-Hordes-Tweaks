package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!EnhancedHordesTweaksConfig.enableHordeDetermination) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                level, EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation)) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;

        long gameTime = level.getGameTime();
        int maxDistance = EnhancedHordesTweaksConfig.hordeDeterminationFollowDistance;
        int maxTimeMinutes = EnhancedHordesTweaksConfig.hordeDeterminationFollowTimeMinutes;
        long maxTicks = (long) maxTimeMinutes * 60L * 20L;

        LivingEntity target = mob.getTarget();

        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                mob.setTarget(null);
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
        if (player.isCreative()) {
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
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % PRUNE_INTERVAL_TICKS != 0) return;
        if (RECORDS.isEmpty()) return;

        long gameTime = event.getServer().overworld().getGameTime();
        int maxTimeMinutes = EnhancedHordesTweaksConfig.hordeDeterminationFollowTimeMinutes;
        long maxTicks = (long) maxTimeMinutes * 60L * 20L;

        Iterator<Map.Entry<UUID, DeterminationRecord>> it = RECORDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DeterminationRecord> e = it.next();
            if (maxTimeMinutes > 0 && (gameTime - e.getValue().startTick) > maxTicks) {
                it.remove();
            }
        }
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
