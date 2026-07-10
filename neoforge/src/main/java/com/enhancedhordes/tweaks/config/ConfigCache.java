package com.enhancedhordes.tweaks.config;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConfigCache {

    private ConfigCache() {}

    private static volatile boolean dirty = true;

    private static EntitySet hordeMobs = EntitySet.EMPTY;
    private static EntitySet intelligentTeamMobs = EntitySet.EMPTY;
    private static EntitySet hostileMobs = EntitySet.EMPTY;
    private static EntitySet hostilityTargetMobs = EntitySet.EMPTY;

    private static Set<Block> hordeBreakableBlocks = Set.of();
    private static Set<TagKey<Block>> hordeBreakableTags = Set.of();

    @SuppressWarnings("unchecked")
    private static Set<Block>[] tierBlocks = new Set[]{Set.of(), Set.of(), Set.of(), Set.of()};
    @SuppressWarnings("unchecked")
    private static Set<TagKey<Block>>[] tierTags = new Set[]{Set.of(), Set.of(), Set.of(), Set.of()};
    private static Set<Block> blacklistBlocks = Set.of();
    private static Set<TagKey<Block>> blacklistTags = Set.of();

    public static void markDirty() {
        dirty = true;
    }

    private static void rebuildIfNeeded() {
        if (!dirty) return;
        synchronized (ConfigCache.class) {
            if (!dirty) return;
            rebuild();
            dirty = false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void rebuild() {
        hordeMobs = resolveEntities(EnhancedHordesTweaksConfig.hordeMobs);
        intelligentTeamMobs = resolveEntities(EnhancedHordesTweaksConfig.intelligentTeamMobs);
        hostileMobs = resolveEntities(EnhancedHordesTweaksConfig.hostileMobs);
        hostilityTargetMobs = resolveEntities(EnhancedHordesTweaksConfig.hostilityTargetMobs);

        Set<Block> hb = new HashSet<>();
        Set<TagKey<Block>> ht = new HashSet<>();
        splitBlocks(EnhancedHordesTweaksConfig.hordeBreakableBlocks, hb, ht);
        hordeBreakableBlocks = hb;
        hordeBreakableTags = ht;

        List<? extends String>[] tierLists = new List[]{
                EnhancedHordesTweaksConfig.hordeMentalityTier1Blocks,
                EnhancedHordesTweaksConfig.hordeMentalityTier2Blocks,
                EnhancedHordesTweaksConfig.hordeMentalityTier3Blocks,
                EnhancedHordesTweaksConfig.hordeMentalityTier4Blocks,
        };
        Set<Block>[] tb = new Set[4];
        Set<TagKey<Block>>[] tt = new Set[4];
        Set<Block> cumBlocks = new HashSet<>();
        Set<TagKey<Block>> cumTags = new HashSet<>();
        for (int t = 0; t < 4; t++) {
            splitBlocks(tierLists[t], cumBlocks, cumTags);
            tb[t] = new HashSet<>(cumBlocks);
            tt[t] = new HashSet<>(cumTags);
        }
        tierBlocks = tb;
        tierTags = tt;

        Set<Block> bb = new HashSet<>();
        Set<TagKey<Block>> bt = new HashSet<>();
        splitBlocks(EnhancedHordesTweaksConfig.hordeMentalityBlacklistBlocks, bb, bt);
        blacklistBlocks = bb;
        blacklistTags = bt;
    }

    private static EntitySet resolveEntities(List<? extends String> list) {
        if (list == null || list.isEmpty()) return EntitySet.EMPTY;
        Set<EntityType<?>> ids = new HashSet<>();
        Set<TagKey<EntityType<?>>> tags = new HashSet<>();
        for (String entry : list) {
            if (entry == null) continue;
            if (entry.startsWith("#")) {
                ResourceLocation rl = ResourceLocation.tryParse(entry.substring(1));
                if (rl != null) tags.add(TagKey.create(Registries.ENTITY_TYPE, rl));
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(entry);
                if (rl != null && BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
                    if (type != null) ids.add(type);
                }
            }
        }
        return new EntitySet(ids, tags);
    }

    private static void splitBlocks(List<? extends String> list, Set<Block> ids, Set<TagKey<Block>> tags) {
        if (list == null) return;
        for (String entry : list) {
            if (entry == null) continue;
            if (entry.startsWith("#")) {
                ResourceLocation rl = ResourceLocation.tryParse(entry.substring(1));
                if (rl != null) tags.add(TagKey.create(Registries.BLOCK, rl));
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(entry);
                if (rl != null && BuiltInRegistries.BLOCK.containsKey(rl)) {
                    Block b = BuiltInRegistries.BLOCK.get(rl);
                    if (b != null) ids.add(b);
                }
            }
        }
    }

    public static boolean isHordeMob(EntityType<?> type) {
        rebuildIfNeeded();
        return hordeMobs.contains(type);
    }

    public static boolean isIntelligentTeamMob(EntityType<?> type) {
        rebuildIfNeeded();
        return intelligentTeamMobs.contains(type);
    }

    public static boolean isHostileMob(EntityType<?> type) {
        rebuildIfNeeded();
        return hostileMobs.contains(type);
    }

    public static boolean isHostilityTarget(EntityType<?> type) {
        rebuildIfNeeded();
        return hostilityTargetMobs.contains(type);
    }

    public static boolean hasHordeMobs() {
        rebuildIfNeeded();
        return !hordeMobs.isEmpty();
    }

    public static boolean isHordeBreakable(BlockState state) {
        rebuildIfNeeded();
        if (hordeBreakableBlocks.contains(state.getBlock())) return true;
        for (TagKey<Block> tag : hordeBreakableTags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    public static boolean isMentalityBlacklisted(BlockState state) {
        rebuildIfNeeded();
        if (blacklistBlocks.isEmpty() && blacklistTags.isEmpty()) return false;
        if (blacklistBlocks.contains(state.getBlock())) return true;
        for (TagKey<Block> tag : blacklistTags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    public static boolean isBreakableAtTier(BlockState state, int qualifiedTier) {
        rebuildIfNeeded();
        if (qualifiedTier < 1) return false;
        int idx = Math.min(qualifiedTier, 4) - 1;
        if (tierBlocks[idx].contains(state.getBlock())) return true;
        for (TagKey<Block> tag : tierTags[idx]) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    private record EntitySet(Set<EntityType<?>> ids, Set<TagKey<EntityType<?>>> tags) {

        static final EntitySet EMPTY = new EntitySet(Set.of(), Set.of());

        boolean contains(EntityType<?> type) {
            if (ids.contains(type)) return true;
            for (TagKey<EntityType<?>> tag : tags) {
                if (type.is(tag)) return true;
            }
            return false;
        }

        boolean isEmpty() {
            return ids.isEmpty() && tags.isEmpty();
        }
    }
}
