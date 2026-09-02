package com.leitzke.simpleradioaviation.client.render;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import com.leitzke.simpleradioaviation.client.model.AviationHeadsetModel;
import com.leitzke.simpleradioaviation.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class AviationHeadsetRenderer {

    private static final ResourceLocation TEXTURE =
            SimpleRadioAviationAddon.id("textures/entity/aviation_headset.png");

    private static AviationHeadsetModel model;

    private AviationHeadsetRenderer() {
    }

    public static void initialize() {
        EntityModelLayerRegistry.registerModelLayer(
                AviationHeadsetModel.LAYER_LOCATION,
                AviationHeadsetModel::createBodyLayer
        );

        ArmorRenderer.register(new ArmorRenderer() {
            @Override
            public void render(PoseStack poseStack,
                               MultiBufferSource vertexConsumers,
                               ItemStack stack,
                               LivingEntity entity,
                               EquipmentSlot slot,
                               int light,
                               HumanoidModel<LivingEntity> contextModel) {

                if (slot != EquipmentSlot.HEAD) {
                    return;
                }

                poseStack.pushPose();

                contextModel.head.translateAndRotate(poseStack);

                ArmorRenderer.renderPart(
                        poseStack,
                        vertexConsumers,
                        light,
                        stack,
                        getModel(),
                        TEXTURE
                );

                poseStack.popPose();
            }

            @Override
            public boolean shouldRenderDefaultHeadItem(
                    LivingEntity entity,
                    ItemStack stack
            ) {
                return false;
            }
        }, ModItems.AVIATION_HEADSET);
    }

    private static AviationHeadsetModel getModel() {
        if (model == null) {
            model = new AviationHeadsetModel(
                    Minecraft.getInstance()
                            .getEntityModels()
                            .bakeLayer(AviationHeadsetModel.LAYER_LOCATION)
            );
        }

        return model;
    }
}