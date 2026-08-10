package net.wolren.reach_display;

import com.mojang.blaze3d.platform.InputConstants;
import eu.midnightdust.core.screen.MidnightConfigOverviewScreen;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.wolren.reach_display.config.DisplayConfig;
import net.wolren.reach_display.data.SharedData;
import org.lwjgl.glfw.GLFW;

@Mod(ReachDisplay.MOD_ID)
public class ReachDisplay {
    public static final String MOD_ID = "reach_display";

    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.reach_display.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            "category.reach_display"
    );

    public ReachDisplay() {
        if (FMLEnvironment.dist.isClient()) {
            MidnightConfig.init(MOD_ID, DisplayConfig.class);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerKeyMappings);
            MinecraftForge.EVENT_BUS.register(this);
            MinecraftForge.EVENT_BUS.register(new HudRenderHandler());
        }
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG_KEY);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (OPEN_CONFIG_KEY.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new MidnightConfigOverviewScreen(minecraft.screen));
        }
    }

    @SubscribeEvent
    public void onGameShuttingDown(GameShuttingDownEvent event) {
        SharedData.getInstance().close();
    }
}
