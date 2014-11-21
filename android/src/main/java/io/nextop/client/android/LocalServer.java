package io.nextop.client.android;


import java.io.IOException;
import java.net.UnknownHostException;

public class LocalServer {
    final String host;
    final int port;
    // FIXME need inactiivy timeout per path


    // return a public key fingerprint that gives full access to host
    public String verify(String host) {

    }

    // returns a transport that talks into the local server
    // closing the transport closes the server connection
    public Transport connect() {

    }

    public ConnectionContext.TransportFactory getTransportFactory() {
        return new ConnectionContext.TransportFactory() {
            @Override
            public Transport create(ConnectionContext.Link link, String host, int port) throws IOException {
                if (LocalServer.this.host.equals(host) && LocalServer.this.port == port) {
                    return connect();
                } else {
                    throw new UnknownHostException();
                }
            }
        };
    }
}
