package com.leitzke.simpleradioaviation.radar;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Lightweight server-side snapshot of one headset detected by a radar sweep.
 */
public record RadarContact(
        UUID playerId,
        String playerName,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ,
        float heading,
        @Nullable ResourceLocation vehicleTypeId
) {
    public boolean isInVehicle() {
        return vehicleTypeId != null;
    }
}
