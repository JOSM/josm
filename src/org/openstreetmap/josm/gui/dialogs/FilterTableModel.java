// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.data.osm.Filter.FilterPreferenceEntry;
import org.openstreetmap.josm.data.osm.FilterMatcher;
import org.openstreetmap.josm.data.osm.FilterWorker;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 *
 * @author Petr_Dlouhý
 */
public class FilterTableModel extends AbstractTableModel {

    public static final int COL_ENABLED = 0;
    public static final int COL_HIDING = 1;
    public static final int COL_TEXT = 2;
    public static final int COL_INVERTED = 3;

    // number of primitives that are disabled but not hidden
    public int disabledCount;
    // number of primitives that are disabled and hidden
    public int disabledAndHiddenCount;

    /**
     * Constructs a new {@code FilterTableModel}.
     */
    public FilterTableModel() {
        loadPrefs();
    }

    private final List<Filter> filters = new LinkedList<Filter>();
    private final FilterMatcher filterMatcher = new FilterMatcher();

    private void updateFilters() {
        try {
            filterMatcher.update(filters);
            executeFilters();
        } catch (ParseError e) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    e.getMessage(),
                    tr("Error in filter"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void executeFilters() {
        DataSet ds = Main.main.getCurrentDataSet();
        boolean changed = false;
        if (ds == null) {
            disabledAndHiddenCount = 0;
            disabledCount = 0;
            changed = true;
        } else {
            final Collection<OsmPrimitive> deselect = new HashSet<OsmPrimitive>();

            ds.beginUpdate();
            try {

                final Collection<OsmPrimitive> all = ds.allNonDeletedCompletePrimitives();

                changed = FilterWorker.executeFilters(all, filterMatcher);

                disabledCount = 0;
                disabledAndHiddenCount = 0;
                // collect disabled and selected the primitives
                for (OsmPrimitive osm : all) {
                    if (osm.isDisabled()) {
                        disabledCount++;
                        if (osm.isSelected()) {
                            deselect.add(osm);
                        }
                        if (osm.isDisabledAndHidden()) {
                            disabledAndHiddenCount++;
                        }
                    }
                }
                disabledCount -= disabledAndHiddenCount;
            } finally {
                ds.endUpdate();
            }

            if (!deselect.isEmpty()) {
                ds.clearSelection(deselect);
            }
        }

        if (Main.isDisplayingMapView() && changed) {
            Main.map.mapView.repaint();
            Main.map.filterDialog.updateDialogHeader();
        }
    }

    public void executeFilters(Collection<? extends OsmPrimitive> primitives) {
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds == null)
            return;

        boolean changed = false;
        List<OsmPrimitive> deselect = new ArrayList<OsmPrimitive>();

        ds.beginUpdate();
        try {
            for (int i=0; i<2; i++) {
                for (OsmPrimitive primitive: primitives) {

                    if (i == 0 && primitive instanceof Node) {
                        continue;
                    }

                    if (i == 1 && !(primitive instanceof Node)) {
                        continue;
                    }

                    if (primitive.isDisabled()) {
                        disabledCount--;
                    }
                    if (primitive.isDisabledAndHidden()) {
                        disabledAndHiddenCount--;
                    }
                    changed = changed | FilterWorker.executeFilters(primitive, filterMatcher);
                    if (primitive.isDisabled()) {
                        disabledCount++;
                    }
                    if (primitive.isDisabledAndHidden()) {
                        disabledAndHiddenCount++;
                    }

                    if (primitive.isSelected() && primitive.isDisabled()) {
                        deselect.add(primitive);
                    }

                }
            }
        } finally {
            ds.endUpdate();
        }

        if (changed) {
            Main.map.mapView.repaint();
            Main.map.filterDialog.updateDialogHeader();
            ds.clearSelection(deselect);
        }

    }

    public void clearFilterFlags() {
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds != null) {
            FilterWorker.clearFilterFlags(ds.allPrimitives());
        }
        disabledCount = 0;
        disabledAndHiddenCount = 0;
    }

    private void loadPrefs() {
        List<FilterPreferenceEntry> entries = Main.pref.getListOfStructs("filters.entries", null, FilterPreferenceEntry.class);
        if (entries != null) {
            for (FilterPreferenceEntry e : entries) {
                filters.add(new Filter(e));
            }
            updateFilters();
        }
    }

    private void savePrefs() {
        Collection<FilterPreferenceEntry> entries = new ArrayList<FilterPreferenceEntry>();
        for (Filter flt : filters) {
            entries.add(flt.getPreferenceEntry());
        }
        Main.pref.putListOfStructs("filters.entries", entries, FilterPreferenceEntry.class);
    }

    public void addFilter(Filter f) {
        filters.add(f);
        savePrefs();
        updateFilters();
        fireTableRowsInserted(filters.size() - 1, filters.size() - 1);
    }

