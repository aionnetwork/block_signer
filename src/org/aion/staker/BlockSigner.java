package org.aion.staker;

import avm.Address;
import main.MessageSigner;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.avm.userlib.abi.ABIStreamingEncoder;
import org.aion.staker.chain.RPC;
import org.aion.staker.chain.Transaction;
import org.aion.staker.utils.PrivateKey;
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

    private static final Address STAKER_REGISTRY_AMITY_ADDRESS = new Address(Hex.decode("a056337bb14e818f3f53e13ab0d93b6539aa570cba91ce65c716058241989be9"));
    private static final Address STAKER_REGISTRY_MAINNET_ADDRESS = new Address(Hex.decode("a0733306c2ee0c60224b0e59efeae8eee558c0ca1b39e7e5a14a575124549416"));

    public static void main(String[] args) throws InvalidKeySpecException, InterruptedException {

        String signingAddressPrivateKey = null;
        String identityAddressString = null;
        String networkName = null;
        String ip = null;
        String port = null;

        if (args.length == 0 || (args.length == 1 && args[0].equals("-h"))) {
            printHelp();
        } else if (args.length == 2 && args[0].equals("-config")) {
            Config config = Config.load(args[1]);
            signingAddressPrivateKey = config.getConfigValue("signingAddressPrivateKey");
            identityAddressString = config.getConfigValue("identityAddress");
            networkName = config.getConfigValue("network");
            ip = config.getConfigValue("ip");
            port = config.getConfigValue("port");
        } else if (args.length >= 3 && args.length <= 5) {
            signingAddressPrivateKey = args[0];
            identityAddressString = args[1];
            networkName = args[2];
            // optional arguments
            if (args.length > 3) {
                ip = args[3];
            } else {
                ip = "127.0.0.1";
            }
            if (args.length > 4) {
                port = args[4];
            } else {
                port = "8545";
            }
        } else {
            printHelp();
        }

        stakerPrivateKey = getStakerPrivateKey(signingAddressPrivateKey);
        rpc = new RPC(ip, port);
        coinbase = getCoinbaseAddress(networkName, identityAddressString);

        System.out.println("Producing blocks now..");

        while(true) {
            try {
                byte[] nextSeed = getNextSeed();
                if(nextSeed != null) {
                    createAndSendStakingBlock(nextSeed);
                }
            } catch (Exception e) {
                System.out.println("Block Signer caught exception " + e.getMessage());
                System.out.println("Sleeping for 5 seconds, then resuming normal behaviour");
                Thread.sleep(4000);
            }

            Thread.sleep(1000);
        }

    }

    public static void createAndSendStakingBlock(byte[] nextSeed) throws InterruptedException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] blockHashToSign = rpc.submitSeed(nextSeed, stakerPrivateKey.getPublicKeyBytes(), coinbase.toByteArray());
        if (blockHashToSign != null) {
            byte[] signature = MessageSigner.signMessageFromKeyBytes(stakerPrivateKey.getPrivateKeyBytes(), blockHashToSign);
            String result = rpc.submitSignature(signature, blockHashToSign);
            if (result.equals("true")) {
                System.out.println("Block submitted successfully.");
            } else {
                System.out.println("This staker must wait longer to submit this block.");
            }
        } else {
            System.out.println("Could not submit the POS block. Waiting for a POW block to be generated.");
        }
    }

    private static byte[] getNextSeed() throws InterruptedException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] oldSeed = rpc.getSeed();
        byte[] seed;
        if (oldSeed != null) {
            seed = MessageSigner.signMessageFromKeyBytes(stakerPrivateKey.getPrivateKeyBytes(), oldSeed);
        } else {
            System.out.println("Unity fork has not been reached yet.");
            seed = null;
        }
        return seed;
    }

    private static PrivateKey getStakerPrivateKey(String privateKeyString) throws InvalidKeySpecException {
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
            throw new IllegalArgumentException("Signing address private key is of unexpected length");
        }

        return PrivateKey.fromBytes(privateKeyBytes);
    }

    private static Address getCoinbaseAddress(String networkName, String identityAddressString) throws InterruptedException {
        if (null == identityAddressString) {
            throw new IllegalArgumentException("null identity address provided");
        }

        if (identityAddressString.startsWith("0x")) {
            identityAddressString = identityAddressString.substring(2);
        }

        byte[] identityAddressBytes = Hex.decode(identityAddressString);

        Address stakerRegistryAddress;
        if (networkName.toLowerCase().equals("amity")) {
            stakerRegistryAddress = STAKER_REGISTRY_AMITY_ADDRESS;
        } else if (networkName.toLowerCase().equals("mainnet")) {
            stakerRegistryAddress = STAKER_REGISTRY_MAINNET_ADDRESS;
        } else {
            throw new IllegalArgumentException("Unsupported network name provided. Only amity and mainnet are supported.");
        }

        System.out.println("Retrieving the coinbase address from the StakerRegistry at " + stakerRegistryAddress + "...");

        byte[] callData = new ABIStreamingEncoder()
                .encodeOneString("getCoinbaseAddress")
                .encodeOneAddress(new avm.Address(identityAddressBytes))
                .toBytes();

        Transaction callTx = new Transaction(stakerRegistryAddress, callData);
        byte[] returnValue = rpc.call(callTx);
        Address coinbase = new Address(new ABIDecoder(returnValue).decodeOneAddress().toByteArray());
        System.out.println("Using coinbase address " + coinbase);
        return coinbase;
    }

    private static void printHelp(){
        System.out.println("Run block signer using one of the following commands:");
        System.out.println("<signingAddressPrivateKey> <identityAddress> <networkName> <ip:127.0.0.1> <port:8545>");
        System.out.println("-config <configFilePath>");
        System.exit(0);
    }
}
