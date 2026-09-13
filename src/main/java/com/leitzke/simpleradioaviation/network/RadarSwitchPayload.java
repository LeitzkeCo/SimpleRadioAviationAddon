package com.leitzke.simpleradioaviation.network;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RadarSwitchPayload(BlockPos deskPosition) implements CustomPacketPayload {
    public static final Type<RadarSwitchPayload> TYPE = new Type<>(SimpleRadioAviationAddon.id("radar_switch"));
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, RadarSwitchPayload> CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, RadarSwitchPayload::deskPosition, RadarSwitchPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
