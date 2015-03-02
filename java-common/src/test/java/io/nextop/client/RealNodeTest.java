package io.nextop.client;

import io.nextop.Authority;
import io.nextop.Message;
import io.nextop.Route;
import io.nextop.client.node.Head;
import io.nextop.client.node.MultiNode;
import io.nextop.client.node.http.HttpNode;
import io.nextop.client.node.nextop.NextopClientWireFactory;
import io.nextop.client.node.nextop.NextopNode;
import io.nextop.rx.MoreSchedulers;
import junit.framework.TestCase;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// tests a real node configurations against a live endpoint
public class RealNodeTest extends TestCase {


    public void testProxy() throws Exception {
        Scheduler testScheduler = MoreSchedulers.serial();

        // run the test on the correct scheduler
        ProxyTest test = new ProxyTest(testScheduler);
        testScheduler.createWorker().schedule(test);

        test.join();
    }

    static final class ProxyTest implements Action0 {
        final Scheduler scheduler;

        @Nullable
        volatile Exception e = null;
        final Semaphore end = new Semaphore(0);

        int n = 1000;

        final List<Message> send = new LinkedList<Message>();
        final List<Message> receive = new LinkedList<Message>();


        ProxyTest(Scheduler scheduler) {
            this.scheduler = scheduler;
        }


        void join() throws Exception {
            end.acquire();
            if (null != e) {
                throw e;
            }
        }

        void end(@Nullable Exception e) {
            this.e = e;
            end.release();
        }


        @Override
        public void call() {
            try {
                run();
                scheduler.createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        try {
                            check();
                        } catch (Exception e) {
                            end(e);
                        }

                        end(null);
                    }
                }, 10, TimeUnit.SECONDS);
            } catch (Exception e) {
                end(e);
            }
        }

        private void run() throws Exception {
            NextopNode nextopNode = new NextopNode();
            nextopNode.setWireFactory(new NextopClientWireFactory(
                    new NextopClientWireFactory.Config(Authority.valueOf("54.149.233.13:2778"), 2)));

            HttpNode httpNode = new HttpNode();

            MultiNode multi = new MultiNode(nextopNode, httpNode);

            MessageContext context = MessageContexts.create();
            MessageControlState mcs = new MessageControlState(context);
            Head head = Head.create(context, mcs, multi, scheduler);


            Subscription a = head.defaultReceive().subscribe(new Action1<Message>() {
                @Override
                public void call(Message message) {
                    receive.add(message);
                }
            });

            for (int i = 0; i < n; ++i) {
                Message message = Message.newBuilder()
                        .setRoute(Route.valueOf("POST http://tests.nextop.io"))
                        .build();
                send.add(message);
                head.send(message);
            }
        }

        void check() throws Exception {
            assertEquals(send.size(), receive.size());
            // FIXME test content
        }
    }





}
