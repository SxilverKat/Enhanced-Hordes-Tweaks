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
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class IronGolemHandler {

    private static final Random RANDOM = new Random();

    // For fire immunity toggle: save pre-tick fire ticks, restore if EH cleared them.
    private static final WeakHashMap<Entity, Integer> savedFireTicks = new WeakHashMap<>();

    // For regen cooldown: tracks the game tick at which each golem last took damage.
    private static final WeakHashMap<IronGolem, Long> lastHurtGameTick = new WeakHashMap<>();

    // Arrow resistance: HIGHEST decides block/pass, LOWEST enforces the decision over EH.
    private static boolean arrowResistanceDecision = false;

    // -----------------------------------------------------------------------
    // Multi-hit sweep toggle
    // IronGolemSwooshProcedure deals exactly 5.0F to nearby mobs when a golem
    // attacks. Vanilla golem hits are ≥7.5F, so 5.0F uniquely identifies the sweep.
    // -----------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurtMultiHit(LivingHurtEvent event) {
        if (EnhancedHordesTweaksConfig.enableIronGolemMultiHit) return;
        if (event.isCanceled()) return;
        if (!(event.getSource().getEntity() instanceof IronGolem)) return;
        if (Math.abs(event.getAmount() - 5.0f) < 0.01f) {
            event.setCanceled(true);
        }
    }

    // -----------------------------------------------------------------------
    // Arrow resistance toggle
    // HIGHEST: decide whether to block this arrow based on our configured percent.
    // LOWEST:  enforce that decision, overriding whatever EH's handler did.
    // -----------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurtArrowResistanceDecide(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow)) return;
        if (!EnhancedHordesTweaksConfig.enableIronGolemArrowResistance) {
            arrowResistanceDecision = false;
            return;
        }
        arrowResistanceDecision = RANDOM.nextInt(100) < EnhancedHordesTweaksConfig.ironGolemArrowResistancePercent;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtArrowResistance(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow)) return;
        event.setCanceled(arrowResistanceDecision);
    }

    // -----------------------------------------------------------------------
    // Effect immunity toggle
    // IronGolemTickProcedure removes Wither, Poison, and Levitation every tick.
    // When disabled, we cancel those MobEffectEvent.Remove events so the effects
    // persist. EH calls removeEffect() again next tick; we cancel that too.
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onMobEffectRemove(MobEffectEvent.Remove event) {
        if (EnhancedHordesTweaksConfig.enableIronGolemEffectImmunity) return;
        if (!(event.getEntity() instanceof IronGolem)) return;
        var effect = event.getEffect();
        if (effect == MobEffects.WITHER || effect == MobEffects.POISON || effect == MobEffects.LEVITATION) {
            event.setCanceled(true);
        }
    }

    // -----------------------------------------------------------------------
    // Hurry toggle
    // IronGolemTickProcedure applies a Speed effect when a villager is targeted.
    // When disabled, we prevent Speed effects from being applied to golems.
    // Note: this also blocks speed from potions/beacons on iron golems.
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (EnhancedHordesTweaksConfig.enableIronGolemHurry) return;
        if (!(event.getEntity() instanceof IronGolem)) return;
        if (event.getEffectInstance().getEffect() == MobEffects.MOVEMENT_SPEED) {
            event.setResult(Event.Result.DENY);
        }
    }

    // -----------------------------------------------------------------------
    // Fire immunity toggle
    // IronGolemTickProcedure clears fire ticks every tick when the golem isn't
    // standing in fire/lava. We save the pre-tick value at HIGHEST priority and
    // restore it at LOWEST if EH cleared it during the tick.
    // -----------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingTickSaveFireTicks(LivingEvent.LivingTickEvent event) {
        if (EnhancedHordesTweaksConfig.enableIronGolemFireImmunity) return;
        if (!(event.getEntity() instanceof IronGolem)) return;
        savedFireTicks.put(event.getEntity(), event.getEntity().getRemainingFireTicks());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickRestoreFireTicks(LivingEvent.LivingTickEvent event) {
        if (EnhancedHordesTweaksConfig.enableIronGolemFireImmunity) return;
        if (!(event.getEntity() instanceof IronGolem)) return;
        Integer saved = savedFireTicks.remove(event.getEntity());
        if (saved != null && saved > 0 && event.getEntity().getRemainingFireTicks() <= 0) {
            // EH cleared the fire ticks; restore minus one natural decrement per tick.
            event.getEntity().setRemainingFireTicks(Math.max(0, saved - 1));
        }
    }

    // -----------------------------------------------------------------------
    // Regen cooldown
    // Records the last time an Iron Golem took actual damage so we can deny
    // the REGENERATION effect for the configured cooldown window.
    // -----------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurtGolemCooldown(LivingHurtEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (golem.level() instanceof ServerLevel serverLevel) {
            lastHurtGameTick.put(golem, serverLevel.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onMobEffectApplicableRegenCooldown(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof IronGolem golem)) return;
        if (event.getEffectInstance().getEffect() != MobEffects.REGENERATION) return;
        if (EnhancedHordesTweaksConfig.ironGolemRegenCooldownSeconds <= 0) return;
        Long lastHurt = lastHurtGameTick.get(golem);
        if (lastHurt == null) return;
        if (golem.level() instanceof ServerLevel serverLevel) {
            long cooldownTicks = (long) EnhancedHordesTweaksConfig.ironGolemRegenCooldownSeconds * 20L;
            if (serverLevel.getGameTime() - lastHurt < cooldownTicks) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Villager defender
    // When a Villager takes damage, nearby Iron Golems will target the attacker.
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onVillagerHurt(LivingHurtEvent event) {
        if (!EnhancedHordesTweaksConfig.enableIronGolemVillagerDefender) return;
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (event.isCanceled()) return;
        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;
        if (!(villager.level() instanceof ServerLevel serverLevel)) return;

        List<IronGolem> nearbyGolems = serverLevel.getEntitiesOfClass(
                IronGolem.class,
                new AABB(villager.blockPosition()).inflate(32.0)
        );
        for (IronGolem golem : nearbyGolems) {
            if (golem.getTarget() == null) {
                golem.setTarget(attacker);
            }
        }
    }
}
