package net.wolren.reach_display.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class DisplayConfig extends MidnightConfig {
    @Entry(name = "Enable")
    public static boolean enabled = true;

    @Entry(name = "Show Players Only")
    public static boolean showPlayers = true;

    @Comment() public static Comment spacer1;

    @Entry(name = "Enable Distance Display")
    public static boolean distanceEnable = true;

    @Entry(name = "Distance Scale", min = 0.1f, max = 5f)
    public static float distanceScale = 1.0f;

    @Entry(name = "Distance X Offset", min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
    public static int xOffset = 0;

    @Entry(name = "Distance Y Offset", min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
    public static int yOffset = -10;

    @Entry(name = "Distance Color", isColor = true)
    public static String distanceColor = "FFFFFF";

    @Entry(name = "Distance Decimal Places")
    public static int distanceDecimalPlaces = 2;

    @Comment() public static Comment spacer2;

    @Entry(name = "Enable Hit Distance Display")
    public static boolean hitDistanceEnable = true;

    @Entry(name = "Hit Distance Scale", min = 0.1f, max = 5f)
    public static float hitDistanceScale = 1.0f;

    @Entry(name = "Hit Distance X Offset", min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
    public static int hitXOffset = 0;

    @Entry(name = "Hit Distance Y Offset", min = Integer.MIN_VALUE, max = Integer.MAX_VALUE)
    public static int hitYOffset = -30;

    @Entry(name = "Hit Distance Color", isColor = true)
    public static String hitDistanceColor = "FFFFFF";

    @Entry(name = "Hit Distance Decimal Places")
    public static int hitDistanceDecimalPlaces = 2;
}
