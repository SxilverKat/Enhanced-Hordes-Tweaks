package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

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
        if (!isHordeMob(observer)) return;

        if (HordeDeterminationHandler.getFollowedPlayer(observer.getUUID()) != null) return;

        double range = observer.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        if (range > MAX_SCAN_RADIUS) range = MAX_SCAN_RADIUS;
        AABB box = observer.getBoundingBox().inflate(range);

        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, box,
                m -> m != observer && m.isAlive() && isHordeMob(m));
        if (nearby.isEmpty()) return;

        long gameTime = level.getGameTime();
        for (Mob other : nearby) {
            UUID playerUuid = HordeDeterminationHandler.getFollowedPlayer(other.getUUID());
            if (playerUuid == null) continue;
            if (!observer.hasLineOfSight(other)) continue;
            HordeDeterminationHandler.seedRecord(observer.getUUID(), playerUuid, gameTime);
            return;
        }
    }

    private static boolean isHordeMob(LivingEntity mob) {
        List<? extends String> ids = EnhancedHordesTweaksConfig.hordeMobs;
        if (ids == null || ids.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return id != null && ids.contains(id.toString());
    }
}
