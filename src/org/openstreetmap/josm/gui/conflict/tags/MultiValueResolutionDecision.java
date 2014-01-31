// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.tools.CheckParameterUtil;
/**
 * Represents a decision for a conflict due to multiple possible value for a tag.
 *
 *
 */
public class MultiValueResolutionDecision {

    /** the type of decision */
    private MultiValueDecisionType type;
    /** the collection of tags for which a decision is needed */
    private TagCollection  tags;
    /** the selected value if {@link #type} is {@link MultiValueDecisionType#KEEP_ONE} */
    private String value;

    /**
     * constuctor
     */
    public MultiValueResolutionDecision() {
        type = MultiValueDecisionType.UNDECIDED;
        tags = new TagCollection();
        autoDecide();
    }

    /**
     * Creates a new decision for the tag collection <code>tags</code>.
     * All tags must have the same key.
     *
     * @param tags the tags. Must not be null.
     * @exception IllegalArgumentException  thrown if tags is null
     * @exception IllegalArgumentException thrown if there are more than one keys
     * @exception IllegalArgumentException thrown if tags is empty
     */
    public MultiValueResolutionDecision(TagCollection tags) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(tags, "tags");
        if (tags.isEmpty())
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' must not be empty.", "tags"));
        if (tags.getKeys().size() != 1)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' with tags for exactly one key expected. Got {1}.", "tags", tags.getKeys().size()));
        this.tags = tags;
        autoDecide();
    }

    /**
     * Tries to find the best decision based on the current values.
     */
    protected void autoDecide() {
        this.type = MultiValueDecisionType.UNDECIDED;
        // exactly one empty value ? -> delete the tag
        if (tags.size() == 1 && tags.getValues().contains("")) {
            this.type = MultiValueDecisionType.KEEP_NONE;

            // exactly one non empty value? -> keep this value
        } else if (tags.size() == 1) {
            this.type = MultiValueDecisionType.KEEP_ONE;
            this.value = tags.getValues().iterator().next();
        }
    }

    /**
     * Apply the decision to keep no value
     */
    public void keepNone() {
        this.type = MultiValueDecisionType.KEEP_NONE;
    }

    /**
     * Apply the decision to keep all values
     */
    public void keepAll() {
        this.type = MultiValueDecisionType.KEEP_ALL;
    }

    /**
     * Apply the decision to keep exactly one value
     *
     * @param value  the value to keep
     * @throws IllegalArgumentException thrown if value is null
     * @throws IllegalStateException thrown if value is not in the list of known values for this tag
     */
    public void keepOne(String value) throws IllegalArgumentException, IllegalStateException {
        CheckParameterUtil.ensureParameterNotNull(value, "value");
        if (!tags.getValues().contains(value))
            throw new IllegalStateException(tr("Tag collection does not include the selected value ''{0}''.", value));
        this.value = value;
        this.type = MultiValueDecisionType.KEEP_ONE;
    }

    /**
     * sets a new value for this
     *
     * @param value the new vlaue
     */
    public void setNew(String value) {
        if (value == null) {
            value = "";
        }
        this.value = value;
        this.type = MultiValueDecisionType.KEEP_ONE;

    }

    /**
     * marks this as undecided
     *
     */
    public void undecide() {
        this.type = MultiValueDecisionType.UNDECIDED;
    }

    /**
     * Replies the chosen value
     *
     * @return the chosen value
     * @throws IllegalStateException thrown if this resolution is not yet decided
     */
    public String getChosenValue() throws IllegalStateException {
        switch(type) {
        case UNDECIDED: throw new IllegalStateException(tr("Not decided yet."));
        case KEEP_ONE: return value;
        case KEEP_NONE: return null;
        case KEEP_ALL: return tags.getJoinedValues(getKey());
        }
        // should not happen
        return null;
    }

    /**
     * Replies the list of possible, non empty values
     *
     * @return the list of possible, non empty values
     */
    public List<String> getValues() {
        List<String> ret = new ArrayList<String>(tags.getValues());
        ret.remove("");
        ret.remove(null);
        Collections.sort(ret);
        return ret;
    }

    /**
     * Replies the key of the tag to be resolved by this resolution
     *
     * @return the key of the tag to be resolved by this resolution
     */
    public String getKey() {
        return tags.getKeys().iterator().next();
    }

    /**
     * Replies true if the empty value is a possible value in this resolution
     *
     * @return true if the empty value is a possible value in this resolution
     */
    public boolean canKeepNone() {
        return tags.getValues().contains("");
    }

    /**
     * Replies true, if this resolution has more than 1 possible non-empty values
     *
     * @return true, if this resolution has more than 1 possible non-empty values
     */
    public boolean canKeepAll() {
        return getValues().size() > 1;
    }

    /**
     * Replies  true if this resolution is decided
     *
     * @return true if this resolution is decided
     */
    public boolean isDecided() {
        return !type.equals(MultiValueDecisionType.UNDECIDED);
    }

    /**
     * Replies the type of the resolution
     *
     * @return the type of the resolution
     */
    public MultiValueDecisionType getDecisionType() {
        return type;
    }

    /**
     * Applies the resolution to an {@link OsmPrimitive}
     *
     * @param primitive the primitive
     * @throws IllegalStateException thrown if this resolution is not resolved yet
     *
     */
    public void applyTo(OsmPrimitive primitive) throws IllegalStateException{
        if (primitive == null) return;
        if (!isDecided())
            throw new IllegalStateException(tr("Not decided yet."));
        String key = tags.getKeys().iterator().next();
        String value = getChosenValue();
        if (type.equals(MultiValueDecisionType.KEEP_NONE)) {
            primitive.remove(key);
        } else {
            primitive.put(key, value);
        }
    }

    /**
     * Applies this resolution to a collection of primitives
     *
     * @param primitives the collection of primitives
     * @throws IllegalStateException thrown if this resolution is not resolved yet
     */
    public void applyTo(Collection<? extends OsmPrimitive> primitives) throws IllegalStateException {
        if (primitives == null) return;
        for (OsmPrimitive primitive: primitives) {
            if (primitive == null) {
                continue;
            }
            applyTo(primitive);
        }
    }

    /**
     * Builds a change command for applying this resolution to a primitive
     *
     * @param primitive  the primitive
     * @return the change command
     * @throws IllegalArgumentException thrown if primitive is null
     * @throws IllegalStateException thrown if this resolution is not resolved yet
     */
    public Command buildChangeCommand(OsmPrimitive primitive) throws IllegalArgumentException, IllegalStateException {
        CheckParameterUtil.ensureParameterNotNull(primitive, "primitive");
        if (!isDecided())
            throw new IllegalStateException(tr("Not decided yet."));
        String key = tags.getKeys().iterator().next();
        return new ChangePropertyCommand(primitive, key, getChosenValue());
    }

    /**
     * Builds a change command for applying this resolution to a collection of primitives
     *
     * @param primitives  the collection of primitives
     * @return the change command
     * @throws IllegalArgumentException thrown if primitives is null
     * @throws IllegalStateException thrown if this resolution is not resolved yet
     */
    public Command buildChangeCommand(Collection<? extends OsmPrimitive> primitives) {
        CheckParameterUtil.ensureParameterNotNull(primitives, "primitives");
        if (!isDecided())
            throw new IllegalStateException(tr("Not decided yet."));
        String key = tags.getKeys().iterator().next();
        return new ChangePropertyCommand(primitives, key, getChosenValue());
    }

    /**
     * Replies a tag representing the current resolution. Null, if this resolution is not resolved yet.
     *
     * @return a tag representing the current resolution. Null, if this resolution is not resolved yet
     */
    public Tag getResolution() {
        switch(type) {
        case KEEP_ALL: return new Tag(getKey(), tags.getJoinedValues(getKey()));
        case KEEP_ONE: return new Tag(getKey(),value);
        case KEEP_NONE: return new Tag(getKey(), "");
        case UNDECIDED: return null;
        }
        return null;
    }
}
