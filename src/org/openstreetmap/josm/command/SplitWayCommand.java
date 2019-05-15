// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * Splits a way into multiple ways (all identical except for their node list).
 *
 * Ways are just split at the selected nodes.  The nodes remain in their
 * original order.  Selected nodes at the end of a way are ignored.
 *
 * @since 12828 ({@code SplitWayAction} converted to a {@link Command})
 */
public class SplitWayCommand extends SequenceCommand {

    private static volatile Consumer<String> warningNotifier = Logging::warn;

    /**
     * Sets the global warning notifier.
     * @param notifier warning notifier in charge of displaying warning message, if any. Must not be null
     */
    public static void setWarningNotifier(Consumer<String> notifier) {
        warningNotifier = Objects.requireNonNull(notifier);
    }

    private final List<? extends PrimitiveId> newSelection;
    private final Way originalWay;
    private final List<Way> newWays;
    /** Map&lt;Restriction type, type to treat it as&gt; */
    private static final Map<String, String> relationSpecialTypes = new HashMap<>();
    static {
        relationSpecialTypes.put("restriction", "restriction");
        relationSpecialTypes.put("destination_sign", "restriction");
    }

    /**
     * Create a new {@code SplitWayCommand}.
     * @param name The description text
     * @param commandList The sequence of commands that should be executed.
     * @param newSelection The new list of selected primitives ids (which is saved for later retrieval with {@link #getNewSelection})
     * @param originalWay The original way being split (which is saved for later retrieval with {@link #getOriginalWay})
     * @param newWays The resulting new ways (which is saved for later retrieval with {@link #getOriginalWay})
     */
    public SplitWayCommand(String name, Collection<Command> commandList,
            List<? extends PrimitiveId> newSelection, Way originalWay, List<Way> newWays) {
        super(name, commandList);
        this.newSelection = newSelection;
        this.originalWay = originalWay;
        this.newWays = newWays;
    }

    /**
     * Replies the new list of selected primitives ids
     * @return The new list of selected primitives ids
     */
    public List<? extends PrimitiveId> getNewSelection() {
        return newSelection;
    }

    /**
     * Replies the original way being split
     * @return The original way being split
     */
    public Way getOriginalWay() {
        return originalWay;
    }

    /**
     * Replies the resulting new ways
     * @return The resulting new ways
     */
    public List<Way> getNewWays() {
        return newWays;
    }

    /**
     * Determines which way chunk should reuse the old id and its history
     */
    @FunctionalInterface
    public interface Strategy {

        /**
         * Determines which way chunk should reuse the old id and its history.
         *
         * @param wayChunks the way chunks
         * @return the way to keep
         */
        Way determineWayToKeep(Iterable<Way> wayChunks);

        /**
         * Returns a strategy which selects the way chunk with the highest node count to keep.
         * @return strategy which selects the way chunk with the highest node count to keep
         */
        static Strategy keepLongestChunk() {
            return wayChunks -> {
                    Way wayToKeep = null;
                    for (Way i : wayChunks) {
                        if (wayToKeep == null || i.getNodesCount() > wayToKeep.getNodesCount()) {
                            wayToKeep = i;
                        }
                    }
                    return wayToKeep;
                };
        }

        /**
         * Returns a strategy which selects the first way chunk.
         * @return strategy which selects the first way chunk
         */
        static Strategy keepFirstChunk() {
            return wayChunks -> wayChunks.iterator().next();
        }
    }

