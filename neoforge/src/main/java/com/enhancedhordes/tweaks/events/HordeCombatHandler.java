package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class HordeCombatHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        int resistance = EnhancedHordesTweaksConfig.hordeFireDamageResistance;
        if (resistance <= 0) return;
        LivingEntity entity = event.getEntity();
        if (!ConfigCache.isHordeMob(entity.getType())) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                entity.level(), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) return;
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * (1.0f - resistance / 100.0f));
        }
    }
}
