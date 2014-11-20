package io.nextop.client;

import android.support.annotation.Nullable;
import rx.functions.Func1;

public interface SimulatedConnectionContext extends ConnectionContext {


    void onConnectivityEvent(ConnectivityEvent e);
    public void onConditionEvent(ConditionEvent e);

    /** takes precedent over the automatic conditioner controlled by {@link #onConditionEvent}
     * @param c <code>null</code> to disable */
    public void setConditioner(@Nullable TransportConditioner c);


    public static interface TransportConditioner extends Func1<TcpSegment, TcpSegmentDelivery> {
    }


    public static enum Link {
        WIFI,
        CELL
    }

    public static final class TcpSegment {
        Link link;
        int sizeBytes;
    }
    public static final class TcpSegmentDelivery {
        boolean delivered;

        int ackTimeoutMs;
        int retryCount;
    }


    public static final class ConnectivityEvent {
        Link link;
        boolean connected;
    }

    public static final class ConditionEvent {
        Link link;
        float downBytesPerSecond;
        float downPacketLoss;
        float upBytesPerSecond;
        float upPacketLoss;
    }
}
