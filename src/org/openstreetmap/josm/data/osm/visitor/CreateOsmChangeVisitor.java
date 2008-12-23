// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.OsmWriter;

/**
 * Creates an OsmChange document from JOSM edits.
 * See http://wiki.openstreetmap.org/index.php/OsmChange for a documentation of the
 * OsmChange format.
 *
 * @author fred
 *
 */
public class CreateOsmChangeVisitor implements Visitor {

    StringBuffer document;
    String currentMode;
    Changeset changeset;
    PrintWriter writer;
    StringWriter swriter;
    OsmWriter osmwriter;

    public CreateOsmChangeVisitor(Changeset changeset) {
        writer = new PrintWriter(swriter = new StringWriter());
        writer.write("<osmChange version=\"");
        writer.write(Main.pref.get("osm-server.version", "0.6"));
        writer.write("\" generator=\"JOSM\">\n");
        this.changeset = changeset;
        osmwriter = new OsmWriter(writer, false, changeset);
    }

    public void visit(Node n) {
        if (n.deleted) {
            switchMode("delete");
            writer.write("<node id=\"");
            writer.write(Long.toString(n.id));
            writer.write("\" version=\"");
            writer.write(Long.toString(n.version));
            writer.write("\" changeset=\"");
            writer.write(Long.toString(changeset.id));
            writer.write("\" />\n");
        } else {
            switchMode((n.id == 0) ? "create" : "modify");
            n.visit(osmwriter);
        }
    }

    public void visit(Way w) {
        if (w.deleted) {
            switchMode("delete");
            writer.write("<way id=\"");
            writer.write(Long.toString(w.id));
            writer.write("\" version=\"");
            writer.write(Long.toString(w.version));
            writer.write("\" changeset=\"");
            writer.write(Long.toString(changeset.id));
            writer.write("\" />\n");
        } else {
            switchMode((w.id == 0) ? "create" : "modify");
            w.visit(osmwriter);
        }
    }

    public void visit(Relation r) {
        if (r.deleted) {
            switchMode("delete");
            writer.write("<relation id=\"");
            writer.write(Long.toString(r.id));
            writer.write("\" version=\"");
            writer.write(Long.toString(r.version));
            writer.write("\" changeset=\"");
            writer.write(Long.toString(changeset.id));
            writer.write("\" />\n");
        } else {
            switchMode((r.id == 0) ? "create" : "modify");
            r.visit(osmwriter);
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
                writer.write(Main.pref.get("osm-server.version", "0.6"));
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
