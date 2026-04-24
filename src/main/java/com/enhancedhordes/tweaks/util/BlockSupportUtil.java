package com.enhancedhordes.tweaks.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class BlockSupportUtil {

    private BlockSupportUtil() {}

    /**
     * Returns true if breaking the block at {@code pos} would cause any adjacent block
     * to fail its {@code canSurvive} check (e.g. the block below a door, the wall a
     * torch/banner is attached to, the block a carpet sits on), or if the block above
     * is a bed/door/tall-plant half that would lose its support.
     *
     * When {@code current} is itself a two-block object (door/bed/tall plant), its own
     * partner position is exempted from the orphan check — callers (see
     * {@code HordeMentalityHandler.breakBlock}) are expected to clean up the partner
     * explicitly with drops suppressed, so it is not "orphaned" in the drop-producing
     * sense.
     *
     * Uses a silent temporary air substitution with flags
     * {@code UPDATE_KNOWN_SHAPE | UPDATE_SUPPRESS_DROPS} (48):
     *   - UPDATE_KNOWN_SHAPE (16) skips {@code markAndNotifyBlock}'s updateShape fan-out,
     *     so the partner half of a two-block object doesn't destroy itself (and drop an
     *     item) while we're probing.
     *   - UPDATE_SUPPRESS_DROPS (32) defends against any updateOrDestroy path that still
     *     resolves to AIR.
     * Flag 0 was wrong — it let the partner's updateShape return AIR, triggering
     * {@code level.destroyBlock(partnerPos, true)} inside the check and dropping a full
     * bed/door item. That was the intermittent leak the user observed.
     */
    public static boolean wouldOrphanNeighbor(ServerLevel level, BlockPos pos, BlockState current) {
        BlockPos ownPartner = getDoubleBlockPartner(current, pos);
        int silentFlags = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), silentFlags);
        try {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                if (ownPartner != null && neighborPos.equals(ownPartner)) continue;
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.isAir()) continue;
                if (!neighborState.canSurvive(level, neighborPos)) return true;
                // Beds and doors above the target count as "supported by this block"
                // even when canSurvive is a no-op for them (beds don't require support,
                // but players still treat the block under them as load-bearing).
                if (dir == Direction.UP && isTwoBlockObject(neighborState)) return true;
            }
            return false;
        } finally {
            level.setBlock(pos, current, silentFlags);
        }
    }

    private static boolean isTwoBlockObject(BlockState state) {
        return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || state.hasProperty(BlockStateProperties.BED_PART);
    }

    /**
     * Returns the partner half position for doors, tall plants, beds, or null
     * if {@code state} is not a two-block object.
     */
    public static BlockPos getDoubleBlockPartner(BlockState state, BlockPos pos) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            return half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)) {
            BedPart part = state.getValue(BlockStateProperties.BED_PART);
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            return part == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
        }
        return null;
    }
}
