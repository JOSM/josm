// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

import static org.openstreetmap.josm.tools.I18n.trc;

import java.util.Objects;

import org.openstreetmap.josm.tools.Logging;

/**
 * This class defines a set of parameters that is used to
 * perform search within the search dialog.
 * @since 12659 (extracted from {@code SearchAction})
 */
public class SearchSetting {
    /** Search text */
    public String text;
    /** Search mode */
    public SearchMode mode;
    /** {@code true} to perform a case-sensitive search */
    public boolean caseSensitive;
    /** {@code true} to perform a regex-based search */
    public boolean regexSearch;
    /** {@code true} to execute a MapCSS selector */
    public boolean mapCSSSearch;
    /** {@code true} to include all objects (even incomplete and deleted ones) */
    public boolean allElements;

    /**
     * Constructs a new {@code SearchSetting}.
     */
    public SearchSetting() {
        text = "";
        mode = SearchMode.replace;
    }

    /**
     * Constructs a new {@code SearchSetting} from an existing one.
     * @param original original search settings
     */
    public SearchSetting(SearchSetting original) {
        text = original.text;
        mode = original.mode;
        caseSensitive = original.caseSensitive;
        regexSearch = original.regexSearch;
        mapCSSSearch = original.mapCSSSearch;
        allElements = original.allElements;
    }

    @Override
    public String toString() {
        String cs = caseSensitive ?
                /*case sensitive*/  trc("search", "CS") :
                    /*case insensitive*/  trc("search", "CI");
        String rx = regexSearch ? ", " +
                        /*regex search*/ trc("search", "RX") : "";
        String css = mapCSSSearch ? ", " +
                        /*MapCSS search*/ trc("search", "CSS") : "";
        String all = allElements ? ", " +
                        /*all elements*/ trc("search", "A") : "";
        return '"' + text + "\" (" + cs + rx + css + all + ", " + mode + ')';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        SearchSetting that = (SearchSetting) other;
        return caseSensitive == that.caseSensitive &&
                regexSearch == that.regexSearch &&
                mapCSSSearch == that.mapCSSSearch &&
                allElements == that.allElements &&
                mode == that.mode &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, mode, caseSensitive, regexSearch, mapCSSSearch, allElements);
    }

    /**
     * <p>Transforms a string following a certain format, namely "[R | A | D | S][C?,R?,A?,M?] [a-zA-Z]"
     * where the first part defines the mode of the search, see {@link SearchMode}, the second defines
     * a set of attributes within the {@code SearchSetting} class and the second is the search query.
     * <p>
     * Attributes are as follows:
     * <ul>
     *     <li>C - if search is case sensitive
     *     <li>R - if the regex syntax is used
     *     <li>A - if all objects are considered
     *     <li>M - if the mapCSS syntax is used
     * </ul>
     * <p>For example, "RC type:node" is a valid string representation of an object that replaces the
     * current selection, is case sensitive and searches for all objects of type node.
     * @param s A string representation of a {@code SearchSetting} object
     *          from which the object must be built.
     * @return A {@code SearchSetting} defined by the input string.
     */
    public static SearchSetting readFromString(String s) {
        if (s.isEmpty())
            return null;

        SearchSetting result = new SearchSetting();

        int index = 1;

        result.mode = SearchMode.fromCode(s.charAt(0));
        if (result.mode == null) {
            result.mode = SearchMode.replace;
            index = 0;
        }

        while (index < s.length()) {
            if (s.charAt(index) == 'C') {
                result.caseSensitive = true;
            } else if (s.charAt(index) == 'R') {
                result.regexSearch = true;
            } else if (s.charAt(index) == 'A') {
                result.allElements = true;
            } else if (s.charAt(index) == 'M') {
                result.mapCSSSearch = true;
            } else if (s.charAt(index) == ' ') {
                break;
            } else {
                Logging.warn("Unknown char in SearchSettings: " + s);
                break;
            }
            index++;
        }

        if (index < s.length() && s.charAt(index) == ' ') {
            index++;
        }

        result.text = s.substring(index);

        return result;
    }

    /**
     * Builds a string representation of the {@code SearchSetting} object,
     * see {@link #readFromString(String)} for more details.
     * @return A string representation of the {@code SearchSetting} object.
     */
    public String writeToString() {
        if (text == null || text.isEmpty())
            return "";

        StringBuilder result = new StringBuilder();
        result.append(mode.getCode());
        if (caseSensitive) {
            result.append('C');
        }
        if (regexSearch) {
            result.append('R');
        }
        if (mapCSSSearch) {
            result.append('M');
        }
        if (allElements) {
            result.append('A');
        }
        result.append(' ')
              .append(text);
        return result.toString();
    }
}
