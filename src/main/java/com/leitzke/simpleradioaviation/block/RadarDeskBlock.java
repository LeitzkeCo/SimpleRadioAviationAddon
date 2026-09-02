package com.leitzke.simpleradioaviation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RadarDeskBlock extends Block {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final EnumProperty<DeskPart> PART = EnumProperty.create("part", DeskPart.class);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public RadarDeskBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(PART, DeskPart.LEFT)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        Direction secondPartDirection = facing.getClockWise();

        BlockPos secondPartPos = context.getClickedPos().relative(secondPartDirection);

        if (!context.getLevel().getBlockState(secondPartPos).canBeReplaced()) {
            return null;
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, DeskPart.LEFT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {

        if (!level.isClientSide) {
            Direction secondPartDirection = state.getValue(FACING).getClockWise();

            level.setBlock(
                    pos.relative(secondPartDirection),
                    state.setValue(PART, DeskPart.RIGHT),
                    Block.UPDATE_ALL
            );
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
                                  BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {

        Direction secondPartDirection = state.getValue(FACING).getClockWise();

        Direction connectedDirection = state.getValue(PART) == DeskPart.LEFT
                ? secondPartDirection
                : secondPartDirection.getOpposite();

        if (direction == connectedDirection
                && (!neighborState.is(this)
                || neighborState.getValue(PART) == state.getValue(PART))) {

            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}