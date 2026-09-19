package com.greysonloomis.zombiedoors.client.render;

import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldArrowImpact;
import java.util.List;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.item.ItemStack;

public interface ZombieDoorShieldRenderStateAccess {
    /** Ambient block light while burning; -1 preserves the caller's normal lighting. */
    int zombiedoors$getDoorBlockLightOverride();
    void zombiedoors$setDoorBlockLightOverride(int blockLight);

    float zombiedoors$getRenderedDoorRecoil();
    void zombiedoors$setRenderedDoorRecoil(float recoil);
    int zombiedoors$getDoorDamageStage();
    BlockStateModel zombiedoors$getLowerCrackModel();
    BlockStateModel zombiedoors$getUpperCrackModel();
    void zombiedoors$setDoorCracks(BlockStateModel lower, BlockStateModel upper, int stage);

	ItemStack zombiedoors$getRenderedDoorShield();

	void zombiedoors$setRenderedDoorShield(ItemStack stack);

	byte zombiedoors$getRenderedDoorShieldPose();

	void zombiedoors$setRenderedDoorShieldPose(byte pose);

	byte zombiedoors$getRenderedPreviousDoorShieldPose();

	void zombiedoors$setRenderedPreviousDoorShieldPose(byte pose);

	float zombiedoors$getRenderedDoorShieldPoseProgress();

	void zombiedoors$setRenderedDoorShieldPoseProgress(float progress);

	List<ZombieDoorShieldArrowImpact> zombiedoors$getRenderedDoorShieldArrowImpacts();

	void zombiedoors$setRenderedDoorShieldArrowImpacts(List<ZombieDoorShieldArrowImpact> impacts);

	BlockModelRenderState zombiedoors$getLowerDoorModel();

	BlockModelRenderState zombiedoors$getUpperDoorModel();
}
