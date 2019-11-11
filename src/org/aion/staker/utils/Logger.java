package org.aion.staker.utils;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Logger {
    private final PrintStream out;
    private static final TimeZone TZ;
    private static final DateFormat FMT;
    private final boolean verboseLoggingEnabled;

    static {
        TZ  = TimeZone.getTimeZone("UTC");
        // ISO-8601 with hardcoded 'Z' because we're always UTC
        FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        FMT.setTimeZone(TZ);
    }

    /** constructor */
    public Logger(boolean verboseLoggingEnabled) {
        this(System.out, verboseLoggingEnabled);
    }

    /** constructor */
    public Logger(PrintStream out, boolean verboseLoggingEnabled) {
        this.out = out;
        this.verboseLoggingEnabled = verboseLoggingEnabled;
    }

    /** log the date and a message */
    public void log(String message) {
        log(message, new Date() /* now */);
    }

    public void logVerbose(String message) {
        if(verboseLoggingEnabled) {
            log(message, new Date() /* now */);
        }
    }

    private void log(String message, Date when) {
        String now = FMT.format(when);
        out.println(String.format("[%s] %s", now, message));
    }
}
