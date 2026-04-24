package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HordeCombatHandler {

    private static final TagKey<EntityType<?>> HORDES = TagKey.create(
            Registries.ENTITY_TYPE, new ResourceLocation("forge", "hordes"));

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        int resistance = EnhancedHordesTweaksConfig.hordeFireDamageResistance;
        if (resistance <= 0) return;
        LivingEntity entity = event.getEntity();
        if (!entity.getType().is(HORDES)) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                entity.level(), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) return;
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setAmount(event.getAmount() * (1.0f - resistance / 100.0f));
        }
    }
}
