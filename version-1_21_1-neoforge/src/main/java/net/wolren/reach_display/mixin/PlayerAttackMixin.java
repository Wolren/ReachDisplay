package net.wolren.reach_display.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.wolren.reach_display.config.DisplayConfig;
import net.wolren.reach_display.data.SharedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class PlayerAttackMixin {

    @Inject(method = "attack(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void onAttack(Player player, Entity target, CallbackInfo ci) {

        DisplayConfig.DistanceCalculationMethod distanceCalculationMethod = DisplayConfig.hitDistanceCalculationMethod;

        if (distanceCalculationMethod == DisplayConfig.DistanceCalculationMethod.RAY_HIT_POINT) {
            Minecraft client = Minecraft.getInstance();

            HitResult result = client.hitResult;
            if (!(result instanceof EntityHitResult hitResult)) return;

            Entity hitEntity = hitResult.getEntity();
            if (hitEntity == null || hitEntity != target) return;

            Vec3 eyePos = player.getEyePosition();
            Vec3 hitPos = hitResult.getLocation();
            double rayDistance = eyePos.distanceTo(hitPos);

            SharedData data = SharedData.getInstance();
            data.setDistanceAndTarget(rayDistance, target);
            data.addDistanceToAverage(rayDistance);
            data.setLastHitTimestamp(System.currentTimeMillis());
        } else {
            Vec3 eyePos = player.getEyePosition();

            AABB box = target.getBoundingBox();

            double closestX = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
            double closestY = Math.max(box.minY, Math.min(eyePos.y, box.maxY));
            double closestZ = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));

            Vec3 closestPoint = new Vec3(closestX, closestY, closestZ);
            double distance = eyePos.distanceTo(closestPoint);

            SharedData data = SharedData.getInstance();
            data.setDistanceAndTarget(distance, target);
            data.addDistanceToAverage(distance);
            data.setLastHitTimestamp(System.currentTimeMillis());
        }
    }
}
