package com.leitzke.simpleradioaviation.block;

import com.leitzke.simpleradioaviation.block.entity.DataLinkBlockEntity;
import com.leitzke.simpleradioaviation.radar.DataLinkNetwork;
import com.leitzke.simpleradioaviation.item.PendriveLockItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class DataLinkBlock extends BaseEntityBlock {
    public static final MapCodec<DataLinkBlock> CODEC = simpleCodec(
            properties -> new DataLinkBlock(properties, Tier.BASIC));
    public static final EnumProperty<Role> ROLE = EnumProperty.create("role", Role.class);

    private final Tier tier;

    public DataLinkBlock(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(ROLE, Role.TRANSMITTER));
    }

    public Role role(BlockState state) { return state.getValue(ROLE); }
    public Tier tier() { return tier; }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override @Nullable public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DataLinkBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROLE);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        if (stack.getItem() instanceof PendriveLockItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof DataLinkBlockEntity device && device.isLocked()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable(
                    "message.simpleradio_aviation.data_link.locked_access"), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DataLinkBlockEntity device && device.isLocked()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable(
                    "message.simpleradio_aviation.data_link.locked_access"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DataLinkBlockEntity device
                && !level.isClientSide) {
            DataLinkNetwork.unregister(device);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public enum Role implements StringRepresentable {
        TRANSMITTER("transmitter"), RECEIVER("receiver");

        private final String serializedName;

        Role(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
    public enum Tier {
        BASIC(20), INTERMEDIATE(40), ADVANCED(60);
        private final int range;
        Tier(int range) { this.range = range; }
        public int range() { return range; }
    }
}
