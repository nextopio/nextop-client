package io.nextop.util;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;

public class NextopExecutors {

    /** serially executes jobs on one of the threads in the delegate */
    public static Executor serialExecutor(Executor delegate) {
        return new SerialExecutor(delegate);
    }



    private static final class SerialExecutor implements Executor {
        private final Executor delegate;
        private final Queue<Runnable> queue = new LinkedList<Runnable>();

        private final Object mutex = new Object();
        @Nullable
        private Runnable active = null;


        SerialExecutor(Executor delegate) {
            this.delegate = delegate;
        }


        @Override
        public void execute(Runnable command) {
            @Nullable Runnable pending;
            synchronized (mutex) {
                queue.add(command);
                if (null == active) {
                    active = new Reactor();
                    pending = active;
                } else {
                    pending = null;
                }
            }
            if (null != pending) {
                delegate.execute(pending);
            }
        }

        private final class Reactor implements Runnable {
            @Override
            public void run() {
                @Nullable Runnable r;
                do {
                    synchronized (mutex) {
                        r = queue.poll();
                        if (null == r) {
                            active = null;
                        }
                    }
                    if (null != r) {
                        r.run();
                    }
                } while (null != r);
            }
        };
    }



}
