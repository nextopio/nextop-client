package io.nextop.client.node.nextop;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.nextop.Id;
import io.nextop.Message;
import io.nextop.client.MessageContext;
import io.nextop.client.MessageContexts;
import io.nextop.client.MessageControlState;
import io.nextop.client.test.WorkloadRunner;
import io.nextop.wire.Pipe;
import io.nextop.client.node.Head;
import io.nextop.rx.MoreSchedulers;
import junit.framework.TestCase;
import rx.Notification;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

import javax.annotation.Nullable;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// connects two nextop nodes in memory
public class NextopNodeTest extends TestCase {


    public void testRandomStreaming() throws Throwable {
        Scheduler testScheduler = MoreSchedulers.serial();

        // run the test on the correct scheduler
        RandomStreamingTest test = new RandomStreamingTest(testScheduler);
        test.start();

        test.join();
    }
    static final class RandomStreamingTest extends WorkloadRunner {
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
            super(scheduler);
        }


        @Override
        protected void run() throws Exception {
            NextopNode a = new NextopNode();

            NextopNode b = new NextopNode();


            Pipe wfp = new Pipe();
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

            aHead.start();
            bHead.start();


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

        @Override
        protected void check() throws Exception {
            assertEquals(aSend.size(), bReceive.size());
            assertEquals(bSend.size(), aReceive.size());

            assertEquals(new HashSet<Message>(aSend), new HashSet<Message>(bReceive));
            assertEquals(new HashSet<Message>(bSend), new HashSet<Message>(aReceive));
        }
    }


    // test that the full set of message control gets transferred
    public void testRandomStreamingMessageControl() throws Throwable {
        Scheduler testScheduler = MoreSchedulers.serial();

        // run the test on the correct scheduler
        RandomStreamingMessageControlTest test = new RandomStreamingMessageControlTest(testScheduler);
        test.start();

        test.join();
    }
    static final class RandomStreamingMessageControlTest extends WorkloadRunner {
        Random r = new Random();

        int rm = 10;
        // ep% chance of an error on each send
        int ep = 10;
        // ep0% change of an error on the first send
        int ep0 = 30;

        int n = 1000;
        RandomMessageGenerator rmg = new RandomMessageGenerator(r);

        List<Message> aSend = new LinkedList<Message>();
        List<Message> bSend = new LinkedList<Message>();
        Multimap<Message, Response> aResponses = ArrayListMultimap.create();
        Multimap<Message, Response> aExpectedResponses = ArrayListMultimap.create();
        Multimap<Message, Response> bResponses = ArrayListMultimap.create();
        Multimap<Message, Response> bExpectedResponses = ArrayListMultimap.create();

        RandomStreamingMessageControlTest(Scheduler scheduler) {
            super(scheduler);
        }

        @Override
        protected void run() throws Exception {
            NextopNode a = new NextopNode();

            NextopNode b = new NextopNode();


            Pipe wfp = new Pipe();
            a.setWireFactory(wfp.getA());
            b.setWireFactory(wfp.getB());




            MessageContext aContext = MessageContexts.create(MoreSchedulers.serial());
            MessageControlState aMcs = new MessageControlState(aContext);

            MessageContext bContext = MessageContexts.create(MoreSchedulers.serial());
            MessageControlState bMcs = new MessageControlState(bContext);

            final Head aHead = Head.create(aContext, aMcs, a, scheduler);
            final Head bHead = Head.create(bContext, bMcs, b, scheduler);


            aHead.init(null);
            bHead.init(null);

            aHead.start();
            bHead.start();

            aHead.defaultReceive().subscribe(new DefaultReceiver(aHead, bExpectedResponses));
            bHead.defaultReceive().subscribe(new DefaultReceiver(bHead, aExpectedResponses));

            for (int i = 0; i < n; ++i) {
                Message message = rmg.next();
                aSend.add(message);
                aHead.receive(message.inboxRoute()).subscribe(new ResponseObserver(message, aResponses));
                aHead.send(message);
            }
            for (int i = 0; i < n; ++i) {
                Message message = rmg.next();
                bSend.add(message);
                bHead.receive(message.inboxRoute()).subscribe(new ResponseObserver(message, bResponses));
                bHead.send(message);
            }
        }

