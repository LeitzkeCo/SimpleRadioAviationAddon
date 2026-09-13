package com.leitzke.simpleradioaviation.network;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RunwayConfigurePayload(BlockPos position, String code, String name, int width)
        implements CustomPacketPayload {
    public static final Type<RunwayConfigurePayload> TYPE = new Type<>(
            SimpleRadioAviationAddon.id("configure_runway_threshold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RunwayConfigurePayload> CODEC =
            StreamCodec.of(RunwayConfigurePayload::write, RunwayConfigurePayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, RunwayConfigurePayload payload) {
        buffer.writeBlockPos(payload.position());
        buffer.writeUtf(payload.code(), 16);
        buffer.writeUtf(payload.name(), 5);
        buffer.writeVarInt(payload.width());
    }

    private static RunwayConfigurePayload read(RegistryFriendlyByteBuf buffer) {
        return new RunwayConfigurePayload(buffer.readBlockPos(), buffer.readUtf(16),
                buffer.readUtf(5), buffer.readVarInt());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
