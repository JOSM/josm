// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Logging;

/**
 * Abstract Reader, allowing other implementations than OsmReader (PbfReader in PBF plugin for example)
 * @author Vincent
 *
 */
public abstract class AbstractReader {

    /**
     * The dataset to add parsed objects to.
     */
    protected DataSet ds = new DataSet();

    protected Changeset uploadChangeset;

    /** the map from external ids to read OsmPrimitives. External ids are
     * longs too, but in contrast to internal ids negative values are used
     * to identify primitives unknown to the OSM server
     */
    protected final Map<PrimitiveId, OsmPrimitive> externalIdMap = new HashMap<>();

    /**
     * Data structure for the remaining way objects
     */
    protected final Map<Long, Collection<Long>> ways = new HashMap<>();

    /**
     * Data structure for relation objects
     */
    protected final Map<Long, Collection<RelationMemberData>> relations = new HashMap<>();

    /**
     * Replies the parsed data set
     *
     * @return the parsed data set
     */
    public DataSet getDataSet() {
        return ds;
    }

    /**
     * Processes the parsed nodes after parsing. Just adds them to
     * the dataset
     *
     */
    protected void processNodesAfterParsing() {
        for (OsmPrimitive primitive: externalIdMap.values()) {
            if (primitive instanceof Node) {
                this.ds.addPrimitive(primitive);
            }
        }
    }

    /**
     * Processes the ways after parsing. Rebuilds the list of nodes of each way and
     * adds the way to the dataset
     *
     * @throws IllegalDataException if a data integrity problem is detected
     */
    protected void processWaysAfterParsing() throws IllegalDataException {
        for (Entry<Long, Collection<Long>> entry : ways.entrySet()) {
            Long externalWayId = entry.getKey();
            Way w = (Way) externalIdMap.get(new SimplePrimitiveId(externalWayId, OsmPrimitiveType.WAY));
            List<Node> wayNodes = new ArrayList<>();
            for (long id : entry.getValue()) {
                Node n = (Node) externalIdMap.get(new SimplePrimitiveId(id, OsmPrimitiveType.NODE));
                if (n == null) {
                    if (id <= 0)
                        throw new IllegalDataException(
                                tr("Way with external ID ''{0}'' includes missing node with external ID ''{1}''.",
                                        Long.toString(externalWayId),
                                        Long.toString(id)));
                    // create an incomplete node if necessary
                    n = (Node) ds.getPrimitiveById(id, OsmPrimitiveType.NODE);
                    if (n == null) {
                        n = new Node(id);
                        ds.addPrimitive(n);
                    }
                }
                if (n.isDeleted()) {
                    Logging.info(tr("Deleted node {0} is part of way {1}", Long.toString(id), Long.toString(w.getId())));
                } else {
                    wayNodes.add(n);
                }
            }
            w.setNodes(wayNodes);
            if (w.hasIncompleteNodes()) {
                Logging.info(tr("Way {0} with {1} nodes is incomplete because at least one node was missing in the loaded data.",
                        Long.toString(externalWayId), w.getNodesCount()));
            }
            ds.addPrimitive(w);
        }
    }

    /**
     * Completes the parsed relations with its members.
     *
     * @throws IllegalDataException if a data integrity problem is detected, i.e. if a
     * relation member refers to a local primitive which wasn't available in the data
     */
    protected void processRelationsAfterParsing() throws IllegalDataException {

        // First add all relations to make sure that when relation reference other relation, the referenced will be already in dataset
        for (Long externalRelationId : relations.keySet()) {
            Relation relation = (Relation) externalIdMap.get(
                    new SimplePrimitiveId(externalRelationId, OsmPrimitiveType.RELATION)
            );
            ds.addPrimitive(relation);
        }

        for (Entry<Long, Collection<RelationMemberData>> entry : relations.entrySet()) {
            Long externalRelationId = entry.getKey();
            Relation relation = (Relation) externalIdMap.get(
                    new SimplePrimitiveId(externalRelationId, OsmPrimitiveType.RELATION)
            );
            List<RelationMember> relationMembers = new ArrayList<>();
            for (RelationMemberData rm : entry.getValue()) {
                // lookup the member from the map of already created primitives
                OsmPrimitive primitive = externalIdMap.get(new SimplePrimitiveId(rm.getMemberId(), rm.getMemberType()));

                if (primitive == null) {
                    if (rm.getMemberId() <= 0)
                        // relation member refers to a primitive with a negative id which was not
                        // found in the data. This is always a data integrity problem and we abort
                        // with an exception
                        //
                        throw new IllegalDataException(
                                tr("Relation with external id ''{0}'' refers to a missing primitive with external id ''{1}''.",
                                        Long.toString(externalRelationId),
                                        Long.toString(rm.getMemberId())));

                    // member refers to OSM primitive which was not present in the parsed data
                    // -> create a new incomplete primitive and add it to the dataset
                    //
                    primitive = ds.getPrimitiveById(rm.getMemberId(), rm.getMemberType());
                    if (primitive == null) {
                        switch (rm.getMemberType()) {
                        case NODE:
                            primitive = new Node(rm.getMemberId()); break;
                        case WAY:
                            primitive = new Way(rm.getMemberId()); break;
                        case RELATION:
                            primitive = new Relation(rm.getMemberId()); break;
                        default: throw new AssertionError(); // can't happen
                        }

                        ds.addPrimitive(primitive);
                        externalIdMap.put(new SimplePrimitiveId(rm.getMemberId(), rm.getMemberType()), primitive);
                    }
                }
                if (primitive.isDeleted()) {
                    Logging.info(tr("Deleted member {0} is used by relation {1}",
                            Long.toString(primitive.getId()), Long.toString(relation.getId())));
                } else {
                    relationMembers.add(new RelationMember(rm.getRole(), primitive));
                }
            }
            relation.setMembers(relationMembers);
        }
    }

    protected void processChangesetAfterParsing() {
        if (uploadChangeset != null) {
            for (Map.Entry<String, String> e : uploadChangeset.getKeys().entrySet()) {
                ds.addChangeSetTag(e.getKey(), e.getValue());
            }
        }
    }

    protected final void prepareDataSet() throws IllegalDataException {
        ds.beginUpdate();
        try {
            processNodesAfterParsing();
            processWaysAfterParsing();
            processRelationsAfterParsing();
            processChangesetAfterParsing();
        } finally {
            ds.endUpdate();
        }
    }

    protected abstract DataSet doParseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException;
}
