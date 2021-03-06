package org.aion.staker;

import avm.Address;
import main.MessageSigner;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.avm.userlib.abi.ABIStreamingEncoder;
import org.aion.staker.chain.RPC;
import org.aion.staker.chain.Transaction;
import org.aion.staker.utils.Logger;
import org.aion.staker.utils.PrivateKey;
import org.aion.staker.vrf.VRF_Ed25519;
import org.aion.util.bytes.ByteUtil;
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

    private static Logger logger;

    private static final long submitSeedSleepTimeMillis = 500;
    private static final long submitSignatureSleepTimeMillis = 100;

    public static void main(String[] args) throws InvalidKeySpecException, InterruptedException {

        String signingAddressPrivateKey = null;
        String identityAddressString = null;
        String networkName = null;
        String ip = null;
        String port = null;
        boolean verboseLoggingEnabled = false;
        String coinbaseAddress = "";

        if (args.length == 0 || (args.length == 1 && args[0].equals("-h"))) {
            printHelp();
        } else if (args.length == 2 && args[0].equals("-config")) {
            Config config = Config.load(args[1]);
            signingAddressPrivateKey = config.getConfigValue("signingAddressPrivateKey");
            identityAddressString = config.getConfigValue("identityAddress");
            networkName = config.getConfigValue("network");
            ip = config.getConfigValue("ip");
            port = config.getConfigValue("port");
            verboseLoggingEnabled = Boolean.parseBoolean(config.getConfigValue("verboseLoggingEnabled"));
            coinbaseAddress = config.getConfigValue("coinbaseAddress");
        } else if (args.length >= 3 && args.length <= 6) {
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
            if (args.length == 6) {
                verboseLoggingEnabled = Boolean.parseBoolean(args[5]);
            }
        } else {
            printHelp();
        }

        logger = new Logger(verboseLoggingEnabled);
        stakerPrivateKey = getStakerPrivateKey(signingAddressPrivateKey);
        rpc = new RPC(ip, port, logger);
        if (coinbaseAddress.length() > 0) {
            coinbase = getAddressFromString(coinbaseAddress);
        } else {
            coinbase = getCoinbaseAddress(networkName, identityAddressString);
        }

        logger.log("Producing blocks now..");

        byte[] lastSubmittedHash = null;
        while(true) {
            try {
                byte[] oldSeed = rpc.getSeed();
                byte[] nextSeed = getNextSeed(oldSeed);
                if (nextSeed == null) {
                    logger.log("Could not submit POS blocks. A POW block is expected at the current block number. Sleeping for " + submitSeedSleepTimeMillis + " ms");
                } else {
                    byte[] blockHashToSign = rpc.submitSeed(nextSeed, stakerPrivateKey.getPublicKeyBytes(), coinbase.toByteArray());
                    if (blockHashToSign != null) {
                        if (!Arrays.equals(lastSubmittedHash, blockHashToSign)) {
                            byte[] signature = MessageSigner.signMessageFromKeyBytes(stakerPrivateKey.getPrivateKeyBytes(), blockHashToSign);
                            boolean submissionResult = Boolean.parseBoolean(rpc.submitSignature(signature, blockHashToSign));
                            if (submissionResult) {
                                logger.log("Block submitted successfully. BlockMineHash: " + ByteUtil.toHexString(blockHashToSign));
                                lastSubmittedHash = blockHashToSign;
                            }
                        } else {
                            logger.log("Block already submitted, blockMineHash: " + ByteUtil.toHexString(blockHashToSign));
                        }
                    } else {
                        logger.log("Could not submit the POS block. Waiting for a POW block to be generated. Sleeping for " + submitSeedSleepTimeMillis + " ms");
                    }
                }
            } catch (Exception e) {
                logger.log("Block Signer caught exception, " + Arrays.toString(e.getStackTrace()));
            }

            Thread.sleep(submitSeedSleepTimeMillis);
        }

    }

    private static byte[] getNextSeed(byte[] oldSeed) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        byte[] seed;
        int seedLength = 64;
        int proofLength = 80;
        if (oldSeed != null) {
            if (oldSeed.length == seedLength) {
                seed = MessageSigner.signMessageFromKeyBytes(stakerPrivateKey.getPrivateKeyBytes(), oldSeed);
            } else if (oldSeed.length == (seedLength + 1) && oldSeed[seedLength] == 0) {
                byte[] oldSeedHash = new byte[seedLength];
                System.arraycopy(oldSeed, 0, oldSeedHash, 0, seedLength);
                seed = VRF_Ed25519.generateProof(oldSeedHash, stakerPrivateKey.getKeyPairBytes());
            } else if (oldSeed.length == proofLength) {
                byte[] oldSeedHash = VRF_Ed25519.generateProofHash(oldSeed);
                seed = VRF_Ed25519.generateProof(oldSeedHash, stakerPrivateKey.getKeyPairBytes());
            } else {
                return null;
            }
        } else {
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

        Address stakerRegistryAddress;
        if (networkName.toLowerCase().equals("amity")) {
            stakerRegistryAddress = STAKER_REGISTRY_AMITY_ADDRESS;
        } else if (networkName.toLowerCase().equals("mainnet")) {
            stakerRegistryAddress = STAKER_REGISTRY_MAINNET_ADDRESS;
        } else {
            throw new IllegalArgumentException("Unsupported network name provided. Only amity and mainnet are supported.");
        }

        logger.log("Retrieving the coinbase address from the StakerRegistry at " + stakerRegistryAddress + "...");

        byte[] callData = new ABIStreamingEncoder()
                .encodeOneString("getCoinbaseAddress")
                .encodeOneAddress(getAddressFromString(identityAddressString))
                .toBytes();

        Transaction callTx = new Transaction(stakerRegistryAddress, callData);
        byte[] returnValue = rpc.call(callTx);
        Address coinbase = new Address(new ABIDecoder(returnValue).decodeOneAddress().toByteArray());
        logger.log("Using coinbase address " + coinbase);
        return coinbase;
    }

    private static void printHelp(){
        System.out.println("Run block signer using one of the following commands:");
        System.out.println("<signingAddressPrivateKey> <identityAddress> <networkName> <ip:127.0.0.1> <port:8545> <verboseLoggingEnabled:false>");
        System.out.println("-config <configFilePath>");
        System.exit(0);
    }

    private static Address getAddressFromString(String addressString){
        if (addressString.startsWith("0x")) {
            addressString = addressString.substring(2);
        }

        byte[] addressBytes = Hex.decode(addressString);
        return new avm.Address(addressBytes);
    }
}