        @Override
        protected void check() throws Exception {
            assertEquals(n, aSend.size());
            assertEquals(n, bSend.size());

            for (Message request : aSend) {
                assertTrue(aResponses.containsKey(request));
            }
            for (Message request : bSend) {
                assertTrue(bResponses.containsKey(request));
            }

            // DEBUGGING (pinpoint the mismatches)
//            for (Message request : aSend) {
//                List<Response> a = (List<Response>) aExpectedResponses.get(request);
//                List<Response> b = (List<Response>) aResponses.get(request);
//                int n = a.size();
//                assertEquals(String.format("%s <> %s", a, b), n, b.size());
//                for (int i = 0; i < n; ++i) {
//                    Response ra = a.get(i);
//                    Response rb = b.get(i);
//                    if (!ra.equals(rb)) {
//                        fail(String.format("[%d] %s <> %s", i, ra, rb));
//                    }
//                }
//            }
//            for (Message request : aSend) {
//                List<Response> a = (List<Response>) aExpectedResponses.get(request);
//                List<Response> b = (List<Response>) aResponses.get(request);
//                int n = a.size();
//                assertEquals(String.format("%s <> %s", a, b), n, b.size());
//                for (int i = 0; i < n; ++i) {
//                    Response ra = a.get(i);
//                    Response rb = b.get(i);
//                    if (!ra.equals(rb)) {
//                        fail(String.format("[%d] %s <> %s", i, ra, rb));
//                    }
//                }
//            }

            for (Message request : aSend) {
                assertEquals(aExpectedResponses.get(request), aResponses.get(request));
            }
            for (Message request : bSend) {
                assertEquals(bExpectedResponses.get(request), bResponses.get(request));
            }

            assertEquals(aExpectedResponses, aResponses);
            assertEquals(bExpectedResponses, bResponses);
        }


        class DefaultReceiver implements Action1<Message> {
            final Head head;
            final Multimap<Message, Response> expectedResponses;

            DefaultReceiver(Head head, Multimap<Message, Response> expectedResponses) {
                this.head = head;
                this.expectedResponses = expectedResponses;
            }

            @Override
            public void call(Message message) {
                Message complete = Message.newBuilder().setRoute(message.inboxRoute()).build();
                if (r.nextInt(100) < ep0) {
                    expectedResponses.put(message, new Response(Notification.Kind.OnError, null));
                    head.error(complete);
                } else {
                    boolean error = false;
                    for (int i = 0, m = r.nextInt(2 * rm); i < m; ++i) {
                        if (r.nextInt(100) < ep) {
                            expectedResponses.put(message, new Response(Notification.Kind.OnError, null));
                            head.error(complete);
                            error = true;
                            break;
                        } else {
                            Message response = rmg.next().toBuilder().setRoute(message.inboxRoute()).build();
                            expectedResponses.put(message, new Response(Notification.Kind.OnNext, response));
                            head.send(response);
                        }
                    }
                    if (!error) {
                        expectedResponses.put(message, new Response(Notification.Kind.OnCompleted, null));
                        head.complete(complete);
                    }
                }
            }
        }


        class ResponseObserver implements Observer<Message> {
            final Message request;
            final Multimap<Message, Response> responses;


            ResponseObserver(Message request, Multimap<Message, Response> responses) {
                this.request = request;
                this.responses = responses;
            }


            @Override
            public void onNext(Message message) {
                responses.put(request, new Response(Notification.Kind.OnNext, message));
            }

            @Override
            public void onCompleted() {
                responses.put(request, new Response(Notification.Kind.OnCompleted, null));
            }

            @Override
            public void onError(Throwable e) {
                responses.put(request, new Response(Notification.Kind.OnError, null));
            }
        }

        final class Response {
            final Notification.Kind kind;
            final @Nullable Message message;

            Response(Notification.Kind kind, @Nullable Message message) {
                this.kind = kind;
                this.message = message;

                switch (kind) {
                    case OnNext:
                        if (null == message) {
                            throw new IllegalArgumentException();
                        }
                        break;
                    case OnCompleted:
                    case OnError:
                        if (null != message) {
                            throw new IllegalArgumentException();
                        }
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }

            @Override
            public String toString() {
                switch (kind) {
                    case OnNext:
                        return String.format("N %s", message);
                    case OnCompleted:
                        return "C";
                    case OnError:
                        return "E";
                    default:
                        throw new IllegalStateException();
                }
            }

            @Override
            public int hashCode() {
                int c = kind.hashCode();
                if (null != message) {
                    c = 31 * c + message.hashCode();
                }
                return c;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Response)) {
                    return false;
                }

                Response b = (Response) o;
                return kind.equals(b.kind) && Objects.equal(message, b.message);
            }
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
            Message.Builder builder = Message.newBuilder();

            if (0 < groupDists.length) {
                GroupDist groupDist = groupDists[r.nextInt(groupDists.length)];
                int groupPriority = groupDist.minPriority + r.nextInt(groupDist.maxPriority - groupDist.minPriority);

                builder
                    .setGroupId(groupDist.groupId)
                    .setGroupPriority(groupPriority);
            }

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
