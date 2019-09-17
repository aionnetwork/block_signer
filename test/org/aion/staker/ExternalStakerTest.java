package org.aion.staker;

import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExternalStakerTest {

    @Test(expected = NullPointerException.class)
    public void testExternalStakerNullInput() throws InterruptedException, InvalidKeySpecException, TimeoutException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        ExternalStaker.main(null);
    }
}
