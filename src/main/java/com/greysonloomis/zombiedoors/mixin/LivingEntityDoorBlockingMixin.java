package com.greysonloomis.zombiedoors.mixin;

import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use vanilla's blocking stage so invulnerability, hit feedback and damage accounting stay intact. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDoorBlockingMixin {
    @Shadow protected abstract void blockUsingItem(ServerLevel level, LivingEntity attacker, DamageSource source, float damage, boolean fullyBlocked);

    @Inject(method = "applyItemBlocking", at = @At("HEAD"), cancellable = true)
    private void zombiedoors$blockMelee(ServerLevel level, DamageSource source, float damage,
                                      CallbackInfoReturnable<Float> callback) {
        if ((Object) this instanceof Zombie zombie) {
            float blocked = ZombieDoorShieldBehavior.blockMelee(zombie, source, damage);
            if (blocked > 0) {
                blockUsingItem(level, (LivingEntity) source.getDirectEntity(), source, damage, blocked >= damage);
                callback.setReturnValue(blocked);
            }
        }
    }
}
