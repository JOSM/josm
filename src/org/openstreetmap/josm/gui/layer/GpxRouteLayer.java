// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Action;
import javax.swing.Icon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.Line;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.gpx.GpxDrawHelper;
import org.openstreetmap.josm.tools.Utils;

/**
 * A layer that displays {@linkplain org.openstreetmap.josm.data.gpx.GpxData#routes} from a GPX file.
 */
public class GpxRouteLayer extends AbstractModifiableLayer {

    public final GpxLayer fromLayer;
    private final Collection<GpxRoute> routes = new ArrayList<>();

    public GpxRouteLayer() {
        this(null, null);
    }

    public GpxRouteLayer(String name, GpxLayer fromLayer) {
        super(name);
        this.fromLayer = fromLayer;
        if (fromLayer != null) {
            this.routes.addAll(fromLayer.data.routes);
        }
    }

    @Override
    public boolean isModified() {
        return fromLayer.isModified();
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getToolTipText() {
        final String tooltip = trn("{0} route, ", "{0} routes, ", routes.size(), routes.size());
        return Utils.strip(tooltip, " ,");
    }

    @Override
    public void mergeFrom(Layer from) {
        if (from instanceof GpxRouteLayer) {
            routes.addAll(((GpxRouteLayer) from).routes);
        }
    }

    @Override
    public boolean isMergable(Layer other) {
        return other instanceof GpxRouteLayer;
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        fromLayer.visitBoundingBox(v);
    }

    @Override
    public Object getInfoComponent() {
        return null;
    }

    @Override
    public Action[] getMenuEntries() {
        return new Action[0];
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        // unused - we use a painter so this is not called.
    }

    @Override
    protected LayerPainter createMapViewPainter(MapViewEvent event) {
        return new GpxDrawHelper(fromLayer) {
            @Override
            protected Iterable<Line> getLinesIterable(boolean[] trackVisibility) {
                return () -> routes.stream().map(Line::new).iterator();
            }
        };
    }
}
