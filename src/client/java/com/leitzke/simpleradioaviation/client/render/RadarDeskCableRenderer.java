package com.leitzke.simpleradioaviation.client.render;

import com.leitzke.simpleradioaviation.block.entity.RadarDeskBlockEntity;
import com.leitzke.simpleradioaviation.registry.ModBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class RadarDeskCableRenderer implements BlockEntityRenderer<RadarDeskBlockEntity> {
    public RadarDeskCableRenderer(BlockEntityRendererProvider.Context context) {}
    public static void initialize() {
        BlockEntityRenderers.register(ModBlockEntities.RADAR_DESK, RadarDeskCableRenderer::new);
    }
    @Override
    public void render(RadarDeskBlockEntity desk, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        RadarCableRenderer.render(desk, partialTick, poseStack, buffers);
    }
    @Override public boolean shouldRenderOffScreen(RadarDeskBlockEntity desk) { return true; }
    @Override public int getViewDistance() { return 64; }
}
