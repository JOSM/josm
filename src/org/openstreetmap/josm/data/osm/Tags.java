// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * Class representing multiple values of a given key.
 * @since 15376
 */
public class Tags implements Serializable {

    private static final long serialVersionUID = 1;

    private final String key;
    private final Set<String> values;

    /**
     * Constructs a new {@code Tags}.
     * @param key the key. Must not be null
     * @param values the values. Must not be null
     */
    public Tags(String key, Set<String> values) {
        this.key = Objects.requireNonNull(key);
        this.values = Objects.requireNonNull(values);
    }

    /**
     * Returns the key.
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the values.
     * @return the values
     */
    public Set<String> getValues() {
        return values;
    }
}
