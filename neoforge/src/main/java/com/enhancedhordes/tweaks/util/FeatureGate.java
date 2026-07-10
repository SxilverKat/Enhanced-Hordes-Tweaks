package com.enhancedhordes.tweaks.util;

import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public final class FeatureGate {

    private FeatureGate() {}

    public static boolean nightBlocked(Level level) {
        return EnhancedHordesTweaksConfig.globalNightOnly && level.isDay();
    }

    public static boolean blocked(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return false;
        if (nightBlocked(level)) return true;
        BlockPos pos = mob.blockPosition();
        if (lightBlocked(level, pos)) return true;
        return graceBlocked(level, pos);
    }

    public static boolean lightBlocked(ServerLevel level, BlockPos pos) {
        if (!EnhancedHordesTweaksConfig.lightLevelGating) return false;
        return level.getMaxLocalRawBrightness(pos) > EnhancedHordesTweaksConfig.maxActiveLightLevel;
    }

    public static boolean graceBlocked(ServerLevel level, BlockPos pos) {
        if (!EnhancedHordesTweaksConfig.enableGraceRadius) return false;
        double radiusSq = (double) EnhancedHordesTweaksConfig.graceRadius * EnhancedHordesTweaksConfig.graceRadius;

        if (EnhancedHordesTweaksConfig.graceUseWorldSpawn
                && level.getSharedSpawnPos().distSqr(pos) <= radiusSq) {
            return true;
        }
        if (EnhancedHordesTweaksConfig.graceUsePlayerSpawn) {
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                if (!level.dimension().equals(player.getRespawnDimension())) continue;
                BlockPos bed = player.getRespawnPosition();
                if (bed != null && bed.distSqr(pos) <= radiusSq) return true;
            }
        }
        return false;
    }
}
