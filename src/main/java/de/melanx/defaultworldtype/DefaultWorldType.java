package de.melanx.defaultworldtype;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientConfig.setup();
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    private static class GuiEventHandler {
        private static boolean doneLogging;
        private static boolean createdWorldTypeFile;

        @SubscribeEvent
        @OnlyIn(Dist.CLIENT)
        public static void onPreInitCreateWorld(ScreenEvent.Opening event) {
            Screen screenGui = event.getScreen();
            String worldTypeName = ClientConfig.worldTypeName.get();

            if (screenGui instanceof CreateWorldScreen createWorldScreen) {
                if (!createdWorldTypeFile) {
                    createAvailablePresetsFile(createWorldScreen.worldGenSettingsComponent);
                    createdWorldTypeFile = true;
                }

                Optional<Holder<WorldPreset>> preset = createWorldScreen.worldGenSettingsComponent.preset;
                if (preset.isPresent() && preset.get().is(new ResourceLocation(worldTypeName))) {
                    if (!doneLogging) {
                        LOGGER.info("Already correct preset selected: " + worldTypeName);
                    }

                    return;
                }

                Optional<Holder<WorldPreset>> customPreset = WorldGenSettingsComponent.findPreset(createWorldScreen.worldGenSettingsComponent.settings(), Optional.of(ResourceKey.create(Registry.WORLD_PRESET_REGISTRY, new ResourceLocation(worldTypeName))));
                if (customPreset.isPresent()) {
                    createWorldScreen.worldGenSettingsComponent.preset = customPreset;
                    createWorldScreen.worldGenSettingsComponent.updateSettings(settings -> customPreset.get().value().recreateWorldGenSettings(settings));
                    return;
                }

                if (!doneLogging) {
                    LOGGER.error(String.format("World-type %s is an invalid world-type.", worldTypeName));
                    doneLogging = true;
                }
            }
        }
    }

    private static void createAvailablePresetsFile(WorldGenSettingsComponent component) {
        File file = Paths.get(configPath.toString()).resolve("world-types.txt").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            IdMap<Holder<WorldPreset>> holders = component.registryHolder().registryOrThrow(Registry.WORLD_PRESET_REGISTRY).asHolderIdMap();
            writer.write(holders.size() + " possible world presets found: \n");
            for (Holder<WorldPreset> holder : holders) {
                if (holder.unwrapKey().isPresent()) {
                    writer.write(holder.unwrapKey().get().location().toString() + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ClientConfig {
        public static ForgeConfigSpec.ConfigValue<String> worldTypeName;
        public static ForgeConfigSpec.ConfigValue<String> flatMapSettings;

        ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("world-preset");
            worldTypeName = builder
                    .comment("Type in the name from the world type which should be selected by default.")
                    .define("world-preset", "minecraft:normal", String.class::isInstance);
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
