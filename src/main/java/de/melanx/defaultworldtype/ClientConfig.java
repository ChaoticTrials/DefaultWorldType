package de.melanx.defaultworldtype;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ClientConfig {

    static {
        final Pair<ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static final Path CONFIG_PATH = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), DefaultWorldType.MODID);
    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    private static ModConfigSpec.ConfigValue<String> worldTypeName;
    public static ModConfigSpec.ConfigValue<String> flatMapSettings;
    private static ModConfigSpec.ConfigValue<String> singleBiome;
    public static ModConfigSpec.BooleanValue disablePresetSelectionButton;
    public static ModConfigSpec.ConfigValue<List<? extends String>> allowedWorldTypes;

    ClientConfig(ModConfigSpec.Builder builder) {
        builder.push("world-preset"); // todo 1.21.5/1.22 remove the extra category
        worldTypeName = builder
                .comment("Type in the name from the world type which should be selected by default.")
                .define("world-preset", "minecraft:normal", String.class::isInstance);
        flatMapSettings = builder
                .comment("Type in a valid generation setting for flat world type.", "Only works if world-type is 'minecraft:flat'.")
                .define("flat-settings", "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;minecraft:plains", String.class::isInstance);
        singleBiome = builder
                .comment("Type in a valid biome for single biome world type.", "Only works if world-type is 'minecraft:single_biome_surface'.")
                .define("single-biome-biome", "minecraft:plains", String.class::isInstance);
        disablePresetSelectionButton = builder
                .comment("Disables the preset selection button in the world selection screen.")
                .define("disable-button", false);
        allowedWorldTypes = builder
                .comment("The list of world types which should be available in the world selection screen. If empty, all world types are available.")
                .defineList("allowed-world-types",
                        List.of(),
                        () -> "",
                        String.class::isInstance);
        builder.pop();
    }

    public static ResourceKey<WorldPreset> getKey() {
        ResourceLocation location = ResourceLocation.tryParse(worldTypeName.get());
        return ResourceKey.create(Registries.WORLD_PRESET, location == null ? ResourceLocation.withDefaultNamespace("normal") : location);
    }

    public static ResourceLocation getFixedBiome() {
        return ResourceLocation.tryParse(singleBiome.get());
    }
}
