package de.melanx.defaultworldtype.mixin;

import de.melanx.defaultworldtype.ClientConfig;
import de.melanx.defaultworldtype.DefaultWorldType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
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

        WorldCreationUiStateMixin.defaultWorldType$writeAvailablePresetsFile(worldPresets);

        List<WorldCreationUiState.WorldTypeEntry> preferredPresets = defaultWorldType$buildPreferredPresetEntries(worldPresets);
        if (!preferredPresets.isEmpty()) {
            state.normalPresetList.clear();
            state.normalPresetList.addAll(preferredPresets);
        }

        WorldCreationUiStateMixin.defaultWorldType$selectInitialWorldType(state);
    }

    @Unique
    private static void defaultWorldType$writeAvailablePresetsFile(Registry<WorldPreset> registry) {
        List<ResourceLocation> ids = registry.entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .map(ResourceKey::location)
                .toList();

        String content = ids.size() + " possible world presets found:\n" +
                ids.stream()
                        .map(loc -> "- \"" + loc + "\"")
                        .collect(Collectors.joining("\n"));

        try {
            Files.writeString(ClientConfig.CONFIG_PATH.resolve("world-presets.txt"), content);
        } catch (IOException ex) {
            DefaultWorldType.LOGGER.error("Couldn't generate file with existing presets", ex);
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

}
