package com.guichaguri.pvptime.api;

/**
 * @author Guilherme Chaguri
 */
public interface IWorldOptions {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    int getEngineMode();

    void setEngineMode(int engineMode);

    int getPvPTimeStart();

    void setPvPTimeStart(int pvptimeStart);

    int getPvPTimeEnd();

    void setPvPTimeEnd(int pvptimeEnd);

    String getStartMessage();

    void setStartMessage(String startMessage);

    String getEndMessage();

    void setEndMessage(String endMessage);

    String[] getStartCmds();

    void setStartCmds(String[] startCmds);

    String[] getEndCmds();

    void setEndCmds(String[] endCmds);

    int getTotalDayTime();

    void setTotalDayTime(int totalDayTime);

}
