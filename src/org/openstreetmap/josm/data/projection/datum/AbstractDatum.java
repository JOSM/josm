// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * Abstract base class for {@link Datum} implementations.
 *
 * Adds common fields and access methods.
 * @since 4285
 */
public abstract class AbstractDatum implements Datum {

    protected String name;
    protected String proj4Id;
    protected Ellipsoid ellps;

    /**
     * Constructs a new {@code AbstractDatum}.
     * @param name The name
     * @param proj4Id The Proj4 identifier
     * @param ellps The ellipsoid
     */
    protected AbstractDatum(String name, String proj4Id, Ellipsoid ellps) {
        this.name = name;
        this.proj4Id = proj4Id;
        this.ellps = ellps;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getProj4Id() {
        return proj4Id;
    }

    @Override
    public Ellipsoid getEllipsoid() {
        return ellps;
    }
}
