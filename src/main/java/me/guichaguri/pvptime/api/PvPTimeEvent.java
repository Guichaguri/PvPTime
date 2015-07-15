package me.guichaguri.pvptime.api;

import me.guichaguri.pvptime.WorldOptions;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * @author Guilherme Chaguri
 */
public class PvPTimeEvent extends Event {

    public static class PvPTimeUpdateEvent extends PvPTimeEvent {
        public final World world;
        public final boolean pvpEnabled;

        public boolean tryToAnnounce;

        public PvPTimeUpdateEvent(World world, boolean pvpEnabled) {
            this.world = world;
            this.pvpEnabled = pvpEnabled;
            this.tryToAnnounce = true;
        }

    }

    public static class PvPTimeWorldSetupEvent extends PvPTimeEvent {
        public final int dimensionId;
        public WorldOptions defaultOptions;

        public PvPTimeWorldSetupEvent(int dimensionId, boolean isOverworld) {
            this.dimensionId = dimensionId;
            this.defaultOptions = new WorldOptions(isOverworld);
        }

    }

}
