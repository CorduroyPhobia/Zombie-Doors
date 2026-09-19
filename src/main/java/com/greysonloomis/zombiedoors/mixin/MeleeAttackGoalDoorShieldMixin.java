package com.greysonloomis.zombiedoors.mixin;

import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldBehavior;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalDoorShieldMixin {
	@Shadow
	protected PathfinderMob mob;

	@Shadow
	protected abstract boolean canPerformAttack(LivingEntity target);

	@ModifyConstant(
		method = {"resetAttackCooldown", "getAttackInterval"},
		constant = @Constant(intValue = 20)
	)
	private int zombiedoors$slowDoorWhack(int vanillaTicks) {
		return mob instanceof Zombie zombie && ZombieDoorShieldBehavior.hasDoor(zombie)
			? ZombieDoorShieldBehavior.doorWhackCooldownTicks()
			: vanillaTicks;
	}

	@Inject(method = "checkAndPerformAttack", at = @At("HEAD"), cancellable = true)
	private void zombiedoors$lowerDoorForWhack(LivingEntity target, CallbackInfo callback) {
		if (mob instanceof Zombie zombie
			&& ZombieDoorShieldBehavior.hasDoor(zombie)
			&& canPerformAttack(target)
			&& !ZombieDoorShieldBehavior.beginDoorWhack(zombie)) {
			callback.cancel();
		}
	}
}
