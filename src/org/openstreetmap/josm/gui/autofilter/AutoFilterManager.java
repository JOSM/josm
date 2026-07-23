// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.data.osm.FilterModel;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.MatchSupplier;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrame.MapModeChangeListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.NavigatableComponent.ZoomChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.widgets.OSDLabel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;

/**
 * The auto filter manager keeps track of registered auto filter rules and applies the active one on the fly,
 * when the map contents, location or zoom changes.
 * @since 12400
 */
public final class AutoFilterManager
implements ZoomChangeListener, MapModeChangeListener, DataSetListener, PreferenceChangedListener, LayerChangeListener {

    /**
     * Property to determine if the auto filter feature is enabled.
     */
    public static final BooleanProperty PROP_AUTO_FILTER_ENABLED = new BooleanProperty("auto.filter.enabled", true);

    /**
     * Property to determine if the auto filter feature completely hides elements instead of just disabling them.
     * Equivalent to {@link Filter#hiding}
     */
    public static final BooleanProperty PROP_AUTO_FILTER_HIDING = new BooleanProperty("auto.filter.hiding", false);


    /**
     * Property to determine the current auto filter rule.
     */
    public static final StringProperty PROP_AUTO_FILTER_RULE = new StringProperty("auto.filter.rule", "level");

    /**
     * The unique instance.
     */
    private static volatile AutoFilterManager instance;

    /**
     * The buttons currently displayed in map view.
     */
    private final Map<OptionalInt, AutoFilterButton> buttons = new HashMap<>();

    /**
     * The list of registered auto filter rules.
     */
    private final List<AutoFilterRule> rules = new ArrayList<>();

    /**
     * A helper for {@link #drawOSDText(Graphics2D)}.
     */
    private final OSDLabel lblOSD = new OSDLabel("");

    /**
     * The filter model.
     */
    private final FilterModel model = new FilterModel();

    /**
     * The currently enabled rule, if any.
     */
    AutoFilterRule enabledRule;

    /**
     * The currently selected auto filters, if any.
     * If more than one auto filter is active, elements will match if they match at least one of them.
     */
    private final List<AutoFilter> currentAutoFilters = new ArrayList<>();

    /**
     * Returns the unique instance.
     * @return the unique instance
     */
    public static AutoFilterManager getInstance() {
        if (instance == null) {
            instance = new AutoFilterManager();
        }
        return instance;
    }

    private AutoFilterManager() {
        MapFrame.addMapModeChangeListener(this);
        Config.getPref().addPreferenceChangeListener(this);
        NavigatableComponent.addZoomChangeListener(this);
        MainApplication.getLayerManager().addLayerChangeListener(this);
        DatasetEventManager.getInstance().addDatasetListener(this, FireMode.IN_EDT_CONSOLIDATED);
        registerAutoFilterRules(AutoFilterRule.defaultRules());
    }

    private synchronized void updateButtons() {
        MapFrame map = MainApplication.getMap();
        if (enabledRule != null && map != null
                && enabledRule.getMinZoomLevel() <= Selector.GeneralSelector.scale2level(map.mapView.getDist100Pixel())) {
            // Retrieve the values from current rule visible on screen
            NavigableSet<Integer> values = getNumericValues();
            // Make sure current auto filter buttons remain visible even if no data is found, to allow user to disable them
            for (var currentAutoFilter : currentAutoFilters) {
                if (currentAutoFilter.getFilter().value != null) {
                    values.add(currentAutoFilter.getFilter().value);
                }
            }
            if (!values.equals(buttons.keySet().stream()
                    .filter(it -> it.isPresent() && it.getAsInt() != Integer.MIN_VALUE)
                    .map(OptionalInt::getAsInt).collect(Collectors.toSet()))) {
                removeAllButtons();
                addNewButtons(values);
            }
        }
    }

    static class CompiledFilter extends Filter implements MatchSupplier {
        final AutoFilterRule rule;
        final Integer value;

        CompiledFilter(AutoFilterRule rule, Integer value, boolean hiding) {
            this.rule = rule;
            this.value = value;
            this.hiding = hiding;
            this.enable = true;
            this.inverted = true;
            this.text = rule.getKey() + "=" + rule.formatValue(value);
        }

        @Override
        public SearchCompiler.Match get() {
            return new Match(rule, value);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + Objects.hash(rule, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj) || getClass() != obj.getClass())
                return false;
            CompiledFilter other = (CompiledFilter) obj;
            return Objects.equals(rule, other.rule) && Objects.equals(value, other.value);
        }
    }

    /** The combination of multiple {@link CompiledFilter}s */
    static class CombinedFilter extends Filter implements MatchSupplier {

        private final List<CompiledFilter> filters;

        CombinedFilter(List<CompiledFilter> filters) {

            if (filters == null || filters.isEmpty()) throw new IllegalArgumentException("no filters provided");

            this.filters = filters;

            boolean hiding = filters.get(0).hiding;
            String key = filters.get(0).rule.getKey();
            List<String> values = new ArrayList<>();

            for (CompiledFilter filter : filters) {
                if (hiding != filter.hiding) throw new IllegalArgumentException("non-matching hiding properties");
                if (!Objects.equals(key, filter.rule.getKey())) throw new IllegalArgumentException("non-matching keys");
                values.add(filter.rule.formatValue(filter.value));
            }

            this.hiding = hiding;
            this.enable = true;
            this.inverted = true;
            this.text = key + "~" + String.join("|", values);

        }

        @Override
        public SearchCompiler.Match get() {
            return new SearchCompiler.Match() {
                @Override
                public boolean match(OsmPrimitive osm) {
                    return filters.stream().anyMatch(filter -> filter.get().match(osm));
                }
            };
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + Objects.hash(filters);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj) || getClass() != obj.getClass())
                return false;
            CombinedFilter other = (CombinedFilter) obj;
            return Objects.equals(filters, other.filters);
        }
    }

    static class Match extends SearchCompiler.Match {
        final AutoFilterRule rule;
        final Integer value;

        Match(AutoFilterRule rule, Integer value) {
            this.rule = rule;
            this.value = value;
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            IntStream values = rule.getTagValuesForPrimitive(osm, false);
            if (value != null) {
                return values.anyMatch(v -> v == value);
            } else {
                return values.findAny().isEmpty();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Match match = (Match) o;
            return Objects.equals(value, match.value) &&
                    Objects.equals(rule, match.rule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rule, value);
        }
    }

    private synchronized void addNewButtons(NavigableSet<Integer> values) {
        if (values.isEmpty()) {
            return;
        }
        int i = 0;
        int maxWidth = 16;
        final AutoFilterButton keyButton = AutoFilterButton.forOsmKey(enabledRule.getKey());
        addButton(keyButton, Integer.MIN_VALUE, i++);
        var valueList = new ArrayList<>(values.descendingSet());
        if (enabledRule.getNoValueFilter()) {
            valueList.add(null);
        }
        for (final Integer value : valueList) {
            CompiledFilter filter = new CompiledFilter(enabledRule, value, PROP_AUTO_FILTER_HIDING.get());
            String label = enabledRule.formatValue(value);
            AutoFilter autoFilter = new AutoFilter(label, filter.text, filter);
            AutoFilterButton button = new AutoFilterButton(autoFilter);
            if (currentAutoFilters.contains(autoFilter)) {
                button.getModel().setPressed(true);
            }
            addButton(button, value, i++);
            maxWidth = Math.max(maxWidth, button.getPreferredSize().width);
        }
        for (AutoFilterButton b : buttons.values()) {
            b.setSize(b == keyButton ? b.getPreferredSize().width : maxWidth, 20);
        }
        MainApplication.getMap().mapView.validate();
    }

    private void addButton(AutoFilterButton button, Integer value, int i) {
        MapView mapView = MainApplication.getMap().mapView;
        buttons.put(value == null ? OptionalInt.empty() : OptionalInt.of(value), button);
        mapView.add(button).setLocation(3, 60 + 22*i);
    }

    private void removeAllButtons() {
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            buttons.values().forEach(map.mapView::remove);
        }
        buttons.clear();
    }

    private synchronized NavigableSet<Integer> getNumericValues() {
        DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
        if (ds == null) {
            return Collections.emptyNavigableSet();
        }
        BBox bbox = MainApplication.getMap().mapView.getState().getViewArea().getLatLonBoundsBox().toBBox();
        NavigableSet<Integer> values = new TreeSet<>();
        for (var primitiveList : List.of(ds.searchNodes(bbox), ds.searchWays(bbox), ds.searchRelations(bbox))) {
            // add all values that are directly mentioned
            primitiveList.forEach(o -> enabledRule.getTagValuesForPrimitive(o, true).forEach(values::add));
            // only add integer values from value ranges, not fractional values
            primitiveList.forEach(o -> enabledRule.getTagValuesForPrimitive(o, false)
                    .filter(v -> !enabledRule.formatValue(v).contains("."))
                    .forEach(values::add));

        }
        return values;
    }

    @Override
    public void zoomChanged() {
        updateButtons();
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        updateFiltersFull();
        updateButtons();
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {
        updateFiltersFull();
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        updateFiltersFull();
    }

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        updateFiltersEvent(event, false);
        updateButtons();
    }

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        updateFiltersFull();
        updateButtons();
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
        updateFiltersEvent(event, true);
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        updateFiltersEvent(event, true);
        updateButtons();
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        updateFiltersEvent(event, true);
    }

    @Override
    public void mapModeChange(MapMode oldMapMode, MapMode newMapMode) {
        updateFiltersFull();
    }

    private synchronized void updateFiltersFull() {
        if (!currentAutoFilters.isEmpty()) {
            model.executeFilters();
        }
    }

    private synchronized void updateFiltersEvent(AbstractDatasetChangedEvent event, boolean affectedOnly) {
        if (!currentAutoFilters.isEmpty()) {
            Collection<? extends OsmPrimitive> prims = event.getPrimitives();
            model.executeFilters(affectedOnly ? FilterModel.getAffectedPrimitives(prims) : prims);
        }
    }

    /**
     * Registers new auto filter rule(s).
     * @param filterRules new auto filter rules. Must not be null
     * @return {@code true} if the list changed as a result of the call
     * @throws NullPointerException if {@code filterRules} is null
     */
    public synchronized boolean registerAutoFilterRules(AutoFilterRule... filterRules) {
        return rules.addAll(Arrays.asList(filterRules));
    }

    /**
     * Unregisters an auto filter rule.
     * @param rule auto filter rule to remove. Must not be null
     * @return {@code true} if the list contained the specified rule
     * @throws NullPointerException if {@code rule} is null
     */
    public synchronized boolean unregisterAutoFilterRule(AutoFilterRule rule) {
        return rules.remove(Objects.requireNonNull(rule, "rule"));
    }

    /**
     * Returns the list of registered auto filter rules.
     * @return the list of registered rules
     */
    public synchronized List<AutoFilterRule> getAutoFilterRules() {
        return new ArrayList<>(rules);
    }

    /**
     * Returns the auto filter rule defined for the given OSM key.
     * @param key OSM key used to identify rule. Can't be null.
     * @return the auto filter rule defined for the given OSM key, or null
     * @throws NullPointerException if key is null
     */
    public synchronized AutoFilterRule getAutoFilterRule(String key) {
        return rules.stream()
                .filter(r -> Objects.equals(key, r.getKey()))
                .findFirst().orElse(null);
    }

    /**
     * Sets the currently enabled auto filter rule to the one defined for the given OSM key.
     * @param key OSM key used to identify new rule to enable. Null to disable the auto filter feature.
     */
    public synchronized void enableAutoFilterRule(String key) {
        enableAutoFilterRule(key == null ? null : getAutoFilterRule(key));
    }

    /**
     * Sets the currently enabled auto filter rule.
     * @param rule new rule to enable. Null to disable the auto filter feature.
     */
    public synchronized void enableAutoFilterRule(AutoFilterRule rule) {
        enabledRule = rule;
    }

    /**
     * Returns the currently selected auto filters, if any.
     * @return the currently selected auto filters. Can be empty.
     */
    public synchronized List<AutoFilter> getCurrentAutoFilters() {
        return currentAutoFilters;
    }

    /**
     * Returns a combination of all {@link #getCurrentAutoFilters()}, if any.
     * @return a single combined filter, or null
     */
    public Filter getCurrentCombinedFilter() {
        if (currentAutoFilters.isEmpty()) {
            return null;
        } else if (currentAutoFilters.size() == 1) {
            return currentAutoFilters.get(0).getFilter();
        } else {
            return new CombinedFilter(currentAutoFilters.stream().map(AutoFilter::getFilter).collect(Collectors.toList()));
        }
    }

    /**
     * Sets the currently selected auto filter, if any.
     * @param autoFilter the currently selected auto filter, or null
     */
    public synchronized void setCurrentAutoFilter(AutoFilter autoFilter) {
        currentAutoFilters.clear();
        if (autoFilter != null) {
            currentAutoFilters.add(autoFilter);
        }
        updateModelFilters();
    }

    public synchronized void addCurrentAutoFilter(AutoFilter autoFilter) {
        if (!currentAutoFilters.contains(autoFilter)) {
            currentAutoFilters.add(autoFilter);
            updateModelFilters();
        }
    }

    public synchronized void removeCurrentAutoFilter(AutoFilter autoFilter) {
        if (currentAutoFilters.contains(autoFilter)) {
            currentAutoFilters.removeIf(it -> Objects.equals(it, autoFilter));
            updateModelFilters();
        }
    }

    private synchronized void updateModelFilters() {
        model.clearFilters();
        if (currentAutoFilters.isEmpty()) {
            if (MainApplication.getMap() != null) {
                MainApplication.getMap().filterDialog.getFilterModel().executeFilters(true);
            }
        } else {
            model.addFilter(getCurrentCombinedFilter());
            model.executeFilters();
            // update the data layer (necessary even if model.isChanged() == false to update the OSDText)
            OsmDataLayer dataLayer = MainApplication.getLayerManager().getActiveDataLayer();
            if (dataLayer != null) {
                dataLayer.invalidate();
            }
        }
    }

    /**
     * Draws a text on the map display that indicates that filters are active.
     * @param g The graphics to draw that text on.
     */
    public synchronized void drawOSDText(Graphics2D g) {
        String filterText = Objects.requireNonNull(getCurrentCombinedFilter()).text;
        String lengthLimitedFilterText = filterText.length() > 18 ? filterText.substring(0, 18) + "…" : filterText;
        model.drawOSDText(g, lblOSD,
            tr("<h2>Filter active: {0}</h2>", lengthLimitedFilterText),
            tr("</p><p>Click again on filter button to see all objects.</p></html>"));
    }

    private void resetCurrentAutoFilter() {
        setCurrentAutoFilter(null);
        removeAllButtons();
        MapFrame map = MainApplication.getMap();
        if (map != null) {
            map.filterDialog.getFilterModel().executeFilters(true);
        }
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey().equals(PROP_AUTO_FILTER_ENABLED.getKey())) {
            if (PROP_AUTO_FILTER_ENABLED.get()) {
                enableAutoFilterRule(PROP_AUTO_FILTER_RULE.get());
                updateButtons();
            } else {
                enableAutoFilterRule((AutoFilterRule) null);
                resetCurrentAutoFilter();
            }
        } else if (e.getKey().equals(PROP_AUTO_FILTER_RULE.getKey())
                || e.getKey().equals(PROP_AUTO_FILTER_HIDING.getKey())) {
            enableAutoFilterRule(PROP_AUTO_FILTER_RULE.get());
            resetCurrentAutoFilter();
            updateButtons();
        }
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        // Do nothing
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (MainApplication.getLayerManager().getActiveDataLayer() == null) {
            resetCurrentAutoFilter();
        }
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // Do nothing
    }
}
