# Block Signer

This is a block signer designed to be used with Aion's Unity Hybrid Consensus Protocol. The signer can be used to interact with a Unity-enabled node over RPC.

The signer is largely built on top of the RPC functionality provided by the [Node Test Harness](https://github.com/aionnetwork/node_test_harness "Node Test Harness").

# Launching the signer

The signer can be used directly from the terminal either by passing in the arguments directly or by using a config file.


### Option 1: Command line options

**Usage:**

`java -jar block_signer.jar <signingAddressPrivateKey> <identityAddress> <networkName> <ip> <port> <verboseLoggingEnabled>`

The signer takes the following arguments when launched:

1. **(required)** the private key of the signing address of the registered staker. 

2. **(required)** the identity address of the staker. 

3. **(required)** the network name. Only `amity` and `mainnet` are supported. 

4. *(optional)* the IP address of a Unity-enabled node accepting RPC requests. If this argument isn't provided, `127.0.0.1` is used as default.

5. *(optional)* the port on which this node is accepting RPC requests. If this argument isn't provided, `8545` is used as default.

6. *(optional)* enable or disable verbose logging level. If this argument isn't provided, verbose logging is disabled by default. 
If verbose logging is enabled, rpc request and response messages are printed.

### Option 2: Config file
**Usage:**

`java -jar block_signer.jar -config <configFilePath>`

The signer takes the following argument when launched:
 - **(required)** path to the config file. 
 

---
Note that `java -jar block_signer.jar -h` will print the usage options.

# Behaviour

Once launched, the signer repeatedly queries the node over RPC, and submits signed staking blocks. Note that it aggressively sends a signed block as soon as it produces it; the nodes must reject future blocks if they want to.
