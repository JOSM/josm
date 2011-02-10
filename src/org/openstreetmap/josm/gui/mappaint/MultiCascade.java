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

    /**
     * Return the cascade for the given layer key. If it does not exist,
     * return a new cascade, but do not keep it.
     */
    public Cascade getCascade(String layer) {
        if (layer == null)
            throw new IllegalArgumentException();
        Cascade c = get(layer);
        if (c == null) {
            c = new Cascade(!layer.equals("default"));
        }
        return c;
    }

}
