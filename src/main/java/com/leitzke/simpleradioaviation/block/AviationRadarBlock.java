package com.leitzke.simpleradioaviation.block;

import com.leitzke.simpleradioaviation.block.entity.AviationRadarBlockEntity;
import com.leitzke.simpleradioaviation.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import com.leitzke.simpleradioaviation.item.RadarCableItem;
import com.leitzke.simpleradioaviation.radar.RadarCableNetwork;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AviationRadarBlock extends BaseEntityBlock {
    public static final MapCodec<AviationRadarBlock> CODEC = simpleCodec(AviationRadarBlock::new);

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final IntegerProperty OFFSET_X = IntegerProperty.create("offset_x", 0, 2);
    public static final IntegerProperty OFFSET_Y = IntegerProperty.create("offset_y", 0, 2);
    public static final IntegerProperty OFFSET_Z = IntegerProperty.create("offset_z", 0, 2);
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    public static final int HORIZONTAL_RANGE = 20_000;
    public static final long HORIZONTAL_RANGE_SQUARED = (long) HORIZONTAL_RANGE * HORIZONTAL_RANGE;

    private static final List<ShapeBox> BASE_SHAPE_BOXES = List.of(
            new ShapeBox(-0.5D, 0.0D, -0.5D, 0.5D, 1.0D, 0.5D),
            new ShapeBox(-1.0D, 0.5D, -0.5D, -0.5D, 1.0D, 0.5D),
            new ShapeBox(0.5D, 0.5D, -0.5D, 1.0D, 1.0D, 0.5D),
            new ShapeBox(-1.0D, 0.5D, -1.0D, 1.0D, 1.0D, -0.5D),
            new ShapeBox(-1.0D, 0.5D, 0.5D, 1.0D, 1.0D, 1.0D),
            new ShapeBox(-0.25D, 1.0D, -0.25D, 0.25D, 1.5D, 0.25D),
            new ShapeBox(-0.5D, 0.0D, 1.125D, 0.5D, 0.125D, 1.5D),
            new ShapeBox(-0.125D, 0.0D, -1.5D, 0.125D, 0.25D, -1.25D)
    );

    private record ShapeBox(double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ) {
    }

    public AviationRadarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OFFSET_X, 1)
                .setValue(OFFSET_Y, 0)
                .setValue(OFFSET_Z, 1)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos corePos = context.getClickedPos();

        if (!canPlaceStructure(context, corePos)) {
            return null;
        }

        // The exported Blockbench model faces opposite its stored horizontal
        // axis. Keeping the player's look direction here makes the visible front
        // of the radar face back toward the player.
        Direction facing = context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, facing);
    }

    private boolean canPlaceStructure(BlockPlaceContext context, BlockPos corePos) {
        Level level = context.getLevel();

        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos partPos = corePos.offset(x, y, z);

                    if (level.isOutsideBuildHeight(partPos)
                            || !level.getBlockState(partPos).canBeReplaced()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide) {
            return;
        }

        // Resolve the orientation once on the authoritative side. Every structure
        // part receives this exact state, so the renderer cannot fall back to the
        // default north-facing state on some placement directions.
        Direction facing = placer == null
                ? state.getValue(FACING)
                : placer.getDirection();
        BlockState structureState = state.setValue(FACING, facing);
        if (state.getValue(FACING) != facing) {
            level.setBlock(pos, structureState, Block.UPDATE_ALL);
        }

        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    level.setBlock(
                            pos.offset(x, y, z),
                            structureState
                                    .setValue(OFFSET_X, x + 1)
                                    .setValue(OFFSET_Y, y)
                                    .setValue(OFFSET_Z, z + 1),
                            Block.UPDATE_ALL
                    );
                }
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos corePos = getCorePos(pos, state);

            for (int y = 0; y <= 2; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos partPos = corePos.offset(x, y, z);

                        if (partPos.equals(pos)) {
                            continue;
                        }

                        BlockState partState = level.getBlockState(partPos);
                        if (partState.is(this) && getCorePos(partPos, partState).equals(corePos)) {
                            level.setBlock(
                                    partPos,
                                    Blocks.AIR.defaultBlockState(),
                                    Block.UPDATE_ALL
                                            | Block.UPDATE_SUPPRESS_DROPS
                            );
                        }
                    }
                }
            }

            // The redstone source sits outside the multiblock. Notify it again
            // after the whole structure is gone so dust recomputes its side shape
            // instead of retaining a stale connection to the removed input port.
            for (Direction side : List.of(
                    state.getValue(FACING).getClockWise(),
                    state.getValue(FACING).getCounterClockWise())) {
                BlockPos externalPort = corePos.relative(side, 2);
                level.updateNeighborsAt(externalPort, Blocks.AIR);
                level.updateNeighborsAt(externalPort.relative(side.getOpposite()), Blocks.AIR);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && isCore(state)
                && level.getBlockEntity(pos) instanceof AviationRadarBlockEntity radar) {
            RadarCableNetwork.disconnectAll(radar);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static boolean isCore(BlockState state) {
        return state.getValue(OFFSET_X) == 1
                && state.getValue(OFFSET_Y) == 0
                && state.getValue(OFFSET_Z) == 1;
    }

    public static BlockPos getCorePos(BlockPos partPos, BlockState state) {
        return partPos.offset(
                -(state.getValue(OFFSET_X) - 1),
                -state.getValue(OFFSET_Y),
                -(state.getValue(OFFSET_Z) - 1)
        );
    }

    public static Direction getRedstoneInputDirection(BlockState state) {
        // The model's redstone socket is opposite the copper data connector.
        return state.getValue(FACING).getClockWise();
    }

    public static Direction getCableOutputDirection(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    public static boolean hasRedstoneInput(Level level, BlockPos corePos, BlockState state) {
        Direction inputDirection = getRedstoneInputDirection(state);
        BlockPos inputPort = corePos.relative(inputDirection);

        // Only power received by the physical input part is valid. A broader
        // neighbor check around the outside source could accidentally capture
        // power routed near the copper data side.
        return level.hasNeighborSignal(inputPort);
    }

    private static boolean isRedstoneInputPort(BlockState state) {
        Direction inputDirection = getRedstoneInputDirection(state);
        return state.getValue(OFFSET_X) == 1 + inputDirection.getStepX()
                && state.getValue(OFFSET_Y) == 0
                && state.getValue(OFFSET_Z) == 1 + inputDirection.getStepZ();
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        // Redstone wire uses this to draw a connection toward the radar. Only the
        // actual input part advertises a connection, not all 27 structure blocks.
        return isRedstoneInputPort(state);
    }

    public static boolean isInsideHorizontalRange(BlockPos radarPos, BlockPos targetPos) {
        long deltaX = (long) targetPos.getX() - radarPos.getX();
        long deltaZ = (long) targetPos.getZ() - radarPos.getZ();
        return deltaX * deltaX + deltaZ * deltaZ <= HORIZONTAL_RANGE_SQUARED;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        if (stack.getItem() instanceof RadarCableItem
                || stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof DataLinkBlock) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Converts the model rotor angle into a clockwise world heading where north is
     * zero. The exported antenna's broad face is aligned with local +X, while the
     * renderer mirrors model Z. This matches the complete renderer transform, not
     * just the model part's local rotation.
     */
    public static float getWorldRotorHeading(BlockState state, float rotorRotation) {
        float heading = 360.0F - state.getValue(FACING).toYRot() + rotorRotation;
        heading %= 360.0F;
        return heading < 0.0F ? heading + 360.0F : heading;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isCore(state) ? new AviationRadarBlockEntity(pos, state) : null;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (!isCore(state)) {
            return null;
        }

        return createTickerHelper(
                type,
                ModBlockEntities.AVIATION_RADAR,
                level.isClientSide
                        ? AviationRadarBlockEntity::clientTick
                        : AviationRadarBlockEntity::serverTick
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return buildBaseShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return buildBaseShape(state);
    }

    private static VoxelShape buildBaseShape(BlockState state) {
        VoxelShape shape = Shapes.empty();
        // Match the mirrored handedness used by the block-entity renderer.
        double facingRotation = -state.getValue(FACING).toYRot() + 90.0D;

        for (ShapeBox box : BASE_SHAPE_BOXES) {
            shape = addRotatedAndClippedBox(shape, state, box, facingRotation);
        }

        return shape;
    }

    private static VoxelShape addRotatedAndClippedBox(VoxelShape shape, BlockState state,
                                                       ShapeBox box, double rotationDegrees) {
        double radians = Math.toRadians(rotationDegrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);

        double rotatedMinX = Double.POSITIVE_INFINITY;
        double rotatedMinZ = Double.POSITIVE_INFINITY;
        double rotatedMaxX = Double.NEGATIVE_INFINITY;
        double rotatedMaxZ = Double.NEGATIVE_INFINITY;

        double[] xValues = {box.minX(), box.maxX()};
        double[] zValues = {box.minZ(), box.maxZ()};

        for (double x : xValues) {
            for (double z : zValues) {
                double rotatedX = x * cosine + z * sine;
                double rotatedZ = -x * sine + z * cosine;
                rotatedMinX = Math.min(rotatedMinX, rotatedX);
                rotatedMinZ = Math.min(rotatedMinZ, rotatedZ);
                rotatedMaxX = Math.max(rotatedMaxX, rotatedX);
                rotatedMaxZ = Math.max(rotatedMaxZ, rotatedZ);
            }
        }

        int partX = state.getValue(OFFSET_X) - 1;
        int partY = state.getValue(OFFSET_Y);
        int partZ = state.getValue(OFFSET_Z) - 1;

        double minX = Math.max(rotatedMinX + 0.5D, partX);
        double minY = Math.max(box.minY(), partY);
        double minZ = Math.max(rotatedMinZ + 0.5D, partZ);
        double maxX = Math.min(rotatedMaxX + 0.5D, partX + 1.0D);
        double maxY = Math.min(box.maxY(), partY + 1.0D);
        double maxZ = Math.min(rotatedMaxZ + 0.5D, partZ + 1.0D);

        if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
            return shape;
        }

        return Shapes.or(
                shape,
                Block.box(
                        (minX - partX) * 16.0D,
                        (minY - partY) * 16.0D,
                        (minZ - partZ) * 16.0D,
                        (maxX - partX) * 16.0D,
                        (maxY - partY) * 16.0D,
                        (maxZ - partZ) * 16.0D
                )
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OFFSET_X, OFFSET_Y, OFFSET_Z, POWERED);
    }
}
