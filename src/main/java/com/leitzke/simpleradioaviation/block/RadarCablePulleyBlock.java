package com.leitzke.simpleradioaviation.block;

import com.leitzke.simpleradioaviation.block.entity.RadarCablePulleyBlockEntity;
import com.leitzke.simpleradioaviation.radar.RadarCableNetwork;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RadarCablePulleyBlock extends BaseEntityBlock {
    public static final MapCodec<RadarCablePulleyBlock> CODEC = simpleCodec(RadarCablePulleyBlock::new);

    public RadarCablePulleyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadarCablePulleyBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RadarCablePulleyBlockEntity pulley) {
            RadarCableNetwork.disconnectAll(pulley);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
