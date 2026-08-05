package net.wolren.reach_display.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.wolren.reach_display.config.DisplayConfig;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CustomRender {
    private static final Map<Integer, DecimalFormat> FORMATTER_CACHE = new HashMap<>();
    private static String cachedDistanceColorHex;
    private static float cachedDistanceOpacityScale;
    private static int cachedDistanceARGBColor;
    private static String cachedHitDistanceColorHex;
    private static float cachedHitDistanceOpacityScale;
    private static int cachedHitDistanceARGBColor;
    private static String cachedAverageHitDistanceColorHex;
    private static float cachedAverageHitDistanceOpacityScale;
    private static int cachedAverageHitDistanceARGBColor;

    public static void renderText(Minecraft minecraft, GuiGraphicsExtractor context, String text, int textClass,
                                  boolean shadow, float scale, int xOffset, int yOffset, boolean centered,
                                  boolean drawBackground, int bgColor, int shadowColor) {
        int color = switch (textClass) {
            case 0 -> getDistanceARGBColor();
            case 1 -> getHitDistanceARGBColor();
            case 2 -> getAverageHitDistanceARGBColor();
            default -> 0xFFFFFF;
        };

        Font font = minecraft.font;
        float y;
        float x;
        if (centered) {
            y = (minecraft.getWindow().getGuiScaledHeight() / 2.0F) - yOffset;
            x = (minecraft.getWindow().getGuiScaledWidth() / 2.0F - ((font.width(text) / 2.0F) * scale)) - xOffset;
        } else {
            x = 4 + xOffset;
            y = 4 + yOffset;
        }

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
        float invScale = 1.0f / scale;
        int drawX = (int) (x * invScale);
        int drawY = (int) (y * invScale);

        if (drawBackground) {
            int textWidth = font.width(text);
            context.fill(drawX - 2, drawY - 2, drawX + textWidth + 2, drawY + font.lineHeight + 2, bgColor);
        }

        if (shadow) {
            context.text(font, text, drawX + 1, drawY + 1, shadowColor, false);
        }
        context.text(font, text, drawX, drawY, color, false);
        context.pose().popMatrix();
    }

    public static String getRoundedDouble(double distance, int precision) {
        int key = (precision * 10) + RoundingMode.DOWN.ordinal();
        return FORMATTER_CACHE.computeIfAbsent(key, k -> {
            DecimalFormat df = new DecimalFormat("0." + "0".repeat(precision));
            df.setRoundingMode(RoundingMode.DOWN);
            return df;
        }).format(distance);
    }

    public static String applyDisplayFormat(String number, DisplayConfig.DisplayMode mode) {
        return switch (mode) {
            case WITH_BLOCKS -> number + " blocks";
            case WITH_M -> number + " M";
            default -> number;
        };
    }

    public static String applyFontStyle(String text, boolean bold, boolean italic, boolean underline) {
        StringBuilder prefix = new StringBuilder();
        if (bold) prefix.append("§l");
        if (italic) prefix.append("§o");
        if (underline) prefix.append("§n");
        return prefix.toString() + text;
    }

    public static int parseColorWithDefault(String colorHex) {
        if (colorHex == null || colorHex.isEmpty()) return 0xFFFFFF;
        try {
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    public static int parseARGBColorWithOpacity(float opacityScale, int colorInt) {
        int alpha = (int) (opacityScale * 255) & 0xFF;
        return (alpha << 24) | (colorInt & 0xFFFFFF);
    }

    public static int lerpColor(int color1, int color2, float t) {
        if (t <= 0) return color1;
        if (t >= 1) return color2;
        int r = (int) (((color1 >> 16) & 0xFF) + (((color2 >> 16) & 0xFF) - ((color1 >> 16) & 0xFF)) * t);
        int g = (int) (((color1 >> 8) & 0xFF) + (((color2 >> 8) & 0xFF) - ((color1 >> 8) & 0xFF)) * t);
        int b = (int) ((color1 & 0xFF) + ((color2 & 0xFF) - (color1 & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    public static int resolveDistanceColorInt(boolean creative, double dist) {
        if (DisplayConfig.distanceGradientEnabled) {
            float maxDist = (creative ? 2.0f : 0.0f) + DisplayConfig.distanceGradientMax;
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

    public static int resolveHitColorInt(boolean creative, double dist) {
        if (DisplayConfig.hitDistanceGradientEnabled) {
            float maxDist = (creative ? 2.0f : 0.0f) + DisplayConfig.hitDistanceGradientMax;
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

    public static int resolveAverageColorInt(boolean creative, double dist) {
        if (DisplayConfig.averageHitDistanceGradientEnabled) {
            float maxDist = (creative ? 2.0f : 0.0f) + DisplayConfig.averageHitDistanceGradientMax;
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

    private static int getDistanceARGBColor() {
        String hex = DisplayConfig.distanceColor;
        float op = DisplayConfig.distanceOpacity;
        if (!Objects.equals(hex, cachedDistanceColorHex) || op != cachedDistanceOpacityScale) {
            cachedDistanceARGBColor = parseARGBColorWithOpacity(op, parseColorWithDefault(hex));
            cachedDistanceColorHex = hex;
            cachedDistanceOpacityScale = op;
        }
        return cachedDistanceARGBColor;
    }

    private static int getHitDistanceARGBColor() {
        String hex = DisplayConfig.hitDistanceColor;
        float op = DisplayConfig.hitDistanceOpacity;
        if (!Objects.equals(hex, cachedHitDistanceColorHex) || op != cachedHitDistanceOpacityScale) {
            cachedHitDistanceARGBColor = parseARGBColorWithOpacity(op, parseColorWithDefault(hex));
            cachedHitDistanceColorHex = hex;
            cachedHitDistanceOpacityScale = op;
        }
        return cachedHitDistanceARGBColor;
    }

    private static int getAverageHitDistanceARGBColor() {
        String hex = DisplayConfig.averageHitDistanceColor;
        float op = DisplayConfig.averageHitDistanceOpacity;
        if (!Objects.equals(hex, cachedAverageHitDistanceColorHex) || op != cachedAverageHitDistanceOpacityScale) {
            cachedAverageHitDistanceARGBColor = parseARGBColorWithOpacity(op, parseColorWithDefault(hex));
            cachedAverageHitDistanceColorHex = hex;
            cachedAverageHitDistanceOpacityScale = op;
        }
        return cachedAverageHitDistanceARGBColor;
    }
}
