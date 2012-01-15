// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimplePrimitiveId implements PrimitiveId, Serializable {
    private final long id;
    private final OsmPrimitiveType type;

    public SimplePrimitiveId(long id, OsmPrimitiveType type) {
        this.id = id;
        this.type = type;
    }

    public OsmPrimitiveType getType() {
        return type;
    }

    public long getUniqueId() {
        return id;
    }

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
     * Parses a {@code OsmPrimitiveType} from the string {@code s}.
     * @param s the string to be parsed, e.g., {@code n1}, {@code node1},
     * {@code w1}, {@code way1}, {@code r1}, {@code rel1}, {@code relation1}.
     * @return the parsed {@code OsmPrimitiveType}
     * @throws IllegalArgumentException if the string does not match the pattern
     */
    public static SimplePrimitiveId fromString(String s) {
        final Pattern p = Pattern.compile("((n(ode)?|w(ay)?|r(el(ation)?)?)/?)(\\d+)");
        final Matcher m = p.matcher(s);
        if (m.matches()) {
            return new SimplePrimitiveId(Long.parseLong(m.group(m.groupCount())),
                    s.charAt(0) == 'n' ? OsmPrimitiveType.NODE
                    : s.charAt(0) == 'w' ? OsmPrimitiveType.WAY
                    : OsmPrimitiveType.RELATION);
        } else {
            throw new IllegalArgumentException("The string " + s + " does not match the pattern " + p);
        }
    }
}
