package com.greysonloomis.zombiedoors.mixin.client;

import com.greysonloomis.zombiedoors.client.render.ZombieDoorShieldArmPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.zombie.ZombieVillagerModel;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombieVillagerModel.class)
public abstract class ZombieVillagerModelDoorShieldMixin {
	@Inject(method = "setupAttackAnimation", at = @At("TAIL"))
	private void zombiedoors$finalDoorShieldArmPose(
		ZombieVillagerRenderState state,
		CallbackInfo callback
	) {
		HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
		ZombieDoorShieldArmPose.apply(model.rightArm, model.leftArm, state);
	}
}
