package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
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

        FearKind kind;
        if (mob instanceof Animal) {
            kind = mob instanceof NeutralMob ? FearKind.NEUTRAL : FearKind.PASSIVE;
        } else {
            kind = FearKind.HOSTILE;
        }

        if (!isUniversalHostilityTarget(mob)) return;

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
        List<? extends String> ids = EnhancedHordesTweaksConfig.hostileMobs;
        if (ids == null || ids.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(candidate.getType());
        return id != null && ids.contains(id.toString());
    }

    private static boolean isFearEnabledFor(FearKind kind) {
        return switch (kind) {
            case PASSIVE -> EnhancedHordesTweaksConfig.enablePassiveFear;
            case NEUTRAL -> EnhancedHordesTweaksConfig.enableNeutralFear;
            case HOSTILE -> EnhancedHordesTweaksConfig.enableHostileFear;
        };
    }

    private static boolean isUniversalHostilityTarget(LivingEntity entity) {
        List<? extends String> ids = EnhancedHordesTweaksConfig.hostilityTargetMobs;
        if (ids == null || ids.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && ids.contains(id.toString());
    }
}
