package io.nextop.log;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.nextop.WireValue;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

// log call to be written upstream
// this is the foundation of network logging in nextop
public final class LogEntry {
    /////// FACTORIES ///////
    /* these correspond to message types in {@link Log} */

    public static LogEntry count(Level level, String key, long d) {
        return new LogEntry(Type.COUNT, level, key, d,
                null, null, null);
    }

    public static LogEntry metric(Level level, String key, long value, Log.Unit unit) {
        return new LogEntry(Type.METRIC, level, key, value, unit,
                null, null);
    }

    public static LogEntry message(Level level, String key, @Nullable String message) {
        return new LogEntry(Type.MESSAGE, level, key,
                0L, null,
                message,
                null);
    }

    public static LogEntry handled(Level level, String key, Throwable t, @Nullable String message) {
        return new LogEntry(Type.HANDLED, level, key,
                0L, null,
                message,
                LogThrowable.valueOf(t));
    }

    public static LogEntry unhandled(Level level, String key, Throwable t, @Nullable  String message) {
        return new LogEntry(Type.UNHANDLED, level, key,
                0L, null,
                message,
                LogThrowable.valueOf(t));
    }


    /////// SERIALIZATION ///////

    private static final int S_VERSION = 1;

    private static final String S_KEY_VERSION = "version";
    private static final String S_KEY_TYPE = "type";
    private static final String S_KEY_LEVEL = "level";
    private static final String S_KEY_KEY = "key";
    private static final String S_KEY_VALUE = "value";
    private static final String S_KEY_UNIT = "unit";
    private static final String S_KEY_MESSAGE = "message";
    private static final String S_KEY_THROWABLE = "throwable";

    private static final String S_THROWABLE_KEY_CLASS_NAME = "className";
    private static final String S_THROWABLE_KEY_MESSAGE = "message";
    private static final String S_THROWABLE_KEY_STACK_TRACE = "stackTrace";
    private static final String S_THROWABLE_KEY_CAUSE = "cause";

    private static final String S_TRACE_CLASS_NAME = "className";
    private static final String S_TRACE_FILE_NAME = "fileName";
    private static final String S_TRACE_LINE_NUMBER = "lineNumber";
    private static final String S_TRACE_METHOD_NAME = "methodName";



    public static WireValue toWireValue(LogEntry entry) {
        Map<Object, Object> map = new HashMap<Object, Object>(32);
        map.put(S_KEY_VERSION, S_VERSION);
        // v1
        map.put(S_KEY_TYPE, entry.type.toString());
        map.put(S_KEY_LEVEL, entry.level.getName());
        map.put(S_KEY_KEY, entry.key);
        switch (entry.type) {
            case COUNT:
            case METRIC:
                map.put(S_KEY_VALUE, entry.value);
                break;
            default:
                // no value
                break;
        }
        switch (entry.type) {
            case METRIC:
                map.put(S_KEY_UNIT, entry.unit.toString());
                break;
            default:
                // no unit
                break;
        }
        if (null != entry.message) {
            map.put(S_KEY_MESSAGE, entry.message);
        }
        if (null != entry.t) {
            map.put(S_KEY_THROWABLE, throwableToWireValue(entry.t));
        }
        return WireValue.of(map);
    }

    public static LogEntry fromWireValue(WireValue value) {
        Map<WireValue, WireValue> map = value.asMap();
        int version = map.get(S_KEY_VERSION).asInt();
        switch (version) {
            default:
                // from the future
                // attempt to parse it as the last known version
                // (if fails, the parsing will error out)
                if (version < S_VERSION) {
                    throw new IllegalArgumentException();
                } // else fall through
            case 1:
                Type type = Type.valueOf(map.get(S_KEY_TYPE).asString());
                Level level = Level.parse(map.get(S_KEY_LEVEL).asString());
                String key = map.get(S_KEY_KEY).asString();
                long v;
                if (map.containsKey(S_KEY_VALUE)) {
                    v = map.get(S_KEY_VALUE).asLong();
                } else {
                    v = 0L;
                }
                @Nullable Log.Unit unit;
                if (map.containsKey(S_KEY_UNIT)) {
                    unit = Log.Unit.valueOf(map.get(S_KEY_UNIT).asString());
                } else {
                    unit = null;
                }
                @Nullable String message;
                if (map.containsKey(S_KEY_MESSAGE)) {
                    message = map.get(S_KEY_MESSAGE).asString();
                } else {
                    message = null;
                }
                @Nullable LogThrowable t;
                if (map.containsKey(S_KEY_THROWABLE)) {
                    t = throwableFromWireValue(map.get(S_KEY_THROWABLE), version);
                } else {
                    t = null;
                }
                return new LogEntry(type, level, key, v, unit, message, t);
        }
    }

