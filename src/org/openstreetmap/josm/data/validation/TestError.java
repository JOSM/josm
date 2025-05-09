// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.validation.util.MultipleNameVisitor;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.I18n;

/**
 * Validation error
 * @since 3669
 */
public class TestError implements Comparable<TestError> {
    /**
     * Used to switch users over to new ignore system, UNIQUE_CODE_MESSAGE_STATE
     * 1_704_067_200L → 2024-01-01
     * We can probably remove this and the supporting code in 2025.
     */
    private static boolean switchOver = Instant.now().isAfter(Instant.ofEpochMilli(1_704_067_200L));
    /** is this error on the ignore list */
    private boolean ignored;
    /** Severity */
    private final Severity severity;
    /** The error message */
    private final String message;
    /** Deeper error description */
    private final String description;
    private final String descriptionEn;
    /** The affected primitives */
    private final Collection<? extends OsmPrimitive> primitives;
    /** The primitives or way segments to be highlighted */
    private final Collection<?> highlighted;
    /** The tester that raised this error */
    private final Test tester;
    /** Internal code used by testers to classify errors */
    private final int code;
    /** Internal code used by testers to classify errors. Used for moving between JOSM versions. */
    private final int uniqueCode;
    /** If this error is selected */
    private boolean selected;
    /** If all relevant primitives are known*/
    private boolean incompletePrimitives;
    /** Supplying a command to fix the error */
    private final Supplier<Command> fixingCommand;

    /**
     * A builder for a {@code TestError}.
     * @since 11129
     */
    public static final class Builder {
        private final Test tester;
        private final Severity severity;
        private final int code;
        private final int uniqueCode;
        private String message;
        private String description;
        private String descriptionEn;
        private Collection<? extends OsmPrimitive> primitives;
        private Collection<?> highlighted;
        private Supplier<Command> fixingCommand;
        private boolean incompletePrimitives;

        Builder(Test tester, Severity severity, int code) {
            this.tester = tester;
            this.severity = severity;
            this.code = code;
            this.uniqueCode = this.tester != null ? this.tester.getClass().getName().hashCode() : code;
        }

        /**
         * Sets the error message.
         *
         * @param message The error message
         * @return {@code this}
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets the error message.
         *
         * @param message       The message of this error group
         * @param description   The translated description of this error
         * @param descriptionEn The English description (for ignoring errors)
         * @return {@code this}
         */
        public Builder messageWithManuallyTranslatedDescription(String message, String description, String descriptionEn) {
            this.message = message;
            this.description = description;
            this.descriptionEn = descriptionEn;
            return this;
        }

        /**
         * Sets the error message.
         *
         * @param message The message of this error group
         * @param marktrDescription The {@linkplain I18n#marktr prepared for i18n} description of this error
         * @param args The description arguments to be applied in {@link I18n#tr(String, Object...)}
         * @return {@code this}
         */
        public Builder message(String message, String marktrDescription, Object... args) {
            this.message = message;
            this.description = I18n.tr(marktrDescription, args);
            this.descriptionEn = new MessageFormat(marktrDescription, Locale.ENGLISH).format(args);
            return this;
        }

        /**
         * Sets the primitives affected by this error.
         *
         * @param primitives the primitives affected by this error
         * @return {@code this}
         */
        public Builder primitives(OsmPrimitive... primitives) {
            return primitives(Arrays.asList(primitives));
        }

        /**
         * Sets the primitives affected by this error.
         *
         * @param primitives the primitives affected by this error
         * @return {@code this}
         */
        public Builder primitives(Collection<? extends OsmPrimitive> primitives) {
            CheckParameterUtil.ensureThat(this.primitives == null, "primitives already set");
            CheckParameterUtil.ensureParameterNotNull(primitives, "primitives");
            this.primitives = primitives;
            if (this.highlighted == null) {
                this.highlighted = primitives;
            }
            return this;
        }

        /**
         * Sets the primitives to highlight when selecting this error.
         *
         * @param highlighted the primitives to highlight
         * @return {@code this}
         * @see ValidatorVisitor#visit(OsmPrimitive)
         */
        public Builder highlight(OsmPrimitive... highlighted) {
            return highlight(Arrays.asList(highlighted));
        }

        /**
         * Sets the primitives to highlight when selecting this error.
         *
         * @param highlighted the primitives to highlight
         * @return {@code this}
         * @see ValidatorVisitor#visit(OsmPrimitive)
         */
        public Builder highlight(Collection<? extends OsmPrimitive> highlighted) {
            CheckParameterUtil.ensureParameterNotNull(highlighted, "highlighted");
            this.highlighted = highlighted;
            return this;
        }

