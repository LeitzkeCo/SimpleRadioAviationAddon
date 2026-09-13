package com.leitzke.simpleradioaviation.radar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Set;

public interface RadarCableNode {
    BlockPos getCableNodePosition();

    Level getCableNodeLevel();

    Set<BlockPos> getCableConnections();

    void setCableConnections(Set<BlockPos> connections);
}
