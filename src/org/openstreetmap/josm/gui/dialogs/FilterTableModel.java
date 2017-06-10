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
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Utils;

/**
 * The model that is used for the table in the {@link FilterDialog}.
 *
 * @author Petr_Dlouhý
 */
public class FilterTableModel extends AbstractTableModel {

    /**
     * The filter enabled column
     */
    public static final int COL_ENABLED = 0;
    /**
     * The column indicating if the filter is hiding.
     */
    public static final int COL_HIDING = 1;
    /**
     * The column that displays the filter text
     */
    public static final int COL_TEXT = 2;
    /**
     * The column to invert the filter
     */
    public static final int COL_INVERTED = 3;

    /**
     * number of primitives that are disabled but not hidden
     */
    public int disabledCount;
    /**
     * number of primitives that are disabled and hidden
     */
    public int disabledAndHiddenCount;

    /**
     * A helper for {@link #drawOSDText(Graphics2D)}.
     */
    private final OSDLabel lblOSD = new OSDLabel("");

    /**
     * Constructs a new {@code FilterTableModel}.
     */
    public FilterTableModel() {
        loadPrefs();
    }

    private final transient List<Filter> filters = new LinkedList<>();
    private final transient FilterMatcher filterMatcher = new FilterMatcher();

    private void updateFilters() {
        filterMatcher.reset();
        for (Filter filter : filters) {
            try {
                filterMatcher.add(filter);
            } catch (ParseError e) {
                Main.error(e);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Error in filter <code>{0}</code>:<br>{1}",
                                Utils.escapeReservedCharactersHTML(Utils.shortenString(filter.text, 80)),
                                Utils.escapeReservedCharactersHTML(e.getMessage())),
                        tr("Error in filter"),
                        JOptionPane.ERROR_MESSAGE);
                filter.enable = false;
                savePrefs();
            }
        }
        executeFilters();
    }

    /**
     * Runs the filters on the current edit data set.
     */
    public void executeFilters() {
        DataSet ds = Main.getLayerManager().getEditDataSet();
        boolean changed = false;
        if (ds == null) {
            disabledAndHiddenCount = 0;
            disabledCount = 0;
            changed = true;
        } else {
            final Collection<OsmPrimitive> deselect = new HashSet<>();

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

        if (changed && Main.isDisplayingMapView()) {
            updateMap();
        }
    }

    /**
     * Runs the filter on a list of primitives that are part of the edit data set.
     * @param primitives The primitives
     */
    public void executeFilters(Collection<? extends OsmPrimitive> primitives) {
        DataSet ds = Main.getLayerManager().getEditDataSet();
        if (ds == null)
            return;

        boolean changed = false;
        List<OsmPrimitive> deselect = new ArrayList<>();

        ds.beginUpdate();
        try {
            for (int i = 0; i < 2; i++) {
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
                    changed |= FilterWorker.executeFilters(primitive, filterMatcher);
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
            updateMap();
            ds.clearSelection(deselect);
        }
    }

    private static void updateMap() {
        OsmDataLayer editLayer = Main.getLayerManager().getEditLayer();
        if (editLayer != null) {
            editLayer.invalidate();
        }
        Main.map.filterDialog.updateDialogHeader();
    }

    /**
     * Clears all filtered flags from all primitives in the dataset
     */
    public void clearFilterFlags() {
        DataSet ds = Main.getLayerManager().getEditDataSet();
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
        Collection<FilterPreferenceEntry> entries = new ArrayList<>();
        for (Filter flt : filters) {
            entries.add(flt.getPreferenceEntry());
        }
        Main.pref.putListOfStructs("filters.entries", entries, FilterPreferenceEntry.class);
    }

    /**
     * Adds a new filter to the filter list.
     * @param filter The new filter
     */
    public void addFilter(Filter filter) {
        filters.add(filter);
        savePrefs();
        updateFilters();
        fireTableRowsInserted(filters.size() - 1, filters.size() - 1);
    }

    /**
     * Moves down the filter in the given row.
     * @param rowIndex The filter row
     */
    public void moveDownFilter(int rowIndex) {
        if (rowIndex >= filters.size() - 1)
            return;
        filters.add(rowIndex + 1, filters.remove(rowIndex));
        savePrefs();
        updateFilters();
        fireTableRowsUpdated(rowIndex, rowIndex + 1);
    }

    /**
     * Moves up the filter in the given row
     * @param rowIndex The filter row
     */
    public void moveUpFilter(int rowIndex) {
        if (rowIndex == 0)
            return;
        filters.add(rowIndex - 1, filters.remove(rowIndex));
        savePrefs();
        updateFilters();
        fireTableRowsUpdated(rowIndex - 1, rowIndex);
    }

    /**
     * Removes the filter that is displayed in the given row
     * @param rowIndex The index of the filter to remove
     */
    public void removeFilter(int rowIndex) {
        filters.remove(rowIndex);
        savePrefs();
        updateFilters();
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    /**
     * Sets/replaces the filter for a given row.
     * @param rowIndex The row index
     * @param filter The filter that should be placed in that row
     */
    public void setFilter(int rowIndex, Filter filter) {
        filters.set(rowIndex, filter);
        savePrefs();
        updateFilters();
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    /**
     * Gets the filter by row index
     * @param rowIndex The row index
     * @return The filter in that row
     */
    public Filter getFilter(int rowIndex) {
        return filters.get(rowIndex);
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
        String[] names = {/* translators notes must be in front */
                /* column header: enable filter */trc("filter", "E"),
                /* column header: hide filter */trc("filter", "H"),
                /* column header: filter text */trc("filter", "Text"),
                /* column header: inverted filter */trc("filter", "I"),
                /* column header: filter mode */trc("filter", "M")};
        return names[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        Class<?>[] classes = {Boolean.class, Boolean.class, String.class, Boolean.class, String.class};
        return classes[column];
    }

    /**
     * Determines if a cell is enabled.
     * @param row row index
     * @param column column index
     * @return {@code true} if the cell at (row, column) is enabled
     */
    public boolean isCellEnabled(int row, int column) {
        return filters.get(row).enable || column == 0;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column < 4 && isCellEnabled(row, column);
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
        default: // Do nothing
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
            default:
                Main.warn("Unknown filter mode: " + f.mode);
            }
            break;
        default: // Do nothing
        }
        return null;
    }

    /**
     * On screen display label
     */
    private static class OSDLabel extends JLabel {
        OSDLabel(String text) {
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

    /**
     * Draws a text on the map display that indicates that filters are active.
     * @param g The graphics to draw that text on.
     */
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

    /**
     * Returns the list of filters.
     * @return the list of filters
     */
    public List<Filter> getFilters() {
        return filters;
    }
}
