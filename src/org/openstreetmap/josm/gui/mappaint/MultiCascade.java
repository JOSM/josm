// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.HashMap;

/**
 * Several cascades, e.g. one for the main Line and one for each overlay.
 * The range is (0,Inf) at first and it shrinks in the process when
 * StyleSources apply zoom level dependent properties.
 */
public class MultiCascade extends HashMap<String, Cascade> {
    public Range range;

    public MultiCascade() {
        super();
        range = new Range();
    }

    public Cascade getCascade(String key) {
        if (key == null)
            throw new IllegalArgumentException();
        Cascade c = get(key);
        if (c == null) {
            c = new Cascade(!key.equals("default"));
            put(key, c);
        }
        return c;
    }

}
