package io.nextop.log;

import rx.schedulers.Schedulers;

import java.util.EnumSet;

/** global log inversion of control.
 * All parts of the system that need to log should use this.
 * Implementations should set {@link #nl} to the correct logger. */
public final class NL {
    // FIXME perf
    // FIXME each platform has its own logging system to hook into
    public static Log nl = new AggregatorLog(Outs.mask(Outs.sysout(),
            EnumSet.complementOf(EnumSet.of(LogEntry.Type.MESSAGE)),
            EnumSet.noneOf(LogEntry.Type.class)), Schedulers.computation());

    private NL() {
    }
}
