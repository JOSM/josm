// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Several layers / cascades, e.g. one for the main Line and one for each overlay.
 * The range is (0,Infinity) at first and it shrinks in the process when
 * StyleSources apply zoom level dependent properties.
 */
public class MultiCascade implements StyleKeys {

    private Map<String, Cascade> layers;
    public Range range;

    public MultiCascade() {
        layers = new HashMap<String, Cascade>();
        range = Range.ZERO_TO_INFINITY;
    }

    /**
     * Return the cascade with the given name. If it doesn't exist, create
     * a new layer with that name and return it. The new layer will be
     * a clone of the "*" layer, if it exists.
     */
    public Cascade getOrCreateCascade(String layer) {
        CheckParameterUtil.ensureParameterNotNull(layer);
        Cascade c = layers.get(layer);
        if (c == null) {
            if (layers.containsKey("*")) {
                c = layers.get("*").clone();
            } else {
                c = new Cascade();
                // Everything that is not on the default layer is assumed to
                // be a modifier. Can be overridden in style definition.
                if (!layer.equals("default") && !layer.equals("*")) {
                    c.put(MODIFIER, true);
                }
            }
            layers.put(layer, c);
        }
        return c;
    }

    /**
     * Read-only version of getOrCreateCascade. For convenience, it returns an
     * empty cascade for non-existing layers. However this empty (read-only) cascade
     * is not added to this MultiCascade object.
     */
    public Cascade getCascade(String layer) {
        if (layer == null) {
            layer = "default";
        }
        Cascade c = layers.get(layer);
        if (c == null) {
            c = new Cascade();
            if (!layer.equals("default") && !layer.equals("*")) {
                c.put(MODIFIER, true);
            }
        }
        return c;
    }

    public Collection<Entry<String, Cascade>> getLayers() {
        return layers.entrySet();
    }

    public boolean hasLayer(String layer) {
        return layers.containsKey(layer);
    }
}
