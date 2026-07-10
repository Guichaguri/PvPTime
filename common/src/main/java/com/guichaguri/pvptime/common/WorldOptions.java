package com.guichaguri.pvptime.common;

import com.guichaguri.pvptime.api.IWorldOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorldOptions implements IWorldOptions {

    private boolean enabled = false;
    private int engineMode = 1; // -2, -1, 1 or 2
    private int totalDayTime = 24000;
    private int pvptimeStart = 13000;
    private int pvptimeEnd = 500;
    private String startMessage = "&cIt's night and PvP is turned on";
    private String endMessage = "&aIt's daytime and PvP is turned off";
    private List<String> startCmds = new ArrayList<>();
    private List<String> endCmds = new ArrayList<>();

    public WorldOptions(boolean enabled, int engineMode, int totalDayTime, int pvptimeStart, int pvptimeEnd,
                        String startMessage, String endMessage, List<String> startCmds, List<String> endCmds) {
        this(enabled, engineMode, pvptimeStart, pvptimeEnd, startMessage, endMessage);
        this.totalDayTime = totalDayTime;
        this.startCmds = startCmds;
        this.endCmds = endCmds;
    }

    @Deprecated
    public WorldOptions(boolean enabled, int engineMode, int totalDayTime, int pvptimeStart, int pvptimeEnd,
                        String startMessage, String endMessage, String[] startCmds, String[] endCmds) {
        this(enabled, engineMode, pvptimeStart, pvptimeEnd, startMessage, endMessage);
        this.totalDayTime = totalDayTime;
        this.startCmds = Arrays.stream(startCmds).toList();
        this.endCmds = Arrays.stream(endCmds).toList();
    }

    public WorldOptions(boolean enabled, int engineMode, int pvptimeStart, int pvptimeEnd, String startMessage, String endMessage) {
        this.enabled = enabled;
        this.engineMode = engineMode;
        this.pvptimeStart = pvptimeStart;
        this.pvptimeEnd = pvptimeEnd;
        this.startMessage = startMessage;
        this.endMessage = endMessage;
    }

    public WorldOptions(IWorldOptions o) {
        this(o.isEnabled(), o.getEngineMode(),
                o.getTotalDayTime(), o.getPvPTimeStart(), o.getPvPTimeEnd(),
                o.getStartMessage(), o.getEndMessage(),
                o.getStartCommands(), o.getEndCommands());
    }

    public WorldOptions() {}

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public int getEngineMode() {
        return engineMode;
    }

    @Override
    public void setEngineMode(int engineMode) {
        this.engineMode = engineMode;
    }

    @Override
    public int getPvPTimeStart() {
        return pvptimeStart;
    }

    @Override
    public void setPvPTimeStart(int pvptimeStart) {
        this.pvptimeStart = pvptimeStart;
    }

    @Override
    public int getPvPTimeEnd() {
        return pvptimeEnd;
    }

    @Override
    public void setPvPTimeEnd(int pvptimeEnd) {
        this.pvptimeEnd = pvptimeEnd;
    }

    @Override
    public String getStartMessage() {
        return startMessage;
    }

    @Override
    public void setStartMessage(String startMessage) {
        this.startMessage = startMessage;
    }

    @Override
    public String getEndMessage() {
        return endMessage;
    }

    @Override
    public void setEndMessage(String endMessage) {
        this.endMessage = endMessage;
    }

    @Override
    public List<String> getStartCommands() {
        return startCmds;
    }

    @Override
    public void setStartCommands(List<String> startCmds) {
        this.startCmds = startCmds;
    }

    @Override
    public List<String> getEndCommands() {
        return endCmds;
    }

    @Override
    public void setEndCommands(List<String> endCmds) {
        this.endCmds = endCmds;
    }

    @Override
    public int getTotalDayTime() {
        return totalDayTime;
    }

    @Override
    public void setTotalDayTime(int totalDayTime) {
        this.totalDayTime = totalDayTime;
    }

}