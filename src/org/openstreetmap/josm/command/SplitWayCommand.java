// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.command.SplitWayCommand.MissingMemberStrategy.GO_AHEAD_WITHOUT_DOWNLOADS;
import static org.openstreetmap.josm.command.SplitWayCommand.MissingMemberStrategy.GO_AHEAD_WITH_DOWNLOADS;
import static org.openstreetmap.josm.command.SplitWayCommand.MissingMemberStrategy.USER_ABORTED;
import static org.openstreetmap.josm.command.SplitWayCommand.WhenRelationOrderUncertain.ASK_USER_FOR_CONSENT_TO_DOWNLOAD;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
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
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.widgets.JMultilineLabel;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * Splits a way into multiple ways (all identical except for their node list).
 * <p>
 * Ways are just split at the selected nodes.  The nodes remain in their
 * original order.  Selected nodes at the end of a way are ignored.
 *
 * @since 12828 ({@code SplitWayAction} converted to a {@link Command})
 */
public class SplitWayCommand extends SequenceCommand {

    private static volatile Consumer<String> warningNotifier = Logging::warn;
    private static final String DOWNLOAD_MISSING_PREF_KEY = "split_way_download_missing_members";

    private static final class RelationInformation {
        boolean warnme;
        boolean insert;
        Relation relation;
    }

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

    private static final String RESTRICTION = "restriction";
    /** Map&lt;Restriction type, type to treat it as&gt; */
    private static final Map<String, String> relationSpecialTypes = new HashMap<>();
    static {
        relationSpecialTypes.put(RESTRICTION, RESTRICTION);
        relationSpecialTypes.put("destination_sign", RESTRICTION);
        relationSpecialTypes.put("connectivity", RESTRICTION);
    }

    /**
     * Create a new {@code SplitWayCommand}.
     * @param name The description text
     * @param commandList The sequence of commands that should be executed.
     * @param newSelection The new list of selected primitives ids (which is saved for later retrieval with {@link #getNewSelection})
     * @param originalWay The original way being split (which is saved for later retrieval with {@link #getOriginalWay})
     * @param newWays The resulting new ways (which is saved for later retrieval with {@link #getNewWays})
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
     * <p>
     * This method displays warning messages if {@code wayToSplit} and/or
     * {@code splitPoints} aren't consistent.
     * <p>
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
     * <p>
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
     * Splits the way {@code way} into chunks of {@code wayChunks} and replies the result of this process in an instance
     * of {@link SplitWayCommand}. The {@link SplitWayCommand.Strategy} is used to determine which way chunk should
     * reuse the old id and its history.
     * <p>
     * If the split way is part of relations, and the order of the new parts in these relations cannot be determined due
     * to missing relation members, the user will be asked to consent to downloading these missing members.
     * <p>
     * Note that changes are not applied to the data yet. You have to submit the command first, i.e. {@code
     * UndoRedoHandler.getInstance().add(result)}.
     *
     * @param way           the way to split. Must not be null.
     * @param wayChunks     the list of way chunks into the way is split. Must not be null.
     * @param selection     The list of currently selected primitives
     * @param splitStrategy The strategy used to determine which way chunk should reuse the old id and its history
     * @return the result from the split operation
     */
    public static SplitWayCommand splitWay(Way way,
                                           List<List<Node>> wayChunks,
                                           Collection<? extends OsmPrimitive> selection,
                                           Strategy splitStrategy) {

        // This method could be refactored to use an Optional in the future, but would need to be deprecated first
        // to phase out use by plugins.
        return splitWay(way, wayChunks, selection, splitStrategy, ASK_USER_FOR_CONSENT_TO_DOWNLOAD).orElse(null);
    }

