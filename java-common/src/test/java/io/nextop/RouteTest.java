package io.nextop;

import junit.framework.TestCase;

public class RouteTest extends TestCase {

    // some specific routes to isSend

    public void testS3_1() {
        Route route = Route.valueOf("GET http://nextop-demo-flip-frames.s3.amazonaws.com/e038321004f147cc9365ca6c4842bbae326bc5793354ad640578bf6550da078c-b9feaace75974087adaedf47f31fc633f9382778a0f2a5ebd65b757c0b9eb08d.jpeg");

        assertEquals(Route.Via.valueOf("http://nextop-demo-flip-frames.s3.amazonaws.com"), route.via);
        assertEquals(Route.Target.valueOf("GET /e038321004f147cc9365ca6c4842bbae326bc5793354ad640578bf6550da078c-b9feaace75974087adaedf47f31fc633f9382778a0f2a5ebd65b757c0b9eb08d.jpeg"), route.target);

        // FIXME dive deeper into each of via and target and isSend parts
    }


    // FIXME isSend malformed values and doing lenient parsing
}