    private static WireValue throwableToWireValue(LogThrowable t) {
        // version is pinned to containing log entry (see #toWireValue)

        Map<Object, Object> map = new HashMap<Object, Object>(8);

        map.put(S_THROWABLE_KEY_CLASS_NAME, t.className);
        if (null != t.message) {
            map.put(S_THROWABLE_KEY_MESSAGE, t.message);
        }
        map.put(S_THROWABLE_KEY_STACK_TRACE, stackTraceToWireValue(t.stackTrace));
        if (null != t.cause) {
            map.put(S_THROWABLE_KEY_CAUSE, throwableToWireValue(t.cause));
        }

        return WireValue.of(map);
    }

    private static LogThrowable throwableFromWireValue(WireValue value, int version) {
        switch (version) {
            default:
                // see notes in #fromWireValue
                if (version < S_VERSION) {
                    throw new IllegalArgumentException();
                } // else fall through
            case 1:
                Map<WireValue, WireValue> map = value.asMap();
                String className = map.get(S_THROWABLE_KEY_CLASS_NAME).asString();
                @Nullable String message;
                if (map.containsKey(S_THROWABLE_KEY_MESSAGE)) {
                    message = map.get(S_THROWABLE_KEY_MESSAGE).asString();
                } else {
                    message = null;
                }
                ImmutableList<StackTraceElement> stackTrace = ImmutableList.copyOf(
                        stackTraceFromWireValue(map.get(S_THROWABLE_KEY_STACK_TRACE), version));
                @Nullable LogThrowable cause;
                if (map.containsKey(S_THROWABLE_KEY_CAUSE)) {
                    cause = throwableFromWireValue(map.get(S_THROWABLE_KEY_CAUSE), version);
                } else {
                    cause = null;
                }
                return new LogThrowable(className, message, stackTrace, cause);
        }
    }


    private static WireValue stackTraceToWireValue(List<StackTraceElement> stackTrace) {
        return WireValue.of(Lists.transform(stackTrace, new Function<StackTraceElement, WireValue>() {
            @Override
            public WireValue apply(@Nullable StackTraceElement input) {
                return stackTraceElementToWireValue(input);
            }
        }));
    }

    private static WireValue stackTraceElementToWireValue(StackTraceElement stackTraceElement) {
        // version is pinned to containing log entry (see #toWireValue)

        Map<Object, Object> map = new HashMap<Object, Object>(8);

        map.put(S_TRACE_CLASS_NAME, stackTraceElement.getClassName());
        map.put(S_TRACE_FILE_NAME, stackTraceElement.getFileName());
        map.put(S_TRACE_LINE_NUMBER, stackTraceElement.getLineNumber());
        map.put(S_TRACE_METHOD_NAME, stackTraceElement.getMethodName());

        return WireValue.of(map);
    }

    private static List<StackTraceElement> stackTraceFromWireValue(WireValue value, final int version) {
        switch (version) {
            default:
                // see notes in #fromWireValue
                if (version < S_VERSION) {
                    throw new IllegalArgumentException();
                } // else fall through
            case 1:
                return Lists.transform(value.asList(), new Function<WireValue, StackTraceElement>() {
                    @Override
                    public StackTraceElement apply(@Nullable WireValue input) {
                        return stackTraceElementFromWireValue(input, version);
                    }
                });
        }
    }

