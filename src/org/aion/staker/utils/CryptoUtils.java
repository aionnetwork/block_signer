package org.aion.staker.utils;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.Utils;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Note that this is similar to the CryptoUtils class in <a href="https://github.com/aionnetwork/node_test_harness"></a>,
 * copied here temporary to remove the cyclic dependency and will be replaced later.
 */
public class CryptoUtils {
    private static final String skEncodedPrefix = "302e020100300506032b657004220420";

    /**
     * Derive the corresponding aion address, given the private key bytes.
     */
    public static byte[] derivePublicKey(byte[] privateKeyBytes) throws InvalidKeySpecException {
        if (privateKeyBytes == null) {
            throw new NullPointerException("private key cannot be null");
        }

        if (privateKeyBytes.length != 32) {
            throw new IllegalArgumentException("private key mute be 32 bytes");
        }

        EdDSAPrivateKey privateKey = new EdDSAPrivateKey(new PKCS8EncodedKeySpec(addSkPrefix(Utils.bytesToHex(privateKeyBytes))));
        return privateKey.getAbyte();
    }


    /**
     * Add encoding prefix for importing private key
     */
    private static byte[] addSkPrefix(String skString) {
        String skEncoded = skEncodedPrefix + skString;
        return Utils.hexToBytes(skEncoded);
    }
}