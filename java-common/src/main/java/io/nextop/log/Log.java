package io.nextop.log;

import io.nextop.WireValue;
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


    // duration* methods write as metrics. the metric is the time to run the callback

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
        public static Unit create(String name) {
            try {
                return timeUnit(name);
            } catch (IllegalArgumentException e) {
                return new Unit(name);
            }
        }
        public static Unit valueOf(Object unit) {
            if (unit instanceof Unit) {
                return (Unit) unit;
            }
            if (unit instanceof TimeUnit) {
                return timeUnit((TimeUnit) unit);
            }
            return create(String.valueOf(unit));
        }


        private final String name;

        private Unit(String name) {
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



        private static Unit timeUnit(TimeUnit timeUnit) {
            class T extends Unit {
                final TimeUnit timeUnit;

                T(TimeUnit timeUnit) {
                    super(timeUnitToName(timeUnit));
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
        private static Unit timeUnit(String name) {
            return timeUnit(timeUnitFromName(name));
        }
        private static String timeUnitToName(TimeUnit timeUnit) {
            switch (timeUnit) {
                case NANOSECONDS:
                    return NAME_NANOSECONDS;
                case MICROSECONDS:
                    return NAME_MICROSECONDS;
                case MILLISECONDS:
                    return NAME_MILLISECONDS;
                case SECONDS:
                    return NAME_SECONDS;
                case MINUTES:
                    return NAME_MINUTES;
                case HOURS:
                    return NAME_HOURS;
                case DAYS:
                    return NAME_DAYS;
                default:
                    throw new IllegalArgumentException();
            }
        }
        private static TimeUnit timeUnitFromName(String name) {
            String n = name.toLowerCase();
            if (NAME_NANOSECONDS.equals(n)) {
                return TimeUnit.NANOSECONDS;
            } else if (NAME_MICROSECONDS.equals(n)) {
                return TimeUnit.MICROSECONDS;
            } else if (NAME_MILLISECONDS.equals(n)) {
                return TimeUnit.MILLISECONDS;
            } else if (NAME_SECONDS.equals(n)) {
                return TimeUnit.SECONDS;
            } else if (NAME_MINUTES.equals(n)) {
                return TimeUnit.MINUTES;
            } else if (NAME_HOURS.equals(n)) {
                return TimeUnit.HOURS;
            } else if (NAME_DAYS.equals(n)) {
                return TimeUnit.DAYS;
            } else {
                throw new IllegalArgumentException();
            }
        }
        private static final String NAME_NANOSECONDS = "ns";
        private static final String NAME_MICROSECONDS = "us";
        private static final String NAME_MILLISECONDS = "ms";
        private static final String NAME_SECONDS = "s";
        private static final String NAME_MINUTES = "m";
        private static final String NAME_HOURS = "h";
        private static final String NAME_DAYS = "d";
    }


    /** thread-safe */
    interface Out {
        boolean isWrite(Level level, LogEntry.Type type);
        /** @return the line width of the log/console, so output can be properly formatted */
        int lineWidth();
        /** @return the width of keys in the log/console. Must be <= {@link #lineWidth} */
        int keyWidth();
        int valueWidth();
        int unitWidth();
        void write(Level level, LogEntry.Type type, String ... lines);

        boolean isWriteUp(Level level, LogEntry.Type type);
        // this can be used to write aggregate statistics or critical logs (e.g. crashes) to an upstream
        void writeUp(LogEntry entry);
    }

}
