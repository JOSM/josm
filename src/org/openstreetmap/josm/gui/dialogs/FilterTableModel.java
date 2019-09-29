// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Graphics2D;
import java.util.Collection;
import java.util.List;

import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.data.osm.FilterModel;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.autofilter.AutoFilterManager;
import org.openstreetmap.josm.gui.util.SortableTableModel;
import org.openstreetmap.josm.gui.widgets.OSDLabel;
import org.openstreetmap.josm.tools.Logging;

/**
 * The model that is used for the table in the {@link FilterDialog}.
 *
 * @author Petr_Dlouhý
 * @since 2125
 */
public class FilterTableModel extends AbstractTableModel implements SortableTableModel<Filter> {

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
     * The filter model
     */
    final FilterModel model = new FilterModel();

    /**
     * The selection model
     */
    final ListSelectionModel selectionModel;

    /**
     * A helper for {@link #drawOSDText(Graphics2D)}
     */
    private final OSDLabel lblOSD = new OSDLabel("");

    /**
     * Constructs a new {@code FilterTableModel}.
     * @param listSelectionModel selection model
     */
    public FilterTableModel(ListSelectionModel listSelectionModel) {
        this.selectionModel = listSelectionModel;
        loadPrefs();
    }

    private void updateFilters() {
        AutoFilterManager.getInstance().setCurrentAutoFilter(null);
        executeFilters(true);
    }

    /**
     * Runs the filters on the current edit data set, if any. Does nothing if no filter is enabled.
     */
    public void executeFilters() {
        executeFilters(false);
    }

    /**
     * Runs the filter on a list of primitives that are part of the edit data set, if any. Does nothing if no filter is enabled.
     * @param primitives The primitives
     */
    public void executeFilters(Collection<? extends OsmPrimitive> primitives) {
        executeFilters(primitives, false);
    }

    /**
     * Runs the filters on the current edit data set, if any.
     * @param force force execution of filters even if no filter is enabled. Useful to reset state after change of filters
     * @since 14206
     */
    public void executeFilters(boolean force) {
        if (AutoFilterManager.getInstance().getCurrentAutoFilter() == null && (force || model.hasFilters())) {
            model.executeFilters();
            updateMap();
        }
    }

    /**
     * Runs the filter on a list of primitives that are part of the edit data set, if any.
     * @param force force execution of filters even if no filter is enabled. Useful to reset state after change of filters
     * @param primitives The primitives
     * @since 14206
     */
    public void executeFilters(Collection<? extends OsmPrimitive> primitives, boolean force) {
        if (AutoFilterManager.getInstance().getCurrentAutoFilter() == null && (force || model.hasFilters())) {
            model.executeFilters(primitives);
            updateMap();
        }
    }

    private void updateMap() {
        MapFrame map = MainApplication.getMap();
        if (map != null && model.isChanged()) {
            map.filterDialog.updateDialogHeader();
        }
    }

    private void loadPrefs() {
        model.loadPrefs("filters.entries");
    }

    private void savePrefs() {
        model.savePrefs("filters.entries");
    }

    /**
     * Adds a new filter to the filter list.
     * @param filter The new filter
     */
    public void addFilter(Filter filter) {
        if (model.addFilter(filter)) {
            savePrefs();
            updateFilters();
            int size = model.getFiltersCount();
            fireTableRowsInserted(size - 1, size - 1);
        }
    }

    @Override
    public boolean doMove(int delta, int... selectedRows) {
        return model.moveFilters(delta, selectedRows);
    }

    @Override
    public boolean move(int delta, int... selectedRows) {
        if (!SortableTableModel.super.move(delta, selectedRows))
            return false;
        savePrefs();
        updateFilters();
        int rowIndex = selectedRows[0];
        if (delta < 0)
            fireTableRowsUpdated(rowIndex + delta, rowIndex);
        else if (delta > 0)
            fireTableRowsUpdated(rowIndex, rowIndex + delta);
        return true;
    }

    /**
     * Removes the filter that is displayed in the given row
     * @param rowIndex The index of the filter to remove
     */
    public void removeFilter(int rowIndex) {
        if (model.removeFilter(rowIndex) != null) {
            savePrefs();
            updateFilters();
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    @Override
    public Filter setValue(int rowIndex, Filter filter) {
        Filter result = model.setValue(rowIndex, filter);
        savePrefs();
        updateFilters();
        fireTableRowsUpdated(rowIndex, rowIndex);
        return result;
    }

    @Override
    public Filter getValue(int rowIndex) {
        return model.getValue(rowIndex);
    }

    @Override
    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    @Override
    public int getRowCount() {
        return model.getFiltersCount();
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
        return model.getValue(row).enable || column == 0;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column < 4 && isCellEnabled(row, column);
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        if (row >= model.getFiltersCount()) {
            return;
        }
        Filter f = model.getValue(row);
        switch (column) {
        case COL_ENABLED:
            f.enable = (Boolean) aValue;
            break;
        case COL_HIDING:
            f.hiding = (Boolean) aValue;
            break;
        case COL_TEXT:
            f.text = (String) aValue;
            break;
        case COL_INVERTED:
            f.inverted = (Boolean) aValue;
            break;
        default: // Do nothing
        }
        setValue(row, f);
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (row >= model.getFiltersCount()) {
            return null;
        }
        Filter f = model.getValue(row);
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
                Logging.warn("Unknown filter mode: " + f.mode);
            }
            break;
        default: // Do nothing
        }
        return null;
    }

    /**
     * Draws a text on the map display that indicates that filters are active.
     * @param g The graphics to draw that text on.
     */
    public void drawOSDText(Graphics2D g) {
        model.drawOSDText(g, lblOSD,
                tr("<h2>Filter active</h2>"),
                tr("</p><p>Close the filter dialog to see all objects.<p></html>"));
    }

    /**
     * Returns the list of filters.
     * @return the list of filters
     */
    public List<Filter> getFilters() {
        return model.getFilters();
    }

    @Override
    public void sort() {
        model.sort();
        fireTableDataChanged();
    }

    @Override
    public void reverse() {
        model.reverse();
        fireTableDataChanged();
    }
}
