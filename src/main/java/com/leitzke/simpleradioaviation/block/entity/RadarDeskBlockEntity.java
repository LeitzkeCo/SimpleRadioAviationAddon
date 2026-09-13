package com.leitzke.simpleradioaviation.block.entity;

import com.leitzke.simpleradioaviation.registry.ModBlockEntities;
import com.leitzke.simpleradioaviation.radar.RadarCableNetwork;
import com.leitzke.simpleradioaviation.radar.RadarCableNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

public class RadarDeskBlockEntity extends BlockEntity implements RadarCableNode {
    private static final String CONNECTIONS_TAG = "CableConnections";
    private static final String SELECTED_RADAR_TAG = "SelectedRadar";
    private static final String RUNWAY_COUNT_TAG = "RunwayCount";
    public static final int MAX_RUNWAYS = 4;

    @Nullable
    private BlockPos selectedRadar;
    private Set<BlockPos> connections = Set.of();
    private final List<String> runwayCodes = new ArrayList<>();

    public RadarDeskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADAR_DESK, pos, state);
    }

    @Nullable
    public BlockPos getRadarPosition() {
        return selectedRadar;
    }

    public void setRadarPosition(@Nullable BlockPos radarPosition) {
        this.selectedRadar = radarPosition == null ? null : radarPosition.immutable();
        sync();
    }

    public List<String> getRunwayCodes() {
        return List.copyOf(runwayCodes);
    }

    public AddRunwayResult addRunwayCode(String code) {
        if (runwayCodes.contains(code)) return AddRunwayResult.DUPLICATE;
        if (runwayCodes.size() >= MAX_RUNWAYS) return AddRunwayResult.FULL;
        runwayCodes.add(code);
        sync();
        return AddRunwayResult.ADDED;
    }

    public boolean removeRunwayCode(String code) {
        boolean removed = runwayCodes.remove(code);
        if (removed) sync();
        return removed;
    }

    @Override public BlockPos getCableNodePosition() { return worldPosition; }
    @Override public net.minecraft.world.level.Level getCableNodeLevel() { return level; }
    @Override public Set<BlockPos> getCableConnections() { return connections; }

    @Override
    public void setCableConnections(Set<BlockPos> connections) {
        this.connections = connections.stream().limit(RadarCableNetwork.MAX_LINKS_PER_NODE)
                .map(BlockPos::immutable)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        selectedRadar = tag.contains(SELECTED_RADAR_TAG)
                ? BlockPos.of(tag.getLong(SELECTED_RADAR_TAG))
                : null;
        connections = Arrays.stream(tag.getLongArray(CONNECTIONS_TAG))
                .limit(RadarCableNetwork.MAX_LINKS_PER_NODE).mapToObj(BlockPos::of)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        runwayCodes.clear();
        int runwayCount = Math.min(MAX_RUNWAYS, tag.getInt(RUNWAY_COUNT_TAG));
        for (int index = 0; index < runwayCount; index++) {
            String code = com.leitzke.simpleradioaviation.radar.RunwayNetwork.normalizeCode(
                    tag.getString("RunwayCode" + index));
            if (!code.isBlank() && !runwayCodes.contains(code)) runwayCodes.add(code);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (selectedRadar != null) tag.putLong(SELECTED_RADAR_TAG, selectedRadar.asLong());
        tag.putLongArray(CONNECTIONS_TAG, connections.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putInt(RUNWAY_COUNT_TAG, runwayCodes.size());
        for (int index = 0; index < runwayCodes.size(); index++) {
            tag.putString("RunwayCode" + index, runwayCodes.get(index));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public enum AddRunwayResult { ADDED, DUPLICATE, FULL }

}
