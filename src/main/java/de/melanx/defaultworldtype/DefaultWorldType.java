package de.melanx.defaultworldtype;

import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.world.WorldType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Mod(DefaultWorldType.MODID)
public class DefaultWorldType {

    public static final String MODID = "defaultworldtype";
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;
    private static final Logger LOGGER = LogManager.getLogger();
    public static Path configPath;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair2 = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair2.getRight();
        CLIENT = specPair2.getLeft();
    }

    public DefaultWorldType() {
        ClientConfig.setup();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    private static class GuiEventHandler {
        private static boolean doneLogging;
        private static boolean createdWorldTypeFile;

        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public static void onPreInitCreateWorld(GuiScreenEvent.InitGuiEvent.Pre event) {
            Screen screenGui = event.getGui();
            String worldTypeName = ClientConfig.worldTypeName.get();
            WorldType defaultWorldType = WorldType.byName(worldTypeName);

            if (screenGui instanceof CreateWorldScreen) {
                if (defaultWorldType != null) {
                    CreateWorldScreen createWorldGui = (CreateWorldScreen) screenGui;

                    if (createWorldGui.selectedIndex == WorldType.DEFAULT.getId()) {
                        createWorldGui.selectedIndex = defaultWorldType.getId();
                        if (!doneLogging && createWorldGui.selectedIndex != WorldType.DEFAULT.getId()) {
                            doneLogging = true;
                            LOGGER.info(String.format("%s was set as default world-type for new world.", worldTypeName));
                        }
                    }
                } else {
                    LOGGER.error(String.format("World-type %s is an invalid world-type.", worldTypeName));
                }
            }

            if (screenGui instanceof MainMenuScreen) {
                if (!createdWorldTypeFile) {
                    try {
                        File worldTypeFile = new File(configPath.toString() + "\\world-types.txt");
                        worldTypeFile.createNewFile();

                        FileWriter writer = new FileWriter(worldTypeFile);
                        writer.write(Util.countWorldTypes() + " possible world types found:");
                        writer.write(Util.getWorldTypeNames());
                        writer.close();
                        createdWorldTypeFile = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static class ClientConfig {
        public static ForgeConfigSpec.ConfigValue<String> worldTypeName;

        ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("world-type");
            worldTypeName = builder
                    .comment("Type in the name from the world type which should be selected by default.")
                    .define("world-type", "default", String.class::isInstance);
            builder.pop();
        }

        public static void setup() {
            configPath = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), MODID);
            try {
                Files.createDirectory(configPath);
            } catch (IOException e) {
                LOGGER.error("Failed to create " + MODID + " config directory", e);
            }

            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, MODID + "/client-config.toml");
        }
    }
}
