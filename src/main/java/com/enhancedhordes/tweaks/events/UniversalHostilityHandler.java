package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.compat.GameStagesCompat;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.util.FeatureGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class UniversalHostilityHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        if (!ConfigCache.isHostileMob(mob.getType())) return;

        mob.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(
                mob, LivingEntity.class, 10, true, false,
                UniversalHostilityHandler::isHostilityTarget));
    }

    public static boolean isHostilityTarget(LivingEntity candidate) {
        if (candidate == null) return false;
        if (!EnhancedHordesTweaksConfig.enableUniversalHostility) return false;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                candidate.level(), EnhancedHordesTweaksConfig.universalHostilityDaysBeforeActivation)) return false;
        if (FeatureGate.nightBlocked(candidate.level())) return false;
        if (candidate.level() instanceof ServerLevel level
                && !GameStagesCompat.anyPlayerHasStage(level, EnhancedHordesTweaksConfig.universalHostilityStage)) return false;
        if (isProtected(candidate)) return false;
        return ConfigCache.isHostilityTarget(candidate.getType());
    }

    public static boolean isProtected(LivingEntity candidate) {
        if (EnhancedHordesTweaksConfig.protectTamedAnimals) {
            if (candidate instanceof TamableAnimal tamable && tamable.isTame()) return true;
            if (candidate instanceof AbstractHorse horse && horse.isTamed()) return true;
        }
        return EnhancedHordesTweaksConfig.protectNamedEntities && candidate.hasCustomName();
    }
}
