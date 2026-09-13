// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17+ for Yarn
// Paste this class into your mod and generate all required imports
public class aviation_radar extends EntityModel<Entity> {
	private final ModelPart rotor;
	private final ModelPart radar;
	private final ModelPart spot;
	private final ModelPart base;
	private final ModelPart redpipe;
	private final ModelPart conectores;
	public aviation_radar(ModelPart root) {
		this.rotor = root.getChild("rotor");
		this.radar = this.rotor.getChild("radar");
		this.spot = this.radar.getChild("spot");
		this.base = root.getChild("base");
		this.redpipe = this.base.getChild("redpipe");
		this.conectores = this.base.getChild("conectores");
	}
	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData rotor = modelPartData.addChild("rotor", ModelPartBuilder.create().uv(96, 64).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData radar = rotor.addChild("radar", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -56.0F, -12.0F, 2.0F, 32.0F, 24.0F, new Dilation(0.0F))
		.uv(0, 80).cuboid(-3.0F, -56.0F, 12.0F, 2.0F, 32.0F, 8.0F, new Dilation(0.0F))
		.uv(20, 80).cuboid(-3.0F, -56.0F, -20.0F, 2.0F, 32.0F, 8.0F, new Dilation(0.0F))
		.uv(72, 93).cuboid(-5.0F, -56.0F, -24.0F, 2.0F, 32.0F, 4.0F, new Dilation(0.0F))
		.uv(84, 93).cuboid(-5.0F, -56.0F, 20.0F, 2.0F, 32.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(-14.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

		ModelPartData spot = radar.addChild("spot", ModelPartBuilder.create().uv(40, 88).cuboid(-1.0F, 0.0F, -1.0F, 25.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(-25.0F, -31.0F, 0.0F));

		ModelPartData spot2_r1 = spot.addChild("spot2_r1", ModelPartBuilder.create().uv(0, 120).cuboid(-1.0F, -2.0F, -1.0F, 4.0F, 2.0F, 2.0F, new Dilation(0.0F))
		.uv(112, 80).cuboid(3.0F, -3.0F, -2.0F, 2.0F, 4.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

		ModelPartData spot1_r1 = spot.addChild("spot1_r1", ModelPartBuilder.create().uv(56, 109).cuboid(-1.0F, -4.0F, -2.0F, 2.0F, 6.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(-1.0F, 2.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

		ModelPartData base = modelPartData.addChild("base", ModelPartBuilder.create().uv(52, 0).cuboid(-8.0F, -16.0F, -8.0F, 16.0F, 16.0F, 16.0F, new Dilation(0.0F))
		.uv(40, 93).cuboid(-4.0F, -24.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F))
		.uv(0, 56).cuboid(8.0F, -16.0F, -8.0F, 8.0F, 8.0F, 16.0F, new Dilation(0.0F))
		.uv(48, 64).cuboid(-16.0F, -16.0F, -8.0F, 8.0F, 8.0F, 16.0F, new Dilation(0.0F))
		.uv(52, 32).cuboid(-16.0F, -16.0F, 8.0F, 32.0F, 8.0F, 8.0F, new Dilation(0.0F))
		.uv(52, 48).cuboid(-16.0F, -16.0F, -16.0F, 32.0F, 8.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData redpipe = base.addChild("redpipe", ModelPartBuilder.create().uv(112, 100).cuboid(-4.0F, -8.0F, -19.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F))
		.uv(116, 0).cuboid(2.0F, -16.0F, -27.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F))
		.uv(116, 10).cuboid(-4.0F, -16.0F, -27.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F))
		.uv(104, 112).cuboid(2.0F, -8.0F, -19.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F))
		.uv(0, 148).cuboid(-2.0F, -4.0F, 11.0F, 4.0F, 4.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 9.0F));

		ModelPartData redpipe_r1 = redpipe.addChild("redpipe_r1", ModelPartBuilder.create().uv(116, 20).cuboid(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -3.1416F, 0.0F, 3.1416F));

		ModelPartData redpipe_r2 = redpipe.addChild("redpipe_r2", ModelPartBuilder.create().uv(48, 109).cuboid(-1.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -17.0F, 9.0F, 1.5708F, 0.0F, -3.1416F));

		ModelPartData redpipe_r3 = redpipe.addChild("redpipe_r3", ModelPartBuilder.create().uv(56, 119).cuboid(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -8.0F, 8.0F, -3.1416F, 0.0F, 3.1416F));

		ModelPartData redpipe_r4 = redpipe.addChild("redpipe_r4", ModelPartBuilder.create().uv(112, 88).cuboid(-1.0F, -10.0F, -1.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -7.0F, -1.0F, -1.5708F, 0.0F, 3.1416F));

		ModelPartData redpipe_r5 = redpipe.addChild("redpipe_r5", ModelPartBuilder.create().uv(96, 80).cuboid(-1.0F, -12.0F, -1.0F, 2.0F, 10.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, -1.0F, -1.0F, -1.5708F, 0.0F, 3.1416F));

		ModelPartData redpipe_r6 = redpipe.addChild("redpipe_r6", ModelPartBuilder.create().uv(104, 80).cuboid(-1.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F, new Dilation(0.0F))
		.uv(96, 98).cuboid(-7.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(3.0F, -17.0F, -27.0F, -1.5708F, 0.0F, 0.0F));

		ModelPartData redpipe_r7 = redpipe.addChild("redpipe_r7", ModelPartBuilder.create().uv(96, 114).cuboid(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F))
		.uv(112, 110).cuboid(-7.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(3.0F, -7.0F, -19.0F, 1.5708F, 0.0F, 0.0F));

		ModelPartData redpipe_r8 = redpipe.addChild("redpipe_r8", ModelPartBuilder.create().uv(40, 109).cuboid(-1.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F, new Dilation(0.0F))
		.uv(104, 96).cuboid(-7.0F, -14.0F, -1.0F, 2.0F, 14.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(3.0F, -1.0F, -19.0F, 1.5708F, 0.0F, 0.0F));

		ModelPartData conectores = base.addChild("conectores", ModelPartBuilder.create().uv(0, 131).cuboid(-8.0F, -2.0F, -24.0F, 16.0F, 2.0F, 6.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
		return TexturedModelData.of(modelData, 256, 256);
	}
	@Override
	public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		rotor.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
		base.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
	}
}