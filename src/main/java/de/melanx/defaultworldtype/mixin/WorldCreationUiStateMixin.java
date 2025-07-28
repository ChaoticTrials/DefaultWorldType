package de.melanx.defaultworldtype.mixin;

import de.melanx.defaultworldtype.ClientConfig;
import de.melanx.defaultworldtype.DefaultWorldType;
import net.minecraft.client.gui.screens.PresetFlatWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin {

    @Inject(
            method = "updatePresetLists",
            at = @At("TAIL")
    )
    private void afterUpdatePresetLists(CallbackInfo ci) {
        WorldCreationUiState state = (WorldCreationUiState) (Object) this;
        Registry<WorldPreset> worldPresets = state.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET);
        Registry<Biome> biomes = state.getSettings().worldgenLoadContext().lookupOrThrow(Registries.BIOME);

        WorldCreationUiStateMixin.defaultWorldType$writeRegistryToFile(worldPresets, "world-presets");
        WorldCreationUiStateMixin.defaultWorldType$writeRegistryToFile(biomes, "biomes");

        List<WorldCreationUiState.WorldTypeEntry> preferredPresets = defaultWorldType$buildPreferredPresetEntries(worldPresets);
        if (!preferredPresets.isEmpty()) {
            state.normalPresetList.clear();
            state.normalPresetList.addAll(preferredPresets);
        }

        WorldCreationUiStateMixin.defaultWorldType$selectInitialWorldType(state);

        if (ClientConfig.getKey() == WorldPresets.FLAT) {
            WorldCreationUiStateMixin.defaultWorldType$setFlatSettings(state);
        }

        if (ClientConfig.getKey() == WorldPresets.SINGLE_BIOME_SURFACE) {
            WorldCreationUiStateMixin.defaultWorldType$setSingleBiome(state);
        }
    }

    @Unique
    private static void defaultWorldType$writeRegistryToFile(Registry<?> registry, String registryName) {
        List<ResourceLocation> ids = registry.entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .map(ResourceKey::location)
                .sorted()
                .toList();

        String content = ids.size() + " possible " + registryName + " found:\n" +
                ids.stream()
                        .map(loc -> "- \"" + loc + "\"")
                        .collect(Collectors.joining("\n"));

        try {
            Files.writeString(ClientConfig.CONFIG_PATH.resolve(registryName + ".txt"), content);
        } catch (IOException ex) {
            DefaultWorldType.LOGGER.error("Couldn't generate file with existing {}", registryName, ex);
        }
    }

    @Unique
    private static List<WorldCreationUiState.WorldTypeEntry> defaultWorldType$buildPreferredPresetEntries(Registry<WorldPreset> registry) {
        List<WorldCreationUiState.WorldTypeEntry> result = new ArrayList<>();
        for (String s : ClientConfig.allowedWorldTypes.get()) {
            ResourceLocation id = ResourceLocation.parse(s);
            ResourceKey<WorldPreset> key = ResourceKey.create(Registries.WORLD_PRESET, id);
            registry.get(key).ifPresentOrElse(
                    holder -> result.add(new WorldCreationUiState.WorldTypeEntry(holder)),
                    () -> DefaultWorldType.LOGGER.warn("World preset \"{}\" not found", id)
            );
        }

        return result;
    }

    @Unique
    private static void defaultWorldType$selectInitialWorldType(WorldCreationUiState state) {
        Optional<ResourceKey<WorldPreset>> configuredKey = Optional.of(ClientConfig.getKey());

        WorldCreationUiState.WorldTypeEntry entry = WorldCreationUiState
                .findPreset(state.getSettings(), configuredKey)
                .map(WorldCreationUiState.WorldTypeEntry::new)
                .orElse(state.normalPresetList.getFirst());

        state.setWorldType(entry);
    }

    @Unique
    private static void defaultWorldType$setFlatSettings(WorldCreationUiState state) {
        state.updateDimensions((registry, worldDimensions) -> {
            WorldCreationContext settings = state.getSettings();

            HolderGetter<Block> block = settings.worldgenLoadContext().lookupOrThrow(Registries.BLOCK);
            HolderGetter<Biome> biome = settings.worldgenLoadContext().lookupOrThrow(Registries.BIOME);
            HolderGetter<StructureSet> structureSet = settings.worldgenLoadContext().lookupOrThrow(Registries.STRUCTURE_SET);
            HolderGetter<PlacedFeature> placedFeature = settings.worldgenLoadContext().lookupOrThrow(Registries.PLACED_FEATURE);
            FlatLevelSource flatLevelSource = new FlatLevelSource(PresetFlatWorldScreen.fromString(block, biome, structureSet, placedFeature, ClientConfig.flatMapSettings.get(), FlatLevelGeneratorSettings.getDefault(biome, structureSet, placedFeature)));

            return worldDimensions.replaceOverworldGenerator(registry, flatLevelSource);
        });
    }

    @Unique
    private static void defaultWorldType$setSingleBiome(WorldCreationUiState state) {
        state.updateDimensions((registry, worldDimensions) -> {
            WorldCreationContext settings = state.getSettings();

            Registry<Biome> biomes = settings.worldgenLoadContext().lookupOrThrow(Registries.BIOME);
            Registry<NoiseGeneratorSettings> noiseGeneratorSettings = settings.worldgenLoadContext().lookupOrThrow(Registries.NOISE_SETTINGS);
            Holder.Reference<Biome> biomeReference = biomes.get(ClientConfig.getFixedBiome()).orElseGet(() -> biomes.getOrThrow(Biomes.PLAINS));
            FixedBiomeSource fixedBiomeSource = new FixedBiomeSource(biomeReference);
            NoiseBasedChunkGenerator noiseBasedChunkGenerator = new NoiseBasedChunkGenerator(fixedBiomeSource, noiseGeneratorSettings.getOrThrow(NoiseGeneratorSettings.OVERWORLD));

            return worldDimensions.replaceOverworldGenerator(registry, noiseBasedChunkGenerator);
        });
    }
}
