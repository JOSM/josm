// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmWriter;

/**
 * Creates an OsmChange document from JOSM edits.
 * See http://wiki.openstreetmap.org/index.php/OsmChange for a documentation of the
 * OsmChange format.
 *
 * @author fred
 *
 */
public class CreateOsmChangeVisitor extends AbstractVisitor {

    private String currentMode;
    private PrintWriter writer;
    private StringWriter swriter;
    private OsmWriter osmwriter;
    private OsmApi api;

    public CreateOsmChangeVisitor(Changeset changeset, OsmApi api) {
        writer = new PrintWriter(swriter = new StringWriter());
        writer.write("<osmChange version=\"");
        writer.write(api.getVersion());
        writer.write("\" generator=\"JOSM\">\n");
        this.api = api;
        // need to set osmConform = false here so that negative IDs get transmitted.
        // this also enables unnecessary and (if the API were more strict) potentially
        // harmful action="..." attributes.
        osmwriter = new OsmWriter(writer, false, api.getVersion());
        osmwriter.setChangeset(changeset);
    }

    // FIXME: This should really NOT use a visitor pattern, it looks
    // stupid. Just have one method named "write" instead of three "visit"s.

    public void visit(Node n) {
        if (n.isDeleted()) {
            switchMode("delete");
            osmwriter.setWithBody(false);
            osmwriter.visit(n);
        } else {
            switchMode((n.getId() == 0) ? "create" : "modify");
            osmwriter.setWithBody(true);
            osmwriter.visit(n);
        }
    }
    public void visit(Way w) {
        if (w.isDeleted()) {
            switchMode("delete");
            osmwriter.setWithBody(false);
            osmwriter.visit(w);
        } else {
            switchMode((w.getId() == 0) ? "create" : "modify");
            osmwriter.setWithBody(true);
            osmwriter.visit(w);
        }
    }
    public void visit(Relation r) {
        if (r.isDeleted()) {
            switchMode("delete");
            osmwriter.setWithBody(false);
            osmwriter.visit(r);
        } else {
            switchMode((r.getId() == 0) ? "create" : "modify");
            osmwriter.setWithBody(true);
            osmwriter.visit(r);
        }
    }

    private void switchMode(String newMode) {
        if ((newMode != null && !newMode.equals(currentMode))||(newMode == null && currentMode != null)) {
            if (currentMode != null) {
                writer.write("</");
                writer.write(currentMode);
                writer.write(">\n");
            }
            if (newMode != null) {
                writer.write("<");
                writer.write(newMode);
                writer.write(" version=\"");
                writer.write(api.getVersion());
                writer.write("\" generator=\"JOSM\">\n");
            }
            currentMode = newMode;
        }
    }

    public String getDocument() {
        switchMode(null);
        return swriter.toString() + "</osmChange>\n";
    }

    public Map<OsmPrimitive,Long> getNewIdMap() {
        return osmwriter.usedNewIds;
    }
}
