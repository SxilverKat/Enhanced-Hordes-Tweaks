package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CollectiveUnderstandingHandler {

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final double MAX_SCAN_RADIUS = 32.0;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!EnhancedHordesTweaksConfig.enableCollectiveUnderstanding) return;
        if (!EnhancedHordesTweaksConfig.enableHordeDetermination) return;
        if (!(event.getEntity() instanceof Mob observer)) return;
        if (!(observer.level() instanceof ServerLevel level)) return;
        if (observer.tickCount % CHECK_INTERVAL_TICKS != 0) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                level, EnhancedHordesTweaksConfig.collectiveUnderstandingDaysBeforeActivation)) return;
        if (!ConfigCache.isHordeMob(observer.getType())) return;

        if (HordeDeterminationHandler.getFollowedPlayer(observer.getUUID()) != null) return;

        final double range = Math.min(observer.getAttributeValue(Attributes.FOLLOW_RANGE), MAX_SCAN_RADIUS);
        final double rangeSq = range * range;
        AABB box = observer.getBoundingBox().inflate(range);

        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, box,
                m -> m != observer && m.isAlive()
                        && observer.distanceToSqr(m) <= rangeSq
                        && ConfigCache.isHordeMob(m.getType()));
        if (nearby.isEmpty()) return;

        for (Mob other : nearby) {
            if (HordeDeterminationHandler.getFollowedPlayer(other.getUUID()) == null) continue;
            if (!(other.getTarget() instanceof Player)) continue;
            if (!observer.hasLineOfSight(other)) continue;
            HordeDeterminationHandler.seedFrom(observer.getUUID(), other.getUUID());
            return;
        }
    }
}
