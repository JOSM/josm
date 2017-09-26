// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.widgets.QuadStateCheckBox;
import org.openstreetmap.josm.tools.GBC;

/**
 * Checkbox type.
 */
public class Check extends KeyedItem {

    /** The localized version of {@link #text}. */
    public String locale_text; // NOSONAR
    /** the value to set when checked (default is "yes") */
    public String value_on = OsmUtils.TRUE_VALUE; // NOSONAR
    /** the value to set when unchecked (default is "no") */
    public String value_off = OsmUtils.FALSE_VALUE; // NOSONAR
    /** whether the off value is disabled in the dialog, i.e., only unset or yes are provided */
    public boolean disable_off; // NOSONAR
    /** "on" or "off" or unset (default is unset) */
    public String default_; // only used for tagless objects // NOSONAR

    private QuadStateCheckBox check;
    private QuadStateCheckBox.State initialState;
    private Boolean def;

    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {

        // find out if our key is already used in the selection.
        final Usage usage = determineBooleanUsage(sel, key);
        final String oneValue = usage.values.isEmpty() ? null : usage.values.last();
        def = "on".equals(default_) ? Boolean.TRUE : "off".equals(default_) ? Boolean.FALSE : null;

        if (locale_text == null) {
            locale_text = getLocaleText(text, text_context, null);
        }

        if (usage.values.size() < 2 && (oneValue == null || value_on.equals(oneValue) || value_off.equals(oneValue))) {
            if (def != null && !PROP_FILL_DEFAULT.get()) {
                // default is set and filling default values feature is disabled - check if all primitives are untagged
                for (OsmPrimitive s : sel) {
                    if (s.hasKeys()) {
                        def = null;
                    }
                }
            }

            // all selected objects share the same value which is either true or false or unset,
            // we can display a standard check box.
            initialState = value_on.equals(oneValue) || Boolean.TRUE.equals(def)
                    ? QuadStateCheckBox.State.SELECTED
                    : value_off.equals(oneValue) || Boolean.FALSE.equals(def)
                    ? QuadStateCheckBox.State.NOT_SELECTED
                    : QuadStateCheckBox.State.UNSET;

        } else {
            def = null;
            // the objects have different values, or one or more objects have something
            // else than true/false. we display a quad-state check box
            // in "partial" state.
            initialState = QuadStateCheckBox.State.PARTIAL;
        }

        final List<QuadStateCheckBox.State> allowedStates = new ArrayList<>(4);
        if (QuadStateCheckBox.State.PARTIAL.equals(initialState))
            allowedStates.add(QuadStateCheckBox.State.PARTIAL);
        allowedStates.add(QuadStateCheckBox.State.SELECTED);
        if (!disable_off || value_off.equals(oneValue))
            allowedStates.add(QuadStateCheckBox.State.NOT_SELECTED);
        allowedStates.add(QuadStateCheckBox.State.UNSET);
        check = new QuadStateCheckBox(locale_text, initialState,
                allowedStates.toArray(new QuadStateCheckBox.State[allowedStates.size()]));
        check.setPropertyText(key);
        check.setState(check.getState()); // to update the tooltip text

        p.add(check, GBC.eol()); // Do not fill, see #15104
        return true;
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
        // if the user hasn't changed anything, don't create a command.
        if (def == null && check.getState() == initialState) return;

        // otherwise change things according to the selected value.
        changedTags.add(new Tag(key,
                check.getState() == QuadStateCheckBox.State.SELECTED ? value_on :
                    check.getState() == QuadStateCheckBox.State.NOT_SELECTED ? value_off :
                        null));
    }

    @Override
    public MatchType getDefaultMatch() {
        return MatchType.NONE;
    }

    @Override
    public Collection<String> getValues() {
        return disable_off ? Arrays.asList(value_on) : Arrays.asList(value_on, value_off);
    }

    @Override
    public String toString() {
        return "Check ["
                + (locale_text != null ? "locale_text=" + locale_text + ", " : "")
                + (value_on != null ? "value_on=" + value_on + ", " : "")
                + (value_off != null ? "value_off=" + value_off + ", " : "")
                + "default_=" + default_ + ", "
                + (check != null ? "check=" + check + ", " : "")
                + (initialState != null ? "initialState=" + initialState
                        + ", " : "") + "def=" + def + ']';
    }
}
