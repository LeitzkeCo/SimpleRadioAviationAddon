package com.leitzke.simpleradioaviation.radar;

import com.leitzke.simpleradioaviation.block.AviationRadarBlock;
import com.leitzke.simpleradioaviation.block.DataLinkBlock;
import com.leitzke.simpleradioaviation.block.RadarDeskBlock;
import com.leitzke.simpleradioaviation.block.entity.AviationRadarBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.DataLinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class DataLinkNetwork {
    private static final Map<Level, Map<BlockPos, DataLinkBlockEntity>> LOADED = new WeakHashMap<>();
    private static final Map<Level, Map<BlockPos, Long>> LAST_DISCOVERY = new WeakHashMap<>();

    private DataLinkNetwork() {}

    public static synchronized void register(DataLinkBlockEntity device) {
        Level level = device.getLevel();
        if (level != null) LOADED.computeIfAbsent(level, ignored -> new java.util.HashMap<>())
                .put(device.getBlockPos().immutable(), device);
    }

    public static synchronized void unregister(DataLinkBlockEntity device) {
        Level level = device.getLevel();
        if (level == null) return;
        if (device.getRole() == DataLinkBlock.Role.RECEIVER) disconnectReceiver(device);
        else disconnectTransmitter(device);
        Map<BlockPos, DataLinkBlockEntity> devices = LOADED.get(level);
        if (devices != null) devices.remove(device.getBlockPos());
    }

    public static synchronized void unregisterLoaded(DataLinkBlockEntity device) {
        Level level = device.getLevel();
        if (level == null) return;
        Map<BlockPos, DataLinkBlockEntity> devices = LOADED.get(level);
        if (devices != null) devices.remove(device.getBlockPos());
    }

    public static synchronized BindResult configure(DataLinkBlockEntity device, String channel,
                                                     DataLinkBlock.Role newRole) {
        register(device);
        if (device.getRole() == DataLinkBlock.Role.RECEIVER) disconnectReceiver(device);
        else disconnectTransmitter(device);
        device.setChannel(channel);
        device.setRole(newRole);
        return device.getRole() == DataLinkBlock.Role.RECEIVER
                ? tryBind(device) : bindWaitingReceivers(device);
    }

    public static synchronized BindResult tryBind(DataLinkBlockEntity receiver) {
        if (receiver.getRole() != DataLinkBlock.Role.RECEIVER || receiver.getChannel().isBlank())
            return BindResult.NO_TRANSMITTER;
        if (findAttachedDesk(receiver) == null) return BindResult.RECEIVER_NOT_ATTACHED;
        Level level = receiver.getLevel();
        if (level == null) return BindResult.NO_TRANSMITTER;
        refreshNearbyIfNeeded(level, receiver.getBlockPos());
        DataLinkBlockEntity current = getDevice(level, receiver.getLinkedTransmitter());
        if (isCompatible(current, receiver) && current.getLinkedReceivers().contains(receiver.getBlockPos())) {
            return BindResult.CONNECTED;
        }
        disconnectReceiver(receiver);

        ArrayList<DataLinkBlockEntity> candidates = new ArrayList<>();
        java.util.Collection<DataLinkBlockEntity> devices = LOADED
                .getOrDefault(level, Map.of()).values();
        boolean sameChannel = false;
        boolean sameTier = false;
        boolean inRange = false;
        boolean attached = false;
        for (DataLinkBlockEntity candidate : devices) {
            if (candidate.getRole() != DataLinkBlock.Role.TRANSMITTER
                    || !candidate.getChannel().equals(receiver.getChannel())) continue;
            sameChannel = true;
            if (candidate.block().tier() != receiver.block().tier()) continue;
            sameTier = true;
            long range = candidate.block().tier().range();
            if (candidate.getBlockPos().distSqr(receiver.getBlockPos()) > range * range) continue;
            inRange = true;
            if (findAttachedRadar(candidate) == null) continue;
            attached = true;
            candidates.add(candidate);
        }
        candidates.sort(Comparator.comparingDouble(candidate ->
                candidate.getBlockPos().distSqr(receiver.getBlockPos())));
        boolean fullMatch = false;
        for (DataLinkBlockEntity transmitter : candidates) {
            pruneLoadedReceivers(transmitter);
            if (transmitter.addReceiver(receiver.getBlockPos())) {
                receiver.setLinkedTransmitter(transmitter.getBlockPos());
                return BindResult.CONNECTED;
            }
            fullMatch = true;
        }
        if (fullMatch) return BindResult.TRANSMITTER_FULL;
        if (!sameChannel) return BindResult.NO_TRANSMITTER;
        if (!sameTier) return BindResult.TIER_MISMATCH;
        if (!inRange) return BindResult.OUT_OF_RANGE;
        if (!attached) return BindResult.TRANSMITTER_NOT_ATTACHED;
        return BindResult.NO_TRANSMITTER;
    }

    private static BindResult bindWaitingReceivers(DataLinkBlockEntity transmitter) {
        Level level = transmitter.getLevel();
        if (level == null) return BindResult.NO_TRANSMITTER;
        refreshNearbyIfNeeded(level, transmitter.getBlockPos());
        if (findAttachedRadar(transmitter) == null) return BindResult.TRANSMITTER_NOT_ATTACHED;
        for (DataLinkBlockEntity device : new ArrayList<>(
                LOADED.getOrDefault(level, Map.of()).values())) {
            if (device.getRole() == DataLinkBlock.Role.RECEIVER
                    && device.getLinkedTransmitter() == null) tryBind(device);
        }
        return BindResult.CONNECTED;
    }

    @Nullable
    public static synchronized BlockPos resolveRadarForDesk(Level level, BlockPos deskPos) {
        BlockState deskState = level.getBlockState(deskPos);
        if (!(deskState.getBlock() instanceof RadarDeskBlock)) return null;
        Direction facing = deskState.getValue(RadarDeskBlock.FACING);
        BlockPos secondPart = deskPos.relative(facing.getCounterClockWise());
        Direction rear = facing.getOpposite();
        BlockPos[] receiverPositions = {
                deskPos.relative(rear),
                secondPart.relative(rear)
        };
        for (BlockPos receiverPos : receiverPositions) {
            if (!(level.getBlockEntity(receiverPos) instanceof DataLinkBlockEntity receiver)
                    || receiver.getRole() != DataLinkBlock.Role.RECEIVER) continue;
            register(receiver);
            if (tryBind(receiver) != BindResult.CONNECTED) continue;
            DataLinkBlockEntity transmitter = getDevice(level, receiver.getLinkedTransmitter());
            BlockPos radar = transmitter == null ? null : findAttachedRadar(transmitter);
            if (radar != null) return radar;
        }
        return null;
    }

    @Nullable
    public static BlockPos findAttachedRadar(DataLinkBlockEntity transmitter) {
        if (transmitter.getRole() != DataLinkBlock.Role.TRANSMITTER) return null;
        Level level = transmitter.getLevel();
        if (level == null) return null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos possiblePart = transmitter.getBlockPos().relative(direction);
            BlockState state = level.getBlockState(possiblePart);
            if (!(state.getBlock() instanceof AviationRadarBlock)) continue;
            BlockPos core = AviationRadarBlock.getCorePos(possiblePart, state);
            BlockState coreState = level.getBlockState(core);
            if (transmitter.getBlockPos().equals(core.relative(
                    AviationRadarBlock.getCableOutputDirection(coreState), 2))) return core;
        }
        return null;
    }

    @Nullable
    private static BlockPos findAttachedDesk(DataLinkBlockEntity receiver) {
        if (receiver.getRole() != DataLinkBlock.Role.RECEIVER) return null;
        Level level = receiver.getLevel();
        if (level == null) return null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos possibleDesk = receiver.getBlockPos().relative(direction);
            BlockState state = level.getBlockState(possibleDesk);
            if (!(state.getBlock() instanceof RadarDeskBlock)) continue;
            BlockPos controller = RadarDeskBlock.getControllerPos(possibleDesk, state);
            BlockState controllerState = level.getBlockState(controller);
            Direction facing = controllerState.getValue(RadarDeskBlock.FACING);
            BlockPos secondPart = controller.relative(facing.getCounterClockWise());
            Direction rear = facing.getOpposite();
            if (receiver.getBlockPos().equals(controller.relative(rear))
                    || receiver.getBlockPos().equals(secondPart.relative(rear))) return controller;
        }
        return null;
    }

    private static boolean isCompatible(@Nullable DataLinkBlockEntity transmitter,
                                        DataLinkBlockEntity receiver) {
        if (transmitter == null || transmitter.getRole() != DataLinkBlock.Role.TRANSMITTER
                || transmitter.block().tier() != receiver.block().tier()
                || !transmitter.getChannel().equals(receiver.getChannel())) return false;
        long range = transmitter.block().tier().range();
        return transmitter.getBlockPos().distSqr(receiver.getBlockPos()) <= range * range;
    }

    private static void refreshNearbyIfNeeded(Level level, BlockPos center) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        long now = level.getGameTime();
        Map<BlockPos, Long> scans = LAST_DISCOVERY.computeIfAbsent(level, ignored -> new java.util.HashMap<>());
        if (now - scans.getOrDefault(center, Long.MIN_VALUE / 2) < 100L) return;
        scans.put(center.immutable(), now);
        int chunkRadius = (DataLinkBlock.Tier.ADVANCED.range() + 15) / 16;
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof DataLinkBlockEntity device) register(device);
                }
            }
        }
    }

    private static void pruneLoadedReceivers(DataLinkBlockEntity transmitter) {
        Level level = transmitter.getLevel();
        if (level == null) return;
        for (BlockPos receiverPos : transmitter.getLinkedReceivers()) {
            if (level.hasChunkAt(receiverPos)) {
                DataLinkBlockEntity receiver = getDevice(level, receiverPos);
                if (receiver == null || !isCompatible(transmitter, receiver)
                        || findAttachedDesk(receiver) == null) {
                    transmitter.removeReceiver(receiverPos);
                }
            }
        }
    }

    private static void disconnectReceiver(DataLinkBlockEntity receiver) {
        Level level = receiver.getLevel();
        DataLinkBlockEntity transmitter = level == null ? null : getDevice(level, receiver.getLinkedTransmitter());
        if (transmitter != null) transmitter.removeReceiver(receiver.getBlockPos());
        receiver.setLinkedTransmitter(null);
    }

    private static void disconnectTransmitter(DataLinkBlockEntity transmitter) {
        Level level = transmitter.getLevel();
        if (level != null) for (BlockPos pos : transmitter.getLinkedReceivers()) {
            DataLinkBlockEntity receiver = getDevice(level, pos);
            if (receiver != null && transmitter.getBlockPos().equals(receiver.getLinkedTransmitter())) {
                receiver.setLinkedTransmitter(null);
            }
        }
        transmitter.clearReceivers();
    }

    @Nullable
    private static DataLinkBlockEntity getDevice(Level level, @Nullable BlockPos pos) {
        if (pos == null || !level.hasChunkAt(pos)) return null;
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof DataLinkBlockEntity device ? device : null;
    }

    public enum BindResult {
        CONNECTED,
        NO_TRANSMITTER,
        RECEIVER_NOT_ATTACHED,
        TRANSMITTER_NOT_ATTACHED,
        TIER_MISMATCH,
        OUT_OF_RANGE,
        TRANSMITTER_FULL
    }
}
