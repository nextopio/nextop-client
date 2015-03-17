package io.nextop.log;

import rx.functions.Func0;

import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

// FIXME support upstream
public class DefaultLog implements Log {
    protected final Out out;


    protected final Level defaultLevel = Level.INFO;
    protected final Level defaultHandledLevel = Level.WARNING;
    protected final Level defaultUnhandledLevel = Level.SEVERE;


    public DefaultLog(Out out) {
        this.out = out;
    }


    /////// Log ///////

    @Override
    public void count(String keyFormat, Object ... keyArgs) {
        count(keyFormat, 1, keyArgs);
    }

    @Override
    public void count(String keyFormat, long d, Object ... keyArgs) {
        count(defaultLevel, keyFormat, d, keyArgs);
    }

    @Override
    public void count(Level level, String keyFormat, long d, Object ... keyArgs) {
        if (out.isWrite(level, LogEntry.Type.COUNT)) {
            String key = String.format(keyFormat, keyArgs);
            int r = out.lineWidth() - (Math.max(key.length(), out.keyWidth()) + 1);
            out.write(level, LogEntry.Type.COUNT, String.format("%-" + out.keyWidth() + "s %" + r + "d",
                    key, d));
        }
    }

    @Override
    public void metric(String keyFormat, long value, Object unit, Object ... keyArgs) {
        metric(defaultLevel, keyFormat, value, unit, keyArgs);
    }

    @Override
    public void metric(Level level, String keyFormat, long value, Object unit, Object ... keyArgs) {
        if (out.isWrite(level, LogEntry.Type.METRIC)) {
            String key = String.format(keyFormat, keyArgs);
            int r = out.lineWidth() - (Math.max(key.length(), out.keyWidth()) + 1);
            out.write(level, LogEntry.Type.METRIC, String.format("%-" + out.keyWidth() + "s %" + r + "s",
                    key, String.format("%d %-" + out.unitWidth() + "s", value, unit)));
        }
    }

    @Override
    public <R> R duration(String key, Func0<R> eval) {
        return duration(defaultLevel, key, eval);
    }

    @Override
    public <R> R duration(Level level, String key, Func0<R> eval) {
        long startNanos = System.nanoTime();
        R r = eval.call();
        metric(level, key, System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        return r;
    }

    @Override
    public <R> R durationWithException(String key, Callable<R> eval) throws Exception {
        return durationWithException(key, eval);
    }

    @Override
    public <R> R durationWithException(Level level, String key, Callable<R> eval) throws Exception {
        // see #duration
        long startNanos = System.nanoTime();
        R r = eval.call();
        metric(level, key, System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        return r;
    }

    @Override
    public void message(String key) {
        message(key, null);
    }

    @Override
    public void message(String key, @Nullable String messageFormat, Object ... args) {
        message(defaultLevel, key, messageFormat, args);
    }

    @Override
    public void message(Level level, String key, @Nullable String messageFormat, Object ... args) {
        if (out.isWrite(level, LogEntry.Type.MESSAGE)) {
            if (null != messageFormat) {
                out.write(level, LogEntry.Type.MESSAGE, String.format("%-" + out.keyWidth() + "s %s",
                        key, String.format(messageFormat, args)));
            } else {
                out.write(level, LogEntry.Type.MESSAGE, String.format("%-" + out.keyWidth() + "s", key));
            }
        }
    }

    @Override
    public void handled(String key, java.lang.Throwable t) {
        handled(defaultHandledLevel, key, t);
    }

    @Override
    public void handled(Level level, String key, Throwable t) {
        handled(level, key, t);
    }

    @Override
    public void handled(String key, Throwable t, @Nullable String messageFormat, Object ... args) {
        handled(key, t, messageFormat, args);
    }

    @Override
    public void handled(Level level, String key, Throwable t, @Nullable String messageFormat, Object ... args) {
        if (out.isWrite(level, LogEntry.Type.HANDLED)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            String s = sw.toString();

            String prefix = String.format("%-" + out.keyWidth() + "s ", key);
            // append the prefix to each line
            out.write(level, LogEntry.Type.HANDLED, prefix + s.replace("\n", "\n" + prefix));
        }
    }

    @Override
    public void unhandled(String key, Throwable t) {
        unhandled(defaultUnhandledLevel, key, t);
    }

    @Override
    public void unhandled(Level level, String key, Throwable t) {
        unhandled(level, key, t);
    }

    @Override
    public void unhandled(String key, Throwable t, @Nullable String messageFormat, Object ... args) {
        unhandled(key, t, messageFormat, args);
    }

    @Override
    public void unhandled(Level level, String key, Throwable t, @Nullable String messageFormat, Object ... args) {
        if (out.isWrite(level, LogEntry.Type.UNHANDLED)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            String s = sw.toString();

            String prefix = String.format("%-" + out.keyWidth() + "s ", key);
            // append the prefix to each line
            out.write(level, LogEntry.Type.UNHANDLED, prefix + s.replace("\n", "\n" + prefix));
        }
    }

}
