package com.greysonloomis.zombiedoors.client.render;

import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldAccess;

public record ZombieDoorShieldPose(float x, float y, float z, float pitch,
                                  float armPitch, float armYaw, float armRoll, float armZ) {
    public static ZombieDoorShieldPose sample(ZombieDoorShieldRenderStateAccess state) {
        var from = pose(state.zombiedoors$getRenderedPreviousDoorShieldPose());
        var to = pose(state.zombiedoors$getRenderedDoorShieldPose());
        float t = Math.clamp(state.zombiedoors$getRenderedDoorShieldPoseProgress(), 0, 1);
        t = t * t * (3 - 2 * t);
        float recoil = state.zombiedoors$getRenderedDoorRecoil();
        // Disabled doors stay at peak recoil.
        float held = mix(isDisabled(state.zombiedoors$getRenderedPreviousDoorShieldPose()),
            isDisabled(state.zombiedoors$getRenderedDoorShieldPose()), t);
        recoil = Math.max(held, recoil);
        return new ZombieDoorShieldPose(mix(from.x, to.x, t), mix(from.y, to.y, t),
            mix(from.z, to.z, t) + .10F * recoil, mix(from.pitch, to.pitch, t),
            mix(from.armPitch, to.armPitch, t), mix(from.armYaw, to.armYaw, t),
            mix(from.armRoll, to.armRoll, t), 1.6F * recoil);
    }

    private static float isDisabled(byte pose) { return pose == ZombieDoorShieldAccess.POSE_DISABLED ? 1 : 0; }
    private static float mix(float a, float b, float t) { return a + (b - a) * t; }

    private static ZombieDoorShieldPose pose(byte pose) {
        return switch (pose) {
            case ZombieDoorShieldAccess.POSE_SUNSHADE -> new ZombieDoorShieldPose(-.5F, 2.9875F, -.95F, 90, -(float) Math.PI, 0, 0, 0);
            case ZombieDoorShieldAccess.POSE_ATTACKING -> new ZombieDoorShieldPose(-.5F, .27F, -.93F, -68, -.95F, .08F, .03F, 0);
            case ZombieDoorShieldAccess.POSE_BLOCKING, ZombieDoorShieldAccess.POSE_DISABLED ->
                new ZombieDoorShieldPose(-.5F, 0, -1.65F, 0, -1.18F, .12F, .02F, 0);
            default -> new ZombieDoorShieldPose(-.5F, 0, -1.35F, -12, -1.05F, .18F, .08F, 0);
        };
    }
}
