package com.enhancedhordes.tweaks;

import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.datapack.EnhancedHordesTweaksPackResources;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EnhancedHordesTweaksMod.MODID)
public class EnhancedHordesTweaksMod {

    public static final String MODID = "enhanced_hordes_tweaks";
    private static final Logger LOGGER = LogUtils.getLogger();

    public EnhancedHordesTweaksMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EnhancedHordesTweaksConfig.SPEC);

        modEventBus.addListener(this::onAddPackFinders);

        LOGGER.info("[Enhanced Hordes Tweaks] Loaded.");
    }

    public void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;

        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    "builtin/enhanced_hordes_tweaks",
                    Component.literal("Enhanced Hordes Tweaks"),
                    true,
                    id -> new EnhancedHordesTweaksPackResources(),
                    PackType.SERVER_DATA,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );
            if (pack != null) {
                consumer.accept(pack);
            } else {
                LOGGER.error("[Enhanced Hordes Tweaks] Failed to create built-in data pack. Tag overrides will not apply.");
            }
        });
    }
}
