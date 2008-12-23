// License: GPL.
package org.openstreetmap.josm.data.gpx;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for various classes in the GPX model.
 * The "attr" hash is used to store the XML payload
 * (not only XML attributes!)
 *
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class WithAttributes {

    public Map<String, Object> attr = new HashMap<String, Object>(0);

    public String getString(String key) {
        Object value = attr.get(key);
        return (value instanceof String) ? (String)value : null;
    }
}
