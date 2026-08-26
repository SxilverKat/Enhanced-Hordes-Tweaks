package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class HordeMobCapHandler {

    private static final int COUNT_CACHE_TTL_TICKS = 20;

    private static final class CachedCount {
        long refreshedAtTick;
        int count;
        boolean valid;
    }

    private static final Map<ResourceKey<Level>, CachedCount> COUNTS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        int cap = EnhancedHordesTweaksConfig.hordeMobCap;
        if (cap <= 0) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;
        if (!(mob.level() instanceof ServerLevel level)) return;

        CachedCount cache = COUNTS.computeIfAbsent(level.dimension(), k -> new CachedCount());
        long gameTime = level.getGameTime();
        if (!cache.valid || gameTime - cache.refreshedAtTick > COUNT_CACHE_TTL_TICKS) {
            cache.count = countHordeMobs(level);
            cache.refreshedAtTick = gameTime;
            cache.valid = true;
        }

        if (cache.count >= cap) {
            event.setCanceled(true);
        } else {
            cache.count++;
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            COUNTS.remove(level.dimension());
        }
    }

    private static int countHordeMobs(ServerLevel level) {
        int n = 0;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof Mob m && m.isAlive() && ConfigCache.isHordeMob(m.getType())) n++;
        }
        return n;
    }
}
