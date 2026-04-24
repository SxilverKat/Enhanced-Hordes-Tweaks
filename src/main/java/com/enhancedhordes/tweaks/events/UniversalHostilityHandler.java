package com.enhancedhordes.tweaks.events;

import com.enhancedhordes.tweaks.EnhancedHordesTweaksMod;
import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Attaches a targeting goal to every entity listed in hostileMobs when it joins
 * the world. The goal's predicate reads {@code hostilityTargetMobs} live, so
 * edits to the target list take effect on subsequent acquisitions without a
 * world reload (goals that have already locked onto a target keep that target
 * until normal AI clears it).
 *
 * Priority 2 sits above a zombie's generic idle goals but below its MeleeAttack
 * (priority 0/1), so chasing a target from this goal doesn't fight the attack
 * behaviour when one is available.
 */
@Mod.EventBusSubscriber(modid = EnhancedHordesTweaksMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class UniversalHostilityHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!EnhancedHordesTweaksConfig.enableUniversalHostility) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        List<? extends String> hostileIds = EnhancedHordesTweaksConfig.hostileMobs;
        if (hostileIds == null || hostileIds.isEmpty()) return;

        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (mobId == null || !hostileIds.contains(mobId.toString())) return;

        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                mob, LivingEntity.class, 10, true, false,
                UniversalHostilityHandler::isHostilityTarget));
    }

    /**
     * Reads {@code hostilityTargetMobs} from config every predicate evaluation so
     * the list can be tuned at runtime. Also enforces the day-gate so that when
     * {@code daysBeforeActivation > 0}, mobs with this goal already attached
     * won't start acquiring hostility targets until the threshold is reached.
     * Hot path but bounded by mob count.
     */
    public static boolean isHostilityTarget(LivingEntity candidate) {
        if (candidate == null) return false;
        if (!EnhancedHordesTweaksConfig.enableUniversalHostility) return false;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                candidate.level(), EnhancedHordesTweaksConfig.universalHostilityDaysBeforeActivation)) return false;
        List<? extends String> targetIds = EnhancedHordesTweaksConfig.hostilityTargetMobs;
        if (targetIds == null || targetIds.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(candidate.getType());
        return id != null && targetIds.contains(id.toString());
    }
}
