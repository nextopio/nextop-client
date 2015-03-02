package io.nextop;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.NoSuchElementException;

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
//    Future<IOException> open() throws IOException;
    void close() throws IOException;

    void read(byte[] buffer, int offset, int length, int messageBoundary) throws IOException;
    void skip(long n, int messageBoundary) throws IOException;

    // messageBoundary indicates the read is up to a message boundary
    // this helps testing count messages, e.g. pass one message, fail at the nth message, etc
    void write(byte[] buffer, int offset, int length, int messageBoundary) throws IOException;
    void flush() throws IOException;


    /** thread-safe */
    interface Factory {
        // this can block until the wire is available
        // @param indicates a wire that failed, to be replaced. the wire factory can use this to infleunce load balancing, etc
        Wire create(@Nullable Wire replace /* FIXME take flags for the replacement reason, e.g. error or known endpoint shutdown, etc */) throws InterruptedException, NoSuchElementException;
    }
    /** thread-safe */
    interface Adapter {
        // this can block until the wire is available
        Wire adapt(Wire wire) throws InterruptedException, NoSuchElementException;
    }
}

// FIXME can this be implemented on top of Netty?
// FIXME would like to use the client on both sides
// FIXME yes, it seems like this could readily be adapted to a push io
