package com.greysonloomis.zombiedoors.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public final class ZombieDoorShieldArmPose {
	private ZombieDoorShieldArmPose() {
	}

	public static void apply(
		ModelPart rightArm,
		ModelPart leftArm,
		HumanoidRenderState state
	) {
		if (!(state instanceof ZombieDoorShieldRenderStateAccess doorState)
			|| doorState.zombiedoors$getRenderedDoorShield().isEmpty()) {
			return;
		}

        var pose = ZombieDoorShieldPose.sample(doorState);
        rightArm.xRot = leftArm.xRot = pose.armPitch();
        rightArm.yRot = -pose.armYaw();
        leftArm.yRot = pose.armYaw();
        rightArm.zRot = pose.armRoll();
        leftArm.zRot = -pose.armRoll();
        // setupAnim hooks can run twice; assign from the rest pose instead of accumulating recoil.
        rightArm.z = rightArm.getInitialPose().z() + pose.armZ();
        leftArm.z = leftArm.getInitialPose().z() + pose.armZ();
    }
}
