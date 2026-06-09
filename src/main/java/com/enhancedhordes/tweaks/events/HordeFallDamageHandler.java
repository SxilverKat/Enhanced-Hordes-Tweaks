package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HordeFallDamageHandler {

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!EnhancedHordesTweaksConfig.hordeFallDamageImmunity) return;
        if (!ConfigCache.isHordeMob(event.getEntity().getType())) return;
        event.setCanceled(true);
    }
}
