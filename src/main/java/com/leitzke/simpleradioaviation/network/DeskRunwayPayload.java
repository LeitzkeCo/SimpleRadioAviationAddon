package com.leitzke.simpleradioaviation.network;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DeskRunwayPayload(BlockPos deskPosition, Action action, String code)
        implements CustomPacketPayload {
    public static final Type<DeskRunwayPayload> TYPE = new Type<>(
            SimpleRadioAviationAddon.id("desk_runway"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeskRunwayPayload> CODEC =
            StreamCodec.of(DeskRunwayPayload::write, DeskRunwayPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, DeskRunwayPayload payload) {
        buffer.writeBlockPos(payload.deskPosition());
        buffer.writeByte(payload.action().ordinal());
        buffer.writeUtf(payload.code(), 16);
    }

    private static DeskRunwayPayload read(RegistryFriendlyByteBuf buffer) {
        BlockPos position = buffer.readBlockPos();
        int action = buffer.readUnsignedByte();
        if (action >= Action.values().length) throw new IllegalArgumentException("Invalid runway action");
        return new DeskRunwayPayload(position, Action.values()[action], buffer.readUtf(16));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public enum Action { ADD, REMOVE }
}
