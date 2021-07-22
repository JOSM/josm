// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.template_engine.TemplateEngineDataProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

/**
 * Supporting class for creating the GUI for a preset item.
 *
 * @since 17609
 */
public final class TaggingPresetItemGuiSupport implements TemplateEngineDataProvider {

    private final Collection<OsmPrimitive> selected;
    private final boolean presetInitiallyMatches;
    private final Supplier<Collection<Tag>> changedTagsSupplier;
    private final ListenerList<ChangeListener> listeners = ListenerList.create();

    /**
     * Interface to notify listeners that a preset item input as changed.
     * @since 17610
     */
    public interface ChangeListener {
        /**
         * Notifies this listener that a preset item input as changed.
         * @param source the source of this event
         * @param key the tag key
         * @param newValue the new tag value
         */
        void itemValueModified(TaggingPresetItem source, String key, String newValue);
    }

    private TaggingPresetItemGuiSupport(
            boolean presetInitiallyMatches, Collection<OsmPrimitive> selected, Supplier<Collection<Tag>> changedTagsSupplier) {
        this.selected = selected;
        this.presetInitiallyMatches = presetInitiallyMatches;
        this.changedTagsSupplier = changedTagsSupplier;
    }

    /**
     * Returns the selected primitives
     *
     * @return the selected primitives
     */
    public Collection<OsmPrimitive> getSelected() {
        return selected;
    }

    /**
     * Returns whether the preset initially matched (before opening the dialog)
     *
     * @return whether the preset initially matched
     */
    public boolean isPresetInitiallyMatches() {
        return presetInitiallyMatches;
    }

    /**
     * Creates a new {@code TaggingPresetItemGuiSupport}
     *
     * @param presetInitiallyMatches whether the preset initially matched
     * @param selected the selected primitives
     * @param changedTagsSupplier the changed tags
     * @return the new {@code TaggingPresetItemGuiSupport}
     */
    public static TaggingPresetItemGuiSupport create(
            boolean presetInitiallyMatches, Collection<OsmPrimitive> selected, Supplier<Collection<Tag>> changedTagsSupplier) {
        return new TaggingPresetItemGuiSupport(presetInitiallyMatches, selected, changedTagsSupplier);
    }

    /**
     * Creates a new {@code TaggingPresetItemGuiSupport}
     *
     * @param presetInitiallyMatches whether the preset initially matched
     * @param selected the selected primitives
     * @return the new {@code TaggingPresetItemGuiSupport}
     */
    public static TaggingPresetItemGuiSupport create(
            boolean presetInitiallyMatches, OsmPrimitive... selected) {
        return new TaggingPresetItemGuiSupport(presetInitiallyMatches, Arrays.asList(selected), Collections::emptyList);
    }

    /**
     * Get tags with values as currently shown in the dialog.
     * If exactly one primitive is selected, get all tags of it, then 
     * overwrite with the current values shown in the dialog.
     * Else get only the tags shown in the dialog.
     * @return Tags
     */
    public Tagged getTagged() {
        if (selected.size() != 1) {
            return Tagged.ofTags(changedTagsSupplier.get());
        }
        // if there is only one primitive selected, get its tags
        Tagged tagged = Tagged.ofMap(selected.iterator().next().getKeys());
        // update changed tags
        changedTagsSupplier.get().forEach(tag -> tagged.put(tag));
        return tagged;
    }

    @Override
    public Collection<String> getTemplateKeys() {
        return getTagged().keySet();
    }

    @Override
    public Object getTemplateValue(String key, boolean special) {
        String value = getTagged().get(key);
        return (value == null || value.isEmpty()) ? null : value;
    }

    @Override
    public boolean evaluateCondition(SearchCompiler.Match condition) {
        return condition.match(getTagged());
    }

    /**
     * Adds a new change listener
     * @param listener the listener to add
     */
    public void addListener(ChangeListener listener) {
        listeners.addListener(listener);
    }

    /**
     * Notifies all listeners that a preset item input as changed.
     * @param source the source of this event
     * @param key the tag key
     * @param newValue the new tag value
     */
    public void fireItemValueModified(TaggingPresetItem source, String key, String newValue) {
        listeners.fireEvent(e -> e.itemValueModified(source, key, newValue));
    }
}
