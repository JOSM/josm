// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
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
import org.openstreetmap.josm.tools.Logging;

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
    private final Map<String, AutoFilterButton> buttons = new TreeMap<>();

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
    private AutoFilterRule enabledRule;

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
        Main.pref.addPreferenceChangeListener(this);
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
            NavigableSet<String> values = getNumericValues(enabledRule.getKey(), enabledRule.getValueComparator());
            // Make sure current auto filter button remains visible even if no data is found, to allow user to disable it
            if (currentAutoFilter != null) {
                values.add(currentAutoFilter.getFilter().text.split("=")[1]);
            }
            if (!values.equals(buttons.keySet())) {
                removeAllButtons();
                addNewButtons(values);
            }
        }
    }

    private synchronized void addNewButtons(NavigableSet<String> values) {
        int i = 0;
        int maxWidth = 16;
        MapView mapView = MainApplication.getMap().mapView;
        for (final String value : values.descendingSet()) {
            Filter filter = new Filter();
            filter.enable = true;
            filter.inverted = true;
            filter.text = enabledRule.getKey() + "=" + value;
            String label = enabledRule.getValueFormatter().apply(value);
            AutoFilter autoFilter = new AutoFilter(label, filter.text, filter);
            AutoFilterButton button = new AutoFilterButton(autoFilter);
            if (autoFilter.equals(currentAutoFilter)) {
                button.getModel().setPressed(true);
            }
            buttons.put(value, button);
            maxWidth = Math.max(maxWidth, button.getPreferredSize().width);
            mapView.add(button).setLocation(3, 60 + 22*i++);
        }
        for (AutoFilterButton b : buttons.values()) {
            b.setSize(maxWidth, 20);
        }
        mapView.validate();
    }

    private void removeAllButtons() {
        for (Iterator<String> it = buttons.keySet().iterator(); it.hasNext();) {
            MainApplication.getMap().mapView.remove(buttons.get(it.next()));
            it.remove();
        }
    }

    private static NavigableSet<String> getNumericValues(String key, Comparator<String> comparator) {
        NavigableSet<String> values = new TreeSet<>(comparator);
        for (String s : getTagValues(key)) {
            try {
                Integer.parseInt(s);
                values.add(s);
            } catch (NumberFormatException e) {
                Logging.trace(e);
            }
        }
        return values;
    }

    private static Set<String> getTagValues(String key) {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        Set<String> values = new TreeSet<>();
        if (ds != null) {
            BBox bbox = MainApplication.getMap().mapView.getState().getViewArea().getLatLonBoundsBox().toBBox();
            Consumer<OsmPrimitive> consumer = getTagValuesConsumer(key, values);
            ds.searchNodes(bbox).forEach(consumer);
            ds.searchWays(bbox).forEach(consumer);
            ds.searchRelations(bbox).forEach(consumer);
        }
        return values;
    }

    static Consumer<OsmPrimitive> getTagValuesConsumer(String key, Set<String> values) {
        return o -> {
            String value = o.get(key);
            if (value != null) {
                Pattern p = Pattern.compile("(-?[0-9]+)-(-?[0-9]+)");
                for (String v : value.split(";")) {
                    Matcher m = p.matcher(v);
                    if (m.matches()) {
                        int a = Integer.parseInt(m.group(1));
                        int b = Integer.parseInt(m.group(2));
                        for (int i = Math.min(a, b); i <= Math.max(a, b); i++) {
                            values.add(Integer.toString(i));
                        }
                    } else {
                        values.add(v);
                    }
                }
            }
        };
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
        for (AutoFilterRule r : rules) {
            if (key.equals(r.getKey())) {
                return r;
            }
        }
        return null;
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
                OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
                if (editLayer != null) {
                    editLayer.invalidate();
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
            map.filterDialog.getFilterModel().executeFilters();
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
        if (MainApplication.getLayerManager().getEditLayer() == null) {
            resetCurrentAutoFilter();
        }
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // Do nothing
    }
}
