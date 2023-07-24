package de.melanx.defaultworldtype;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DefaultWorldType.MODID)
public class DefaultWorldType {

    public static final String MODID = "defaultworldtype";
    public static final Logger LOGGER = LogManager.getLogger();

    public DefaultWorldType() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientConfig.setup();
        }
    }
}
