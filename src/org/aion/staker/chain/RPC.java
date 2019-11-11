package org.aion.staker.chain;

import com.google.gson.JsonParser;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * A class that facilitates communication with the node via the kernel's RPC server.
 */
public class RPC {
    private final RpcCaller rpc;

    public RPC(String ip, String port) {
        this.rpc = new RpcCaller(ip, port);
    }

    /**
     * Perform the special RPC method <code>getseed</code> (synchronous).
     *
     * @return the bytes returned by the <code>getseed</code>
     */
    public byte[] getSeed() throws InterruptedException {
        String rpcResult = sendCall(RpcMethod.GET_SEED, "");
        return (rpcResult != null) ? decodeHex(rpcResult) : null;
    }

    /**
     * Perform the special RPC method <code>submitseed</code> (synchronous).
     *
     * @return the bytes returned by the <code>submitseed</code>, which should be the so-called "mining hash" field of a block template.
     */
    public byte[] submitSeed(byte[] seed, byte[] publicKey, byte[] coinbase) throws InterruptedException {
        String seedString = "\"0x" + Hex.encodeHexString(seed) + '\"';
        String publicKeyString = "\"0x" + Hex.encodeHexString(publicKey) + '\"';
        String coinbaseString = "\"0x" + Hex.encodeHexString(coinbase) + '\"';
        String rpcResult = sendCall(RpcMethod.SUBMIT_SEED, seedString + ", " + publicKeyString + ", " + coinbaseString);
        return (rpcResult != null) ? decodeHex(rpcResult) : null;
    }

    /**
     * Perform the special RPC method <code>submitSignature</code> (synchronous).
     *
     * @return the submit result (true/false) returned by the <code>submitSignature</code>
     */
    public String submitSignature(byte[] signature, byte[] hash) throws InterruptedException {
        String signatureString = "\"0x" + Hex.encodeHexString(signature) + '\"';
        String hashString = "\"0x" + Hex.encodeHexString(hash) + '\"';
        return sendCall(RpcMethod.SUBMIT_SIGNATURE, signatureString + ", " + hashString);
    }

    /**
     * Perform <code>eth_call</code> RPC method (synchronous).
     *
     * @param tx transaction to call
     * @return the bytes returned by the <code>eth_call</code>
     */
    public byte[] call(Transaction tx) throws InterruptedException {
        String params = tx.jsonString() + ",\"latest\"";
        String rpcResult = sendCall(RpcMethod.CALL, params);
        return (rpcResult != null) ? decodeHex(rpcResult) : null;
    }

    private String sendCall(RpcMethod method, String params) throws InterruptedException {
        String payload = RpcPayload.generatePayload(method, params);

        System.out.println(method.getMethod() + " payload: " + payload);
        InternalRpcResult response = rpc.call(payload, false);
        System.out.println(method.getMethod() + " response: " + response.toString());

        return (response.output != null) ?
                new JsonParser().parse(response.output).getAsJsonObject().get("result").getAsString() : null;
    }

    private byte[] decodeHex(String rpcResult) {
        try {
            String resultHex = rpcResult.replace("0x", "");
            return Hex.decodeHex(resultHex);
        } catch (DecoderException dx) {
            throw new IllegalStateException("Could not decode the rpcResult " + rpcResult);
        }
    }
}
