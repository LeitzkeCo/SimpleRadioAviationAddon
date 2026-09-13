package com.leitzke.simpleradioaviation.client.render;

import com.codinglitch.simpleradio.client.core.registry.renderers.WireRenderer;
import com.leitzke.simpleradioaviation.radar.RadarCableNetwork;
import com.leitzke.simpleradioaviation.radar.RadarCableNode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class RadarCableRenderer {
    private RadarCableRenderer() {}

    public static void render(RadarCableNode node, float partialTick, PoseStack poseStack,
                              MultiBufferSource buffers) {
        Level level = node.getCableNodeLevel();
        if (level == null) return;
        BlockPos nodePos = node.getCableNodePosition();
        Vec3 origin = Vec3.atLowerCornerOf(nodePos);
        for (BlockPos linkedPos : node.getCableConnections()) {
            if (nodePos.asLong() >= linkedPos.asLong() || !level.hasChunkAt(linkedPos)) continue;
            BlockEntity otherEntity = level.getBlockEntity(linkedPos);
            if (!(otherEntity instanceof RadarCableNode other)
                    || !other.getCableConnections().contains(nodePos)) continue;
            poseStack.pushPose();
            poseStack.translate(-origin.x, -origin.y, -origin.z);
            WireRenderer.renderWire(level, buffers, poseStack,
                    RadarCableNetwork.anchor(node), RadarCableNetwork.anchor(other), null, partialTick);
            poseStack.popPose();
        }
    }
}