    /**
     * Splits the nodes of {@code wayToSplit} into a list of node sequences
     * which are separated at the nodes in {@code splitPoints}.
     *
     * This method displays warning messages if {@code wayToSplit} and/or
     * {@code splitPoints} aren't consistent.
     *
     * Returns null, if building the split chunks fails.
     *
     * @param wayToSplit the way to split. Must not be null.
     * @param splitPoints the nodes where the way is split. Must not be null.
     * @return the list of chunks
     */
    public static List<List<Node>> buildSplitChunks(Way wayToSplit, List<Node> splitPoints) {
        CheckParameterUtil.ensureParameterNotNull(wayToSplit, "wayToSplit");
        CheckParameterUtil.ensureParameterNotNull(splitPoints, "splitPoints");

        Set<Node> nodeSet = new HashSet<>(splitPoints);
        List<List<Node>> wayChunks = new LinkedList<>();
        List<Node> currentWayChunk = new ArrayList<>();
        wayChunks.add(currentWayChunk);

        Iterator<Node> it = wayToSplit.getNodes().iterator();
        while (it.hasNext()) {
            Node currentNode = it.next();
            boolean atEndOfWay = currentWayChunk.isEmpty() || !it.hasNext();
            currentWayChunk.add(currentNode);
            if (nodeSet.contains(currentNode) && !atEndOfWay) {
                currentWayChunk = new ArrayList<>();
                currentWayChunk.add(currentNode);
                wayChunks.add(currentWayChunk);
            }
        }

        // Handle circular ways specially.
        // If you split at a circular way at two nodes, you just want to split
        // it at these points, not also at the former endpoint.
        // So if the last node is the same first node, join the last and the
        // first way chunk.
        List<Node> lastWayChunk = wayChunks.get(wayChunks.size() - 1);
        if (wayChunks.size() >= 2
                && wayChunks.get(0).get(0) == lastWayChunk.get(lastWayChunk.size() - 1)
                && !nodeSet.contains(wayChunks.get(0).get(0))) {
            if (wayChunks.size() == 2) {
                warningNotifier.accept(tr("You must select two or more nodes to split a circular way."));
                return null;
            }
            lastWayChunk.remove(lastWayChunk.size() - 1);
            lastWayChunk.addAll(wayChunks.get(0));
            wayChunks.remove(wayChunks.size() - 1);
            wayChunks.set(0, lastWayChunk);
        }

        if (wayChunks.size() < 2) {
            if (wayChunks.get(0).get(0) == wayChunks.get(0).get(wayChunks.get(0).size() - 1)) {
                warningNotifier.accept(
                        tr("You must select two or more nodes to split a circular way."));
            } else {
                warningNotifier.accept(
                        tr("The way cannot be split at the selected nodes. (Hint: Select nodes in the middle of the way.)"));
            }
            return null;
        }
        return wayChunks;
    }

    /**
     * Creates new way objects for the way chunks and transfers the keys from the original way.
     * @param way the original way whose  keys are transferred
     * @param wayChunks the way chunks
     * @return the new way objects
     */
    public static List<Way> createNewWaysFromChunks(Way way, Iterable<List<Node>> wayChunks) {
        final List<Way> newWays = new ArrayList<>();
        for (List<Node> wayChunk : wayChunks) {
            Way wayToAdd = new Way();
            wayToAdd.setKeys(way.getKeys());
            wayToAdd.setNodes(wayChunk);
            newWays.add(wayToAdd);
        }
        return newWays;
    }

    /**
     * Splits the way {@code way} into chunks of {@code wayChunks} and replies
     * the result of this process in an instance of {@link SplitWayCommand}.
     *
     * Note that changes are not applied to the data yet. You have to
     * submit the command first, i.e. {@code UndoRedoHandler.getInstance().add(result)}.
     *
     * @param way the way to split. Must not be null.
     * @param wayChunks the list of way chunks into the way is split. Must not be null.
     * @param selection The list of currently selected primitives
     * @return the result from the split operation
     */
    public static SplitWayCommand splitWay(Way way, List<List<Node>> wayChunks, Collection<? extends OsmPrimitive> selection) {
        return splitWay(way, wayChunks, selection, Strategy.keepLongestChunk());
    }

    /**
     * Splits the way {@code way} into chunks of {@code wayChunks} and replies
     * the result of this process in an instance of {@link SplitWayCommand}.
     * The {@link SplitWayCommand.Strategy} is used to determine which
     * way chunk should reuse the old id and its history.
     *
     * Note that changes are not applied to the data yet. You have to
     * submit the command first, i.e. {@code UndoRedoHandler.getInstance().add(result)}.
     *
     * @param way the way to split. Must not be null.
     * @param wayChunks the list of way chunks into the way is split. Must not be null.
     * @param selection The list of currently selected primitives
     * @param splitStrategy The strategy used to determine which way chunk should reuse the old id and its history
     * @return the result from the split operation
     */
    public static SplitWayCommand splitWay(Way way, List<List<Node>> wayChunks,
            Collection<? extends OsmPrimitive> selection, Strategy splitStrategy) {
        // build a list of commands, and also a new selection list
        final List<OsmPrimitive> newSelection = new ArrayList<>(selection.size() + wayChunks.size());
        newSelection.addAll(selection);

        // Create all potential new ways
        final List<Way> newWays = createNewWaysFromChunks(way, wayChunks);

        // Determine which part reuses the existing way
        final Way wayToKeep = splitStrategy.determineWayToKeep(newWays);

        return wayToKeep != null ? doSplitWay(way, wayToKeep, newWays, newSelection) : null;
    }

