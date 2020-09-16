// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.MultipolygonTest;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationTask;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * Create multipolygon from selected ways automatically.
 *
 * New relation with type=multipolygon is created.
 *
 * If one or more of ways is already in relation with type=multipolygon or the
 * way is not closed, then error is reported and no relation is created.
 *
 * The "inner" and "outer" roles are guessed automatically. First, bbox is
 * calculated for each way. then the largest area is assumed to be outside and
 * the rest inside. In cases with one "outside" area and several cut-ins, the
 * guess should be always good ... In more complex (multiple outer areas) or
 * buggy (inner and outer ways intersect) scenarios the result is likely to be
 * wrong.
 */
public class CreateMultipolygonAction extends JosmAction {

    private final boolean update;
    private static final int MAX_MEMBERS_TO_DOWNLOAD = 100;

    /**
     * Constructs a new {@code CreateMultipolygonAction}.
     * @param update {@code true} if the multipolygon must be updated, {@code false} if it must be created
     */
    public CreateMultipolygonAction(final boolean update) {
        super(getName(update),
                update ? /* ICON */ "multipoly_update" : /* ICON */ "multipoly_create",
                getName(update),
                /* at least three lines for each shortcut or the server extractor fails */
                update ? Shortcut.registerShortcut("tools:multipoly_update",
                            tr("Tool: {0}", getName(true)),
                            KeyEvent.VK_B, Shortcut.CTRL_SHIFT)
                       : Shortcut.registerShortcut("tools:multipoly_create",
                            tr("Tool: {0}", getName(false)),
                            KeyEvent.VK_B, Shortcut.CTRL),
                true, update ? "multipoly_update" : "multipoly_create", true);
        this.update = update;
    }

    private static String getName(boolean update) {
        return update ? tr("Update multipolygon") : tr("Create multipolygon");
    }

    private static final class CreateUpdateMultipolygonTask implements Runnable {
        private final Collection<Way> selectedWays;
        private final Relation multipolygonRelation;

        private CreateUpdateMultipolygonTask(Collection<Way> selectedWays, Relation multipolygonRelation) {
            this.selectedWays = selectedWays;
            this.multipolygonRelation = multipolygonRelation;
        }

