// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.search;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.InArea;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * Matches objects within current map view.
 * @since 12662 (extracted from {@code SearchCompiler})
 */
class InView extends InArea {

    InView(boolean all) {
        super(all);
    }

    @Override
    protected Collection<Bounds> getBounds(OsmPrimitive primitive) {
        if (!MainApplication.isDisplayingMapView()) {
            return null;
        }
        return Collections.singleton(MainApplication.getMap().mapView.getRealBounds());
    }

    @Override
    public String toString() {
        return all ? "allinview" : "inview";
    }
}
