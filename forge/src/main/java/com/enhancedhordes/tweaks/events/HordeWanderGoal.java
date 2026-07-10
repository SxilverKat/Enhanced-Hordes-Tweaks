package com.enhancedhordes.tweaks.events;
import com.enhancedhordes.tweaks.util.VersionCompat;

import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class HordeWanderGoal extends Goal {

    private static final double LOOK_AHEAD = 10.0;
    private static final int PICK_RADIUS = 10;
    private static final int PICK_VERTICAL = 4;
    private static final double SPEED = 1.0;
    private static final int FALL_DROP_LIMIT = 4;

    private final PathfinderMob mob;
    private double targetX;
    private double targetY;
    private double targetZ;

    public HordeWanderGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!EnhancedHordesTweaksConfig.enableHordeWandering) return false;
        if (mob.getTarget() != null) return false;
        if (!mob.getNavigation().isDone()) return false;

        HordeWanderingHandler.GroupState state = HordeWanderingHandler.getGroup(mob);
        if (state == null) return false;

        Vec3 direction = state.direction;
        Vec3 anchor = state.centroid != null ? state.centroid : mob.position();
        Vec3 ahead = anchor.add(direction.x * LOOK_AHEAD, 0.0, direction.z * LOOK_AHEAD);

        boolean avoidWater = EnhancedHordesTweaksConfig.hordeWanderingAvoidWater;
        boolean avoidFalls = EnhancedHordesTweaksConfig.hordeWanderingAvoidFalls;

        Vec3 walkable;
        if (avoidWater && avoidFalls) {
            walkable = LandRandomPos.getPosTowards(mob, PICK_RADIUS, PICK_VERTICAL, ahead);
        } else {
            walkable = DefaultRandomPos.getPosTowards(mob, PICK_RADIUS, PICK_VERTICAL, ahead, Math.PI / 2.0);
            if (walkable != null) {
                //? if >=1.20.1 {
                BlockPos bp = BlockPos.containing(walkable);
                //?} else {
                /*BlockPos bp = new BlockPos(walkable.x, walkable.y, walkable.z);*/
                //?}
                if (avoidWater && !VersionCompat.level(mob).getFluidState(bp).isEmpty()) walkable = null;
                if (walkable != null && avoidFalls) {
                    BlockPos below = bp.below();
                    int drop = 0;
                    while (drop < FALL_DROP_LIMIT && VersionCompat.level(mob).isEmptyBlock(below)) {
                        drop++;
                        below = below.below();
                    }
                    if (drop >= FALL_DROP_LIMIT) walkable = null;
                }
            }
        }
        if (walkable == null) return false;

        targetX = walkable.x;
        targetY = walkable.y;
        targetZ = walkable.z;
        return true;
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(targetX, targetY, targetZ, SPEED);
    }

    @Override
    public boolean canContinueToUse() {
        if (!EnhancedHordesTweaksConfig.enableHordeWandering) return false;
        if (mob.getTarget() != null) return false;
        return !mob.getNavigation().isDone();
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
