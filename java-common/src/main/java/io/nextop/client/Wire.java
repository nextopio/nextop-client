package io.nextop.client;

import io.nextop.Id;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;

// blocking interface. each method blocks
// calling close should interrupt blocked methods
// must be closed after any exception
public interface Wire {
    int BOUNDARY_START = 0x01;
    int BOUNDARY_END = 0x02;

    // return the termination future
    Future<IOException> open() throws IOException;
    int read(byte[] buffer, int offset, int n, int messageBoundary) throws IOException;
    // messageBoundary indicates the read is up to a message boundary
    // this helps testing count messages, e.g. pass one message, fail at the nth message, etc
    void write(byte[] buffer, int offset, int n, int messageBoundary) throws IOException;
    void flush() throws IOException;


    interface Factory {
        // this can block until the wire is available
        Wire create() throws NoSuchElementException;

        // adapter state that can be used to adapt a sequence of wires
        // this is useful some other factory is creating the wires,
        // but this factory needs to inject itself on top,
        // and (optionally) maintain some state between each wire
        Adapter createAdapter();
    }
    interface Adapter {
        // this can block until the wire is available
        Wire adapt(Wire wire) throws NoSuchElementException;
    }
}

// FIXME can this be implemented on top of Netty?
// FIXME would like to use the client on both sides
// FIXME yes, it seems like this could readily be adapted to a push io
