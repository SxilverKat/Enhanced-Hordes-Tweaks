package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HordeSightHandler {

    private static final UUID SIGHT_MODIFIER_ID = UUID.fromString("a7d3c1f4-9b2e-4d56-8f10-2c4e9a6b3d71");
    private static final String SIGHT_MODIFIER_NAME = "EnhancedHordes Horde Sight Bonus";
    private static final int REFRESH_INTERVAL_TICKS = 200;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (mob.tickCount % REFRESH_INTERVAL_TICKS != 0) return;
        if (!isHordeMob(mob)) return;

        AttributeInstance inst = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (inst == null) return;

        double targetBonus = computeBonus(level);
        AttributeModifier existing = inst.getModifier(SIGHT_MODIFIER_ID);
        double currentBonus = existing == null ? 0.0 : existing.getAmount();
        if (Math.abs(currentBonus - targetBonus) < 1.0e-6) return;

        if (existing != null) {
            inst.removeModifier(SIGHT_MODIFIER_ID);
        }
        if (targetBonus > 0.0) {
            inst.addPermanentModifier(new AttributeModifier(
                    SIGHT_MODIFIER_ID,
                    SIGHT_MODIFIER_NAME,
                    targetBonus,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    private static double computeBonus(ServerLevel level) {
        int baseBonus = EnhancedHordesTweaksConfig.hordeSightRangeBonus;
        int threshold = EnhancedHordesTweaksConfig.hordeSightDaysBeforeActivation;
        long daysElapsed = level.getDayTime() / 24000L;

        if (daysElapsed < threshold) return 0.0;
        double bonus = baseBonus;

        if (EnhancedHordesTweaksConfig.hordeSightIncreaseOverTime) {
            long daysSinceActivation = daysElapsed - threshold;
            int interval = Math.max(1, EnhancedHordesTweaksConfig.hordeSightIncreaseIntervalDays);
            long increments = daysSinceActivation / interval;
            bonus += increments * EnhancedHordesTweaksConfig.hordeSightIncreaseAmount;
        }
        return bonus;
    }

    private static boolean isHordeMob(LivingEntity mob) {
        List<? extends String> ids = EnhancedHordesTweaksConfig.hordeMobs;
        if (ids == null || ids.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return id != null && ids.contains(id.toString());
    }
}
