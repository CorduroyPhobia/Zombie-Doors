package com.greysonloomis.zombiedoors.mixin;

import com.greysonloomis.zombiedoors.ZombieDoors;
import com.greysonloomis.zombiedoors.gameplay.ZombieDoorShieldBehavior;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobDoorShieldMixin {
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Mob;canPickUpLoot()Z"))
    private boolean zombiedoors$alwaysCheckForDoors(boolean vanillaCanPickUpLoot) {
        // Vanilla still handles range, delay, Mob Griefing and stack consumption.
        return vanillaCanPickUpLoot || (Object) this instanceof Zombie zombie
            && ZombieDoorShieldBehavior.canAcquire(zombie);
    }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Mob;wantsToPickUp(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean zombiedoors$limitExtraPickupToDoors(Mob mob, ServerLevel level,
                                                       ItemStack stack, Operation<Boolean> original) {
        if (mob instanceof Zombie && !mob.canPickUpLoot()
            && !ZombieDoorShieldBehavior.isWoodenDoor(stack)) {
            return false;
        }
        return original.call(mob, level, stack);
    }

    @ModifyExpressionValue(method = "burnUndead", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/Mob;isSunBurnTick()Z"))
    private boolean zombiedoors$shade(boolean vanillaSunBurnTick) {
        return (Object) this instanceof Zombie zombie
            && ZombieDoorShieldBehavior.protectFromSun(zombie, vanillaSunBurnTick)
            ? false : vanillaSunBurnTick;
    }

    @ModifyReturnValue(method = "isWithinMeleeAttackRange", at = @At("RETURN"))
    private boolean zombiedoors$reach(boolean vanillaInRange, LivingEntity target) {
        return vanillaInRange || (Object) this instanceof Zombie zombie
            && ZombieDoorShieldBehavior.isDoorWhackInRange(zombie, target);
    }

    @Inject(method = "equipItemIfPossible", at = @At("HEAD"), cancellable = true)
    private void zombiedoors$pickup(ServerLevel level, ItemStack stack,
                                  CallbackInfoReturnable<ItemStack> callback) {
        if ((Object) this instanceof Zombie zombie
            && ZombieDoorShieldBehavior.isWoodenDoor(stack)) {
            callback.setReturnValue(ZombieDoorShieldBehavior.equipPickedUpDoor(zombie, level, stack));
        } else if ((Object) this instanceof Zombie zombie
            && ZombieDoorShieldBehavior.refusesEquipmentPickup(zombie, stack)) {
            callback.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void zombiedoors$doorNavigation(CallbackInfo callback) {
        if ((Object) this instanceof Zombie zombie) {
            zombie.getNavigation().setCanOpenDoors(zombie.canBreakDoors()
                || ZombieDoors.configOrDefaults().enableReliableZombieDoorBreaking());
        }
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("TAIL"))
    private void zombiedoors$dropDoor(ServerLevel level, DamageSource source,
                                    boolean recentlyHit, CallbackInfo callback) {
        if ((Object) this instanceof Zombie zombie) {
            ZombieDoorShieldBehavior.dropDoor(level, zombie);
        }
    }
}
