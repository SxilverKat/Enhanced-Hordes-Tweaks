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

    public static boolean wouldOrphanNeighbor(ServerLevel level, BlockPos pos, BlockState current) {
        BlockPos ownPartner = getDoubleBlockPartner(current, pos);

        if (current.hasBlockEntity()) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                if (ownPartner != null && neighborPos.equals(ownPartner)) continue;
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.isAir()) continue;
                if (dir == Direction.UP && isTwoBlockObject(neighborState)) return true;
            }
            return false;
        }

        int silentFlags = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), silentFlags);
        try {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                if (ownPartner != null && neighborPos.equals(ownPartner)) continue;
                BlockState neighborState = level.getBlockState(neighborPos);
                if (neighborState.isAir()) continue;
                if (!neighborState.canSurvive(level, neighborPos)) return true;
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
