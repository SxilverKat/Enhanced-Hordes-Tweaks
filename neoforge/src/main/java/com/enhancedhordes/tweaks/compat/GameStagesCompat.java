package com.enhancedhordes.tweaks.compat;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class GameStagesCompat {

    private static final boolean LOADED = ModList.get().isLoaded("gamestages");
    private static final int CACHE_TTL_TICKS = 20;

    private static volatile boolean resolved;
    private static Method hasStageMethod;

    private static final Map<String, CacheEntry> ANY_PLAYER_CACHE = new ConcurrentHashMap<>();

    private GameStagesCompat() {}

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ANY_PLAYER_CACHE.clear();
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static boolean allows(Player player, String stage) {
        if (!EnhancedHordesTweaksConfig.enableGameStages) return true;
        if (stage == null || stage.isEmpty()) return true;
        if (!LOADED || player == null) return true;
        return hasStage(player, stage);
    }

    public static boolean anyPlayerHasStage(ServerLevel level, String stage) {
        if (!EnhancedHordesTweaksConfig.enableGameStages) return true;
        if (stage == null || stage.isEmpty()) return true;
        if (!LOADED || level == null) return true;

        int tick = level.getServer().getTickCount();
        CacheEntry cached = ANY_PLAYER_CACHE.get(stage);
        if (cached != null) {
            int age = tick - cached.tick;
            if (age >= 0 && age < CACHE_TTL_TICKS) return cached.value;
        }

        boolean result = false;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (hasStage(player, stage)) {
                result = true;
                break;
            }
        }
        ANY_PLAYER_CACHE.put(stage, new CacheEntry(tick, result));
        return result;
    }

    private static boolean hasStage(Player player, String stage) {
        Method method = resolve(player);
        if (method == null) return true;
        try {
            Object result = method.invoke(null, player, stage);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable t) {
            return true;
        }
    }

    private static Method resolve(Player player) {
        if (resolved) return hasStageMethod;
        synchronized (GameStagesCompat.class) {
            if (resolved) return hasStageMethod;
            try {
                Class<?> helper = Class.forName("net.darkhax.gamestages.GameStageHelper");
                for (Method m : helper.getMethods()) {
                    if (!m.getName().equals("hasStage")) continue;
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 2
                            && params[1] == String.class
                            && params[0].isAssignableFrom(player.getClass())
                            && m.getReturnType() == boolean.class) {
                        hasStageMethod = m;
                        break;
                    }
                }
            } catch (Throwable t) {
                hasStageMethod = null;
            }
            resolved = true;
            return hasStageMethod;
        }
    }

    private record CacheEntry(int tick, boolean value) {}
}
