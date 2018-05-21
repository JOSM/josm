// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.TagCollection;

/**
 * Combine primitives conflicts resolver.
 * @since 11772
 */
public class CombinePrimitiveResolver {

    private final TagConflictResolverModel modelTagConflictResolver;
    private final RelationMemberConflictResolverModel modelRelConflictResolver;

    /**
     * Constructs a new {@code CombinePrimitiveResolver}.
     * @param tagModel tag conflict resolver model
     * @param relModel relation member conflict resolver model
     */
    public CombinePrimitiveResolver(TagConflictResolverModel tagModel, RelationMemberConflictResolverModel relModel) {
        this.modelTagConflictResolver = tagModel;
        this.modelRelConflictResolver = relModel;
    }

    /**
     * Builds conflicts resolution commands for the given target primitive.
     * @param targetPrimitive target primitive
     * @return list of conflicts resolution commands
     */
    public List<Command> buildResolutionCommands(OsmPrimitive targetPrimitive) {
        List<Command> cmds = new LinkedList<>();

        TagCollection allResolutions = modelTagConflictResolver.getAllResolutions();
        if (!allResolutions.isEmpty()) {
            cmds.addAll(buildTagChangeCommand(targetPrimitive, allResolutions));
        }
        for (String p : AbstractPrimitive.getDiscardableKeys()) {
            if (targetPrimitive.get(p) != null) {
                cmds.add(new ChangePropertyCommand(targetPrimitive, p, null));
            }
        }

        if (modelRelConflictResolver.getNumDecisions() > 0) {
            cmds.addAll(modelRelConflictResolver.buildResolutionCommands(targetPrimitive));
        }

        return cmds;
    }

    /**
     * Builds the list of tag change commands.
     * @param primitive target primitive
     * @param tc all resolutions
     * @return the list of tag change commands
     */
    protected List<Command> buildTagChangeCommand(OsmPrimitive primitive, TagCollection tc) {
        List<Command> cmds = new LinkedList<>();
        for (String key : tc.getKeys()) {
            if (tc.hasUniqueEmptyValue(key)) {
                if (primitive.get(key) != null) {
                    cmds.add(new ChangePropertyCommand(primitive, key, null));
                }
            } else {
                String value = tc.getJoinedValues(key);
                if (!value.equals(primitive.get(key))) {
                    cmds.add(new ChangePropertyCommand(primitive, key, value));
                }
            }
        }
        return cmds;
    }
}
