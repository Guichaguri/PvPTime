package com.guichaguri.pvptime.api;

import java.util.Arrays;
import java.util.List;

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

    List<String> getStartCommands();

    void setStartCommands(List<String> startCmds);

    List<String> getEndCommands();

    void setEndCommands(List<String> startCmds);

    /** Use {@link this#getStartCommands()} instead */
    @Deprecated()
    default String[] getStartCmds() {
        return getStartCommands().toArray(new String[0]);
    }

    /** Use {@link this#setStartCommands(List)} instead */
    @Deprecated
    default void setStartCmds(String[] startCmds) {
        setStartCommands(Arrays.stream(startCmds).toList());
    }

    /** Use {@link this#getEndCommands()} instead */
    @Deprecated
    default String[] getEndCmds() {
        return getEndCommands().toArray(new String[0]);
    }

    /** Use {@link this#setEndCommands(List)} instead */
    @Deprecated
    default void setEndCmds(String[] endCmds) {
        setEndCommands(Arrays.stream(endCmds).toList());
    }

    int getTotalDayTime();

    void setTotalDayTime(int totalDayTime);

}
