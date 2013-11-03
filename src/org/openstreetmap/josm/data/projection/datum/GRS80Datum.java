// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * This datum indicates, that GRS80 ellipsoid is used and no conversion
 * is necessary to get from or to the WGS84 datum.
 */
public final class GRS80Datum extends NullDatum {

    public final static GRS80Datum INSTANCE = new GRS80Datum();

    private GRS80Datum() {
        super(tr("GRS80"), Ellipsoid.GRS80);
    }
}
