package io.nextop.log;

import rx.functions.Func0;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// common logging interface for all of Nextop
// in this interface, keys are dot-separated strings that form a hierarchy (like Graphite)
// implementations should be lightweight the system can log metrics everywhere
/** thread-safe */
public interface Log {


    void count(String key);
    void count(String key, long d);
    void count(Level level, String key, long d);

    /** @param unit a {@link TimeUnit} or general {@link Unit} */
    void metric(String key, long value, Object unit);
    void metric(Level level, String key, long value, Object unit);


    <R> R duration(String key, Func0<R> eval);
    <R> R duration(Level level, String key, Func0<R> eval);

    /** does not log on exception */
    <R> R durationWithException(String key, Callable<R> eval) throws Exception;
    /** does not log on exception */
    <R> R durationWithException(Level level, String key, Callable<R> eval) throws Exception;

    void message(String key);
    void message(String key, @Nullable String messageFormat, Object ... args);
    void message(Level level, String key, @Nullable String messageFormat, Object ... args);

    void handled(String key, java.lang.Throwable t);
    void handled(Level level, String key, Throwable t);
    void handled(String key, Throwable t, @Nullable String messageFormat, Object ... args);
    void handled(Level level, String key, Throwable t, @Nullable String messageFormat, Object ... args);

    void unhandled(String key, Throwable t);
    void unhandled(Level level, String key, Throwable t);
    void unhandled(String key, Throwable t, @Nullable String messageFormat, Object ... args);
    void unhandled(Level level, String key, Throwable t, @Nullable String messageFormat, Object ... args);


    class Unit {
        private final String name;

        public Unit(String name) {
            this.name = name;
        }

        // convert to this unit from "source"
        public long convert(long sourceValue, Unit sourceUnit) {
            throw new UnsupportedOperationException();
        }


        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }


        public static Unit timeUnit(TimeUnit timeUnit) {
            class T extends Unit {
                final TimeUnit timeUnit;

                T(TimeUnit timeUnit) {
                    super(timeUnitName(timeUnit));
                    this.timeUnit = timeUnit;
                }

                @Override
                public long convert(long sourceValue, Unit sourceUnit) {
                    if (!(sourceUnit instanceof T)) {
                        throw new IllegalArgumentException();
                    }
                    return timeUnit.convert(sourceValue, ((T) sourceUnit).timeUnit);
                }
            }
            return new T(timeUnit);
        }
        private static String timeUnitName(TimeUnit timeUnit) {
            switch (timeUnit) {
                case NANOSECONDS:
                    return "ns";
                case MICROSECONDS:
                    return "us";
                case MILLISECONDS:
                    return "ms";
                case SECONDS:
                    return "s";
                case MINUTES:
                    return "m";
                case HOURS:
                    return "h";
                case DAYS:
                    return "d";
                default:
                    throw new IllegalArgumentException();
            }
        }
    }


    /** thread-safe */
    interface Out {
        /** @return the line width of the log/console, so output can be properly formatted */
        int lineWidth();
        /** @return the width of keys in the log/console. Must be <= {@link #lineWidth} */
        int keyWidth();
        int valueWidth();
        int unitWidth();
        void write(Level level, String ... lines);
    }

}
