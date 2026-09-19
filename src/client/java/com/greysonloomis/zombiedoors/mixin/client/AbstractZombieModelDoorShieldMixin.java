package com.greysonloomis.zombiedoors.mixin.client;

import com.greysonloomis.zombiedoors.client.render.ZombieDoorShieldArmPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.zombie.AbstractZombieModel;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractZombieModel.class)
public abstract class AbstractZombieModelDoorShieldMixin {
	@Inject(method = "setupAnim", at = @At("TAIL"))
	private void zombiedoors$finalDoorShieldArmPose(
		ZombieRenderState state,
		CallbackInfo callback
	) {
		HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
		ZombieDoorShieldArmPose.apply(model.rightArm, model.leftArm, state);
	}
}
