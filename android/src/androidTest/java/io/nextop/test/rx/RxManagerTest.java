package io.nextop.test.rx;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import io.nextop.rx.RxManager;
import junit.framework.TestCase;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.internal.util.SubscriptionList;
import rx.subjects.BehaviorSubject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RxManagerTest extends TestCase {


    // TODO test peek, test more get/update
    // TODO test cache ejection


    public void testLifecycle() {
        final int[] callCount = {0};
        final int[] startUpdateCount = {0};
        final int[] stopUpdateCount = {0};
        final int[] closeCount = {0};

        class TestManaged extends RxManaged {
            TestManaged(Id id) {
                super(id);
            }

            @Override
            public void close() {
                ++closeCount[0];
            }
        }

        class TestManager extends RxManager<TestManaged> {
            @Override
            protected TestManaged create(Id id) {
                return new TestManaged(id);
            }

            @Override
            protected void startUpdates(TestManaged m, final RxState state) {
                ++startUpdateCount[0];
                state.sync();
            }

            @Override
            protected void stopUpdates(Id id) {
                ++stopUpdateCount[0];
            }
        }


        int n = 16;
        int d = 4;
        List<Id> ids = new ArrayList<Id>(n);
        for (int i = 0; i < n; ++i) {
            ids.add(Id.create());
        }

        TestManager manager = new TestManager();

        SubscriptionList subscriptions;

        subscriptions = new SubscriptionList();
        for (Id id : ids) {
            for (int i = 0; i < d; ++i) {
                subscriptions.add(manager.get(id).subscribe(new Action1<RxManaged>() {
                    @Override
                    public void call(RxManaged m) {
                        ++callCount[0];
                    }
                }));
            }
        }

        assertEquals(n, startUpdateCount[0]);
        assertEquals(0, stopUpdateCount[0]);
        assertEquals(d * n, callCount[0]);
        assertEquals(0, closeCount[0]);

        subscriptions.unsubscribe();
        assertEquals(n, stopUpdateCount[0]);
        // because of the cache, none should be closed
        assertEquals(0, closeCount[0]);


        // subscribe again, check that the same managed objects are used

        subscriptions = new SubscriptionList();
        for (Id id : ids) {
            for (int i = 0; i < d; ++i) {
                subscriptions.add(manager.get(id).subscribe(new Action1<RxManaged>() {
                    @Override
                    public void call(RxManaged m) {
                        ++callCount[0];
                    }
                }));
            }
        }

        assertEquals(2 * n, startUpdateCount[0]);
        assertEquals(n, stopUpdateCount[0]);
        assertEquals(2 * d * n, callCount[0]);
        assertEquals(0, closeCount[0]);

        subscriptions.unsubscribe();
        assertEquals(2 * n, stopUpdateCount[0]);
        // because of the cache, none should be closed
        assertEquals(0, closeCount[0]);

        manager.clear();

        assertEquals(n, closeCount[0]);
    }



    public void testSync() {
        // test that notifications come after sync()

        final BehaviorSubject<Boolean> syncGate = BehaviorSubject.create();
        final int[] callCount = {0};

        class TestManager extends RxManager<RxManaged> {
            @Override
            protected RxManaged create(Id id) {
                return new RxManaged(id);
            }

            @Override
            protected void startUpdates(RxManaged m, final RxState state) {
                syncGate.subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean sync) {
                        if (sync) {
                            state.sync();
                        }
                    }
                });
            }
        }


        int n = 16;
        List<Id> ids = new ArrayList<Id>(n);
        for (int i = 0; i < n; ++i) {
            ids.add(Id.create());
        }

        TestManager manager = new TestManager();

        for (Id id : ids) {
            manager.get(id).subscribe(new Action1<RxManaged>() {
                @Override
                public void call(RxManaged m) {
                    ++callCount[0];
                }
            });
        }

        assertEquals(0, callCount[0]);

        syncGate.onNext(true);

        assertEquals(n, callCount[0]);
    }


    public void testUpdateClobber() {
        // test notifications on updates
        // when updates are dispatched inside of updates

        final BehaviorSubject<Boolean> syncGate = BehaviorSubject.create();

        final int[] callCount = {0};
        final int[] updateCount = {0};

        class TestManager extends RxManager<RxManaged> {
            @Override
            protected RxManaged create(Id id) {
                return new RxManaged(id);
            }

            @Override
            protected void startUpdates(RxManaged m, final RxState state) {
                syncGate.subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean sync) {
                        if (sync) {
                            state.sync();
                        }
                    }
                });
            }

            void updateOne(Id id) {
                update(id, new Func2<RxManaged, RxState, RxManaged>() {
                    @Override
                    public RxManaged call(RxManaged m, RxState state) {
                        ++updateCount[0];
                        return null;
                    }
                });
            }
        }


        int n = 16;
        int d = 4;
        int r = 5;
        List<Id> ids = new ArrayList<Id>(n);
        for (int i = 0; i < n; ++i) {
            ids.add(Id.create());
        }

        final TestManager manager = new TestManager();

        for (final Id id : ids) {
            for (int i = 0; i < d; ++i) {
                final int[] rc = {r};
                manager.get(id).subscribe(new Action1<RxManaged>() {
                    @Override
                    public void call(RxManaged m) {
                        ++callCount[0];
                        if (0 < rc[0]) {
                            --rc[0];
                            manager.updateOne(id);
                        }
                    }
                });
            }
        }

        assertEquals(0, updateCount[0]);
        assertEquals(0, callCount[0]);

        syncGate.onNext(true);


        assertEquals(n * r * d, updateCount[0]);
        int c = 0;
        for (int i = 0; i < d; ++i) {
            // (i + 1) subscribers per id gets called 'r' times
            c += 1 + r * (i + 1);
        }
        c *= n;

        assertEquals(c, callCount[0]);
    }



}
