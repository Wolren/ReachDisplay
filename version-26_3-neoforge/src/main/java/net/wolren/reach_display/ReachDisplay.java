package net.wolren.reach_display;

import eu.midnightdust.lib.config.MidnightConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.wolren.reach_display.config.DisplayConfig;
import net.wolren.reach_display.data.SharedData;

@Mod(ReachDisplay.MOD_ID)
public class ReachDisplay {
    public static final String MOD_ID = "reach_display";

    public ReachDisplay() {
        if (FMLEnvironment.getDist().isClient()) {
            MidnightConfig.init(MOD_ID, DisplayConfig.class);
            NeoForge.EVENT_BUS.register(this);
        }
    }

    @SubscribeEvent
    public void onGameShuttingDown(GameShuttingDownEvent event) {
        SharedData.getInstance().close();
    }
}
