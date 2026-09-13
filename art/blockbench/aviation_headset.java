// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class aviation_headset<T extends LivingEntity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "aviation_headset"), "main");
	private final ModelPart head;

	public aviation_headset(ModelPart root) {
		this.head = root.getChild("head");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition microphone_r1 = head.addOrReplaceChild("microphone_r1", CubeListBuilder.create().texOffs(12, 16).addBox(1.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, -1.0F, -3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition mic_boom_r1 = head.addOrReplaceChild("mic_boom_r1", CubeListBuilder.create().texOffs(18, 11).addBox(-3.0F, -1.0F, -1.0F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -1.0F, -3.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition earcup_left_r1 = head.addOrReplaceChild("earcup_left_r1", CubeListBuilder.create().texOffs(10, 11).addBox(-2.0F, -3.0F, -1.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -3.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition earcup_right_r1 = head.addOrReplaceChild("earcup_right_r1", CubeListBuilder.create().texOffs(0, 11).addBox(-2.0F, -3.0F, -1.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -3.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition headband_right_r1 = head.addOrReplaceChild("headband_right_r1", CubeListBuilder.create().texOffs(6, 16).addBox(-1.0F, -8.0F, 0.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition headband_left_r1 = head.addOrReplaceChild("headband_left_r1", CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, -8.0F, 0.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, 0.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		PartDefinition headband_top_r1 = head.addOrReplaceChild("headband_top_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -5.0F, 2.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 1.5708F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}