    private static StackTraceElement stackTraceElementFromWireValue(WireValue value, int version) {
        switch (version) {
            default:
                // see notes in #fromWireValue
                if (version < S_VERSION) {
                    throw new IllegalArgumentException();
                } // else fall through
            case 1:
                Map<WireValue, WireValue> map = value.asMap();
                String className = map.get(S_TRACE_CLASS_NAME).asString();
                String fileName = map.get(S_TRACE_FILE_NAME).asString();
                int lineNumber = map.get(S_TRACE_LINE_NUMBER).asInt();
                String methodName = map.get(S_TRACE_METHOD_NAME).asString();
                return new StackTraceElement(className, methodName, fileName, lineNumber);
        }
    }


    //

    public static enum Type {
        COUNT,
        METRIC,
        MESSAGE,
        HANDLED,
        UNHANDLED
    }


    public final Type type;
    public final Level level;
    public final String key;
    public final long value;
    @Nullable
    public final Log.Unit unit;
    @Nullable
    public final String message;
    @Nullable
    public final LogThrowable t;


    LogEntry(Type type, Level level, String key, long value, @Nullable Log.Unit unit, @Nullable String message, @Nullable LogThrowable t) {
        this.type = type;
        this.level = level;
        this.key = key;
        this.value = value;
        this.unit = unit;
        this.message = message;
        this.t = t;
    }


    public void writeTo(Log log) {
        switch (type) {
            case COUNT:
                log.count(level, key, value);
                break;
            case METRIC:
                log.metric(level, key, value, unit);
                break;
            case MESSAGE:
                log.message(level, key, message);
                break;
            case HANDLED:
                log.handled(level, key, t.toThrowable(), message);
                break;
            case UNHANDLED:
                log.unhandled(level, key, t.toThrowable(), message);
                break;
            default:
                throw new IllegalStateException();
        }
    }


    public static final class LogThrowable {
        public static LogThrowable valueOf(Throwable t) {
            @Nullable LogThrowable cause;
            if (null != t.getCause()) {
                cause = valueOf(t.getCause());
            } else {
                cause = null;
            }

            return new LogThrowable(t.getClass().getCanonicalName(), t.getMessage(),
                ImmutableList.copyOf(t.getStackTrace()), cause);
        }


        public final String className;
        @Nullable
        public final String message;
        public final ImmutableList<StackTraceElement> stackTrace;
        @Nullable
        public final LogThrowable cause;

        LogThrowable(String className, @Nullable String message, ImmutableList<StackTraceElement> stackTrace, @Nullable LogThrowable cause) {
            this.className = className;
            this.message = message;
            this.stackTrace = stackTrace;
            this.cause = cause;
        }


        public Throwable toThrowable() {
            Throwable t = _toThrowable();
            t.setStackTrace(stackTrace.toArray(new StackTraceElement[stackTrace.size()]));
            return t;
        }
        private Throwable _toThrowable() {
            Throwable ct;
            if (null != cause) {
                ct = cause.toThrowable();
            } else {
                ct = null;
            }

            try {
                Class<? extends Throwable> clazz = (Class<? extends Throwable>) Class.forName(className);
                try {
                    Constructor<? extends Throwable> c = clazz.getConstructor(String.class, Throwable.class);
                    return c.newInstance(message, ct);
                } catch (NoSuchMethodException e) {
                    if (null == message && null == ct) {
                        Constructor<? extends Throwable> c = clazz.getConstructor();
                        return c.newInstance();
                    } else if (null == message) {
                        Constructor<? extends Throwable> c = clazz.getConstructor(Throwable.class);
                        return c.newInstance(ct);
                    } else if (null == ct) {
                        Constructor<? extends Throwable> c = clazz.getConstructor(String.class);
                        return c.newInstance(message);
                    } else {
                        // no compatible constructor
                        return new LogThrowableMissingImplementation(className, message, ct);
                    }
                }
            } catch (Exception e) {
                return new LogThrowableMissingImplementation(className, message, ct);
            }
        }
    }


    public static final class LogThrowableMissingImplementation extends Throwable {
        private final String className;
        private final String message;


        public LogThrowableMissingImplementation(String className, @Nullable String message, @Nullable Throwable cause) {
            super(concat(className, message), cause);
            this.className = className;
            this.message = message;
        }


        private static String concat(String className, @Nullable String message) {
            String prefix = String.format("Missing \"%s\"", className);
            if (null != message) {
                return String.format("%s: %s", prefix, message);
            } else {
                return message;
            }
        }
    }
}
