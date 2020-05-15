// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A primitive id and a primitive type
 */
public class SimplePrimitiveId implements PrimitiveId, Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final OsmPrimitiveType type;

    /**
     * A pattern that is used to parse a textual primitive id
     */
    public static final Pattern ID_PATTERN = Pattern.compile("(n|node|w|way|r|rel|relation)[ /]?(\\d+)");

    /**
     * A pattern that is used to parse an id range
     */
    public static final Pattern MULTIPLE_IDS_PATTERN = Pattern.compile(ID_PATTERN.pattern() + "(-(\\d+))?");

    /**
     * Create a new primitive id
     * @param id The id
     * @param type The type of the primitive
     */
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
        return Objects.hash(id, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SimplePrimitiveId that = (SimplePrimitiveId) obj;
        return id == that.id &&
                type == that.type;
    }

    @Override
    public String toString() {
        return type.toString() + ' ' + id;
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
            return new SimplePrimitiveId(Long.parseLong(m.group(m.groupCount())), getOsmPrimitiveType(s.charAt(0)));
        } else {
            throw new IllegalArgumentException("The string " + s + " does not match the pattern " + ID_PATTERN);
        }
    }

    /**
     * Parses a range {@code SimplePrimitiveId} from the string {@code s}.
     * @param s the string to be parsed, e.g., {@code node1}, {@code node1-7}, {@code node70-7}.
     * @return the parsed {@code SimplePrimitiveId}s
     * @throws IllegalArgumentException if the string does not match the pattern
     */
    public static List<SimplePrimitiveId> multipleFromString(String s) {
        final Matcher m = MULTIPLE_IDS_PATTERN.matcher(s);
        if (m.matches()) {
            return extractIdsInto(m, new ArrayList<SimplePrimitiveId>());
        } else {
            throw new IllegalArgumentException("The string " + s + " does not match the pattern " + MULTIPLE_IDS_PATTERN);
        }
    }

    /**
     * Attempts to parse extract any primitive id from the string {@code s}.
     * @param s the string to be parsed, e.g., {@code "n1, w1"}, {@code "node1 and rel2"}, {@code "node 123-29"}.
     * @return the parsed list of {@code OsmPrimitiveType}s.
     */
    public static List<SimplePrimitiveId> fuzzyParse(String s) {
        final List<SimplePrimitiveId> ids = new ArrayList<>();
        final Matcher m = MULTIPLE_IDS_PATTERN.matcher(s);
        while (m.find()) {
            extractIdsInto(m, ids);
        }
        return ids;
    }

    private static List<SimplePrimitiveId> extractIdsInto(MatchResult m, List<SimplePrimitiveId> ids) {
        final OsmPrimitiveType type = getOsmPrimitiveType(m.group(1).charAt(0));
        final String firstId = m.group(2);
        final String lastId = m.group(4);
        if (lastId != null) {
            final long lastIdParsed;
            if (lastId.length() < firstId.length()) {
                // parse ranges such as 123-25 or 123-5
                lastIdParsed = Long.parseLong(firstId.substring(0, firstId.length() - lastId.length()) + lastId);
            } else {
                // parse ranges such as 123-125 or 998-1001
                lastIdParsed = Long.parseLong(lastId);
            }
            for (long i = Long.parseLong(firstId); i <= lastIdParsed; i++) {
                if (i > 0) {
                    ids.add(new SimplePrimitiveId(i, type));
                }
            }
        } else {
            long i = Long.parseLong(firstId);
            if (i > 0) {
                ids.add(new SimplePrimitiveId(i, type));
            }
        }
        return ids;
    }

    private static OsmPrimitiveType getOsmPrimitiveType(char firstChar) {
        return firstChar == 'n' ? OsmPrimitiveType.NODE : firstChar == 'w' ? OsmPrimitiveType.WAY : OsmPrimitiveType.RELATION;
    }
}
