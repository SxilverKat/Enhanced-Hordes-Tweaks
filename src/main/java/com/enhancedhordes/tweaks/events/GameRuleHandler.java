package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.mojang.brigadier.tree.CommandNode;
import net.mcreator.horde_hoard.init.HordeHoardModGameRules;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.reflect.Field;
import java.util.List;
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

    // Runs at LOWEST priority so EH's TeamMakerProcedure has already created the
    // 'intelligentmobs' team before we try to modify its friendlyFire setting.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLevelLoadFriendlyFire(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        PlayerTeam team = level.getScoreboard().getPlayerTeam("intelligentmobs");
        if (team != null) {
            team.setAllowFriendlyFire(EnhancedHordesTweaksConfig.enableFriendlyFire);
            LOGGER.info("[Enhanced Hordes Tweaks] Set intelligentmobs team friendlyFire={}.", EnhancedHordesTweaksConfig.enableFriendlyFire);
        }
        // If the team doesn't exist yet on this load (first-ever world load, EH may not have
        // run its handler yet), this is a no-op. EH will create it with friendlyFire=false;
        // on the next load our LOWEST-priority handler will correct it.
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (EnhancedHordesTweaksConfig.allowFriendlyFireToHordeMobs) return;

        // Find the entity actually responsible for the damage (unwrap projectile owners etc.)
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;

        // Victim must be a horde mob
        List<? extends String> hordeMobIds = EnhancedHordesTweaksConfig.hordeMobs;
        if (hordeMobIds == null) return;
        ResourceLocation victimId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (victimId == null || !hordeMobIds.contains(victimId.toString())) return;

        // Attacker must be an intelligent team mob
        List<? extends String> intelligentMobIds = EnhancedHordesTweaksConfig.intelligentTeamMobs;
        if (intelligentMobIds == null) return;
        ResourceLocation attackerId = ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType());
        if (attackerId == null || !intelligentMobIds.contains(attackerId.toString())) return;

        event.setCanceled(true);
    }

    // Suppresses intelligent mob dashes when the mob is too close to its target.
    // EH's dash logic applies a large away-from-target velocity impulse; at LOWEST
    // we zero the horizontal component when the mob is within minDashDistance.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingTickDashDistance(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        List<? extends String> intelligentMobIds = EnhancedHordesTweaksConfig.intelligentTeamMobs;
        if (intelligentMobIds == null) return;
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (entityId == null || !intelligentMobIds.contains(entityId.toString())) return;

        LivingEntity target = mob.getTarget();
        if (target == null) return;

        int minDist = EnhancedHordesTweaksConfig.intelligentMobMinDashDistance;
        if (mob.distanceToSqr(target) >= (double)(minDist * minDist)) return;

        // Within minDashDistance — zero any horizontal velocity that is directed away from the target
        Vec3 velocity = mob.getDeltaMovement();
        Vec3 horizVelocity = new Vec3(velocity.x, 0, velocity.z);
        if (horizVelocity.lengthSqr() < 0.0001) return; // no significant horizontal movement
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

        // Config is authoritative on level load — sync in both directions so toggling
        // a config back to true actually restores the gamerule (the reverse direction
        // was missing, which left the rule stuck at false on re-enable).
        rules.getRule(HordeHoardModGameRules.HORDESTACKING)
                .set(EnhancedHordesTweaksConfig.enableHordeStacking, null);
        rules.getRule(HordeHoardModGameRules.HORDEMULTIPLYING)
                .set(EnhancedHordesTweaksConfig.enableHordeMultiplying, null);

        if (!EnhancedHordesTweaksConfig.enableIronGolemRegen) {
            rules.getRule(HordeHoardModGameRules.IRONGOLEMREGENPOWER).set(0, null);
        } else if (level.getGameTime() == 0L) {
            rules.getRule(HordeHoardModGameRules.IRONGOLEMREGENPOWER).set(EnhancedHordesTweaksConfig.ironGolemRegenPower, null);
        }
    }
}
