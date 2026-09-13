package com.leitzke.simpleradioaviation.radar;

import com.leitzke.simpleradioaviation.block.AviationRadarBlock;
import com.leitzke.simpleradioaviation.block.RadarDeskBlock;
import com.leitzke.simpleradioaviation.block.entity.AviationRadarBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.RadarCablePulleyBlockEntity;
import com.leitzke.simpleradioaviation.block.entity.RadarDeskBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RadarCableNetwork {
    public static final int MAX_LINKS_PER_NODE = 8;
    public static final int MAX_VISITED_NODES = 512;
    private static long revision;

    private RadarCableNetwork() {
    }

    public static long revision() {
        return revision;
    }

    @Nullable
    public static RadarCableNode findNode(Level level, BlockPos clickedPos) {
        BlockState state = level.getBlockState(clickedPos);
        BlockPos nodePos = clickedPos;
        if (state.getBlock() instanceof AviationRadarBlock) {
            nodePos = AviationRadarBlock.getCorePos(clickedPos, state);
        } else if (state.getBlock() instanceof RadarDeskBlock) {
            nodePos = RadarDeskBlock.getControllerPos(clickedPos, state);
        }
        BlockEntity blockEntity = level.getBlockEntity(nodePos);
        return blockEntity instanceof RadarCableNode node ? node : null;
    }

    public static boolean connect(RadarCableNode first, RadarCableNode second) {
        if (first == second || first.getCableNodeLevel() != second.getCableNodeLevel()) {
            return false;
        }
        BlockPos firstPos = first.getCableNodePosition();
        BlockPos secondPos = second.getCableNodePosition();
        if (firstPos.equals(secondPos)
                || first.getCableConnections().size() >= MAX_LINKS_PER_NODE
                || second.getCableConnections().size() >= MAX_LINKS_PER_NODE
                || anchor(first).distanceToSqr(anchor(second)) > 100.0D) {
            return false;
        }
        Set<BlockPos> firstLinks = new HashSet<>(first.getCableConnections());
        Set<BlockPos> secondLinks = new HashSet<>(second.getCableConnections());
        boolean changed = firstLinks.add(secondPos.immutable());
        changed |= secondLinks.add(firstPos.immutable());
        if (!changed) {
            return false;
        }
        first.setCableConnections(firstLinks);
        second.setCableConnections(secondLinks);
        revision++;
        return true;
    }

    public static void disconnectAll(RadarCableNode node) {
        Level level = node.getCableNodeLevel();
        BlockPos nodePos = node.getCableNodePosition();
        for (BlockPos otherPos : List.copyOf(node.getCableConnections())) {
            if (level.hasChunkAt(otherPos)
                    && level.getBlockEntity(otherPos) instanceof RadarCableNode other) {
                Set<BlockPos> links = new HashSet<>(other.getCableConnections());
                if (links.remove(nodePos)) {
                    other.setCableConnections(links);
                }
            }
        }
        if (!node.getCableConnections().isEmpty()) {
            node.setCableConnections(Set.of());
            revision++;
        }
    }

    public static List<BlockPos> findConnectedRadars(Level level, BlockPos deskPosition) {
        if (!(level.getBlockEntity(deskPosition) instanceof RadarDeskBlockEntity)) {
            return List.of();
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> radars = new ArrayList<>();
        queue.add(deskPosition.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_VISITED_NODES) {
            BlockPos current = queue.removeFirst();
            if (!visited.add(current) || !level.hasChunkAt(current)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(current);
            if (!(blockEntity instanceof RadarCableNode node)) {
                continue;
            }
            if (blockEntity instanceof AviationRadarBlockEntity) {
                radars.add(current.immutable());
            }
            for (BlockPos linked : node.getCableConnections()) {
                if (level.hasChunkAt(linked)
                        && level.getBlockEntity(linked) instanceof RadarCableNode other
                        && other.getCableConnections().contains(current)
                        && anchor(node).distanceToSqr(anchor(other)) <= 100.0D) {
                    queue.addLast(linked);
                }
            }
        }
        return radars.stream().distinct().sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
    }

    public static Vec3 anchor(RadarCableNode node) {
        BlockPos pos = node.getCableNodePosition();
        BlockState state = node.getCableNodeLevel().getBlockState(pos);
        if (node instanceof AviationRadarBlockEntity && state.getBlock() instanceof AviationRadarBlock) {
            Direction output = AviationRadarBlock.getCableOutputDirection(state);
            // The black data socket is at the outer end of the low copper pipe:
            // 1.5 blocks from the core and only 2 model pixels above the ground.
            return Vec3.atCenterOf(pos.relative(output)).add(
                    output.getStepX() * 0.51D, -0.375D, output.getStepZ() * 0.51D);
        }
        if (node instanceof RadarDeskBlockEntity && state.getBlock() instanceof RadarDeskBlock) {
            // End at the controller block's internal center. The final half-block
            // stays hidden inside the rear socket without crossing to the front.
            return Vec3.atCenterOf(pos).add(0.0D, -0.12D, 0.0D);
        }
        if (node instanceof RadarCablePulleyBlockEntity) {
            return Vec3.atCenterOf(pos);
        }
        return Vec3.atCenterOf(pos);
    }
}
