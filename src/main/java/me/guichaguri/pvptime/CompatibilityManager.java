package me.guichaguri.pvptime;

import java.lang.reflect.Method;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import org.bukkit.World.Environment;
import org.spongepowered.api.world.DimensionTypes;

/**
 * @author Guilherme Chaguri
 */
public class CompatibilityManager {

    public static boolean isBukkitServer() {
        boolean bukkit = true;
        try  {
            Class.forName("org.bukkit.Bukkit");
        } catch(Exception ex) {
            bukkit = false;
        }
        return bukkit;
    }

    public static Object getBukkitWorld(World w) {
        try {
            for(Method m : World.class.getDeclaredMethods()) {
                if(org.bukkit.World.class.isAssignableFrom(m.getReturnType())) {
                    return m.invoke(w);
                }
            }
        } catch(Exception ex) {
            return null;
        }
        return null;
    }

    public static boolean createBukkitCompatibility(Configuration config, Object world, World mcWorld) {
        if(!(world instanceof org.bukkit.World)) return false;
        org.bukkit.World w = (org.bukkit.World)world;
        int id = mcWorld.provider.getDimensionId();
        String cat = w.getName();
        config.setCategoryComment(cat, "Options for dimension " + id + " - " + w.getName());
        boolean enabled = config.get(cat, "enabled", w.getEnvironment() == Environment.NORMAL).getBoolean();
        long start = config.get(cat, "startTime", 13000, "Time in ticks that the PvP will be enabled").getInt();
        long end = config.get(cat, "endTime", 500, "Time in ticks that the PvP will be disabled").getInt();
        String startMsg = config.get(cat, "startMessage", "&cIt's night and PvP is turned on").getString();
        String endMsg = config.get(cat, "endMessage", "&aIt's daytime and PvP is turned off").getString();
        WorldOptions options = new WorldOptions(enabled, start, end, startMsg, endMsg);
        PvPTimeRegistry.setWorldOptions(id, options);
        return true;
    }

    public static boolean isSpongeServer() {
        return Loader.isModLoaded("Sponge");
    }

    public static boolean createSpongeCompatibility(Configuration config, Object world, World mcWorld) {
        if(!(world instanceof org.spongepowered.api.world.World)) return false;
        org.spongepowered.api.world.World w = (org.spongepowered.api.world.World)world;
        int id = mcWorld.provider.getDimensionId();
        String cat = w.getName();
        config.setCategoryComment(cat, "Options for dimension " + id + " - " + w.getName());
        boolean enabled = config.get(cat, "enabled", w.getDimension().getType() == DimensionTypes.OVERWORLD).getBoolean();
        long start = config.get(cat, "startTime", 13000, "Time in ticks that the PvP will be enabled").getInt();
        long end = config.get(cat, "endTime", 500, "Time in ticks that the PvP will be disabled").getInt();
        String startMsg = config.get(cat, "startMessage", "&cIt's night and PvP is turned on").getString();
        String endMsg = config.get(cat, "endMessage", "&aIt's daytime and PvP is turned off").getString();
        WorldOptions options = new WorldOptions(enabled, start, end, startMsg, endMsg);
        PvPTimeRegistry.setWorldOptions(id, options);
        return true;
    }

}
