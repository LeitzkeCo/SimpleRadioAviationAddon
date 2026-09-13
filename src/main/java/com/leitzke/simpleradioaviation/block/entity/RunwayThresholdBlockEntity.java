package com.leitzke.simpleradioaviation.block.entity;

import com.leitzke.simpleradioaviation.radar.RunwayNetwork;
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

public class RunwayThresholdBlockEntity extends BlockEntity {
    private String code = "";
    private String thresholdName = "";
    private int runwayWidth = 15;
    private boolean locked;

    public RunwayThresholdBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNWAY_THRESHOLD, pos, state);
    }

    public String getCode() { return code; }
    public String getThresholdName() { return thresholdName; }
    public int getRunwayWidth() { return runwayWidth; }
    public boolean isLocked() { return locked; }

    public void applyConfiguration(String code, String name, int width) {
        if (!setConfiguration(code, name, width)) return;
        sync();
    }

    /**
     * Reconciles the block entity with the saved runway network while its chunk is
     * still being deserialized. Calling setChanged() from this phase asks the
     * server for the same chunk that is currently loading and can deadlock the
     * integrated server.
     */
    public void applyConfigurationOnLoad(String code, String name, int width) {
        setConfiguration(code, name, width);
    }

    private boolean setConfiguration(String code, String name, int width) {
        String normalizedCode = RunwayNetwork.normalizeCode(code);
        String normalizedName = RunwayNetwork.normalizeName(name);
        int normalizedWidth = Math.max(RunwayNetwork.MIN_WIDTH,
                Math.min(RunwayNetwork.MAX_WIDTH, width));
        if (this.code.equals(normalizedCode) && this.thresholdName.equals(normalizedName)
                && this.runwayWidth == normalizedWidth) return false;
        this.code = normalizedCode;
        this.thresholdName = normalizedName;
        this.runwayWidth = normalizedWidth;
        return true;
    }

    public void lock() {
        if (!locked) {
            locked = true;
            sync();
        }
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(),
                getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide) RunwayNetwork.onLoaded(this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        code = RunwayNetwork.normalizeCode(tag.getString("Code"));
        thresholdName = RunwayNetwork.normalizeName(tag.getString("ThresholdName"));
        runwayWidth = Math.max(RunwayNetwork.MIN_WIDTH,
                Math.min(RunwayNetwork.MAX_WIDTH, tag.contains("RunwayWidth")
                        ? tag.getInt("RunwayWidth") : 15));
        locked = tag.getBoolean("Locked");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Code", code);
        tag.putString("ThresholdName", thresholdName);
        tag.putInt("RunwayWidth", runwayWidth);
        tag.putBoolean("Locked", locked);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override @Nullable public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
