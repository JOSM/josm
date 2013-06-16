// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.Map;
import java.util.Map.Entry;
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
    private StringBuilder out;
    private final boolean skipEmptyNodes = true;
    private boolean insertComma = false;

    public GeoJSONWriter(OsmDataLayer layer) {
        this.layer = layer;
    }

    public String write() {
        out = new StringBuilder(1 << 12);
        out.append("{\"type\": \"FeatureCollection\",\n");
        out.append("\"features\": [\n");
        for (Node n : layer.data.getNodes()) {
            appendPrimitive(n);
        }
        for (Way w : layer.data.getWays()) {
            appendPrimitive(w);
        }
        out.append("\n]\n}");
        return out.toString();
    }

    @Override
    public void visit(Node n) {
        out.append("\"type\": \"Point\", \"coordinates\": ");
        appendCoord(n.getCoor());
    }

    @Override
    public void visit(Way w) {
        out.append("\"type\": \"LineString\", \"coordinates\": [");
        boolean insertCommaCoords = false;
        for (Node n : w.getNodes()) {
            if (insertCommaCoords) {
                out.append(", ");
            }
            insertCommaCoords = true;
            appendCoord(n.getCoor());
        }
        out.append("]");
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
        if (insertComma) {
            out.append(",\n");
        }
        insertComma = true;
        out.append("{\"type\": \"Feature\",\n");
        Map<String, String> tags = p.getKeys();
        if (!tags.isEmpty()) {
            out.append("\t\"properties\": {\n");
            boolean insertCommaTags = false;
            for (Entry<String, String> t : tags.entrySet()) {
                if (insertCommaTags) {
                    out.append(",\n");
                }
                insertCommaTags = true;
                out.append("\t\t\"").append(escape(t.getKey())).append("\": ");
                out.append("\"").append(escape(t.getValue())).append("\"");
            }
            out.append("\n\t},\n");
        } else {
            out.append("\t\"properties\": {},\n");
        }
        { // append primitive specific
            out.append("\t\"geometry\": {");
            p.accept(this);
            out.append("}");
        }
        out.append("}");
    }

    protected void appendCoord(LatLon c) {
        if (c != null) {
            out.append("[").append(c.lon()).append(", ").append(c.lat()).append("]");
        }
    }
}
