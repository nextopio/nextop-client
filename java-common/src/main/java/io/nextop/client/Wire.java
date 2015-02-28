package io.nextop.client;

import io.nextop.Id;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;

// the foundation of
// - data shaping
// - connecting client to server in the same memory space ("pipe wire"). useful for testing
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


    /** thread-safe */
    interface Factory {
        // this can block until the wire is available
        // @param indicates a wire that failed, to be replaced. the wire factory can use this to infleunce load balancing, etc
        Wire create(@Nullable Wire replace) throws NoSuchElementException;
    }
    /** thread-safe */
    interface Adapter {
        // this can block until the wire is available
        Wire adapt(Wire wire) throws NoSuchElementException;
    }
}

// FIXME can this be implemented on top of Netty?
// FIXME would like to use the client on both sides
// FIXME yes, it seems like this could readily be adapted to a push io
