package com.leitzke.simpleradioaviation.client.model;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
public class AviationHeadsetModel extends EntityModel<LivingEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    SimpleRadioAviationAddon.id("aviation_headset"),
                    "main"
            );

    private final ModelPart head;

    public AviationHeadsetModel(ModelPart root) {
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create(),
                PartPose.ZERO
        );

        head.addOrReplaceChild(
                "microphone_r1",
                CubeListBuilder.create()
                        .texOffs(12, 16)
                        .addBox(1.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -1.0F, -3.0F, 0.0F, 1.5708F, 0.0F)
        );

        head.addOrReplaceChild(
                "mic_boom_r1",
                CubeListBuilder.create()
                        .texOffs(18, 11)
                        .addBox(-3.0F, -1.0F, -1.0F, 5.0F, 1.0F, 1.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, -1.0F, -3.0F, 0.0F, 1.5708F, 0.0F)
        );

        head.addOrReplaceChild(
                "earcup_left_r1",
                CubeListBuilder.create()
                        .texOffs(10, 11)
                        .addBox(-2.0F, -3.0F, -1.0F, 4.0F, 4.0F, 1.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -3.0F, 0.0F, 0.0F, 1.5708F, 0.0F)
        );

        head.addOrReplaceChild(
                "earcup_right_r1",
                CubeListBuilder.create()
                        .texOffs(0, 11)
                        .addBox(-2.0F, -3.0F, -1.0F, 4.0F, 4.0F, 1.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, -3.0F, 0.0F, 0.0F, 1.5708F, 0.0F)
        );

        head.addOrReplaceChild(
                "headband_right_r1",
                CubeListBuilder.create()
                        .texOffs(6, 16)
                        .addBox(-1.0F, -8.0F, 0.0F, 2.0F, 2.0F, 1.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F)
        );

        head.addOrReplaceChild(
                "headband_left_r1",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-1.0F, -8.0F, 0.0F, 2.0F, 2.0F, 1.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F)
        );

        head.addOrReplaceChild(
                "headband_top_r1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -1.0F, -5.0F, 2.0F, 1.0F, 10.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 1.5708F, 0.0F)
        );

        return LayerDefinition.create(meshDefinition, 32, 32);
    }

    @Override
    public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {

        head.render(
                poseStack,
                vertexConsumer,
                packedLight,
                packedOverlay,
                color
        );
    }
}
