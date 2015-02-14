package io.nextop.rx;

import android.util.Log;
import android.view.View;
import immutablecollections.ImSet;
import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action2;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.Subscriptions;

import javax.annotation.Nullable;
import java.util.*;

// must be called on the main thread
public final class RxDebugger {
    private static final RxDebugger global = new RxDebugger();

    public static RxDebugger get() {
        return global;
    }


    static final String TAG = "RxDebugger";


    private final Map<Subscriber<?>, Stats> statsMap;
    private ImSet<Subscriber<? super Stats>> subscribers;

    /** non-null when stepping */
    @Nullable
    private Queue<Step> stepping = null;

    private final BehaviorSubject stepStatePublish;

    private long headSubscriberId = 0;

    private boolean suppressed = false;


    RxDebugger() {
        statsMap = new LinkedHashMap<Subscriber<?>, Stats>(8);
        subscribers = ImSet.empty();

        stepStatePublish = BehaviorSubject.create(new StepState(false, 0));
    }


    public boolean isEnabled() {
        return !suppressed;
    }


    void update(final Stats stats) {
        if (suppressed) {
            assert false : "Callers must check isEnabled.";
            return;
        }
        if (!stats.active) {
            throw new IllegalArgumentException();
        }

        @Nullable Stats r = statsMap.put(stats.subscriber, stats);
        if (null != r) {
            r.active = false;
            stats.subscriberId = r.subscriberId;
        } else {
            stats.subscriberId = headSubscriberId++;
        }
        stats.nanos = System.nanoTime();

        // do this to avoid an infinite recursion where dispatching to the debugger observers
        // triggers more debug stats
        suppressed = true;
        try {
            for (Subscriber<? super Stats> subscriber : subscribers) {
                // check if the debug subscribers triggered a newer update on the stats subscriber
                // if so, do not publish old stats
                // this should probably trigger a warning (debug observer has side effects)
                if (!stats.active) {
                    break;
                }
                try {
                    subscriber.onNext(stats);
                } catch (Throwable t) {
                    // FIXME severe
                    Log.e(TAG, null, t);
                }
            }
        } finally {
            suppressed = false;
        }
        if (stats.active && stats.removed) {
            stats.active = false;
            statsMap.remove(stats.subscriber);
        }
    }


    void deliver(Subscriber subscriber, Notification notification, Action2<Subscriber, Notification> action) {
        if (null != stepping) {
            stepping.add(new Step(subscriber, notification, action));
            publishStepState();
        } else {
            action.call(subscriber, notification);
        }
    }


    // STEPPING

    public boolean step() {
        if (null != stepping) {
            @Nullable Step step = stepping.poll();
            if (null != step) {
                step.action.call(step.subscriber, step.notification);

                publishStepState();
                return true;
            }
        }
        return false;
    }

    public void setStepping(boolean s) {
        if (s) {
            if (null == stepping) {
                stepping = new LinkedList<Step>();

                publishStepState();
            }
        } else {
            if (null != stepping) {
                for (@Nullable Step step; null != (step = stepping.poll()); ) {
                    step.action.call(step.subscriber, step.notification);
                }
                assert stepping.isEmpty();
                stepping = null;

                publishStepState();
            }
        }
    }

    private void publishStepState() {
        stepStatePublish.onNext(createStepState());
    }

    private StepState createStepState() {
        if (null != stepping) {
            return new StepState(true, stepping.size());
        } else {
            return new StepState(false, 0);
        }
    }


    // INTROSPECTION

    public Observable<StepState> getStepState() {
        return stepStatePublish;
    }

    /**
     * a stream of stats, one object per subscriber.
     * Newer stats replace older stats for the same subscriber.
     */
    public Observable<Stats> getStats() {
        return Observable.create(new Observable.OnSubscribe<Stats>() {
            @Override
            public void call(final Subscriber<? super Stats> subscriber) {
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        subscribers = subscribers.removing(subscriber);
                    }
                }));

                subscribers = subscribers.adding(subscriber);
                for (Stats stats : new ArrayList<Stats>(statsMap.values())) {
                    // this should probably trigger a warning (debug observer has side effects)
                    if (stats.active) {
                        try {
                            subscriber.onNext(stats);
                        } catch (Throwable t) {
                            // FIXME severe
                            Log.e(TAG, null, t);
                        }
                    }
                }
            }
        });
    }


    public static final class Stats {
        public static final int F_NEXT = 0x01;
        public static final int F_COMPLETED = 0x02;
        public static final int F_ERROR = 0x04;
        public static final int F_FAILED = 0x08;
        public static final int F_CONNECTED = 0x10;
        public static final int F_DISCONNECTED = 0x20;



        public final int flags;

        public final Subscriber<?> subscriber;
        @Nullable
        public final View view;
        public final boolean removed;

        public final boolean connected;
        public final int onNextCount;
        public final int onCompletedCount;
        public final int onErrorCount;

        @Nullable
        public final Notification mostRecentNotification;


        // FAILED NOTIFICATIONS
        // this is where a call to oNext, onCompleted, onError failed
        public final int failedNotificationCount;
        @Nullable
        public final Notification mostRecentFailedNotification;
        @Nullable
        public final Throwable mostRecentFailedNotificationReason;


        // internally updated

        public long subscriberId;
        public long nanos;

        // internal
        boolean active = true;




        Stats(int flags, Subscriber<?> subscriber, @Nullable View view, boolean removed,
              boolean connected,
              int onNextCount, int onCompletedCount, int onErrorCount, @Nullable Notification mostRecentNotification,
              int failedNotificationCount, @Nullable Notification mostRecentFailedNotification, @Nullable Throwable mostRecentFailedNotificationReason) {
            this.flags = flags;

            this.subscriber = subscriber;
            this.view = view;
            this.removed = removed;

            this.connected = connected;
            this.onNextCount = onNextCount;
            this.onCompletedCount = onCompletedCount;
            this.onErrorCount = onErrorCount;
            this.mostRecentNotification = mostRecentNotification;

            this.failedNotificationCount = failedNotificationCount;
            this.mostRecentFailedNotification = mostRecentFailedNotification;
            this.mostRecentFailedNotificationReason = mostRecentFailedNotificationReason;
        }
    }

    public static final class StepState {
        public final boolean stepping;
        public final int stepCount;

        StepState(boolean stepping, int stepCount) {
            this.stepping = stepping;
            this.stepCount = stepCount;
        }
    }


    // internal

    private static final class Step {
        Subscriber subscriber;
        Notification notification;
        Action2<Subscriber, Notification> action;

        Step(Subscriber subscriber, Notification notification, Action2<Subscriber, Notification> action) {
            this.subscriber = subscriber;
            this.notification = notification;
            this.action = action;
        }
    }

}
