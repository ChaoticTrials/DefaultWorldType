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

public class ClientConfig {

    static {
        final Pair<ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static final Path CONFIG_PATH = Paths.get(FMLPaths.CONFIGDIR.get().toAbsolutePath().toString(), DefaultWorldType.MODID);
    public static final ClientConfig CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    public static ModConfigSpec.ConfigValue<String> worldTypeName;
    public static ModConfigSpec.ConfigValue<String> flatMapSettings;

    ClientConfig(ModConfigSpec.Builder builder) {
        builder.push("world-preset");
        worldTypeName = builder
                .comment("Type in the name from the world type which should be selected by default.")
                .define("world-preset", "minecraft:normal", String.class::isInstance);
        flatMapSettings = builder
                .comment("Type in a valid generation setting for flat world type.", "Only works if world-type if 'minecraft:flat'.")
                .define("flat-settings", "minecraft:bedrock,2*minecraft:dirt,minecraft:grass_block;minecraft:plains", String.class::isInstance);
        builder.pop();
    }

    public static ResourceKey<WorldPreset> getKey() {
        ResourceLocation location = ResourceLocation.tryParse(worldTypeName.get());
        return ResourceKey.create(Registries.WORLD_PRESET, location == null ? ResourceLocation.withDefaultNamespace("normal") : location);
    }
}
