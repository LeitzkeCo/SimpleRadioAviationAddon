package com.leitzke.simpleradioaviation.network;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigureDevicePayload(BlockPos position, Target target, String value, boolean receiver)
        implements CustomPacketPayload {
    public static final Type<ConfigureDevicePayload> TYPE = new Type<>(SimpleRadioAviationAddon.id("configure_device"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureDevicePayload> CODEC =
            StreamCodec.of(ConfigureDevicePayload::write, ConfigureDevicePayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, ConfigureDevicePayload payload) {
        buffer.writeBlockPos(payload.position());
        buffer.writeByte(payload.target().ordinal());
        buffer.writeUtf(payload.value(), 32);
        buffer.writeBoolean(payload.receiver());
    }

    private static ConfigureDevicePayload read(RegistryFriendlyByteBuf buffer) {
        BlockPos position = buffer.readBlockPos();
        int target = buffer.readUnsignedByte();
        if (target >= Target.values().length) throw new IllegalArgumentException("Invalid device target");
        return new ConfigureDevicePayload(position, Target.values()[target], buffer.readUtf(32),
                buffer.readBoolean());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public enum Target { RADAR, DATA_LINK }
}
