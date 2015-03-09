package io.nextop.log;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.logging.Level;

public final class Outs {

    public static Log.Out split(@Nullable Log.Out console, @Nullable Log.Out upstream) {
        return new SplitOut(console, upstream);
    }

    public static Log.Out upstreamMask(Log.Out out, EnumSet<LogEntry.Type> allowedTypes) {
        return new MaskedOut(out, allowedTypes);
    }


    public static final int DEFAULT_LINE_WIDTH = 320;
    public static final int DEFAULT_KEY_WIDTH = 18;
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
        private final EnumSet<LogEntry.Type> allowedUp;

        public MaskedOut(Log.Out impl, EnumSet<LogEntry.Type> allowedUp) {
            this.impl = impl;
            this.allowedUp = allowedUp;
        }


        /////// Console Out ///////


        @Override
        public boolean isWrite(Level level, LogEntry.Type type) {
            return impl.isWrite(level, type);
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
            impl.write(level, type, lines);
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
}
