package io.nextop.client.http;

import io.nextop.client.SubjectNode;

import java.io.IOException;

public class ReceiveIoException extends IOException {

    public ReceiveIoException(IOException cause) {
        super(cause);
    }
}
