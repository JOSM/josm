// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;

/**
 * Represents the server capabilities
 *
 * Example capabilites document:
 * <pre>
 * &lt;osm version="0.6" generator="OpenStreetMap server"&gt;
 *   &lt;api&gt;
 *     &lt;version minimum="0.6" maximum="0.6"/&gt;
 *     &lt;area maximum="0.25"/&gt;
 *     &lt;tracepoints per_page="5000"/&gt;
 *     &lt;waynodes maximum="2000"/&gt;
 *     &lt;changesets maximum_elements="50000"/&gt;
 *     &lt;timeout seconds="300"/&gt;
 *   &lt;/api&gt;
 *   &lt;policy&gt;
 *     &lt;imagery&gt;
 *       &lt;blacklist regex=".*\.google\.com/.*"/&gt;
 *       &lt;blacklist regex=".*209\.85\.2\d\d.*"/&gt;
 *       &lt;blacklist regex=".*209\.85\.1[3-9]\d.*"/&gt;
 *       &lt;blacklist regex=".*209\.85\.12[89].*"/&gt;
 *     &lt;/imagery&gt;
 *   &lt;/policy&gt;
 * &lt;/osm&gt;
 * </pre>
 * This class is used in conjunction with a very primitive parser
 * and simply stuffs the each tag and its attributes into a hash
 * of hashes, with the exception of the "blacklist" tag which gets
 * a list of its own. The DOM hierarchy is disregarded.
 */
public class Capabilities {

    private Map<String, HashMap<String,String>> capabilities;
    private List<String> imageryBlacklist;

    /**
     * Constructs new {@code Capabilities}.
     */
    public Capabilities() {
        clear();
    }

    public boolean isDefined(String element, String attribute) {
        if (! capabilities.containsKey(element)) return false;
        HashMap<String, String> e = capabilities.get(element);
        if (e == null) return false;
        return (e.get(attribute) != null);
    }

    public String get(String element, String attribute ) {
        if (! capabilities.containsKey(element)) return null;
        HashMap<String, String> e = capabilities.get(element);
        if (e == null) return null;
        return e.get(attribute);
    }

    /**
     * returns the value of configuration item in the capabilities as
     * double value
     *
     * @param element  the name of the element
     * @param attribute the name of the attribute
     * @return the value; null, if the respective configuration item doesn't exist
     * @throws NumberFormatException  if the value is not a valid double
     */
    public Double getDouble(String element, String attribute) throws NumberFormatException {
        String s = get(element, attribute);
        if (s == null) return null;
        return Double.parseDouble(s);
    }

    public Long getLong(String element, String attribute) {
        String s = get(element, attribute);
        if (s == null) return null;
        return Long.parseLong(s);
    }

    public void put(String element, String attribute, String value) {
        if (element.equals("blacklist")) {
            if (attribute.equals("regex")) {
                imageryBlacklist.add(value);
            }
        } else {
            if (! capabilities.containsKey(element))  {
                HashMap<String,String> h = new HashMap<String, String>();
                capabilities.put(element, h);
            }
            HashMap<String, String> e = capabilities.get(element);
            e.put(attribute, value);
        }
    }

    public void clear() {
        capabilities = new HashMap<String, HashMap<String,String>>();
        imageryBlacklist = new ArrayList<String>();
    }

    public boolean supportsVersion(String version) {
        return get("version", "minimum").compareTo(version) <= 0
        && get("version", "maximum").compareTo(version) >= 0;
    }

    /**
     * Returns the max number of objects in a changeset. -1 if either the capabilities
     * don't include this parameter or if the parameter value is illegal (not a number,
     * a negative number)
     *
     * @return the max number of objects in a changeset
     */
    public int getMaxChangesetSize() {
        String v = get("changesets", "maximum_elements");
        if (v == null) return -1;
        try {
            int n = Integer.parseInt(v);
            if (n <= 0) {
                Main.warn(tr("Illegal value of attribute ''{0}'' of element ''{1}'' in server capabilities. Got ''{2}''", "changesets", "maximum_elements", n ));
                return -1;
            }
            return n;
        } catch (NumberFormatException e) {
            Main.warn(tr("Illegal value of attribute ''{0}'' of element ''{1}'' in server capabilities. Got ''{2}''", "changesets", "maximum_elements", v ));
            return -1;
        }
    }

    /**
     * checks if the given URL is blacklisted by one of the of the
     * regular expressions.
     */

    public boolean isOnImageryBlacklist(String url)
    {
        if (url != null && imageryBlacklist != null) {
            for (String blacklistRegex : imageryBlacklist) {
                if (url.matches(blacklistRegex))
                    return true;
            }
        }
        return false;
    }

    /**
     * returns the full list of blacklist regular expressions.
     */
    public List<String> getImageryBlacklist()
    {
        return Collections.unmodifiableList(imageryBlacklist);
    }
}
