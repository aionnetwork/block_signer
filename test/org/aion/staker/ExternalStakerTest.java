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
    public void testExternalStakerNullInput() throws InterruptedException, InvalidKeySpecException {
        ExternalStaker.main(null);
    }

    @Test
    public void testExternalStakerWith0xInput() throws InterruptedException, InvalidKeySpecException {
        String[] args = new String[]{"0xb5d819f4f63f40bb52e09e35c0f09267ccbb8b250f2f09cf62013c00b9b01480ea3b1f6b8612427dd23d68bd69ef89534bb182d924f6fc5e7483979d038c6883"};
        ExternalStaker.main(args);
    }
}
