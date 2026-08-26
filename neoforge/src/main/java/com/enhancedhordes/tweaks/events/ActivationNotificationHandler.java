package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ActivationNotificationHandler {

    private static long lastDay = -1;

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        lastDay = -1;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;

        ServerLevel overworld = server.overworld();
        long day = overworld.getGameTime() / 24000L;

        if (lastDay < 0 || day <= lastDay) {
            lastDay = day;
            return;
        }

        if (!EnhancedHordesTweaksConfig.enableActivationNotifications) {
            lastDay = day;
            return;
        }
        if (server.getPlayerList().getPlayers().isEmpty()) return;

        long from = lastDay;
        lastDay = day;

        check(server, from, day, true,
                EnhancedHordesTweaksConfig.featuresDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyFeaturesMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableHordeMentality,
                EnhancedHordesTweaksConfig.hordeMentalityDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyHordeMentalityMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableUniversalHostility,
                EnhancedHordesTweaksConfig.universalHostilityDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyUniversalHostilityMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableHordeDetermination,
                EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyHordeDeterminationMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableHordeWandering,
                EnhancedHordesTweaksConfig.hordeWanderingDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyHordeWanderingMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableCollectiveUnderstanding,
                EnhancedHordesTweaksConfig.collectiveUnderstandingDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyCollectiveUnderstandingMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableHeightenedSense,
                EnhancedHordesTweaksConfig.heightenedSenseDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyHeightenedSenseMessage);
        check(server, from, day,
                EnhancedHordesTweaksConfig.hordeSightRangeBonus > 0
                        || EnhancedHordesTweaksConfig.hordeSightIncreaseOverTime,
                EnhancedHordesTweaksConfig.hordeSightDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyHordeSightMessage);
        check(server, from, day, EnhancedHordesTweaksConfig.enableCreeperWallExplosion,
                EnhancedHordesTweaksConfig.creeperWallExplosionDaysBeforeActivation,
                EnhancedHordesTweaksConfig.notifyCreeperWallExplosionMessage);
    }

    private static void check(MinecraftServer server, long from, long to,
                              boolean enabled, int threshold, String message) {
        if (!enabled) return;
        if (message == null || message.isEmpty()) return;
        if (threshold <= 0) return;
        if (threshold <= from || threshold > to) return;
        announce(server, message);
    }

    private static void announce(MinecraftServer server, String message) {
        Component text = format(message);
        SoundEvent sound = resolveSound();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(text);
            if (sound != null) {
                player.connection.send(new ClientboundSoundPacket(
                        Holder.direct(sound), SoundSource.MASTER,
                        player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, 0L));
            }
        }
    }

    private static SoundEvent resolveSound() {
        if (!EnhancedHordesTweaksConfig.notificationPlaySound) return null;
        ResourceLocation rl = ResourceLocation.tryParse(EnhancedHordesTweaksConfig.notificationSound);
        if (rl == null) return null;
        return BuiltInRegistries.SOUND_EVENT.get(rl);
    }

    private static Component format(String message) {
        StringBuilder sb = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '&' && i + 1 < message.length() && isFormatCode(message.charAt(i + 1))) {
                sb.append('§');
            } else {
                sb.append(c);
            }
        }
        return Component.literal(sb.toString());
    }

    private static boolean isFormatCode(char c) {
        c = Character.toLowerCase(c);
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || "klmnor".indexOf(c) >= 0;
    }
}
