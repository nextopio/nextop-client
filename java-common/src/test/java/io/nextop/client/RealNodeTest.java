package io.nextop.client;

import io.nextop.*;
import io.nextop.client.node.Head;
import io.nextop.client.node.MultiNode;
import io.nextop.client.node.http.HttpNode;
import io.nextop.client.node.nextop.NextopClientWireFactory;
import io.nextop.client.node.nextop.NextopNode;
import io.nextop.client.test.WorkloadRunner;
import io.nextop.rx.MoreSchedulers;
import junit.framework.TestCase;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// tests a real node configurations against a live endpoint
public class RealNodeTest extends TestCase {


    public void testRealProxy() throws Throwable {
        Scheduler testScheduler = MoreSchedulers.serial();

        // run the test on the correct scheduler
        RealProxyTest test = new RealProxyTest(testScheduler);
        test.start();

        test.join();
    }

    // test images via the real proxy
    static final class RealProxyTest extends WorkloadRunner {
        int n = 1000;

        final List<Message> send = new LinkedList<Message>();
        final List<Message> receive = new LinkedList<Message>();


        RealProxyTest(Scheduler scheduler) {
            super(scheduler);
        }


        @Override
        protected void run() throws Exception {
            NextopNode nextopNode = new NextopNode();
            nextopNode.setWireFactory(new NextopClientWireFactory(
                    new NextopClientWireFactory.Config(Authority.valueOf(/* FIXME move to config */ "dev-dns.nextop.io"), 2)));

//            HttpNode httpNode = new HttpNode();
//
//            MultiNode multi = new MultiNode(nextopNode, httpNode);

            MessageContext context = MessageContexts.create();
            MessageControlState mcs = new MessageControlState(context);
//            final Head head = Head.create(context, mcs, multi, scheduler);
            final Head head = Head.create(context, mcs, nextopNode, scheduler);

            head.init(null);
            head.start();


            Subscription a = head.defaultReceive().subscribe(new Action1<Message>() {
                @Override
                public void call(Message message) {
                    receive.add(message);
                }
            });


            final Id lowPriorityGroupId = Id.create();
            final Id highPriorityGroupId = Id.create();

            Action0 sendOne = new Action0() {
                @Override
                public void call() {
                    Message.Builder builder  = Message.newBuilder()
                            .setRoute(Route.valueOf("GET http://s3-us-west-2.amazonaws.com/nextop-demo-flip-frames/b5bacea252864f938d851be98fdb1a3900af0ad183bf63b9a9bb321f2e063596-5090de8538ea489c94dc362f20c0cc67ea98dfc67437a990b57ab4ff7ee005d1.jpeg"));

                    Message.setLayers(builder,
                            new Message.LayerInfo(Message.LayerInfo.Quality.LOW, EncodedImage.Format.JPEG, 32, 0,
                                    highPriorityGroupId, 10),
                            new Message.LayerInfo(Message.LayerInfo.Quality.HIGH, EncodedImage.Format.JPEG, 0, 0,
                                    lowPriorityGroupId, 0)
                    );

                    Message message = builder.build();

                    // FIXME
//                    String uriString = message.toUriString();

                    head.send(message);

                    send.add(message);
                }
            };


            // send one then give the cache time to fill
            // TODO won't have to do this with the in-flight module in place!
            sendOne.call();
            for (int i = 0; i < n; ++i) {
                scheduler.createWorker().schedule(sendOne, 4000, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        protected void check() throws Exception {
            assertEquals(2 * send.size(), receive.size());
            // FIXME test content
        }
    }





}
