package com.leitzke.simpleradioaviation.network;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import com.leitzke.simpleradioaviation.radar.RadarContact;
import com.leitzke.simpleradioaviation.radar.RunwayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record RadarContactsPayload(
        BlockPos deskPosition,
        Status status,
        @Nullable BlockPos radarPosition,
        String radarName,
        int radarIndex,
        int radarCount,
        float sweepHeading,
        float sweepSpeed,
        List<RadarContact> contacts,
        List<RunwayInfo> runways
) implements CustomPacketPayload {
    public static final int MAX_CONTACTS = 256;

    public static final Type<RadarContactsPayload> TYPE = new Type<>(
            SimpleRadioAviationAddon.id("radar_contacts")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RadarContactsPayload> CODEC =
            StreamCodec.of(RadarContactsPayload::write, RadarContactsPayload::read);

    public RadarContactsPayload {
        contacts = List.copyOf(contacts);
        runways = List.copyOf(runways);
    }

    private static void write(RegistryFriendlyByteBuf buffer, RadarContactsPayload payload) {
        buffer.writeBlockPos(payload.deskPosition());
        buffer.writeByte(payload.status().ordinal());
        buffer.writeBoolean(payload.radarPosition() != null);
        if (payload.radarPosition() != null) {
            buffer.writeBlockPos(payload.radarPosition());
        }
        buffer.writeUtf(payload.radarName(), 32);
        buffer.writeVarInt(payload.radarIndex());
        buffer.writeVarInt(payload.radarCount());
        buffer.writeFloat(payload.sweepHeading());
        buffer.writeFloat(payload.sweepSpeed());

        int contactCount = Math.min(payload.contacts().size(), MAX_CONTACTS);
        buffer.writeVarInt(contactCount);
        for (int index = 0; index < contactCount; index++) {
            RadarContact contact = payload.contacts().get(index);
            buffer.writeUUID(contact.playerId());
            buffer.writeUtf(contact.playerName(), 64);
            buffer.writeDouble(contact.x());
            buffer.writeDouble(contact.y());
            buffer.writeDouble(contact.z());
            buffer.writeDouble(contact.velocityX());
            buffer.writeDouble(contact.velocityY());
            buffer.writeDouble(contact.velocityZ());
            buffer.writeFloat(contact.heading());
            buffer.writeBoolean(contact.vehicleTypeId() != null);
            if (contact.vehicleTypeId() != null) {
                buffer.writeResourceLocation(contact.vehicleTypeId());
            }
        }

        int runwayCount = Math.min(payload.runways().size(), 4);
        buffer.writeVarInt(runwayCount);
        for (int index = 0; index < runwayCount; index++) {
            RunwayInfo runway = payload.runways().get(index);
            buffer.writeUtf(runway.code(), 16);
            buffer.writeUtf(runway.firstName(), 5);
            buffer.writeUtf(runway.secondName(), 5);
            buffer.writeVarInt(runway.width());
            buffer.writeBoolean(runway.firstPosition() != null);
            if (runway.firstPosition() != null) buffer.writeBlockPos(runway.firstPosition());
            buffer.writeBoolean(runway.secondPosition() != null);
            if (runway.secondPosition() != null) buffer.writeBlockPos(runway.secondPosition());
        }
    }

    private static RadarContactsPayload read(RegistryFriendlyByteBuf buffer) {
        BlockPos deskPosition = buffer.readBlockPos();
        int statusIndex = buffer.readUnsignedByte();
        if (statusIndex >= Status.values().length) {
            throw new IllegalArgumentException("Invalid radar status: " + statusIndex);
        }

        Status status = Status.values()[statusIndex];
        BlockPos radarPosition = buffer.readBoolean() ? buffer.readBlockPos() : null;
        String radarName = buffer.readUtf(32);
        int radarIndex = buffer.readVarInt();
        int radarCount = buffer.readVarInt();
        float sweepHeading = buffer.readFloat();
        float sweepSpeed = buffer.readFloat();
        int contactCount = buffer.readVarInt();
        if (contactCount < 0 || contactCount > MAX_CONTACTS) {
            throw new IllegalArgumentException("Invalid radar contact count: " + contactCount);
        }

        List<RadarContact> contacts = new ArrayList<>(contactCount);
        for (int index = 0; index < contactCount; index++) {
            var playerId = buffer.readUUID();
            String playerName = buffer.readUtf(64);
            double x = buffer.readDouble();
            double y = buffer.readDouble();
            double z = buffer.readDouble();
            double velocityX = buffer.readDouble();
            double velocityY = buffer.readDouble();
            double velocityZ = buffer.readDouble();
            float heading = buffer.readFloat();
            ResourceLocation vehicleTypeId = buffer.readBoolean()
                    ? buffer.readResourceLocation()
                    : null;

            contacts.add(new RadarContact(
                    playerId,
                    playerName,
                    x,
                    y,
                    z,
                    velocityX,
                    velocityY,
                    velocityZ,
                    heading,
                    vehicleTypeId
            ));
        }

        int runwayCount = buffer.readVarInt();
        if (runwayCount < 0 || runwayCount > 4) {
            throw new IllegalArgumentException("Invalid runway count: " + runwayCount);
        }
        List<RunwayInfo> runways = new ArrayList<>(runwayCount);
        for (int index = 0; index < runwayCount; index++) {
            String code = buffer.readUtf(16);
            String firstName = buffer.readUtf(5);
            String secondName = buffer.readUtf(5);
            int width = buffer.readVarInt();
            BlockPos firstPosition = buffer.readBoolean() ? buffer.readBlockPos() : null;
            BlockPos secondPosition = buffer.readBoolean() ? buffer.readBlockPos() : null;
            runways.add(new RunwayInfo(code, firstName, secondName, width,
                    firstPosition, secondPosition));
        }

        return new RadarContactsPayload(
                deskPosition,
                status,
                radarPosition,
                radarName,
                radarIndex,
                radarCount,
                sweepHeading,
                sweepSpeed,
                contacts,
                runways
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Status {
        DISCONNECTED,
        RADAR_OFFLINE,
        ACTIVE
    }
}
