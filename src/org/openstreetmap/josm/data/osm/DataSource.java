// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.Bounds;

public class DataSource {
    public final Bounds bounds;
    public final String origin;

    public DataSource(Bounds bounds, String origin) {
        this.bounds = bounds;
        this.origin = origin;
        if (bounds == null)
            throw new NullPointerException();
    }
}
