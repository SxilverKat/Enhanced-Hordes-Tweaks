package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.compat.GameStagesCompat;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.util.FeatureGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CollectiveUnderstandingHandler {

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final double MAX_RANGE = 128.0;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!EnhancedHordesTweaksConfig.enableCollectiveUnderstanding) return;
        if (!(event.getEntity() instanceof Mob observer)) return;
        if (!(observer.level() instanceof ServerLevel level)) return;
        if (observer.tickCount % CHECK_INTERVAL_TICKS != 0) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                level, EnhancedHordesTweaksConfig.collectiveUnderstandingDaysBeforeActivation)) return;
        if (!ConfigCache.isHordeMob(observer.getType())) return;
        if (FeatureGate.blocked(observer)) return;

        boolean determination = EnhancedHordesTweaksConfig.enableHordeDetermination;
        if (observer.getTarget() instanceof Player) return;
        if (determination && HordeDeterminationHandler.getFollowedPlayer(observer.getUUID()) != null) return;

        final double range = computeRange(level);
        final double rangeSq = range * range;
        AABB box = observer.getBoundingBox().inflate(range);

        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, box,
                m -> m != observer && m.isAlive()
                        && observer.distanceToSqr(m) <= rangeSq
                        && ConfigCache.isHordeMob(m.getType()));
        if (nearby.isEmpty()) return;

        for (Mob other : nearby) {
            Player chased = resolveChasedPlayer(level, other, determination);
            if (chased == null) continue;
            if (chased.isCreative() || chased.isSpectator()) continue;
            if (!GameStagesCompat.allows(chased, EnhancedHordesTweaksConfig.collectiveUnderstandingStage)) continue;
            if (!observer.hasLineOfSight(other)) continue;
            if (determination) {
                HordeDeterminationHandler.seedFrom(observer.getUUID(), other.getUUID());
            }
            observer.setTarget(chased);
            return;
        }
    }

    private static Player resolveChasedPlayer(ServerLevel level, Mob other, boolean determination) {
        if (other.getTarget() instanceof Player player && player.isAlive()) return player;
        if (determination) {
            UUID followed = HordeDeterminationHandler.getFollowedPlayer(other.getUUID());
            if (followed != null) {
                Player player = level.getPlayerByUUID(followed);
                if (player != null && player.isAlive()) return player;
            }
        }
        return null;
    }

    private static double computeRange(ServerLevel level) {
        double range = EnhancedHordesTweaksConfig.collectiveUnderstandingRange;
        if (EnhancedHordesTweaksConfig.collectiveUnderstandingIncreaseOverTime) {
            int threshold = EnhancedHordesTweaksConfig.collectiveUnderstandingDaysBeforeActivation;
            long daysElapsed = level.getGameTime() / 24000L;
            if (daysElapsed >= threshold) {
                int interval = Math.max(1, EnhancedHordesTweaksConfig.collectiveUnderstandingIncreaseIntervalDays);
                long increments = (daysElapsed - threshold) / interval;
                range += increments * EnhancedHordesTweaksConfig.collectiveUnderstandingIncreaseAmount;
            }
        }
        return Math.min(range, MAX_RANGE);
    }
}
