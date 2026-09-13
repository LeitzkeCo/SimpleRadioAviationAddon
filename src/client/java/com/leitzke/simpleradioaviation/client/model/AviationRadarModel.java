package com.leitzke.simpleradioaviation.client.model;

import com.leitzke.simpleradioaviation.SimpleRadioAviationAddon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

public class AviationRadarModel extends EntityModel<Entity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            SimpleRadioAviationAddon.id("aviation_radar"),
            "main"
    );

    private final ModelPart rotor;
    private final ModelPart base;

    public AviationRadarModel(ModelPart root) {
        this.rotor = root.getChild("rotor");
        this.base = root.getChild("base");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();

        PartDefinition rotor = root.addOrReplaceChild(
                "rotor",
                CubeListBuilder.create()
                        .texOffs(96, 64)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        PartDefinition radar = rotor.addOrReplaceChild(
                "radar",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -56.0F, -12.0F, 2.0F, 32.0F, 24.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(0, 80)
                        .addBox(-3.0F, -56.0F, 12.0F, 2.0F, 32.0F, 8.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(20, 80)
                        .addBox(-3.0F, -56.0F, -20.0F, 2.0F, 32.0F, 8.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(72, 93)
                        .addBox(-5.0F, -56.0F, -24.0F, 2.0F, 32.0F, 4.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(84, 93)
                        .addBox(-5.0F, -56.0F, 20.0F, 2.0F, 32.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-14.0F, 24.0F, 0.0F,
                        0.0F, 0.0F, 0.3927F)
        );

        PartDefinition spot = radar.addOrReplaceChild(
                "spot",
                CubeListBuilder.create()
                        .texOffs(40, 88)
                        .addBox(-1.0F, 0.0F, -1.0F, 25.0F, 3.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(-25.0F, -31.0F, 0.0F)
        );

        spot.addOrReplaceChild(
                "spot2_r1",
                CubeListBuilder.create()
                        .texOffs(0, 120)
                        .addBox(-1.0F, -2.0F, -1.0F, 4.0F, 2.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(112, 80)
                        .addBox(3.0F, -3.0F, -2.0F, 2.0F, 4.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
                        0.0F, 0.0F, -0.3927F)
        );

        spot.addOrReplaceChild(
                "spot1_r1",
                CubeListBuilder.create()
                        .texOffs(56, 109)
                        .addBox(-1.0F, -4.0F, -2.0F, 2.0F, 6.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, 2.0F, 0.0F,
                        0.0F, 0.0F, -0.3927F)
        );

        PartDefinition base = root.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(52, 0)
                        .addBox(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(40, 93)
                        .addBox(-4.0F, -24.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(0, 56)
                        .addBox(8.0F, -16.0F, -8.0F, 8.0F, 8.0F, 16.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(48, 64)
                        .addBox(-16.0F, -16.0F, -8.0F, 8.0F, 8.0F, 16.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(52, 32)
                        .addBox(-16.0F, -16.0F, 8.0F, 32.0F, 8.0F, 8.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(52, 48)
                        .addBox(-16.0F, -16.0F, -16.0F, 32.0F, 8.0F, 8.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        PartDefinition redpipe = base.addOrReplaceChild(
                "redpipe",
                CubeListBuilder.create()
                        .texOffs(112, 100)
                        .addBox(-4.0F, -8.0F, -19.0F, 2.0F, 8.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(116, 0)
                        .addBox(2.0F, -16.0F, -27.0F, 2.0F, 8.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(116, 10)
                        .addBox(-4.0F, -16.0F, -27.0F, 2.0F, 8.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(104, 112)
                        .addBox(2.0F, -8.0F, -19.0F, 2.0F, 8.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(0, 148)
                        .addBox(-2.0F, -4.0F, 11.0F, 4.0F, 4.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 9.0F)
        );

        redpipe.addOrReplaceChild(
                "redpipe_r1",
                CubeListBuilder.create()
                        .texOffs(116, 20)
                        .addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
                        -3.1416F, 0.0F, 3.1416F)
        );

        redpipe.addOrReplaceChild(
                "redpipe_r2",
                CubeListBuilder.create()
                        .texOffs(48, 109)
                        .addBox(-1.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -17.0F, 9.0F,
                        1.5708F, 0.0F, -3.1416F)
        );

        redpipe.addOrReplaceChild(
                "redpipe_r3",
                CubeListBuilder.create()
                        .texOffs(56, 119)
                        .addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 8.0F,
                        -3.1416F, 0.0F, 3.1416F)
        );

        redpipe.addOrReplaceChild(
                "redpipe_r4",
                CubeListBuilder.create()
                        .texOffs(112, 88)
                        .addBox(-1.0F, -10.0F, -1.0F, 2.0F, 8.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -7.0F, -1.0F,
                        -1.5708F, 0.0F, 3.1416F)
        );

        redpipe.addOrReplaceChild(
                "redpipe_r5",
                CubeListBuilder.create()
                        .texOffs(96, 80)
                        .addBox(-1.0F, -12.0F, -1.0F, 2.0F, 10.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, -1.0F,
                        -1.5708F, 0.0F, 3.1416F)
        );

        redpipe.addOrReplaceChild(
                "redpipe_r6",
                CubeListBuilder.create()
                        .texOffs(104, 80)
                        .addBox(-1.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(96, 98)
                        .addBox(-7.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, -17.0F, -27.0F,
                        -1.5708F, 0.0F, 0.0F)
        );

        redpipe.addOrReplaceChild(
                "redpipe_r7",
                CubeListBuilder.create()
                        .texOffs(96, 114)
                        .addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(112, 110)
                        .addBox(-7.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, -7.0F, -19.0F,
                        1.5708F, 0.0F, 0.0F)
        );

        redpipe.addOrReplaceChild(
                "redpipe_r8",
                CubeListBuilder.create()
                        .texOffs(40, 109)
                        .addBox(-1.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F,
                                new CubeDeformation(0.0F))
                        .texOffs(104, 96)
                        .addBox(-7.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, -1.0F, -19.0F,
                        1.5708F, 0.0F, 0.0F)
        );

        base.addOrReplaceChild(
                "conectores",
                CubeListBuilder.create()
                        .texOffs(0, 131)
                        .addBox(-8.0F, -2.0F, -24.0F, 16.0F, 2.0F, 6.0F,
                                new CubeDeformation(0.0F)),
                PartPose.ZERO
        );

        return LayerDefinition.create(meshDefinition, 256, 256);
    }

    public void setRotorRotation(float rotationDegrees) {
        rotor.yRot = rotationDegrees * ((float) Math.PI / 180.0F);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, int color) {
        base.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        rotor.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
