// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.Bounds;

public class DataSource implements Cloneable {
    public final Bounds bounds;
    public final String origin;

    public DataSource(Bounds bounds, String origin) {
        this.bounds = bounds;
        this.origin = origin;
        if (bounds == null) {
            throw new NullPointerException();
        }
    }

    @Override protected Object clone() throws CloneNotSupportedException {
        return new DataSource(bounds, origin);
    }
}
