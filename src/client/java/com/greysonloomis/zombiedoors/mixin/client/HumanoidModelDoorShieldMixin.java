package com.greysonloomis.zombiedoors.mixin.client;

import com.greysonloomis.zombiedoors.client.render.ZombieDoorShieldArmPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelDoorShieldMixin {
	@Shadow @Final public ModelPart rightArm;
	@Shadow @Final public ModelPart leftArm;

	@Inject(method = "setupAnim", at = @At("TAIL"))
	private void zombiedoors$poseDoorShieldArms(
		HumanoidRenderState state,
		CallbackInfo callback
	) {
		ZombieDoorShieldArmPose.apply(rightArm, leftArm, state);
	}
}
