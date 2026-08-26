package com.enhancedhordes.tweaks.command;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.compat.GameStagesCompat;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class EhtCommandHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("eht")
                        .then(Commands.literal("status")
                                .executes(ctx -> status(ctx.getSource())))
        );
    }

    private static int status(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        long day = level.getGameTime() / 24000L;

        send(source, Component.literal("=== Enhanced Hordes Tweaks ===").withStyle(ChatFormatting.GOLD));
        info(source, "Day", String.valueOf(day));
        info(source, "Difficulty preset", EnhancedHordesTweaksConfig.difficultyPreset.name());
        info(source, "Global night-only", onOff(EnhancedHordesTweaksConfig.globalNightOnly));
        if (GameStagesCompat.isLoaded()) {
            info(source, "Game Stages", onOff(EnhancedHordesTweaksConfig.enableGameStages));
        }

        feature(source, "Horde Determination", EnhancedHordesTweaksConfig.enableHordeDetermination, day,
                EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation,
                "distance " + scaled(
                        EnhancedHordesTweaksConfig.hordeDeterminationFollowDistance,
                        EnhancedHordesTweaksConfig.hordeDeterminationDistanceIncreaseOverTime,
                        EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation,
                        EnhancedHordesTweaksConfig.hordeDeterminationDistanceIncreaseIntervalDays,
                        EnhancedHordesTweaksConfig.hordeDeterminationDistanceIncreaseAmount, 10000, day)
                        + ", time " + followTime(day) + " min");

        feature(source, "Heightened Sense", EnhancedHordesTweaksConfig.enableHeightenedSense, day,
                EnhancedHordesTweaksConfig.heightenedSenseDaysBeforeActivation,
                "range " + scaled(
                        EnhancedHordesTweaksConfig.heightenedSenseRange,
                        EnhancedHordesTweaksConfig.heightenedSenseIncreaseOverTime,
                        EnhancedHordesTweaksConfig.heightenedSenseDaysBeforeActivation,
                        EnhancedHordesTweaksConfig.heightenedSenseIncreaseIntervalDays,
                        EnhancedHordesTweaksConfig.heightenedSenseIncreaseAmount, 128, day));

        feature(source, "Collective Understanding", EnhancedHordesTweaksConfig.enableCollectiveUnderstanding, day,
                EnhancedHordesTweaksConfig.collectiveUnderstandingDaysBeforeActivation,
                "range " + scaled(
                        EnhancedHordesTweaksConfig.collectiveUnderstandingRange,
                        EnhancedHordesTweaksConfig.collectiveUnderstandingIncreaseOverTime,
                        EnhancedHordesTweaksConfig.collectiveUnderstandingDaysBeforeActivation,
                        EnhancedHordesTweaksConfig.collectiveUnderstandingIncreaseIntervalDays,
                        EnhancedHordesTweaksConfig.collectiveUnderstandingIncreaseAmount, 128, day));

        hordeSight(source, day);

        feature(source, "Horde Mentality", EnhancedHordesTweaksConfig.enableHordeMentality, day,
                EnhancedHordesTweaksConfig.hordeMentalityDaysBeforeActivation,
                "damage/mob " + EnhancedHordesTweaksConfig.hordeMentalityDamageRatePerMob);

        feature(source, "Universal Hostility", EnhancedHordesTweaksConfig.enableUniversalHostility, day,
                EnhancedHordesTweaksConfig.universalHostilityDaysBeforeActivation, "");

        return 1;
    }

    private static void hordeSight(CommandSourceStack source, long day) {
        boolean overTime = EnhancedHordesTweaksConfig.hordeSightIncreaseOverTime;
        int threshold = EnhancedHordesTweaksConfig.hordeSightDaysBeforeActivation;

        Component status;
        if (!overTime) {
            status = Component.literal("disabled").withStyle(ChatFormatting.DARK_GRAY);
        } else if (day < threshold) {
            status = Component.literal("waiting (day " + threshold + ")").withStyle(ChatFormatting.YELLOW);
        } else {
            status = Component.literal("active").withStyle(ChatFormatting.GREEN);
        }

        long bonus = scaled(EnhancedHordesTweaksConfig.hordeSightRangeBonus, overTime, threshold,
                EnhancedHordesTweaksConfig.hordeSightIncreaseIntervalDays,
                EnhancedHordesTweaksConfig.hordeSightIncreaseAmount, 256, day);

        send(source, Component.literal("Horde Sight: ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal("Increase Over Time: ").withStyle(ChatFormatting.WHITE))
                .append(status)
                .append(Component.literal(". Increase Amount: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("+" + EnhancedHordesTweaksConfig.hordeSightIncreaseAmount)
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal(". Sight Range Bonus: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(bonus)).withStyle(ChatFormatting.AQUA)));
    }

    private static long followTime(long day) {
        int base = EnhancedHordesTweaksConfig.hordeDeterminationFollowTimeMinutes;
        if (base <= 0) return 0;
        return scaled(base,
                EnhancedHordesTweaksConfig.hordeDeterminationTimeIncreaseOverTime,
                EnhancedHordesTweaksConfig.hordeDeterminationDaysBeforeActivation,
                EnhancedHordesTweaksConfig.hordeDeterminationTimeIncreaseIntervalDays,
                EnhancedHordesTweaksConfig.hordeDeterminationTimeIncreaseAmount, 1440, day);
    }

    private static long scaled(long base, boolean overTime, int daysBefore, int interval, int amount, long max, long day) {
        if (!overTime || day < daysBefore) return Math.min(base, max);
        long increments = (day - daysBefore) / Math.max(1, interval);
        return Math.min(base + increments * amount, max);
    }

    private static void feature(CommandSourceStack source, String name, boolean enabled, long day,
                                int threshold, String detail) {
        if (!enabled) {
            send(source, Component.literal(name + ": ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("disabled").withStyle(ChatFormatting.DARK_GRAY)));
            return;
        }
        boolean active = day >= threshold;
        Component status = active
                ? Component.literal("active").withStyle(ChatFormatting.GREEN)
                : Component.literal("waiting (day " + threshold + ")").withStyle(ChatFormatting.YELLOW);
        Component line = Component.literal(name + ": ").withStyle(ChatFormatting.WHITE).append(status);
        if (detail != null && !detail.isEmpty()) {
            line = line.copy().append(Component.literal(" [" + detail + "]").withStyle(ChatFormatting.AQUA));
        }
        send(source, line);
    }

    private static void info(CommandSourceStack source, String name, String value) {
        send(source, Component.literal(name + ": ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(value).withStyle(ChatFormatting.AQUA)));
    }

    private static String onOff(boolean value) {
        return value ? "on" : "off";
    }

    private static void send(CommandSourceStack source, Component component) {
        source.sendSuccess(() -> component, false);
    }
}
