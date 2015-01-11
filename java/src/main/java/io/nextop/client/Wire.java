package io.nextop.client;

import java.io.IOException;
import java.util.concurrent.Future;

// blocking interface. each method blocks
// calling close should interrupt blocked methods
// must be closed after any exception
interface Wire {
    // return the termination future
    Future<IOException> open() throws IOException;
    // messageBoundary indicates the read is up to a message boundary
    // this helps testing count messages, e.g. pass one message, fail at the nth message, etc
    void send(byte[] buffer, int offset, int n, boolean messageBoundary) throws IOException;
    void read(byte[] buffer, int offset, int n, boolean messageBoundary) throws IOException;


    interface Factory {
        Wire create();
    }
}

// FIXME can this be implemented on top of Netty?
// FIXME would like to use the client on both sides
// FIXME yes, it seems like this could readily be adapted to a push io
