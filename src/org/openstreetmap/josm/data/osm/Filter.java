// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

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
        super();
        mode = SearchMode.add;
    }

    public Filter(FilterPreferenceEntry e) {
        this();
        text = e.text;
        if ("replace".equals(e.mode)) {
            mode = SearchMode.replace;
        } else if ("add".equals(e.mode)) {
            mode = SearchMode.add;
        } else if ("remove".equals(e.mode)) {
            mode = SearchMode.remove;
        } else  if ("in_selection".equals(e.mode)) {
            mode = SearchMode.in_selection;
        }
        caseSensitive = e.case_sensitive;
        regexSearch = e.regex_search;
        mapCSSSearch = e.mapCSS_search;
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
        @pref public boolean mapCSS_search = false;
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
        e.mapCSS_search = mapCSSSearch;
        e.enable = enable;
        e.hiding = hiding;
        e.inverted = inverted;
        return e;
    }
}
