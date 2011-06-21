// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SingleSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.StyleCache;
import org.openstreetmap.josm.gui.mappaint.StyleCache.StyleList;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.xml.XmlStyleSource;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.WindowGeometry;

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
    private JTextArea txtData;
    private JTextArea txtMappaint;
    boolean mappaintTabLoaded;

    public InspectPrimitiveDialog(Collection<OsmPrimitive> primitives) {
        super(Main.parent, tr("Advanced object info"), new String[] {tr("Close")});
        this.primitives = primitives;
        setRememberWindowGeometry(getClass().getName() + ".geometry",
                WindowGeometry.centerInWindow(Main.parent, new Dimension(750, 550)));

        setButtonIcons(new String[] {"ok.png"});
        final JTabbedPane tabs = new JTabbedPane();
        JPanel pData = buildDataPanel();
        tabs.addTab(tr("data"), pData);
        final JPanel pMapPaint = new JPanel();
        tabs.addTab(tr("map style"), pMapPaint);
        tabs.getModel().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (!mappaintTabLoaded && ((SingleSelectionModel) e.getSource()).getSelectedIndex() == 1) {
                    mappaintTabLoaded = true;
                    buildMapPaintPanel(pMapPaint);
                    createMapPaintText();
                }
            }
        });
        txtData.setText(buildDataText());
        setContent(tabs, false);
    }

    protected JPanel buildDataPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        txtData = new JTextArea();
        txtData.setFont(new Font("Monospaced", txtData.getFont().getStyle(), txtData.getFont().getSize()));
        txtData.setEditable(false);

        JScrollPane scroll = new JScrollPane(txtData);

        p.add(scroll, GBC.std().fill());
        return p;
    }

    protected String buildDataText() {
        StringBuilder s = new StringBuilder();
        for (Node n : new SubclassFilteredCollection<OsmPrimitive, Node>(primitives, OsmPrimitive.nodePredicate)) {
            s.append(tr("Node id={0}", n.getUniqueId()));
            if (!checkDataSet(n)) {
                s.append(tr(" not in data set"));
                continue;
            }
            if (n.isIncomplete()) {
                s.append(tr(" incomplete\n"));
                addWayReferrer(s, n);
                addRelationReferrer(s, n);
                continue;
            }
            s.append(tr(" lat={0} lon={1} (projected: x={2}, y={3}); ",
                    Double.toString(n.getCoor().lat()), Double.toString(n.getCoor().lon()),
                    Double.toString(n.getEastNorth().east()), Double.toString(n.getEastNorth().north())));
            addCommon(s, n);
            addAttributes(s, n);
            addWayReferrer(s, n);
            addRelationReferrer(s, n);
            s.append('\n');
        }

        for (Way w : new SubclassFilteredCollection<OsmPrimitive, Way>(primitives, OsmPrimitive.wayPredicate)) {
            s.append(tr("Way id={0}", w.getUniqueId()));
            if (!checkDataSet(w)) {
                s.append(tr(" not in data set"));
                continue;
            }
            if (w.isIncomplete()) {
                s.append(tr(" incomplete\n"));
                addRelationReferrer(s, w);
                continue;
            }
            s.append(tr(" {0} nodes; ", w.getNodes().size()));
            addCommon(s, w);
            addAttributes(s, w);
            addRelationReferrer(s, w);

            s.append(tr("  nodes:\n"));
            for (Node n : w.getNodes()) {
                s.append(String.format("    %d\n", n.getUniqueId()));
            }
            s.append('\n');
        }

        for (Relation r : new SubclassFilteredCollection<OsmPrimitive, Relation>(primitives, OsmPrimitive.relationPredicate)) {
            s.append(tr("Relation id={0}",r.getUniqueId()));
            if (!checkDataSet(r)) {
                s.append(tr(" not in data set"));
                continue;
            }
            if (r.isIncomplete()) {
                s.append(tr(" incomplete\n"));
                addRelationReferrer(s, r);
                continue;
            }
            s.append(tr(" {0} members; ",r.getMembersCount()));
            addCommon(s, r);
            addAttributes(s, r);
            addRelationReferrer(s, r);

            s.append(tr("  members:\n"));
            for (RelationMember m : r.getMembers() ) {
                s.append(String.format("    %s%d '%s'\n", m.getMember().getType().getAPIName().substring(0,1), m.getMember().getUniqueId(), m.getRole()));
            }
            s.append('\n');
        }

        return s.toString().trim();
    }

    protected void addCommon(StringBuilder s, OsmPrimitive o) {
        s.append(tr("Data set: {0}; User: [{1}]; ChangeSet id: {2}; Timestamp: {3}, Version: {4}",
                Integer.toHexString(o.getDataSet().hashCode()),
                userString(o.getUser()),
                o.getChangesetId(),
                o.isTimestampEmpty() ? tr("<new object>") : DateUtils.fromDate(o.getTimestamp()),
                o.getVersion()));

        /* selected state is left out: not interesting as it is always selected */
        if (o.isDeleted()) {
            s.append(tr("; deleted"));
        }
        if (!o.isVisible()) {
            s.append(tr("; deleted-on-server"));
        }
        if (o.isModified()) {
            s.append(tr("; modified"));
        }
        if (o.isDisabledAndHidden()) {
            s.append(tr("; filtered/hidden"));
        }
        if (o.isDisabled()) {
            s.append(tr("; filtered/disabled"));
        }
        if (o.hasDirectionKeys()) {
            s.append(tr("; has direction keys"));
            if (o.reversedDirection()) {
                s.append(tr(" (reversed)"));
            }
        }
        s.append("\n");
    }

    protected void addAttributes(StringBuilder s, OsmPrimitive o) {
        if (o.hasKeys()) {
            s.append(tr("  tags:\n"));
            for (String key: o.keySet()) {
                s.append(String.format("    \"%s\"=\"%s\"\n", key, o.get(key)));
            }
        }
    }

    protected void addWayReferrer(StringBuilder s, Node n) {
        // add way referrer
        List<OsmPrimitive> refs = n.getReferrers();
        Collection<Way> wayRefs = new SubclassFilteredCollection<OsmPrimitive, Way>(refs, OsmPrimitive.wayPredicate);
        if (wayRefs.size() > 0) {
            s.append(tr("  way referrer:\n"));
            for (Way w : wayRefs) {
                s.append("    "+w.getUniqueId()+"\n");
            }
        }
    }

    protected void addRelationReferrer(StringBuilder s, OsmPrimitive o) {
        List<OsmPrimitive> refs = o.getReferrers();
        Collection<Relation> relRefs = new SubclassFilteredCollection<OsmPrimitive, Relation>(refs, OsmPrimitive.relationPredicate);
        if (relRefs.size() > 0) {
            s.append(tr("  relation referrer:\n"));
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
            return tr("<new object>");

        List<String> names = user.getNames();

        StringBuilder us = new StringBuilder();

        us.append(tr("id: {0}",user.getId()));
        if (names.size() == 1) {
            us.append(tr(" name: {0}",user.getName()));
        }
        else if (names.size() > 1) {
            us.append(tr(" {0} names: {1}", names.size(), user.getName()));
        }
        return us.toString();
    }

    protected void buildMapPaintPanel(JPanel p) {
        p.setLayout(new GridBagLayout());
        txtMappaint = new JTextArea();
        txtMappaint.setFont(new Font("Monospaced", txtMappaint.getFont().getStyle(), txtMappaint.getFont().getSize()));
        txtMappaint.setEditable(false);

        p.add(new JScrollPane(txtMappaint), GBC.std().fill());
    }

    protected void createMapPaintText() {
        final Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
        ElemStyles elemstyles = MapPaintStyles.getStyles();
        NavigatableComponent nc = Main.map.mapView;
        double scale = nc.getDist100Pixel();

        for (OsmPrimitive osm : sel) {
            txtMappaint.append(tr("Styles Cache for \"{0}\":",osm.getDisplayName(DefaultNameFormatter.getInstance())));

            MultiCascade mc = new MultiCascade();

            for (StyleSource s : elemstyles.getStyleSources()) {
                if (s.active) {
                    txtMappaint.append(tr("\n\n> applying {0} style \"{1}\"\n",getSort(s), s.getDisplayString()));
                    s.apply(mc, osm, scale, null, false);
                    txtMappaint.append(tr("\nRange:{0}",mc.range));
                    for (Entry<String, Cascade> e : mc.getLayers()) {
                        txtMappaint.append("\n "+e.getKey()+": \n"+e.getValue());
                    }
                } else {
                    txtMappaint.append(tr("\n\n> skipping \"{0}\" (not active)",s.getDisplayString()));
                }
            }
            txtMappaint.append(tr("\n\nList of generated Styles:\n"));
            StyleList sl = elemstyles.get(osm, scale, nc);
            for (ElemStyle s : sl) {
                txtMappaint.append(" * "+s+"\n");
            }
            txtMappaint.append("\n\n");
        }

        if (sel.size() == 2) {
            List<OsmPrimitive> selList = new ArrayList<OsmPrimitive>(sel);
            StyleCache sc1 = selList.get(0).mappaintStyle;
            StyleCache sc2 = selList.get(1).mappaintStyle;
            if (sc1 == sc2) {
                txtMappaint.append(tr("The 2 selected objects have identical style caches."));
            }
            if (!sc1.equals(sc2)) {
                txtMappaint.append(tr("The 2 selected objects have different style caches."));
            }
            if (sc1.equals(sc2) && sc1 != sc2) {
                txtMappaint.append(tr("Warning: The 2 selected objects have equal, but not identical style caches."));
            }
        }
    }

    private String getSort(StyleSource s) {
        if (s instanceof XmlStyleSource)
            return tr("xml");
        if (s instanceof MapCSSStyleSource)
            return tr("mapcss");
        return tr("unkown");
    }

}
