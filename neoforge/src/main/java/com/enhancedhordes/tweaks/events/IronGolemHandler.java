package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class IronGolemHandler {

    private static final Random RANDOM = new Random();

    private static final WeakHashMap<Entity, Integer> savedFireTicks = new WeakHashMap<>();

    private static final WeakHashMap<IronGolem, Long> lastHurtGameTick = new WeakHashMap<>();

    private static boolean arrowResistanceDecision = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurtMultiHit(LivingIncomingDamageEvent event) {
        if (EnhancedHordesTweaksConfig.enableIronGolemMultiHit) return;
        if (event.isCanceled()) return;
        if (!(event.getSource().getEntity() instanceof IronGolem)) return;
        if (Math.abs(event.getAmount() - 5.0f) < 0.01f) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurtArrowResistanceDecide(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow)) return;
        if (!EnhancedHordesTweaksConfig.enableIronGolemArrowResistance) {
            arrowResistanceDecision = false;
            return;
        }
        arrowResistanceDecision = RANDOM.nextInt(100) < EnhancedHordesTweaksConfig.ironGolemArrowResistancePercent;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtArrowResistance(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow)) return;
        event.setCanceled(arrowResistanceDecision);
    }

    @SubscribeEvent
    public static void onMobEffectRemove(MobEffectEvent.Remove event) {
        if (EnhancedHordesTweaksConfig.enableIronGolemEffectImmunity) return;
        if (!(event.getEntity() instanceof IronGolem)) return;
        var effect = event.getEffect().value();
        if (effect == MobEffects.WITHER.value() || effect == MobEffects.POISON.value() || effect == MobEffects.LEVITATION.value()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (EnhancedHordesTweaksConfig.enableIronGolemHurry) return;
        if (!(event.getEntity() instanceof IronGolem)) return;
        if (event.getEffectInstance().getEffect().value() == MobEffects.MOVEMENT_SPEED.value()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingTickSaveFireTicks(EntityTickEvent.Pre event) {
        if (EnhancedHordesTweaksConfig.enableIronGolemFireImmunity) return;
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (!(golem.level() instanceof ServerLevel)) return;
        savedFireTicks.put(golem, golem.getRemainingFireTicks());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickRestoreFireTicks(EntityTickEvent.Pre event) {
        if (EnhancedHordesTweaksConfig.enableIronGolemFireImmunity) return;
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (!(golem.level() instanceof ServerLevel)) return;
        Integer saved = savedFireTicks.remove(golem);
        if (saved != null && saved > 0 && golem.getRemainingFireTicks() <= 0) {
            golem.setRemainingFireTicks(Math.max(0, saved - 1));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtGolemCooldown(LivingIncomingDamageEvent event) {
        if (!EnhancedHordesTweaksConfig.enableIronGolemRegen) return;
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (golem.level() instanceof ServerLevel serverLevel) {
            lastHurtGameTick.put(golem, serverLevel.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onMobEffectApplicableRegenCooldown(MobEffectEvent.Applicable event) {
        if (!EnhancedHordesTweaksConfig.enableIronGolemRegen) return;
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (!(golem.level() instanceof ServerLevel serverLevel)) return;
        if (event.getEffectInstance().getEffect().value() != MobEffects.REGENERATION.value()) return;
        if (EnhancedHordesTweaksConfig.ironGolemRegenCooldownSeconds <= 0) return;
        Long lastHurt = lastHurtGameTick.get(golem);
        if (lastHurt == null) return;
        long cooldownTicks = (long) EnhancedHordesTweaksConfig.ironGolemRegenCooldownSeconds * 20L;
        long delta = serverLevel.getGameTime() - lastHurt;
        if (delta >= 0 && delta < cooldownTicks) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onVillagerHurt(LivingIncomingDamageEvent event) {
        if (!EnhancedHordesTweaksConfig.enableIronGolemVillagerDefender) return;
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (event.isCanceled()) return;
        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return;

        List<IronGolem> nearbyGolems = serverLevel.getEntitiesOfClass(
                IronGolem.class,
                villager.getBoundingBox().inflate(16.0)
        );
        for (IronGolem golem : nearbyGolems) {
            if (golem.getTarget() == null) {
                golem.setTarget(attacker);
            }
        }
    }
}
