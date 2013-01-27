// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data class for extensions in a GPX-File.
 */
public class Extensions extends LinkedHashMap<String, String> {

    public Extensions(Map<? extends String, ? extends String> m) {
        super(m);
    }

    public Extensions() {
    }
}
