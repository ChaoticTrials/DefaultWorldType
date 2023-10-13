package de.melanx.defaultworldtype.mixin;

import de.melanx.defaultworldtype.ClientConfig;
import de.melanx.defaultworldtype.DefaultWorldType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PresetFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Redirect(
            method = "openFresh",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContext;Ljava/util/Optional;Ljava/util/OptionalLong;)Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;"
            )
    )
    private static CreateWorldScreen modifyWorldScreen(Minecraft minecraft, Screen lastScreen, WorldCreationContext settings, Optional<ResourceKey<WorldPreset>> oldPreset, OptionalLong seed) {
        List<ResourceLocation> presets = settings.worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET).entrySet().stream()
                .map(Map.Entry::getKey)
                .map(ResourceKey::location)
                .toList();
        String file = presets.size() + " possible world presets found:\n" + presets.stream()
                .map(location -> "- \"" + location + "\"")
                .collect(Collectors.joining("\n"));

        try {
            Files.writeString(ClientConfig.CONFIG_PATH.resolve("world-presets.txt"), file);
        } catch (IOException e) {
            DefaultWorldType.LOGGER.error("Couldn't generate file with existing presets", e);
        }

        Optional<Holder.Reference<WorldPreset>> holder = settings.worldgenLoadContext().registryOrThrow(Registries.WORLD_PRESET).getHolder(ClientConfig.getKey());
        CreateWorldScreen createWorldScreen = new CreateWorldScreen(minecraft, lastScreen, settings, oldPreset, seed);
        if (holder.isPresent()) {
            Holder.Reference<WorldPreset> preset = holder.get();
            DefaultWorldType.LOGGER.info("Set world type to " + ClientConfig.getKey().location());
            List<WorldCreationUiState.WorldTypeEntry> presetList = new ArrayList<>(createWorldScreen.getUiState().getNormalPresetList());
            presetList.addAll(createWorldScreen.getUiState().getAltPresetList());

            for (WorldCreationUiState.WorldTypeEntry worldTypeEntry : presetList) {
                if (worldTypeEntry.preset() == preset) {
                    createWorldScreen.getUiState().setWorldType(worldTypeEntry);
                    if (preset.is(WorldPresets.FLAT)) {
                        createWorldScreen.getUiState().updateDimensions((registry, worldDimensions) -> {
                            HolderGetter<Block> block = settings.worldgenLoadContext().lookupOrThrow(Registries.BLOCK);
                            HolderGetter<Biome> biome = settings.worldgenLoadContext().lookupOrThrow(Registries.BIOME);
                            HolderGetter<StructureSet> structureSet = settings.worldgenLoadContext().lookupOrThrow(Registries.STRUCTURE_SET);
                            HolderGetter<PlacedFeature> placedFeature = settings.worldgenLoadContext().lookupOrThrow(Registries.PLACED_FEATURE);
                            FlatLevelSource flatLevelSource = new FlatLevelSource(PresetFlatWorldScreen.fromString(block, biome, structureSet, placedFeature, ClientConfig.flatMapSettings.get(), FlatLevelGeneratorSettings.getDefault(biome, structureSet, placedFeature)));

                            return worldDimensions.replaceOverworldGenerator(registry, flatLevelSource);
                        });
                    }
                    break;
                }
            }
        }

        return createWorldScreen;
    }
}