        @Override
        public void run() {
            final Pair<SequenceCommand, Relation> commandAndRelation = createMultipolygonCommand(selectedWays, multipolygonRelation);
            if (commandAndRelation == null) {
                return;
            }
            final Command command = commandAndRelation.a;

            // to avoid EDT violations
            SwingUtilities.invokeLater(() -> {
                UndoRedoHandler.getInstance().add(command);
                final Relation relation = (Relation) MainApplication.getLayerManager().getEditDataSet()
                        .getPrimitiveById(commandAndRelation.b);
                if (relation == null || relation.getDataSet() == null)
                    return; // should not happen

                // Use 'SwingUtilities.invokeLater' to make sure the relationListDialog
                // knows about the new relation before we try to select it.
                // (Yes, we are already in event dispatch thread. But DatasetEventManager
                // uses 'SwingUtilities.invokeLater' to fire events so we have to do the same.)
                SwingUtilities.invokeLater(() -> {
                    MainApplication.getMap().relationListDialog.selectRelation(relation);
                    if (Config.getPref().getBoolean("multipoly.show-relation-editor", false)) {
                        //Open relation edit window, if set up in preferences
                        // see #19346 un-select updated multipolygon
                        MainApplication.getLayerManager().getEditDataSet().clearSelection(relation);
                        RelationEditor editor = RelationEditor
                                .getEditor(MainApplication.getLayerManager().getEditLayer(), relation, null);
                        editor.setModal(true);
                        editor.setVisible(true);
                    } else {
                        MainApplication.getLayerManager().getEditLayer().setRecentRelation(relation);
                        if (multipolygonRelation == null) {
                            // see #19346 select new multipolygon
                            MainApplication.getLayerManager().getEditDataSet().setSelected(relation);
                        }
                    }
                });
            });
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet dataSet = getLayerManager().getEditDataSet();
        if (dataSet == null) {
            new Notification(
                    tr("No data loaded."))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        final Collection<Way> selectedWays = dataSet.getSelectedWays();

        if (selectedWays.isEmpty()) {
            // Sometimes it make sense creating multipoly of only one way (so it will form outer way)
            // and then splitting the way later (so there are multiple ways forming outer way)
            new Notification(
                    tr("You must select at least one way."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        final Collection<Relation> selectedRelations = dataSet.getSelectedRelations();
        final Relation multipolygonRelation = update
                ? getSelectedMultipolygonRelation(selectedWays, selectedRelations)
                : null;

        if (update && multipolygonRelation == null)
            return;
        // download incomplete relation or incomplete members if necessary
        OsmDataLayer editLayer = getLayerManager().getEditLayer();
        if (multipolygonRelation != null && editLayer != null && editLayer.isDownloadable()) {
            if (!multipolygonRelation.isNew() && multipolygonRelation.isIncomplete()) {
                MainApplication.worker
                        .submit(new DownloadRelationTask(Collections.singleton(multipolygonRelation), editLayer));
            } else if (multipolygonRelation.hasIncompleteMembers()) {
                // with complex relations the download of the full relation is much faster than download of almost all members, see #18341
                SubclassFilteredCollection<IPrimitive, OsmPrimitive> incompleteMembers = Utils
                        .filteredCollection(DownloadSelectedIncompleteMembersAction.buildSetOfIncompleteMembers(
                                Collections.singleton(multipolygonRelation)), OsmPrimitive.class);

                if (incompleteMembers.size() <= MAX_MEMBERS_TO_DOWNLOAD) {
                    MainApplication.worker
                            .submit(new DownloadRelationMemberTask(multipolygonRelation, incompleteMembers, editLayer));
                } else {
                    MainApplication.worker
                            .submit(new DownloadRelationTask(Collections.singleton(multipolygonRelation), editLayer));

                }
            }
        }
        // create/update multipolygon relation
        MainApplication.worker.submit(new CreateUpdateMultipolygonTask(selectedWays, multipolygonRelation));
    }

    private static Relation getSelectedMultipolygonRelation(Collection<Way> selectedWays, Collection<Relation> selectedRelations) {
        Relation candidate = null;
        if (selectedRelations.size() == 1) {
            candidate = selectedRelations.iterator().next();
            if (!candidate.hasTag("type", "multipolygon"))
                candidate = null;
        } else if (!selectedWays.isEmpty()) {
            for (final Way w : selectedWays) {
                for (OsmPrimitive r : w.getReferrers()) {
                    if (r != candidate && !r.isDisabled() && r instanceof Relation && r.hasTag("type", "multipolygon")) {
                        if (candidate != null)
                            return null; // found another multipolygon relation
                        candidate = (Relation) r;
                    }
                }
            }
        }
        return candidate;
    }

    /**
     * Returns a {@link Pair} of the old multipolygon {@link Relation} (or null) and the newly created/modified multipolygon {@link Relation}.
     * @param selectedWays selected ways
     * @param selectedMultipolygonRelation selected multipolygon relation
     * @return pair of old and new multipolygon relation if a difference was found, else the pair contains the old relation twice
     */
    public static Pair<Relation, Relation> updateMultipolygonRelation(Collection<Way> selectedWays, Relation selectedMultipolygonRelation) {

        // add ways of existing relation to include them in polygon analysis
        Set<Way> ways = new HashSet<>(selectedWays);
        ways.addAll(selectedMultipolygonRelation.getMemberPrimitives(Way.class));

        // even if no way was added the inner/outer roles might be different
        MultipolygonTest mpTest = new MultipolygonTest();
        Relation calculated = mpTest.makeFromWays(ways);
        if (mpTest.getErrors().isEmpty()) {
            return mergeRelationsMembers(selectedMultipolygonRelation, calculated);
        }
        showErrors(mpTest.getErrors());
        return null; //could not make multipolygon.
    }

    /**
     * Merge members of multipolygon relation. Maintains the order of the old relation. May change roles,
     * removes duplicate and non-way members and adds new members found in {@code calculated}.
     * @param old old multipolygon relation
     * @param calculated calculated multipolygon relation
     * @return pair of old and new multipolygon relation if a difference was found, else the pair contains the old relation twice
     */
    private static Pair<Relation, Relation> mergeRelationsMembers(Relation old, Relation calculated) {
        Set<RelationMember> merged = new LinkedHashSet<>();
        boolean foundDiff = false;
        int nonWayMember = 0;
        // maintain order of members in updated relation
        for (RelationMember oldMem :old.getMembers()) {
            if (oldMem.isNode() || oldMem.isRelation()) {
                nonWayMember++;
                continue;
            }
            for (RelationMember newMem : calculated.getMembers()) {
                if (newMem.getMember().equals(oldMem.getMember())) {
                    if (!newMem.getRole().equals(oldMem.getRole())) {
                        foundDiff = true;
                    }
                    foundDiff |= !merged.add(newMem); // detect duplicate members in old relation
                    break;
                }
            }
        }
        if (nonWayMember > 0) {
            foundDiff = true;
            String msg = trn("Non-Way member removed from multipolygon", "Non-Way members removed from multipolygon", nonWayMember);
            GuiHelper.runInEDT(() -> new Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show());
        }
        foundDiff |= merged.addAll(calculated.getMembers());
        if (!foundDiff) {
            return Pair.create(old, old); // unchanged
        }
        Relation toModify = new Relation(old);
        toModify.setMembers(new ArrayList<>(merged));
        return Pair.create(old, toModify);
    }

    /**
     * Returns a {@link Pair} null and the newly created/modified multipolygon {@link Relation}.
     * @param selectedWays selected ways
     * @param showNotif if {@code true}, shows a notification if an error occurs
     * @return pair of null and new multipolygon relation
     */
    public static Pair<Relation, Relation> createMultipolygonRelation(Collection<Way> selectedWays, boolean showNotif) {
        MultipolygonTest mpTest = new MultipolygonTest();
        Relation calculated = mpTest.makeFromWays(selectedWays);
        calculated.setMembers(RelationSorter.sortMembersByConnectivity(calculated.getMembers()));
        if (mpTest.getErrors().isEmpty())
            return Pair.create(null, calculated);
        if (showNotif) {
            showErrors(mpTest.getErrors());
        }
        return null; //could not make multipolygon.
    }

    private static void showErrors(List<TestError> errors) {
        if (!errors.isEmpty()) {
            String errorMessages = errors.stream()
                    .map(TestError::getMessage)
                    .distinct()
                    .collect(Collectors.joining("\n"));
            GuiHelper.runInEDT(() -> new Notification(errorMessages).setIcon(JOptionPane.INFORMATION_MESSAGE).show());
        }
    }

    /**
     * Returns a {@link Pair} of a multipolygon creating/modifying {@link Command} as well as the multipolygon {@link Relation}.
     * @param selectedWays selected ways
     * @param selectedMultipolygonRelation selected multipolygon relation
     * @return pair of command and multipolygon relation
     */
    public static Pair<SequenceCommand, Relation> createMultipolygonCommand(Collection<Way> selectedWays,
            Relation selectedMultipolygonRelation) {

        final Pair<Relation, Relation> rr = selectedMultipolygonRelation == null
                ? createMultipolygonRelation(selectedWays, true)
                : updateMultipolygonRelation(selectedWays, selectedMultipolygonRelation);
        if (rr == null) {
            return null;
        }
        boolean unchanged = rr.a == rr.b;
        final Relation existingRelation = rr.a;
        final Relation relation = rr.b;

        final List<Command> list = removeTagsFromWaysIfNeeded(relation);
        final String commandName;
        if (existingRelation == null) {
            list.add(new AddCommand(selectedWays.iterator().next().getDataSet(), relation));
            commandName = getName(false);
        } else {
            if (!unchanged) {
                list.add(new ChangeCommand(existingRelation, relation));
            }
            if (list.isEmpty()) {
                if (unchanged) {
                    MultipolygonTest mpTest = new MultipolygonTest();
                    mpTest.visit(existingRelation);
                    if (!mpTest.getErrors().isEmpty()) {
                        showErrors(mpTest.getErrors());
                        return null;
                    }
                }

                GuiHelper.runInEDT(() -> new Notification(tr("Nothing changed")).setDuration(Notification.TIME_SHORT)
                        .setIcon(JOptionPane.INFORMATION_MESSAGE).show());
                return null;
            }
            commandName = getName(true);
        }
        return Pair.create(new SequenceCommand(commandName, list), relation);
    }

    /** Enable this action only if something is selected */
    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    /**
      * Enable this action only if something is selected
      *
      * @param selection the current selection, gets tested for emptiness
      */
    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null || selection.isEmpty()) {
            setEnabled(false);
        } else if (update) {
            setEnabled(getSelectedMultipolygonRelation(ds.getSelectedWays(), ds.getSelectedRelations()) != null);
        } else {
            setEnabled(!ds.getSelectedWays().isEmpty());
        }
    }

    private static final List<String> DEFAULT_LINEAR_TAGS = Arrays.asList("barrier", "fence_type", "source");

    /**
     * This method removes tags/value pairs from inner and outer ways and put them on relation if necessary
     * Function was extended in reltoolbox plugin by Zverikk and copied back to the core
     * @param relation the multipolygon style relation to process
     * @return a list of commands to execute
     */
    public static List<Command> removeTagsFromWaysIfNeeded(Relation relation) {
        Map<String, String> values = new HashMap<>(relation.getKeys());

        List<Way> innerWays = new ArrayList<>();
        List<Way> outerWays = new ArrayList<>();

        Set<String> conflictingKeys = new TreeSet<>();

        for (RelationMember m : relation.getMembers()) {

            if (m.hasRole() && "inner".equals(m.getRole()) && m.isWay() && m.getWay().hasKeys()) {
                innerWays.add(m.getWay());
            }

            if (m.hasRole() && "outer".equals(m.getRole()) && m.isWay() && m.getWay().hasKeys()) {
                Way way = m.getWay();
                outerWays.add(way);

                for (String key : way.keySet()) {
                    if (!values.containsKey(key)) { //relation values take precedence
                        values.put(key, way.get(key));
                    } else if (!relation.hasKey(key) && !values.get(key).equals(way.get(key))) {
                        conflictingKeys.add(key);
                    }
                }
            }
        }

        // filter out empty key conflicts - we need second iteration
        if (!Config.getPref().getBoolean("multipoly.alltags", false)) {
            for (RelationMember m : relation.getMembers()) {
                if (m.hasRole() && "outer".equals(m.getRole()) && m.isWay()) {
                    for (String key : values.keySet()) {
                        if (!m.getWay().hasKey(key) && !relation.hasKey(key)) {
                            conflictingKeys.add(key);
                        }
                    }
                }
            }
        }

        for (String key : conflictingKeys) {
            values.remove(key);
        }

        for (String linearTag : Config.getPref().getList("multipoly.lineartagstokeep", DEFAULT_LINEAR_TAGS)) {
            values.remove(linearTag);
        }

        if ("coastline".equals(values.get("natural")))
            values.remove("natural");

        values.put("area", OsmUtils.TRUE_VALUE);

        List<Command> commands = new ArrayList<>();
        boolean moveTags = Config.getPref().getBoolean("multipoly.movetags", true);

        for (Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            List<OsmPrimitive> affectedWays = innerWays.stream().filter(way -> value.equals(way.get(key))).collect(Collectors.toList());

            if (moveTags) {
                // remove duplicated tags from outer ways
                for (Way way : outerWays) {
                    if (way.hasKey(key)) {
                        affectedWays.add(way);
                    }
                }
            }

            if (!affectedWays.isEmpty()) {
                // reset key tag on affected ways
                commands.add(new ChangePropertyCommand(affectedWays, key, null));
            }
        }

        if (moveTags) {
            // add those tag values to the relation
            boolean fixed = false;
            Relation r2 = new Relation(relation);
            for (Entry<String, String> entry : values.entrySet()) {
                String key = entry.getKey();
                if (!r2.hasKey(key) && !"area".equals(key)) {
                    if (relation.isNew())
                        relation.put(key, entry.getValue());
                    else
                        r2.put(key, entry.getValue());
                    fixed = true;
                }
            }
            if (fixed && !relation.isNew()) {
                DataSet ds = relation.getDataSet();
                if (ds == null) {
                    ds = MainApplication.getLayerManager().getEditDataSet();
                }
                commands.add(new ChangeCommand(ds, relation, r2));
            }
        }

        return commands;
    }
}
