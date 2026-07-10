package com.enhancedhordes.tweaks.datapack;

import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnhancedHordesTweaksPackResources implements PackResources {

    private static final String PACK_ID = "builtin/enhanced_hordes_tweaks";

    private static final ResourceLocation LOC_HORDES =
            new ResourceLocation("forge", "tags/entity_types/hordes");
    private static final ResourceLocation LOC_INTELLIGENT_TEAMS =
            new ResourceLocation("forge", "tags/entity_types/intelligent_teams");
    private static final ResourceLocation LOC_LEAPING_MOBS =
            new ResourceLocation("forge", "tags/entity_types/leaping_mobs");
    private static final ResourceLocation LOC_GRAVE_ROBBERS =
            new ResourceLocation("forge", "tags/entity_types/horde_grave_robbers");
    private static final ResourceLocation LOC_INTELLIGENT_PIGLINS =
            new ResourceLocation("forge", "tags/entity_types/intelligent_piglin");
    private static final ResourceLocation LOC_HIDDEN_ZOMBIE_BLOCKS =
            new ResourceLocation("forge", "tags/blocks/hidden_zombie_blocks");
    private static final ResourceLocation LOC_HORDE_BREAKABLE =
            new ResourceLocation("forge", "tags/blocks/horde_breakable");

    private final Map<ResourceLocation, String> jsonCache;

    public EnhancedHordesTweaksPackResources() {
        Map<ResourceLocation, String> map = new HashMap<>();
        for (ResourceLocation loc : List.of(
                LOC_HORDES, LOC_INTELLIGENT_TEAMS, LOC_LEAPING_MOBS,
                LOC_GRAVE_ROBBERS, LOC_INTELLIGENT_PIGLINS,
                LOC_HIDDEN_ZOMBIE_BLOCKS, LOC_HORDE_BREAKABLE)) {
            String json = buildJson(loc);
            if (json != null) map.put(loc, json);
        }
        this.jsonCache = Collections.unmodifiableMap(map);
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.SERVER_DATA) return null;
        String json = resolveJson(location);
        if (json == null) return null;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return () -> new ByteArrayInputStream(bytes);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, PackResources.ResourceOutput output) {
        if (type != PackType.SERVER_DATA || !namespace.equals("forge")) return;

        checkAndOutput(LOC_HORDES, path, output);
        checkAndOutput(LOC_INTELLIGENT_TEAMS, path, output);
        checkAndOutput(LOC_LEAPING_MOBS, path, output);
        checkAndOutput(LOC_GRAVE_ROBBERS, path, output);
        checkAndOutput(LOC_INTELLIGENT_PIGLINS, path, output);
        checkAndOutput(LOC_HIDDEN_ZOMBIE_BLOCKS, path, output);
        checkAndOutput(LOC_HORDE_BREAKABLE, path, output);
    }

    private void checkAndOutput(ResourceLocation loc, String pathPrefix, PackResources.ResourceOutput output) {
        if (pathPrefix.isEmpty() || loc.getPath().startsWith(pathPrefix + "/")) {
            String json = resolveJson(loc);
            if (json != null) {
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                output.accept(loc, () -> new ByteArrayInputStream(bytes));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? Set.of("forge") : Set.of();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
        if (deserializer == PackMetadataSection.TYPE) {
            return (T) new PackMetadataSection(
                    Component.literal("Enhanced Hordes Tweaks data"),
                    15
            );
        }
        return null;
    }

    @Override
    public String packId() {
        return PACK_ID;
    }

    @Override
    public void close() {
    }

    @Nullable
    private String resolveJson(ResourceLocation location) {
        return jsonCache.get(location);
    }

    @Nullable
    private static String buildJson(ResourceLocation location) {
        if (LOC_HORDES.equals(location))
            return buildTagJson(EnhancedHordesTweaksConfig.enableHordeStacking || EnhancedHordesTweaksConfig.enableHordeMultiplying ? EnhancedHordesTweaksConfig.hordeMobs : List.of());
        if (LOC_INTELLIGENT_TEAMS.equals(location)) {
            if (!EnhancedHordesTweaksConfig.enableIntelligentTeams) return buildTagJson(List.of());
            List<? extends String> mobs = EnhancedHordesTweaksConfig.intelligentTeamMobs;
            if (!EnhancedHordesTweaksConfig.enableWitherSkeletonBowTactics)
                mobs = mobs.stream()
                        .filter(s -> !s.equals("minecraft:wither_skeleton"))
                        .toList();
            return buildTagJson(mobs);
        }
        if (LOC_LEAPING_MOBS.equals(location))
            return buildTagJson(EnhancedHordesTweaksConfig.enableLeapingMobs ? EnhancedHordesTweaksConfig.leapingMobs : List.of());
        if (LOC_GRAVE_ROBBERS.equals(location))
            return buildTagJson(EnhancedHordesTweaksConfig.enableHordeMultiplying ? EnhancedHordesTweaksConfig.graveRobbers : List.of());
        if (LOC_INTELLIGENT_PIGLINS.equals(location)) {
            if (!EnhancedHordesTweaksConfig.enableIntelligentPiglins) return buildTagJson(List.of());
            List<? extends String> piglins = EnhancedHordesTweaksConfig.intelligentPiglins;
            if (!EnhancedHordesTweaksConfig.enableZombifiedPiglinCrossbow)
                piglins = piglins.stream()
                        .filter(s -> !s.equals("minecraft:zombified_piglin"))
                        .toList();
            return buildTagJson(piglins);
        }
        if (LOC_HIDDEN_ZOMBIE_BLOCKS.equals(location))
            return buildTagJson(EnhancedHordesTweaksConfig.enableHiddenZombies ? EnhancedHordesTweaksConfig.hiddenZombieBlocks : List.of());
        if (LOC_HORDE_BREAKABLE.equals(location))
            return buildTagJson(EnhancedHordesTweaksConfig.enableHordeBlockBreaking ? EnhancedHordesTweaksConfig.hordeBreakableBlocks : List.of("minecraft:air"));
        return null;
    }

    private static String buildTagJson(List<? extends String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"replace\": true,\n  \"values\": [");
        if (values != null && !values.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < values.size(); i++) {
                String entry = values.get(i);
                if (entry.startsWith("#")) {
                    sb.append("    { \"id\": \"").append(entry).append("\", \"required\": false }");
                } else {
                    sb.append("    \"").append(entry).append("\"");
                }
                if (i < values.size() - 1) sb.append(",");
                sb.append("\n");
            }
        }
        sb.append("  ]\n}");
        return sb.toString();
    }
}
