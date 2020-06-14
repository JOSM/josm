// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Logging;

/**
 * Check for inconsistencies in lane information between relation and members.
 */
public class ConnectivityRelations extends Test {

    protected static final int INCONSISTENT_LANE_COUNT = 3900;

    protected static final int UNKNOWN_CONNECTIVITY_ROLE = 3901;

    protected static final int NO_CONNECTIVITY_TAG = 3902;

    protected static final int MALFORMED_CONNECTIVITY_TAG = 3903;

    protected static final int MISSING_COMMA_CONNECTIVITY_TAG = 3904;

    protected static final int TOO_MANY_ROLES = 3905;

    protected static final int MISSING_ROLE = 3906;

    protected static final int MEMBER_MISSING_LANES = 3907;

    protected static final int CONNECTIVITY_IMPLIED = 3908;

    private static final String CONNECTIVITY_TAG = "connectivity";
    private static final String VIA = "via";
    private static final String TO = "to";
    private static final String FROM = "from";
    private static final int BW = -1000;
    private static final Pattern OPTIONAL_LANE_PATTERN = Pattern.compile("\\([0-9-]+\\)");
    private static final Pattern TO_LANE_PATTERN = Pattern.compile("\\p{Zs}*[,:;]\\p{Zs}*");
    private static final Pattern MISSING_COMMA_PATTERN = Pattern.compile("[0-9]+\\([0-9]+\\)|\\([0-9]+\\)[0-9]+");
    private static final Pattern LANE_TAG_PATTERN = Pattern.compile(".*:lanes");

    /**
    * Constructor
    */
    public ConnectivityRelations() {
        super(tr("Connectivity Relations"), tr("Validates connectivity relations"));
    }

