package com.greysonloomis.zombiedoors.mixin.client;

import com.greysonloomis.zombiedoors.client.render.ZombieDoorShieldArmPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.piglin.ZombifiedPiglinModel;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombifiedPiglinModel.class)
public abstract class ZombifiedPiglinModelDoorMixin {
    @Inject(method = "setupAttackAnimation", at = @At("TAIL"))
    private void zombiedoors$poseArms(ZombifiedPiglinRenderState state, CallbackInfo callback) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        ZombieDoorShieldArmPose.apply(model.rightArm, model.leftArm, state);
    }
}
