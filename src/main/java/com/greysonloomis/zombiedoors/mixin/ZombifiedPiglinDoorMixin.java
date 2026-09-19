package com.greysonloomis.zombiedoors.mixin;

import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldBehavior;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ZombifiedPiglin.class)
public abstract class ZombifiedPiglinDoorMixin {
    @Inject(method = "populateDefaultEquipmentSlots", at = @At("TAIL"))
    private void zombiedoors$spawnDoor(RandomSource random, DifficultyInstance difficulty, CallbackInfo callback) {
        ZombieDoorShieldBehavior.maybeEquipSpawnedDoor((ZombifiedPiglin) (Object) this, random);
    }
}