    /**
     * Splits the way {@code way} into chunks of {@code wayChunks} and replies the result of this process in an instance
     * of {@link SplitWayCommand}. The {@link SplitWayCommand.Strategy} is used to determine which way chunk should
     * reuse the old id and its history.
     * <p>
     * Note that changes are not applied to the data yet. You have to submit the command first, i.e. {@code
     * UndoRedoHandler.getInstance().add(result)}.
     *
     * @param way                        the way to split. Must not be null.
     * @param wayChunks                  the list of way chunks into the way is split. Must not be null.
     * @param selection                  The list of currently selected primitives
     * @param splitStrategy              The strategy used to determine which way chunk should reuse the old id and its
     *                                   history
     * @param whenRelationOrderUncertain What to do when the split way is part of relations, and the order of the new
     *                                   parts in the relation cannot be determined without downloading missing relation
     *                                   members.
     * @return The result from the split operation, may be an empty {@link Optional} if the operation is aborted.
     */
    public static Optional<SplitWayCommand> splitWay(Way way,
                                                     List<List<Node>> wayChunks,
                                                     Collection<? extends OsmPrimitive> selection,
                                                     Strategy splitStrategy,
                                                     WhenRelationOrderUncertain whenRelationOrderUncertain) {
        // build a list of commands, and also a new selection list
        final List<OsmPrimitive> newSelection = new ArrayList<>(selection.size() + wayChunks.size());
        newSelection.addAll(selection);

        // Create all potential new ways
        final List<Way> newWays = createNewWaysFromChunks(way, wayChunks);

        // Determine which part reuses the existing way
        final Way wayToKeep = splitStrategy.determineWayToKeep(newWays);

        return wayToKeep != null
                ? doSplitWay(way, wayToKeep, newWays, newSelection, whenRelationOrderUncertain)
                : Optional.empty();
    }

    /**
     * Effectively constructs the {@link SplitWayCommand}.
     * This method is only public for {@code SplitWayAction}.
     *
     * @param way the way to split. Must not be null.
     * @param wayToKeep way chunk which should reuse the old id and its history
     * @param newWays potential new ways
     * @param newSelection new selection list to update (optional: can be null)
     * @param whenRelationOrderUncertain Action to perform when the order of the new parts in relations the way is
     *                                   member of could not be reliably determined. See
     *                                   {@link WhenRelationOrderUncertain}.
     * @return the {@code SplitWayCommand}
     */
    public static Optional<SplitWayCommand> doSplitWay(Way way,
                                                       Way wayToKeep,
                                                       List<Way> newWays,
                                                       List<OsmPrimitive> newSelection,
                                                       WhenRelationOrderUncertain whenRelationOrderUncertain) {
        if (whenRelationOrderUncertain == null) whenRelationOrderUncertain = ASK_USER_FOR_CONSENT_TO_DOWNLOAD;

        final int indexOfWayToKeep = newWays.indexOf(wayToKeep);
        newWays.remove(wayToKeep);

        // Figure out the order of relation members (if any).
        Analysis analysis = analyseSplit(way, wayToKeep, newWays);

        // If there are relations that cannot be split properly without downloading more members,
        // present the user with an option to do so, or to abort the split.
        Set<Relation> relationsNeedingMoreMembers = new HashSet<>();
        Set<OsmPrimitive> incompleteMembers = new HashSet<>();
        for (RelationAnalysis relationAnalysis : analysis.getRelationAnalyses()) {
            if (!relationAnalysis.getNeededIncompleteMembers().isEmpty()) {
                incompleteMembers.addAll(relationAnalysis.getNeededIncompleteMembers());
                relationsNeedingMoreMembers.add(relationAnalysis.getRelation());
            }
        }

        MissingMemberStrategy missingMemberStrategy;
        if (relationsNeedingMoreMembers.isEmpty()) {
            // The split can be performed without any extra downloads.
            missingMemberStrategy = GO_AHEAD_WITHOUT_DOWNLOADS;
        } else {
            switch (whenRelationOrderUncertain) {
                case ASK_USER_FOR_CONSENT_TO_DOWNLOAD:
                    // If the analysis shows that for some relations missing members should be downloaded, offer the user the
                    // chance to consent to this.

                    // Only ask the user about downloading missing members when they haven't consented to this before.
                    if (ConditionalOptionPaneUtil.getDialogReturnValue(DOWNLOAD_MISSING_PREF_KEY) == Integer.MAX_VALUE) {
                        // User has previously told us downloading missing relation members is fine.
                        missingMemberStrategy = GO_AHEAD_WITH_DOWNLOADS;
                    } else {
                        // Ask the user.
                        missingMemberStrategy = offerToDownloadMissingMembersIfNeeded(analysis, relationsNeedingMoreMembers.size());
                    }
                    break;
                case SPLIT_ANYWAY:
                    missingMemberStrategy = GO_AHEAD_WITHOUT_DOWNLOADS;
                    break;
                case DOWNLOAD_MISSING_MEMBERS:
                    missingMemberStrategy = GO_AHEAD_WITH_DOWNLOADS;
                    break;
                case ABORT:
                default:
                    missingMemberStrategy = USER_ABORTED;
                    break;
            }
        }

        try {
            switch (missingMemberStrategy) {
            case GO_AHEAD_WITH_DOWNLOADS:
                try {
                    downloadMissingMembers(incompleteMembers);
                } catch (OsmTransferException e) {
                    ExceptionDialogUtil.explainException(e);
                    return Optional.empty();
                }
                // If missing relation members were downloaded, perform the analysis again to find the relation
                // member order for all relations.
                analysis.cleanup();
                analysis = analyseSplit(way, wayToKeep, newWays);
                break;
            case GO_AHEAD_WITHOUT_DOWNLOADS:
                // Proceed with the split with the information we have.
                // This can mean that there are no missing members we want, or that the user chooses to continue
                // the split without downloading them.
                break;
            case USER_ABORTED:
            default:
                return Optional.empty();
            }
            return Optional.of(splitBasedOnAnalyses(way, newWays, newSelection, analysis, indexOfWayToKeep));
        } finally {
            // see #19885
            wayToKeep.setNodes(null);
            analysis.cleanup();
        }
    }

