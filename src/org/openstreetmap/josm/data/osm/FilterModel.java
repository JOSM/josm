// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.osm.Filter.FilterPreferenceEntry;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.OSDLabel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * The model that is used both for auto and manual filters.
 * @since 12400
 */
public class FilterModel {

    /**
     * number of primitives that are disabled but not hidden
     */
    private int disabledCount;
    /**
     * number of primitives that are disabled and hidden
     */
    private int disabledAndHiddenCount;
    /**
     * true, if the filter state (normal / disabled / hidden) of any primitive has changed in the process
     */
    private boolean changed;

    private final List<Filter> filters = new LinkedList<>();
    private final FilterMatcher filterMatcher = new FilterMatcher();

    private void updateFilterMatcher() {
        filterMatcher.reset();
        for (Filter filter : filters) {
            try {
                filterMatcher.add(filter);
            } catch (SearchParseError e) {
                Logging.error(e);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Error in filter <code>{0}</code>:<br>{1}",
                                Utils.escapeReservedCharactersHTML(Utils.shortenString(filter.text, 80)),
                                Utils.escapeReservedCharactersHTML(e.getMessage())),
                        tr("Error in filter"),
                        JOptionPane.ERROR_MESSAGE);
                filter.enable = false;
            }
        }
    }

    /**
     * Initializes the model from preferences.
     * @param prefEntry preference key
     */
    public void loadPrefs(String prefEntry) {
        List<FilterPreferenceEntry> entries = StructUtils.getListOfStructs(
                Config.getPref(), prefEntry, null, FilterPreferenceEntry.class);
        if (entries != null) {
            for (FilterPreferenceEntry e : entries) {
                filters.add(new Filter(e));
            }
            updateFilterMatcher();
        }
    }

    /**
     * Saves the model to preferences.
     * @param prefEntry preferences key
     */
    public void savePrefs(String prefEntry) {
        Collection<FilterPreferenceEntry> entries = new ArrayList<>();
        for (Filter flt : filters) {
            entries.add(flt.getPreferenceEntry());
        }
        StructUtils.putListOfStructs(Config.getPref(), prefEntry, entries, FilterPreferenceEntry.class);
    }

    /**
     * Runs the filters on the current edit data set.
     */
    public void executeFilters() {
        DataSet ds = Main.main.getEditDataSet();
        changed = false;
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
        if (changed) {
            updateMap();
        }
    }

    /**
     * Runs the filter on a list of primitives that are part of the edit data set.
     * @param primitives The primitives
     */
    public void executeFilters(Collection<? extends OsmPrimitive> primitives) {
        DataSet ds = Main.main.getEditDataSet();
        if (ds == null)
            return;

        changed = false;
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

        if (!deselect.isEmpty()) {
            ds.clearSelection(deselect);
        }
        if (changed) {
            updateMap();
        }
    }

    private static void updateMap() {
        OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
        if (editLayer != null) {
            editLayer.invalidate();
        }
    }

    /**
     * Clears all filtered flags from all primitives in the dataset
     */
    public void clearFilterFlags() {
        DataSet ds = Main.main.getEditDataSet();
        if (ds != null) {
            FilterWorker.clearFilterFlags(ds.allPrimitives());
        }
        disabledCount = 0;
        disabledAndHiddenCount = 0;
    }

    /**
     * Removes all filters from this model.
     */
    public void clearFilters() {
        filters.clear();
        updateFilterMatcher();
    }

    /**
     * Adds a new filter to the filter list.
     * @param filter The new filter
     * @return true (as specified by {@link Collection#add})
     */
    public boolean addFilter(Filter filter) {
        filters.add(filter);
        updateFilterMatcher();
        return true;
    }

    /**
     * Moves down the filter in the given row.
     * @param rowIndex The filter row
     * @return true if the filter has been moved down
     */
    public boolean moveDownFilter(int rowIndex) {
        if (rowIndex >= filters.size() - 1)
            return false;
        filters.add(rowIndex + 1, filters.remove(rowIndex));
        updateFilterMatcher();
        return true;
    }

    /**
     * Moves up the filter in the given row
     * @param rowIndex The filter row
     * @return true if the filter has been moved up
     */
    public boolean moveUpFilter(int rowIndex) {
        if (rowIndex == 0)
            return false;
        filters.add(rowIndex - 1, filters.remove(rowIndex));
        updateFilterMatcher();
        return true;
    }

    /**
     * Removes the filter that is displayed in the given row
     * @param rowIndex The index of the filter to remove
     * @return the filter previously at the specified position
     */
    public Filter removeFilter(int rowIndex) {
        Filter result = filters.remove(rowIndex);
        updateFilterMatcher();
        return result;
    }

    /**
     * Sets/replaces the filter for a given row.
     * @param rowIndex The row index
     * @param filter The filter that should be placed in that row
     * @return the filter previously at the specified position
     */
    public Filter setFilter(int rowIndex, Filter filter) {
        Filter result = filters.set(rowIndex, filter);
        updateFilterMatcher();
        return result;
    }

    /**
     * Gets the filter by row index
     * @param rowIndex The row index
     * @return The filter in that row
     */
    public Filter getFilter(int rowIndex) {
        return filters.get(rowIndex);
    }

    /**
     * Draws a text on the map display that indicates that filters are active.
     * @param g The graphics to draw that text on.
     * @param lblOSD On Screen Display label
     * @param header The title to display at the beginning of OSD
     * @param footer The message to display at the bottom of OSD. Must end by {@code </html>}
     */
    public void drawOSDText(Graphics2D g, OSDLabel lblOSD, String header, String footer) {
        if (disabledCount == 0 && disabledAndHiddenCount == 0)
            return;

        String message = "<html>" + header;

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

        message += footer;

        lblOSD.setText(message);
        lblOSD.setSize(lblOSD.getPreferredSize());

        int dx = MainApplication.getMap().mapView.getWidth() - lblOSD.getPreferredSize().width - 15;
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
        return new ArrayList<>(filters);
    }

    /**
     * Returns the number of filters.
     * @return the number of filters
     */
    public int getFiltersCount() {
        return filters.size();
    }

    /**
     * Returns the number of primitives that are disabled but not hidden.
     * @return the number of primitives that are disabled but not hidden
     */
    public int getDisabledCount() {
        return disabledCount;
    }

    /**
     * Returns the number of primitives that are disabled and hidden.
     * @return the number of primitives that are disabled and hidden
     */
    public int getDisabledAndHiddenCount() {
        return disabledAndHiddenCount;
    }

    /**
     * Determines if the filter state (normal / disabled / hidden) of any primitive has changed in the process.
     * @return true, if the filter state (normal / disabled / hidden) of any primitive has changed in the process
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * Returns the list of primitives whose filtering can be affected by change in primitive
     * @param primitives list of primitives to check
     * @return List of primitives whose filtering can be affected by change in source primitives
     */
    public static Collection<OsmPrimitive> getAffectedPrimitives(Collection<? extends OsmPrimitive> primitives) {
        // Filters can use nested parent/child expression so complete tree is necessary
        Set<OsmPrimitive> result = new HashSet<>();
        Stack<OsmPrimitive> stack = new Stack<>();
        stack.addAll(primitives);

        while (!stack.isEmpty()) {
            OsmPrimitive p = stack.pop();

            if (result.contains(p)) {
                continue;
            }

            result.add(p);

            if (p instanceof Way) {
                for (OsmPrimitive n: ((Way) p).getNodes()) {
                    stack.push(n);
                }
            } else if (p instanceof Relation) {
                for (RelationMember rm: ((Relation) p).getMembers()) {
                    stack.push(rm.getMember());
                }
            }

            for (OsmPrimitive ref: p.getReferrers()) {
                stack.push(ref);
            }
        }

        return result;
    }
}
