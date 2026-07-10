package com.enhancedhordes.tweaks;

import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.datapack.EnhancedHordesTweaksPackResources;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(EnhancedHordesTweaksMod.MODID)
public class EnhancedHordesTweaksMod {

    public static final String MODID = "enhanced_hordes_tweaks";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EnhancedHordesTweaksMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, EnhancedHordesTweaksConfig.SPEC);

        modEventBus.addListener(this::onAddPackFinders);

        LOGGER.info("[Enhanced Hordes Tweaks] Loaded.");
    }

    public void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;

        PackLocationInfo location = new PackLocationInfo(
                "builtin/enhanced_hordes_tweaks",
                Component.literal("Enhanced Hordes Tweaks"),
                PackSource.BUILT_IN,
                Optional.empty()
        );

        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo loc) {
                return new EnhancedHordesTweaksPackResources(loc);
            }

            @Override
            public PackResources openFull(PackLocationInfo loc, Pack.Metadata metadata) {
                return new EnhancedHordesTweaksPackResources(loc);
            }
        };

        Pack pack = Pack.readMetaAndCreate(
                location,
                supplier,
                PackType.SERVER_DATA,
                new PackSelectionConfig(true, Pack.Position.TOP, false)
        );

        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        } else {
            LOGGER.error("[Enhanced Hordes Tweaks] Failed to create built-in data pack. Tag overrides will not apply.");
        }
    }
}