    static Analysis analyseSplit(Way way,
                                 Way wayToKeep,
                                 List<Way> newWays) {
        Collection<Command> commandList = new ArrayList<>();
        Collection<String> nowarnroles = Config.getPref().getList("way.split.roles.nowarn",
                Arrays.asList("outer", "inner", "forward", "backward", "north", "south", "east", "west"));

        // Change the original way
        final List<Node> changedWayNodes = wayToKeep.getNodes();
        commandList.add(new ChangeNodesCommand(way, changedWayNodes));
        for (Way wayToAdd : newWays) {
            commandList.add(new AddCommand(way.getDataSet(), wayToAdd));
        }

        List<RelationAnalysis> relationAnalyses = new ArrayList<>();
        EnumSet<WarningType> warnings = EnumSet.noneOf(WarningType.class);
        int numberOfRelations = 0;

        for (Relation r : OsmPrimitive.getParentRelations(Collections.singleton(way))) {
            if (!r.isUsable()) {
                continue;
            }

            numberOfRelations++;
            boolean isSimpleCase = true;

            Relation c = null;
            String type = Optional.ofNullable(r.get("type")).orElse("");
            // Known types of ordered relations.
            boolean isOrderedRelation = "route".equals(type) || "multipolygon".equals(type) || "boundary".equals(type);

            for (int ir = 0; ir < r.getMembersCount(); ir++) {
                RelationMember rm = r.getMember(ir);
                if (rm.getMember() == way) {
                    boolean insert = true;
                    if (relationSpecialTypes.containsKey(type) && RESTRICTION.equals(relationSpecialTypes.get(type))) {
                        RelationInformation rValue = treatAsRestriction(r, rm, c, newWays, way, changedWayNodes);
                        if (rValue.warnme) warnings.add(WarningType.GENERIC);
                        insert = rValue.insert;
                        c = rValue.relation; // Value.relation is null or contains a modified copy
                    } else if (!isOrderedRelation) {
                        // Warn the user when relations that are not a route or multipolygon are modified as a result
                        // of splitting up the way, because we can't tell if this might break anything.
                        warnings.add(WarningType.GENERIC);
                    }

                    if (insert) {
                        if (c == null) {
                            c = new Relation(r);
                        }
                        if (rm.hasRole() && !nowarnroles.contains(rm.getRole())) {
                            warnings.add(WarningType.ROLE);
                        }

                        // Attempt to determine the direction the ways in the relation are ordered.
                        Direction direction = Direction.UNKNOWN;
                        Set<Way> missingWays = new HashSet<>();
                        if (isOrderedRelation) {
                            if (way.lastNode() == way.firstNode()) {
                                // Self-closing way.
                                direction = direction.merge(Direction.IRRELEVANT);
                            } else {
                                // For ordered relations, looking beyond the nearest neighbour members is not required,
                                // and can even cause the wrong direction to be guessed (with closed loops).
                                if (ir - 1 >= 0 && r.getMember(ir - 1).isWay()) {
                                    Way w = r.getMember(ir - 1).getWay();
                                    if (w.isIncomplete())
                                        missingWays.add(w);
                                    else {
                                        if (w.lastNode() == way.firstNode() || w.firstNode() == way.firstNode()) {
                                            direction = direction.merge(Direction.FORWARDS);
                                        } else if (w.firstNode() == way.lastNode() || w.lastNode() == way.lastNode()) {
                                            direction = direction.merge(Direction.BACKWARDS);
                                        }
                                    }
                                }
                                if (ir + 1 < r.getMembersCount() && r.getMember(ir + 1).isWay()) {
                                    Way w = r.getMember(ir + 1).getWay();
                                    if (w.isIncomplete())
                                        missingWays.add(w);
                                    else {
                                        if (w.lastNode() == way.firstNode() || w.firstNode() == way.firstNode()) {
                                            direction = direction.merge(Direction.BACKWARDS);
                                        } else if (w.firstNode() == way.lastNode() || w.lastNode() == way.lastNode()) {
                                            direction = direction.merge(Direction.FORWARDS);
                                        }
                                    }
                                }

                                if (direction == Direction.UNKNOWN && missingWays.isEmpty()) {
                                    // we cannot detect the direction and no way is missing.
                                    // We can safely assume that the direction doesn't matter.
                                    direction = Direction.IRRELEVANT;
                                }
                            }
                        } else {
                            int k = 1;
                            while (ir - k >= 0 || ir + k < r.getMembersCount()) {
                                if (ir - k >= 0 && r.getMember(ir - k).isWay()) {
                                    Way w = r.getMember(ir - k).getWay();
                                    if (w.lastNode() == way.firstNode() || w.firstNode() == way.firstNode()) {
                                        direction = direction.merge(Direction.FORWARDS);
                                    } else if (w.firstNode() == way.lastNode() || w.lastNode() == way.lastNode()) {
                                        direction = direction.merge(Direction.BACKWARDS);
                                    }
                                    break;
                                }
                                if (ir + k < r.getMembersCount() && r.getMember(ir + k).isWay()) {
                                    Way w = r.getMember(ir + k).getWay();
                                    if (w.lastNode() == way.firstNode() || w.firstNode() == way.firstNode()) {
                                        direction = direction.merge(Direction.BACKWARDS);
                                    } else if (w.firstNode() == way.lastNode() || w.lastNode() == way.lastNode()) {
                                        direction = direction.merge(Direction.FORWARDS);
                                    }
                                    break;
                                }
                                k++;
                            }
                        }

                        if (direction == Direction.UNKNOWN) {
                            // We don't have enough information to determine the order of the new ways in this relation.
                            // This may cause relations to be saved with the two new way sections in reverse order.
                            //
                            // This often breaks routes.
                            //
                        } else {
                            missingWays = Collections.emptySet();
                        }
                        relationAnalyses.add(new RelationAnalysis(c, rm, direction, missingWays));
                        isSimpleCase = false;
                    }
                }
            }
            if (c != null && isSimpleCase) {
                if (!r.getMembers().equals(c.getMembers())) {
                    commandList.add(new ChangeMembersCommand(r, new ArrayList<>(c.getMembers())));
                }
                c.setMembers(null); // see #19885
            }
        }
        return new Analysis(relationAnalyses, commandList, warnings, numberOfRelations);
    }

