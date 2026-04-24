package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

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
        if (!isHordeMob(mob)) return;

        LivingEntity current = mob.getTarget();
        if (current != null && current.isAlive()) return;

        double range = EnhancedHordesTweaksConfig.heightenedSenseRange;
        AABB box = mob.getBoundingBox().inflate(range);
        double rangeSq = range * range;

        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Player player : level.getEntitiesOfClass(Player.class, box,
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            double d = mob.distanceToSqr(player);
            if (d <= rangeSq && d < bestDistSq) {
                best = player;
                bestDistSq = d;
            }
        }

        if (EnhancedHordesTweaksConfig.enableUniversalHostility) {
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, box,
                    HeightenedSenseHandler::isUniversalHostilityTarget)) {
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

    private static boolean isHordeMob(LivingEntity mob) {
        List<? extends String> ids = EnhancedHordesTweaksConfig.hordeMobs;
        if (ids == null || ids.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return id != null && ids.contains(id.toString());
    }

    private static boolean isUniversalHostilityTarget(LivingEntity entity) {
        if (!entity.isAlive()) return false;
        List<? extends String> ids = EnhancedHordesTweaksConfig.hostilityTargetMobs;
        if (ids == null || ids.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && ids.contains(id.toString());
    }
}
