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


import io.nextop.Message;

public class ClientDemo {


    public static void main(String[] in) {

        TestWireFactory nWifiWireFactory = new TestWireFactory();
        TestWireFactory nCellWireFactory = new TestWireFactory();
        TestWireFactory hWifiWireFactory = new TestWireFactory();
        TestWireFactory hCellWireFactory = new TestWireFactory();


        // incremental transfer state, etc
        NextopNode.State nextopState = new NextopNode.State(/* sqlite connection provider */);
        // TODO state stores client ID
        // TODO tls disabled now


        SubjectNode s = new SubjectNode(
                new CacheNode(
                        new DurableNode(
                                new MultiNode(
                                        new MultiNode.WeightedDownstream(new NextopNode(nWifiWireFactory, nextopState), 4),
                                        new MultiNode.WeightedDownstream(new NextopNode(nCellWireFactory, nextopState), 3),
                                        new MultiNode.WeightedDownstream(new HttpNode(hWifiWireFactory), 2),
                                        new MultiNode.WeightedDownstream(new HttpNode(hCellWireFactory), 1)
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


        for (TestWireFactory twf : new TestWireFactory[]{nWifiWireFactory, nCellWireFactory, hWifiWireFactory, hCellWireFactory}) {
            twf.advanceOpen();
        }


        s.send(Message.newBuilder()
                .setNurl("GET https://google.com/")
                .setBody("hello world!")
                .build());

        // this should completely send the message
        for (TestWireFactory twf : new TestWireFactory[]{nWifiWireFactory, nCellWireFactory, hWifiWireFactory, hCellWireFactory}) {
            twf.advanceReadToMessageStart();
            twf.advanceWriteToMessageStart();
        }
        nWifiWireFactory.advanceWrite(1);
        // done!
        // FIXME be able to check for an ack here



        s.send(Message.newBuilder()
                .setNurl("GET https://google.com/")
                .setBody("hello world!")
                .build());

        for (TestWireFactory twf : new TestWireFactory[]{nWifiWireFactory, nCellWireFactory, hWifiWireFactory, hCellWireFactory}) {
            twf.advanceReadToMessageStart();
            twf.advanceWriteToMessageStart();
        }
        nWifiWireFactory.failWrite();
        // the system should fail over to nextop cell until wifi comes back
        nCellWireFactory.failWrite();
        // the system should fail over to http wifi
        hWifiWireFactory.failWrite();
        // the system should fail over to http cell
        hCellWireFactory.failWrite();
        // the system should go into holding ...

        nWifiWireFactory.advanceOpen();
        // the system should try nextop wifi now ...

        nWifiWireFactory.advanceReadToMessageStart();
        nWifiWireFactory.advanceWriteToMessageStart();
        nWifiWireFactory.advanceWrite(1);
        // done!
        // FIXME be able to check for an ack here




    }
}
