// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.TigerUtils;

/**
 * Collection of utility methods for tag conflict resolution
 *
 */
public class TagConflictResolutionUtil {

    /** no constructor, just static utility methods */
    private TagConflictResolutionUtil() {}

    /**
     * Normalizes the tags in the tag collection <code>tc</code> before resolving tag conflicts.
     *
     *  Removes irrelevant tags like "created_by".
     *
     *  For tags which are not present on at least one of the merged nodes, the empty value ""
     *  is added to the list of values for this tag, but only if there are at least two
     *  primitives with tags.
     *
     * @param tc the tag collection
     * @param merged the collection of merged  primitives
     */
    public static void normalizeTagCollectionBeforeEditing(TagCollection tc, Collection<? extends OsmPrimitive> merged) {
        // remove irrelevant tags
        //
        tc.removeByKey("created_by");

        int numNodesWithTags = 0;
        for (OsmPrimitive p: merged) {
            if (p.getKeys().size() >0) {
                numNodesWithTags++;
            }
        }
        if (numNodesWithTags <= 1)
            return;

        for (String key: tc.getKeys()) {
            // make sure the empty value is in the tag set if a tag is not present
            // on all merged nodes
            //
            for (OsmPrimitive p: merged) {
                if (p.get(key) == null) {
                    tc.add(new Tag(key, "")); // add a tag with key and empty value
                }
            }
        }
    }

    /**
     * Combines tags from TIGER data
     *
     * @param tc the tag collection
     */
    public static void combineTigerTags(TagCollection tc) {
        for (String key: tc.getKeys()) {
            if (TigerUtils.isTigerTag(key)) {
                tc.setUniqueForKey(key, TigerUtils.combineTags(key, tc.getValues(key)));
            }
        }
    }

    /**
     * Completes tags in the tag collection <code>tc</code> with the empty value
     * for each tag. If the empty value is present the tag conflict resolution dialog
     * will offer an option for removing the tag and not only options for selecting
     * one of the current values of the tag.
     *
     * @param tc the tag collection
     */
    public static void completeTagCollectionForEditing(TagCollection tc) {
        for (String key: tc.getKeys()) {
            // make sure the empty value is in the tag set such that we can delete the tag
            // in the conflict dialog if necessary
            //
            tc.add(new Tag(key,""));
        }
    }
}
