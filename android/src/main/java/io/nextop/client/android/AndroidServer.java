package io.nextop.client.android;


import java.io.IOException;
import java.net.UnknownHostException;

public class AndroidServer {
    String host;
    int port;
    // FIXME need inactiivy timeout per path


    // TODO get admin key

    public AndroidServer(String host, int port) {
        this.host = host;
        this.port = port;
    }


    // returns a transport that talks into the local server
    // closing the transport closes the server connection
    public NxClient.Transport connect() {
        // FIXME
        return null;
    }

    public NxClient.TransportFactory getTransportFactory() {
        return new NxClient.TransportFactory() {
            @Override
            public NxClient.Transport create(NxClient.Link link, String host, int port) throws IOException {
                if (AndroidServer.this.host.equals(host) && AndroidServer.this.port == port) {
                    return connect();
                } else {
                    throw new UnknownHostException();
                }
            }
        };
    }
}
