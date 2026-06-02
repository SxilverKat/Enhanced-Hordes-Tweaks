package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PassiveFearHandler {

    private static final float AVOID_DISTANCE = 8.0f;
    private static final double WALK_SPEED = 1.0;
    private static final double SPRINT_SPEED = 1.3;

    private enum FearKind { PASSIVE, NEUTRAL, HOSTILE }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof PathfinderMob mob)) return;

        if (!ConfigCache.isHostilityTarget(mob.getType())) return;

        FearKind kind;
        if (mob instanceof Enemy) {
            kind = FearKind.HOSTILE;
        } else if (mob instanceof NeutralMob) {
            kind = FearKind.NEUTRAL;
        } else {
            kind = FearKind.PASSIVE;
        }

        Predicate<LivingEntity> predicate = candidate -> isActiveThreat(candidate, kind);
        mob.goalSelector.addGoal(2, new AvoidEntityGoal<>(
                mob,
                LivingEntity.class,
                predicate,
                AVOID_DISTANCE,
                WALK_SPEED,
                SPRINT_SPEED,
                predicate));
    }

    private static boolean isActiveThreat(LivingEntity candidate, FearKind kind) {
        if (!EnhancedHordesTweaksConfig.enableUniversalHostility) return false;
        if (!isFearEnabledFor(kind)) return false;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                candidate.level(), EnhancedHordesTweaksConfig.universalHostilityDaysBeforeActivation)) return false;
        return ConfigCache.isHostileMob(candidate.getType());
    }

    private static boolean isFearEnabledFor(FearKind kind) {
        return switch (kind) {
            case PASSIVE -> EnhancedHordesTweaksConfig.enablePassiveFear;
            case NEUTRAL -> EnhancedHordesTweaksConfig.enableNeutralFear;
            case HOSTILE -> EnhancedHordesTweaksConfig.enableHostileFear;
        };
    }
}
