package org.aion.staker;

import main.MessageSigner;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.avm.userlib.abi.ABIStreamingEncoder;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.Transaction;
import org.aion.harness.main.RPC;
import org.aion.util.conversions.Hex;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class BlockSigner {

    private static PrivateKey stakerPrivateKey;
    private static RPC rpc;
    private static Address coinbase;
    
    private static String stakerRegistryAddress = "a056337bb14e818f3f53e13ab0d93b6539aa570cba91ce65c716058241989be9";
    
    public static void main(String[] args) throws InvalidKeySpecException, InterruptedException {
        
        if (args.length < 2) {
            System.err.println("Usage: <signingAddressPrivateKey> <identityAddress> <ip:127.0.0.1> <port:8545>");
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

        if (args.length > 2) {
            ip = args[2];
        }

        if (args.length > 3) {
            port = args[3];
        }

        rpc = new RPC(ip, port);

        String identityString = args[1];

        if (null == identityString) {
            throw new IllegalArgumentException("null identity address provided");
        }
        
        if (identityString.startsWith("0x")) {
            identityString = identityString.substring(2);
        }

        Address identityAddress = new Address(Hex.decode(identityString));
        
        System.out.println("Retrieving coinbase from the Staker Registry at " + stakerRegistryAddress + "...");
        byte[] callData = new ABIStreamingEncoder()
                .encodeOneString("getCoinbaseAddress")
                .encodeOneAddress(new avm.Address(identityAddress.getAddressBytes()))
                .toBytes();

        Transaction callTx = new Transaction(new Address(Hex.decode(stakerRegistryAddress)), callData);
        byte[] returnValue = rpc.call(callTx);
        coinbase = new Address(new ABIDecoder(returnValue).decodeOneAddress().toByteArray());
        System.out.println("Using coinbase " + coinbase);

        
        System.out.println("Producing blocks now..");
        
        while(true) {
            try {
                createAndSendStakingBlock();
            } catch (Exception e) {
                System.out.println("Block Signer caught exception " + e.getMessage());
                System.out.println("Sleeping for 5 seconds, then resuming normal behaviour");
                Thread.sleep(4000);
            }
            
            Thread.sleep(1000);
        }
        
    }
    // Returns the seed of the newly created block
    public static byte[] createAndSendStakingBlock() throws InterruptedException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] nextSeed = getNextSeed();
        byte[] blockHashToSign = rpc.submitSeed(nextSeed, stakerPrivateKey.getPublicKeyBytes(), coinbase);
        byte[] signature = MessageSigner.signMessageFromKeyBytes(stakerPrivateKey.getPrivateKeyBytes(), blockHashToSign);
        rpc.submitSignature(signature, blockHashToSign);
        return nextSeed;
    }

    private static byte[] getNextSeed() throws InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] oldSeed = rpc.getSeed();
        return MessageSigner.signMessageFromKeyBytes(stakerPrivateKey.getPrivateKeyBytes(), oldSeed);
    }
}
