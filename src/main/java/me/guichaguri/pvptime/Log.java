package me.guichaguri.pvptime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {

    private static final Logger log = LogManager.getLogger("PvPTime");

    public static void info(String msg) {
        log.info(msg);
    }

    public static void warn(String msg) {
        log.warn(msg);
    }

    public static void fatal(String msg) {
        log.fatal(msg);
    }

    public static void crash(Throwable t) {
        log.catching(t);
    }

}