        /**
         * Sets the way segments to highlight when selecting this error.
         *
         * @param highlighted the way segments to highlight
         * @return {@code this}
         * @see ValidatorVisitor#visit(WaySegment)
         */
        public Builder highlightWaySegments(Collection<WaySegment> highlighted) {
            CheckParameterUtil.ensureParameterNotNull(highlighted, "highlighted");
            this.highlighted = highlighted;
            return this;
        }

        /**
         * Sets the node pairs to highlight when selecting this error.
         *
         * @param highlighted the node pairs to highlight
         * @return {@code this}
         * @see ValidatorVisitor#visit(List)
         */
        public Builder highlightNodePairs(Collection<List<Node>> highlighted) {
            CheckParameterUtil.ensureParameterNotNull(highlighted, "highlighted");
            this.highlighted = highlighted;
            return this;
        }

        /**
         * Sets an area to highlight when selecting this error.
         *
         * @param highlighted the area to highlight
         * @return {@code this}
         */
        public Builder highlight(Area highlighted) {
            CheckParameterUtil.ensureParameterNotNull(highlighted, "highlighted");
            this.highlighted = Collections.singleton(highlighted);
            return this;
        }

        /**
         * Sets a flag that the list of primitives may be incomplete. See #23397
         *
         * @return {@code this}
         */
        public Builder imcompletePrimitives() {
            this.incompletePrimitives = true;
            return this;
        }

        /**
         * Sets a supplier to obtain a command to fix the error.
         *
         * @param fixingCommand the fix supplier. Can be null
         * @return {@code this}
         */
        public Builder fix(Supplier<Command> fixingCommand) {
            CheckParameterUtil.ensureThat(this.fixingCommand == null, "fixingCommand already set");
            this.fixingCommand = fixingCommand;
            return this;
        }

        /**
         * Returns a new test error with the specified values
         *
         * @return a new test error with the specified values
         * @throws IllegalArgumentException when {@link #message} or {@link #primitives} is null.
         */
        public TestError build() {
            CheckParameterUtil.ensureParameterNotNull(message, "message not set");
            CheckParameterUtil.ensureParameterNotNull(primitives, "primitives not set");
            if (this.highlighted == null) {
                this.highlighted = Collections.emptySet();
            }
            return new TestError(this);
        }
    }

    /**
     * Update error codes on read and save. Used for tests.
     * @param updateErrorCodes {@code true} to update error codes. See {@link #switchOver} for default.
     */
    static void setUpdateErrorCodes(boolean updateErrorCodes) {
        switchOver = updateErrorCodes;
    }

    /**
     * Starts building a new {@code TestError}
     * @param tester The tester
     * @param severity The severity of this error
     * @param code The test error reference code
     * @return a new test builder
     * @since 11129
     */
    public static Builder builder(Test tester, Severity severity, int code) {
        return new Builder(tester, severity, code);
    }

    TestError(Builder builder) {
        this.tester = builder.tester;
        this.severity = builder.severity;
        this.message = builder.message;
        this.description = builder.description;
        this.descriptionEn = builder.descriptionEn;
        this.primitives = builder.primitives;
        this.highlighted = builder.highlighted;
        this.code = builder.code;
        this.uniqueCode = builder.uniqueCode;
        this.fixingCommand = builder.fixingCommand;
        this.incompletePrimitives = builder.incompletePrimitives;
    }

    /**
     * Gets the error message
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the error message
     * @return the error description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the list of primitives affected by this error
     * @return the list of primitives affected by this error
     */
    public Collection<? extends OsmPrimitive> getPrimitives() {
        return Collections.unmodifiableCollection(primitives);
    }

    /**
     * Gets all primitives of the given type affected by this error
     * @param type restrict primitives to subclasses
     * @param <T> type of primitives
     * @return the primitives as Stream
     */
    public final <T extends OsmPrimitive> Stream<T> primitives(Class<T> type) {
        return primitives.stream()
                .filter(type::isInstance)
                .map(type::cast);
    }

    /**
     * Gets the severity of this error
     * @return the severity of this error
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Returns the ignore state for this error.
     * @return the ignore state for this error or null if any primitive is new
     */
    public String getIgnoreState() {
        return getIgnoreState(false);
    }

    /**
     * Get the ignore state
     * @param useOriginal if {@code true}, use the original code to get the ignore state
     * @return The ignore state ({@link #getIgnoreGroup} + ignored object list)
     */
    private String getIgnoreState(boolean useOriginal) {
        Collection<String> strings = new TreeSet<>();
        for (OsmPrimitive o : primitives) {
            // ignore data not yet uploaded
            if (o.isNew())
                return null;
            String type = "u";
            if (o instanceof Way) {
                type = "w";
            } else if (o instanceof Relation) {
                type = "r";
            } else if (o instanceof Node) {
                type = "n";
            }
            strings.add(type + '_' + o.getId());
        }
        return strings.stream().map(o -> ':' + o).collect(Collectors.joining("", getIgnoreSubGroup(useOriginal), ""));
    }

