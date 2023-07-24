package de.melanx.defaultworldtype.mixin;

import de.melanx.defaultworldtype.ClientConfig;
import de.melanx.defaultworldtype.DefaultWorldType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
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
    private static CreateWorldScreen modifyWorldScreen(Minecraft minecraft, Screen lastScreen, WorldCreationContext settings, Optional<ResourceKey<WorldPreset>> preset, OptionalLong seed) {
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

        DefaultWorldType.LOGGER.info("Set world type to " + ClientConfig.getKey().location());
        return new CreateWorldScreen(minecraft, lastScreen, settings, Optional.of(ClientConfig.getKey()), seed);
    }
}
