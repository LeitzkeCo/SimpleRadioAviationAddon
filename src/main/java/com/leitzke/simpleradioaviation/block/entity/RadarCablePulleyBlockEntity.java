package com.leitzke.simpleradioaviation.block.entity;

import com.leitzke.simpleradioaviation.radar.RadarCableNetwork;
import com.leitzke.simpleradioaviation.radar.RadarCableNode;
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

public class RadarCablePulleyBlockEntity extends BlockEntity implements RadarCableNode {
    private static final String CONNECTIONS_TAG = "CableConnections";
    private Set<BlockPos> connections = Set.of();

    public RadarCablePulleyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_CABLE_PULLEY, pos, state);
    }

    @Override public BlockPos getCableNodePosition() { return worldPosition; }
    @Override public Level getCableNodeLevel() { return level; }
    @Override public Set<BlockPos> getCableConnections() { return connections; }

    @Override
    public void setCableConnections(Set<BlockPos> connections) {
        this.connections = connections.stream().limit(RadarCableNetwork.MAX_LINKS_PER_NODE)
                .map(BlockPos::immutable).collect(java.util.stream.Collectors.toUnmodifiableSet());
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        connections = Arrays.stream(tag.getLongArray(CONNECTIONS_TAG))
                .limit(RadarCableNetwork.MAX_LINKS_PER_NODE).mapToObj(BlockPos::of)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLongArray(CONNECTIONS_TAG, connections.stream().mapToLong(BlockPos::asLong).toArray());
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override @Nullable public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

}
