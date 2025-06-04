package de.melanx.defaultworldtype.mixin;

import de.melanx.defaultworldtype.ClientConfig;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SwitchGrid;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CreateWorldScreen.WorldTab.class)
public class WorldTabMixin {

    @Inject(
            method = "<init>",
            at = @At("TAIL"),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void onInit(CreateWorldScreen this$0, CallbackInfo ci, GridLayout.RowHelper gridlayout$rowhelper, CycleButton<WorldCreationUiState.WorldTypeEntry> cyclebutton, SwitchGrid.Builder switchgrid$builder, SwitchGrid switchgrid) {
        if (ClientConfig.disablePresetSelectionButton.get()) {
            this$0.getUiState().addListener(state -> {
                cyclebutton.active = false;
                cyclebutton.setTooltip(Tooltip.create(Component.translatable("defaultworldtype.tooltip.disable")));
            });
        }
    }
}
