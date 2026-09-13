package com.leitzke.simpleradioaviation.client.render;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import com.leitzke.simpleradioaviation.block.entity.AviationRadarBlockEntity;
import com.leitzke.simpleradioaviation.block.AviationRadarBlock;
import com.leitzke.simpleradioaviation.client.model.AviationRadarModel;
import com.leitzke.simpleradioaviation.registry.ModBlockEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class AviationRadarRenderer implements BlockEntityRenderer<AviationRadarBlockEntity> {
    private static final ResourceLocation TEXTURE =
            SimpleRadioAviationAddon.id("textures/entity/aviation_radar.png");

    private final AviationRadarModel model;

    public AviationRadarRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new AviationRadarModel(
                context.bakeLayer(AviationRadarModel.LAYER_LOCATION)
        );
    }

    public static void initialize() {
        EntityModelLayerRegistry.registerModelLayer(
                AviationRadarModel.LAYER_LOCATION,
                AviationRadarModel::createBodyLayer
        );
        BlockEntityRenderers.register(
                ModBlockEntities.AVIATION_RADAR,
                AviationRadarRenderer::new
        );
    }

    @Override
    public void render(AviationRadarBlockEntity radar, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5D, 0.5D);

        Direction facing = radar.getBlockState().getValue(AviationRadarBlock.FACING);
        // The following Z mirror reverses the yaw handedness. Negating the stored
        // direction keeps east/west from being swapped while north/south remain
        // unchanged, with redstone on the viewer's right and copper on the left.
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 270.0F));
        poseStack.scale(1.0F, -1.0F, -1.0F);

        model.setRotorRotation(radar.getRotorRotation(partialTick));

        VertexConsumer vertexConsumer = buffers.getBuffer(
                RenderType.entityCutoutNoCull(TEXTURE)
        );
        model.renderToBuffer(
                poseStack,
                vertexConsumer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );

        poseStack.popPose();
        RadarCableRenderer.render(radar, partialTick, poseStack, buffers);
    }

    @Override
    public boolean shouldRenderOffScreen(AviationRadarBlockEntity radar) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
