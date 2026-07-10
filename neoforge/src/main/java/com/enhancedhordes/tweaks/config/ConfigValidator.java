package com.enhancedhordes.tweaks.config;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ConfigValidator {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ConfigValidator() {}

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        List<String> errors = new ArrayList<>();

        validateEntityList("hordeMobs", EnhancedHordesTweaksConfig.hordeMobs, errors);
        validateEntityList("intelligentTeamMobs", EnhancedHordesTweaksConfig.intelligentTeamMobs, errors);
        validateEntityList("leapingMobs", EnhancedHordesTweaksConfig.leapingMobs, errors);
        validateEntityList("graveRobbers", EnhancedHordesTweaksConfig.graveRobbers, errors);
        validateEntityList("intelligentPiglins", EnhancedHordesTweaksConfig.intelligentPiglins, errors);
        validateEntityList("hostileMobs", EnhancedHordesTweaksConfig.hostileMobs, errors);
        validateEntityList("hostilityTargetMobs", EnhancedHordesTweaksConfig.hostilityTargetMobs, errors);

        validateBlockList("hiddenZombieBlocks", EnhancedHordesTweaksConfig.hiddenZombieBlocks, errors);
        validateBlockList("hordeBreakableBlocks", EnhancedHordesTweaksConfig.hordeBreakableBlocks, errors);
        validateBlockList("hordeMentalityTier1Blocks", EnhancedHordesTweaksConfig.hordeMentalityTier1Blocks, errors);
        validateBlockList("hordeMentalityTier2Blocks", EnhancedHordesTweaksConfig.hordeMentalityTier2Blocks, errors);
        validateBlockList("hordeMentalityTier3Blocks", EnhancedHordesTweaksConfig.hordeMentalityTier3Blocks, errors);
        validateBlockList("hordeMentalityTier4Blocks", EnhancedHordesTweaksConfig.hordeMentalityTier4Blocks, errors);
        validateBlockList("hordeMentalityBlacklistBlocks", EnhancedHordesTweaksConfig.hordeMentalityBlacklistBlocks, errors);

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Enhanced Hordes Tweaks config validation failed — ")
              .append(errors.size())
              .append(" invalid entr").append(errors.size() == 1 ? "y" : "ies").append(":\n");
            for (String err : errors) sb.append("  - ").append(err).append('\n');
            sb.append("These are not present on this Minecraft version and will simply have no effect.");
            LOGGER.warn("[Enhanced Hordes Tweaks] {}", sb.toString());
        }

        LOGGER.info("[Enhanced Hordes Tweaks] Config validation passed.");
    }

    private static void validateEntityList(String name, List<? extends String> list, List<String> errors) {
        validateList(name, list, BuiltInRegistries.ENTITY_TYPE, "entity type", errors);
    }

    private static void validateBlockList(String name, List<? extends String> list, List<String> errors) {
        validateList(name, list, BuiltInRegistries.BLOCK, "block", errors);
    }

    private static void validateList(String name, List<? extends String> list,
                                     Registry<?> registry, String kind, List<String> errors) {
        if (list == null) return;
        for (String entry : list) {
            if (entry == null || entry.isBlank()) {
                errors.add(name + ": empty entry");
                continue;
            }
            if (entry.startsWith("#")) {
                String raw = entry.substring(1);
                if (ResourceLocation.tryParse(raw) == null) {
                    errors.add(name + ": malformed tag id '" + entry + "'");
                }
                continue;
            }
            ResourceLocation loc = ResourceLocation.tryParse(entry);
            if (loc == null) {
                errors.add(name + ": malformed " + kind + " id '" + entry + "'");
                continue;
            }
            if (!registry.containsKey(loc)) {
                errors.add(name + ": unknown " + kind + " '" + entry + "' (not in registry)");
            }
        }
    }
}
