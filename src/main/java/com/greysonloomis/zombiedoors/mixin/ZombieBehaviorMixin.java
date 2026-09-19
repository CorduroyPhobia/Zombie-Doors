package com.greysonloomis.zombiedoors.mixin;

import com.greysonloomis.zombiedoors.gameplay.DoorZombieSeekShadeGoal;
import com.greysonloomis.zombiedoors.gameplay.ReliableZombieBreakDoorGoal;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldAccess;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldArrowImpact;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldBehavior;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieBehaviorMixin implements ZombieDoorShieldAccess {
	@Unique private static final EntityDataAccessor<Integer> ZOMBIE_DOORS_IMPACT_TICKS =
		SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.INT);
	@Override public int zombiedoors$getDoorImpactTicks() {
		return ((Zombie) (Object) this).getEntityData().get(ZOMBIE_DOORS_IMPACT_TICKS);
	}
	@Override public void zombiedoors$setDoorImpactTicks(int ticks) {
		((Zombie) (Object) this).getEntityData().set(ZOMBIE_DOORS_IMPACT_TICKS, Math.max(0, ticks));
	}
	@Unique
	private static final EntityDataAccessor<ItemStack> ZOMBIE_DOORS_DOOR_SHIELD =
		SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.ITEM_STACK);
	@Unique
	private static final EntityDataAccessor<Byte> ZOMBIE_DOORS_DOOR_SHIELD_POSE =
		SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BYTE);
	@Unique
	private static final EntityDataAccessor<String> ZOMBIE_DOORS_DOOR_SHIELD_ARROW_IMPACTS =
		SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.STRING);

	@Unique
	private static final EntityDataAccessor<Integer> ZOMBIE_DOORS_DURABILITY =
		SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.INT);
	@Unique
	private static final EntityDataAccessor<Integer> ZOMBIE_DOORS_MAX_DURABILITY =
		SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.INT);
	@Unique
	private int zombiedoors$doorShieldDisabledTicks;
	@Unique
	private int zombiedoors$doorShieldAttackTicks;
	@Unique
	private int zombiedoors$doorShieldProjectileTicks;
	@Unique
	private int zombiedoors$doorShieldImpactSlowTicks;
	@Unique
	private int zombiedoors$doorShieldSunshadeTicks;
	@Unique
	private int zombiedoors$doorShieldPoseCooldownTicks;
	@Unique
	private int zombiedoors$doorWhackHitCooldownTicks;
	@Unique
	private boolean zombiedoors$doorWhackDamageReady;
	@Unique
	private byte zombiedoors$previousAnimatedDoorShieldPose;
	@Unique
	private byte zombiedoors$targetAnimatedDoorShieldPose;
	@Unique
	private float zombiedoors$doorShieldPoseProgress;
	@Unique
	private float zombiedoors$doorShieldPoseProgressOld;

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void zombiedoors$defineDoorShieldData(
		SynchedEntityData.Builder entityData,
		CallbackInfo callback
	) {
		entityData.define(ZOMBIE_DOORS_DOOR_SHIELD, ItemStack.EMPTY);
		entityData.define(ZOMBIE_DOORS_DOOR_SHIELD_POSE, ZombieDoorShieldAccess.POSE_NONE);
		entityData.define(ZOMBIE_DOORS_DOOR_SHIELD_ARROW_IMPACTS, "");
		entityData.define(ZOMBIE_DOORS_DURABILITY, 0);
		entityData.define(ZOMBIE_DOORS_MAX_DURABILITY, 0);
		entityData.define(ZOMBIE_DOORS_IMPACT_TICKS, 0);
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void zombiedoors$addReliableDoorBreaking(CallbackInfo callback) {
		Zombie zombie = (Zombie) (Object) this;
		((MobGoalSelectorAccess) zombie).zombiedoors$getGoalSelector()
			.addGoal(0, new DoorZombieSeekShadeGoal(zombie));
		((MobGoalSelectorAccess) zombie).zombiedoors$getGoalSelector()
			.addGoal(1, new ReliableZombieBreakDoorGoal(zombie));
	}

	@Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
	private void zombiedoors$addZombieEquipment(
		RandomSource random,
		DifficultyInstance difficulty,
		CallbackInfo callback
	) {
		Zombie zombie = (Zombie) (Object) this;
		ZombieDoorShieldBehavior.maybeEquipSpawnedDoor(zombie, random);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void zombiedoors$tickDoorShield(CallbackInfo callback) {
		Zombie zombie = (Zombie) (Object) this;
		ZombieDoorShieldBehavior.tick(zombie);
	}

	@Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true)
	private void zombiedoors$pickUpWoodenDoors(
		ServerLevel level,
		ItemStack stack,
		CallbackInfoReturnable<Boolean> callback
	) {
		if (ZombieDoorShieldBehavior.isWoodenDoor(stack)) {
			callback.setReturnValue(
				ZombieDoorShieldBehavior.shouldPickUp((Zombie) (Object) this, stack)
			);
		} else if (ZombieDoorShieldBehavior.refusesEquipmentPickup(
			(Zombie) (Object) this,
			stack
		)) {
			callback.setReturnValue(false);
		}
	}

	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void zombiedoors$handleDoorShieldHit(
		ServerLevel level,
		DamageSource source,
		float damage,
		CallbackInfoReturnable<Boolean> callback
	) {
		Zombie zombie = (Zombie) (Object) this;
		if (source.getDirectEntity() instanceof Projectile projectile
			&& ZombieDoorShieldBehavior.tryBlockProjectile(zombie, projectile, damage)) {
			callback.setReturnValue(false);
		}
	}

	@Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
	private void zombiedoors$allowOneDoorWhackDamage(
		ServerLevel level,
		Entity target,
		CallbackInfoReturnable<Boolean> callback
	) {
		if (!ZombieDoorShieldBehavior.consumeDoorWhackDamage((Zombie) (Object) this)) {
			callback.setReturnValue(false);
		}
	}

	@Inject(method = "doHurtTarget", at = @At("RETURN"))
	private void zombiedoors$playDoorWhackHitSound(
		ServerLevel level,
		Entity target,
		CallbackInfoReturnable<Boolean> callback
	) {
		Zombie zombie = (Zombie) (Object) this;
		if (callback.getReturnValueZ()
			&& ZombieDoorShieldBehavior.hasDoor(zombie)
			&& zombiedoors$doorShieldAttackTicks > 0) {
			ZombieDoorShieldBehavior.playDoorWhackHitSound(zombie);
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void zombiedoors$saveDoorShield(ValueOutput output, CallbackInfo callback) {
		ItemStack door = zombiedoors$getDoorShield();
		if (!door.isEmpty()) {
			output.store("RebornDoorShield", ItemStack.CODEC, door);
			output.putInt("RebornDoorShieldDurability", zombiedoors$getDoorShieldDurability());
			output.putInt("RebornDoorShieldMaxDurability", zombiedoors$getDoorShieldMaxDurability());
			output.putString("RebornDoorShieldArrowImpacts", zombiedoors$getDoorShieldArrowImpacts());
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void zombiedoors$loadDoorShield(ValueInput input, CallbackInfo callback) {
		ItemStack door = input.read("RebornDoorShield", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		Zombie zombie = (Zombie) (Object) this;
		if (ZombieDoorShieldBehavior.isWoodenDoor(door) && !zombie.isBaby()) {
			zombiedoors$setDoorShield(
				door,
				input.getIntOr("RebornDoorShieldMaxDurability", Math.max(
					input.getIntOr("RebornDoorShieldDurability", 1),
					com.greysonloomis.zombiedoors.ZombieDoors.configOrDefaults().zombieDoorShieldDurability()))
			);
			zombiedoors$setDoorShieldDurability(input.getIntOr("RebornDoorShieldDurability", 1));
			String impacts = input.getStringOr("RebornDoorShieldArrowImpacts", "");
			if (impacts.isEmpty()) {
				impacts = ZombieDoorShieldArrowImpact.legacyPlacements(
					input.getIntOr("RebornDoorShieldArrows", 0)
				);
			}
			zombiedoors$setDoorShieldArrowImpacts(impacts);
		} else {
			zombiedoors$setDoorShield(ItemStack.EMPTY, 0);
		}
	}

	@Override
	public ItemStack zombiedoors$getDoorShield() {
		return ((Zombie) (Object) this).getEntityData().get(ZOMBIE_DOORS_DOOR_SHIELD);
	}

	@Override
	public void zombiedoors$setDoorShield(ItemStack stack, int durability) {
		ItemStack stored = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		if (!stored.isEmpty()) {
			stored.setCount(1);
		}
		((Zombie) (Object) this).getEntityData().set(ZOMBIE_DOORS_DOOR_SHIELD, stored);
		int startingDurability = stored.isEmpty() ? 0 : Math.max(1, durability);
		((Zombie) (Object) this).getEntityData().set(ZOMBIE_DOORS_MAX_DURABILITY, startingDurability);
		zombiedoors$setDoorShieldDurability(startingDurability);
		zombiedoors$setDoorShieldArrowImpacts("");
		zombiedoors$setDoorImpactTicks(0);
		if (stored.isEmpty()) {
			zombiedoors$doorShieldDisabledTicks = 0;
			zombiedoors$doorShieldAttackTicks = 0;
			zombiedoors$doorShieldProjectileTicks = 0;
			zombiedoors$doorShieldImpactSlowTicks = 0;
			zombiedoors$doorShieldSunshadeTicks = 0;
			zombiedoors$doorShieldPoseCooldownTicks = 0;
			zombiedoors$doorWhackHitCooldownTicks = 0;
			zombiedoors$doorWhackDamageReady = false;
			zombiedoors$setDoorShieldPose(ZombieDoorShieldAccess.POSE_NONE);
		} else {
			zombiedoors$setDoorShieldPose(ZombieDoorShieldAccess.POSE_CARRIED);
		}
	}

	@Override
	public int zombiedoors$getDoorShieldDurability() {
		return ((Zombie) (Object) this).getEntityData().get(ZOMBIE_DOORS_DURABILITY);
	}

	@Override
	public int zombiedoors$getDoorShieldMaxDurability() {
		return ((Zombie) (Object) this).getEntityData().get(ZOMBIE_DOORS_MAX_DURABILITY);
	}

	@Override
	public void zombiedoors$setDoorShieldDurability(int durability) {
		((Zombie) (Object) this).getEntityData().set(ZOMBIE_DOORS_DURABILITY,
			Math.clamp(durability, 0, zombiedoors$getDoorShieldMaxDurability()));
	}

	@Override
	public byte zombiedoors$getDoorShieldPose() {
		return ((Zombie) (Object) this).getEntityData().get(ZOMBIE_DOORS_DOOR_SHIELD_POSE);
	}

	@Override
	public void zombiedoors$setDoorShieldPose(byte pose) {
		((Zombie) (Object) this).getEntityData().set(ZOMBIE_DOORS_DOOR_SHIELD_POSE, pose);
	}

	@Override
	public int zombiedoors$getDoorShieldDisabledTicks() {
		return zombiedoors$doorShieldDisabledTicks;
	}

	@Override
	public void zombiedoors$setDoorShieldDisabledTicks(int ticks) {
		zombiedoors$doorShieldDisabledTicks = Math.max(0, ticks);
	}

	@Override
	public int zombiedoors$getDoorShieldAttackTicks() {
		return zombiedoors$doorShieldAttackTicks;
	}

	@Override
	public void zombiedoors$setDoorShieldAttackTicks(int ticks) {
		zombiedoors$doorShieldAttackTicks = Math.max(0, ticks);
	}

	@Override
	public int zombiedoors$getDoorShieldProjectileTicks() {
		return zombiedoors$doorShieldProjectileTicks;
	}

	@Override
	public void zombiedoors$setDoorShieldProjectileTicks(int ticks) {
		zombiedoors$doorShieldProjectileTicks = Math.max(0, ticks);
	}

	@Override
	public int zombiedoors$getDoorShieldImpactSlowTicks() {
		return zombiedoors$doorShieldImpactSlowTicks;
	}

	@Override
	public void zombiedoors$setDoorShieldImpactSlowTicks(int ticks) {
		zombiedoors$doorShieldImpactSlowTicks = Math.max(0, ticks);
	}

	@Override
	public int zombiedoors$getDoorShieldSunshadeTicks() {
		return zombiedoors$doorShieldSunshadeTicks;
	}

	@Override
	public void zombiedoors$setDoorShieldSunshadeTicks(int ticks) {
		zombiedoors$doorShieldSunshadeTicks = Math.max(0, ticks);
	}

	@Override
	public int zombiedoors$getDoorShieldPoseCooldownTicks() {
		return zombiedoors$doorShieldPoseCooldownTicks;
	}

	@Override
	public void zombiedoors$setDoorShieldPoseCooldownTicks(int ticks) {
		zombiedoors$doorShieldPoseCooldownTicks = Math.max(0, ticks);
	}

	@Override
	public int zombiedoors$getDoorWhackHitCooldownTicks() {
		return zombiedoors$doorWhackHitCooldownTicks;
	}

	@Override
	public void zombiedoors$setDoorWhackHitCooldownTicks(int ticks) {
		zombiedoors$doorWhackHitCooldownTicks = Math.max(0, ticks);
	}

	@Override
	public boolean zombiedoors$isDoorWhackDamageReady() {
		return zombiedoors$doorWhackDamageReady;
	}

	@Override
	public void zombiedoors$setDoorWhackDamageReady(boolean ready) {
		zombiedoors$doorWhackDamageReady = ready;
	}

	@Override
	public String zombiedoors$getDoorShieldArrowImpacts() {
		return ((Zombie) (Object) this).getEntityData().get(ZOMBIE_DOORS_DOOR_SHIELD_ARROW_IMPACTS);
	}

	@Override
	public void zombiedoors$setDoorShieldArrowImpacts(String impacts) {
		((Zombie) (Object) this).getEntityData().set(
			ZOMBIE_DOORS_DOOR_SHIELD_ARROW_IMPACTS,
			ZombieDoorShieldArrowImpact.normalize(impacts)
		);
	}

	@Override
	public byte zombiedoors$getPreviousAnimatedDoorShieldPose() {
		return zombiedoors$previousAnimatedDoorShieldPose;
	}

	@Override
	public byte zombiedoors$getTargetAnimatedDoorShieldPose() {
		return zombiedoors$targetAnimatedDoorShieldPose;
	}

	@Override
	public float zombiedoors$getDoorShieldPoseProgress(float partialTick) {
		float progress = zombiedoors$doorShieldPoseProgressOld
			+ (zombiedoors$doorShieldPoseProgress - zombiedoors$doorShieldPoseProgressOld) * partialTick;
		return Math.max(0.0F, Math.min(1.0F, progress));
	}

	@Override
	public void zombiedoors$startDoorShieldPoseAnimation(byte previousPose, byte targetPose) {
		zombiedoors$previousAnimatedDoorShieldPose = previousPose;
		zombiedoors$targetAnimatedDoorShieldPose = targetPose;
		zombiedoors$doorShieldPoseProgress = previousPose == targetPose ? 1.0F : 0.0F;
		zombiedoors$doorShieldPoseProgressOld = zombiedoors$doorShieldPoseProgress;
	}

	@Override
	public void zombiedoors$advanceDoorShieldPoseAnimation(int transitionTicks) {
		zombiedoors$doorShieldPoseProgressOld = zombiedoors$doorShieldPoseProgress;
		zombiedoors$doorShieldPoseProgress = Math.min(
			1.0F,
			zombiedoors$doorShieldPoseProgress + 1.0F / Math.max(1, transitionTicks)
		);
	}
}
