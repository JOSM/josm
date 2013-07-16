// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import org.openstreetmap.josm.data.projection.Ellipsoid;

abstract public class AbstractDatum implements Datum {

    protected String name;
    protected String proj4Id;
    protected Ellipsoid ellps;

    public AbstractDatum(String name, String proj4Id, Ellipsoid ellps) {
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
