package de.melanx.defaultworldtype;

import net.minecraft.client.gui.screen.*;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

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
        public static void onPreInitCreateWorld(GuiOpenEvent event) {
            Screen screenGui = event.getGui();
            String worldTypeName = ClientConfig.worldTypeName.get();

            if (screenGui instanceof CreateWorldScreen) {
                List<BiomeGeneratorTypeScreens> types = BiomeGeneratorTypeScreens.field_239068_c_;
                for (int i = 0; i < types.size(); i++) {
                    BiomeGeneratorTypeScreens s = BiomeGeneratorTypeScreens.field_239068_c_.get(i);
                    String name = ((TranslationTextComponent) s.func_239077_a_()).getKey().replace("generator.", "");
                    if (name.equals(worldTypeName)) {
                        WorldOptionsScreen optionsScreen = ((CreateWorldScreen) screenGui).field_238934_c_;
                        if (optionsScreen.field_239040_n_.isPresent()) {
                            optionsScreen.field_239040_n_ = Optional.of(s);
                            optionsScreen.field_239039_m_ = s.func_241220_a_(optionsScreen.field_239038_l_, optionsScreen.field_239039_m_.getSeed(), optionsScreen.field_239039_m_.doesGenerateFeatures(), optionsScreen.field_239039_m_.hasBonusChest());
                        }
                        if (!doneLogging && !worldTypeName.equals("default")) {
                            doneLogging = true;
                            LOGGER.info(String.format("%s was set as default world-type for new world.", worldTypeName));
                        }
                        return;
                    }
                }
                if (!doneLogging) {
                    doneLogging = true;
                    LOGGER.error(String.format("World-type %s is an invalid world-type.", worldTypeName));
                }
            }

            if (screenGui instanceof MainMenuScreen) {
                if (!createdWorldTypeFile) {
                    try {
                        File worldTypeFile = new File(configPath.toString() + "\\world-types.txt");
                        //noinspection ResultOfMethodCallIgnored
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
            } catch (FileAlreadyExistsException e) {
                LOGGER.info("Config directory " + MODID + " already exists. Skip creating.");
            } catch (IOException e) {
                LOGGER.error("Failed to create " + MODID + " config directory", e);
            }

            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, MODID + "/client-config.toml");
        }
    }
}
