package me.guichaguri.pvptime;

public class WorldOptions {
    private boolean enabled = true;
    private long pvptimeStart = 13000;
    private long pvptimeEnd = 500;
    private String startMessage = "&cIt's night and PvP is turned on";
    private String endMessage = "&aIt's daytime and PvP is turned off";

    public WorldOptions(boolean enabled, long pvptimeStart, long pvptimeEnd, String startMessage, String endMessage) {
        this.enabled = enabled;
        this.pvptimeStart = pvptimeStart;
        this.pvptimeEnd = pvptimeEnd;
        this.startMessage = startMessage;
        this.endMessage = endMessage;
    }

    public WorldOptions(long pvptimeStart, long pvptimeEnd) {
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

    public long getPvPTimeStart() {
        return pvptimeStart;
    }

    public void setPvPTimeStart(long pvptimeStart) {
        this.pvptimeStart = pvptimeStart;
    }

    public long getPvPTimeEnd() {
        return pvptimeEnd;
    }

    public void setPvPTimeEnd(long pvptimeEnd) {
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
}
