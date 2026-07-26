package com.guichaguri.pvptime.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface CommandPerformCallback {
    Event<CommandPerformCallback> EVENT = EventFactory.createArrayBacked(CommandPerformCallback.class, (callbacks) -> () -> {
        for (CommandPerformCallback callback : callbacks) {
            callback.onCommand();
        }
    });

    /**
     * Called when a command is performed.
     */
    void onCommand();
}
