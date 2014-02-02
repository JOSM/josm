// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.validation.util.MultipleNameVisitor;
import org.openstreetmap.josm.tools.AlphanumComparator;

/**
 * Validation error
 * @since 3669
 */
public class TestError implements Comparable<TestError>, DataSetListener {
    /** is this error on the ignore list */
    private Boolean ignored = false;
    /** Severity */
    private Severity severity;
    /** The error message */
    private String message;
    /** Deeper error description */
    private String description;
    private String description_en;
    /** The affected primitives */
    private Collection<? extends OsmPrimitive> primitives;
    /** The primitives or way segments to be highlighted */
    private Collection<?> highlighted;
    /** The tester that raised this error */
    private Test tester;
    /** Internal code used by testers to classify errors */
    private int code;
    /** If this error is selected */
    private boolean selected;

    /**
     * Constructs a new {@code TestError}.
     * @param tester The tester
     * @param severity The severity of this error
     * @param message The error message
     * @param primitives The affected primitives
     * @param code The test error reference code
     */
    public TestError(Test tester, Severity severity, String message, String description, String description_en,
            int code, Collection<? extends OsmPrimitive> primitives, Collection<?> highlighted) {
        this.tester = tester;
        this.severity = severity;
        this.message = message;
        this.description = description;
        this.description_en = description_en;
        this.primitives = primitives;
        this.highlighted = highlighted;
        this.code = code;
    }

    public TestError(Test tester, Severity severity, String message, int code, Collection<? extends OsmPrimitive> primitives,
            Collection<?> highlighted) {
        this(tester, severity, message, null, null, code, primitives, highlighted);
    }

    public TestError(Test tester, Severity severity, String message, String description, String description_en,
            int code, Collection<? extends OsmPrimitive> primitives) {
        this(tester, severity, message, description, description_en, code, primitives, primitives);
    }

    public TestError(Test tester, Severity severity, String message, int code, Collection<? extends OsmPrimitive> primitives) {
        this(tester, severity, message, null, null, code, primitives, primitives);
    }

    public TestError(Test tester, Severity severity, String message, int code, OsmPrimitive primitive) {
        this(tester, severity, message, null, null, code, Collections.singletonList(primitive), Collections
                .singletonList(primitive));
    }

    public TestError(Test tester, Severity severity, String message, String description, String description_en,
            int code, OsmPrimitive primitive) {
        this(tester, severity, message, description, description_en, code, Collections.singletonList(primitive));
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
     * Sets the error message
     * @param message The error message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the list of primitives affected by this error
     * @return the list of primitives affected by this error
     */
    public Collection<? extends OsmPrimitive> getPrimitives() {
        return primitives;
    }

    /**
     * Gets the list of primitives affected by this error and are selectable
     * @return the list of selectable primitives affected by this error
     */
    public Collection<? extends OsmPrimitive> getSelectablePrimitives() {
        List<OsmPrimitive> selectablePrimitives = new ArrayList<OsmPrimitive>(primitives.size());
        for (OsmPrimitive o : primitives) {
            if (o.isSelectable()) {
                selectablePrimitives.add(o);
            }
        }
        return selectablePrimitives;
    }

    /**
     * Sets the list of primitives affected by this error
     * @param primitives the list of primitives affected by this error
     */
    public void setPrimitives(List<OsmPrimitive> primitives) {
        this.primitives = primitives;
    }

    /**
     * Gets the severity of this error
     * @return the severity of this error
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Sets the severity of this error
     * @param severity the severity of this error
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Sets the ignore state for this error
     */
    public String getIgnoreState() {
        Collection<String> strings = new TreeSet<String>();
        StringBuilder ignorestring = new StringBuilder(getIgnoreSubGroup());
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
            strings.add(type + "_" + o.getId());
        }
        for (String o : strings) {
            ignorestring.append(":").append(o);
        }
        return ignorestring.toString();
    }

    public String getIgnoreSubGroup() {
        String ignorestring = getIgnoreGroup();
        if (description_en != null) {
            ignorestring += "_" + description_en;
        }
        return ignorestring;
    }

    public String getIgnoreGroup() {
        return Integer.toString(code);
    }

    public void setIgnored(boolean state) {
        ignored = state;
    }

    public Boolean getIgnored() {
        return ignored;
    }

    /**
     * Gets the tester that raised this error
     * @return the tester that raised this error
     */
    public Test getTester() {
        return tester;
    }

    public void setTester(Test tester) {
        this.tester = tester;
    }

    /**
     * Gets the code
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns true if the error can be fixed automatically
     *
     * @return true if the error can be fixed
     */
    public boolean isFixable() {
        return tester != null && tester.isFixable(this);
    }

    /**
     * Fixes the error with the appropriate command
     *
     * @return The command to fix the error
     */
    public Command getFix() {
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

    @SuppressWarnings("unchecked")
    public void visitHighlighted(ValidatorVisitor v) {
        for (Object o : highlighted) {
            if (o instanceof OsmPrimitive) {
                v.visit((OsmPrimitive) o);
            } else if (o instanceof WaySegment) {
                v.visit((WaySegment) o);
            } else if (o instanceof List<?>) {
                v.visit((List<Node>)o);
            }
        }
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
        return highlighted;
    }

    @Override
    public int compareTo(TestError o) {
        if (equals(o)) return 0;

        MultipleNameVisitor v1 = new MultipleNameVisitor();
        MultipleNameVisitor v2 = new MultipleNameVisitor();

        v1.visit(getPrimitives());
        v2.visit(o.getPrimitives());
        return AlphanumComparator.getInstance().compare(v1.toString(), v2.toString());
    }

    @Override public void primitivesRemoved(PrimitivesRemovedEvent event) {
        // Remove purged primitives (fix #8639)
        try {
            primitives.removeAll(event.getPrimitives());
        } catch (UnsupportedOperationException e) {
            if (event.getPrimitives().containsAll(primitives)) {
                primitives = Collections.emptyList();
            } else {
                Main.warn("Unable to remove primitives from "+this);
            }
        }
    }

    @Override public void primitivesAdded(PrimitivesAddedEvent event) {}
    @Override public void tagsChanged(TagsChangedEvent event) {}
    @Override public void nodeMoved(NodeMovedEvent event) {}
    @Override public void wayNodesChanged(WayNodesChangedEvent event) {}
    @Override public void relationMembersChanged(RelationMembersChangedEvent event) {}
    @Override public void otherDatasetChange(AbstractDatasetChangedEvent event) {}
    @Override public void dataChanged(DataChangedEvent event) {}

    @Override
    public String toString() {
        return "TestError [tester=" + tester + ", code=" + code + "]";
    }
}
