// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimplePrimitiveId implements PrimitiveId, Serializable {
    private final long id;
    private final OsmPrimitiveType type;

    public static final Pattern ID_PATTERN = Pattern.compile("((n(ode)?|w(ay)?|r(el(ation)?)?)/?)(\\d+)");

    public SimplePrimitiveId(long id, OsmPrimitiveType type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public OsmPrimitiveType getType() {
        return type;
    }

    @Override
    public long getUniqueId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return id <= 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimplePrimitiveId other = (SimplePrimitiveId) obj;
        if (id != other.id)
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return type + " " + id;
    }

    /**
     * Parses a {@code SimplePrimitiveId} from the string {@code s}.
     * @param s the string to be parsed, e.g., {@code n1}, {@code node1},
     * {@code w1}, {@code way1}, {@code r1}, {@code rel1}, {@code relation1}.
     * @return the parsed {@code SimplePrimitiveId}
     * @throws IllegalArgumentException if the string does not match the pattern
     */
    public static SimplePrimitiveId fromString(String s) {
        final Matcher m = ID_PATTERN.matcher(s);
        if (m.matches()) {
            return new SimplePrimitiveId(Long.parseLong(m.group(m.groupCount())),
                    s.charAt(0) == 'n'
                            ? OsmPrimitiveType.NODE
                            : s.charAt(0) == 'w'
                            ? OsmPrimitiveType.WAY
                            : OsmPrimitiveType.RELATION);
        } else {
            throw new IllegalArgumentException("The string " + s + " does not match the pattern " + ID_PATTERN);
        }
    }

    /**
     * Attempts to parse extract any primitive id from the string {@code s}.
     * @param s the string to be parsed, e.g., {@code n1, w1}, {@code node1 and rel2}.
     * @return the parsed list of {@code OsmPrimitiveType}s.
     */
    public static List<SimplePrimitiveId> fuzzyParse(String s) {
        final ArrayList<SimplePrimitiveId> ids = new ArrayList<SimplePrimitiveId>();
        final Matcher m = ID_PATTERN.matcher(s);
        while (m.find()) {
            final char firstChar = s.charAt(m.start());
            ids.add(new SimplePrimitiveId(Long.parseLong(m.group(m.groupCount())),
                    firstChar == 'n'
                            ? OsmPrimitiveType.NODE
                            : firstChar == 'w'
                            ? OsmPrimitiveType.WAY
                            : OsmPrimitiveType.RELATION));
        }
        return ids;
    }
}