    /**
     * Effectively constructs the {@link SplitWayCommand}.
     * This method is only public for {@code SplitWayAction}.
     *
     * @param way the way to split. Must not be null.
     * @param wayToKeep way chunk which should reuse the old id and its history
     * @param newWays potential new ways
     * @param newSelection new selection list to update (optional: can be null)
     * @return the {@code SplitWayCommand}
     */
    public static SplitWayCommand doSplitWay(Way way, Way wayToKeep, List<Way> newWays, List<OsmPrimitive> newSelection) {

        Collection<Command> commandList = new ArrayList<>(newWays.size());
        Collection<String> nowarnroles = Config.getPref().getList("way.split.roles.nowarn",
                Arrays.asList("outer", "inner", "forward", "backward", "north", "south", "east", "west"));

        // Change the original way
        final Way changedWay = new Way(way);
        changedWay.setNodes(wayToKeep.getNodes());
        commandList.add(new ChangeCommand(way, changedWay));
        if (/*!isMapModeDraw &&*/ newSelection != null && !newSelection.contains(way)) {
            newSelection.add(way);
        }
        final int indexOfWayToKeep = newWays.indexOf(wayToKeep);
        newWays.remove(wayToKeep);

        if (/*!isMapModeDraw &&*/ newSelection != null) {
            newSelection.addAll(newWays);
        }
        for (Way wayToAdd : newWays) {
            commandList.add(new AddCommand(way.getDataSet(), wayToAdd));
        }

        boolean warnmerole = false;
        boolean warnme = false;
        // now copy all relations to new way also

        for (Relation r : OsmPrimitive.getParentRelations(Collections.singleton(way))) {
            if (!r.isUsable()) {
                continue;
            }
            Relation c = null;
            String type = Optional.ofNullable(r.get("type")).orElse("");

            int ic = 0;
            int ir = 0;
            List<RelationMember> relationMembers = r.getMembers();
            for (RelationMember rm: relationMembers) {
                if (rm.isWay() && rm.getMember() == way) {
                    boolean insert = true;
                    if (relationSpecialTypes.containsKey(type) && "restriction".equals(relationSpecialTypes.get(type))) {
                        Map<String, Boolean> rValue = treatAsRestriction(r, rm, c, newWays, way, changedWay);
                        warnme = rValue.containsKey("warnme") ? rValue.get("warnme") : warnme;
                        insert = rValue.containsKey("insert") ? rValue.get("insert") : insert;
                    } else if (!("route".equals(type)) && !("multipolygon".equals(type))) {
                        warnme = true;
                    }
                    if (c == null) {
                        c = new Relation(r);
                    }

                    if (insert) {
                        if (rm.hasRole() && !nowarnroles.contains(rm.getRole())) {
                            warnmerole = true;
                        }

                        Boolean backwards = null;
                        int k = 1;
                        while (ir - k >= 0 || ir + k < relationMembers.size()) {
                            if ((ir - k >= 0) && relationMembers.get(ir - k).isWay()) {
                                Way w = relationMembers.get(ir - k).getWay();
                                if ((w.lastNode() == way.firstNode()) || w.firstNode() == way.firstNode()) {
                                    backwards = Boolean.FALSE;
                                } else if ((w.firstNode() == way.lastNode()) || w.lastNode() == way.lastNode()) {
                                    backwards = Boolean.TRUE;
                                }
                                break;
                            }
                            if ((ir + k < relationMembers.size()) && relationMembers.get(ir + k).isWay()) {
                                Way w = relationMembers.get(ir + k).getWay();
                                if ((w.lastNode() == way.firstNode()) || w.firstNode() == way.firstNode()) {
                                    backwards = Boolean.TRUE;
                                } else if ((w.firstNode() == way.lastNode()) || w.lastNode() == way.lastNode()) {
                                    backwards = Boolean.FALSE;
                                }
                                break;
                            }
                            k++;
                        }

                        int j = ic;
                        final List<Way> waysToAddBefore = newWays.subList(0, indexOfWayToKeep);
                        for (Way wayToAdd : waysToAddBefore) {
                            RelationMember em = new RelationMember(rm.getRole(), wayToAdd);
                            j++;
                            if (Boolean.TRUE.equals(backwards)) {
                                c.addMember(ic + 1, em);
                            } else {
                                c.addMember(j - 1, em);
                            }
                        }
                        final List<Way> waysToAddAfter = newWays.subList(indexOfWayToKeep, newWays.size());
                        for (Way wayToAdd : waysToAddAfter) {
                            RelationMember em = new RelationMember(rm.getRole(), wayToAdd);
                            j++;
                            if (Boolean.TRUE.equals(backwards)) {
                                c.addMember(ic, em);
                            } else {
                                c.addMember(j, em);
                            }
                        }
                        ic = j;
                    }
                }
                ic++;
                ir++;
            }

            if (c != null) {
                commandList.add(new ChangeCommand(r.getDataSet(), r, c));
            }
        }
        if (warnmerole) {
            warningNotifier.accept(
                    tr("A role based relation membership was copied to all new ways.<br>You should verify this and correct it when necessary."));
        } else if (warnme) {
            warningNotifier.accept(
                    tr("A relation membership was copied to all new ways.<br>You should verify this and correct it when necessary."));
        }

        return new SplitWayCommand(
                    /* for correct i18n of plural forms - see #9110 */
                    trn("Split way {0} into {1} part", "Split way {0} into {1} parts", newWays.size() + 1,
                            way.getDisplayName(DefaultNameFormatter.getInstance()), newWays.size() + 1),
                    commandList,
                    newSelection,
                    way,
                    newWays
            );
    }

