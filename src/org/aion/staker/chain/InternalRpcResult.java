package org.aion.staker.chain;

import java.util.concurrent.TimeUnit;

/**
 * An internal rpc result.
 *
 * Note that this is similar to the InternalRpcResult class in <a href="https://github.com/aionnetwork/node_test_harness"></a>,
 * copied here temporary to remove the cyclic dependency. It will be replaced later by the extracted rpc library.
 */
public final class InternalRpcResult {
    public final boolean success;
    public final String output;
    public final String error;
    private final long timeOfCallInNanos;

    private InternalRpcResult(boolean success, String output, String error, long timeOfCall, TimeUnit unit) {
        this.success = success;
        this.output = output;
        this.error = error;
        this.timeOfCallInNanos = (timeOfCall < 0) ? timeOfCall : unit.toNanos(timeOfCall);
    }

    public static InternalRpcResult successful(String output, long timeOfCall, TimeUnit unit) {
        if (output == null) {
            throw new NullPointerException("Cannot construct successful internal rpc result with null output.");
        }
        if (timeOfCall < 0) {
            throw new IllegalArgumentException("Cannot construct successful internal rpc result with negative time.");
        }
        if (unit == null) {
            throw new NullPointerException("Cannot construct successful internal rpc result with null time units.");
        }

        return new InternalRpcResult(true, output, null, timeOfCall, unit);
    }

    public static InternalRpcResult unsuccessful(String error) {
        if (error == null) {
            throw new NullPointerException("Cannot construct unsuccessful internal rpc result with null error.");
        }

        return new InternalRpcResult(false, null, error, -1, null);
    }

    @Override
    public String toString() {
        if (this.success) {
            return "InternalRpcResult { successful | output = " + this.output
                    + " | time of call = " + this.timeOfCallInNanos + " (nanos) }";
        } else {
            return "InternalRpcResult { unsuccessful due to: " + this.error + " }";
        }
    }
}
