package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
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

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!EnhancedHordesTweaksConfig.enableHeightenedSense) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (mob.tickCount % CHECK_INTERVAL_TICKS != 0) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                level, EnhancedHordesTweaksConfig.heightenedSenseDaysBeforeActivation)) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;

        LivingEntity current = mob.getTarget();
        if (current != null && current.isAlive()) return;

        double range = EnhancedHordesTweaksConfig.heightenedSenseRange;
        AABB box = mob.getBoundingBox().inflate(range);
        double rangeSq = range * range;

        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Player player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
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
}