    /**
     * Check if this error matches an entry in the ignore list and
     * set the ignored flag if it is.
     * @return the new ignored state
     */
    public boolean updateIgnored() {
        setIgnored(calcIgnored());
        return isIgnored();
    }

    private boolean calcIgnored() {
        // Begin code removal section (backwards compatibility)
        if (OsmValidator.hasIgnoredError(getIgnoreGroup(true))) {
            updateIgnoreList(getIgnoreGroup(true), getIgnoreGroup(false));
            return true;
        }
        if (OsmValidator.hasIgnoredError(getIgnoreSubGroup(true))) {
            updateIgnoreList(getIgnoreSubGroup(true), getIgnoreSubGroup(false));
            return true;
        }
        String oldState = getIgnoreState(true);
        String state = getIgnoreState(false);
        if (oldState != null && OsmValidator.hasIgnoredError(oldState)) {
            updateIgnoreList(oldState, state);
            return true;
        }
        // End code removal section
        if (OsmValidator.hasIgnoredError(getIgnoreGroup()))
            return true;
        if (OsmValidator.hasIgnoredError(getIgnoreSubGroup()))
            return true;
        return state != null && OsmValidator.hasIgnoredError(state);
    }

    /**
     * Convert old keys to new keys. Only takes effect when {@link #switchOver} is true
     * @param oldKey The key to replace
     * @param newKey The new key
     */
    private static void updateIgnoreList(String oldKey, String newKey) {
        if (switchOver) {
            Map<String, String> errors = OsmValidator.getIgnoredErrors();
            if (errors.containsKey(oldKey)) {
                String value = errors.remove(oldKey);
                errors.put(newKey, value);
            }
        }
    }

    /**
     * Gets the ignores subgroup that is more specialized than {@link #getIgnoreGroup()}
     * @return The ignore sub group
     */
    public String getIgnoreSubGroup() {
        return getIgnoreSubGroup(false);
    }

    /**
     * Get the subgroup for the error
     * @param useOriginal if {@code true}, use the original code instead of the new unique codes.
     * @return The ignore subgroup
     */
    private String getIgnoreSubGroup(boolean useOriginal) {
        if (code == 3000) {
            // see #19053
            return "3000_" + (description == null ? message : description);
        }
        String ignorestring = getIgnoreGroup(useOriginal);
        if (descriptionEn != null) {
            ignorestring += '_' + descriptionEn;
        }
        return ignorestring;
    }

    /**
     * Gets the ignore group ID that is used to allow the user to ignore all same errors
     * @return The group id
     * @see TestError#getIgnoreSubGroup()
     */
    public String getIgnoreGroup() {
        return getIgnoreGroup(false);
    }

    /**
     * Get the ignore group
     * @param useOriginal if {@code true}, use the original code instead of a unique code + original code.
     *                    Used for reading and understanding old ignore groups.
     * @return The ignore group.
     */
    private String getIgnoreGroup(boolean useOriginal) {
        if (code == 3000) {
            // see #19053
            return "3000_" + getMessage();
        }
        if (useOriginal) {
            return Integer.toString(this.code);
        }
        return this.uniqueCode + "_" + this.code;
    }

    /**
     * Flags this error as ignored
     * @param state The ignore flag
     */
    public void setIgnored(boolean state) {
        ignored = state;
    }

    /**
     * Checks if this error is ignored
     * @return <code>true</code> if it is ignored
     */
    public boolean isIgnored() {
        return ignored;
    }

    /**
     * Gets the tester that raised this error
     * @return the tester that raised this error
     */
    public Test getTester() {
        return tester;
    }

    /**
     * Gets the code
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get the unique code for this test. Used for ignore lists.
     * @return The unique code (generated with {@code tester.getClass().getName().hashCode() + code}).
     * @since 18636
     */
    public int getUniqueCode() {
        return this.uniqueCode;
    }

    /**
     * Returns true if the error can be fixed automatically
     *
     * @return true if the error can be fixed
     */
    public boolean isFixable() {
        return (fixingCommand != null || ((tester != null) && tester.isFixable(this)))
                && OsmUtils.isOsmCollectionEditable(primitives);
    }

    /**
     * Fixes the error with the appropriate command
     *
     * @return The command to fix the error
     */
    public Command getFix() {
        // obtain fix from the error
        final Command fix = fixingCommand != null ? fixingCommand.get() : null;
        if (fix != null) {
            return fix;
        }

        // obtain fix from the tester
        if (tester == null || !tester.isFixable(this) || primitives.isEmpty())
            return null;

        return tester.fixError(this);
    }

