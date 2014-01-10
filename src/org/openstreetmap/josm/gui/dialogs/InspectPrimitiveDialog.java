// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SingleSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveComparator;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
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
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * Panel to inspect one or more OsmPrimitives.
 *
 * Gives an unfiltered view of the object's internal state.
 * Might be useful for power users to give more detailed bug reports and
 * to better understand the JOSM data representation.
 */
public class InspectPrimitiveDialog extends ExtendedDialog {

    protected List<OsmPrimitive> primitives;
    protected OsmDataLayer layer;
    private JosmTextArea txtMappaint;
    boolean mappaintTabLoaded;

    public InspectPrimitiveDialog(Collection<OsmPrimitive> primitives, OsmDataLayer layer) {
        super(Main.parent, tr("Advanced object info"), new String[] {tr("Close")});
        this.primitives = new ArrayList<OsmPrimitive>(primitives);
        this.layer = layer;
        setRememberWindowGeometry(getClass().getName() + ".geometry",
                WindowGeometry.centerInWindow(Main.parent, new Dimension(750, 550)));

        setButtonIcons(new String[]{"ok.png"});
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
        setContent(tabs, false);
    }

    protected JPanel buildDataPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        JosmTextArea txtData = new JosmTextArea();
        txtData.setFont(new Font("Monospaced", txtData.getFont().getStyle(), txtData.getFont().getSize()));
        txtData.setEditable(false);
        txtData.setText(buildDataText());
        txtData.setSelectionStart(0);
        txtData.setSelectionEnd(0);

        JScrollPane scroll = new JScrollPane(txtData);

