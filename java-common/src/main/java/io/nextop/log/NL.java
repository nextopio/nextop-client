package io.nextop.log;

/** global log inversion of control.
 * All parts of the system that need to log should use this.
 * Implementations should set {@link #nl} to the correct logger. */
public final class NL {
    public static Log nl = new DefaultLog(Outs.empty());

    private NL() {
    }
}
