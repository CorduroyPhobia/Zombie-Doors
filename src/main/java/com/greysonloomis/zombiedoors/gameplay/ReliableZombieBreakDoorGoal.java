package com.greysonloomis.zombiedoors.gameplay;

import com.greysonloomis.zombiedoors.ZombieDoors;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;

public final class ReliableZombieBreakDoorGoal extends BreakDoorGoal {
	public ReliableZombieBreakDoorGoal(Zombie zombie) {
		super(zombie, ReliableZombieBreakDoorGoal::canBreakOnDifficulty);
	}

	@Override
	public boolean canUse() {
		return ZombieDoors.configOrDefaults().enableReliableZombieDoorBreaking()
			&& super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return ZombieDoors.configOrDefaults().enableReliableZombieDoorBreaking()
			&& super.canContinueToUse();
	}

	static boolean canBreakOnDifficulty(Difficulty difficulty) {
		return difficulty != Difficulty.PEACEFUL;
	}
}