        p.add(scroll, GBC.std().fill());
        return p;
    }

    protected String buildDataText() {
        DataText dt = new DataText();
        Collections.sort(primitives, new OsmPrimitiveComparator());
        for (OsmPrimitive o : primitives) {
            dt.addPrimitive(o);
        }
        return dt.toString();
    }

    class DataText {
        static final String INDENT = "  ";
        static final String NL = "\n";

        private StringBuilder s = new StringBuilder();

        private DataText add(String title, String... values) {
            s.append(INDENT).append(title);
            for (String v : values) {
                s.append(v);
            }
            s.append(NL);
            return this;
        }

        private String getNameAndId(String name, long id) {
            if (name != null) {
                return name + tr(" ({0})", /* sic to avoid thousand seperators */ Long.toString(id));
            } else {
                return Long.toString(id);
            }
        }

        public void addPrimitive(OsmPrimitive o) {

            addHeadline(o);

            if (!(o.getDataSet() != null && o.getDataSet().getPrimitiveById(o) != null)) {
                s.append(NL).append(INDENT).append(tr("not in data set")).append(NL);
                return;
            }
            if (o.isIncomplete()) {
                s.append(NL).append(INDENT).append(tr("incomplete")).append(NL);
                return;
            }
            s.append(NL);

            addState(o);
            addCommon(o);
            addAttributes(o);
            addSpecial(o);
            addReferrers(s, o);
            addConflicts(o);
            s.append(NL);
        }

        void addHeadline(OsmPrimitive o) {
            addType(o);
            addNameAndId(o);
        }

        void addType(OsmPrimitive o) {
            if (o instanceof Node) {
                s.append(tr("Node: "));
            } else if (o instanceof Way) {
                s.append(tr("Way: "));
            } else if (o instanceof Relation) {
                s.append(tr("Relation: "));
            }
        }

        void addNameAndId(OsmPrimitive o) {
            String name = o.get("name");
            if (name == null) {
                s.append(o.getUniqueId());
            } else {
                s.append(getNameAndId(name, o.getUniqueId()));
            }
        }

        void addState(OsmPrimitive o) {
            StringBuilder sb = new StringBuilder(INDENT);
            /* selected state is left out: not interesting as it is always selected */
            if (o.isDeleted()) {
                sb.append(tr("deleted")).append(INDENT);
            }
            if (!o.isVisible()) {
                sb.append(tr("deleted-on-server")).append(INDENT);
            }
            if (o.isModified()) {
                sb.append(tr("modified")).append(INDENT);
            }
            if (o.isDisabledAndHidden()) {
                sb.append(tr("filtered/hidden")).append(INDENT);
            }
            if (o.isDisabled()) {
                sb.append(tr("filtered/disabled")).append(INDENT);
            }
            if (o.hasDirectionKeys()) {
                if (o.reversedDirection()) {
                    sb.append(tr("has direction keys (reversed)")).append(INDENT);
                } else {
                    sb.append(tr("has direction keys")).append(INDENT);
                }
            }
            String state = sb.toString().trim();
            if (!state.isEmpty()) {
                add(tr("State: "), sb.toString().trim());
            }
        }

        void addCommon(OsmPrimitive o) {
            add(tr("Data Set: "), Integer.toHexString(o.getDataSet().hashCode()));
            add(tr("Edited at: "), o.isTimestampEmpty() ? tr("<new object>")
                    : DateUtils.fromDate(o.getTimestamp()));
            add(tr("Edited by: "), o.getUser() == null ? tr("<new object>")
                    : getNameAndId(o.getUser().getName(), o.getUser().getId()));
            add(tr("Version: "), Integer.toString(o.getVersion()));
            add(tr("In changeset: "), Integer.toString(o.getChangesetId()));
        }

        void addAttributes(OsmPrimitive o) {
            if (o.hasKeys()) {
                add(tr("Tags: "));
                for (String key : o.keySet()) {
                    s.append(INDENT).append(INDENT);
                    s.append(String.format("\"%s\"=\"%s\"%n", key, o.get(key)));
                }
            }
        }

        void addSpecial(OsmPrimitive o) {
            if (o instanceof Node) {
                addCoordinates((Node) o);
            } else if (o instanceof Way) {
                addBbox(o);
                add(tr("Centroid: "), Main.getProjection().eastNorth2latlon(
                        Geometry.getCentroid(((Way) o).getNodes())).toStringCSV(", "));
                addWayNodes((Way) o);
            } else if (o instanceof Relation) {
                addBbox(o);
                addRelationMembers((Relation) o);
            }
        }

        void addRelationMembers(Relation r) {
            add(trn("{0} Member: ", "{0} Members: ", r.getMembersCount(), r.getMembersCount()));
            for (RelationMember m : r.getMembers()) {
                s.append(INDENT).append(INDENT);
                addHeadline(m.getMember());
                s.append(tr(" as \"{0}\"", m.getRole()));
                s.append(NL);
            }
        }

        void addWayNodes(Way w) {
            add(tr("{0} Nodes: ", w.getNodesCount()));
            for (Node n : w.getNodes()) {
                s.append(INDENT).append(INDENT);
                addNameAndId(n);
                s.append(NL);
            }
        }

        void addBbox(OsmPrimitive o) {
            BBox bbox = o.getBBox();
            if (bbox != null) {
                add(tr("Bounding box: "), bbox.toStringCSV(", "));
                EastNorth bottomRigth = Main.getProjection().latlon2eastNorth(bbox.getBottomRight());
                EastNorth topLeft = Main.getProjection().latlon2eastNorth(bbox.getTopLeft());
                add(tr("Bounding box (projected): "),
                        Double.toString(topLeft.east()), ", ",
                        Double.toString(bottomRigth.north()), ", ",
                        Double.toString(bottomRigth.east()), ", ",
                        Double.toString(topLeft.north()));
                add(tr("Center of bounding box: "), bbox.getCenter().toStringCSV(", "));
            }
        }

        void addCoordinates(Node n) {
            if (n.getCoor() != null) {
                add(tr("Coordinates: "),
                        Double.toString(n.getCoor().lat()), ", ",
                        Double.toString(n.getCoor().lon()));
                add(tr("Coordinates (projected): "),
                        Double.toString(n.getEastNorth().east()), ", ",
                        Double.toString(n.getEastNorth().north()));
            }
        }

        void addReferrers(StringBuilder s, OsmPrimitive o) {
            List<OsmPrimitive> refs = o.getReferrers();
            if (!refs.isEmpty()) {
                add(tr("Part of: "));
                for (OsmPrimitive p : refs) {
                    s.append(INDENT).append(INDENT);
                    addHeadline(p);
                    s.append(NL);
                }
            }
        }

        void addConflicts(OsmPrimitive o) {
            Conflict<?> c = layer.getConflicts().getConflictForMy(o);
            if (c != null) {
                add(tr("In conflict with: "));
                addNameAndId(c.getTheir());
            }
        }

        @Override
        public String toString() {
            return s.toString();
        }
    }

    protected void buildMapPaintPanel(JPanel p) {
        p.setLayout(new GridBagLayout());
        txtMappaint = new JosmTextArea();
        txtMappaint.setFont(new Font("Monospaced", txtMappaint.getFont().getStyle(), txtMappaint.getFont().getSize()));
        txtMappaint.setEditable(false);

        p.add(new JScrollPane(txtMappaint), GBC.std().fill());
    }

    protected void createMapPaintText() {
        final Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getAllSelected();
        ElemStyles elemstyles = MapPaintStyles.getStyles();
        NavigatableComponent nc = Main.map.mapView;
        double scale = nc.getDist100Pixel();

        for (OsmPrimitive osm : sel) {
            txtMappaint.append(tr("Styles Cache for \"{0}\":", osm.getDisplayName(DefaultNameFormatter.getInstance())));

            MultiCascade mc = new MultiCascade();

            for (StyleSource s : elemstyles.getStyleSources()) {
                if (s.active) {
                    txtMappaint.append(tr("\n\n> applying {0} style \"{1}\"\n", getSort(s), s.getDisplayString()));
                    s.apply(mc, osm, scale, null, false);
                    txtMappaint.append(tr("\nRange:{0}", mc.range));
                    for (Entry<String, Cascade> e : mc.getLayers()) {
                        txtMappaint.append("\n " + e.getKey() + ": \n" + e.getValue());
                    }
                } else {
                    txtMappaint.append(tr("\n\n> skipping \"{0}\" (not active)", s.getDisplayString()));
                }
            }
            txtMappaint.append(tr("\n\nList of generated Styles:\n"));
            StyleList sl = elemstyles.get(osm, scale, nc);
            for (ElemStyle s : sl) {
                txtMappaint.append(" * " + s + "\n");
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
        if (s instanceof XmlStyleSource) {
            return tr("xml");
        } else if (s instanceof MapCSSStyleSource) {
            return tr("mapcss");
        } else {
            return tr("unknown");
        }
    }
}
