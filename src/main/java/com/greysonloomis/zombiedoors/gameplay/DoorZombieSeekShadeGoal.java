package com.greysonloomis.zombiedoors.gameplay;

import com.greysonloomis.zombiedoors.ZombieDoors;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;

public final class DoorZombieSeekShadeGoal extends FleeSunGoal {
	private static final double SHADE_SEEK_SPEED = 1.0;

	private final Zombie zombie;

	public DoorZombieSeekShadeGoal(Zombie zombie) {
		super(zombie, SHADE_SEEK_SPEED);
		this.zombie = zombie;
	}

	@Override
	public boolean canUse() {
		if (!shouldSeekShade()) {
			return false;
		}
		return setWantedPos();
	}

	@Override
	public boolean canContinueToUse() {
		return ZombieDoorShieldBehavior.hasDoor(zombie)
			&& ZombieDoors.configOrDefaults().enableZombieDoorShields()
			&& !hasTargetInAttackRange()
			&& !zombie.getNavigation().isDone();
	}

	private boolean shouldSeekShade() {
		return ZombieDoors.configOrDefaults().enableZombieDoorShields()
			&& ZombieDoorShieldRules.shouldSeekShade(
				ZombieDoorShieldBehavior.hasDoor(zombie),
				ZombieDoorShieldBehavior.isDirectDaylightExposed(zombie),
				hasTargetInAttackRange()
			);
	}

	private boolean hasTargetInAttackRange() {
		LivingEntity target = zombie.getTarget();
		return target != null
			&& target.isAlive()
			&& ZombieDoorShieldBehavior.isDoorWhackInRange(zombie, target);
	}
}
