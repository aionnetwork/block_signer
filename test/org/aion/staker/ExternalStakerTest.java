package org.aion.staker;

import org.junit.Test;

import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExternalStakerTest {

    @Test(expected = NullPointerException.class)
    public void testExternalStakerNullInput() throws InterruptedException, InvalidKeySpecException, TimeoutException {
        ExternalStaker.main(null);
    }

    @Test
    public void testExternalStakerWithPKey() throws InterruptedException, InvalidKeySpecException, TimeoutException {
        ExternalStaker.main(new String[] {"cc76648ce8798bc18130bc9d637995e5c42a922ebeab78795fac58081b9cf9d4"});
    }
}
