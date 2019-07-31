# External Staker

This is a very simple external staker designed to be used with Aion's Unity Hybrid Consensus Protocol. The staker can be used to interact with a Unity-enabled node over RPC.

The staker is largely built on top of the RPC functionality provided by the [Node Test Harness](https://github.com/aionnetwork/node_test_harness "Node Test Harness").

# Launching the staker

The staker can be used directly from the terminal by launching

`java -jar external_staker.jar <args>`

The staker takes the following arguments when launched:

1. **(required)** the 32-bit private key of a rich Aion account without `0x`. 

e.g. "cc76648ce8798bc18130bc9d637995e5c42a922ebeab78795fac58081b9cf9d4"

2. *(optional)* the address where the [Staking Registry contract](https://github.com/aionnetwork/protocol_contracts/blob/master/StakeRegistry/src/main/java/org/aion/StakingRegistry.java "Staking Registry") is deployed. If this argument isn't provided, OR if the value provided is `0`, the staker will deploy the contract itself. Note that this might not be the latest version of the Staking Contract.

3. *(optional)*) the IP address of a Unity-enabled node accepting RPC requests. If this argument isn't provided, `127.0.0.1` is used as default.

4. *(optional)*) the port on which this node is accepting RPC requests. If this argument isn't provided, `8545` is used as default.

# Using the staker

The staker runs an endless loop reading commands from standard input. It takes the following arguments:

1. `createParticipant`: This creates a new `Participant`, which is an account that can be registered with the Staking Registry. This command also funds the account from the rich account provided in step 1 of the launch process.

The staker assigns an ID to the participant, which is printed to standard output.

2. `register <participantId`>: This registers the given `participantId` as a staker by calling the `register()` method in the Staking Registry contract.

3. `vote <senderId> <recipientId> <amount>`: This transfers `amount` from `senderId`'s as stake for `recipientId` by calling the `vote()` method in the Staking Registry contract.

4.  `unvote <senderId> <recipientId> <amount>`: This unvotes `amount` from `recipientId`'s stake, returning the balance to `senderId` by calling the `unvote()` method in the Staking Registry contract.

5. `getVote <participantId>`: This returns the current total stake of `participantId` by calling the `getVote()` method in the Staking Registry contract.

6. `exit`: Quits the program.
