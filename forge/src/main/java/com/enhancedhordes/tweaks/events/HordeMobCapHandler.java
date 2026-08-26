package com.enhancedhordes.tweaks.events;
import com.enhancedhordes.tweaks.util.VersionCompat;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
//? if >=1.19.2 {
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
//?} else {
/*import net.minecraftforge.event.entity.EntityJoinWorldEvent;*/
//?}
//? if >=1.19.2 {
import net.minecraftforge.event.level.LevelEvent;
//?} else {
/*import net.minecraftforge.event.world.WorldEvent;*/
//?}
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HordeMobCapHandler {

    private static final int COUNT_CACHE_TTL_TICKS = 20;

    private static final class CachedCount {
        long refreshedAtTick;
        int count;
        boolean valid;
    }

    private static final Map<ResourceKey<Level>, CachedCount> COUNTS = new ConcurrentHashMap<>();

    @SubscribeEvent
    //? if >=1.19.2 {
    public static void onEntityJoin(EntityJoinLevelEvent event) {
    //?} else {
    /*public static void onEntityJoin(EntityJoinWorldEvent event) {*/
    //?}
        int cap = EnhancedHordesTweaksConfig.hordeMobCap;
        if (cap <= 0) return;
        //? if >=1.19.2 {
        if (event.getLevel().isClientSide()) return;
        //?} else {
        /*if (event.getWorld().isClientSide()) return;*/
        //?}
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!ConfigCache.isHordeMob(mob.getType())) return;
        if (!(VersionCompat.level(mob) instanceof ServerLevel level)) return;

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
    //? if >=1.19.2 {
    public static void onLevelUnload(LevelEvent.Unload event) {
    //?} else {
    /*public static void onLevelUnload(WorldEvent.Unload event) {*/
    //?}
        //? if >=1.19.2 {
        if (event.getLevel() instanceof ServerLevel level) {
        //?} else {
        /*if (event.getWorld() instanceof ServerLevel level) {*/
        //?}
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
