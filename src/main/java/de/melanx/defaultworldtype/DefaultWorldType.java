package de.melanx.defaultworldtype;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

@Mod(value = DefaultWorldType.MODID, dist = Dist.CLIENT)
public class DefaultWorldType {

    public static final String MODID = "defaultworldtype";
    public static final Logger LOGGER = LogManager.getLogger();

    public DefaultWorldType(ModContainer modContainer) {
        try {
            Files.createDirectory(ClientConfig.CONFIG_PATH);
        } catch (FileAlreadyExistsException e) {
            DefaultWorldType.LOGGER.debug("Config directory " + DefaultWorldType.MODID + " already exists. Skip creating.");
        } catch (IOException e) {
            DefaultWorldType.LOGGER.error("Failed to create " + DefaultWorldType.MODID + " config directory", e);
        }

        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC, DefaultWorldType.MODID + "/client-config.toml");
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
