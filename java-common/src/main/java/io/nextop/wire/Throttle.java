package io.nextop.wire;

import io.nextop.Wire;
import io.nextop.log.NL;
import rx.Scheduler;
import rx.functions.Action0;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/** thread-safe */
// TODO apply shape profiles
public class Throttle implements Wire.Adapter {

    private final Object mutex = new Object();

    private final Scheduler scheduler;
    private final Scheduler.Worker worker;

    private List<Wire> active;
    private boolean online = true;


    public Throttle(Scheduler scheduler) {
        this.scheduler = scheduler;
        worker = scheduler.createWorker();

        active = new ArrayList<Wire>(4);
    }


    public void online() {
        synchronized (mutex) {
            online = true;
        }
    }

    // closes all active wires
    public void offline() {
        @Nullable List<Wire> drop;
        synchronized (mutex) {
            if (online) {
                online = false;
                drop = active;
                active = new ArrayList<Wire>(4);
            } else {
                drop = null;
            }
        }
        if (null != drop) {
            for (Wire wire : drop) {
                try {
                    wire.close();
                } catch (IOException e) {
                    NL.nl.handled("wire.throttle", e);
                }
            }
        }
    }


    final Action0 ONLINE = new Action0() {
        @Override
        public void call() {
            online();
        }
    };

    final Action0 OFFLINE = new Action0() {
        @Override
        public void call() {
            offline();
        }
    };

    public void online(int timeout, TimeUnit timeUnit) {
        if (timeout <= 0) {
            online();
        } else {
            worker.schedule(ONLINE, timeout, timeUnit);
        }
    }

    public void offline(int timeout, TimeUnit timeUnit) {
        if (timeout <= 0) {
            offline();
        } else {
            worker.schedule(OFFLINE, timeout, timeUnit);
        }
    }


    /** thread-safe */
    private void addActive(Wire wire) {
        synchronized (mutex) {
            active.add(wire);
        }
    }

    /** thread-safe */
    private void removeActive(Wire wire) {
        synchronized (mutex) {
            active.remove(wire);
        }
    }


    /////// Wire.Adapter ///////

    @Override
    public Wire adapt(Wire wire) throws InterruptedException, NoSuchElementException {
        ThrottledWire tw = new ThrottledWire(wire);
        addActive(tw);
        return tw;
    }


    final class ThrottledWire implements Wire {
        final Wire impl;


        ThrottledWire(Wire impl) {
            this.impl = impl;
        }

        void destroy() {
            removeActive(this);
        }


        @Override
        public void close() throws IOException {
            try {
                impl.close();
            } finally {
                destroy();
            }
        }
        @Override
        public void read(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            try {
                impl.read(buffer, offset, length, messageBoundary);
            } finally {
                destroy();
            }
        }
        @Override
        public void skip(long n, int messageBoundary) throws IOException {
            try {
                impl.skip(n, messageBoundary);
            } finally {
                destroy();
            }
        }
        @Override
        public void write(byte[] buffer, int offset, int length, int messageBoundary) throws IOException {
            try {
                impl.write(buffer, offset, length, messageBoundary);
            } finally {
                destroy();
            }
        }
        @Override
        public void flush() throws IOException {
            try {
                impl.flush();
            } finally {
                destroy();
            }
        }
    }
}
