package de.melanx.defaultworldtype;

import net.minecraft.world.WorldType;

public class Util {

    public static String getWorldTypeNames() {
        StringBuilder stringBuilder = new StringBuilder();
        for (WorldType worldType : WorldType.WORLD_TYPES) {
            if (worldType != null)
                stringBuilder.append("\n- ").append(worldType.getName());
        }
        return stringBuilder.toString();
    }

    public static int countWorldTypes() {
        int x = 0;
        for (WorldType type : WorldType.WORLD_TYPES) {
            if (type != null) {
                x += 1;
            }
        }
        return x;
    }

}
