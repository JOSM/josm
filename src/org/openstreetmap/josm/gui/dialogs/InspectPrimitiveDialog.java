// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SingleSelectionModel;

import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.PrimitiveComparator;
import org.openstreetmap.josm.data.osm.User;
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

    protected transient List<IPrimitive> primitives;
    private boolean mappaintTabLoaded;
    private boolean editcountTabLoaded;

    /**
     * Constructs a new {@code InspectPrimitiveDialog}.
     * @param primitives collection of primitives
     * @param data data set
     * @since 12672 (signature)
     */
    public InspectPrimitiveDialog(final Collection<? extends IPrimitive> primitives, OsmData<?, ?, ?, ?> data) {
        super(MainApplication.getMainFrame(), tr("Advanced object info"), tr("Close"));
        this.primitives = new ArrayList<>(primitives);
        setRememberWindowGeometry(getClass().getName() + ".geometry",
                WindowGeometry.centerInWindow(MainApplication.getMainFrame(), new Dimension(750, 550)));

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
        configureContextsensitiveHelp("/Action/InfoAboutElements", true /* show help button */);
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

    protected static String buildDataText(OsmData<?, ?, ?, ?> data, List<IPrimitive> primitives) {
        InspectPrimitiveDataText dt = new InspectPrimitiveDataText(data);
        primitives.stream()
                .sorted(PrimitiveComparator.orderingWaysRelationsNodes().thenComparing(PrimitiveComparator.comparingNames()))
                .forEachOrdered(dt::addPrimitive);
        return dt.toString();
    }

    protected static String buildMapPaintText() {
        final Collection<? extends IPrimitive> sel = MainApplication.getLayerManager().getActiveData().getAllSelected();
        ElemStyles elemstyles = MapPaintStyles.getStyles();
        NavigatableComponent nc = MainApplication.getMap().mapView;
        double scale = nc.getDist100Pixel();

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter txtMappaint = new PrintWriter(stringWriter);
        MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
        try {
            for (IPrimitive osm : sel) {
                String heading = tr("Styles for \"{0}\":", osm.getDisplayName(DefaultNameFormatter.getInstance())
                        .replace(Character.toString(DefaultNameFormatter.BIDI_FIRST_STRONG_ISOLATE), "")
                        .replace(Character.toString(DefaultNameFormatter.BIDI_POP_DIRECTIONAL_ISOLATE), ""));
                txtMappaint.println(heading);
                txtMappaint.println(repeatString("=", heading.length()));

                MultiCascade mc = new MultiCascade();

                for (StyleSource s : elemstyles.getStyleSources()) {
                    if (s.active) {
                        heading = tr("{0} style \"{1}\"", getSort(s), s.getDisplayString());
                        txtMappaint.println(heading);
                        txtMappaint.println(repeatString("-", heading.length()));
                        s.apply(mc, osm, scale, false);
                        txtMappaint.println(tr("Display range: {0}", mc.range));
                        for (Entry<String, Cascade> e : mc.getLayers()) {
                            txtMappaint.println(tr("Layer {0}", e.getKey()));
                            txtMappaint.print(" * ");
                            txtMappaint.println(e.getValue());
                        }
                    }
                }
                txtMappaint.println();
                heading = tr("List of generated Styles:");
                txtMappaint.println(heading);
                txtMappaint.println(repeatString("-", heading.length()));
                StyleElementList sl = elemstyles.get(osm, scale, nc);
                for (StyleElement s : sl) {
                    txtMappaint.print(" * ");
                    txtMappaint.println(s);
                }
                txtMappaint.println();
                txtMappaint.println();
            }
        } finally {
            MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
        }
        if (sel.size() == 2) {
            List<IPrimitive> selList = new ArrayList<>(sel);
            StyleCache sc1 = selList.get(0).getCachedStyle();
            StyleCache sc2 = selList.get(1).getCachedStyle();
            if (sc1 == sc2) {
                txtMappaint.println(tr("The 2 selected objects have identical style caches."));
            }
            if (!sc1.equals(sc2)) {
                txtMappaint.println(tr("The 2 selected objects have different style caches."));
            }
            if (sc1 != sc2 && sc1.equals(sc2)) {
                txtMappaint.println(tr("Warning: The 2 selected objects have equal, but not identical style caches."));
            }
        }
        return stringWriter.toString();
    }

    private static String repeatString(String string, int count) {
        // Java 11: use String.repeat
        return new String(new char[count]).replace("\0", string);
    }

    /*  Future Ideas:
        Calculate the most recent edit date from o.getTimestamp().
        Sort by the count for presentation, so the most active editors are on top.
        Count only tagged nodes (so empty way nodes don't inflate counts).
    */
    protected static String buildListOfEditorsText(Collection<? extends IPrimitive> primitives) {
        final Map<String, Long> editCountByUser = primitives.stream()
                .map(IPrimitive::getUser)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        User::getName,
                        () -> new TreeMap<>(Collator.getInstance(Locale.getDefault())),
                        Collectors.counting()));

        // Print the count in sorted order
        final StringBuilder s = new StringBuilder(48)
            .append(trn("{0} user last edited the selection:", "{0} users last edited the selection:",
                editCountByUser.size(), editCountByUser.size()))
            .append("\n\n");
        editCountByUser.forEach((username, editCount) ->
                s.append(String.format("%6d  %s", editCount, username)).append('\n'));
        return s.toString();
    }

    private static String getSort(StyleSource s) {
        if (s instanceof MapCSSStyleSource) {
            return "MapCSS";
        } else {
            return tr("UNKNOWN");
        }
    }
}
