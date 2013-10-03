// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.Utils.equal;

import org.openstreetmap.josm.actions.search.SearchAction.SearchMode;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.data.Preferences.pref;
import org.openstreetmap.josm.data.Preferences.writeExplicitly;

/**
 *
 * @author Petr_Dlouhý
 */
public class Filter extends SearchSetting {
    private static final String version = "1";

    public boolean enable = true;
    public boolean hiding = false;
    public boolean inverted = false;

    /**
     * Constructs a new {@code Filter}.
     */
    public Filter() {
        super("", SearchMode.add, false, false, false);
    }
    
    public Filter(String text, SearchMode mode, boolean caseSensitive,
            boolean regexSearch, boolean allElements) {
        super(text, mode, caseSensitive, regexSearch, allElements);
    }

    public Filter(FilterPreferenceEntry e) {
        super(e.text, SearchMode.add, false, false, false);
        if (equal(e.mode, "replace")) {
            mode = SearchMode.replace;
        } else if (equal(e.mode, "add")) {
            mode = SearchMode.add;
        } else if (equal(e.mode, "remove")) {
            mode = SearchMode.remove;
        } else  if (equal(e.mode, "in_selection")) {
            mode = SearchMode.in_selection;
        }
        caseSensitive = e.case_sensitive;
        regexSearch = e.regex_search;
        enable = e.enable;
        hiding = e.hiding;
        inverted = e.inverted;
    }

    public static class FilterPreferenceEntry {
        @pref @writeExplicitly public String version = "1";
        @pref public String text = null;
        @pref @writeExplicitly public String mode = "add";
        @pref public boolean case_sensitive = false;
        @pref public boolean regex_search = false;
        @pref @writeExplicitly public boolean enable = true;
        @pref @writeExplicitly public boolean hiding = false;
        @pref @writeExplicitly public boolean inverted = false;
    }

    public FilterPreferenceEntry getPreferenceEntry() {
        FilterPreferenceEntry e = new FilterPreferenceEntry();
        e.version = version;
        e.text = text;
        e.mode = mode.toString();
        e.case_sensitive = caseSensitive;
        e.regex_search = regexSearch;
        e.enable = enable;
        e.hiding = hiding;
        e.inverted = inverted;
        return e;
    }
}
