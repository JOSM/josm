// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DatasetCollection;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.GBC;

/**
 * Panel to inspect one or more OsmPrimitives.
 *
 * Gives an unfiltered view of the object's internal state.
 * Might be useful for power users to give more detailed bug reports and
 * to better understand the JOSM data representation.
 *
 * TODO: show conflicts
 */
public class InspectPrimitiveDialog extends ExtendedDialog {
    protected Collection<OsmPrimitive> primitives;
    protected JTextArea textArea;

    public InspectPrimitiveDialog(Collection<OsmPrimitive> primitives) {
        super(Main.parent, tr("Advanced object info"), new String[] {"Close"});
        this.primitives = primitives;
        setPreferredSize(new Dimension(450, 350));

        setButtonIcons(new String[] {"ok.png"});
        JPanel p = buildPanel();
        textArea.setText(buildText());
        setContent(p, false);
    }

    protected JPanel buildPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", textArea.getFont().getStyle(), textArea.getFont().getSize()));
        textArea.setEditable(false);

        JScrollPane scroll = new JScrollPane(textArea);

        p.add(scroll, GBC.std().fill());
        return p;
    }

    protected String buildText() {
        StringBuffer s = new StringBuffer();
        for (Node n : new DatasetCollection<Node>(primitives, OsmPrimitive.nodePredicate)) {
            s.append("Node id="+n.getUniqueId());
            if (!checkDataSet(n)) {
                s.append(" not in data set");
                continue;
            }
            if (n.isIncomplete()) {
                s.append(" incomplete\n");
                addWayReferrer(s, n);
                addRelationReferrer(s, n);
                continue;
            }
            s.append(String.format(" lat=%s; lon=%s; ", Double.toString(n.getCoor().lat()), Double.toString(n.getCoor().lon())));
            addCommon(s, n);
            addAttributes(s, n);
            addWayReferrer(s, n);
            addRelationReferrer(s, n);
            s.append('\n');
        }

        for (Way w : new DatasetCollection<Way>(primitives, OsmPrimitive.wayPredicate)) {
            s.append("Way id="+ w.getUniqueId());
            if (!checkDataSet(w)) {
                s.append(" not in data set");
                continue;
            }
            if (w.isIncomplete()) {
                s.append(" incomplete\n");
                addRelationReferrer(s, w);
                continue;
            }
            s.append(String.format(" %d nodes; ", w.getNodes().size()));
            addCommon(s, w);
            addAttributes(s, w);
            addRelationReferrer(s, w);

            s.append("  nodes:\n");
            for (Node n : w.getNodes()) {
                s.append(String.format("    %d\n", n.getUniqueId()));
            }
            s.append('\n');
        }

        for (Relation r : new DatasetCollection<Relation>(primitives, OsmPrimitive.relationPredicate)) {
            s.append("Relation id="+r.getUniqueId());
            if (!checkDataSet(r)) {
                s.append(" not in data set");
                continue;
            }
            if (r.isIncomplete()) {
                s.append(" incomplete\n");
                addRelationReferrer(s, r);
                continue;
            }
            s.append(String.format(" %d members; ",r.getMembersCount()));
            addCommon(s, r);
            addAttributes(s, r);
            addRelationReferrer(s, r);

            s.append("  members:\n");
            for (RelationMember m : r.getMembers() ) {
                s.append(String.format("    %d %s\n", m.getMember().getUniqueId(), m.getRole()));
            }
            s.append('\n');
        }

        return s.toString().trim();
    }

    protected void addCommon(StringBuffer s, OsmPrimitive o) {
        s.append(String.format("Data set: %X; User: [%s]; ChangeSet id: %H; Timestamp: %s, Version: %d",
                    o.getDataSet().hashCode(),
                    userString(o.getUser()),
                    o.getChangesetId(),
                    DateUtils.fromDate(o.getTimestamp()),
                    o.getVersion()));

        /* selected state is left out: not interesting as it is always selected */
        if (o.isDeleted()) {
            s.append("; deleted");
        }
        if (!o.isVisible()) {
            s.append("; deleted-on-server");
        }
        if (o.isModified()) {
            s.append("; modified");
        }
        if (o.isDisabledAndHidden()) {
            s.append("; filtered/hidden");
        }
        if (o.isDisabled()) {
            s.append("; filtered/disabled");
        }
        if (o.hasDirectionKeys()) {
            s.append("; has direction keys");
            if (o.reversedDirection()) {
                s.append(" (reversed)");
            }
        }
        s.append("\n");
    }

    protected void addAttributes(StringBuffer s, OsmPrimitive o) {
        if (o.hasKeys()) {
            s.append("  tags:\n");
            for (String key: o.keySet()) {
                s.append(String.format("    \"%s\"=\"%s\"\n", key, o.get(key)));
            }
        }
    }

    protected void addWayReferrer(StringBuffer s, Node n) {
        // add way referrer
        List<OsmPrimitive> refs = n.getReferrers();
        DatasetCollection<Way> wayRefs = new DatasetCollection<Way>(refs, OsmPrimitive.wayPredicate);
        if (wayRefs.size() > 0) {
            s.append("  way referrer:\n");
            for (Way w : wayRefs) {
                s.append("    "+w.getUniqueId()+"\n");
            }
        }
    }

    protected void addRelationReferrer(StringBuffer s, OsmPrimitive o) {
        List<OsmPrimitive> refs = o.getReferrers();
        DatasetCollection<Relation> relRefs = new DatasetCollection<Relation>(refs, OsmPrimitive.relationPredicate);
        if (relRefs.size() > 0) {
            s.append("  relation referrer:\n");
            for (Relation r : relRefs) {
                s.append("    "+r.getUniqueId()+"\n");
            }
        }
    }

    /**
     * See if primitive is in a data set properly.
     * This does not hold for primitives that are new and deleted.
     */
    protected boolean checkDataSet(OsmPrimitive o) {
        DataSet ds = o.getDataSet();
        if (ds == null)
            return false;
        return ds.getPrimitiveById(o) != null;
    }

    protected String userString(User user) {
        if (user == null)
            return "<null>";

        List<String> names = user.getNames();

        StringBuffer us = new StringBuffer();

        us.append("id:"+user.getId());
        if (names.size() == 1) {
            us.append(" name:"+user.getName());
        }
        else if (names.size() > 1) {
            us.append(String.format(" %d names:%s", names.size(), user.getName()));
        }
        return us.toString();
    }
}
