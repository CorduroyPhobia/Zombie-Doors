package com.greysonloomis.zombiedoors.mixin.client;

import com.greysonloomis.zombiedoors.ZombieDoors;
import com.greysonloomis.zombiedoors.client.render.ZombieDoorShieldLayer;
import com.greysonloomis.zombiedoors.client.render.ZombieDoorShieldRenderStateAccess;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldAccess;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldArrowImpact;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldBehavior;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.ZombifiedPiglinRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityRenderer.class, priority = 2100)
public abstract class LivingEntityRendererDoorShieldMixin<
	T extends LivingEntity,
	S extends LivingEntityRenderState,
	M extends EntityModel<? super S>
> {
	@Unique
	private static final BlockDisplayContext ZOMBIE_DOORS_DOOR_DISPLAY_CONTEXT =
		BlockDisplayContext.create();

	@Unique
	private BlockModelResolver zombiedoors$doorBlockModelResolver;

	@Shadow
	protected abstract boolean addLayer(RenderLayer<S, M> layer);

	@Inject(method = "<init>", at = @At("TAIL"))
	private void zombiedoors$addDoorShieldLayer(
		EntityRendererProvider.Context context,
		M model,
		float shadowRadius,
		CallbackInfo callback
	) {
		zombiedoors$doorBlockModelResolver = context.getBlockModelResolver();
		if ((Object) this instanceof AbstractZombieRenderer<?, ?, ?>
			|| (Object) this instanceof ZombieVillagerRenderer
            || (Object) this instanceof ZombifiedPiglinRenderer) {
			try {
				LivingEntityRenderer<T, S, M> renderer =
					(LivingEntityRenderer<T, S, M>) (Object) this;
				addLayer(new ZombieDoorShieldLayer<>(renderer, context));
			} catch (RuntimeException exception) {
				ZombieDoors.LOGGER.warn(
					"Could not add wooden-door shield layer to {}",
					getClass().getName(),
					exception
				);
			}
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void zombiedoors$copyDoorShieldState(
		T entity,
		S state,
		float partialTick,
		CallbackInfo callback
	) {
		ZombieDoorShieldRenderStateAccess renderState =
			(ZombieDoorShieldRenderStateAccess) state;
		// Render states are reused after extinguishing or losing a door.
		renderState.zombiedoors$setDoorBlockLightOverride(-1);
		if (!(entity instanceof Zombie zombie)
			|| !(zombie instanceof ZombieDoorShieldAccess doorZombie)
			|| !ZombieDoorShieldBehavior.canHoldDoor(zombie, doorZombie.zombiedoors$getDoorShield())) {
			renderState.zombiedoors$setRenderedDoorShield(ItemStack.EMPTY);
			renderState.zombiedoors$setRenderedDoorShieldPose(ZombieDoorShieldAccess.POSE_NONE);
			renderState.zombiedoors$setRenderedPreviousDoorShieldPose(ZombieDoorShieldAccess.POSE_NONE);
			renderState.zombiedoors$setRenderedDoorShieldPoseProgress(1.0F);
			renderState.zombiedoors$setRenderedDoorShieldArrowImpacts(java.util.List.of());
			renderState.zombiedoors$getLowerDoorModel().clear();
			renderState.zombiedoors$getUpperDoorModel().clear();
			renderState.zombiedoors$setDoorCracks(null, null, -1);
			renderState.zombiedoors$setRenderedDoorRecoil(0);
			return;
		}

		ItemStack doorStack = doorZombie.zombiedoors$getDoorShield().copy();
		if (zombie.isOnFire()) {
			// Burning mobs get block light 15; the door still needs ambient light.
			var probe = BlockPos.containing(zombie.getLightProbePosition(partialTick));
			renderState.zombiedoors$setDoorBlockLightOverride(
				zombie.level().getBrightness(LightLayer.BLOCK, probe));
		}
		float age = ZombieDoorShieldBehavior.IMPACT_ANIMATION_TICKS
			- doorZombie.zombiedoors$getDoorImpactTicks() + partialTick;
		// Quick two-tick compression, then six ticks returning to the normal guard.
		float recoil = doorZombie.zombiedoors$getDoorImpactTicks() <= 0 ? 0
			: age < 2 ? age / 2 : Math.max(0, (8 - age) / 6);
		renderState.zombiedoors$setRenderedDoorRecoil(recoil);
		renderState.zombiedoors$setRenderedDoorShield(doorStack);
		renderState.zombiedoors$setRenderedDoorShieldPose(
			doorZombie.zombiedoors$getTargetAnimatedDoorShieldPose()
		);
		renderState.zombiedoors$setRenderedPreviousDoorShieldPose(
			doorZombie.zombiedoors$getPreviousAnimatedDoorShieldPose()
		);
		renderState.zombiedoors$setRenderedDoorShieldPoseProgress(
			doorZombie.zombiedoors$getDoorShieldPoseProgress(partialTick)
		);
		renderState.zombiedoors$setRenderedDoorShieldArrowImpacts(
			ZombieDoorShieldArrowImpact.decode(doorZombie.zombiedoors$getDoorShieldArrowImpacts())
		);
		if (doorStack.getItem() instanceof BlockItem blockItem
			&& blockItem.getBlock() instanceof DoorBlock) {
			BlockState lower = blockItem.getBlock().defaultBlockState()
				.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
			BlockState upper = lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
			var models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
			renderState.zombiedoors$setDoorCracks(models.get(lower), models.get(upper),
				ZombieDoorShieldRules.doorDamageStage(doorZombie.zombiedoors$getDoorShieldDurability(),
					doorZombie.zombiedoors$getDoorShieldMaxDurability()));
			zombiedoors$doorBlockModelResolver.update(
				renderState.zombiedoors$getLowerDoorModel(), lower, ZOMBIE_DOORS_DOOR_DISPLAY_CONTEXT
			);
			zombiedoors$doorBlockModelResolver.update(
				renderState.zombiedoors$getUpperDoorModel(), upper, ZOMBIE_DOORS_DOOR_DISPLAY_CONTEXT
			);
		}
	}
}
