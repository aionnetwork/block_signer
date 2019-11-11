package org.aion.staker.chain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A tool for parsing Json strings.
 *
 * Note that this is similar to the JsonStringParser class in <a href="https://github.com/aionnetwork/node_test_harness"></a>,
 * copied here temporary to remove the cyclic dependency. It will be replaced later by the extracted rpc library.
 */

public final class JsonStringParser {
    private final JsonObject stringAsJson;

    public JsonStringParser(String jsonString) {
        if (jsonString == null) {
            throw new NullPointerException("Cannot construct JsonStringParser with null jsonString.");
        }
        if (jsonString.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct JsonStringParser with empty jsonString.");
        }

        this.stringAsJson = (JsonObject) new JsonParser().parse(jsonString);
    }

    /**
     * Returns {@code true} only if the json string being parsed contains the specified attribute.
     *
     * @param attribute The attribute whose existence is to be determined.
     * @return whether or not the attribute exists.
     */
    public boolean hasAttribute(String attribute) {
        return this.stringAsJson.has(attribute);
    }

    /**
     * Returns the content corresponding to the specified attribute as a string.
     * <p>
     * If the specified attribute does not exist, or if its content is the string 'null' or the
     * empty string, then this method returns null.
     * <p>
     * Otherwise, this method returns the corresponding content.
     * <p>
     * Note, the returned string will be stripped of any leading or trailing quotation marks and
     * if the '0x' hexadecimal identifier was present it will also be removed.
     *
     * @param attribute The attribute whose content is to be fetched.
     * @return the corresponding content as a string.
     */
    public String attributeToString(String attribute) {
        if (!hasAttribute(attribute)) {
            return null;
        }

        String element = cleanElement(this.stringAsJson.get(attribute).toString());

        if ((element.isEmpty()) || (element.equals("null"))) {
            return null;
        }

        return element;
    }

    /**
     * Returns rawElement with no leading or trailing quotation marks and with no '0x' hex
     * identifier.
     */
    private String cleanElement(String rawElement) {
        rawElement = (rawElement.startsWith("\"")) ? rawElement.substring(1) : rawElement;
        rawElement = (rawElement.endsWith("\"")) ? rawElement.substring(0, rawElement.length() - 1) : rawElement;
        return (rawElement.startsWith("0x")) ? rawElement.substring(2) : rawElement;
    }
}
