package io.nextop.client.node.log;

import io.nextop.Id;
import io.nextop.Message;
import io.nextop.client.node.Head;
import io.nextop.log.Log;
import io.nextop.log.LogEntry;
import io.nextop.log.Outs;
import rx.Observer;
import rx.Subscription;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public final class RemoteOut implements Log.Out {
    private final Head head;

    private final Object mutex = new Object();
    private final Map<String, LogState> mostRecentMessages = new HashMap<String, LogState>(32);


    public RemoteOut(Head head) {
        this.head = head;
    }


    /////// Log.Out ///////

    @Override
    public boolean isWrite(Level level, LogEntry.Type type) {
        return false;
    }

    @Override
    public int lineWidth() {
        return Outs.DEFAULT_LINE_WIDTH;
    }

    @Override
    public int keyWidth() {
        return Outs.DEFAULT_KEY_WIDTH;
    }

    @Override
    public int valueWidth() {
        return Outs.DEFAULT_VALUE_WIDTH;
    }

    @Override
    public int unitWidth() {
        return Outs.DEFAULT_UNIT_WIDTH;
    }

    @Override
    public void write(Level level, LogEntry.Type type, String ... lines) {
        // Do nothing
    }

    @Override
    public boolean isWriteUp(Level level, LogEntry.Type type) {
        return true;
    }

    /** thread-safe */
    @Override
    public void writeUp(final LogEntry entry) {
        // writes to a key replace the previous pending write on that key
        // this is essential to avoid a backlog while using V_PASSIVE_HOLD_FOR_ACTIVE_RADIO

        final Message logMessage = Message.newBuilder()
                .setRoute(Message.logRoute())
                .setContent(LogEntry.toWireValue(entry))
                .set(Message.H_PASSIVE, Message.V_PASSIVE_HOLD_FOR_ACTIVE_RADIO)
                .build();

        Subscription s = head.receive(logMessage.inboxRoute()).subscribe(new Observer<Message>() {
            @Override
            public void onNext(Message message) {
                // Do nothing
            }

            @Override
            public void onCompleted() {
                endSelf();
            }

            @Override
            public void onError(Throwable e) {
                endSelf();
            }

            private void endSelf() {
                synchronized (mutex) {
                    @Nullable LogState state = mostRecentMessages.get(entry.key);
                    if (null != state) {
                        if (logMessage.id.equals(state.id)) {
                            // remove self
                            mostRecentMessages.remove(entry.key);
                            state.subscription.unsubscribe();
                        }
                    }
                }
            }
        });

        LogState state = new LogState(logMessage.id, s);

        synchronized (mutex) {
            @Nullable LogState previousState = mostRecentMessages.put(entry.key, state);
            if (null != previousState) {
                previousState.subscription.unsubscribe();
                head.cancelSend(previousState.id);
            }
            head.send(logMessage);
        }
    }


    private static final class LogState {
        final Id id;
        final Subscription subscription;

        LogState(Id id, Subscription subscription) {
            this.id = id;
            this.subscription = subscription;
        }
    }
}
