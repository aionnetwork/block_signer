package org.aion.staker.chain;

import avm.Address;
import org.apache.commons.codec.binary.Hex;

/**
 * A transaction.
 *
 * @implNote Currently this is using the avm api Address type.
 */
public class Transaction {
    private final Address to;
    private final byte[] data;

    /**
     * Constructor
     */
    public Transaction(Address to, byte[] data) {
        this.to = to;
        this.data = data;
    }

    /**
     * @return <code>to</code> field of transaction
     */
    public Address getTo() {
        return to;
    }

    /**
     * @return <code>data</code> field of transaction
     */
    public byte[] getData() {
        return data;
    }

    /**
     * serialize to JSON
     *
     * @return JSON representation
     * @implNote If we end up have more classes like this (data holder objects that
     * represent structures defined in JSON), should add Jackson mapper library and use
     * that instead.
     */
    public String jsonString() {
        final String dataVal;
        if (getData() == null) {
            dataVal = "null";
        } else {
            dataVal = String.format("\"0x%s\"", Hex.encodeHexString(getData()));
        }

        return String.format("{"
                        + "\"to\" : \"0x%s\","
                        + "\"data\" : %s"
                        + "}",
                Hex.encodeHexString(getTo().toByteArray()),
                getData() != null ? dataVal : "null"
        );
    }
}
