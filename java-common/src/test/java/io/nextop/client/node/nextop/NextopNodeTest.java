package io.nextop.client.node.nextop;

import io.nextop.Id;
import io.nextop.Message;
import io.nextop.client.MessageContext;
import io.nextop.client.MessageContexts;
import io.nextop.client.MessageControlState;
import io.nextop.wire.WireFactoryPair;
import io.nextop.client.node.Head;
import io.nextop.rx.MoreSchedulers;
import junit.framework.TestCase;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
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


        Random r = new Random();

        // FIXME more thorough stream
        // FIXME list of send messages for each. then just verify send==received
        // FIXME random messages: from a fixed number of groups, set different priorities
        int n = 1000;
        RandomMessageGenerator rmg = new RandomMessageGenerator(r,
                new RandomMessageGenerator.GroupDist(Id.create(), 0, 4),
                new RandomMessageGenerator.GroupDist(Id.create(), 0, 10),
                new RandomMessageGenerator.GroupDist(Id.create(), 10, 20),
                new RandomMessageGenerator.GroupDist(Id.create(), 4, 10));

        final List<Message> aSend = new LinkedList<Message>();
        final List<Message> bSend = new LinkedList<Message>();
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


            for (int i = 0; i < n; ++i) {
                Message message = rmg.next();
                aSend.add(message);
                aHead.send(message);
            }
            for (int i = 0; i < n; ++i) {
                Message message = rmg.next();
                bSend.add(message);
                bHead.send(message);
            }
        }

        void check() throws Exception {
            assertEquals(aSend.size(), bReceive.size());
            assertEquals(bSend.size(), aReceive.size());

            assertEquals(new HashSet<Message>(aSend), new HashSet<Message>(bReceive));
            assertEquals(new HashSet<Message>(bSend), new HashSet<Message>(aReceive));
        }
    }



    static final class RandomMessageGenerator {
        final Random r;
        final GroupDist[] groupDists;


        RandomMessageGenerator(Random r, GroupDist ... groupDists) {
            this.r = r;
            this.groupDists = groupDists;
        }


        Message next() {
            GroupDist groupDist = groupDists[r.nextInt(groupDists.length)];

            int groupPriority = groupDist.minPriority + r.nextInt(groupDist.maxPriority - groupDist.minPriority);

            Message.Builder builder = Message.newBuilder()
                    .setGroupId(groupDist.groupId)
                    .setGroupPriority(groupPriority);

            // FIXME set more properties here
            builder.setRoute("GET http://nextop.io");

            return builder.build();
        }



        static final class GroupDist {
            final Id groupId;
            final int minPriority;
            final int maxPriority;

            GroupDist(Id groupId, int minPriority, int maxPriority) {
                this.groupId = groupId;
                this.minPriority = minPriority;
                this.maxPriority = maxPriority;
            }
        }
    }




    // FIXME tests around sync, reconnect, disconnect behavior
    // FIXME test around never losing a message


}
