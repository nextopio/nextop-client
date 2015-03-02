package io.nextop.client.node.nextop;

import io.nextop.Message;
import io.nextop.client.MessageContext;
import io.nextop.client.MessageContexts;
import io.nextop.client.MessageControlState;
import io.nextop.client.WireFactoryPair;
import io.nextop.client.node.Head;
import io.nextop.rx.MoreSchedulers;
import junit.framework.TestCase;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// connects two nextop nodes in memory
public class NextopNodeTest extends TestCase {


    public void testRandomStreaming() throws Exception {
        Scheduler testScheduler = MoreSchedulers.serial();

        // run the test on the correct scheduler
        RandomStreamingTest test = new RandomStreamingTest(testScheduler);
        testScheduler.createWorker().schedule(test);

        test.join();
    }
    static final class RandomStreamingTest implements Action0 {
        final Scheduler scheduler;

        @Nullable
        volatile Exception e = null;
        final Semaphore end = new Semaphore(0);


        // FIXME more thorough stream
        // FIXME list of send messages for each. then just verify send==received
        int n = 1000;
        final List<Message> aReceive = new LinkedList<Message>();
        final List<Message> bReceive = new LinkedList<Message>();




        RandomStreamingTest(Scheduler scheduler) {
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
                }, 5, TimeUnit.SECONDS);
            } catch (Exception e) {
                end(e);
            }
        }

        private void run() throws Exception {
            NextopNode a = new NextopNode();

            NextopNode b = new NextopNode();


            WireFactoryPair wfp = new WireFactoryPair();
            a.setWireFactory(wfp.getA());
            b.setWireFactory(wfp.getB());




            MessageContext aContext = MessageContexts.create(MoreSchedulers.serial());
            MessageControlState aMcs = new MessageControlState(aContext);

            MessageContext bContext = MessageContexts.create(MoreSchedulers.serial());
            MessageControlState bMcs = new MessageControlState(bContext);

            Head aHead = Head.create(aContext, aMcs, a, scheduler);
            Head bHead = Head.create(bContext, bMcs, b, scheduler);


            aHead.init(null);
            bHead.init(null);

            aHead.onActive(true);
            bHead.onActive(true);


            Subscription da = aHead.defaultReceive().subscribe(new Action1<Message>() {
                @Override
                public void call(Message message) {
                    aReceive.add(message);
                }
            });
            Subscription db = bHead.defaultReceive().subscribe(new Action1<Message>() {
                @Override
                public void call(Message message) {
                    bReceive.add(message);
                }
            });



            // FIXME send messages with priorities, and measure when each receives the message


            for (int i = 0; i < n; ++i) {
                aHead.send(Message.newBuilder().setRoute("GET http://nextop.io").build());
            }
            for (int i = 0; i < n; ++i) {
                bHead.send(Message.newBuilder().setRoute("GET http://nextop.io").build());
            }




        }

        void check() throws Exception {

            assertEquals(n, bReceive.size());
            assertEquals(n, aReceive.size());

        }
    }


    // FIXME tests around sync, reconnect, disconnect behavior
    // FIXME test around never losing a message


}
