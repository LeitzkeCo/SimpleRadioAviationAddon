package com.leitzke.simpleradioaviation.network;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RadarContactsRequestPayload(BlockPos deskPosition) implements CustomPacketPayload {
    public static final Type<RadarContactsRequestPayload> TYPE = new Type<>(
            SimpleRadioAviationAddon.id("radar_contacts_request")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RadarContactsRequestPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    RadarContactsRequestPayload::deskPosition,
                    RadarContactsRequestPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
