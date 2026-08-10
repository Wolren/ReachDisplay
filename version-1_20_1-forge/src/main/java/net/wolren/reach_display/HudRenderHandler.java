package net.wolren.reach_display;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.wolren.reach_display.config.DisplayConfig;
import net.wolren.reach_display.data.SharedData;
import net.wolren.reach_display.filter.EntityFilterHelper;
import org.joml.Vector2f;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class HudRenderHandler {
    private double smoothedCrosshairDistance = 0.0;
    private long lastDistanceUpdateTime = 0L;
    private String lastDistanceDisplayString = "";

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (!DisplayConfig.enabled) return;
        if (client.screen != null) return;

        Player player = client.player;
        HitResult target = client.hitResult;

        if (player == null || target == null) return;

        if (target.getType() == HitResult.Type.ENTITY && DisplayConfig.distanceEnable) {
            Entity targetEntity = ((EntityHitResult) target).getEntity();
            if (!targetEntity.isInvisibleTo(player) && (!DisplayConfig.entityFilterEnable || EntityFilterHelper.shouldTrack(targetEntity))) {
                String displayString;
                if (DisplayConfig.distanceUpdateRate > 0) {
                    long now = System.currentTimeMillis();
                    if (now - lastDistanceUpdateTime < DisplayConfig.distanceUpdateRate) {
                        displayString = lastDistanceDisplayString;
                    } else {
                        displayString = getDisplayString(client, player, targetEntity);
                        lastDistanceUpdateTime = now;
                        lastDistanceDisplayString = displayString;
                    }
                } else {
                    displayString = getDisplayString(client, player, targetEntity);
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
                renderText(client, event.getGuiGraphics(), displayString, getDistance(client, displayString).x, getDistance(client, displayString).y, ARGBColorInt, shadow, scale, drawBackground, bgColor, shadowColor);
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
                renderText(client, event.getGuiGraphics(), displayString, getHitDistance(displayString).x, getHitDistance(displayString).y, ARGBColorInt, shadow, scale, drawBackground, bgColor, shadowColor);
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
                renderText(client, event.getGuiGraphics(), displayString, getAverageHitDistance(displayString).x, getAverageHitDistance(displayString).y, ARGBColorInt, shadow, scale, drawBackground, bgColor, shadowColor);
            }
        }
    }

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

    private int resolveHitColorInt(double dist) {
        if (DisplayConfig.hitDistanceGradientEnabled) {
            Player p = Minecraft.getInstance().player;
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

    private int resolveAverageColorInt(double dist) {
        if (DisplayConfig.averageHitDistanceGradientEnabled) {
            Player p = Minecraft.getInstance().player;
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

    private String getAverageHitDisplayString(Double distance) {
        int decimalPlaces = DisplayConfig.averageHitDistanceDecimalPlaces;

        DecimalFormat df = new DecimalFormat("0." + "0".repeat(decimalPlaces));

        return applyFontStyle(applyDisplayFormat(df.format(distance), DisplayConfig.averageHitDistanceDisplayMode), DisplayConfig.averageHitDistanceBold, DisplayConfig.averageHitDistanceItalic, DisplayConfig.averageHitDistanceUnderline);
    }

    private String getHitDisplayString(Double distance) {
        int decimalPlaces = DisplayConfig.hitDistanceDecimalPlaces;

        DecimalFormat df = new DecimalFormat("0." + "0".repeat(decimalPlaces));

        return applyFontStyle(applyDisplayFormat(df.format(distance), DisplayConfig.hitDistanceDisplayMode), DisplayConfig.hitDistanceBold, DisplayConfig.hitDistanceItalic, DisplayConfig.hitDistanceUnderline);
    }

    private String getDisplayString(Minecraft client, Player player, Entity targetEntity) {
        if (player.isSpectator()) return "";

        int decimalPlaces = DisplayConfig.distanceDecimalPlaces;
        DecimalFormat df = new DecimalFormat("0." + "0".repeat(decimalPlaces));
        df.setRoundingMode(RoundingMode.DOWN);

        double distance;
        Vec3 eyePos = player.getEyePosition();

        if (DisplayConfig.distanceCalculationMethod == DisplayConfig.DistanceCalculationMethod.RAY_HIT_POINT) {
            HitResult result = client.hitResult;
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

    private void renderText(Minecraft client, GuiGraphics context, String text, float x, float y, int color, boolean shadow, float scale, boolean drawBackground, int bgColor, int shadowColor) {
        context.pose().scale(scale, scale, scale);
        Font renderer = client.font;
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
        context.pose().scale((1 / scale), (1 / scale), (1 / scale));
    }

    private Vector2f getDistance(Minecraft client, String displayString) {
        float y = (client.getWindow().getGuiScaledHeight() / 2.0F) - DisplayConfig.yOffset;
        float x = (client.getWindow().getGuiScaledWidth() / 2.0F - ((client.font.width(displayString) / 2.0F) * DisplayConfig.distanceScale)) - DisplayConfig.xOffset;
        return new Vector2f(x, y);
    }

    private Vector2f getHitDistance(String displayString) {
        float x = 4 + DisplayConfig.hitXOffset;
        float y = 4 + DisplayConfig.hitYOffset;
        return new Vector2f(x, y);
    }

    private Vector2f getAverageHitDistance(String displayString) {
        float x = 4 + DisplayConfig.averageHitXOffset;
        float y = 4 + DisplayConfig.averageHitYOffset;
        return new Vector2f(x, y);
    }

    private double smoothDistance(double target) {
        double diff = target - smoothedCrosshairDistance;
        smoothedCrosshairDistance += diff * 0.3;
        if (Math.abs(diff) < 0.001) smoothedCrosshairDistance = target;
        return smoothedCrosshairDistance;
    }

    private static int parseColorWithDefault(String colorHex) {
        if (colorHex == null || colorHex.isEmpty()) return 0xFFFFFF;
        try {
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    private static int parseARGBColorWithOpacity(float opacityScale, int colorInt) {
        int alpha = (int) (opacityScale * 255) & 0xFF;
        return (alpha << 24) | (colorInt & 0xFFFFFF);
    }

    private static int lerpColor(int color1, int color2, float t) {
        if (t <= 0) return color1;
        if (t >= 1) return color2;
        int r = (int) (((color1 >> 16) & 0xFF) + (((color2 >> 16) & 0xFF) - ((color1 >> 16) & 0xFF)) * t);
        int g = (int) (((color1 >> 8) & 0xFF) + (((color2 >> 8) & 0xFF) - ((color1 >> 8) & 0xFF)) * t);
        int b = (int) ((color1 & 0xFF) + ((color2 & 0xFF) - (color1 & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

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

    private static String applyFontStyle(String text, boolean bold, boolean italic, boolean underline) {
        StringBuilder prefix = new StringBuilder();
        if (bold) prefix.append("§l");
        if (italic) prefix.append("§o");
        if (underline) prefix.append("§n");
        return prefix.toString() + text;
    }
}
