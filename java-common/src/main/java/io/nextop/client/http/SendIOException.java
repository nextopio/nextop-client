package io.nextop.client.http;

import java.io.IOException;

public class SendIOException extends IOException {
    public final boolean sentUpToTcpWindow;


    public SendIOException(IOException cause) {
        this(cause, false);
    }

    public SendIOException(IOException cause, boolean sentUpToTcpWindow) {
        super(cause);
        this.sentUpToTcpWindow = sentUpToTcpWindow;
    }
}
