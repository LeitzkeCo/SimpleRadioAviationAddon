package com.leitzke.simpleradioaviation.radar;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public record RunwayInfo(
        String code,
        String firstName,
        String secondName,
        int width,
        @Nullable BlockPos firstPosition,
        @Nullable BlockPos secondPosition
) {
    public boolean complete() {
        return firstPosition != null && secondPosition != null;
    }

    public String displayName() {
        if (firstName.isBlank() && secondName.isBlank()) return code;
        if (secondName.isBlank()) return firstName;
        if (firstName.isBlank()) return secondName;
        return firstName + "/" + secondName;
    }
}
