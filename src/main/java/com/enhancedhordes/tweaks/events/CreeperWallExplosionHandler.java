package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CreeperWallExplosionHandler {

    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final double WALL_TOUCH_REACH = 0.7;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!EnhancedHordesTweaksConfig.enableCreeperWallExplosion) return;
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        if (!(creeper.level() instanceof ServerLevel level)) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                level, EnhancedHordesTweaksConfig.creeperWallExplosionDaysBeforeActivation)) return;
        if (creeper.tickCount % CHECK_INTERVAL_TICKS != 0) return;

        LivingEntity target = creeper.getTarget();
        if (!(target instanceof Player player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        int maxDistance = EnhancedHordesTweaksConfig.creeperWallExplosionDistance;
        if (creeper.distanceToSqr(player) > (double) maxDistance * maxDistance) return;

        if (creeper.getSwellDir() > 0) return;

        if (!isTouchingWallTowardPlayer(level, creeper, player)) return;
        if (canReachTarget(creeper, player)) return;

        creeper.ignite();
    }

    private static boolean canReachTarget(Creeper creeper, Player player) {
        PathNavigation nav = creeper.getNavigation();
        Path path = nav.createPath(player, 0);
        return path != null && path.canReach();
    }

    private static boolean isTouchingWallTowardPlayer(ServerLevel level, Creeper creeper, Player player) {
        Vec3 creeperBody = new Vec3(
                creeper.getX(),
                creeper.getY() + creeper.getBbHeight() * 0.5,
                creeper.getZ());

        double dx = player.getX() - creeper.getX();
        double dz = player.getZ() - creeper.getZ();
        double horizLen = Math.sqrt(dx * dx + dz * dz);
        if (horizLen < 1.0e-4) return false;
        double ux = dx / horizLen;
        double uz = dz / horizLen;

        double reach = creeper.getBbWidth() * 0.5 + WALL_TOUCH_REACH;
        Vec3 to = new Vec3(
                creeperBody.x + ux * reach,
                creeperBody.y,
                creeperBody.z + uz * reach);

        BlockHitResult hit = level.clip(new ClipContext(
                creeperBody, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                creeper));
        return hit.getType() == HitResult.Type.BLOCK;
    }
}
