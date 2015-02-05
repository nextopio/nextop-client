package io.nextop.test;

import android.test.InstrumentationTestCase;
import io.nextop.Nextop;
import io.nextop.NextopAndroid;
import junit.framework.TestCase;

public class NextopTest extends InstrumentationTestCase {

    Nextop nextop;


    @Override
    protected void setUp() throws Exception {
        super.setUp();

        nextop = Nextop.create(getInstrumentation().getContext()).start();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        nextop = nextop.stop();
    }


    public void testRandomSendReceive() {
        assertNotNull(nextop);
        assertNotNull(nextop.getAuth());


        // FIXME
        // 1. for all methods
        // 2. for all response types
        // 3. send a request with the intended response type
        // 4. assert that it is received correctly

    }


}
