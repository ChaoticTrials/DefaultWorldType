package de.melanx.defaultworldtype;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ClientConfig {

    static {
        final Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static final Path CONFIG_PATH = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), DefaultWorldType.MODID);
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static ForgeConfigSpec.ConfigValue<String> worldTypeName;
    public static ForgeConfigSpec.ConfigValue<String> flatMapSettings;
    public static ForgeConfigSpec.BooleanValue disablePresetSelectionButton;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> allowedWorldTypes;

    ClientConfig(ForgeConfigSpec.Builder builder) {
        builder.push("world-preset");
        worldTypeName = builder
                .comment("Type in the name from the world type which should be selected by default.")
                .define("world-preset", "minecraft:normal", String.class::isInstance);
        flatMapSettings = builder
                .comment("Type in a valid generation setting for flat world type.", "Only works if world-type if 'minecraft:flat'.")
                .define("flat-settings", "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;minecraft:plains", String.class::isInstance);
        disablePresetSelectionButton = builder
                .comment("Disables the preset selection button in the world selection screen.")
                .define("disable-button", false);
        allowedWorldTypes = builder
                .comment("The list of world types which should be available in the world selection screen. If empty, all world types are available.")
                .defineList("allowed-world-types",
                        List.of(),
                        String.class::isInstance);
        builder.pop();
    }

    public static void setup() {
        try {
            Files.createDirectory(CONFIG_PATH);
        } catch (FileAlreadyExistsException e) {
            DefaultWorldType.LOGGER.debug("Config directory " + DefaultWorldType.MODID + " already exists. Skip creating.");
        } catch (IOException e) {
            DefaultWorldType.LOGGER.error("Failed to create " + DefaultWorldType.MODID + " config directory", e);
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, DefaultWorldType.MODID + "/client-config.toml");
    }

    public static ResourceKey<WorldPreset> getKey() {
        return ResourceKey.create(Registries.WORLD_PRESET, new ResourceLocation(worldTypeName.get()));
    }
}