    /**
     * Sets the selection flag of this error
     * @param selected if this error is selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Visits all highlighted validation elements
     * @param v The visitor that should receive a visit-notification on all highlighted elements
     */
    @SuppressWarnings("unchecked")
    public void visitHighlighted(ValidatorVisitor v) {
        for (Object o : highlighted) {
            if (o instanceof OsmPrimitive) {
                v.visit((OsmPrimitive) o);
            } else if (o instanceof WaySegment) {
                v.visit((WaySegment) o);
            } else if (o instanceof List<?>) {
                v.visit((List<Node>) o);
            } else if (o instanceof Area) {
                for (List<Node> l : getHiliteNodesForArea((Area) o)) {
                    v.visit(l);
                }
            }
        }
    }

    /**
     * Calculate list of node pairs describing the area.
     * @param area the area
     * @return list of node pairs describing the area
     */
    private static List<List<Node>> getHiliteNodesForArea(Area area) {
        List<List<Node>> hilite = new ArrayList<>();
        PathIterator pit = area.getPathIterator(null);
        double[] res = new double[6];
        List<Node> nodes = new ArrayList<>();
        while (!pit.isDone()) {
            int type = pit.currentSegment(res);
            Node n = new Node(new EastNorth(res[0], res[1]));
            switch (type) {
            case PathIterator.SEG_MOVETO:
                if (!nodes.isEmpty()) {
                    hilite.add(nodes);
                }
                nodes = new ArrayList<>();
                nodes.add(n);
                break;
            case PathIterator.SEG_LINETO:
                nodes.add(n);
                break;
            case PathIterator.SEG_CLOSE:
                if (!nodes.isEmpty()) {
                    nodes.add(nodes.get(0));
                    hilite.add(nodes);
                    nodes = new ArrayList<>();
                }
                break;
            default:
                break;
            }
            pit.next();
        }
        if (nodes.size() > 1) {
            hilite.add(nodes);
        }
        return hilite;
    }

    /**
     * Returns the selection flag of this error
     * @return true if this error is selected
     * @since 5671
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Returns The primitives or way segments to be highlighted
     * @return The primitives or way segments to be highlighted
     * @since 5671
     */
    public Collection<?> getHighlighted() {
        return Collections.unmodifiableCollection(highlighted);
    }

    @Override
    public int compareTo(TestError o) {
        if (equals(o)) return 0;

        return AlphanumComparator.getInstance().compare(getNameVisitor().toString(), o.getNameVisitor().toString());
    }

    /**
     * Returns a new {@link MultipleNameVisitor} for the list of primitives affected by this error.
     * @return Name visitor (used in cell renderer and for sorting)
     */
    public MultipleNameVisitor getNameVisitor() {
        MultipleNameVisitor v = new MultipleNameVisitor();
        v.visit(getPrimitives());
        return v;
    }

    /**
     * Tests if two errors are similar, i.e.,
     * same code and description and same combination of primitives and same combination of highlighted objects, but maybe with different orders.
     * @param other the other error to be compared
     * @return true if two errors are similar
     */
    public boolean isSimilar(TestError other) {
        return getUniqueCode() == other.getUniqueCode()
                && getCode() == other.getCode()
                && getMessage().equals(other.getMessage())
                && getPrimitives().size() == other.getPrimitives().size()
                && getPrimitives().containsAll(other.getPrimitives())
                && highlightedIsEqual(getHighlighted(), other.getHighlighted());
    }

    private static boolean highlightedIsEqual(Collection<?> highlighted, Collection<?> highlighted2) {
        if (highlighted.size() == highlighted2.size()) {
            if (!highlighted.isEmpty()) {
                Object h1 = highlighted.iterator().next();
                Object h2 = highlighted2.iterator().next();
                if (h1 instanceof Area && h2 instanceof Area) {
                    return ((Area) h1).equals((Area) h2);
                }
                return highlighted.containsAll(highlighted2);
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "TestError [tester=" + tester + ", unique code=" + this.uniqueCode +
                ", code=" + code + ", message=" + message + ']';
    }

    /**
     * Check if any of the primitives in this error occurs in the given set of primitives.
     * @param given the set of primitives
     * @return true if any of the primitives in this error occurs in the given set of primitives, else false
     * @since 18960
     */
    public boolean isConcerned(Set<? extends OsmPrimitive> given) {
        if (incompletePrimitives)
            return true;
        for (OsmPrimitive p : getPrimitives()) {
            if (given.contains(p)) {
                return true;
            }
        }
        return false;
    }
}