    public void moveDownFilter(int i) {
        if (i >= filters.size() - 1)
            return;
        filters.add(i + 1, filters.remove(i));
        savePrefs();
        updateFilters();
        fireTableRowsUpdated(i, i + 1);
    }

    public void moveUpFilter(int i) {
        if (i == 0)
            return;
        filters.add(i - 1, filters.remove(i));
        savePrefs();
        updateFilters();
        fireTableRowsUpdated(i - 1, i);
    }

    public void removeFilter(int i) {
        filters.remove(i);
        savePrefs();
        updateFilters();
        fireTableRowsDeleted(i, i);
    }

    public void setFilter(int i, Filter f) {
        filters.set(i, f);
        savePrefs();
        updateFilters();
        fireTableRowsUpdated(i, i);
    }

    public Filter getFilter(int i) {
        return filters.get(i);
    }

    @Override
    public int getRowCount() {
        return filters.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int column) {
        String[] names = { /* translators notes must be in front */
                /* column header: enable filter */trc("filter", "E"),
                /* column header: hide filter */trc("filter", "H"),
                /* column header: filter text */trc("filter", "Text"),
                /* column header: inverted filter */trc("filter", "I"),
                /* column header: filter mode */trc("filter", "M") };
        return names[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        Class<?>[] classes = { Boolean.class, Boolean.class, String.class, Boolean.class, String.class };
        return classes[column];
    }

    public boolean isCellEnabled(int row, int column) {
        if (!filters.get(row).enable && column != 0)
            return false;
        return true;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (!filters.get(row).enable && column != 0)
            return false;
        if (column < 4)
            return true;
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        if (row >= filters.size()) {
            return;
        }
        Filter f = filters.get(row);
        switch (column) {
        case COL_ENABLED:
            f.enable = (Boolean) aValue;
            savePrefs();
            updateFilters();
            fireTableRowsUpdated(row, row);
            break;
        case COL_HIDING:
            f.hiding = (Boolean) aValue;
            savePrefs();
            updateFilters();
            break;
        case COL_TEXT:
            f.text = (String) aValue;
            savePrefs();
            break;
        case COL_INVERTED:
            f.inverted = (Boolean) aValue;
            savePrefs();
            updateFilters();
            break;
        }
        if (column != 0) {
            fireTableCellUpdated(row, column);
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (row >= filters.size()) {
            return null;
        }
        Filter f = filters.get(row);
        switch (column) {
        case COL_ENABLED:
            return f.enable;
        case COL_HIDING:
            return f.hiding;
        case COL_TEXT:
            return f.text;
        case COL_INVERTED:
            return f.inverted;
        case 4:
            switch (f.mode) { /* translators notes must be in front */
            case replace: /* filter mode: replace */
                return trc("filter", "R");
            case add: /* filter mode: add */
                return trc("filter", "A");
            case remove: /* filter mode: remove */
                return trc("filter", "D");
            case in_selection: /* filter mode: in selection */
                return trc("filter", "F");
            }
        }
        return null;
    }

    /**
     * On screen display label
     */
    private static class OSDLabel extends JLabel {
        public OSDLabel(String text) {
            super(text);
            setOpaque(true);
            setForeground(Color.black);
            setBackground(new Color(0, 0, 0, 0));
            setFont(getFont().deriveFont(Font.PLAIN));
            setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        }

        @Override
        public void paintComponent(Graphics g) {
            g.setColor(new Color(255, 255, 255, 140));
            g.fillRoundRect(getX(), getY(), getWidth(), getHeight(), 10, 10);
            super.paintComponent(g);
        }
    }

    private OSDLabel lblOSD = new OSDLabel("");

    public void drawOSDText(Graphics2D g) {
        String message = "<html>" + tr("<h2>Filter active</h2>");

        if (disabledCount == 0 && disabledAndHiddenCount == 0)
            return;

        if (disabledAndHiddenCount != 0) {
            /* for correct i18n of plural forms - see #9110 */
            message += trn("<p><b>{0}</b> object hidden", "<p><b>{0}</b> objects hidden", disabledAndHiddenCount, disabledAndHiddenCount);
        }

        if (disabledAndHiddenCount != 0 && disabledCount != 0) {
            message += "<br>";
        }

        if (disabledCount != 0) {
            /* for correct i18n of plural forms - see #9110 */
            message += trn("<b>{0}</b> object disabled", "<b>{0}</b> objects disabled", disabledCount, disabledCount);
        }

        message += tr("</p><p>Close the filter dialog to see all objects.<p></html>");

        lblOSD.setText(message);
        lblOSD.setSize(lblOSD.getPreferredSize());

        int dx = Main.map.mapView.getWidth() - lblOSD.getPreferredSize().width - 15;
        int dy = 15;
        g.translate(dx, dy);
        lblOSD.paintComponent(g);
        g.translate(-dx, -dy);
    }

    public List<Filter> getFilters() {
        return filters;
    }
}
