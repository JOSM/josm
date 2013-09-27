// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * TagMergeItem represents an individual merge action for a specific pair of key/value.
 *
 * A TagMergeItem manages the values of the two key/value-pairs and keeps track of the applied
 * merge decision.
 *
 */
public class TagMergeItem {

    private String key = null;
    private String myTagValue = null;
    private String theirTagValue = null;
    private MergeDecisionType mergeDecision = MergeDecisionType.UNDECIDED;

    /**
     * constructor
     *
     * @param key  the common tag key. Must not be null.
     * @param myTagValue  the value for this key known in the local dataset
     * @param theirTagValue  the value for this key known in the dataset on the server
     * @throws IllegalArgumentException if key is null
     */
    public TagMergeItem(String key, String myTagValue, String theirTagValue) {
        CheckParameterUtil.ensureParameterNotNull(key, "key");
        this.key  = key;
        this.myTagValue = myTagValue;
        this.theirTagValue = theirTagValue;
        this.mergeDecision = MergeDecisionType.UNDECIDED;
    }

    /**
     * constructor
     *
     * @param key  the tag key common to the merged OSM primitives. Must not be null.
     * @param my  my version of the OSM primitive (i.e. the version known in the local dataset). Must not be null.
     * @param their their version of the OSM primitive (i.e. the version known on the server). Must not be null.
     * @throws IllegalArgumentException thrown if key is null
     * @throws IllegalArgumentException thrown if my is null
     * @throws IllegalArgumentException thrown if their is null
     */
    public TagMergeItem(String key, OsmPrimitive my, OsmPrimitive their) {
        CheckParameterUtil.ensureParameterNotNull(key, "key");
        CheckParameterUtil.ensureParameterNotNull(my, "my");
        CheckParameterUtil.ensureParameterNotNull(their, "their");
        this.key = key;
        myTagValue = my.get(key);
        theirTagValue = their.get(key);
    }

    /**
     * applies a merge decision to this merge item
     *
     * @param decision the merge decision. Must not be null.
     * @exception IllegalArgumentException thrown if decision is null
     *
     */
    public void decide(MergeDecisionType decision) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(decision, "decision");
        this.mergeDecision = decision;
    }

    public String getKey() {
        return key;
    }

    public String getMyTagValue() {
        return myTagValue;
    }

    public String getTheirTagValue() {
        return theirTagValue;
    }

    public MergeDecisionType getMergeDecision() {
        return mergeDecision;
    }

    /**
     * applies the current merge decisions to the tag set of an OSM primitive. The
     * OSM primitive has the role of primitive in the local dataset ('my' primitive,
     * not 'their' primitive)
     *
     * @param primitive the OSM primitive. Must not be null.
     * @exception IllegalArgumentException thrown, if primitive is null
     * @exception IllegalStateException  thrown, if this merge item is undecided
     */
    public void applyToMyPrimitive(OsmPrimitive primitive) throws IllegalArgumentException, IllegalStateException {
        CheckParameterUtil.ensureParameterNotNull(primitive, "primitive");
        if (mergeDecision == MergeDecisionType.UNDECIDED) {
            throw new IllegalStateException(tr("Cannot apply undecided tag merge item."));
        } else if (mergeDecision == MergeDecisionType.KEEP_THEIR) {
            if (theirTagValue == null) {
                primitive.remove(key);
            } else {
                primitive.put(key, theirTagValue);
            }
        } else if (mergeDecision == MergeDecisionType.KEEP_MINE) {
            if (myTagValue == null) {
                primitive.remove(key);
            } else {
                primitive.put(key, myTagValue);
            }
        } else {
           // should not happen
        }
    }
}
