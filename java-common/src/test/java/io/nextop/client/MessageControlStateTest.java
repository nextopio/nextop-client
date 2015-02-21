package io.nextop.client;

import io.nextop.Message;
import junit.framework.TestCase;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MessageControlStateTest extends TestCase {


    public void testEntryObserver() {

        MessageContext context = MessageContexts.executorContext();
        MessageControlState mcs = new MessageControlState(context);

        Message a = Message.newBuilder()
                .setRoute("GET http://tests.nextop.io/")
                .build();
        Message b = a.toBuilder().build();
        Message c = a.toBuilder().build();
        Message d = a.toBuilder().build();
        Message e = a.toBuilder().build();

        final int[] aCalls = {0};
        final int[] bCalls = {0};
        final int[] cCalls = {0};
        final int[] dCalls = {0};
        final int[] eCalls = {0};

        mcs.notifyPending(a.id);
        mcs.notifyPending(b.id);
        // do not notifyPending on c.id
        mcs.notifyPending(d.id);
        mcs.notifyPending(e.id);

        mcs.getObservable(a.id).subscribe(new Action1<MessageControlState.Entry>() {
              @Override
              public void call(MessageControlState.Entry entry) {
                ++aCalls[0];
              }
        });

        mcs.getObservable(b.id, 2, TimeUnit.SECONDS).subscribe(new Action1<MessageControlState.Entry>() {
            @Override
            public void call(MessageControlState.Entry entry) {
                ++bCalls[0];
            }
        });

        mcs.getObservable(c.id, 2, TimeUnit.SECONDS).subscribe(new Action1<MessageControlState.Entry>() {
            @Override
            public void call(MessageControlState.Entry entry) {
                ++cCalls[0];
            }
        });

        mcs.getObservable(d.id, 2, TimeUnit.SECONDS).subscribe(new Action1<MessageControlState.Entry>() {
            @Override
            public void call(MessageControlState.Entry entry) {
                ++dCalls[0];
            }
        });

        mcs.getObservable(e.id, 4, TimeUnit.SECONDS).subscribe(new Action1<MessageControlState.Entry>() {
            @Override
            public void call(MessageControlState.Entry entry) {
                ++eCalls[0];
            }
        });



        mcs.add(a);
        mcs.add(b);
        mcs.add(c);

        // sleep longer than the wait time for d but less than the wait time for e
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        } catch (InterruptedException t) {
            fail();
        }

        mcs.add(d);
        mcs.add(e);

        assertEquals(0, aCalls[0]);
        assertEquals(1, bCalls[0]);
        assertEquals(0, cCalls[0]);
        assertEquals(0, dCalls[0]);
        assertEquals(1, eCalls[0]);
    }



    public void testProgress() {


        MessageContext context = MessageContexts.executorContext();
        MessageControlState mcs = new MessageControlState(context);

        Message a = Message.newBuilder()
                .setRoute("GET http://tests.nextop.io/")
                .build();

        mcs.notifyPending(a.id);


        final List<MessageControlState.TransferProgress> inboxProgresses = new ArrayList<MessageControlState.TransferProgress>(4);
        final List<MessageControlState.TransferProgress> outboxProgresses = new ArrayList<MessageControlState.TransferProgress>(4);

        mcs.getObservable(a.id, 5, TimeUnit.SECONDS).map(new Func1<MessageControlState.Entry, MessageControlState.TransferProgress>() {
            @Override
            public MessageControlState.TransferProgress call(MessageControlState.Entry entry) {
                return entry.inboxTransferProgress;
            }
        }).distinctUntilChanged().subscribe(new Action1<MessageControlState.TransferProgress>() {
            @Override
            public void call(MessageControlState.TransferProgress transferProgress) {
                inboxProgresses.add(transferProgress);
            }
        });

        mcs.getObservable(a.id, 5, TimeUnit.SECONDS).map(new Func1<MessageControlState.Entry, MessageControlState.TransferProgress>() {
            @Override
            public MessageControlState.TransferProgress call(MessageControlState.Entry entry) {
                return entry.outboxTransferProgress;
            }
        }).distinctUntilChanged().subscribe(new Action1<MessageControlState.TransferProgress>() {
            @Override
            public void call(MessageControlState.TransferProgress transferProgress) {
                outboxProgresses.add(transferProgress);
            }
        });

        mcs.add(a);

        // generate progress events
        int n = 100;
        for (int i = 0; i <= n; ++i) {
            mcs.setOutboxTransferProgress(a.id, MessageControlState.TransferProgress.create(a.id, i, n));
        }
        for (int i = 0; i <= n; ++i) {
            mcs.setInboxTransferProgress(a.id, MessageControlState.TransferProgress.create(a.id, i, n));
        }

        // verify
        // the first call was empty (initial)
        assertEquals(MessageControlState.TransferProgress.none(a.id), outboxProgresses.get(0));
        assertEquals(MessageControlState.TransferProgress.none(a.id), inboxProgresses.get(0));

        for (int i = 0; i <= n; ++i) {
            assertEquals(MessageControlState.TransferProgress.create(a.id, i, n), outboxProgresses.get(i + 1));
        }
        for (int i = 0; i <= n; ++i) {
            assertEquals(MessageControlState.TransferProgress.create(a.id, i, n), inboxProgresses.get(i + 1));
        }
    }

}
