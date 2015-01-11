package io.nextop.client;


// DurableTransport ->
//   MultiTransport ->
//     MultiTransport ->
//       NextopTransport for WiFi
//       NextopTransport for Cell
//     HttpTransport


// for nextop, session id is given by the server
// nextop hello sends the client id
// (can have multiple transports per session, sharing the same state)


public class ClientDemo {


    public void test1() {

        TestWireFactory nWifiWireFactory = new TestWireFactory();
        TestWireFactory nCellWireFactory = new TestWireFactory();
        TestWireFactory hWifiWireFactory = new TestWireFactory();
        TestWireFactory hCellWireFactory = new TestWireFactory();


        // incremental transfer state, etc
        NextopNode.State nextopState = new NextopNode.State(/* sqlite connection provider */);
        // TODO state stores client ID


        SubjectNode s = new SubjectNode(
                new CacheNode(
                        new DurableNode(
                                new MultiNode(
                                        new MultiNode.WeightedDownstream(new NextopNode(nWifiWireFactory, nextopState)),
                                        new MultiNode.WeightedDownstream(new NextopNode(nCellWireFactory, nextopState)),
                                        new MultiNode.WeightedDownstream(new HttpNode(hWifiWireFactory)),
                                        new MultiNode.WeightedDownstream(new HttpNode(hCellWireFactory))
                                )
                        )
                )
        );

        MessageControlState mcs = new MessageControlState();

        MessageContext context = new MessageContext(mcs);
        s.init(context);
        s.start();

        // FIXME wifiWireFactory and cellWireFactory are test wrappers
        // FIXME set a message and regulate the traffic, then watch the callbacks to see what happens

//        s.send();



    }
}
