package com.enhancedhordes.tweaks.mixin;
import com.enhancedhordes.tweaks.util.VersionCompat;

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

@Mixin(targets = "net.mcreator.horde_hoard.procedures.HordeTickProcedure", remap = false)
public class HordeTickMixin {

    private static final String EXECUTE =
            "execute(Lnet/minecraftforge/eventbus/api/Event;" +
            "Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V";

    private static final ThreadLocal<java.lang.ref.WeakReference<Entity>> CURRENT_ENTITY = new ThreadLocal<>();

    @Inject(method = EXECUTE, at = @At("HEAD"), remap = false)
    private static void captureCurrentEntity(Event ev, LevelAccessor level, double x, double y, double z,
                                             Entity entity, CallbackInfo ci) {
        CURRENT_ENTITY.set(new java.lang.ref.WeakReference<>(entity));
    }

    @Inject(method = EXECUTE, at = @At("RETURN"), remap = false)
    private static void clearCurrentEntity(Event ev, LevelAccessor level, double x, double y, double z,
                                           Entity entity, CallbackInfo ci) {
        CURRENT_ENTITY.remove();
    }

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;m_20254_(I)V",
            remap = false),
        require = 1)
    private static void redirectFireSpread(Entity nearbyEntity, int seconds) {
        if (!EnhancedHordesTweaksConfig.hordeFireSpread) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                VersionCompat.level(nearbyEntity), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) return;
        nearbyEntity.setSecondsOnFire(seconds);
    }

    @Redirect(method = EXECUTE,
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V",
            remap = false,
            ordinal = 0),
        require = 1)
    private static void redirectBabyThrow(Entity entity, Vec3 velocity) {
        if (!EnhancedHordesTweaksConfig.hordeBabyThrow) return;
        if (!EnhancedHordesTweaksConfig.daysElapsedReached(
                VersionCompat.level(entity), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) return;
        entity.setDeltaMovement(velocity);
    }

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
                VersionCompat.level(entity), EnhancedHordesTweaksConfig.featuresDaysBeforeActivation)) {
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
        java.lang.ref.WeakReference<Entity> ref = CURRENT_ENTITY.get();
        Entity currentEntity = ref == null ? null : ref.get();
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
