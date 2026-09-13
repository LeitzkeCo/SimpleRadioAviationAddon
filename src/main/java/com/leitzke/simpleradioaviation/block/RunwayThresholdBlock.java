package com.leitzke.simpleradioaviation.block;

import com.leitzke.simpleradioaviation.block.entity.RunwayThresholdBlockEntity;
import com.leitzke.simpleradioaviation.item.PendriveLockItem;
import com.leitzke.simpleradioaviation.radar.RunwayNetwork;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class RunwayThresholdBlock extends BaseEntityBlock {
    public static final MapCodec<RunwayThresholdBlock> CODEC = simpleCodec(RunwayThresholdBlock::new);

    public RunwayThresholdBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RunwayThresholdBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        if (stack.getItem() instanceof PendriveLockItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        showLockedMessage(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        showLockedMessage(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void showLockedMessage(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof RunwayThresholdBlockEntity threshold
                && threshold.isLocked()) {
            player.displayClientMessage(Component.translatable(
                    "message.simpleradio_aviation.runway.locked_access"), true);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof RunwayThresholdBlockEntity threshold) {
            RunwayNetwork.remove(threshold);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
