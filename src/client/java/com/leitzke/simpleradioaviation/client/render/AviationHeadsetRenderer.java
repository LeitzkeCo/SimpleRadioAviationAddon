package com.leitzke.simpleradioaviation.client.render;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import com.leitzke.simpleradioaviation.client.model.AviationHeadsetModel;
import com.leitzke.simpleradioaviation.client.model.MilitaryAviationHeadsetModel;
import com.leitzke.simpleradioaviation.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class AviationHeadsetRenderer {

    private static final ResourceLocation AVIATION_TEXTURE =
            SimpleRadioAviationAddon.id("textures/entity/aviation_headset.png");
    private static final ResourceLocation MILITARY_TEXTURE =
            SimpleRadioAviationAddon.id("textures/entity/military_aviation_headset.png");

    private static AviationHeadsetModel aviationModel;
    private static MilitaryAviationHeadsetModel militaryModel;

    private AviationHeadsetRenderer() {
    }

    public static void initialize() {
        EntityModelLayerRegistry.registerModelLayer(
                AviationHeadsetModel.LAYER_LOCATION,
                AviationHeadsetModel::createBodyLayer
        );
        EntityModelLayerRegistry.registerModelLayer(
                MilitaryAviationHeadsetModel.LAYER_LOCATION,
                MilitaryAviationHeadsetModel::createBodyLayer
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

                if (stack.is(ModItems.MILITARY_AVIATION_HEADSET)) {
                    renderTranslucentPart(
                            poseStack,
                            vertexConsumers,
                            light,
                            stack,
                            getMilitaryModel(),
                            MILITARY_TEXTURE
                    );
                } else {
                    ArmorRenderer.renderPart(
                            poseStack,
                            vertexConsumers,
                            light,
                            stack,
                            getAviationModel(),
                            AVIATION_TEXTURE
                    );
                }

                poseStack.popPose();
            }

            @Override
            public boolean shouldRenderDefaultHeadItem(
                    LivingEntity entity,
                    ItemStack stack
            ) {
                return false;
            }
        }, ModItems.AVIATION_HEADSET, ModItems.MILITARY_AVIATION_HEADSET);
    }

    private static AviationHeadsetModel getAviationModel() {
        if (aviationModel == null) {
            aviationModel = new AviationHeadsetModel(
                    Minecraft.getInstance()
                            .getEntityModels()
                            .bakeLayer(AviationHeadsetModel.LAYER_LOCATION)
            );
        }

        return aviationModel;
    }

    private static MilitaryAviationHeadsetModel getMilitaryModel() {
        if (militaryModel == null) {
            militaryModel = new MilitaryAviationHeadsetModel(
                    Minecraft.getInstance()
                            .getEntityModels()
                            .bakeLayer(MilitaryAviationHeadsetModel.LAYER_LOCATION)
            );
        }

        return militaryModel;
    }

    private static void renderTranslucentPart(PoseStack poseStack,
                                              MultiBufferSource vertexConsumers,
                                              int light,
                                              ItemStack stack,
                                              Model model,
                                              ResourceLocation texture) {
        VertexConsumer vertexConsumer = ItemRenderer.getArmorFoilBuffer(
                vertexConsumers,
                RenderType.entityTranslucent(texture),
                stack.hasFoil()
        );

        model.renderToBuffer(
                poseStack,
                vertexConsumer,
                light,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );
    }
}
