package com.leitzke.simpleradioaviation.block;

import com.leitzke.simpleradioaviation.block.entity.RadarDeskBlockEntity;
import com.leitzke.simpleradioaviation.item.RadarCableItem;
import com.leitzke.simpleradioaviation.radar.RadarCableNetwork;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RadarDeskBlock extends BaseEntityBlock {

    public static final MapCodec<RadarDeskBlock> CODEC = simpleCodec(RadarDeskBlock::new);

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final EnumProperty<DeskPart> PART = EnumProperty.create("part", DeskPart.class);

    private static final VoxelShape LEFT_NORTH_BASE_SHAPE = Shapes.or(
            Block.box(0, 1, 1, 15, 14, 15),
            Block.box(13, 0, 1, 15, 1, 3),
            Block.box(13, 0, 13, 15, 1, 15),
            Block.box(1, 2, 0, 14, 13, 1),
            Block.box(0, 14, 0, 16, 16, 16)
    );

    private static final VoxelShape RIGHT_NORTH_BASE_SHAPE = Shapes.or(
            Block.box(1, 1, 1, 16, 14, 15),
            Block.box(1, 0, 1, 3, 1, 3),
            Block.box(1, 0, 13, 3, 1, 15),
            Block.box(2, 2, 0, 15, 7, 1),
            Block.box(2, 8, 0, 15, 13, 1),
            Block.box(0, 14, 0, 16, 16, 16)
    );

    private static final VoxelShape LEFT_EAST_BASE_SHAPE = rotateClockwise(LEFT_NORTH_BASE_SHAPE);
    private static final VoxelShape LEFT_SOUTH_BASE_SHAPE = rotateClockwise(LEFT_EAST_BASE_SHAPE);
    private static final VoxelShape LEFT_WEST_BASE_SHAPE = rotateClockwise(LEFT_SOUTH_BASE_SHAPE);
    private static final VoxelShape RIGHT_EAST_BASE_SHAPE = rotateClockwise(RIGHT_NORTH_BASE_SHAPE);
    private static final VoxelShape RIGHT_SOUTH_BASE_SHAPE = rotateClockwise(RIGHT_EAST_BASE_SHAPE);
    private static final VoxelShape RIGHT_WEST_BASE_SHAPE = rotateClockwise(RIGHT_SOUTH_BASE_SHAPE);

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
        Direction facing = context.getHorizontalDirection().getOpposite();
        Direction secondPartDirection = facing.getCounterClockWise();

        BlockPos secondPartPos = context.getClickedPos().relative(secondPartDirection);

        if (!context.getLevel().getBlockState(secondPartPos).canBeReplaced()) {
            return null;
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, DeskPart.LEFT);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        // Continue to the cable item's useOn method. Every other held item is
        // consumed by the desk so it cannot place a block through the furniture.
        if (stack.getItem() instanceof RadarCableItem
                || stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof DataLinkBlock) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {

        if (!level.isClientSide) {
            Direction secondPartDirection = state.getValue(FACING).getCounterClockWise();

            level.setBlock(
                    pos.relative(secondPartDirection),
                    state.setValue(PART, DeskPart.RIGHT),
                    Block.UPDATE_ALL
            );
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        BlockPos connectedPos = pos.relative(getConnectedDirection(state));
        BlockState connectedState = level.getBlockState(connectedPos);

        if (connectedState.is(this)
                && connectedState.getValue(FACING) == state.getValue(FACING)
                && connectedState.getValue(PART) != state.getValue(PART)) {
            level.setBlock(
                    connectedPos,
                    Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            );
        }

        return state;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RadarDeskBlockEntity desk) {
            RadarCableNetwork.disconnectAll(desk);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
                                  BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {

        Direction connectedDirection = getConnectedDirection(state);

        if (direction == connectedDirection
                && (!neighborState.is(this)
                || neighborState.getValue(PART) == state.getValue(PART))) {

            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private static Direction getConnectedDirection(BlockState state) {
        Direction secondPartDirection = state.getValue(FACING).getCounterClockWise();

        return state.getValue(PART) == DeskPart.LEFT
                ? secondPartDirection
                : secondPartDirection.getOpposite();
    }

    public static BlockPos getControllerPos(BlockPos partPos, BlockState state) {
        return state.getValue(PART) == DeskPart.LEFT
                ? partPos
                : partPos.relative(getConnectedDirection(state));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == DeskPart.LEFT
                ? new RadarDeskBlockEntity(pos, state)
                : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        boolean left = state.getValue(PART) == DeskPart.LEFT;

        return switch (state.getValue(FACING)) {
            case NORTH -> left ? LEFT_NORTH_BASE_SHAPE : RIGHT_NORTH_BASE_SHAPE;
            case EAST -> left ? LEFT_EAST_BASE_SHAPE : RIGHT_EAST_BASE_SHAPE;
            case SOUTH -> left ? LEFT_SOUTH_BASE_SHAPE : RIGHT_SOUTH_BASE_SHAPE;
            case WEST -> left ? LEFT_WEST_BASE_SHAPE : RIGHT_WEST_BASE_SHAPE;
            default -> left ? LEFT_NORTH_BASE_SHAPE : RIGHT_NORTH_BASE_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == DeskPart.LEFT
                ? RenderShape.MODEL
                : RenderShape.INVISIBLE;
    }

    private static VoxelShape rotateClockwise(VoxelShape shape) {
        VoxelShape[] rotated = {Shapes.empty()};

        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                rotated[0] = Shapes.or(
                        rotated[0],
                        Shapes.box(1.0D - maxZ, minY, minX, 1.0D - minZ, maxY, maxX)
                )
        );

        return rotated[0];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}
