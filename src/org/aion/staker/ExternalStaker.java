package org.aion.staker;

import main.MessageSigner;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.main.RPC;
import org.aion.util.conversions.Hex;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class ExternalStaker {

    private static PrivateKey stakerPrivateKey;
    private static RPC rpc;
    
    public static void main(String[] args) throws InvalidKeySpecException, InterruptedException {
        
        if (args.length < 1) {
            System.err.println("Usage: <signingAddressPrivateKey> <ip:127.0.0.1> <port:8545>");
            return;
        }
        
        String privateKeyString = args[0];
        
        if (null == privateKeyString) {
            throw new IllegalArgumentException("null signing address private key provided");
        }
        
        if (privateKeyString.startsWith("0x")) {
            privateKeyString = privateKeyString.substring(2);
        }
        
        byte[] privateKeyBytes = Hex.decode(privateKeyString);
        
        if (privateKeyBytes.length == 64) {
            privateKeyBytes = Arrays.copyOf(privateKeyBytes, 32);
        }
        
        if (privateKeyBytes.length != 32) {
            throw new IllegalArgumentException("signing address private key is of unexpected length");
        }

        stakerPrivateKey = PrivateKey.fromBytes(privateKeyBytes);

        String ip = "127.0.0.1", port = "8545";

        if (args.length > 1) {
            ip = args[1];
        }

        if (args.length > 2) {
            port = args[2];
        }

        rpc = new RPC(ip, port);
        
        System.out.println("Producing blocks now");
        
        while(true) {
            try {
                createAndSendStakingBlock();
            } catch (Exception e) {
                System.out.println("External Staker caught exception " + e.getMessage());
                System.out.println("Sleeping for 5 seconds, then resuming normal behaviour");
                Thread.sleep(4000);
            }
            
            Thread.sleep(1000);
        }
        
    }
    // Returns the seed of the newly created block
    public static byte[] createAndSendStakingBlock() throws InterruptedException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] nextSeed = getNextSeed();
        byte[] blockHashToSign = rpc.submitSeed(nextSeed, stakerPrivateKey.getPublicKeyBytes());
        byte[] signature = MessageSigner.signMessageFromKeyBytes(stakerPrivateKey.getPrivateKeyBytes(), blockHashToSign);
        rpc.submitSignature(signature, blockHashToSign);
        return nextSeed;
    }

    private static byte[] getNextSeed() throws InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] oldSeed = rpc.getSeed();
        return MessageSigner.signMessageFromKeyBytes(stakerPrivateKey.getPrivateKeyBytes(), oldSeed);
    }
}
