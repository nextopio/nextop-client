package io.nextop.client.android;

import android.database.Observable;

import java.io.IOException;

public interface Transport {
    void send(byte[] frame) throws IOException;
    Observable<byte[]> receive();

    void close();
}
