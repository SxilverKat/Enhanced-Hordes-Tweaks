package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.compat.GameStagesCompat;
import com.enhancedhordes.tweaks.config.ConfigCache;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.mojang.brigadier.tree.CommandNode;
import net.mcreator.horde_hoard.init.EnhancedHordesModGameRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;


@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GameRuleHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> LOCKED_GAMERULES = Set.of(
            "hordeStacking", "hordeMultiplying", "ironGolemRegenPower"
    );

    @Nullable private static final Field COMMAND_NODE_CHILDREN;
    @Nullable private static final Field COMMAND_NODE_LITERALS;

    static {
        Field children = null;
        Field literals = null;
        try {
            children = CommandNode.class.getDeclaredField("children");
            literals  = CommandNode.class.getDeclaredField("literals");
            children.setAccessible(true);
            literals.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LOGGER.error("[Enhanced Hordes Tweaks] Could not cache CommandNode reflection fields — /gamerule locking will be unavailable: {}", e.getMessage());
        }
        COMMAND_NODE_CHILDREN = children;
        COMMAND_NODE_LITERALS = literals;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!EnhancedHordesTweaksConfig.disableGameruleCommands) return;

        if (COMMAND_NODE_CHILDREN == null || COMMAND_NODE_LITERALS == null) {
            LOGGER.warn("[Enhanced Hordes Tweaks] Skipping /gamerule locking — reflection fields unavailable.");
            return;
        }

        CommandNode<CommandSourceStack> gameruleNode =
                event.getDispatcher().getRoot().getChild("gamerule");

        if (gameruleNode == null) {
            LOGGER.warn("[Enhanced Hordes Tweaks] Could not find /gamerule command node.");
            return;
        }

        for (String ruleName : LOCKED_GAMERULES) {
            removeChildNode(gameruleNode, ruleName);
        }

        LOGGER.info("[Enhanced Hordes Tweaks] Removed Enhanced Hordes game rule nodes from /gamerule.");
    }

    @SuppressWarnings("unchecked")
    private static void removeChildNode(CommandNode<?> parent, String name) {
        try {
            ((Map<String, ?>) COMMAND_NODE_CHILDREN.get(parent)).remove(name);
            ((Map<String, ?>) COMMAND_NODE_LITERALS.get(parent)).remove(name);
        } catch (Exception e) {
            LOGGER.error("[Enhanced Hordes Tweaks] Failed to remove /gamerule {} node: {}", name, e.getMessage());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLevelLoadFriendlyFire(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        PlayerTeam team = level.getScoreboard().getPlayerTeam("intelligentmobs");
        if (team != null) {
            team.setAllowFriendlyFire(EnhancedHordesTweaksConfig.enableFriendlyFire);
            LOGGER.info("[Enhanced Hordes Tweaks] Set intelligentmobs team friendlyFire={}.", EnhancedHordesTweaksConfig.enableFriendlyFire);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (EnhancedHordesTweaksConfig.allowFriendlyFireToHordeMobs) return;

        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;

        if (!ConfigCache.isHordeMob(event.getEntity().getType())) return;
        if (!ConfigCache.isIntelligentTeamMob(attacker.getType())) return;

        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickDashDistance(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        LivingEntity target = mob.getTarget();
        if (target == null) return;

        if (!ConfigCache.isIntelligentTeamMob(mob.getType())) return;

        int minDist = EnhancedHordesTweaksConfig.intelligentMobMinDashDistance;
        if (mob.distanceToSqr(target) >= (double)(minDist * minDist)) return;

        Vec3 velocity = mob.getDeltaMovement();
        Vec3 horizVelocity = new Vec3(velocity.x, 0, velocity.z);
        if (horizVelocity.lengthSqr() < 0.0001) return;
        Vec3 mobToTarget = target.position().subtract(mob.position());
        if (mobToTarget.lengthSqr() < 0.0001) return;
        if (horizVelocity.normalize().dot(mobToTarget.normalize()) < 0) {
            mob.setDeltaMovement(0, velocity.y, 0);
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        GameRules rules = level.getGameRules();

        rules.getRule(EnhancedHordesModGameRules.HORDESTACKING)
                .set(EnhancedHordesTweaksConfig.enableHordeStacking, null);
        rules.getRule(EnhancedHordesModGameRules.HORDEMULTIPLYING)
                .set(EnhancedHordesTweaksConfig.enableHordeMultiplying, null);
        rules.getRule(EnhancedHordesModGameRules.IRONGOLEMREGENPOWER)
                .set(EnhancedHordesTweaksConfig.enableIronGolemRegen
                        ? EnhancedHordesTweaksConfig.ironGolemRegenPower : 0, null);
        lastMultiplyingValue = null;
    }

    private static Boolean lastMultiplyingValue = null;

    @SubscribeEvent
    public static void onServerTickMultiplying(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % 20 != 0) return;

        ServerLevel overworld = event.getServer().overworld();
        boolean desired = EnhancedHordesTweaksConfig.enableHordeMultiplying
                && GameStagesCompat.anyPlayerHasStage(overworld, EnhancedHordesTweaksConfig.hordeMultiplyingStage);
        if (lastMultiplyingValue != null && lastMultiplyingValue == desired) return;

        overworld.getGameRules().getRule(EnhancedHordesModGameRules.HORDEMULTIPLYING).set(desired, null);
        lastMultiplyingValue = desired;
    }
}
