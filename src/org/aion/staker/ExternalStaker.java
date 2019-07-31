package org.aion.staker;

import org.aion.avm.core.dappreading.JarBuilder;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.userlib.AionMap;
import org.aion.avm.userlib.abi.ABIDecoder;
import org.aion.avm.userlib.abi.ABIEncoder;
import org.aion.avm.userlib.abi.ABIException;
import org.aion.harness.kernel.Address;
import org.aion.harness.kernel.PrivateKey;
import org.aion.harness.kernel.RawTransaction;
import org.aion.harness.main.RPC;
import org.aion.harness.main.types.ReceiptHash;
import org.aion.harness.main.types.TransactionReceipt;
import org.aion.harness.main.unity.Participant;
import org.aion.harness.result.TransactionResult;
import org.aion.util.conversions.Hex;

import java.math.BigInteger;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.sleep;

public class ExternalStaker {
    
    private static final long ENERGY_LIMIT_CREATE = 4_233_567L;
    private static final long ENERGY_LIMIT_CALL = 1_233_567L;
    private static final long ENERGY_PRICE = 10_010_020_345L;

    private static PrivateKey preminedAccount;
    private static RPC rpc;
    private static Address stakingRegistry;
    private byte[] bestBlockSeed = new byte[0];
    
    public static void main(String[] args) throws InvalidKeySpecException, InterruptedException, TimeoutException {
        
        if (args.length < 1) {
            System.err.println("Usage: MAIN <preminedAccountPrivateKey> <stakingRegistryAddress:0> <ip:127.0.0.1> <port:8545>");
            return;
        }
        
        preminedAccount = PrivateKey.fromBytes(Hex.decode(args[0]));

        String ip = "127.0.0.1", port = "8545";

        if (args.length > 2) {
            ip = args[2];
        }

        if (args.length > 3) {
            port = args[3];
        }

        rpc = new RPC(ip, port);

        if (args.length > 1) {
            if (!args[1].equals("0")) {
                stakingRegistry = new Address(Hex.decode(args[1]));
            }
        }

        if (null == stakingRegistry) {
            stakingRegistry = deployStakingContract().getAddressOfDeployedContract().get();
        }
        
        List<Participant> participants = new ArrayList<>();

        System.out.println("ready for the first input");
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("ready for the next input");
            String[] tokens = scanner.nextLine().split(" ");
            
            switch(tokens[0]) {
                case "createParticipant":
                    Participant participant = new Participant(stakingRegistry, rpc);
                    participants.add(participant);
                    System.out.println("Created participant with ID "
                            + (participants.size() - 1)
                            + " and address "
                            + participant.getParticipantAddress());
                    if (fundParticipant(participant).transactionWasSuccessful()) {
                        System.out.println("Successfully transferred funds to participant " + (participants.size() - 1));
                    }
                    break;
                case "register":
                    if (tokens.length < 2) {
                        System.err.println("Usage: register <participantId>");
                    } else {
                        int participantId = Integer.parseInt(tokens[1]);
                        if (participants.size() > participantId) {
                            if (participants.get(participantId).register().transactionWasSuccessful()) {
                                System.out.println("Successfully registered participant " + participantId + " as a staker");
                            } else {
                                System.out.println("Failed to register participant " + participantId + " as a staker");
                            }
                        } else {
                            System.err.println("No participant found for this ID " + participantId);
                        }
                    }
                    break;
                case "vote":
                    if (tokens.length < 4) {
                        System.err.println("Usage: vote <senderId> <recipientId> <amount>");
                    } else {
                        int senderId = Integer.parseInt(tokens[1]);
                        int recipientId = Integer.parseInt(tokens[2]);
                        BigInteger amount = new BigInteger(tokens[3]);
                        
                        if (participants.size() > Integer.max(senderId, recipientId)) {

                            Address recipientAddress = participants.get(recipientId).getParticipantAddress();

                            if (participants.get(senderId).vote(recipientAddress, amount).transactionWasSuccessful()) {
                                System.out.println("Successfully voted!");
                            } else {
                                System.out.println("Failed to vote!");
                            }
                        } else {
                            System.err.println("No participant found for this ID " + Integer.max(senderId, recipientId));
                        }
                    }
                    break;
                case "unvote":
                    if (tokens.length < 4) {
                        System.err.println("Usage: unvote <senderId> <recipientId> <amount>");
                    } else {
                        int senderId = Integer.parseInt(tokens[1]);
                        int recipientId = Integer.parseInt(tokens[2]);
                        long amount = Long.parseLong(tokens[3]);
                        
                        if (participants.size() > Integer.max(senderId, recipientId)) {

                            Address recipientAddress = participants.get(recipientId).getParticipantAddress();

                            if (participants.get(senderId).unvote(recipientAddress, amount).transactionWasSuccessful()) {
                                System.out.println("Successfully voted!");
                            } else {
                                System.out.println("Failed to vote!");
                            }
                        } else {
                            System.err.println("No participant found for this ID " + Integer.max(senderId, recipientId));
                        }
                    }
                    break;
                case "getVote":
                    if (tokens.length < 2) {
                        System.err.println("Usage: getVote <participantId>");
                    } else {
                        int participantId = Integer.parseInt(tokens[1]);
                        System.out.println("Participant "
                                + participantId
                                + " has "
                                + participants.get(participantId).getVote()
                                + " 1 total votes");
                    }
                    break;
                case "getSeed":
                    
                    System.out.println("Updated next block seed to ");
                    break;
                case "exit":
                    return;
                default:
                    System.err.println("First token must be in {createParticipant, register, vote, unvote, getVote}");
                    break;
            }
        }
    }

    private static TransactionReceipt fundParticipant(Participant participant) throws InterruptedException {
        TransactionResult result = RawTransaction.buildAndSignGeneralTransaction(
                preminedAccount,
                rpc.getNonce(preminedAccount.getAddress()).getResult(),
                participant.getParticipantAddress(),
                new byte[0],
                ENERGY_LIMIT_CALL,
                ENERGY_PRICE,
                BigInteger.valueOf(100_000_000_000_000_000L));

        ReceiptHash hash = rpc.sendTransaction(result.getTransaction()).getResult();
        return waitOnTransactionReceipt(hash);
    }
    
    private static TransactionReceipt deployStakingContract() throws InterruptedException, TimeoutException {
        TransactionResult result = RawTransaction.buildAndSignAvmCreateTransaction(
                preminedAccount,
                rpc.getNonce(preminedAccount.getAddress()).getResult(),
                getStakingContractBytes(),
                ENERGY_LIMIT_CREATE,
                ENERGY_PRICE,
                BigInteger.ZERO);


        ReceiptHash hash = rpc.sendTransaction(result.getTransaction()).getResult();
        return waitOnTransactionReceipt(hash);
    }
    
    private static TransactionReceipt waitOnTransactionReceipt(ReceiptHash hash) throws InterruptedException {
        TransactionReceipt receipt = rpc.getTransactionReceipt(hash).getResult();
        while (receipt == null) {
            sleep(1000);
            receipt = rpc.getTransactionReceipt(hash).getResult();
            System.out.println("Waiting for receipt...");
        }
        return receipt;
    }
    
    private static byte[] getStakingContractBytes() {
        return new CodeAndArguments(JarBuilder.buildJarForMainAndClasses(StakingRegistry.class,
                ABIDecoder.class, ABIEncoder.class, ABIException.class, AionMap.class), new byte[0]).encodeToBytes();
    }
}
