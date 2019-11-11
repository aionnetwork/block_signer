package org.aion.staker.chain;

import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * A class responsible for calling an RPC endpoint using the provided payload.
 *
 * Note that this is similar to the RpcCaller class in <a href="https://github.com/aionnetwork/node_test_harness"></a>,
 * copied here temporary to remove the cyclic dependency. It will be replaced later by the extracted rpc library.
 */
public final class RpcCaller {
    private final String ip;
    private final String port;

    public RpcCaller(String ip, String port) {
        if (ip == null) {
            throw new NullPointerException("IP cannot be null");
        }

        if (port == null) {
            throw new NullPointerException("Port cannot be null");
        }

        this.ip = ip;
        this.port = port;
    }

    /**
     * Returns an internal rpc result to the caller.
     * <p>
     * The returned result is unsuccessful if either the attempt to send the RPC call failed or
     * if the kernel itself responded back with an explicit error message.
     * <p>
     * Otherwise, the returned result is successful.
     * <p>
     * This does mean that the kernel may still return a 'null' result here when we return success.
     * Though 'null' is usually interpreted as an error, we leave that up to the caller, since this
     * is not always the case.
     * <p>
     * A successful result will contain the raw response of the server. It will still need to be
     * parsed.
     */
    public InternalRpcResult call(String payload, boolean verbose) throws InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command("curl", "-X", "POST", "-H", "Content-type: application/json", "--data", payload, this.ip + ":" + this.port);

        if (verbose) {
            processBuilder.inheritIO();
        }

        try {
            long timeOfCallInNanos = System.nanoTime();
            Process rpcProcess = processBuilder.start();

            int status = rpcProcess.waitFor();
            StringBuilder stringBuilder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(rpcProcess.getInputStream()))) {
                String line = reader.readLine();

                while (line != null) {
                    stringBuilder.append(line);
                    line = reader.readLine();
                }
            }

            String output = stringBuilder.toString();

            if (output.isEmpty()) {
                return InternalRpcResult.unsuccessful("unknown error");
            }


            final JsonStringParser outputParser;
            try {
                outputParser = new JsonStringParser(output);
            } catch (JsonSyntaxException mje) {
                throw new RuntimeException("Error parsing json: " + output);
            }

            // This is only successful if the RPC Process exited successfully, and the RPC output
            // contained no 'error' content and it does contain 'result' content.

            if ((status == 0) && (!outputParser.hasAttribute("error"))) {
                return InternalRpcResult.successful(output, timeOfCallInNanos, TimeUnit.NANOSECONDS);
            } else {
                String error = outputParser.attributeToString("error");

                // We expect the content of 'error' to itself be a Json String. If it has no content
                // then the error is unknown.
                if (error == null) {
                    return InternalRpcResult.unsuccessful(getCurlError(status));
                } else {
                    JsonStringParser errorParser = new JsonStringParser(error);

                    // The 'data' attribute should capture the error.
                    error = errorParser.attributeToString("data");

                    // If there was no data value then try to grab the less informative 'message'.
                    error = (error == null) ? errorParser.attributeToString("message") : error;

                    // If there was no message this is probably a curl error.
                    error = (error == null) ? getCurlError(status) : error;

                    return InternalRpcResult.unsuccessful(error);
                }
            }

        } catch (IOException e) {
            return InternalRpcResult.unsuccessful(e.toString());
        }
    }

    /**
     * Returns descriptive strings for curl errors. We only cover the errors that we deem possible
     * here and that we can give better answers for.
     */
    private String getCurlError(int status) {
        switch (status) {
            case 7:
                return "Failed to connect to the host, check your IP and port are correct: " + this.ip + ":" + this.port;
            case 8:
                return "The server replied with data that curl was unable to parse.";
            case 9:
                return "The server denied login or the particular resource you wanted to reach.";
            case 15:
                return "Couldn't resolve the specified IP: " + this.ip;
            case 23:
                return "Failed to write the data to the server.";
            case 26:
                return "Failed to read the data from the server.";
            case 27:
                return "Out of memory error";
            case 28:
                return "Timed out";
            default:
                return "unknown error; curl exit code: " + status;
        }
    }
}
