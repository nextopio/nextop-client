package io.nextop.log;

import javax.annotation.Nullable;
import java.util.logging.Level;

public class SplitOut implements Log.Out {
    private final Log.Out console;
    @Nullable
    private final Log.Out upstream;

    public SplitOut(Log.Out console, @Nullable Log.Out upstream) {
        this.console = console;
        this.upstream = upstream;
    }


    /////// Console Out ///////

    @Override
    public int lineWidth() {
        return console.lineWidth();
    }

    @Override
    public int keyWidth() {
        return console.keyWidth();
    }

    @Override
    public int valueWidth() {
        return console.valueWidth();
    }

    @Override
    public int unitWidth() {
        return console.unitWidth();
    }

    @Override
    public void write(Level level, String ... lines) {
        console.write(level, lines);
    }


    /////// Upstream Out ///////

    @Override
    public boolean isWriteUp(Level level) {
        return null != upstream && upstream.isWriteUp(level);
    }

    @Override
    public void writeUp(LogEntry entry) {
        if (null == upstream) {
            throw new IllegalStateException();
        }
        upstream.writeUp(entry);
    }

}
