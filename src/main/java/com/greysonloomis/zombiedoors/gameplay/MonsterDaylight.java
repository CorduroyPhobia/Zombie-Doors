package com.greysonloomis.zombiedoors.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Mob;

/** Vanilla sun exposure without the random tick roll. */
public final class MonsterDaylight {
	private MonsterDaylight() {
	}

	@SuppressWarnings("deprecation") // Mirrors Mob.isSunBurnTick's vanilla light check.
	public static boolean isDirectlyExposed(Mob mob) {
		if (!(mob.level() instanceof ServerLevel level)
			|| !level.environmentAttributes().getValue(
				EnvironmentAttributes.MONSTERS_BURN,
				mob.position()
			)
			|| mob.getLightLevelDependentMagicValue() <= 0.5F
			|| mob.isInWaterOrRain()
			|| mob.isInPowderSnow
			|| mob.wasInPowderSnow) {
			return false;
		}
		return level.canSeeSky(BlockPos.containing(
			mob.getX(),
			mob.getEyeY(),
			mob.getZ()
		));
	}
}
