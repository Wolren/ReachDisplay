package net.wolren.reach_display.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.wolren.reach_display.config.DisplayConfig;
import net.wolren.reach_display.data.SharedData;

public class ReachCalculation {
    /**
     * Measures the reach to {@code target} using the configured method and
     * records the hit (last distance, target, average, timestamp) when the
     * measurement is valid.
     */
    public static void recordHit(Player player, Entity target) {
        double reach = measureReach(player, target);
        if (reach == -1) return;

        SharedData data = SharedData.getInstance();
        data.setDistanceAndTarget(reach, target);
        data.addDistanceToAverage(reach);
        data.setLastHitTimestamp(System.currentTimeMillis());
    }

    /**
     * Measures the reach to {@code target} using the configured method.
     * In RAY_HIT_POINT mode the current crosshair hit must actually be on
     * {@code target} — otherwise the distance belongs to a different entity
     * and -1 is returned so the caller can skip the hit.
     */
    public static double measureReach(Player player, Entity target) {
        Minecraft client = Minecraft.getInstance();
        Vec3 eyePos = player.getEyePosition();

        if (DisplayConfig.hitDistanceCalculationMethod == DisplayConfig.DistanceCalculationMethod.RAY_HIT_POINT) {
            HitResult result = client.hitResult;
            if (!(result instanceof EntityHitResult hitResult)) return -1;
            if (hitResult.getEntity() != target) return -1;
            return eyePos.distanceTo(hitResult.getLocation());
        }

        AABB box = target.getBoundingBox();
        double cx = Math.clamp(eyePos.x, box.minX, box.maxX);
        double cy = Math.clamp(eyePos.y, box.minY, box.maxY);
        double cz = Math.clamp(eyePos.z, box.minZ, box.maxZ);
        return eyePos.distanceTo(new Vec3(cx, cy, cz));
    }
}
