package com.leitzke.simpleradioaviation.block;

import net.minecraft.util.StringRepresentable;

public enum DeskPart implements StringRepresentable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    DeskPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}