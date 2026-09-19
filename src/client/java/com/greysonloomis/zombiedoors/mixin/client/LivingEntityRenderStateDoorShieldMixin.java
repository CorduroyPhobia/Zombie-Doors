package com.greysonloomis.zombiedoors.mixin.client;

import com.greysonloomis.zombiedoors.client.render.ZombieDoorShieldRenderStateAccess;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldArrowImpact;
import java.util.List;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateDoorShieldMixin
	implements ZombieDoorShieldRenderStateAccess {
	@Unique private int zombiedoors$doorBlockLightOverride = -1;
	@Override public int zombiedoors$getDoorBlockLightOverride() { return zombiedoors$doorBlockLightOverride; }
	@Override public void zombiedoors$setDoorBlockLightOverride(int blockLight) { zombiedoors$doorBlockLightOverride = blockLight; }

	@Unique private float zombiedoors$doorRecoil;
	@Override public float zombiedoors$getRenderedDoorRecoil() { return zombiedoors$doorRecoil; }
	@Override public void zombiedoors$setRenderedDoorRecoil(float recoil) { zombiedoors$doorRecoil = recoil; }
	@Unique private int zombiedoors$damageStage = -1;
	@Unique private BlockStateModel zombiedoors$lowerCrackModel;
	@Unique private BlockStateModel zombiedoors$upperCrackModel;
	@Override public int zombiedoors$getDoorDamageStage() { return zombiedoors$damageStage; }
	@Override public BlockStateModel zombiedoors$getLowerCrackModel() { return zombiedoors$lowerCrackModel; }
	@Override public BlockStateModel zombiedoors$getUpperCrackModel() { return zombiedoors$upperCrackModel; }
	@Override public void zombiedoors$setDoorCracks(BlockStateModel lower, BlockStateModel upper, int stage) {
		zombiedoors$lowerCrackModel = lower;
		zombiedoors$upperCrackModel = upper;
		zombiedoors$damageStage = stage;
	}
	@Unique
	private ItemStack zombiedoors$renderedDoorShield = ItemStack.EMPTY;
	@Unique
	private byte zombiedoors$renderedDoorShieldPose;
	@Unique
	private byte zombiedoors$renderedPreviousDoorShieldPose;
	@Unique
	private float zombiedoors$renderedDoorShieldPoseProgress = 1.0F;
	@Unique
	private List<ZombieDoorShieldArrowImpact> zombiedoors$renderedDoorShieldArrowImpacts = List.of();
	@Unique
	private final BlockModelRenderState zombiedoors$lowerDoorModel = new BlockModelRenderState();
	@Unique
	private final BlockModelRenderState zombiedoors$upperDoorModel = new BlockModelRenderState();

	@Override
	public ItemStack zombiedoors$getRenderedDoorShield() {
		return zombiedoors$renderedDoorShield;
	}

	@Override
	public void zombiedoors$setRenderedDoorShield(ItemStack stack) {
		zombiedoors$renderedDoorShield = stack;
	}

	@Override
	public byte zombiedoors$getRenderedDoorShieldPose() {
		return zombiedoors$renderedDoorShieldPose;
	}

	@Override
	public void zombiedoors$setRenderedDoorShieldPose(byte pose) {
		zombiedoors$renderedDoorShieldPose = pose;
	}

	@Override
	public byte zombiedoors$getRenderedPreviousDoorShieldPose() {
		return zombiedoors$renderedPreviousDoorShieldPose;
	}

	@Override
	public void zombiedoors$setRenderedPreviousDoorShieldPose(byte pose) {
		zombiedoors$renderedPreviousDoorShieldPose = pose;
	}

	@Override
	public float zombiedoors$getRenderedDoorShieldPoseProgress() {
		return zombiedoors$renderedDoorShieldPoseProgress;
	}

	@Override
	public void zombiedoors$setRenderedDoorShieldPoseProgress(float progress) {
		zombiedoors$renderedDoorShieldPoseProgress = progress;
	}

	@Override
	public List<ZombieDoorShieldArrowImpact> zombiedoors$getRenderedDoorShieldArrowImpacts() {
		return zombiedoors$renderedDoorShieldArrowImpacts;
	}

	@Override
	public void zombiedoors$setRenderedDoorShieldArrowImpacts(
		List<ZombieDoorShieldArrowImpact> impacts
	) {
		zombiedoors$renderedDoorShieldArrowImpacts = List.copyOf(impacts);
	}

	@Override
	public BlockModelRenderState zombiedoors$getLowerDoorModel() {
		return zombiedoors$lowerDoorModel;
	}

	@Override
	public BlockModelRenderState zombiedoors$getUpperDoorModel() {
		return zombiedoors$upperDoorModel;
	}
}
