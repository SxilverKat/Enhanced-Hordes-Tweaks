package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class HordeWanderingHandler {

    public static final double FORMATION_RADIUS = 8.0;
    private static final double FORMATION_RADIUS_SQ = FORMATION_RADIUS * FORMATION_RADIUS;

    private static final int REBUILD_INTERVAL_TICKS = 40;

    public static final class GroupState {
        public final UUID groupId;
        public Vec3 direction;
        public long directionSetTick;
        public int size;
        public Vec3 centroid;

        GroupState(UUID groupId, Vec3 direction, long directionSetTick, int size, Vec3 centroid) {
            this.groupId = groupId;
            this.direction = direction;
            this.directionSetTick = directionSetTick;
            this.size = size;
            this.centroid = centroid;
        }
    }

    private static final class DimensionState {
        final Map<UUID, GroupState> groupsById = new HashMap<>();
        final Map<UUID, UUID> mobToGroup = new HashMap<>();
    }

    private static final Map<ResourceKey<Level>, DimensionState> STATE = new ConcurrentHashMap<>();
    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof PathfinderMob mob)) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;
        mob.goalSelector.addGoal(6, new HordeWanderGoal(mob));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!EnhancedHordesTweaksConfig.enableHordeWandering
                || !isDayGateOpen(serverLevel)) {
            STATE.remove(serverLevel.dimension());
            return;
        }
        if (serverLevel.getGameTime() % REBUILD_INTERVAL_TICKS != 0) return;
        rebuild(serverLevel);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            STATE.remove(serverLevel.dimension());
        }
    }

    public static GroupState getGroup(LivingEntity mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return null;
        DimensionState dim = STATE.get(serverLevel.dimension());
        if (dim == null) return null;
        UUID groupId = dim.mobToGroup.get(mob.getUUID());
        if (groupId == null) return null;
        GroupState state = dim.groupsById.get(groupId);
        if (state == null) return null;
        if (state.size < EnhancedHordesTweaksConfig.hordeGroupMinimum) return null;
        return state;
    }

    private static boolean isDayGateOpen(ServerLevel level) {
        int threshold = EnhancedHordesTweaksConfig.hordeWanderingDaysBeforeActivation;
        return EnhancedHordesTweaksConfig.daysElapsedReached(level, threshold);
    }

    private static void rebuild(ServerLevel level) {
        long gameTime = level.getGameTime();
        long directionChangeTicks = (long) EnhancedHordesTweaksConfig.hordeWanderingDirectionChangeMinutes * 60L * 20L;

        List<PathfinderMob> mobs = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof PathfinderMob pm && pm.isAlive() && ConfigCache.isHordeMob(pm.getType())
                    && pm.getTarget() == null
                    && HordeDeterminationHandler.getFollowedPlayer(pm.getUUID()) == null) {
                mobs.add(pm);
            }
        }
        int n = mobs.size();

        DimensionState prev = STATE.get(level.dimension());
        DimensionState next = new DimensionState();
        if (n == 0) {
            STATE.put(level.dimension(), next);
            return;
        }

        int[] parent = new int[n];
        int[] size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }

        int cap = EnhancedHordesTweaksConfig.maxHordeGroup;

        for (int i = 0; i < n; i++) {
            PathfinderMob a = mobs.get(i);
            for (int j = i + 1; j < n; j++) {
                PathfinderMob b = mobs.get(j);
                if (a.distanceToSqr(b) > FORMATION_RADIUS_SQ) continue;
                int ra = find(parent, i);
                int rb = find(parent, j);
                if (ra == rb) continue;
                int combined = size[ra] + size[rb];
                if (cap > 0 && combined > cap) continue;
                if (size[ra] < size[rb]) {
                    int t = ra;
                    ra = rb;
                    rb = t;
                }
                parent[rb] = ra;
                size[ra] = combined;
            }
        }

        Map<Integer, List<Integer>> clusters = new HashMap<>();
        for (int i = 0; i < n; i++) {
            clusters.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(i);
        }

        Set<UUID> usedInheritedIds = new HashSet<>();
        for (Map.Entry<Integer, List<Integer>> entry : clusters.entrySet()) {
            List<Integer> indices = entry.getValue();

            double sx = 0, sy = 0, sz = 0;
            for (int idx : indices) {
                PathfinderMob m = mobs.get(idx);
                sx += m.getX();
                sy += m.getY();
                sz += m.getZ();
            }
            int sz_ = indices.size();
            Vec3 centroid = new Vec3(sx / sz_, sy / sz_, sz / sz_);

            Set<UUID> priorGroupIds = new HashSet<>();
            if (prev != null) {
                for (int idx : indices) {
                    UUID mobUuid = mobs.get(idx).getUUID();
                    UUID priorGroup = prev.mobToGroup.get(mobUuid);
                    if (priorGroup != null && prev.groupsById.containsKey(priorGroup)) {
                        priorGroupIds.add(priorGroup);
                    }
                }
            }

            GroupState state = null;
            if (priorGroupIds.size() == 1) {
                UUID inherited = priorGroupIds.iterator().next();
                if (usedInheritedIds.add(inherited)) {
                    GroupState old = prev.groupsById.get(inherited);
                    state = new GroupState(old.groupId, old.direction, old.directionSetTick, sz_, centroid);
                    if (gameTime - state.directionSetTick >= directionChangeTicks) {
                        state.direction = randomDirection();
                        state.directionSetTick = gameTime;
                    }
                }
            }
            if (state == null) {
                state = new GroupState(UUID.randomUUID(), randomDirection(), gameTime, sz_, centroid);
            }

            next.groupsById.put(state.groupId, state);
            for (int idx : indices) {
                next.mobToGroup.put(mobs.get(idx).getUUID(), state.groupId);
            }
        }

        Iterator<Map.Entry<UUID, GroupState>> it = next.groupsById.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().size == 0) it.remove();
        }

        STATE.put(level.dimension(), next);
    }

    private static Vec3 randomDirection() {
        double angle = RNG.nextDouble() * Math.PI * 2.0;
        return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }
}
