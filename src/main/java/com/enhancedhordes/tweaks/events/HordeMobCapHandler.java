package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HordeMobCapHandler {

    private static final int COUNT_CACHE_TTL_TICKS = 20;

    private static final class CachedCount {
        long refreshedAtTick;
        int count;
    }

    private static final Map<ResourceKey<Level>, CachedCount> COUNTS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        int cap = EnhancedHordesTweaksConfig.hordeMobCap;
        if (cap <= 0) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!isHordeMob(mob)) return;
        if (!(mob.level() instanceof ServerLevel level)) return;

        CachedCount cache = COUNTS.computeIfAbsent(level.dimension(), k -> new CachedCount());
        long gameTime = level.getGameTime();
        if (gameTime - cache.refreshedAtTick > COUNT_CACHE_TTL_TICKS) {
            cache.count = countHordeMobs(level);
            cache.refreshedAtTick = gameTime;
        }

        if (cache.count >= cap) {
            event.setCanceled(true);
            return;
        }
        cache.count++;
    }

    private static int countHordeMobs(ServerLevel level) {
        int n = 0;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof Mob m && m.isAlive() && isHordeMob(m)) n++;
        }
        return n;
    }

    private static boolean isHordeMob(Mob mob) {
        List<? extends String> ids = EnhancedHordesTweaksConfig.hordeMobs;
        if (ids == null || ids.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return id != null && ids.contains(id.toString());
    }
}