    private static Map<String, Boolean> treatAsRestriction(Relation r,
            RelationMember rm, Relation c, Collection<Way> newWays, Way way,
            Way changedWay) {
        HashMap<String, Boolean> rMap = new HashMap<>();
        /* this code assumes the restriction is correct. No real error checking done */
        String role = rm.getRole();
        String type = Optional.ofNullable(r.get("type")).orElse("");
        if ("from".equals(role) || "to".equals(role)) {
            OsmPrimitive via = findVia(r, type);
            List<Node> nodes = new ArrayList<>();
            if (via != null) {
                if (via instanceof Node) {
                    nodes.add((Node) via);
                } else if (via instanceof Way) {
                    nodes.add(((Way) via).lastNode());
                    nodes.add(((Way) via).firstNode());
                }
            }
            Way res = null;
            for (Node n : nodes) {
                if (changedWay.isFirstLastNode(n)) {
                    res = way;
                }
            }
            if (res == null) {
                for (Way wayToAdd : newWays) {
                    for (Node n : nodes) {
                        if (wayToAdd.isFirstLastNode(n)) {
                            res = wayToAdd;
                        }
                    }
                }
                if (res != null) {
                    if (c == null) {
                        c = new Relation(r);
                    }
                    c.addMember(new RelationMember(role, res));
                    c.removeMembersFor(way);
                    rMap.put("insert", false);
                }
            } else {
                rMap.put("insert", false);
            }
        } else if (!"via".equals(role)) {
            rMap.put("warnme", true);
        }
        return rMap;
    }

    static OsmPrimitive findVia(Relation r, String type) {
        if (type != null) {
            switch (type) {
            case "restriction":
                return findRelationMember(r, "via").orElse(null);
            case "destination_sign":
                // Prefer intersection over sign, see #12347
                return findRelationMember(r, "intersection").orElse(findRelationMember(r, "sign").orElse(null));
            default:
                return null;
            }
        }
        return null;
    }

    static Optional<OsmPrimitive> findRelationMember(Relation r, String role) {
        return r.getMembers().stream().filter(rmv -> role.equals(rmv.getRole()))
                .map(RelationMember::getMember).findAny();
    }

    /**
     * Splits the way {@code way} at the nodes in {@code atNodes} and replies
     * the result of this process in an instance of {@link SplitWayCommand}.
     *
     * Note that changes are not applied to the data yet. You have to
     * submit the command first, i.e. {@code UndoRedoHandler.getInstance().add(result)}.
     *
     * Replies null if the way couldn't be split at the given nodes.
     *
     * @param way the way to split. Must not be null.
     * @param atNodes the list of nodes where the way is split. Must not be null.
     * @param selection The list of currently selected primitives
     * @return the result from the split operation
     */
    public static SplitWayCommand split(Way way, List<Node> atNodes, Collection<? extends OsmPrimitive> selection) {
        List<List<Node>> chunks = buildSplitChunks(way, atNodes);
        return chunks != null ? splitWay(way, chunks, selection) : null;
    }

    /**
     * Add relations that are treated in a specific way.
     * @param relationType The value in the {@code type} key
     * @param treatAs The type of relation to treat the {@code relationType} as.
     * Currently only supports relations that can be handled like "restriction"
     * relations.
     * @return the previous value associated with relationType, or null if there was no mapping
     * @since 15078
     */
    public static String addSpecialRelationType(String relationType, String treatAs) {
        return relationSpecialTypes.put(relationType, treatAs);
    }

    /**
     * Get the types of relations that are treated differently
     * @return {@code Map<Relation Type, Type of Relation it is to be treated as>}
     * @since 15078
     */
    public static Map<String, String> getSpecialRelationTypes() {
        return relationSpecialTypes;
    }
}
