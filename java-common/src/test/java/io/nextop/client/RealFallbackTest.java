package io.nextop.client;

import io.nextop.Authority;
import io.nextop.Wires;
import io.nextop.client.node.MultiNode;
import io.nextop.client.node.http.HttpNode;
import io.nextop.client.node.nextop.NextopClientWireFactory;
import io.nextop.client.node.nextop.NextopNode;
import io.nextop.wire.Probe;
import io.nextop.wire.Throttle;
import rx.schedulers.Schedulers;

// tests that a multi -> (nextop, http) falls back correctly
// when
// 1. nextop has a short recoverable error
// 2. nextop has a recoverable error with a long delay
// 3. nextop goes offline indefinitely
// uses the test endpoint and a local proxy
public class RealFallbackTest {
    // FIXME


    public void testShortRecoverableError() {

        Throttle nextopThrottle = new Throttle(Schedulers.newThread());
        Probe nextopProbe = new Probe();
        Probe httpProbe = new Probe();


        NextopNode nextopNode = new NextopNode();
        nextopNode.setWireFactory(new NextopClientWireFactory(
                new NextopClientWireFactory.Config(Authority.valueOf(/* FIXME move to config */ "dev-dns.nextop.io"), 2)));
        nextopNode.setWireAdapter(Wires.compose(nextopThrottle, nextopProbe));

        HttpNode httpNode = new HttpNode();
        httpNode.setWireAdapter(httpProbe);

        MultiNode multiNode = new MultiNode(MultiNode.Downstream.create(nextopNode),
                MultiNode.Downstream.create(httpNode));

        // FIXME

        // send a message
        // drop the nextop throttle briefly
        // send a message
        // bring it back
        // check that both get sent on nextop
        // zero sent on http


    }

    public void testRecoverableErrorWithLongDelay() {
        // FIXME

        // send a message
        // drop the nextop throttle
        // send a message
        // after a while, bring nextop back
        // check that both get sent on http
        // zero sent on nextop
    }

    public void testIrrecoverableError() {
        // FIXME

        // send a message
        // drop the nextop throttle, bring it back
        // after a while, drop nextop
        // send a message
        // check that one gets sent on http
        // one gets sent on nextop
    }

}
