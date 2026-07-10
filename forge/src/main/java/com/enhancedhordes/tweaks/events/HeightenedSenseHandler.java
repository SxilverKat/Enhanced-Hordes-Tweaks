package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.compat.GameStagesCompat;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.util.FeatureGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HeightenedSenseHandler {

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final double MAX_RANGE = 128.0;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!EnhancedHordesTweaksConfig.enableHeightenedSense) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (mob.tickCount % CHECK_INTERVAL_TICKS != 0) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                level, EnhancedHordesTweaksConfig.heightenedSenseDaysBeforeActivation)) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;
        if (FeatureGate.blocked(mob)) return;

        LivingEntity current = mob.getTarget();
        if (current != null && current.isAlive()) return;

        double range = computeRange(level);
        AABB box = mob.getBoundingBox().inflate(range);
        double rangeSq = range * range;

        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Player player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            if (!GameStagesCompat.allows(player, EnhancedHordesTweaksConfig.heightenedSenseStage)) continue;
            double d = mob.distanceToSqr(player);
            if (d <= rangeSq && d < bestDistSq) {
                best = player;
                bestDistSq = d;
            }
        }

        if (best == null && EnhancedHordesTweaksConfig.enableUniversalHostility) {
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box,
                    c -> c != mob && c.isAlive() && !UniversalHostilityHandler.isProtected(c)
                            && ConfigCache.isHostilityTarget(c.getType()))) {
                double d = mob.distanceToSqr(candidate);
                if (d <= rangeSq && d < bestDistSq) {
                    best = candidate;
                    bestDistSq = d;
                }
            }
        }

        if (best != null) {
            mob.setTarget(best);
        }
    }

    private static double computeRange(ServerLevel level) {
        double range = EnhancedHordesTweaksConfig.heightenedSenseRange;
        if (EnhancedHordesTweaksConfig.heightenedSenseIncreaseOverTime) {
            int threshold = EnhancedHordesTweaksConfig.heightenedSenseDaysBeforeActivation;
            long daysElapsed = level.getGameTime() / 24000L;
            if (daysElapsed >= threshold) {
                int interval = Math.max(1, EnhancedHordesTweaksConfig.heightenedSenseIncreaseIntervalDays);
                long increments = (daysElapsed - threshold) / interval;
                range += increments * EnhancedHordesTweaksConfig.heightenedSenseIncreaseAmount;
            }
        }
        return Math.min(range, MAX_RANGE);
    }
}
