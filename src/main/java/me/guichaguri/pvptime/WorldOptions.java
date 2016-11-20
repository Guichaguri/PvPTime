package me.guichaguri.pvptime;

public class WorldOptions {
    private boolean enabled = true;
    private int engineMode = 1; // -2, -1, 1 or 2
    private int totalDayTime = 24000;
    private int pvptimeStart = 13000;
    private int pvptimeEnd = 500;
    private String startMessage = "&cIt's night and PvP is turned on";
    private String endMessage = "&aIt's daytime and PvP is turned off";
    private String[] startCmds = new String[0];
    private String[] endCmds = new String[0];

    public WorldOptions(boolean enabled, int engineMode, int totalDayTime, int pvptimeStart, int pvptimeEnd,
                        String startMessage, String endMessage, String[] startCmds, String[] endCmds) {
        this(enabled, engineMode, pvptimeStart, pvptimeEnd, startMessage, endMessage);
        this.totalDayTime = totalDayTime;
        this.startCmds = startCmds;
        this.endCmds = endCmds;
    }

    public WorldOptions(boolean enabled, int totalDayTime, int pvptimeStart, int pvptimeEnd,
                        String startMessage, String endMessage, String[] startCmds, String[] endCmds) {
        this(enabled, 1, totalDayTime, pvptimeStart, pvptimeEnd, startMessage, endMessage, startCmds, endCmds);
    }

    public WorldOptions(boolean enabled, int engineMode, int pvptimeStart, int pvptimeEnd, String startMessage, String endMessage) {
        this.enabled = enabled;
        this.engineMode = engineMode;
        this.pvptimeStart = pvptimeStart;
        this.pvptimeEnd = pvptimeEnd;
        this.startMessage = startMessage;
        this.endMessage = endMessage;
    }

    public WorldOptions(boolean enabled, int pvptimeStart, int pvptimeEnd, String startMessage, String endMessage) {
        this(enabled, 1, pvptimeStart, pvptimeEnd, startMessage, endMessage);
    }

    public WorldOptions(int pvptimeStart, int pvptimeEnd) {
        this.enabled = true;
        this.pvptimeStart = pvptimeStart;
        this.pvptimeEnd = pvptimeEnd;
    }

    public WorldOptions(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getEngineMode() {
        return engineMode;
    }

    public void setEngineMode(int engineMode) {
        this.engineMode = engineMode;
    }

    public int getPvPTimeStart() {
        return pvptimeStart;
    }

    public void setPvPTimeStart(int pvptimeStart) {
        this.pvptimeStart = pvptimeStart;
    }

    public int getPvPTimeEnd() {
        return pvptimeEnd;
    }

    public void setPvPTimeEnd(int pvptimeEnd) {
        this.pvptimeEnd = pvptimeEnd;
    }

    public String getStartMessage() {
        return startMessage;
    }

    public void setStartMessage(String startMessage) {
        this.startMessage = startMessage;
    }

    public String getEndMessage() {
        return endMessage;
    }

    public void setEndMessage(String endMessage) {
        this.endMessage = endMessage;
    }

    public String[] getStartCmds() {
        return startCmds;
    }

    public void setStartCmds(String[] startCmds) {
        this.startCmds = startCmds;
    }

    public String[] getEndCmds() {
        return endCmds;
    }

    public void setEndCmds(String[] endCmds) {
        this.endCmds = endCmds;
    }

    public int getTotalDayTime() {
        return totalDayTime;
    }

    public void setTotalDayTime(int totalDayTime) {
        this.totalDayTime = totalDayTime;
    }
}