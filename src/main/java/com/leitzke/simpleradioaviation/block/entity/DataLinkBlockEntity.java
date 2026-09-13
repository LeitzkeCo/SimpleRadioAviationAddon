package com.leitzke.simpleradioaviation.block.entity;

import com.leitzke.simpleradioaviation.block.DataLinkBlock;
import com.leitzke.simpleradioaviation.radar.DataLinkNetwork;
import com.leitzke.simpleradioaviation.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class DataLinkBlockEntity extends BlockEntity {
    public static final int MAX_RECEIVERS = 3;
    private static final String CHANNEL_TAG = "Channel";
    private static final String RECEIVERS_TAG = "LinkedReceivers";
    private static final String TRANSMITTER_TAG = "LinkedTransmitter";
    private static final String LOCKED_TAG = "Locked";

    private String channel = "";
    private boolean locked;
    private final LinkedHashSet<BlockPos> linkedReceivers = new LinkedHashSet<>();
    @Nullable private BlockPos linkedTransmitter;

    public DataLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DATA_LINK, pos, state);
    }

    public DataLinkBlock block() { return (DataLinkBlock) getBlockState().getBlock(); }
    public DataLinkBlock.Role getRole() { return getBlockState().getValue(DataLinkBlock.ROLE); }
    public String getChannel() { return channel; }
    public boolean isLocked() { return locked; }
    public Set<BlockPos> getLinkedReceivers() { return Set.copyOf(linkedReceivers); }
    @Nullable public BlockPos getLinkedTransmitter() { return linkedTransmitter; }

    public void setChannel(String value) {
        channel = normalizeChannel(value);
        sync();
    }

    public void setRole(DataLinkBlock.Role role) {
        if (level != null && getRole() != role) {
            level.setBlock(worldPosition, getBlockState().setValue(DataLinkBlock.ROLE, role),
                    Block.UPDATE_ALL);
        }
    }

    public void lock() {
        if (!locked) {
            locked = true;
            sync();
        }
    }

    public boolean addReceiver(BlockPos position) {
        if (linkedReceivers.contains(position)) return true;
        if (linkedReceivers.size() >= MAX_RECEIVERS) return false;
        linkedReceivers.add(position.immutable());
        sync();
        return true;
    }

    public void removeReceiver(BlockPos position) {
        if (linkedReceivers.remove(position)) sync();
    }

    public void clearReceivers() {
        if (!linkedReceivers.isEmpty()) { linkedReceivers.clear(); sync(); }
    }

    public void setLinkedTransmitter(@Nullable BlockPos position) {
        linkedTransmitter = position == null ? null : position.immutable();
        sync();
    }

    public static String normalizeChannel(String value) {
        if (value == null) return "";
        String normalized = value.strip().toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9_-]", "");
        return normalized.substring(0, Math.min(16, normalized.length()));
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide) DataLinkNetwork.register(this);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) DataLinkNetwork.unregisterLoaded(this);
        super.setRemoved();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        channel = normalizeChannel(tag.getString(CHANNEL_TAG));
        locked = tag.getBoolean(LOCKED_TAG);
        linkedReceivers.clear();
        Arrays.stream(tag.getLongArray(RECEIVERS_TAG)).limit(MAX_RECEIVERS)
                .mapToObj(BlockPos::of).forEach(linkedReceivers::add);
        linkedTransmitter = tag.contains(TRANSMITTER_TAG)
                ? BlockPos.of(tag.getLong(TRANSMITTER_TAG)) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(CHANNEL_TAG, channel);
        tag.putBoolean(LOCKED_TAG, locked);
        tag.putLongArray(RECEIVERS_TAG, linkedReceivers.stream().mapToLong(BlockPos::asLong).toArray());
        if (linkedTransmitter != null) tag.putLong(TRANSMITTER_TAG, linkedTransmitter.asLong());
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override @Nullable public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
