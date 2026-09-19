package com.greysonloomis.zombiedoors.gameplay;

public final class ZombieDoorShieldRules {
    /** Vanilla destruction texture index, or -1 while a door is undamaged. */
    public static int doorDamageStage(int durability, int maximum) {
        if (maximum <= 0 || durability >= maximum) return -1;
        return Math.clamp((int) ((1.0 - (double) Math.max(0, durability) / maximum) * 10), 0, 9);
    }
	private static final double SLAM_ANIMATION_SPEED_MULTIPLIER = 1.75;

	private ZombieDoorShieldRules() {
	}

	public static boolean projectileWillReachTarget(
		double projectileX,
		double projectileY,
		double projectileZ,
		double velocityX,
		double velocityY,
		double velocityZ,
		double targetX,
		double targetY,
		double targetZ,
		double targetRadius,
		double maximumTicks
	) {
		double speedSqr = velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ;
		if (speedSqr < 1.0E-6) {
			return false;
		}
		double offsetX = targetX - projectileX;
		double offsetY = targetY - projectileY;
		double offsetZ = targetZ - projectileZ;
		double ticks = (offsetX * velocityX + offsetY * velocityY + offsetZ * velocityZ) / speedSqr;
		if (ticks < 0.0 || ticks > maximumTicks) {
			return false;
		}
		double closestX = projectileX + velocityX * ticks;
		double closestY = projectileY + velocityY * ticks;
		double closestZ = projectileZ + velocityZ * ticks;
		double dx = closestX - targetX;
		double dy = closestY - targetY;
		double dz = closestZ - targetZ;
		return dx * dx + dy * dy + dz * dz <= targetRadius * targetRadius;
	}

	public static boolean isFrontal(
		double lookX,
		double lookZ,
		double sourceX,
		double sourceZ,
		double minimumDot
	) {
		double lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
		double sourceLength = Math.sqrt(sourceX * sourceX + sourceZ * sourceZ);
		if (lookLength < 1.0E-6 || sourceLength < 1.0E-6) {
			return false;
		}
		double dot = (lookX * sourceX + lookZ * sourceZ) / (lookLength * sourceLength);
		return dot >= minimumDot;
	}

	public static int damageToDurability(float damage) {
		return Math.max(1, (int) Math.ceil(damage));
	}

	public static int secondsToTicks(double seconds) {
		return Math.max(0, (int) Math.round(seconds * 20.0));
	}

	public static int poseTransitionTicks(double seconds, byte targetPose) {
		int baseTicks = Math.max(1, secondsToTicks(seconds));
		if (targetPose != ZombieDoorShieldAccess.POSE_ATTACKING) {
			return baseTicks;
		}
		return Math.max(1, (int) Math.round(
			baseTicks / SLAM_ANIMATION_SPEED_MULTIPLIER
		));
	}

	public static boolean slamReachedContact(
		int remainingAttackTicks,
		int totalAttackTicks,
		int slamTransitionTicks
	) {
		return remainingAttackTicks > 0
			&& remainingAttackTicks <= totalAttackTicks - slamTransitionTicks;
	}

	public static double movementSpeedMultiplier(
		boolean carriesDoor,
		byte pose,
		int impactSlowTicks
	) {
		if (!carriesDoor) {
			return 1.0;
		}
		if (impactSlowTicks > 0) {
			return 0.75;
		}
		return pose == ZombieDoorShieldAccess.POSE_SUNSHADE ? 0.92 : 0.85;
	}

	public static boolean protectsFromSun(
		boolean carriesDoor,
		byte pose,
		int disabledTicks,
		int attackTicks,
		int projectileTicks
	) {
		return carriesDoor
			&& pose == ZombieDoorShieldAccess.POSE_SUNSHADE
			&& disabledTicks <= 0
			&& attackTicks <= 0
			&& projectileTicks <= 0;
	}

	public static boolean shouldSeekShade(
		boolean carriesDoor,
		boolean directlyExposed,
		boolean targetInDoorWhackRange
	) {
		return carriesDoor && directlyExposed && !targetInDoorWhackRange;
	}
}
