package com.greysonloomis.zombiedoors.gameplay;

import net.minecraft.world.item.ItemStack;

public interface ZombieDoorShieldAccess {
	byte POSE_NONE = 0;
	byte POSE_CARRIED = 1;
	byte POSE_BLOCKING = 2;
	byte POSE_SUNSHADE = 3;
	byte POSE_ATTACKING = 4;
	byte POSE_DISABLED = 5;

	int zombiedoors$getDoorImpactTicks();
	void zombiedoors$setDoorImpactTicks(int ticks);

	ItemStack zombiedoors$getDoorShield();

	void zombiedoors$setDoorShield(ItemStack stack, int durability);

	int zombiedoors$getDoorShieldDurability();

	int zombiedoors$getDoorShieldMaxDurability();

	void zombiedoors$setDoorShieldDurability(int durability);

	byte zombiedoors$getDoorShieldPose();

	void zombiedoors$setDoorShieldPose(byte pose);

	int zombiedoors$getDoorShieldDisabledTicks();

	void zombiedoors$setDoorShieldDisabledTicks(int ticks);

	int zombiedoors$getDoorShieldAttackTicks();

	void zombiedoors$setDoorShieldAttackTicks(int ticks);

	int zombiedoors$getDoorShieldProjectileTicks();

	void zombiedoors$setDoorShieldProjectileTicks(int ticks);

	int zombiedoors$getDoorShieldImpactSlowTicks();

	void zombiedoors$setDoorShieldImpactSlowTicks(int ticks);

	int zombiedoors$getDoorShieldSunshadeTicks();

	void zombiedoors$setDoorShieldSunshadeTicks(int ticks);

	int zombiedoors$getDoorShieldPoseCooldownTicks();

	void zombiedoors$setDoorShieldPoseCooldownTicks(int ticks);

	int zombiedoors$getDoorWhackHitCooldownTicks();

	void zombiedoors$setDoorWhackHitCooldownTicks(int ticks);

	boolean zombiedoors$isDoorWhackDamageReady();

	void zombiedoors$setDoorWhackDamageReady(boolean ready);

	String zombiedoors$getDoorShieldArrowImpacts();

	void zombiedoors$setDoorShieldArrowImpacts(String impacts);

	byte zombiedoors$getPreviousAnimatedDoorShieldPose();

	byte zombiedoors$getTargetAnimatedDoorShieldPose();

	float zombiedoors$getDoorShieldPoseProgress(float partialTick);

	void zombiedoors$startDoorShieldPoseAnimation(byte previousPose, byte targetPose);

	void zombiedoors$advanceDoorShieldPoseAnimation(int transitionTicks);
}
