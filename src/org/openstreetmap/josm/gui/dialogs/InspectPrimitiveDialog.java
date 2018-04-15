// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SingleSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveComparator;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.StyleCache;
import org.openstreetmap.josm.gui.mappaint.StyleElementList;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.GBC;

/**
 * Panel to inspect one or more OsmPrimitives.
 *
 * Gives an unfiltered view of the object's internal state.
 * Might be useful for power users to give more detailed bug reports and
 * to better understand the JOSM data representation.
 */
public class InspectPrimitiveDialog extends ExtendedDialog {

    protected transient List<OsmPrimitive> primitives;
    private boolean mappaintTabLoaded;
    private boolean editcountTabLoaded;

    /**
     * Constructs a new {@code InspectPrimitiveDialog}.
     * @param primitives collection of primitives
     * @param data data set
     * @since 12672 (signature)
     */
    public InspectPrimitiveDialog(final Collection<OsmPrimitive> primitives, DataSet data) {
        super(Main.parent, tr("Advanced object info"), tr("Close"));
        this.primitives = new ArrayList<>(primitives);
        setRememberWindowGeometry(getClass().getName() + ".geometry",
                WindowGeometry.centerInWindow(Main.parent, new Dimension(750, 550)));

        setButtonIcons("ok");
        final JTabbedPane tabs = new JTabbedPane();

        tabs.addTab(tr("data"), genericMonospacePanel(new JPanel(), buildDataText(data, this.primitives)));

        final JPanel pMapPaint = new JPanel();
        tabs.addTab(tr("map style"), pMapPaint);
        tabs.getModel().addChangeListener(e -> {
            if (!mappaintTabLoaded && ((SingleSelectionModel) e.getSource()).getSelectedIndex() == 1) {
                mappaintTabLoaded = true;
                genericMonospacePanel(pMapPaint, buildMapPaintText());
            }
        });

        final JPanel pEditCounts = new JPanel();
        tabs.addTab(tr("edit counts"), pEditCounts);
        tabs.getModel().addChangeListener(e -> {
            if (!editcountTabLoaded && ((SingleSelectionModel) e.getSource()).getSelectedIndex() == 2) {
                editcountTabLoaded = true;
                genericMonospacePanel(pEditCounts, buildListOfEditorsText(primitives));
            }
        });

        setContent(tabs, false);
    }

    protected static JPanel genericMonospacePanel(JPanel p, String s) {
        p.setLayout(new GridBagLayout());
        JosmTextArea jte = new JosmTextArea();
        jte.setFont(GuiHelper.getMonospacedFont(jte));
        jte.setEditable(false);
        jte.append(s);
        jte.setCaretPosition(0);
        p.add(new JScrollPane(jte), GBC.std().fill());
        return p;
    }

    protected static String buildDataText(DataSet data, List<OsmPrimitive> primitives) {
        InspectPrimitiveDataText dt = new InspectPrimitiveDataText(data);
        primitives.stream()
                .sorted(OsmPrimitiveComparator.orderingWaysRelationsNodes().thenComparing(OsmPrimitiveComparator.comparingNames()))
                .forEachOrdered(dt::addPrimitive);
        return dt.toString();
    }

    protected static String buildMapPaintText() {
        final Collection<OsmPrimitive> sel = MainApplication.getLayerManager().getActiveDataSet().getAllSelected();
        ElemStyles elemstyles = MapPaintStyles.getStyles();
        NavigatableComponent nc = MainApplication.getMap().mapView;
        double scale = nc.getDist100Pixel();

        final StringBuilder txtMappaint = new StringBuilder();
        MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
        try {
            for (OsmPrimitive osm : sel) {
                txtMappaint.append(tr("Styles Cache for \"{0}\":", osm.getDisplayName(DefaultNameFormatter.getInstance())));

                MultiCascade mc = new MultiCascade();

                for (StyleSource s : elemstyles.getStyleSources()) {
                    if (s.active) {
                        txtMappaint.append(tr("\n\n> applying {0} style \"{1}\"\n", getSort(s), s.getDisplayString()));
                        s.apply(mc, osm, scale, false);
                        txtMappaint.append(tr("\nRange:{0}", mc.range));
                        for (Entry<String, Cascade> e : mc.getLayers()) {
                            txtMappaint.append("\n ").append(e.getKey()).append(": \n").append(e.getValue());
                        }
                    } else {
                        txtMappaint.append(tr("\n\n> skipping \"{0}\" (not active)", s.getDisplayString()));
                    }
                }
                txtMappaint.append(tr("\n\nList of generated Styles:\n"));
                StyleElementList sl = elemstyles.get(osm, scale, nc);
                for (StyleElement s : sl) {
                    txtMappaint.append(" * ").append(s).append('\n');
                }
                txtMappaint.append("\n\n");
            }
        } finally {
            MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
        }
        if (sel.size() == 2) {
            List<OsmPrimitive> selList = new ArrayList<>(sel);
            StyleCache sc1 = selList.get(0).getCachedStyle();
            StyleCache sc2 = selList.get(1).getCachedStyle();
            if (sc1 == sc2) {
                txtMappaint.append(tr("The 2 selected objects have identical style caches."));
            }
            if (!sc1.equals(sc2)) {
                txtMappaint.append(tr("The 2 selected objects have different style caches."));
            }
            if (sc1 != sc2 && sc1.equals(sc2)) {
                txtMappaint.append(tr("Warning: The 2 selected objects have equal, but not identical style caches."));
            }
        }
        return txtMappaint.toString();
    }

    /*  Future Ideas:
        Calculate the most recent edit date from o.getTimestamp().
        Sort by the count for presentation, so the most active editors are on top.
        Count only tagged nodes (so empty way nodes don't inflate counts).
    */
    protected static String buildListOfEditorsText(Iterable<OsmPrimitive> primitives) {
        final Map<String, Integer> editCountByUser = new TreeMap<>(Collator.getInstance(Locale.getDefault()));

        // Count who edited each selected object
        for (OsmPrimitive o : primitives) {
            if (o.getUser() != null) {
                String username = o.getUser().getName();
                Integer oldCount = editCountByUser.get(username);
                if (oldCount == null) {
                    editCountByUser.put(username, 1);
                } else {
                    editCountByUser.put(username, oldCount + 1);
                }
            }
        }

        // Print the count in sorted order
        final StringBuilder s = new StringBuilder(48)
            .append(trn("{0} user last edited the selection:", "{0} users last edited the selection:",
                editCountByUser.size(), editCountByUser.size()))
            .append("\n\n");
        for (Map.Entry<String, Integer> entry : editCountByUser.entrySet()) {
            final String username = entry.getKey();
            final Integer editCount = entry.getValue();
            s.append(String.format("%6d  %s", editCount, username)).append('\n');
        }
        return s.toString();
    }

    private static String getSort(StyleSource s) {
        if (s instanceof MapCSSStyleSource) {
            return tr("mapcss");
        } else {
            return tr("unknown");
        }
    }
}
