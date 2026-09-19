package com.greysonloomis.zombiedoors.gameplay;

import com.greysonloomis.zombiedoors.ZombieDoors;
import com.greysonloomis.zombiedoors.config.ZombieDoorsConfig;
import com.greysonloomis.zombiedoors.generated.ProjectIdentity;
import java.util.Comparator;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ZombieDoorShieldBehavior {
	private static final TagKey<Item> DOOR_SHIELDS = TagKey.create(Registries.ITEM,
		Identifier.fromNamespaceAndPath(ProjectIdentity.MOD_ID, "door_shields"));
	private static final TagKey<Item> PIGLIN_DOORS = TagKey.create(Registries.ITEM,
		Identifier.fromNamespaceAndPath(ProjectIdentity.MOD_ID, "piglin_doors"));
	public static final int IMPACT_ANIMATION_TICKS = 8;
	private static final double PROJECTILE_SCAN_RADIUS = 14.0;
	private static final double PROJECTILE_LOOKAHEAD_TICKS = 18.0;
	private static final int PROJECTILE_RAISE_TICKS = 12;
	private static final int ATTACK_POSE_TICKS = 14;
	private static final int SUNSHADE_HOLD_TICKS = 40;
	private static final double FRONTAL_MINIMUM_DOT = 0.0;
	private static final double VANILLA_ATTACK_EXPANSION = Math.sqrt(2.04F) - 0.6F;
	private static final int IMPACT_SLOW_TICKS = 30;
	private static final Identifier DOOR_MOVEMENT_SPEED = Identifier.fromNamespaceAndPath(
		ProjectIdentity.MOD_ID,
		"zombie_door_movement_speed"
	);

	private ZombieDoorShieldBehavior() {
	}

	public static boolean isWoodenDoor(ItemStack stack) {
		return !stack.isEmpty()
			&& stack.getItem() instanceof BlockItem blockItem
			&& blockItem.getBlock() instanceof DoorBlock
			&& (stack.is(ItemTags.WOODEN_DOORS) || stack.is(DOOR_SHIELDS)
				|| blockItem.getBlock().defaultBlockState().is(BlockTags.WOODEN_DOORS));
	}

	public static boolean hasDoor(Zombie zombie) {
		return zombie instanceof ZombieDoorShieldAccess access
			&& isWoodenDoor(access.zombiedoors$getDoorShield());
	}

	public static boolean canAcquire(Zombie zombie) {
		return ZombieDoors.configOrDefaults().enableZombieDoorShields()
			&& isEligibleCarrier(zombie)
			&& !hasDoor(zombie);
	}

	private static boolean isEligibleCarrier(Zombie zombie) {
		return !zombie.isBaby()
			&& !(zombie instanceof Drowned);
	}

	public static boolean canHoldDoor(Zombie zombie, ItemStack stack) {
		return isEligibleCarrier(zombie) && isWoodenDoor(stack)
			&& (!(zombie instanceof ZombifiedPiglin) || stack.is(PIGLIN_DOORS));
	}

	public static void maybeEquipSpawnedDoor(Zombie zombie, RandomSource random) {
		if (!(zombie.level() instanceof ServerLevel level) || !canAcquire(zombie)
			|| !zombie.getMainHandItem().isEmpty() && !(zombie instanceof ZombifiedPiglin)
			|| !zombie.getOffhandItem().isEmpty()) {
			return;
		}
		ZombieDoorsConfig config = ZombieDoors.configOrDefaults();
		double chance = config.zombieDoorShieldSpawnChance();
		if (zombie.level().getDifficulty() == Difficulty.HARD) {
			chance *= 2.0;
		}
		if (random.nextDouble() >= chance) {
			return;
		}
		ItemStack door = new ItemStack(ZombieDoorBiomes.doorFor(
			level.getBiome(zombie.blockPosition()), level, zombie.blockPosition(), random
		));
		if (zombie instanceof ZombifiedPiglin) {
			if (!canHoldDoor(zombie, door)) door = new ItemStack(random.nextBoolean() ? Items.CRIMSON_DOOR : Items.WARPED_DOOR);
			zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		}
		if (canHoldDoor(zombie, door)) {
			acquire(zombie, door, false);
		}
	}

	public static boolean shouldPickUp(Zombie zombie, ItemStack stack) {
		return canHoldDoor(zombie, stack) && canAcquire(zombie);
	}

	public static boolean refusesEquipmentPickup(Zombie zombie, ItemStack stack) {
		if (!hasDoor(zombie)) {
			return false;
		}
		EquipmentSlot slot = zombie.getEquipmentSlotForItem(stack);
		return slot == EquipmentSlot.MAINHAND
			|| slot == EquipmentSlot.OFFHAND
			|| slot.isArmor() && !zombie.getItemBySlot(slot).isEmpty();
	}

	public static ItemStack equipPickedUpDoor(Zombie zombie, ServerLevel level, ItemStack stack) {
		if (!canHoldDoor(zombie, stack) || !canAcquire(zombie)) {
			return ItemStack.EMPTY;
		}
		clearHands(zombie, level);
		ItemStack pickedUp = stack.copy();
		pickedUp.setCount(1);
		acquire(zombie, pickedUp, true);
		zombie.setPersistenceRequired();
		return pickedUp;
	}

	public static void acquireBrokenDoor(Zombie zombie, BlockState doorState) {
		Item item = doorState.getBlock().asItem();
		ItemStack stack = new ItemStack(item);
		if (canAcquire(zombie) && canHoldDoor(zombie, stack)) {
			if (zombie.level() instanceof ServerLevel level) {
				clearHands(zombie, level);
			}
			acquire(zombie, stack, true);
		}
	}

	public static boolean canAcquireBrokenDoor(Zombie zombie, BlockState doorState) {
		return canAcquire(zombie)
			&& canHoldDoor(zombie, new ItemStack(doorState.getBlock().asItem()));
	}

	private static void acquire(Zombie zombie, ItemStack stack, boolean playSound) {
		if (!(zombie instanceof ZombieDoorShieldAccess access)) {
			return;
		}
		access.zombiedoors$setDoorShield(stack, ZombieDoors.configOrDefaults().zombieDoorShieldDurability());
		if (playSound) {
			playDoorSound(zombie, stack, true);
		}
	}

	private static void clearHands(Zombie zombie, ServerLevel level) {
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
			ItemStack held = zombie.getItemBySlot(slot);
			if (!held.isEmpty()) {
				zombie.spawnAtLocation(level, held.copy());
				zombie.setItemSlot(slot, ItemStack.EMPTY);
			}
		}
	}

	public static void tick(Zombie zombie) {
		if (!(zombie instanceof ZombieDoorShieldAccess access)) {
			return;
		}
		if (zombie.level().isClientSide()) {
			tickClientAnimation(access);
			return;
		}
		if (!(zombie.level() instanceof ServerLevel level)) {
			return;
		}
		updateMovementSpeed(zombie);
		if (!hasDoor(zombie)) {
			return;
		}
		if (!ZombieDoors.configOrDefaults().enableZombieDoorShields()
			|| !canHoldDoor(zombie, access.zombiedoors$getDoorShield())) {
			dropDoor(level, zombie);
			return;
		}

		access.zombiedoors$setDoorShieldDisabledTicks(decrement(access.zombiedoors$getDoorShieldDisabledTicks()));
		access.zombiedoors$setDoorImpactTicks(decrement(access.zombiedoors$getDoorImpactTicks()));
		access.zombiedoors$setDoorShieldAttackTicks(decrement(access.zombiedoors$getDoorShieldAttackTicks()));
		access.zombiedoors$setDoorShieldProjectileTicks(decrement(access.zombiedoors$getDoorShieldProjectileTicks()));
		access.zombiedoors$setDoorShieldImpactSlowTicks(decrement(access.zombiedoors$getDoorShieldImpactSlowTicks()));
		access.zombiedoors$setDoorShieldSunshadeTicks(decrement(access.zombiedoors$getDoorShieldSunshadeTicks()));
		access.zombiedoors$setDoorShieldPoseCooldownTicks(decrement(access.zombiedoors$getDoorShieldPoseCooldownTicks()));
		access.zombiedoors$setDoorWhackHitCooldownTicks(decrement(access.zombiedoors$getDoorWhackHitCooldownTicks()));
		updateDoorWhackContact(access);

		boolean directlyExposed = isDirectDaylightExposed(zombie);
		if (!directlyExposed) {
			access.zombiedoors$setDoorShieldSunshadeTicks(0);
		} else if (canRaiseSunshade(access)) {
			access.zombiedoors$setDoorShieldSunshadeTicks(SUNSHADE_HOLD_TICKS);
		}

		if (access.zombiedoors$getDoorShieldDisabledTicks() <= 0
			&& access.zombiedoors$getDoorShieldAttackTicks() <= 0) {
			Projectile threat = findIncomingProjectile(level, zombie);
			if (threat != null) {
				access.zombiedoors$setDoorShieldProjectileTicks(PROJECTILE_RAISE_TICKS);
				Entity source = threat.getOwner();
				Vec3 lookAt = source != null ? source.getEyePosition() : threat.position();
				zombie.getLookControl().setLookAt(lookAt.x, lookAt.y, lookAt.z, 45.0F, 45.0F);
			}
		}
		updatePose(access);
		igniteIfExposed(zombie, directlyExposed);
		updateMovementSpeed(zombie);
	}

	private static int decrement(int value) {
		return Math.max(0, value - 1);
	}

	private static Projectile findIncomingProjectile(ServerLevel level, Zombie zombie) {
		Vec3 target = zombie.getBoundingBox().getCenter();
		double radius = Math.max(0.9, zombie.getBoundingBox().getSize() * 0.65);
		return level.getEntitiesOfClass(
			Projectile.class,
			zombie.getBoundingBox().inflate(PROJECTILE_SCAN_RADIUS),
			projectile -> projectile.isAlive()
				&& projectile.getOwner() != zombie
				&& !(projectile instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0)
				&& isIncoming(projectile, target, radius)
		).stream().min(Comparator.comparingDouble(zombie::distanceToSqr)).orElse(null);
	}

	private static boolean isIncoming(Projectile projectile, Vec3 target, double radius) {
		Vec3 velocity = projectile.getDeltaMovement();
		Vec3 position = projectile.position();
		return ZombieDoorShieldRules.projectileWillReachTarget(
			position.x, position.y, position.z,
			velocity.x, velocity.y, velocity.z,
			target.x, target.y, target.z,
			radius, PROJECTILE_LOOKAHEAD_TICKS
		);
	}

	public static boolean tryBlockProjectile(
		Zombie zombie,
		Projectile projectile,
		float damage
	) {
		if (!(damage > 0) || !Float.isFinite(damage)
			|| !(zombie instanceof ZombieDoorShieldAccess access)
			|| !hasDoor(zombie) || !canHoldDoor(zombie, access.zombiedoors$getDoorShield())
			|| !ZombieDoors.configOrDefaults().enableZombieDoorShields()
			|| access.zombiedoors$getDoorShieldPose() != ZombieDoorShieldAccess.POSE_BLOCKING
			|| access.zombiedoors$getDoorShieldDisabledTicks() > 0
			|| access.zombiedoors$getDoorShieldAttackTicks() > 0
			|| projectile instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0
			|| !isFrontal(zombie, projectile)) {
			return false;
		}

		ItemStack door = access.zombiedoors$getDoorShield();
		int durability = access.zombiedoors$getDoorShieldDurability()
			- ZombieDoorShieldRules.damageToDurability(damage);
		boolean sticks = projectile instanceof ThrownTrident trident
			&& zombie.level() instanceof ServerLevel level
			&& EnchantmentHelper.getTridentReturnToOwnerAcceleration(level, trident.getWeaponItem(), trident) == 0
			&& ZombieDoorShieldArrowImpact.decode(access.zombiedoors$getDoorShieldArrowImpacts()).size() < ZombieDoorShieldArrowImpact.MAX_IMPACTS;
		boolean embedsInDoor = projectile instanceof Arrow || sticks;
		playShieldBlockSound(zombie);
		if (durability <= 0) {
			breakDoor(zombie, door);
		} else {
			access.zombiedoors$setDoorShieldDurability(durability);
			if (embedsInDoor) {
				ZombieDoorShieldArrowImpact impact = ZombieDoorShieldArrowImpact.fromWorldHit(
					projectile.position(),
					projectile.getDeltaMovement(),
					zombie.position(),
					zombie.yBodyRot
				);
				if (sticks && projectile instanceof ThrownTrident trident) {
					impact = impact.asTrident();
					if (trident.pickup == AbstractArrow.Pickup.ALLOWED) {
						access.zombiedoors$getEmbeddedTridents().add(trident.getPickupItemStackOrigin().copy());
					}
				}
				access.zombiedoors$setDoorShieldArrowImpacts(
					ZombieDoorShieldArrowImpact.append(
						access.zombiedoors$getDoorShieldArrowImpacts(),
						impact
					)
				);
			}
		}
		if (sticks && durability <= 0 && projectile instanceof ThrownTrident trident
			&& trident.pickup == AbstractArrow.Pickup.ALLOWED && zombie.level() instanceof ServerLevel level) {
			zombie.spawnAtLocation(level, trident.getPickupItemStackOrigin().copy());
		}
		awardBlockedAttack(projectile.getOwner(), zombie);
		if (embedsInDoor) {
			projectile.discard();
		}
		if (hasDoor(zombie)
			&& (projectile instanceof AbstractArrow || projectile instanceof ThrownTrident)) {
			access.zombiedoors$setDoorShieldImpactSlowTicks(IMPACT_SLOW_TICKS);
			updateMovementSpeed(zombie);
		}
		if (hasDoor(zombie)) reactToBlockedHit(access);
		return true;
	}

	public static float blockMelee(Zombie zombie, DamageSource source, float damage) {
		if (!(damage > 0) || !Float.isFinite(damage)
			|| !ZombieDoors.configOrDefaults().enableZombieDoorShields() || !isEligibleCarrier(zombie)
			|| !hasDoor(zombie) || !(source.getDirectEntity() instanceof LivingEntity attacker)
			|| source.getDirectEntity() != source.getEntity()
			|| !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
				|| source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))) return 0;
		var access = (ZombieDoorShieldAccess) zombie;
		byte pose = access.zombiedoors$getDoorShieldPose();
		if (access.zombiedoors$getDoorShieldDisabledTicks() > 0 || access.zombiedoors$getDoorShieldAttackTicks() > 0
			|| (pose != ZombieDoorShieldAccess.POSE_CARRIED && pose != ZombieDoorShieldAccess.POSE_BLOCKING)) return 0;
		var shield = Items.SHIELD.components().get(DataComponents.BLOCKS_ATTACKS);
		if (shield == null || shield.bypassedBy().map(types -> types.contains(source.typeHolder())).orElse(false)) return 0;
		Vec3 offset = attacker.position().subtract(zombie.position()).multiply(1, 0, 1).normalize();
		double yaw = Math.toRadians(zombie.yBodyRot);
		double angle = Math.acos(Math.clamp(offset.dot(new Vec3(-Math.sin(yaw), 0, Math.cos(yaw))), -1, 1));
		float blocked = shield.resolveBlockedDamage(source, damage, angle);
		if (blocked <= 0) return 0;
		awardBlockedAttack(attacker, zombie);
		int remaining = access.zombiedoors$getDoorShieldDurability() - ZombieDoorShieldRules.damageToDurability(blocked);
		playShieldBlockSound(zombie);
		if (remaining <= 0) breakDoor(zombie, access.zombiedoors$getDoorShield());
		else {
			access.zombiedoors$setDoorShieldDurability(remaining);
			reactToBlockedHit(access);
			if (attacker.getSecondsToDisableBlocking() > 0) disableWithAxe(zombie);
		}
		return blocked;
	}

	private static void awardBlockedAttack(Entity attacker, Zombie zombie) {
		if (attacker instanceof ServerPlayer player && zombie.level() instanceof ServerLevel level) {
			var advancement = level.getServer().getAdvancements().get(
				Identifier.fromNamespaceAndPath(ProjectIdentity.MOD_ID, "the_zombies_are_coming"));
			if (advancement != null) player.getAdvancements().award(advancement, "blocked_attack");
		}
	}

	private static void reactToBlockedHit(ZombieDoorShieldAccess access) {
		access.zombiedoors$setDoorImpactTicks(IMPACT_ANIMATION_TICKS);
		access.zombiedoors$setDoorShieldProjectileTicks(PROJECTILE_RAISE_TICKS);
		access.zombiedoors$setDoorShieldSunshadeTicks(0);
		updatePose(access);
	}

	private static boolean isFrontal(Zombie zombie, Projectile projectile) {
		// The door follows the body, not the head. The shooter may have moved since firing.
		double bodyYaw = Math.toRadians(zombie.yBodyRot);
		Vec3 sourceDirection = projectile.getDeltaMovement().scale(-1.0);
		if (sourceDirection.horizontalDistanceSqr() < 1.0E-12) {
			sourceDirection = projectile.position().subtract(zombie.position());
		}
		return ZombieDoorShieldRules.isFrontal(
			-Math.sin(bodyYaw), Math.cos(bodyYaw),
			sourceDirection.x, sourceDirection.z, FRONTAL_MINIMUM_DOT
		);
	}

	public static void disableWithAxe(Zombie zombie) {
		if (!(zombie instanceof ZombieDoorShieldAccess access) || !hasDoor(zombie)) {
			return;
		}
		int ticks = ZombieDoorShieldRules.secondsToTicks(
			ZombieDoors.configOrDefaults().zombieDoorShieldAxeDisableSeconds()
		);
		if (ticks <= 0) return;
		access.zombiedoors$setDoorShieldDisabledTicks(
			Math.max(access.zombiedoors$getDoorShieldDisabledTicks(), ticks)
		);
		access.zombiedoors$setDoorShieldProjectileTicks(0);
		access.zombiedoors$setDoorShieldSunshadeTicks(0);
		updatePose(access);
		zombie.level().playSound(null, zombie.blockPosition(), SoundEvents.SHIELD_BREAK.value(),
			SoundSource.HOSTILE, 0.8F, 0.8F);
		igniteIfExposed(zombie, isDirectDaylightExposed(zombie));
	}

	public static boolean beginDoorWhack(Zombie zombie) {
		if (!(zombie instanceof ZombieDoorShieldAccess access) || !hasDoor(zombie)) {
			return false;
		}
		if (access.zombiedoors$getDoorShieldAttackTicks() > 0) {
			return access.zombiedoors$isDoorWhackDamageReady();
		}
		if (access.zombiedoors$getDoorWhackHitCooldownTicks() > 0
			|| access.zombiedoors$getDoorShieldPoseCooldownTicks() > 0) {
			return false;
		}
		access.zombiedoors$setDoorWhackHitCooldownTicks(doorWhackCooldownTicks());
		access.zombiedoors$setDoorWhackDamageReady(false);
		access.zombiedoors$setDoorShieldAttackTicks(ATTACK_POSE_TICKS);
		zombie.level().playSound(null, zombie.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
			SoundSource.HOSTILE, 0.6F, 0.65F);
		access.zombiedoors$setDoorShieldProjectileTicks(0);
		access.zombiedoors$setDoorShieldSunshadeTicks(0);
		updatePose(access);
		igniteIfExposed(zombie, isDirectDaylightExposed(zombie));
		return false;
	}

	private static void updateDoorWhackContact(ZombieDoorShieldAccess access) {
		int attackTicks = access.zombiedoors$getDoorShieldAttackTicks();
		if (attackTicks <= 0) {
			access.zombiedoors$setDoorWhackDamageReady(false);
			return;
		}
		if (ZombieDoorShieldRules.slamReachedContact(
			attackTicks,
			ATTACK_POSE_TICKS,
			poseTransitionTicks(ZombieDoorShieldAccess.POSE_ATTACKING)
		)) {
			access.zombiedoors$setDoorWhackDamageReady(true);
		}
	}

	public static boolean consumeDoorWhackDamage(Zombie zombie) {
		if (!(zombie instanceof ZombieDoorShieldAccess access) || !hasDoor(zombie)) {
			return true;
		}
		if (!access.zombiedoors$isDoorWhackDamageReady()) {
			return false;
		}
		access.zombiedoors$setDoorWhackDamageReady(false);
		return true;
	}

	public static void playDoorWhackHitSound(Zombie zombie) {
		playShieldBlockSound(zombie);
	}

	private static void playShieldBlockSound(Zombie zombie) {
		zombie.level().playSound(
			null,
			zombie.getX(), zombie.getY(), zombie.getZ(),
			SoundEvents.SHIELD_BLOCK,
			SoundSource.HOSTILE,
			1.0F,
			0.8F + zombie.getRandom().nextFloat() * 0.15F
		);
	}

	public static boolean protectFromSun(Zombie zombie, boolean vanillaSunBurnTick) {
		if (!vanillaSunBurnTick
			|| !(zombie instanceof ZombieDoorShieldAccess access)
			|| !ZombieDoors.configOrDefaults().enableZombieDoorShields()) {
			return false;
		}
		return isSunshadeProtecting(zombie, access);
	}

	public static boolean isDirectDaylightExposed(Zombie zombie) {
		return !(zombie instanceof Husk) && !(zombie instanceof ZombifiedPiglin) && MonsterDaylight.isDirectlyExposed(zombie);
	}

	private static boolean canRaiseSunshade(ZombieDoorShieldAccess access) {
		return access.zombiedoors$getDoorShieldDisabledTicks() <= 0
			&& access.zombiedoors$getDoorShieldAttackTicks() <= 0
			&& access.zombiedoors$getDoorShieldProjectileTicks() <= 0;
	}

	private static boolean isSunshadeProtecting(
		Zombie zombie,
		ZombieDoorShieldAccess access
	) {
		return ZombieDoorShieldRules.protectsFromSun(
			hasDoor(zombie),
			access.zombiedoors$getDoorShieldPose(),
			access.zombiedoors$getDoorShieldDisabledTicks(),
			access.zombiedoors$getDoorShieldAttackTicks(),
			access.zombiedoors$getDoorShieldProjectileTicks()
		);
	}

	private static void igniteIfExposed(
		Zombie zombie,
		boolean directlyExposed
	) {
		if (!(zombie instanceof ZombieDoorShieldAccess access)
			|| !directlyExposed
			|| isSunshadeProtecting(zombie, access)
			|| !zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
			return;
		}
		zombie.igniteForSeconds(8.0F);
	}

	public static boolean isDoorWhackInRange(Zombie zombie, LivingEntity target) {
		if (!hasDoor(zombie) || !ZombieDoors.configOrDefaults().enableZombieDoorShields()) {
			return false;
		}
		double expansion = VANILLA_ATTACK_EXPANSION
			+ ZombieDoors.configOrDefaults().zombieDoorWhackReachBonus();
		return zombie.getBoundingBox().inflate(expansion, 0.0, expansion)
			.intersects(target.getBoundingBox());
	}

	public static int doorWhackCooldownTicks() {
		return Math.max(20, ZombieDoorShieldRules.secondsToTicks(
			ZombieDoors.configOrDefaults().zombieDoorWhackCooldownSeconds()
		));
	}

	public static void dropDoor(ServerLevel level, Zombie zombie) {
		if (!(zombie instanceof ZombieDoorShieldAccess access) || !hasDoor(zombie)) {
			return;
		}
		ItemStack drop = access.zombiedoors$getDoorShield().copy();
		for (ItemStack trident : access.zombiedoors$getEmbeddedTridents()) zombie.spawnAtLocation(level, trident.copy());
		access.zombiedoors$setDoorShield(ItemStack.EMPTY, 0);
		zombie.spawnAtLocation(level, drop);
	}

	private static void breakDoor(Zombie zombie, ItemStack door) {
		if (!(zombie instanceof ZombieDoorShieldAccess access)) {
			return;
		}
		BlockState state = doorState(door);
		if (zombie.level() instanceof ServerLevel level && state != null) {
			level.levelEvent(2001, zombie.blockPosition().above(), Block.getId(state));
			level.playSound(
				null, zombie.blockPosition(), state.getSoundType().getBreakSound(),
				SoundSource.HOSTILE, 1.0F, 0.9F + zombie.getRandom().nextFloat() * 0.2F
			);
		}
		if (zombie.level() instanceof ServerLevel server) {
			for (ItemStack trident : access.zombiedoors$getEmbeddedTridents()) zombie.spawnAtLocation(server, trident.copy());
		}
		access.zombiedoors$setDoorShield(ItemStack.EMPTY, 0);
	}

	private static void playDoorSound(Zombie zombie, ItemStack door, boolean placement) {
		BlockState state = doorState(door);
		if (state == null) {
			return;
		}
		zombie.level().playSound(
			null,
			zombie.blockPosition(),
			placement ? state.getSoundType().getPlaceSound() : state.getSoundType().getHitSound(),
			SoundSource.HOSTILE,
			0.8F,
			0.9F + zombie.getRandom().nextFloat() * 0.2F
		);
	}

	private static BlockState doorState(ItemStack door) {
		return door.getItem() instanceof BlockItem blockItem
			? blockItem.getBlock().defaultBlockState()
			: null;
	}

	private static void updatePose(ZombieDoorShieldAccess access) {
		byte pose;
		if (access.zombiedoors$getDoorShieldAttackTicks() > 0) {
			pose = ZombieDoorShieldAccess.POSE_ATTACKING;
		} else if (access.zombiedoors$getDoorShieldDisabledTicks() > 0) {
			pose = ZombieDoorShieldAccess.POSE_DISABLED;
		} else if (access.zombiedoors$getDoorShieldProjectileTicks() > 0) {
			pose = ZombieDoorShieldAccess.POSE_BLOCKING;
		} else if (access.zombiedoors$getDoorShieldSunshadeTicks() > 0) {
			pose = ZombieDoorShieldAccess.POSE_SUNSHADE;
		} else {
			pose = ZombieDoorShieldAccess.POSE_CARRIED;
		}
		byte current = access.zombiedoors$getDoorShieldPose();
		if (pose == current) {
			return;
		}
		boolean urgent = pose == ZombieDoorShieldAccess.POSE_ATTACKING
			|| pose == ZombieDoorShieldAccess.POSE_DISABLED
			|| pose == ZombieDoorShieldAccess.POSE_BLOCKING;
		if (!urgent && access.zombiedoors$getDoorShieldPoseCooldownTicks() > 0) {
			return;
		}
		access.zombiedoors$setDoorShieldPose(pose);
		access.zombiedoors$setDoorShieldPoseCooldownTicks(poseCooldownTicks());
	}

	private static void tickClientAnimation(ZombieDoorShieldAccess access) {
		byte desired = access.zombiedoors$getDoorShieldPose();
		byte target = access.zombiedoors$getTargetAnimatedDoorShieldPose();
		if (target == ZombieDoorShieldAccess.POSE_NONE) {
			access.zombiedoors$startDoorShieldPoseAnimation(desired, desired);
			return;
		}
		if (desired != target) {
			access.zombiedoors$startDoorShieldPoseAnimation(target, desired);
		}
		access.zombiedoors$advanceDoorShieldPoseAnimation(ZombieDoorShieldRules.poseTransitionTicks(
			ZombieDoors.clientConfigOrDefaults().zombieDoorPoseTransitionSeconds(), desired));
	}

	private static int poseCooldownTicks() {
		return Math.max(0, ZombieDoorShieldRules.secondsToTicks(
			ZombieDoors.configOrDefaults().zombieDoorPoseCooldownSeconds()
		));
	}

	private static int poseTransitionTicks(byte targetPose) {
		return ZombieDoorShieldRules.poseTransitionTicks(
			ZombieDoors.configOrDefaults().zombieDoorPoseTransitionSeconds(),
			targetPose
		);
	}

	private static void updateMovementSpeed(Zombie zombie) {
		AttributeInstance speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		boolean shouldSlow = ZombieDoors.configOrDefaults().enableZombieDoorShields()
			&& hasDoor(zombie)
			&& isEligibleCarrier(zombie);
		ZombieDoorShieldAccess access = (ZombieDoorShieldAccess) zombie;
		double multiplier = ZombieDoorShieldRules.movementSpeedMultiplier(
			shouldSlow,
			access.zombiedoors$getDoorShieldPose(),
			access.zombiedoors$getDoorShieldImpactSlowTicks()
		);
		AttributeModifier current = speed.getModifier(DOOR_MOVEMENT_SPEED);
		if (multiplier == 1.0) {
			if (current != null) {
				speed.removeModifier(DOOR_MOVEMENT_SPEED);
			}
			return;
		}
		double amount = multiplier - 1.0;
		if (current == null || Double.compare(current.amount(), amount) != 0) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(
				DOOR_MOVEMENT_SPEED,
				amount,
				AttributeModifier.Operation.ADD_MULTIPLIED_BASE
			));
		}
	}
}
