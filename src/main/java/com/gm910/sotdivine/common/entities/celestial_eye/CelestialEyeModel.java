package com.gm910.sotdivine.common.entities.celestial_eye;

// Made with Blockbench 5.0.2
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.gm910.sotdivine.util.ModUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.IllusionerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CelestialEyeModel extends EntityModel<CelestialEyeRenderState> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ModUtils.path("celestial_eye_model"),
			"main");
	private final ModelPart eye_WE;
	private final ModelPart structural5;
	private final ModelPart frontlash2;
	private final ModelPart inner_lower_lash2;
	private final ModelPart inner_upper_lash2;
	private final ModelPart frontlash3;
	private final ModelPart inner_lower_lash3;
	private final ModelPart inner_upper_lash3;
	private final ModelPart eye_NS;
	private final ModelPart structural6;
	private final ModelPart frontlash7;
	private final ModelPart inner_lower_lash10;
	private final ModelPart inner_upper_lash10;
	private final ModelPart frontlash8;
	private final ModelPart inner_lower_lash11;
	private final ModelPart inner_upper_lash11;
	private final ModelPart pupil;
	private final KeyframeAnimation eyeMoveAnimation;
	private final KeyframeAnimation rotateAnimation;

	public CelestialEyeModel(ModelPart root) {
		super(root);
		this.eye_WE = root.getChild("eye_WE");
		this.structural5 = this.eye_WE.getChild("structural5");
		this.frontlash2 = this.eye_WE.getChild("frontlash2");
		this.inner_lower_lash2 = this.frontlash2.getChild("inner_lower_lash2");
		this.inner_upper_lash2 = this.frontlash2.getChild("inner_upper_lash2");
		this.frontlash3 = this.eye_WE.getChild("frontlash3");
		this.inner_lower_lash3 = this.frontlash3.getChild("inner_lower_lash3");
		this.inner_upper_lash3 = this.frontlash3.getChild("inner_upper_lash3");
		this.eye_NS = root.getChild("eye_NS");
		this.structural6 = this.eye_NS.getChild("structural6");
		this.frontlash7 = this.eye_NS.getChild("frontlash7");
		this.inner_lower_lash10 = this.frontlash7.getChild("inner_lower_lash10");
		this.inner_upper_lash10 = this.frontlash7.getChild("inner_upper_lash10");
		this.frontlash8 = this.eye_NS.getChild("frontlash8");
		this.inner_lower_lash11 = this.frontlash8.getChild("inner_lower_lash11");
		this.inner_upper_lash11 = this.frontlash8.getChild("inner_upper_lash11");
		this.pupil = root.getChild("pupil");
		this.eyeMoveAnimation = CelestialEyeAnimations.eye_move.bake(root);
		this.rotateAnimation = CelestialEyeAnimations.rotation.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition eye_WE = partdefinition.addOrReplaceChild("eye_WE", CubeListBuilder.create(),
				PartPose.offset(0.0F, 16.0F, 0.0F));

		PartDefinition structural5 = eye_WE.addOrReplaceChild("structural5",
				CubeListBuilder.create().texOffs(0, 1)
						.addBox(-6.0F, -4.0F, 1.0F, 10.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 2)
						.addBox(3.0F, -5.0F, 1.0F, 3.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 3)
						.addBox(-8.0F, -5.0F, 1.0F, 3.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 4)
						.addBox(5.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 5)
						.addBox(-8.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 7)
						.addBox(6.0F, -7.0F, 1.0F, 4.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 8)
						.addBox(-12.0F, -7.0F, 1.0F, 4.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 4)
						.addBox(7.0F, -8.0F, 1.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 5)
						.addBox(-11.0F, -8.0F, 1.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 6)
						.addBox(6.0F, -9.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 7)
						.addBox(-9.0F, -9.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 6)
						.addBox(-8.0F, -11.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 8)
						.addBox(5.0F, -11.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-7.0F, -12.0F, 1.0F, 12.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(1.0F, 7.0F, -1.0F));

		PartDefinition frontlash2 = eye_WE.addOrReplaceChild("frontlash2", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition inner_lower_lash2 = frontlash2.addOrReplaceChild("inner_lower_lash2",
				CubeListBuilder.create().texOffs(0, 9)
						.addBox(5.0F, 3.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 9)
						.addBox(6.0F, 5.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 12)
						.addBox(1.0F, 4.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(4, 9)
						.addBox(1.0F, 6.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(6, 9)
						.addBox(-2.0F, 6.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 8)
						.addBox(-2.0F, 4.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(10, 12)
						.addBox(-7.0F, 5.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(2, 9)
						.addBox(-6.0F, 3.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition inner_upper_lash2 = frontlash2.addOrReplaceChild("inner_upper_lash2",
				CubeListBuilder.create().texOffs(12, 10)
						.addBox(5.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 11)
						.addBox(6.0F, -7.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 12)
						.addBox(1.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(10, 6)
						.addBox(1.0F, -9.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 10)
						.addBox(-2.0F, -9.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 13)
						.addBox(-2.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(2, 13)
						.addBox(-7.0F, -7.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(4, 13)
						.addBox(-6.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition frontlash3 = eye_WE.addOrReplaceChild("frontlash3", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition inner_lower_lash3 = frontlash3.addOrReplaceChild("inner_lower_lash3",
				CubeListBuilder.create().texOffs(10, 8)
						.addBox(5.0F, 3.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(6, 13)
						.addBox(6.0F, 5.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 13)
						.addBox(1.0F, 4.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(10, 10)
						.addBox(1.0F, 6.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 11)
						.addBox(-2.0F, 6.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(10, 13)
						.addBox(-2.0F, 4.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 13)
						.addBox(-7.0F, 5.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(2, 11)
						.addBox(-6.0F, 3.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition inner_upper_lash3 = frontlash3.addOrReplaceChild("inner_upper_lash3",
				CubeListBuilder.create().texOffs(0, 14)
						.addBox(5.0F, -6.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(2, 14)
						.addBox(6.0F, -7.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(14, 2)
						.addBox(1.0F, -6.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(4, 11)
						.addBox(1.0F, -9.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(6, 11)
						.addBox(-2.0F, -9.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(14, 3)
						.addBox(-2.0F, -6.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(4, 14)
						.addBox(-7.0F, -7.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(14, 4)
						.addBox(-6.0F, -6.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition eye_NS = partdefinition.addOrReplaceChild("eye_NS", CubeListBuilder.create(),
				PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition structural6 = eye_NS.addOrReplaceChild("structural6",
				CubeListBuilder.create().texOffs(0, 1)
						.addBox(-6.0F, -4.0F, 1.0F, 10.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 2)
						.addBox(3.0F, -5.0F, 1.0F, 3.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 3)
						.addBox(-8.0F, -5.0F, 1.0F, 3.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 4)
						.addBox(5.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 5)
						.addBox(-8.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 7)
						.addBox(6.0F, -7.0F, 1.0F, 4.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 8)
						.addBox(-12.0F, -7.0F, 1.0F, 4.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 4)
						.addBox(7.0F, -8.0F, 1.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 5)
						.addBox(-11.0F, -8.0F, 1.0F, 2.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 6)
						.addBox(6.0F, -9.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 7)
						.addBox(-9.0F, -9.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 6)
						.addBox(-8.0F, -11.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 8)
						.addBox(5.0F, -11.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-7.0F, -12.0F, 1.0F, 12.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(1.0F, 7.0F, -1.0F));

		PartDefinition frontlash7 = eye_NS.addOrReplaceChild("frontlash7", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition inner_lower_lash10 = frontlash7.addOrReplaceChild("inner_lower_lash10",
				CubeListBuilder.create().texOffs(0, 9)
						.addBox(5.0F, 3.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 9)
						.addBox(6.0F, 5.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 12)
						.addBox(1.0F, 4.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(4, 9)
						.addBox(1.0F, 6.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(6, 9)
						.addBox(-2.0F, 6.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 8)
						.addBox(-2.0F, 4.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(10, 12)
						.addBox(-7.0F, 5.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(2, 9)
						.addBox(-6.0F, 3.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition inner_upper_lash10 = frontlash7.addOrReplaceChild("inner_upper_lash10",
				CubeListBuilder.create().texOffs(12, 10)
						.addBox(5.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 11)
						.addBox(6.0F, -7.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 12)
						.addBox(1.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(10, 6)
						.addBox(1.0F, -9.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 10)
						.addBox(-2.0F, -9.0F, 1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 13)
						.addBox(-2.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(2, 13)
						.addBox(-7.0F, -7.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(4, 13)
						.addBox(-6.0F, -6.0F, 1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition frontlash8 = eye_NS.addOrReplaceChild("frontlash8", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition inner_lower_lash11 = frontlash8.addOrReplaceChild("inner_lower_lash11",
				CubeListBuilder.create().texOffs(10, 8)
						.addBox(5.0F, 3.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(6, 13)
						.addBox(6.0F, 5.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(8, 13)
						.addBox(1.0F, 4.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(10, 10)
						.addBox(1.0F, 6.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 11)
						.addBox(-2.0F, 6.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(10, 13)
						.addBox(-2.0F, 4.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(12, 13)
						.addBox(-7.0F, 5.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(2, 11)
						.addBox(-6.0F, 3.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition inner_upper_lash11 = frontlash8.addOrReplaceChild("inner_upper_lash11",
				CubeListBuilder.create().texOffs(0, 14)
						.addBox(5.0F, -6.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(2, 14)
						.addBox(6.0F, -7.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(14, 2)
						.addBox(1.0F, -6.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(4, 11)
						.addBox(1.0F, -9.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(6, 11)
						.addBox(-2.0F, -9.0F, -1.0F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(14, 3)
						.addBox(-2.0F, -6.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(4, 14)
						.addBox(-7.0F, -7.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(14, 4)
						.addBox(-6.0F, -6.0F, -1.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition pupil = partdefinition.addOrReplaceChild("pupil", CubeListBuilder.create().texOffs(0, 2).addBox(
				-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 16.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(CelestialEyeRenderState renderer) {
		super.setupAnim(renderer);
	}

}