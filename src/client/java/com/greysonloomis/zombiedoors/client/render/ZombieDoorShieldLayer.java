package com.greysonloomis.zombiedoors.client.render;

import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldArrowImpact;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.util.RandomSource;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;

public final class ZombieDoorShieldLayer<
	S extends LivingEntityRenderState,
	M extends EntityModel<? super S>
> extends RenderLayer<S, M> {
	private static final float ARROW_MODEL_ORIGIN_AT_DOOR_FACE = 0.59F;

	private final ArrowModel arrowModel;
	private final ArrowRenderState arrowState = new ArrowRenderState();

	public ZombieDoorShieldLayer(
		RenderLayerParent<S, M> renderer,
		EntityRendererProvider.Context context
	) {
		super(renderer);
		arrowModel = new ArrowModel(context.bakeLayer(ModelLayers.ARROW));
	}

	@Override
	public void submit(
		PoseStack poseStack,
		SubmitNodeCollector collector,
		int packedLight,
		S state,
		float yRot,
		float xRot
	) {
		if (!(state instanceof ZombieDoorShieldRenderStateAccess doorState)
			|| doorState.zombiedoors$getRenderedDoorShield().isEmpty()) {
			return;
		}
		int blockLightOverride = doorState.zombiedoors$getDoorBlockLightOverride();
		int doorLight = blockLightOverride < 0 ? packedLight
			: LightCoordsUtil.withBlock(packedLight, blockLightOverride);

		poseStack.pushPose();
		// Undo the inverted entity-model transform before drawing block models.
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		var pose = ZombieDoorShieldPose.sample(doorState);
		poseStack.translate(pose.x(), pose.y(), pose.z());
		poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitch()));

		doorState.zombiedoors$getLowerDoorModel().submit(
			poseStack, collector, doorLight, OverlayTexture.NO_OVERLAY, state.outlineColor
		);
		if (doorState.zombiedoors$getDoorDamageStage() >= 0) {
			submitCracks(poseStack, collector, doorState.zombiedoors$getLowerCrackModel(),
				doorState.zombiedoors$getDoorDamageStage());
		}
		poseStack.pushPose();
		poseStack.translate(0.0F, 1.0F, 0.0F);
		doorState.zombiedoors$getUpperDoorModel().submit(
			poseStack, collector, doorLight, OverlayTexture.NO_OVERLAY, state.outlineColor
		);
		if (doorState.zombiedoors$getDoorDamageStage() >= 0) {
			submitCracks(poseStack, collector, doorState.zombiedoors$getUpperCrackModel(),
				doorState.zombiedoors$getDoorDamageStage());
		}
		poseStack.popPose();
		submitEmbeddedArrows(
			poseStack,
			collector,
			doorLight,
			state.outlineColor,
			doorState.zombiedoors$getRenderedDoorShieldArrowImpacts()
		);
		poseStack.popPose();
	}

	private static void submitCracks(PoseStack poseStack, SubmitNodeCollector collector,
		BlockStateModel model, int stage) {
		var parts = new ArrayList<BlockStateModelPart>();
		model.collectParts(RandomSource.create(0L), parts);
		collector.submitBreakingBlockModel(poseStack, parts, stage);
	}

	private void submitEmbeddedArrows(
		PoseStack poseStack,
		SubmitNodeCollector collector,
		int packedLight,
		int outlineColor,
		java.util.List<ZombieDoorShieldArrowImpact> impacts
	) {
		for (ZombieDoorShieldArrowImpact impact : impacts) {
			poseStack.pushPose();
			poseStack.translate(impact.x(), impact.y(), ARROW_MODEL_ORIGIN_AT_DOOR_FACE);
			float horizontal = (float) Math.sqrt(
				impact.directionX() * impact.directionX()
					+ impact.directionZ() * impact.directionZ()
			);
			float yaw = (float) Math.toDegrees(Math.atan2(
				impact.directionX(), impact.directionZ()
			));
			float pitch = (float) Math.toDegrees(Math.atan2(
				impact.directionY(), horizontal
			));
			poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
			poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
			collector.submitModel(
				arrowModel,
				arrowState,
				poseStack,
				TippableArrowRenderer.NORMAL_ARROW_LOCATION,
				packedLight,
				OverlayTexture.NO_OVERLAY,
				outlineColor,
				null
			);
			poseStack.popPose();
		}
	}

}
