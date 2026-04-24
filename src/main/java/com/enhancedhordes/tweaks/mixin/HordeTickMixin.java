package com.enhancedhordes.tweaks.mixin;

import com.enhancedhordes.tweaks.config.EnhancedHordesTweaksConfig;
import com.enhancedhordes.tweaks.util.BlockSupportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * All redirects into HordeTickProcedure.execute (private overload).
 *
 * horde_hoard-1.3.1 was compiled with an older SRG scheme. Forge's remapper
 * translates those old SRGs to Mojang official names at load time, so by the
 * time Mixin processes the class the call sites carry official names.
 *
 * Confirmed mappings (via srg_to_official_1.20.1.tsrg):
 *   m_20254_ → setSecondsOnFire(I)V   (fire spread, offset ~1248)
 *   m_20256_ → setDeltaMovement(Vec3)V (baby throw ordinal 0, push ordinal 1)
 *   m_7292_  → addEffect(MobEffectInstance)Z (speed boost, ordinals 0-3)
 *   m_5776_  → isClientSide()Z         (NOT isDay — all 5 calls are server guards)
 * There is NO isDay() call in execute; EH applies speed whenever on-fire server-side.
 *
 * @At targets use SRG names because ModLauncher in this Forge version reports
 * `naming: srg` at runtime — method references in the bytecode remain as SRG
 * (m_XXXXX_) identifiers when Mixin processes the class. Using Mojang names here
 * produces "Scanned 1 target(s), (0/1) succeeded" injection failures.
 */
@Mixin(targets = "net.mcreator.horde_hoard.procedures.HordeTickProcedure", remap = false)
public class HordeTickMixin {

    private static final String EXECUTE =
            "execute(Lnet/minecraftforge/eventbus/api/Event;" +
            "Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V";

    // The entity currently being processed by HordeTickProcedure.execute. Captured at
    // HEAD and cleared at RETURN so redirects into vanilla calls (which don't carry
    // the tick entity as an argument) can still discriminate on it — used for the
    // baby-mob gates on block breaking. Server thread only; no synchronization needed.
    private static final ThreadLocal<Entity> CURRENT_ENTITY = new ThreadLocal<>();

    @Inject(method = EXECUTE, at = @At("HEAD"), remap = false)
    private static void captureCurrentEntity(Event ev, LevelAccessor level, double x, double y, double z,
                                             Entity entity, CallbackInfo ci) {
        CURRENT_ENTITY.set(entity);
    }

    @Inject(method = EXECUTE, at = @At("RETURN"), remap = false)
    private static void clearCurrentEntity(Event ev, LevelAccessor level, double x, double y, double z,
                                           Entity entity, CallbackInfo ci) {
        CURRENT_ENTITY.remove();
    }

    // -----------------------------------------------------------------------
    // Fire spreading — nearby entity ignited via setSecondsOnFire when source is on fire
    // -----------------------------------------------------------------------

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;m_20254_(I)V",
            remap = false),
        require = 1)
    private static void redirectFireSpread(Entity nearbyEntity, int seconds) {
        if (!EnhancedHordesTweaksConfig.hordeFireSpread) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                nearbyEntity.level(), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) return;
        nearbyEntity.setSecondsOnFire(seconds);
    }

    // -----------------------------------------------------------------------
    // Baby throw — ordinal 0 is the baby-mob launch; ordinal 1 is normal push
    // -----------------------------------------------------------------------

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V",
            remap = false,
            ordinal = 0),
        require = 1)
    private static void redirectBabyThrow(Entity entity, Vec3 velocity) {
        if (!EnhancedHordesTweaksConfig.hordeBabyThrow) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                entity.level(), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) return;
        entity.setDeltaMovement(velocity);
    }

    // -----------------------------------------------------------------------
    // Fire speed — EH applies MOVEMENT_SPEED any time a mob is on fire (server-side).
    // We handle the on/off toggle and amplifier here.
    // -----------------------------------------------------------------------

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_7292_(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            remap = false),
        require = 1)
    private static boolean redirectAddEffect(LivingEntity entity, MobEffectInstance effectInstance) {
        if (effectInstance.getEffect() != MobEffects.MOVEMENT_SPEED) {
            return entity.addEffect(effectInstance);
        }
        if (!EnhancedHordesTweaksConfig.hordeFireSpeedBoost) {
            return false;
        }
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                entity.level(), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) {
            return false;
        }
        if (!EnhancedHordesTweaksConfig.hordeBabyFireSpeedBoost
                && entity instanceof Mob mob && mob.isBaby()) {
            return false;
        }
        int amp = EnhancedHordesTweaksConfig.hordeFireSpeedAmplifier;
        if (amp != effectInstance.getAmplifier()) {
            return entity.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED, effectInstance.getDuration(), amp));
        }
        return entity.addEffect(effectInstance);
    }

    // -----------------------------------------------------------------------
    // Block breaking — EH calls Block.dropResources then LevelAccessor.destroyBlock
    // sequentially at the end of execute (offsets ~1508 and ~1541). We gate both on
    // enableHordeBlockBreaking; when protectSupportingBlocks is true we also block
    // breaks that would orphan a neighbor (e.g. dirt under a door/bed). The datapack
    // tag-override path doesn't reliably win over EH's shipped tag, so this mixin is
    // the authoritative off-switch.
    // -----------------------------------------------------------------------

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;m_49892_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            remap = false),
        require = 1)
    private static void redirectBlockDropResources(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity blockEntity) {
        if (shouldAllowEHBreak(level, pos, state)) {
            Block.dropResources(state, level, pos, blockEntity);
        }
    }

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;m_46961_(Lnet/minecraft/core/BlockPos;Z)Z",
            remap = false),
        require = 1)
    private static boolean redirectLevelDestroyBlock(LevelAccessor level, BlockPos pos, boolean drop) {
        BlockState state = level.getBlockState(pos);
        if (!shouldAllowEHBreak(level, pos, state)) return false;
        return level.destroyBlock(pos, drop);
    }

    private static boolean shouldAllowEHBreak(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!EnhancedHordesTweaksConfig.enableHordeBlockBreaking) return false;
        if (level instanceof net.minecraft.world.level.Level concrete
                && !EnhancedHordesTweaksConfig.daysElapsedReached(
                        concrete, EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) {
            return false;
        }
        Entity currentEntity = CURRENT_ENTITY.get();
        if (!EnhancedHordesTweaksConfig.hordeBabyBlockBreaking
                && currentEntity instanceof Mob mob && mob.isBaby()) {
            return false;
        }
        if (EnhancedHordesTweaksConfig.hordeMentalityProtectSupportingBlocks
                && level instanceof ServerLevel serverLevel
                && BlockSupportUtil.wouldOrphanNeighbor(serverLevel, pos, state)) {
            return false;
        }
        return true;
    }
}
