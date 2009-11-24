// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.HashMap;

/**
 * Represents the server capabilities
 *
 */
public class Capabilities {
    private  HashMap<String, HashMap<String,String>> capabilities;

    public Capabilities() {
        capabilities = new HashMap<String, HashMap<String,String>>();
    }

    public boolean isDefined(String element, String attribute) {
        if (! capabilities.containsKey(element)) return false;
        HashMap<String, String> e = capabilities.get(element);
        if (e == null) return false;
        return (e.get(attribute) != null);
    }

    public String get(String element, String attribute ) {
        if (! capabilities.containsKey(element)) return null;
        HashMap<String, String> e = capabilities.get(element);
        if (e == null) return null;
        return e.get(attribute);
    }

    /**
     * replies the value of configuration item in the capabilities as
     * double value
     *
     * @param element  the name of the element
     * @param attribute the name of the attribute
     * @return the value; null, if the respective configuration item doesn't exist
     * @throws NumberFormatException  if the value is not a valid double
     */
    public Double getDouble(String element, String attribute) throws NumberFormatException {
        String s = get(element, attribute);
        if (s == null) return null;
        return Double.parseDouble(s);
    }

    public Long getLong(String element, String attribute) {
        String s = get(element, attribute);
        if (s == null) return null;
        return Long.parseLong(s);
    }

    public void put(String element, String attribute, String value) {
        if (capabilities == null) {
            capabilities = new HashMap<String, HashMap<String,String>>();
        }
        if (! capabilities.containsKey(element))  {
            HashMap<String,String> h = new HashMap<String, String>();
            capabilities.put(element, h);
        }
        HashMap<String, String> e = capabilities.get(element);
        e.put(attribute, value);
    }

    public void clear() {
        capabilities = new HashMap<String, HashMap<String,String>>();
    }

    public boolean supportsVersion(String version) {
        return get("version", "minimum").compareTo(version) <= 0
        && get("version", "maximum").compareTo(version) >= 0;
    }

}
