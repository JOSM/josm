// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

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
     * Property to determines if the auto filter feature is enabled.
     */
    public static final BooleanProperty PROP_AUTO_FILTER_ENABLED = new BooleanProperty("auto.filter.enabled", true);

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
    private final Map<Integer, AutoFilterButton> buttons = new TreeMap<>();

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
     * The currently selected auto filter, if any.
     */
    private AutoFilter currentAutoFilter;

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
            // Make sure current auto filter button remains visible even if no data is found, to allow user to disable it
            if (currentAutoFilter != null) {
                values.add(currentAutoFilter.getFilter().value);
            }
            if (!values.equals(buttons.keySet())) {
                removeAllButtons();
                addNewButtons(values);
            }
        }
    }

    static class CompiledFilter extends Filter implements MatchSupplier {
        final AutoFilterRule rule;
        final int value;

        CompiledFilter(AutoFilterRule rule, int value) {
            this.rule = rule;
            this.value = value;
            this.enable = true;
            this.inverted = true;
            this.text = rule.getKey() + "=" + value;
        }

        @Override
        public SearchCompiler.Match get() {
            return new Match(rule, value);
        }
    }

    static class Match extends SearchCompiler.Match {
        final AutoFilterRule rule;
        final int value;

        Match(AutoFilterRule rule, int value) {
            this.rule = rule;
            this.value = value;
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return rule.getTagValuesForPrimitive(osm).anyMatch(v -> v == value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Match match = (Match) o;
            return value == match.value &&
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
        for (final Integer value : values.descendingSet()) {
            CompiledFilter filter = new CompiledFilter(enabledRule, value);
            String label = enabledRule.formatValue(value);
            AutoFilter autoFilter = new AutoFilter(label, filter.text, filter);
            AutoFilterButton button = new AutoFilterButton(autoFilter);
            if (autoFilter.equals(currentAutoFilter)) {
                button.getModel().setPressed(true);
            }
            maxWidth = Math.max(maxWidth, button.getPreferredSize().width);
            addButton(button, value, i++);
        }
        for (AutoFilterButton b : buttons.values()) {
            b.setSize(b == keyButton ? b.getPreferredSize().width : maxWidth, 20);
        }
        MainApplication.getMap().mapView.validate();
    }

    private void addButton(AutoFilterButton button, int value, int i) {
        MapView mapView = MainApplication.getMap().mapView;
        buttons.put(value, button);
        mapView.add(button).setLocation(3, 60 + 22*i);
    }

    private void removeAllButtons() {
        MapFrame map= MainApplication.getMap();
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
        Consumer<OsmPrimitive> consumer = o -> enabledRule.getTagValuesForPrimitive(o).forEach(values::add);
        ds.searchNodes(bbox).forEach(consumer);
        ds.searchWays(bbox).forEach(consumer);
        ds.searchRelations(bbox).forEach(consumer);
        return values;
    }

    @Override
    public void zoomChanged() {
        updateButtons();
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        updateFiltersFull();
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
        if (currentAutoFilter != null) {
            model.executeFilters();
        }
    }

    private synchronized void updateFiltersEvent(AbstractDatasetChangedEvent event, boolean affectedOnly) {
        if (currentAutoFilter != null) {
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
     * Returns the currently selected auto filter, if any.
     * @return the currently selected auto filter, or null
     */
    public synchronized AutoFilter getCurrentAutoFilter() {
        return currentAutoFilter;
    }

    /**
     * Sets the currently selected auto filter, if any.
     * @param autoFilter the currently selected auto filter, or null
     */
    public synchronized void setCurrentAutoFilter(AutoFilter autoFilter) {
        model.clearFilters();
        currentAutoFilter = autoFilter;
        if (autoFilter != null) {
            model.addFilter(autoFilter.getFilter());
            model.executeFilters();
            if (model.isChanged()) {
                OsmDataLayer dataLayer = MainApplication.getLayerManager().getActiveDataLayer();
                if (dataLayer != null) {
                    dataLayer.invalidate();
                }
            }
        }
    }

    /**
     * Draws a text on the map display that indicates that filters are active.
     * @param g The graphics to draw that text on.
     */
    public synchronized void drawOSDText(Graphics2D g) {
        model.drawOSDText(g, lblOSD,
            tr("<h2>Filter active: {0}</h2>", currentAutoFilter.getFilter().text),
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
        } else if (e.getKey().equals(PROP_AUTO_FILTER_RULE.getKey())) {
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
