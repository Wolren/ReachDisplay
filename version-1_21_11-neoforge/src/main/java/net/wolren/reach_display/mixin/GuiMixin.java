package net.wolren.reach_display.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.wolren.reach_display.config.DisplayConfig;
import net.wolren.reach_display.data.SharedData;
import net.wolren.reach_display.filter.EntityFilterHelper;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.math.RoundingMode;
import java.text.DecimalFormat;

@Mixin(Gui.class)
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
    private static int parseColorWithDefault(String colorHex) {
        if (colorHex == null || colorHex.isEmpty()) return 0xFFFFFF;
        try {
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    @Unique
    private static int parseARGBColorWithOpacity(float opacityScale, int colorInt) {
        int alpha = (int) (opacityScale * 255) & 0xFF;
        return (alpha << 24) | (colorInt & 0xFFFFFF);
    }

    @Unique
    private static int lerpColor(int color1, int color2, float t) {
        if (t <= 0) return color1;
        if (t >= 1) return color2;
        int r = (int) (((color1 >> 16) & 0xFF) + (((color2 >> 16) & 0xFF) - ((color1 >> 16) & 0xFF)) * t);
        int g = (int) (((color1 >> 8) & 0xFF) + (((color2 >> 8) & 0xFF) - ((color1 >> 8) & 0xFF)) * t);
        int b = (int) ((color1 & 0xFF) + ((color2 & 0xFF) - (color1 & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    @Unique
    private static String applyDisplayFormat(String number, DisplayConfig.DisplayMode mode) {
        switch (mode) {
            case WITH_BLOCKS:
                return number + " blocks";
            case WITH_M:
                return number + " M";
            default:
                return number;
        }
    }

    @Unique
    private static String applyFontStyle(String text, boolean bold, boolean italic, boolean underline) {
        StringBuilder prefix = new StringBuilder();
        if (bold) prefix.append("§l");
        if (italic) prefix.append("§o");
        if (underline) prefix.append("§n");
        return prefix.toString() + text;
    }

    @Shadow
    public abstract Font getFont();

    @Unique
    private double smoothDistance(double target) {
        double diff = target - reach$smoothedCrosshairDistance;
        reach$smoothedCrosshairDistance += diff * 0.3;
        if (Math.abs(diff) < 0.001) reach$smoothedCrosshairDistance = target;
        return reach$smoothedCrosshairDistance;
    }

    @Inject(at = @At("TAIL"), method = "render")
    public void render(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (!DisplayConfig.enabled) return;

        Player player = minecraft.player;
        HitResult target = minecraft.hitResult;

        if (player == null || target == null) return;

        if (target.getType() == HitResult.Type.ENTITY && DisplayConfig.distanceEnable) {
            Entity targetEntity = ((EntityHitResult) target).getEntity();
            if (!targetEntity.isInvisibleTo(player) && (!DisplayConfig.entityFilterEnable || EntityFilterHelper.shouldTrack(targetEntity))) {
                String displayString;
                if (DisplayConfig.distanceUpdateRate > 0) {
                    long now = System.currentTimeMillis();
                    if (now - reach$lastDistanceUpdateTime < DisplayConfig.distanceUpdateRate) {
                        displayString = reach$lastDistanceDisplayString;
                    } else {
                        displayString = getDisplayString(player, targetEntity);
                        reach$lastDistanceUpdateTime = now;
                        reach$lastDistanceDisplayString = displayString;
                    }
                } else {
                    displayString = getDisplayString(player, targetEntity);
                }

                double dist = player.getEyePosition().distanceTo(((EntityHitResult) target).getLocation());
                int colorInt = resolveDistanceColorInt(player, dist);
                float opacityScale = DisplayConfig.distanceOpacity;
                int ARGBColorInt = parseARGBColorWithOpacity(opacityScale, colorInt);
                boolean shadow = DisplayConfig.distanceShadow;
                boolean drawBackground = DisplayConfig.distanceBackground;
                int bgColor = DisplayConfig.distanceBackground ? parseARGBColorWithOpacity(DisplayConfig.distanceBackgroundOpacity, parseColorWithDefault(DisplayConfig.distanceBackgroundColor)) : 0;
                int shadowColor = DisplayConfig.distanceShadow ? parseARGBColorWithOpacity(1.0f, parseColorWithDefault(DisplayConfig.distanceShadowColor)) : 0;
                float scale = DisplayConfig.distanceScale;
                renderText(context, displayString, getDistance(displayString).x, getDistance(displayString).y, ARGBColorInt, shadow, scale, drawBackground, bgColor, shadowColor);
            }
        }

        if (DisplayConfig.hitDistanceEnable) {
            Entity entity = SharedData.getInstance().getEntity();
            boolean hitTimedOut = !DisplayConfig.hitDistanceKeep && SharedData.getInstance().isLastHitExpired(DisplayConfig.hitDistanceResetSeconds * 1000L);
            if (entity != null && !hitTimedOut && !entity.isInvisibleTo(player) && (!DisplayConfig.entityFilterEnable || EntityFilterHelper.shouldTrack(entity))) {
                String displayString = getHitDisplayString(SharedData.getInstance().getDistance());

                double hitDist = SharedData.getInstance().getDistance();
                int colorInt = resolveHitColorInt(hitDist);
                float opacityScale = DisplayConfig.hitDistanceOpacity;
                int ARGBColorInt = parseARGBColorWithOpacity(opacityScale, colorInt);
                boolean shadow = DisplayConfig.hitDistanceShadow;
                boolean drawBackground = DisplayConfig.hitDistanceBackground;
                int bgColor = DisplayConfig.hitDistanceBackground ? parseARGBColorWithOpacity(DisplayConfig.hitDistanceBackgroundOpacity, parseColorWithDefault(DisplayConfig.hitDistanceBackgroundColor)) : 0;
                int shadowColor = DisplayConfig.hitDistanceShadow ? parseARGBColorWithOpacity(1.0f, parseColorWithDefault(DisplayConfig.hitDistanceShadowColor)) : 0;
                float scale = DisplayConfig.hitDistanceScale;
                renderText(context, displayString, getHitDistance(displayString).x, getHitDistance(displayString).y, ARGBColorInt, shadow, scale, drawBackground, bgColor, shadowColor);
            }
        }

        if (DisplayConfig.averageHitDistanceEnable) {
            Entity entity = SharedData.getInstance().getEntity();
            boolean avgTimedOut = !DisplayConfig.hitDistanceKeep && SharedData.getInstance().isLastHitExpired(DisplayConfig.hitDistanceResetSeconds * 1000L);
            if (entity != null && !avgTimedOut && (!DisplayConfig.entityFilterEnable || EntityFilterHelper.shouldTrack(entity))) {
                String displayString = getAverageHitDisplayString(SharedData.getInstance().getAverageDistance());

                double avgDist = SharedData.getInstance().getAverageDistance();
                int colorInt = resolveAverageColorInt(avgDist);
                float opacityScale = DisplayConfig.averageHitDistanceOpacity;
                int ARGBColorInt = parseARGBColorWithOpacity(opacityScale, colorInt);
                boolean shadow = DisplayConfig.averageHitDistanceShadow;
                boolean drawBackground = DisplayConfig.averageHitDistanceBackground;
                int bgColor = DisplayConfig.averageHitDistanceBackground ? parseARGBColorWithOpacity(DisplayConfig.averageHitDistanceBackgroundOpacity, parseColorWithDefault(DisplayConfig.averageHitDistanceBackgroundColor)) : 0;
                int shadowColor = DisplayConfig.averageHitDistanceShadow ? parseARGBColorWithOpacity(1.0f, parseColorWithDefault(DisplayConfig.averageHitDistanceShadowColor)) : 0;
                float scale = DisplayConfig.averageHitDistanceScale;
                renderText(context, displayString, getAverageHitDistance(displayString).x, getAverageHitDistance(displayString).y, ARGBColorInt, shadow, scale, drawBackground, bgColor, shadowColor);
            }
        }


    }

    @Unique
    private int resolveDistanceColorInt(Player player, double dist) {
        double maxDist = player.isCreative() ? 5.0 : 3.0;
        if (DisplayConfig.distanceGradientEnabled) {
            float t = (float) (dist / maxDist);
            if (t < 0) t = 0;
            if (t > 1) t = 1;
            return lerpColor(parseColorWithDefault(DisplayConfig.distanceGradientStartColor), parseColorWithDefault(DisplayConfig.distanceGradientEndColor), t);
        }
        if (DisplayConfig.distanceColorBandsEnabled) {
            if (dist < DisplayConfig.distanceBand1Threshold)
                return parseColorWithDefault(DisplayConfig.distanceBand1Color);
            if (dist < DisplayConfig.distanceBand2Threshold)
                return parseColorWithDefault(DisplayConfig.distanceBand2Color);
            return parseColorWithDefault(DisplayConfig.distanceBand3Color);
        }
        return parseColorWithDefault(DisplayConfig.distanceColor);
    }

    @Unique
    private int resolveHitColorInt(double dist) {
        if (DisplayConfig.hitDistanceGradientEnabled) {
            Player p = minecraft.player;
            double maxDist = (p != null && p.isCreative()) ? 5.0 : 3.0;
            float t = (float) (dist / maxDist);
            if (t < 0) t = 0;
            if (t > 1) t = 1;
            return lerpColor(parseColorWithDefault(DisplayConfig.hitDistanceGradientStartColor), parseColorWithDefault(DisplayConfig.hitDistanceGradientEndColor), t);
        }
        if (DisplayConfig.hitDistanceColorBandsEnabled) {
            if (dist < DisplayConfig.hitDistanceBand1Threshold)
                return parseColorWithDefault(DisplayConfig.hitDistanceBand1Color);
            if (dist < DisplayConfig.hitDistanceBand2Threshold)
                return parseColorWithDefault(DisplayConfig.hitDistanceBand2Color);
            return parseColorWithDefault(DisplayConfig.hitDistanceBand3Color);
        }
        return parseColorWithDefault(DisplayConfig.hitDistanceColor);
    }

    @Unique
    private int resolveAverageColorInt(double dist) {
        if (DisplayConfig.averageHitDistanceGradientEnabled) {
            Player p = minecraft.player;
            double maxDist = (p != null && p.isCreative()) ? 5.0 : 3.0;
            float t = (float) (dist / maxDist);
            if (t < 0) t = 0;
            if (t > 1) t = 1;
            return lerpColor(parseColorWithDefault(DisplayConfig.averageHitDistanceGradientStartColor), parseColorWithDefault(DisplayConfig.averageHitDistanceGradientEndColor), t);
        }
        if (DisplayConfig.averageHitDistanceColorBandsEnabled) {
            if (dist < DisplayConfig.averageHitDistanceBand1Threshold)
                return parseColorWithDefault(DisplayConfig.averageHitDistanceBand1Color);
            if (dist < DisplayConfig.averageHitDistanceBand2Threshold)
                return parseColorWithDefault(DisplayConfig.averageHitDistanceBand2Color);
            return parseColorWithDefault(DisplayConfig.averageHitDistanceBand3Color);
        }
        return parseColorWithDefault(DisplayConfig.averageHitDistanceColor);
    }

    @Unique
    private String getAverageHitDisplayString(Double distance) {
        int decimalPlaces = DisplayConfig.averageHitDistanceDecimalPlaces;

        DecimalFormat df = new DecimalFormat("0." + "0".repeat(decimalPlaces));

        return applyFontStyle(applyDisplayFormat(df.format(distance), DisplayConfig.averageHitDistanceDisplayMode), DisplayConfig.averageHitDistanceBold, DisplayConfig.averageHitDistanceItalic, DisplayConfig.averageHitDistanceUnderline);
    }

    @Unique
    private String getHitDisplayString(Double distance) {
        int decimalPlaces = DisplayConfig.hitDistanceDecimalPlaces;

        DecimalFormat df = new DecimalFormat("0." + "0".repeat(decimalPlaces));

        return applyFontStyle(applyDisplayFormat(df.format(distance), DisplayConfig.hitDistanceDisplayMode), DisplayConfig.hitDistanceBold, DisplayConfig.hitDistanceItalic, DisplayConfig.hitDistanceUnderline);
    }

    @Unique
    private String getDisplayString(Player player, Entity targetEntity) {
        if (player.isSpectator()) return "";

        int decimalPlaces = DisplayConfig.distanceDecimalPlaces;
        DecimalFormat df = new DecimalFormat("0." + "0".repeat(decimalPlaces));
        df.setRoundingMode(RoundingMode.DOWN);

        double distance;
        Vec3 eyePos = player.getEyePosition();

        if (DisplayConfig.distanceCalculationMethod == DisplayConfig.DistanceCalculationMethod.RAY_HIT_POINT) {
            HitResult result = minecraft.hitResult;
            if (!(result instanceof EntityHitResult entityHit) || entityHit.getEntity() != targetEntity) return "";

            Vec3 hitPos = entityHit.getLocation();
            distance = eyePos.distanceTo(hitPos);


        } else {
            Vec3 closestPoint = new Vec3(
                    Math.max(targetEntity.getBoundingBox().minX, Math.min(eyePos.x, targetEntity.getBoundingBox().maxX)),
                    Math.max(targetEntity.getBoundingBox().minY, Math.min(eyePos.y, targetEntity.getBoundingBox().maxY)),
                    Math.max(targetEntity.getBoundingBox().minZ, Math.min(eyePos.z, targetEntity.getBoundingBox().maxZ))
            );
            distance = eyePos.distanceTo(closestPoint);
        }
        if (DisplayConfig.distanceSmoothInterpolation) {
            distance = smoothDistance(distance);
        }
        return applyFontStyle(df.format(distance), DisplayConfig.distanceBold, DisplayConfig.distanceItalic, DisplayConfig.distanceUnderline);
    }

    @Unique
    private void renderText(GuiGraphics context, String text, float x, float y, int color, boolean shadow, float scale, boolean drawBackground, int bgColor, int shadowColor) {
        context.pose().scale(scale, scale);
        Font renderer = this.getFont();
        int drawX = (int) (x * (1 / scale));
        int drawY = (int) (y * (1 / scale));

        if (drawBackground) {
            int textWidth = renderer.width(text);
            context.fill(drawX - 2, drawY - 2, drawX + textWidth + 2, drawY + renderer.lineHeight + 2, bgColor);
        }

        if (shadow) {
            context.drawString(renderer, text, drawX + 1, drawY + 1, shadowColor, false);
        }
        context.drawString(renderer, text, drawX, drawY, color, false);
        context.pose().scale((1 / scale), (1 / scale));
    }

    @Unique
    public Vector2f getDistance(String displayString) {
        float y = (minecraft.getWindow().getGuiScaledHeight() / 2.0F) - DisplayConfig.yOffset;
        float x = (minecraft.getWindow().getGuiScaledWidth() / 2.0F - ((minecraft.font.width(displayString) / 2.0F) * DisplayConfig.distanceScale)) - DisplayConfig.xOffset;
        return new Vector2f(x, y);
    }

    @Unique
    public Vector2f getHitDistance(String displayString) {
        float x = 4 + DisplayConfig.hitXOffset;
        float y = 4 + DisplayConfig.hitYOffset;
        return new Vector2f(x, y);
    }

    @Unique
    public Vector2f getAverageHitDistance(String displayString) {
        float x = 4 + DisplayConfig.averageHitXOffset;
        float y = 4 + DisplayConfig.averageHitYOffset;
        return new Vector2f(x, y);
    }
}
