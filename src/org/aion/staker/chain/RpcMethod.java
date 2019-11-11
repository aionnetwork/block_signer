package org.aion.staker.chain;

/**
 * An enum that serves as a mapping between an enum type and a String. Namely, the String that
 * is to be put into the payload for an RPC call.
 *
 */
public enum RpcMethod {
    CALL("eth_call"),

    GET_SEED("getseed"),

    SUBMIT_SEED("submitseed"),

    SUBMIT_SIGNATURE("submitsignature");

    private String method;

    private RpcMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the method name corresponding to this enumeration for an RPC call.
     *
     * @return the method name.
     */
    public String getMethod() {
        return this.method;
    }

    @Override
    public String toString() {
        return "RpcMethod { " + this.method + " }";
    }

}
