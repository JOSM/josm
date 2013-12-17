// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONStringer;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class GeoJSONWriter implements Visitor {

    private OsmDataLayer layer;
    private JSONStringer out;
    private static final boolean skipEmptyNodes = true;

    public GeoJSONWriter(OsmDataLayer layer) {
        this.layer = layer;
    }

    public String write() {
        out = new JSONStringer();
        out.object().key("type").value("FeatureCollection");
        out.key("features").array();
        for (Node n : layer.data.getNodes()) {
            appendPrimitive(n);
        }
        for (Way w : layer.data.getWays()) {
            appendPrimitive(w);
        }
        out.endArray().endObject();
        return out.toString();
    }

    @Override
    public void visit(Node n) {
        out.key("type").value("Point").key("coordinates");
        appendCoord(n.getCoor());
    }

    @Override
    public void visit(Way w) {
        out.key("type").value("LineString").key("coordinates").array();
        for (Node n : w.getNodes()) {
            appendCoord(n.getCoor());
        }
        out.endArray();
    }

    @Override
    public void visit(Relation e) {
    }

    @Override
    public void visit(Changeset cs) {
    }

    protected String escape(String s) {
        return s.replace("\"", "\\\"").replace("\\", "\\\\").replace("\n", "\\n");
    }

    protected void appendPrimitive(OsmPrimitive p) {
        if (p.isIncomplete()) {
            return;
        } else if (skipEmptyNodes && p instanceof Node && p.getKeys().isEmpty()) {
            return;
        }
        out.object().key("type").value("Feature");
        Map<String, String> tags = p.getKeys();
        out.key("properties").object();
        for (Entry<String, String> t : tags.entrySet()) {
            out.key(t.getKey()).value(t.getValue());
        }
        out.endObject();
        // append primitive specific
        out.key("geometry").object();
        p.accept(this);
        out.endObject();
        out.endObject();
    }

    protected void appendCoord(LatLon c) {
        if (c != null) {
            out.array().value(c.lon()).value(c.lat()).endArray();
        }
    }
}
