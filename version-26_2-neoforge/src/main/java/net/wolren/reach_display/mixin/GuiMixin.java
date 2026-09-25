package net.wolren.reach_display.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.wolren.reach_display.config.DisplayConfig;
import net.wolren.reach_display.data.SharedData;
import net.wolren.reach_display.filter.EntityFilterHelper;
import net.wolren.reach_display.utils.CustomRender;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private double reach$smoothedCrosshairDistance = 0.0;
    @Unique
    private long reach$lastDistanceUpdateTime = 0L;
    @Unique
    private String reach$lastDistanceDisplayString = "";
    @Unique
    private int reach$lastDistanceColor = 0xFFFFFFFF;

    @Inject(at = @At("TAIL"), method = "extractRenderState")
    public void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter, CallbackInfo ci) {
        if (!DisplayConfig.enabled) return;
        Player player = minecraft.player;
        HitResult target = minecraft.hitResult;
        if (player == null || target == null) return;

        SharedData sharedData = SharedData.getInstance();

        if (DisplayConfig.distanceEnable && target.getType() == HitResult.Type.ENTITY) {
            Entity targetEntity = ((EntityHitResult) target).getEntity();
            if (!targetEntity.isInvisibleTo(player) && (!DisplayConfig.entityFilterEnable || EntityFilterHelper.shouldTrack(targetEntity))) {
                String displayString;
                int argbColor;
                long now = System.currentTimeMillis();
                if (DisplayConfig.distanceUpdateRate > 0 && now - reach$lastDistanceUpdateTime < DisplayConfig.distanceUpdateRate) {
                    displayString = reach$lastDistanceDisplayString;
                    argbColor = reach$lastDistanceColor;
                } else {
                    // One distance, shared by the number and the colour, so the two can never disagree.
                    Double dist = computeDistance(player, targetEntity);
                    displayString = dist == null ? "" : formatDistance(dist);
                    argbColor = CustomRender.parseARGBColorWithOpacity(DisplayConfig.distanceOpacity,
                            CustomRender.resolveDistanceColorInt(player.isCreative(), dist == null ? 0.0 : dist));
                    reach$lastDistanceUpdateTime = now;
                    reach$lastDistanceDisplayString = displayString;
                    reach$lastDistanceColor = argbColor;
                }
                boolean shadow = DisplayConfig.distanceShadow;
                int bgColor = DisplayConfig.distanceBackground
                        ? CustomRender.parseARGBColorWithOpacity(DisplayConfig.distanceBackgroundOpacity,
                        CustomRender.parseColorWithDefault(DisplayConfig.distanceBackgroundColor)) : 0;
                int shadowColor = DisplayConfig.distanceShadow
                        ? CustomRender.parseARGBColorWithOpacity(1.0f,
                        CustomRender.parseColorWithDefault(DisplayConfig.distanceShadowColor)) : 0;
                CustomRender.renderText(minecraft, graphics, displayString, argbColor, shadow, DisplayConfig.distanceScale,
                        DisplayConfig.xOffset, DisplayConfig.yOffset, true, DisplayConfig.distanceBackground, bgColor, shadowColor);
            }
        }

        if (DisplayConfig.hitDistanceEnable) {
            Entity entity = sharedData.getEntity();
            boolean hitTimedOut = !DisplayConfig.hitDistanceKeep && sharedData.isLastHitExpired(DisplayConfig.hitDistanceResetSeconds * 1000L);
            if (entity != null && !hitTimedOut && !entity.isInvisibleTo(player)
                    && (!DisplayConfig.entityFilterEnable || EntityFilterHelper.shouldTrack(entity))) {
                String displayString = getHitDisplayString(sharedData.getDistance());

                double hitDist = sharedData.getDistance();
                int argbColor = CustomRender.parseARGBColorWithOpacity(DisplayConfig.hitDistanceOpacity,
                        CustomRender.resolveHitColorInt(player.isCreative(), hitDist));
                boolean shadow = DisplayConfig.hitDistanceShadow;
                int bgColor = DisplayConfig.hitDistanceBackground
                        ? CustomRender.parseARGBColorWithOpacity(DisplayConfig.hitDistanceBackgroundOpacity,
                        CustomRender.parseColorWithDefault(DisplayConfig.hitDistanceBackgroundColor)) : 0;
                int shadowColor = DisplayConfig.hitDistanceShadow
                        ? CustomRender.parseARGBColorWithOpacity(1.0f,
                        CustomRender.parseColorWithDefault(DisplayConfig.hitDistanceShadowColor)) : 0;
                CustomRender.renderText(minecraft, graphics, displayString, argbColor, shadow, DisplayConfig.hitDistanceScale,
                        DisplayConfig.hitXOffset, DisplayConfig.hitYOffset, false, DisplayConfig.hitDistanceBackground, bgColor, shadowColor);
            }
        }

        if (DisplayConfig.averageHitDistanceEnable) {
            Entity entity = sharedData.getEntity();
            boolean avgTimedOut = !DisplayConfig.hitDistanceKeep && sharedData.isLastHitExpired(DisplayConfig.hitDistanceResetSeconds * 1000L);
            if (entity != null && !avgTimedOut && (!DisplayConfig.entityFilterEnable || EntityFilterHelper.shouldTrack(entity))) {
                String displayString = getAverageHitDisplayString(sharedData.getAverageDistance());

                double avgDist = sharedData.getAverageDistance();
                int argbColor = CustomRender.parseARGBColorWithOpacity(DisplayConfig.averageHitDistanceOpacity,
                        CustomRender.resolveAverageColorInt(player.isCreative(), avgDist));
                boolean shadow = DisplayConfig.averageHitDistanceShadow;
                int bgColor = DisplayConfig.averageHitDistanceBackground
                        ? CustomRender.parseARGBColorWithOpacity(DisplayConfig.averageHitDistanceBackgroundOpacity,
                        CustomRender.parseColorWithDefault(DisplayConfig.averageHitDistanceBackgroundColor)) : 0;
                int shadowColor = DisplayConfig.averageHitDistanceShadow
                        ? CustomRender.parseARGBColorWithOpacity(1.0f,
                        CustomRender.parseColorWithDefault(DisplayConfig.averageHitDistanceShadowColor)) : 0;
                CustomRender.renderText(minecraft, graphics, displayString, argbColor, shadow, DisplayConfig.averageHitDistanceScale,
                        DisplayConfig.averageHitXOffset, DisplayConfig.averageHitYOffset, false, DisplayConfig.averageHitDistanceBackground, bgColor, shadowColor);
            }
        }
    }

    @Unique
    private double smoothDistance(double target) {
        double diff = target - reach$smoothedCrosshairDistance;
        reach$smoothedCrosshairDistance += diff * 0.3;
        if (Math.abs(diff) < 0.001) reach$smoothedCrosshairDistance = target;
        return reach$smoothedCrosshairDistance;
    }

    @Unique
    private String getAverageHitDisplayString(Double distance) {
        String rounded = CustomRender.getRoundedDouble(distance, DisplayConfig.averageHitDistanceDecimalPlaces);
        return CustomRender.applyFontStyle(
                CustomRender.applyDisplayFormat(rounded, DisplayConfig.averageHitDistanceDisplayMode),
                DisplayConfig.averageHitDistanceBold, DisplayConfig.averageHitDistanceItalic, DisplayConfig.averageHitDistanceUnderline);
    }

    @Unique
    private String getHitDisplayString(Double distance) {
        String rounded = CustomRender.getRoundedDouble(distance, DisplayConfig.hitDistanceDecimalPlaces);
        return CustomRender.applyFontStyle(
                CustomRender.applyDisplayFormat(rounded, DisplayConfig.hitDistanceDisplayMode),
                DisplayConfig.hitDistanceBold, DisplayConfig.hitDistanceItalic, DisplayConfig.hitDistanceUnderline);
    }

    @Unique
    private Double computeDistance(Player player, Entity targetEntity) {
        if (player.isSpectator()) return null;

        double distance;
        Vec3 eyePos = player.getEyePosition();

        if (DisplayConfig.distanceCalculationMethod == DisplayConfig.DistanceCalculationMethod.RAY_HIT_POINT) {
            HitResult result = minecraft.hitResult;
            if (!(result instanceof EntityHitResult entityHit) || entityHit.getEntity() != targetEntity) return null;

            Vec3 hitPos = entityHit.getLocation();
            distance = eyePos.distanceTo(hitPos);
        } else {
            AABB box = targetEntity.getBoundingBox();

            double closestX = Math.max(box.minX, Math.min(eyePos.x, box.maxX));
            double closestY = Math.max(box.minY, Math.min(eyePos.y, box.maxY));
            double closestZ = Math.max(box.minZ, Math.min(eyePos.z, box.maxZ));

            Vec3 closestPoint = new Vec3(closestX, closestY, closestZ);
            distance = eyePos.distanceTo(closestPoint);
        }
        if (DisplayConfig.distanceSmoothInterpolation) {
            distance = smoothDistance(distance);
        }
        return distance;
    }

    @Unique
    private String formatDistance(double distance) {
        String rounded = CustomRender.getRoundedDouble(distance, DisplayConfig.distanceDecimalPlaces);
        return CustomRender.applyFontStyle(
                CustomRender.applyDisplayFormat(rounded, DisplayConfig.distanceDisplayMode),
                DisplayConfig.distanceBold, DisplayConfig.distanceItalic, DisplayConfig.distanceUnderline);
    }
}
