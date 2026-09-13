package com.leitzke.simpleradioaviation.block.entity;

import com.leitzke.simpleradioaviation.block.AviationRadarBlock;
import com.leitzke.simpleradioaviation.item.AviationHeadsetItem;
import com.leitzke.simpleradioaviation.radar.RadarContact;
import com.leitzke.simpleradioaviation.registry.ModBlockEntities;
import com.leitzke.simpleradioaviation.radar.RadarCableNetwork;
import com.leitzke.simpleradioaviation.radar.RadarCableNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AviationRadarBlockEntity extends BlockEntity implements RadarCableNode {
    private static final String ROTATION_TAG = "RotorRotation";
    private static final String ROTATION_SPEED_TAG = "RotorSpeed";
    private static final String NAME_TAG = "RadarName";
    private static final String CONNECTIONS_TAG = "CableConnections";

    private static final float MAX_ROTATION_SPEED = 10.0F;
    private static final float ROTATION_ACCELERATION = 0.30F;

    private float previousRotorRotation;
    private float rotorRotation;
    private float rotorSpeed;
    private int saveCountdown;
    private List<RadarContact> contacts = List.of();
    private final Map<UUID, RadarContact> contactSnapshots = new LinkedHashMap<>();
    private final Map<UUID, MotionSample> motionSamples = new LinkedHashMap<>();
    private String radarName = "";
    private Set<BlockPos> connections = Set.of();

    public AviationRadarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AVIATION_RADAR, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  AviationRadarBlockEntity radar) {
        boolean powered = AviationRadarBlock.hasRedstoneInput(level, pos, state);

        if (state.getValue(AviationRadarBlock.POWERED) != powered) {
            BlockState updatedState = state.setValue(AviationRadarBlock.POWERED, powered);
            level.setBlock(pos, updatedState, Block.UPDATE_CLIENTS);
            radar.setChanged();
            level.sendBlockUpdated(pos, state, updatedState, Block.UPDATE_CLIENTS);
            state = updatedState;
        }

        radar.advanceRotor(state.getValue(AviationRadarBlock.POWERED));
        radar.updateContacts(level, pos, state.getValue(AviationRadarBlock.POWERED));

        if (radar.rotorSpeed != 0.0F && ++radar.saveCountdown >= 20) {
            radar.saveCountdown = 0;
            radar.setChanged();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  AviationRadarBlockEntity radar) {
        radar.advanceRotor(state.getValue(AviationRadarBlock.POWERED));
    }

    private void advanceRotor(boolean powered) {
        previousRotorRotation = rotorRotation;
        float targetSpeed = powered ? MAX_ROTATION_SPEED : 0.0F;
        rotorSpeed = Mth.approach(rotorSpeed, targetSpeed, ROTATION_ACCELERATION);
        rotorRotation = wrapRotation(rotorRotation + rotorSpeed);
    }

    private void updateContacts(Level level, BlockPos radarPos, boolean powered) {
        if (!powered) {
            if (!contactSnapshots.isEmpty()) {
                contactSnapshots.clear();
                contacts = List.of();
            }
            motionSamples.clear();
            return;
        }

        float currentHeading = AviationRadarBlock.getWorldRotorHeading(
                getBlockState(), rotorRotation);
        Set<UUID> validContacts = new HashSet<>();
        boolean snapshotsChanged = false;

        for (Player player : level.players()) {
            if (!player.isAlive()
                    || !(player.getItemBySlot(EquipmentSlot.HEAD).getItem()
                    instanceof AviationHeadsetItem)) {
                continue;
            }

            Entity rootVehicle = player.getRootVehicle();
            Entity trackedEntity = rootVehicle == player ? player : rootVehicle;
            if (!AviationRadarBlock.isInsideHorizontalRange(
                    radarPos, trackedEntity.blockPosition())) {
                continue;
            }

            validContacts.add(player.getUUID());
            Vec3 measuredVelocity = measureVelocity(
                    player.getUUID(), trackedEntity, level.getGameTime());
            double relativeX = trackedEntity.getX() - (radarPos.getX() + 0.5D);
            double relativeZ = trackedEntity.getZ() - (radarPos.getZ() + 0.5D);
            float contactHeading = headingTo(relativeX, relativeZ);
            if (relativeX * relativeX + relativeZ * relativeZ > 0.0001D
                    && !isAtSweepLine(currentHeading, contactHeading, rotorSpeed)) {
                continue;
            }

            RadarContact snapshot = new RadarContact(
                    player.getUUID(),
                    player.getGameProfile().getName(),
                    trackedEntity.getX(),
                    trackedEntity.getY(),
                    trackedEntity.getZ(),
                    measuredVelocity.x,
                    measuredVelocity.y,
                    measuredVelocity.z,
                    trackedEntity.getYRot(),
                    rootVehicle == player
                            ? null
                            : BuiltInRegistries.ENTITY_TYPE.getKey(rootVehicle.getType())
            );
            RadarContact previousSnapshot = contactSnapshots.put(player.getUUID(), snapshot);
            snapshotsChanged |= !snapshot.equals(previousSnapshot);
        }

        snapshotsChanged |= contactSnapshots.keySet().removeIf(id -> !validContacts.contains(id));
        motionSamples.keySet().removeIf(id -> !validContacts.contains(id));
        if (snapshotsChanged) {
            contacts = List.copyOf(contactSnapshots.values());
        }
    }

    private Vec3 measureVelocity(UUID playerId, Entity trackedEntity, long currentTick) {
        Vec3 currentPosition = trackedEntity.position();
        MotionSample previous = motionSamples.get(playerId);
        Vec3 measuredPerTick = Vec3.ZERO;

        if (previous != null && previous.entityId() == trackedEntity.getId()) {
            long elapsedTicks = currentTick - previous.tick();
            if (elapsedTicks > 0L && elapsedTicks <= 5L) {
                Vec3 rawPerTick = currentPosition.subtract(previous.position())
                        .scale(1.0D / elapsedTicks);
                // A short exponential average removes one-tick vehicle jitter
                // without changing the unit: the stored value remains blocks/tick.
                measuredPerTick = previous.velocityPerTick().scale(0.5D)
                        .add(rawPerTick.scale(0.5D));
            }
        }

        motionSamples.put(playerId, new MotionSample(
                trackedEntity.getId(), currentPosition, currentTick, measuredPerTick));
        return measuredPerTick;
    }

    private static float headingTo(double relativeX, double relativeZ) {
        return wrapRotation((float) Math.toDegrees(Math.atan2(relativeX, -relativeZ)));
    }

    private static boolean isAtSweepLine(float sweepHeading, float contactHeading,
                                         float rotationSpeed) {
        float difference = Math.abs(Mth.wrapDegrees(contactHeading - sweepHeading));
        // The rotor advances in discrete server ticks. Half of one tick's movement
        // selects the nearest sample instead of updating only after the line has
        // already crossed the contact.
        return difference <= Math.max(0.5F, Math.abs(rotationSpeed) * 0.5F);
    }

    private record MotionSample(int entityId, Vec3 position, long tick,
                                Vec3 velocityPerTick) {
    }

    public List<RadarContact> getContacts() {
        return contacts;
    }

    private static float wrapRotation(float rotation) {
        rotation %= 360.0F;
        return rotation < 0.0F ? rotation + 360.0F : rotation;
    }

    public float getRotorRotation(float partialTick) {
        float difference = rotorRotation - previousRotorRotation;
        if (difference < -180.0F) {
            difference += 360.0F;
        } else if (difference > 180.0F) {
            difference -= 360.0F;
        }
        return wrapRotation(previousRotorRotation + difference * partialTick);
    }

    public float getRotorSpeed() {
        return rotorSpeed;
    }

    public String getRadarName() {
        return radarName.isBlank() ? "Aviation Radar" : radarName;
    }

    public void setRadarName(String name) {
        radarName = name == null ? "" : name.strip().substring(0, Math.min(name.strip().length(), 32));
        sync();
    }

    @Override public BlockPos getCableNodePosition() { return worldPosition; }
    @Override public Level getCableNodeLevel() { return level; }
    @Override public Set<BlockPos> getCableConnections() { return connections; }

    @Override
    public void setCableConnections(Set<BlockPos> connections) {
        this.connections = connections.stream().limit(RadarCableNetwork.MAX_LINKS_PER_NODE)
                .map(BlockPos::immutable).collect(java.util.stream.Collectors.toUnmodifiableSet());
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rotorRotation = wrapRotation(tag.getFloat(ROTATION_TAG));
        previousRotorRotation = rotorRotation;
        rotorSpeed = tag.getFloat(ROTATION_SPEED_TAG);
        radarName = tag.getString(NAME_TAG);
        connections = Arrays.stream(tag.getLongArray(CONNECTIONS_TAG))
                .limit(RadarCableNetwork.MAX_LINKS_PER_NODE).mapToObj(BlockPos::of)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat(ROTATION_TAG, rotorRotation);
        tag.putFloat(ROTATION_SPEED_TAG, rotorSpeed);
        tag.putString(NAME_TAG, radarName);
        tag.putLongArray(CONNECTIONS_TAG, connections.stream().mapToLong(BlockPos::asLong).toArray());
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

}
