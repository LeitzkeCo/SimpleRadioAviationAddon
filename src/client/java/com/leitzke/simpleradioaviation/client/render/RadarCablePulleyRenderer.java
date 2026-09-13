package com.leitzke.simpleradioaviation.client.render;

import com.leitzke.simpleradioaviation.block.entity.RadarCablePulleyBlockEntity;
import com.leitzke.simpleradioaviation.registry.ModBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class RadarCablePulleyRenderer implements BlockEntityRenderer<RadarCablePulleyBlockEntity> {
    public RadarCablePulleyRenderer(BlockEntityRendererProvider.Context context) {}
    public static void initialize() {
        BlockEntityRenderers.register(ModBlockEntities.RADAR_CABLE_PULLEY, RadarCablePulleyRenderer::new);
    }
    @Override
    public void render(RadarCablePulleyBlockEntity pulley, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        RadarCableRenderer.render(pulley, partialTick, poseStack, buffers);
    }
    @Override public boolean shouldRenderOffScreen(RadarCablePulleyBlockEntity pulley) { return true; }
    @Override public int getViewDistance() { return 64; }
}
