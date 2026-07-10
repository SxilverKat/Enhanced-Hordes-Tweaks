package com.enhancedhordes.tweaks.events;
import com.enhancedhordes.tweaks.util.VersionCompat;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
//? if >=1.20.1 {
import net.minecraft.tags.DamageTypeTags;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HordeCombatHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        int resistance = EnhancedHordesTweaksConfig.hordeFireDamageResistance;
        if (resistance <= 0) return;
        //? if >=1.19.2 {
        LivingEntity entity = event.getEntity();
        //?} else {
        /*LivingEntity entity = event.getEntityLiving();*/
        //?}
        if (!ConfigCache.isHordeMob(entity.getType())) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                VersionCompat.level(entity), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) return;
        //? if >=1.20.1 {
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
        //?} else {
        /*if (event.getSource().isFire()) {*/
        //?}
            event.setAmount(event.getAmount() * (1.0f - resistance / 100.0f));
        }
    }
}
