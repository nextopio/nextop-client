package io.nextop.log;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.logging.Level;

public final class Outs {

    public static Log.Out split(@Nullable Log.Out console, @Nullable Log.Out upstream) {
        return new SplitOut(console, upstream);
    }

    public static Log.Out mask(Log.Out out, EnumSet<LogEntry.Type> allowedConsole, EnumSet<LogEntry.Type> allowedUp) {
        return new MaskedOut(out, allowedConsole, allowedUp);
    }

    public static Log.Out sysout() {
        return new SysOut();
    }

    public static Log.Out empty() {
        return new EmptyOut();
    }



    public static final int DEFAULT_LINE_WIDTH = 90;
    public static final int DEFAULT_KEY_WIDTH = 32;
    public static final int DEFAULT_VALUE_WIDTH = 8;
    public static final int DEFAULT_UNIT_WIDTH = 6;



    private Outs() {
    }



    private static final class SplitOut implements Log.Out {
        @Nullable
        private final Log.Out console;
        @Nullable
        private final Log.Out upstream;

        public SplitOut(@Nullable Log.Out console, @Nullable Log.Out upstream) {
            this.console = console;
            this.upstream = upstream;
        }


        /////// Console Out ///////

        @Override
        public boolean isWrite(Level level, LogEntry.Type type) {
            return null != console && console.isWrite(level, type);
        }

        @Override
        public int lineWidth() {
            if (null != console) {
                return console.lineWidth();
            } else {
                return DEFAULT_LINE_WIDTH;
            }
        }

        @Override
        public int keyWidth() {
            if (null != console) {
                return console.keyWidth();
            } else {
                return DEFAULT_KEY_WIDTH;
            }
        }

        @Override
        public int valueWidth() {
            if (null != console) {
                return console.valueWidth();
            } else {
                return DEFAULT_VALUE_WIDTH;
            }
        }

        @Override
        public int unitWidth() {
            if (null != console) {
                return console.unitWidth();
            } else {
                return DEFAULT_UNIT_WIDTH;
            }
        }

        @Override
        public void write(Level level, LogEntry.Type type, String ... lines) {
            if (isWrite(level, type)) {
                console.write(level, type, lines);
            }
        }


        /////// Upstream Out ///////

        @Override
        public boolean isWriteUp(Level level, LogEntry.Type type) {
            return null != upstream && upstream.isWriteUp(level, type);
        }

        @Override
        public void writeUp(LogEntry entry) {
            if (isWriteUp(entry.level, entry.type)) {
                upstream.writeUp(entry);
            }
        }
    }

    private static final class MaskedOut implements Log.Out {
        private final Log.Out impl;
        private final EnumSet<LogEntry.Type> allowedConsole;
        private final EnumSet<LogEntry.Type> allowedUp;

        public MaskedOut(Log.Out impl, EnumSet<LogEntry.Type> allowedConsole, EnumSet<LogEntry.Type> allowedUp) {
            this.impl = impl;
            this.allowedConsole = allowedConsole;
            this.allowedUp = allowedUp;
        }


        /////// Console Out ///////


        @Override
        public boolean isWrite(Level level, LogEntry.Type type) {
            return allowedConsole.contains(type) && impl.isWrite(level, type);
        }

        @Override
        public int lineWidth() {
            return impl.lineWidth();
        }

        @Override
        public int keyWidth() {
            return impl.keyWidth();
        }

        @Override
        public int valueWidth() {
            return impl.valueWidth();
        }

        @Override
        public int unitWidth() {
            return impl.unitWidth();
        }

        @Override
        public void write(Level level, LogEntry.Type type, String ... lines) {
            if (isWrite(level, type)) {
                impl.write(level, type, lines);
            }
        }


        /////// Upstream Out ///////

        @Override
        public boolean isWriteUp(Level level, LogEntry.Type type) {
            return allowedUp.contains(type) && impl.isWriteUp(level, type);
        }

        @Override
        public void writeUp(LogEntry entry) {
            if (isWriteUp(entry.level, entry.type)) {
                impl.writeUp(entry);
            }
        }
    }

    private static final class SysOut implements Log.Out {
        SysOut() {
        }


        /////// Out ///////

        @Override
        public boolean isWrite(Level level, LogEntry.Type type) {
            return true;
        }

        @Override
        public int lineWidth() {
            return DEFAULT_LINE_WIDTH;
        }

        @Override
        public int keyWidth() {
            return DEFAULT_KEY_WIDTH;
        }

        @Override
        public int valueWidth() {
            return DEFAULT_VALUE_WIDTH;
        }

        @Override
        public int unitWidth() {
            return DEFAULT_UNIT_WIDTH;
        }

        @Override
        public void write(Level level, LogEntry.Type type, String ... lines) {
            int n = lines.length;
            if (n <= 0) {
                return;
            }
            String prefix = String.format("[%s] ", level);
            int net = 0;
            for (int i = 0; i < n; ++i) {
                net += prefix.length() + lines[i].length() + 1;
            }
            StringBuilder sb = new StringBuilder(net);
            for (String line : lines) {
                sb.append(line).append('\n');
            }
            System.out.print(sb.toString());
        }

        @Override
        public boolean isWriteUp(Level level, LogEntry.Type type) {
            return false;
        }

        @Override
        public void writeUp(LogEntry entry) {
            // Do nothing
        }
    }

    private static final class EmptyOut implements Log.Out {
        EmptyOut() {
        }


        /////// Out ///////

        @Override
        public boolean isWrite(Level level, LogEntry.Type type) {
            return false;
        }

        @Override
        public int lineWidth() {
            return DEFAULT_LINE_WIDTH;
        }

        @Override
        public int keyWidth() {
            return DEFAULT_KEY_WIDTH;
        }

        @Override
        public int valueWidth() {
            return DEFAULT_VALUE_WIDTH;
        }

        @Override
        public int unitWidth() {
            return DEFAULT_UNIT_WIDTH;
        }

        @Override
        public void write(Level level, LogEntry.Type type, String ... lines) {
            // Do nothing
        }

        @Override
        public boolean isWriteUp(Level level, LogEntry.Type type) {
            return false;
        }

        @Override
        public void writeUp(LogEntry entry) {
            // Do nothing
        }
    }
}
