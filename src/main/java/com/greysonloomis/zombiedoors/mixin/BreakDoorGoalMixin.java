package com.greysonloomis.zombiedoors.mixin;

import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldBehavior;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalMixin extends DoorInteractGoal {
	protected BreakDoorGoalMixin(Mob mob) {
		super(mob);
	}

	@Redirect(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
		)
	)
	private boolean zombiedoors$equipBrokenWoodenDoor(Level level, BlockPos pos, boolean moving) {
		BlockState state = level.getBlockState(pos);
		boolean willAcquire = mob instanceof Zombie zombie
			&& ZombieDoorShieldBehavior.canAcquireBrokenDoor(zombie, state);
		AABB dropArea = new AABB(pos).inflate(2.0);
		Set<Integer> existingDoorDrops = willAcquire
			? level.getEntitiesOfClass(ItemEntity.class, dropArea).stream()
				.map(ItemEntity::getId)
				.collect(Collectors.toSet())
			: Set.of();
		boolean removed = level.removeBlock(pos, moving);
		if (removed && willAcquire && mob instanceof Zombie zombie) {
			Item doorItem = state.getBlock().asItem();
			level.getEntitiesOfClass(ItemEntity.class, dropArea, item ->
				!existingDoorDrops.contains(item.getId())
					&& item.getItem().is(doorItem)
			).forEach(ItemEntity::discard);
			ZombieDoorShieldBehavior.acquireBrokenDoor(zombie, state);
		}
		return removed;
	}
}
