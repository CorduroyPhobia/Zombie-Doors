package com.greysonloomis.zombiedoors.gametest;

import com.greysonloomis.zombiedoors.client.render.ZombieDoorShieldRenderStateAccess;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldAccess;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldArrowImpact;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ZombieDoorsThumbnailCapture implements FabricClientGameTest {
    private record Variant(String name, EntityType<? extends Zombie> mob, Item door) {}

    @Override public void runTest(ClientGameTestContext context) {
        var variants = List.of(
            new Variant("01-zombie-oak", EntityTypes.ZOMBIE, Items.OAK_DOOR),
            new Variant("02-husk-acacia", EntityTypes.HUSK, Items.ACACIA_DOOR),
            new Variant("03-villager-spruce", EntityTypes.ZOMBIE_VILLAGER, Items.SPRUCE_DOOR),
            new Variant("04-zombie-birch", EntityTypes.ZOMBIE, Items.BIRCH_DOOR),
            new Variant("05-husk-jungle", EntityTypes.HUSK, Items.JUNGLE_DOOR),
            new Variant("06-villager-dark-oak", EntityTypes.ZOMBIE_VILLAGER, Items.DARK_OAK_DOOR),
            new Variant("07-zombie-cherry", EntityTypes.ZOMBIE, Items.CHERRY_DOOR),
            new Variant("08-husk-bamboo", EntityTypes.HUSK, Items.BAMBOO_DOOR),
            new Variant("09-villager-mangrove", EntityTypes.ZOMBIE_VILLAGER, Items.MANGROVE_DOOR),
            new Variant("10-zombie-pale-oak", EntityTypes.ZOMBIE, Items.PALE_OAK_DOOR),
            new Variant("11-husk-crimson", EntityTypes.HUSK, Items.CRIMSON_DOOR),
            new Variant("12-villager-warped", EntityTypes.ZOMBIE_VILLAGER, Items.WARPED_DOOR));
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(mc -> {
                // The screenshot API resizes the render target but does not recalculate GUI scale.
                mc.getWindow().setWidth(1536);
                mc.getWindow().setHeight(1536);
                mc.getWindow().setGuiScale(1);
                mc.gameRenderer.mainRenderTarget().resize(1536, 1536);
            });
            for (var variant : variants) {
                context.runOnClient(mc -> {
                    Zombie zombie = variant.mob.create(mc.level, EntitySpawnReason.COMMAND);
                    zombie.setId(1);
                    zombie.setBaby(false);
                    zombie.setNoAi(true);
                    zombie.setPos(mc.player.position());
                    zombie.setYRot(180);
                    zombie.setYHeadRot(180);
                    zombie.yBodyRot = 180;
                    if (zombie instanceof ZombieVillager villager) {
                        var type = variant.door == Items.MANGROVE_DOOR ? VillagerType.SWAMP
                            : variant.door == Items.SPRUCE_DOOR ? VillagerType.TAIGA : VillagerType.PLAINS;
                        villager.setVillagerData(villager.getVillagerData()
                            .withType(mc.level.registryAccess(), type)
                            .withProfession(mc.level.registryAccess(), VillagerProfession.NONE));
                    }
                    var shield = (ZombieDoorShieldAccess) zombie;
                    shield.zombiedoors$setDoorShield(new ItemStack(variant.door), 48);
                    shield.zombiedoors$setDoorShieldPose(ZombieDoorShieldAccess.POSE_BLOCKING);
                    shield.zombiedoors$startDoorShieldPoseAnimation(ZombieDoorShieldAccess.POSE_BLOCKING, ZombieDoorShieldAccess.POSE_BLOCKING);
                    String arrows = "";
                    for (var impact : List.of(
                        new ZombieDoorShieldArrowImpact(.24F, .43F, .14F, -.07F, .988F),
                        new ZombieDoorShieldArrowImpact(.85F, 1.10F, -.12F, .04F, .992F),
                        new ZombieDoorShieldArrowImpact(.72F, 1.90F, .06F, -.10F, .993F))) {
                        arrows = ZombieDoorShieldArrowImpact.append(arrows, impact);
                    }
                    shield.zombiedoors$setDoorShieldArrowImpacts(arrows);
                    mc.gui.setScreen(new StudioScreen(zombie));
                });
                context.waitTicks(3);
                context.takeScreenshot(TestScreenshotOptions.of(variant.name).disableCounterPrefix().withSize(1536, 1536));
            }
            context.setScreen(() -> null);
        }
    }

    private static final class StudioScreen extends Screen {
        private final Zombie zombie;
        StudioScreen(Zombie zombie) { super(Component.literal("Zombie Doors thumbnail capture")); this.zombie = zombie; }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {}
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
            int width = graphics.guiWidth(), height = graphics.guiHeight();
            // Chroma key for artwork/build_thumbnail.py.
            graphics.fill(0, 0, width, height, 0xFFFF00FF);
            var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(zombie);
            var state = (LivingEntityRenderState) renderer.createRenderState(zombie, 1);
            state.bodyRot = 220; // 40 degrees toward the left in the inventory camera.
            state.yRot = -25; // A small glance toward the camera keeps the mob's face readable.
            state.xRot = 0;
            state.ageInTicks = 0;
            state.walkAnimationPos = state.walkAnimationSpeed = 0;
            state.lightCoords = 0xF000F0;
            state.shadowPieces.clear();
            state.outlineColor = 0;
            state.displayFireAnimation = false;
            state.scale = 1;
            var door = (ZombieDoorShieldRenderStateAccess) state;
            door.zombiedoors$setRenderedDoorShieldPoseProgress(1);
            var tilt = new Quaternionf().rotationX((float) Math.toRadians(-25));
            graphics.entity(state, height * .30F, new Vector3f(0, 1.05F, 0),
                new Quaternionf().rotationZ((float) Math.PI).mul(tilt), tilt, 0, 0, width, height);
        }
    }
}
