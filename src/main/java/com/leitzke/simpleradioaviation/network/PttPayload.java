package com.leitzke.simpleradioaviation.network;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PttPayload(boolean pressed) implements CustomPacketPayload {

    public static final Type<PttPayload> TYPE = new Type<>(
            SimpleRadioAviationAddon.id("ptt")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, PttPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    PttPayload::pressed,
                    PttPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}