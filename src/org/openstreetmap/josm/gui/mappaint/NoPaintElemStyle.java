// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

public class NoPaintElemStyle extends ElemStyle {

    public static final NoPaintElemStyle INSTANCE = new NoPaintElemStyle();

    private NoPaintElemStyle() {

    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected) {
        // Do nothing
    }

}
