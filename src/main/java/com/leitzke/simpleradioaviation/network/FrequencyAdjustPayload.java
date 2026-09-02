package com.leitzke.simpleradioaviation.network;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FrequencyAdjustPayload(int amount) implements CustomPacketPayload {

    public static final Type<FrequencyAdjustPayload> TYPE = new Type<>(
            SimpleRadioAviationAddon.id("frequency_adjust")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FrequencyAdjustPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    FrequencyAdjustPayload::amount,
                    FrequencyAdjustPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}