    static class Analysis {
        List<RelationAnalysis> relationAnalyses;
        Collection<Command> commands;
        EnumSet<WarningType> warningTypes;
        private final int numberOfRelations;

        Analysis(List<RelationAnalysis> relationAnalyses,
                 Collection<Command> commandList,
                 EnumSet<WarningType> warnings,
                 int numberOfRelations) {
            this.relationAnalyses = relationAnalyses;
            commands = commandList;
            warningTypes = warnings;
            this.numberOfRelations = numberOfRelations;
        }

        /**
         * Unlink temporary copies of relations. See #19885
         */
        void cleanup() {
            for (RelationAnalysis ra : relationAnalyses) {
                if (ra.relation.getDataSet() == null)
                    ra.relation.setMembers(null);
            }
        }

        List<RelationAnalysis> getRelationAnalyses() {
            return relationAnalyses;
        }

        Collection<Command> getCommands() {
            return commands;
        }

        EnumSet<WarningType> getWarningTypes() {
            return warningTypes;
        }

        public int getNumberOfRelations() {
            return numberOfRelations;
        }
    }

    static MissingMemberStrategy offerToDownloadMissingMembersIfNeeded(Analysis analysis,
                                                                       int numRelationsNeedingMoreMembers) {
        String[] options = {
                tr("Yes, download the missing members"),
                tr("No, abort the split operation"),
                tr("No, perform the split without downloading")
        };

        String msgMemberOfRelations = trn(
                "This way is part of a relation.",
                "This way is part of {0} relations.",
                analysis.getNumberOfRelations(),
                analysis.getNumberOfRelations()
        );

        String msgReferToRelations;
        if (analysis.getNumberOfRelations() == 1) {
            msgReferToRelations = tr("this relation");
        } else if (analysis.getNumberOfRelations() == numRelationsNeedingMoreMembers) {
            msgReferToRelations = tr("these relations");
        } else {
            msgReferToRelations = trn(
                    "one relation",
                    "{0} relations",
                    numRelationsNeedingMoreMembers,
                    numRelationsNeedingMoreMembers
            );
        }

        String msgRelationsMissingData = tr(
                "For {0} the correct order of the new way parts could not be determined. " +
                        "To fix this, some missing relation members should be downloaded first.",
                msgReferToRelations
        );

        JMultilineLabel msg = new JMultilineLabel(msgMemberOfRelations + " " + msgRelationsMissingData);
        msg.setMaxWidth(600);

        int ret = JOptionPane.showOptionDialog(
                MainApplication.getMainFrame(),
                msg,
                tr("Download missing relation members?"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        switch (ret) {
            case JOptionPane.OK_OPTION:
                // Ask the user if they want to do this automatically from now on. We only ask this for the download
                // action, because automatically cancelling is confusing (the user can't tell why this happened), and
                // automatically performing the split without downloading missing members despite needing them is
                // likely to break a lot of routes. The user also can't tell the difference between a split that needs
                // no downloads at all, and this special case where downloading missing relation members will prevent
                // broken relations.
                ConditionalOptionPaneUtil.showMessageDialog(
                        DOWNLOAD_MISSING_PREF_KEY,
                        MainApplication.getMainFrame(),
                        tr("Missing relation members will be downloaded. Should this be done automatically from now on?"),
                        tr("Downloading missing relation members"),
                        JOptionPane.INFORMATION_MESSAGE
                );
                return GO_AHEAD_WITH_DOWNLOADS;
            case JOptionPane.CANCEL_OPTION:
                return GO_AHEAD_WITHOUT_DOWNLOADS;
            default:
                return USER_ABORTED;
        }
    }

    static void downloadMissingMembers(Set<OsmPrimitive> incompleteMembers) throws OsmTransferException {
        // Download the missing members.
        MultiFetchServerObjectReader reader = MultiFetchServerObjectReader.create();
        reader.append(incompleteMembers);

        DataSet ds = reader.parseOsm(NullProgressMonitor.INSTANCE);
        MainApplication.getLayerManager().getEditLayer().mergeFrom(ds);
    }

    static SplitWayCommand splitBasedOnAnalyses(Way way,
                                                List<Way> newWays,
                                                List<OsmPrimitive> newSelection,
                                                Analysis analysis,
                                                int indexOfWayToKeep) {
        if (newSelection != null && !newSelection.contains(way)) {
            newSelection.add(way);
        }

        if (newSelection != null) {
            newSelection.addAll(newWays);
        }

        // Perform the split.
        for (RelationAnalysis relationAnalysis : analysis.getRelationAnalyses()) {
            RelationMember rm = relationAnalysis.getRelationMember();
            Relation relation = relationAnalysis.getRelation();
            Direction direction = relationAnalysis.getDirection();

            for (int i = 0; i < relation.getMembersCount(); i++) {
                // search for identical member (can't use indexOf() as it uses equals()
                if (rm == relation.getMember(i)) {
                    addSortedWays(i, indexOfWayToKeep, direction, newWays, relation);
                    break;
                }
            }
        }

        // add one command for each complex case with relations
        final DataSet ds = way.getDataSet();
        for (Relation r : analysis.getRelationAnalyses().stream().map(RelationAnalysis::getRelation).collect(Collectors.toSet())) {
            Relation orig = (Relation) ds.getPrimitiveById(r);
            analysis.getCommands().add(new ChangeMembersCommand(orig, new ArrayList<>(r.getMembers())));
            r.setMembers(null); // see #19885
        }

        EnumSet<WarningType> warnings = analysis.getWarningTypes();

        if (warnings.contains(WarningType.ROLE)) {
            warningNotifier.accept(
                    tr("A role based relation membership was copied to all new ways.<br>You should verify this and correct it when necessary."));
        } else if (warnings.contains(WarningType.GENERIC)) {
            warningNotifier.accept(
                    tr("A relation membership was copied to all new ways.<br>You should verify this and correct it when necessary."));
        }

        return new SplitWayCommand(
                    /* for correct i18n of plural forms - see #9110 */
                    trn("Split way {0} into {1} part", "Split way {0} into {1} parts", newWays.size() + 1,
                            way.getDisplayName(DefaultNameFormatter.getInstance()), newWays.size() + 1),
                    analysis.getCommands(),
                    newSelection,
                    way,
                    newWays
            );
    }

    /**
     * Add ways in a sorted manner
     * @param position The position of the relation member we are operating on
     * @param indexOfWayToKeep The index of the way that is keeping history if it were in {@code newWays}
     * @param direction The direction of the ways
     * @param newWays The ways that are being added to the relation
     * @param relation The relation we are operating on
     */
    private static void addSortedWays(final int position, final int indexOfWayToKeep, final Direction direction,
                                      final List<Way> newWays, final Relation relation) {
        // sanity check
        if (position < 0) {
            throw new AssertionError("Relation member not found");
        }
        final RelationMember rm = relation.getMember(position);
        final boolean reverse = direction == Direction.BACKWARDS || needToReverseSplit(position, indexOfWayToKeep, relation, newWays);
        final List<Way> waysToAddBefore = newWays.subList(0, indexOfWayToKeep);
        int j = position;
        for (Way wayToAdd : waysToAddBefore) {
            RelationMember em = new RelationMember(rm.getRole(), wayToAdd);
            j++;
            if (reverse) {
                relation.addMember(position + 1, em);
            } else {
                relation.addMember(j - 1, em);
            }
        }
        final List<Way> waysToAddAfter = newWays.subList(indexOfWayToKeep, newWays.size());
        for (Way wayToAdd : waysToAddAfter) {
            RelationMember em = new RelationMember(rm.getRole(), wayToAdd);
            j++;
            if (reverse) {
                relation.addMember(position, em);
            } else {
                relation.addMember(j, em);
            }
        }
    }

    /**
     * This is only strictly necessary when we are splitting a route where it starts to loop back.
     * Example: way1 -> way2 -> way2 -> way1
     *
     * @param position         The position of the original way in the relation
     * @param indexOfWayToKeep The index of the way to keep in relation to {@code newWays}
     * @param relation         The relation we are working on
     * @param newWays          The ways that are being added
     * @return {@code true} if we need to reverse the direction of the ways
     */
    private static boolean needToReverseSplit(final int position, int indexOfWayToKeep, final Relation relation, final List<Way> newWays) {
        final RelationMember rm = relation.getMember(position);
        if (!rm.isWay()) {
            return false;
        }
        final RelationMember previous = position <= 0 ? null : relation.getMember(position - 1);
        final RelationMember next = position + 1 >= relation.getMembersCount() ? null : relation.getMember(position + 1);
        final Way first = indexOfWayToKeep == 0 ? rm.getWay() : newWays.get(0);
        final Way last = indexOfWayToKeep == newWays.size() ? rm.getWay() : newWays.get(newWays.size() - 1);
        if (previous != null && previous.isWay() && previous.getWay().isUsable()) {
            final Way compare = previous.getWay();
            if (!(compare.isFirstLastNode(first.firstNode()) || compare.isFirstLastNode(first.lastNode()))) {
                return true;
            }
        }
        if (next != null && next.isWay() && next.getWay().isUsable()) {
            final Way compare = next.getWay();
            return !(compare.isFirstLastNode(last.firstNode()) || compare.isFirstLastNode(last.lastNode()));
        }
        return false;
    }

    private static RelationInformation treatAsRestriction(Relation r,
            RelationMember rm, Relation c, Collection<Way> newWays, Way way,
            List<Node> changedWayNodes) {
        RelationInformation relationInformation = new RelationInformation();
        /* this code assumes the restriction is correct. No real error checking done */
        String role = rm.getRole();
        String type = Optional.ofNullable(r.get("type")).orElse("");
        if ("from".equals(role) || "to".equals(role)) {
            List<Node> nodes = new ArrayList<>();
            for (OsmPrimitive via : findVias(r, type)) {
                if (via instanceof Node) {
                    nodes.add((Node) via);
                } else if (via instanceof Way) {
                    nodes.add(((Way) via).lastNode());
                    nodes.add(((Way) via).firstNode());
                }
            }
            Way res = null;
            for (Node n : nodes) {
                if (changedWayNodes.get(0) == n || changedWayNodes.get(changedWayNodes.size() - 1) == n) {
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
                }
            }
        } else if (!"via".equals(role)) {
            relationInformation.warnme = true;
        } else {
            relationInformation.insert = true;
        }
        relationInformation.relation = c;
        return relationInformation;
    }

    static List<? extends OsmPrimitive> findVias(Relation r, String type) {
        if (type != null) {
            switch (type) {
            case "connectivity":
            case RESTRICTION:
                return r.findRelationMembers("via");
            case "destination_sign":
                // Prefer intersection over sign, see #12347
                List<? extends OsmPrimitive> intersections = r.findRelationMembers("intersection");
                return intersections.isEmpty() ? r.findRelationMembers("sign") : intersections;
            default:
                break;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Splits the way {@code way} at the nodes in {@code atNodes} and replies
     * the result of this process in an instance of {@link SplitWayCommand}.
     * <p>
     * Note that changes are not applied to the data yet. You have to
     * submit the command first, i.e. {@code UndoRedoHandler.getInstance().add(result)}.
     * <p>
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

    /**
     * What to do when the split way is part of relations, and the order of the new parts in the relation cannot be
     * determined without downloading missing relation members.
     */
    public enum WhenRelationOrderUncertain {
        /**
         * Ask the user to consent to downloading the missing members. The user can abort the operation or choose to
         * proceed without downloading anything.
         */
        ASK_USER_FOR_CONSENT_TO_DOWNLOAD,
        /**
         * If there are relation members missing, and these are needed to determine the order of the new parts in
         * that relation, abort the split operation.
         */
        ABORT,
        /**
         * If there are relation members missing, and these are needed to determine the order of the new parts in
         * that relation, continue with the split operation anyway, without downloading anything. Caution: use this
         * option with care.
         */
        SPLIT_ANYWAY,
        /**
         * If there are relation members missing, and these are needed to determine the order of the new parts in
         * that relation, automatically download these without prompting the user.
         */
        DOWNLOAD_MISSING_MEMBERS
    }

    static class RelationAnalysis {
        private final Relation relation;
        private final RelationMember relationMember;
        private final Direction direction;
        private final Set<Way> neededIncompleteMembers;

        RelationAnalysis(Relation relation,
                                RelationMember relationMember,
                                Direction direction,
                                Set<Way> neededIncompleteMembers) {
            this.relation = relation;
            this.relationMember = relationMember;
            this.direction = direction;
            this.neededIncompleteMembers = neededIncompleteMembers;
        }

        RelationMember getRelationMember() {
            return relationMember;
        }

        Direction getDirection() {
            return direction;
        }

        public Set<Way> getNeededIncompleteMembers() {
            return neededIncompleteMembers;
        }

        Relation getRelation() {
            return relation;
        }
    }

    enum Direction {
        FORWARDS,
        BACKWARDS,
        UNKNOWN,
        IRRELEVANT;

        /**
         * Merge directions (this helps avoid overriding {@link #FORWARDS} with {@link #BACKWARDS}).
         * @param other The other direction to merge. {@link #UNKNOWN} will be overridden.
         * @return The merged direction
         */
        Direction merge(Direction other) {
            if (this == other) {
                return this;
            }
            if (this == IRRELEVANT || other == IRRELEVANT ||
                    (this == FORWARDS && other == BACKWARDS) ||
                    (other == FORWARDS && this == BACKWARDS)) {
                return IRRELEVANT;
            }
            if (this == UNKNOWN) {
                return other;
            }
            if (other == UNKNOWN) {
                return this;
            }
            return UNKNOWN;
        }
    }

    enum WarningType {
        GENERIC,
        ROLE
    }

    enum MissingMemberStrategy {
        GO_AHEAD_WITH_DOWNLOADS,
        GO_AHEAD_WITHOUT_DOWNLOADS,
        USER_ABORTED
    }
}