    /**
     * Convert the connectivity tag into a map of values
     *
     * @param relation A relation with a {@code connectivity} tag.
     * @return A Map in the form of {@code Map<Lane From, Map<Lane To, Optional>>} May contain nulls when errors are encountered
     */
    public static Map<Integer, Map<Integer, Boolean>> parseConnectivityTag(Relation relation) {
        String cnTag = relation.get(CONNECTIVITY_TAG);
        if (cnTag == null) {
            return Collections.emptyMap();
        }
        final String joined = cnTag.replace("bw", Integer.toString(BW));


        final Map<Integer, Map<Integer, Boolean>> result = new HashMap<>();
        String[] lanes = joined.split("\\|", -1);
        for (int i = 0; i < lanes.length; i++) {
            String[] lane = lanes[i].split(":", -1);
            int laneNumber;
            //Ignore connections from bw, since we cannot derive a lane number from bw
            if (!"bw".equals(lane[0])) {
                laneNumber = Integer.parseInt(lane[0].trim());
            } else {
                laneNumber = BW;
            }
            Map<Integer, Boolean> connections = new HashMap<>();
            String[] toLanes = TO_LANE_PATTERN.split(lane[1], -1);
            for (int j = 0; j < toLanes.length; j++) {
                String toLane = toLanes[j].trim();
                try {
                    if (OPTIONAL_LANE_PATTERN.matcher(toLane).matches()) {
                        toLane = toLane.replace("(", "").replace(")", "").trim();
                        if (!"bw".equals(toLane)) {
                            connections.put(Integer.parseInt(toLane), Boolean.TRUE);
                        } else
                            connections.put(BW, Boolean.TRUE);
                    } else {
                        if (!toLane.contains("bw")) {
                            connections.put(Integer.parseInt(toLane), Boolean.FALSE);
                        } else {
                            connections.put(BW, Boolean.FALSE);
                        }
                    }
                } catch (NumberFormatException e) {
                    if (MISSING_COMMA_PATTERN.matcher(toLane).matches()) {
                        connections.put(null, true);
                    } else {
                        connections.put(null, null);
                    }
                }
            }
            result.put(laneNumber, connections);
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public void visit(Relation r) {
        if (r.hasTag("type", CONNECTIVITY_TAG)) {
            if (!r.hasKey(CONNECTIVITY_TAG)) {
                errors.add(TestError.builder(this, Severity.WARNING, NO_CONNECTIVITY_TAG)
                        .message(tr("Connectivity relation without connectivity tag")).primitives(r).build());
            } else if (!r.hasIncompleteMembers()) {
                boolean badRole = checkForBadRole(r);
                boolean missingRole = checkForMissingRole(r);
                if (!badRole && !missingRole) {
                    Map<String, Integer> roleLanes = checkForInconsistentLanes(r);
                    checkForImpliedConnectivity(r, roleLanes);
                }
            }
        }
    }

    /**
     * Compare lane tags of members to values in the {@code connectivity} tag of the relation
     *
     * @param relation A relation with a {@code connectivity} tag.
     * @return A Map in the form of {@code Map<Role, Lane Count>}
     */
    private Map<String, Integer> checkForInconsistentLanes(Relation relation) {
        StringBuilder lanelessRoles = new StringBuilder();
        int lanelessRolesCount = 0;
        // Lane count from connectivity tag
        Map<Integer, Map<Integer, Boolean>> connTagLanes = parseConnectivityTag(relation);
        // If the ways involved in the connectivity tag are assuming a standard 2-way bi-directional highway
        boolean defaultLanes = true;
        for (Entry<Integer, Map<Integer, Boolean>> thisEntry : connTagLanes.entrySet()) {
            for (Entry<Integer, Boolean> thisEntry2 : thisEntry.getValue().entrySet()) {
                Logging.debug("Checking: " + thisEntry2.toString());
                if (thisEntry2.getKey() != null && thisEntry2.getKey() > 1) {
                    defaultLanes = false;
                    break;
                }
            }
            if (!defaultLanes) {
                break;
            }
        }
        // Lane count from member tags
        Map<String, Integer> roleLanes = new HashMap<>();
        for (RelationMember rM : relation.getMembers()) {
            // Check lanes
            if (rM.getType() == OsmPrimitiveType.WAY) {
                OsmPrimitive prim = rM.getMember();
                if (!VIA.equals(rM.getRole())) {
                    Map<String, String> primKeys = prim.getKeys();
                    List<Long> laneCounts = new ArrayList<>();
                    long maxLaneCount;
                    if (prim.hasTag("lanes")) {
                        laneCounts.add(Long.parseLong(prim.get("lanes")));
                    }
                    for (Entry<String, String> entry : primKeys.entrySet()) {
                        String thisKey = entry.getKey();
                        String thisValue = entry.getValue();
                        if (LANE_TAG_PATTERN.matcher(thisKey).matches()) {
                            //Count bar characters
                            long count = thisValue.chars().filter(ch -> ch == '|').count() + 1;
                            laneCounts.add(count);
                        }
                    }

                    if (!laneCounts.equals(Collections.emptyList())) {
                        maxLaneCount = Collections.max(laneCounts);
                        roleLanes.put(rM.getRole(), (int) maxLaneCount);
                    } else {
                        String addString = "'" + rM.getRole() + "'";
                        StringBuilder sb = new StringBuilder(addString);
                        if (lanelessRoles.length() > 0) {
                            sb.insert(0, " and ");
                        }
                        lanelessRoles.append(sb.toString());
                        lanelessRolesCount++;
                    }
                }
            }
        }

        if (lanelessRoles.toString().isEmpty()) {
            boolean fromCheck = roleLanes.get(FROM) < Collections
                    .max(connTagLanes.entrySet(), Comparator.comparingInt(Map.Entry::getKey)).getKey();
            boolean toCheck = false;
            for (Entry<Integer, Map<Integer, Boolean>> to : connTagLanes.entrySet()) {
                if (!to.getValue().containsKey(null)) {
                    toCheck = roleLanes.get(TO) < Collections
                            .max(to.getValue().entrySet(), Comparator.comparingInt(Map.Entry::getKey)).getKey();
                } else {
                    if (to.getValue().containsValue(true)) {
                        errors.add(TestError.builder(this, Severity.ERROR, MISSING_COMMA_CONNECTIVITY_TAG)
                                .message(tr("Connectivity tag missing comma between optional and non-optional values")).primitives(relation)
                                .build());
                    } else {
                        errors.add(TestError.builder(this, Severity.ERROR, MALFORMED_CONNECTIVITY_TAG)
                                .message(tr("Connectivity tag contains unusual data")).primitives(relation)
                                .build());
                    }
                }
            }
            if (fromCheck || toCheck) {
                errors.add(TestError.builder(this, Severity.WARNING, INCONSISTENT_LANE_COUNT)
                        .message(tr("Inconsistent lane numbering between relation and member tags")).primitives(relation)
                        .build());
            }
        } else if (!defaultLanes) {
            errors.add(TestError.builder(this, Severity.WARNING, MEMBER_MISSING_LANES)
                    .message(trn("Relation {0} member is missing a lanes or *:lanes tag",
                            "Relation {0} members are missing a lanes or *:lanes tag", lanelessRolesCount,
                            lanelessRoles))
                    .primitives(relation).build());
        }
        return roleLanes;
    }

    /**
     * Check the relation to see if the connectivity described is already implied by other data
     *
     * @param relation A relation with a {@code connectivity} tag.
     * @param roleLanes The lane counts for each relation role
     */
    private void checkForImpliedConnectivity(Relation relation, Map<String, Integer> roleLanes) {
        Map<Integer, Map<Integer, Boolean>> connTagLanes = parseConnectivityTag(relation);
        // Don't flag connectivity as already implied when:
        // - Lane counts are different on the roads
        // - Placement tags convey the connectivity
        // - The relation passes through an intersection
        //   - If via member is a node, it's connected to ways not in the relation
        //   - If a via member is a way, ways not in the relation connect to its nodes
        // - Highways that appear to be merging have a different cumulative number of lanes than
        //   the highway that they're merging into

        boolean connImplied = checkMemberTagsForImpliedConnectivity(relation, roleLanes) && !checkForIntersectionAtMembers(relation)
                // Check if connectivity tag implies default connectivity
                && connTagLanes.entrySet().stream()
                .noneMatch(to -> {
                    int fromLane = to.getKey();
                    return to.getValue().entrySet().stream()
                            .anyMatch(lane -> lane.getKey() != null && fromLane != lane.getKey());
                });

        if (connImplied) {
            errors.add(TestError.builder(this, Severity.WARNING, CONNECTIVITY_IMPLIED)
                    .message(tr("This connectivity may already be implied")).primitives(relation)
                    .build());
        }
    }

    /**
     * Check to see if there is an intersection present at the via member
     *
     * @param relation A relation with a {@code connectivity} tag.
     * @return A Boolean that indicates whether an intersection is present at the via member
     */
    private static boolean checkForIntersectionAtMembers(Relation relation) {
        OsmPrimitive viaPrim = relation.findRelationMembers("via").get(0);
        Set<OsmPrimitive> relationMembers = relation.getMemberPrimitives();

        if (viaPrim.getType() == OsmPrimitiveType.NODE) {
            Node viaNode = (Node) viaPrim;
            List<Way> parentWays = viaNode.getParentWays();
            if (parentWays.size() > 2) {
                return parentWays.stream()
                        .anyMatch(thisWay -> !relationMembers.contains(thisWay) && thisWay.hasTag("highway"));
            }
        } else if (viaPrim.getType() == OsmPrimitiveType.WAY) {
            Way viaWay = (Way) viaPrim;
            return viaWay.getNodes().stream()
                    .map(Node::getParentWays).filter(parentWays -> parentWays.size() > 2)
                    .flatMap(Collection::stream)
                    .anyMatch(thisWay -> !relationMembers.contains(thisWay) && thisWay.hasTag("highway"));
        }
        return false;
    }

    /**
     * Check the relation to see if the connectivity described is already implied by the relation members' tags
     *
     * @param relation A relation with a {@code connectivity} tag.
     * @param roleLanes The lane counts for each relation role
     * @return Whether connectivity is already implied by tags on relation members
     */
    private static boolean checkMemberTagsForImpliedConnectivity(Relation relation, Map<String, Integer> roleLanes) {
        // The members have different lane counts
        if (roleLanes.containsKey(TO) && roleLanes.containsKey(FROM) && !roleLanes.get(TO).equals(roleLanes.get(FROM))) {
            return false;
        }

        // The members don't have placement tags defining the connectivity
        List<RelationMember> members = relation.getMembers();
        Map<String, OsmPrimitive> toFromMembers = new HashMap<>();
        for (RelationMember mem : members) {
            if (mem.getRole().equals(FROM)) {
                toFromMembers.put(FROM, mem.getMember());
            } else if (mem.getRole().equals(TO)) {
                toFromMembers.put(TO, mem.getMember());
            }
        }

        return toFromMembers.get(TO).hasKey("placement") || toFromMembers.get(FROM).hasKey("placement");
    }

    /**
     * Check if the roles of the relation are appropriate
     *
     * @param relation A relation with a {@code connectivity} tag.
     * @return Whether one or more of the relation's members has an unusual role
     */
    private boolean checkForBadRole(Relation relation) {
        // Check role names
        int viaWays = 0;
        int viaNodes = 0;
        for (RelationMember relationMember : relation.getMembers()) {
            if (relationMember.getMember() instanceof Way) {
                if (relationMember.hasRole(VIA))
                    viaWays++;
                else if (!relationMember.hasRole(FROM) && !relationMember.hasRole(TO)) {
                    return true;
                }
            } else if (relationMember.getMember() instanceof Node) {
                if (!relationMember.hasRole(VIA)) {
                    return true;
                }
                viaNodes++;
            }
        }
        return mixedViaNodeAndWay(relation, viaWays, viaNodes);
    }

    /**
     * Check if the relation contains all necessary roles
     *
     * @param relation A relation with a {@code connectivity} tag.
     * @return Whether the relation is missing one or more of the critical {@code from}, {@code via}, or {@code to} roles
     */
    private static boolean checkForMissingRole(Relation relation) {
        List<String> necessaryRoles = new ArrayList<>();
        necessaryRoles.add(FROM);
        necessaryRoles.add(VIA);
        necessaryRoles.add(TO);
        return !relation.getMemberRoles().containsAll(necessaryRoles);
    }

    /**
     * Check if the relation's roles are on appropriate objects
     *
     * @param relation A relation with a {@code connectivity} tag.
     * @param viaWays The number of ways in the relation with the {@code via} role
     * @param viaNodes The number of nodes in the relation with the {@code via} role
     * @return Whether the relation is missing one or more of the critical 'from', 'via', or 'to' roles
     */
    private boolean mixedViaNodeAndWay(Relation relation, int viaWays, int viaNodes) {
        String message = "";
        if (viaNodes > 1) {
            if (viaWays > 0) {
                message = tr("Relation should not contain mixed ''via'' ways and nodes");
            } else {
                message = tr("Multiple ''via'' roles only allowed with ways");
            }
        }
        if (message.isEmpty()) {
            return false;
        } else {
            errors.add(TestError.builder(this, Severity.WARNING, TOO_MANY_ROLES)
                    .message(message).primitives(relation).build());
            return true;
        }
    }

}
