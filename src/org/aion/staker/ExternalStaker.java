package org.aion.staker;

import main.MessageSigner;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.main.RPC;
import org.aion.util.conversions.Hex;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.TimeoutException;

public class ExternalStaker {

    private static PrivateKey stakerPrivateKey;
    private static RPC rpc;
    
    public static void main(String[] args) throws InvalidKeySpecException, InterruptedException, TimeoutException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        
        if (args.length < 1) {
            System.err.println("Usage: <preminedAccountPrivateKey> <ip:127.0.0.1> <port:8545>");
            return;
        }

        stakerPrivateKey = PrivateKey.fromBytes(Hex.decode(args[0]));

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
          createAndSendStakingBlock();
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
