package com.guichaguri.pvptime.common;

import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.guichaguri.pvptime.api.IWorldOptions;
import java.util.Map;

/**
 * @author Guilherme Chaguri
 */
public abstract class PvPTime<D> implements IPvPTimeAPI<D> {

    public static final String VERSION = "2.0.3";

    private final Map<D, IWorldOptions> dimensions;
    private final Map<D, Boolean> cache;

    protected boolean atLeastTwoPlayers = false;

    public PvPTime(Map<D, IWorldOptions> dimensions, Map<D, Boolean> cache) {
        this.dimensions = dimensions;
        this.cache = cache;
    }

    public void resetWorldOptions() {
        dimensions.clear();
    }

    public long update() {
        long ticksLeft = Long.MAX_VALUE;

        for(D id : dimensions.keySet()) {
            ticksLeft = Math.min(ticksLeft, updateDimension(id));
        }

        return ticksLeft;
    }

    private long updateDimension(D id) {
        IWorldOptions options = dimensions.get(id);
        if(!options.isEnabled()) return Long.MAX_VALUE;

        Boolean was = cache.get(id);
        Boolean is = isRawPvPTime(id);

        if(is == null) {
            // Unloaded dimension
            return Long.MAX_VALUE;
        }

        if(was != null) {
            // Checks if the pvp time changed
            if(is != was) {
                // Announces it and updates the cache
                announce(id, options, is);
                cache.put(id, is);
            }
        } else {
            // There's no cache for this dimension, we'll create one
            cache.put(id, is);
        }

        return getTimeLeft(id, options, is);
    }

    public Boolean isRawPvPTime(D dimension) {
        IWorldOptions options = dimensions.get(dimension);
        if(options == null || !options.isEnabled()) return null;
        return isRawPvPTime(dimension, options);
    }

    protected abstract Boolean isRawPvPTime(D dimension, IWorldOptions options);

    protected abstract long getTimeLeft(D dimension, IWorldOptions options, boolean isPvPTime);

    protected abstract void announce(D dimension, IWorldOptions options, boolean isPvPTime);

    protected boolean checkPvPTime(IWorldOptions options, long totalTime) {
        long startTime = options.getPvPTimeStart();
        long endTime = options.getPvPTimeEnd();
        long time = totalTime % options.getTotalDayTime();

        if(startTime < endTime) {
            return time >= startTime && time < endTime;
        } else {
            return time >= startTime || time < endTime;
        }
    }

    protected long calculateTimeLeft(IWorldOptions options, long totalTime, boolean isPvPTime) {
        long totalDayTime = options.getTotalDayTime();
        long time = totalTime % totalDayTime;

        if(isPvPTime) {
            long end = options.getPvPTimeEnd();

            if(time > end) {
                return totalDayTime - time + end;
            } else {
                return end - time;
            }
        } else {
            long start = options.getPvPTimeStart();

            if(time > start) {
                return totalDayTime - time + start;
            } else {
                return start - time;
            }
        }
    }

    @Override
    public Boolean isPvPTime(D dimension) {
        Boolean isPvPTime = cache.get(dimension);
        if(isPvPTime != null) return isPvPTime;

        isPvPTime = isRawPvPTime(dimension);
        if(isPvPTime != null) cache.put(dimension, isPvPTime);

        return isPvPTime;
    }

    @Override
    public IWorldOptions getWorldOptions(D dimension) {
        return dimensions.get(dimension);
    }

    @Override
    public void setWorldOptions(D dimension, IWorldOptions options) {
        dimensions.put(dimension, options);
    }

    @Override
    public IWorldOptions createWorldOptions(IWorldOptions base) {
        return new WorldOptions(base);
    }

    @Override
    public IWorldOptions createWorldOptions() {
        return new WorldOptions();
    }

    public void setAtLeastTwoPlayers(boolean atLeastTwoPlayers) {
        this.atLeastTwoPlayers = atLeastTwoPlayers;
    }
}
