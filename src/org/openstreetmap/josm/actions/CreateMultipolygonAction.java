// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.relation.DownloadSelectedIncompleteMembersAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder.JoinedPolygon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationTask;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.sort.RelationSorter;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;
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

    /**
     * Constructs a new {@code CreateMultipolygonAction}.
     * @param update {@code true} if the multipolygon must be updated, {@code false} if it must be created
     */
    public CreateMultipolygonAction(final boolean update) {
        super(getName(update), /* ICON */ "multipoly_create", getName(update),
                /* atleast three lines for each shortcut or the server extractor fails */
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
            final Relation relation = commandAndRelation.b;

            // to avoid EDT violations
            SwingUtilities.invokeLater(() -> {
                    Main.main.undoRedo.add(command);

                    // Use 'SwingUtilities.invokeLater' to make sure the relationListDialog
                    // knows about the new relation before we try to select it.
                    // (Yes, we are already in event dispatch thread. But DatasetEventManager
                    // uses 'SwingUtilities.invokeLater' to fire events so we have to do the same.)
                    SwingUtilities.invokeLater(() -> {
                            MainApplication.getMap().relationListDialog.selectRelation(relation);
                            if (Main.pref.getBoolean("multipoly.show-relation-editor", false)) {
                                //Open relation edit window, if set up in preferences
                                RelationEditor editor = RelationEditor.getEditor(Main.getLayerManager().getEditLayer(), relation, null);
                                editor.setModal(true);
                                editor.setVisible(true);
                            } else {
                                Main.getLayerManager().getEditLayer().setRecentRelation(relation);
                            }
                    });
            });
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet dataSet = Main.getLayerManager().getEditDataSet();
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

        // download incomplete relation or incomplete members if necessary
        if (multipolygonRelation != null) {
            if (!multipolygonRelation.isNew() && multipolygonRelation.isIncomplete()) {
                MainApplication.worker.submit(
                        new DownloadRelationTask(Collections.singleton(multipolygonRelation), Main.getLayerManager().getEditLayer()));
            } else if (multipolygonRelation.hasIncompleteMembers()) {
                MainApplication.worker.submit(new DownloadRelationMemberTask(multipolygonRelation,
                        DownloadSelectedIncompleteMembersAction.buildSetOfIncompleteMembers(Collections.singleton(multipolygonRelation)),
                        Main.getLayerManager().getEditLayer()));
            }
        }
        // create/update multipolygon relation
        MainApplication.worker.submit(new CreateUpdateMultipolygonTask(selectedWays, multipolygonRelation));
    }

    private Relation getSelectedMultipolygonRelation() {
        DataSet ds = getLayerManager().getEditDataSet();
        return getSelectedMultipolygonRelation(ds.getSelectedWays(), ds.getSelectedRelations());
    }

    private static Relation getSelectedMultipolygonRelation(Collection<Way> selectedWays, Collection<Relation> selectedRelations) {
        if (selectedRelations.size() == 1 && "multipolygon".equals(selectedRelations.iterator().next().get("type"))) {
            return selectedRelations.iterator().next();
        } else {
            final Set<Relation> relatedRelations = new HashSet<>();
            for (final Way w : selectedWays) {
                relatedRelations.addAll(Utils.filteredCollection(w.getReferrers(), Relation.class));
            }
            return relatedRelations.size() == 1 ? relatedRelations.iterator().next() : null;
        }
    }

    /**
     * Returns a {@link Pair} of the old multipolygon {@link Relation} (or null) and the newly created/modified multipolygon {@link Relation}.
     * @param selectedWays selected ways
     * @param selectedMultipolygonRelation selected multipolygon relation
     * @return pair of old and new multipolygon relation
     */
    public static Pair<Relation, Relation> updateMultipolygonRelation(Collection<Way> selectedWays, Relation selectedMultipolygonRelation) {

        // add ways of existing relation to include them in polygon analysis
        Set<Way> ways = new HashSet<>(selectedWays);
        ways.addAll(selectedMultipolygonRelation.getMemberPrimitives(Way.class));

        final MultipolygonBuilder polygon = analyzeWays(ways, true);
        if (polygon == null) {
            return null; //could not make multipolygon.
        } else {
            return Pair.create(selectedMultipolygonRelation, createRelation(polygon, selectedMultipolygonRelation));
        }
    }

    /**
     * Returns a {@link Pair} null and the newly created/modified multipolygon {@link Relation}.
     * @param selectedWays selected ways
     * @param showNotif if {@code true}, shows a notification if an error occurs
     * @return pair of null and new multipolygon relation
     */
    public static Pair<Relation, Relation> createMultipolygonRelation(Collection<Way> selectedWays, boolean showNotif) {

        final MultipolygonBuilder polygon = analyzeWays(selectedWays, showNotif);
        if (polygon == null) {
            return null; //could not make multipolygon.
        } else {
            return Pair.create(null, createRelation(polygon, null));
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
        final Relation existingRelation = rr.a;
        final Relation relation = rr.b;

        final List<Command> list = removeTagsFromWaysIfNeeded(relation);
        final String commandName;
        if (existingRelation == null) {
            list.add(new AddCommand(relation));
            commandName = getName(false);
        } else {
            list.add(new ChangeCommand(existingRelation, relation));
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
      * @param selection the current selection, gets tested for emptyness
      */
    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null) {
            setEnabled(false);
        } else if (update) {
            setEnabled(getSelectedMultipolygonRelation() != null);
        } else {
            setEnabled(!getLayerManager().getEditDataSet().getSelectedWays().isEmpty());
        }
    }

    /**
     * This method analyzes ways and creates multipolygon.
     * @param selectedWays list of selected ways
     * @param showNotif if {@code true}, shows a notification if an error occurs
     * @return <code>null</code>, if there was a problem with the ways.
     */
    private static MultipolygonBuilder analyzeWays(Collection<Way> selectedWays, boolean showNotif) {

        MultipolygonBuilder pol = new MultipolygonBuilder();
        final String error = pol.makeFromWays(selectedWays);

        if (error != null) {
            if (showNotif) {
                GuiHelper.runInEDT(() ->
                        new Notification(error)
                        .setIcon(JOptionPane.INFORMATION_MESSAGE)
                        .show());
            }
            return null;
        } else {
            return pol;
        }
    }

    /**
     * Builds a relation from polygon ways.
     * @param pol data storage class containing polygon information
     * @param clone relation to clone, can be null
     * @return multipolygon relation
     */
    private static Relation createRelation(MultipolygonBuilder pol, Relation clone) {
        // Create new relation
        Relation rel = clone != null ? new Relation(clone) : new Relation();
        rel.put("type", "multipolygon");
        // Add ways to it
        for (JoinedPolygon jway:pol.outerWays) {
            addMembers(jway, rel, "outer");
        }

        for (JoinedPolygon jway:pol.innerWays) {
            addMembers(jway, rel, "inner");
        }

        if (clone == null) {
            rel.setMembers(RelationSorter.sortMembersByConnectivity(rel.getMembers()));
        }

        return rel;
    }

    private static void addMembers(JoinedPolygon polygon, Relation rel, String role) {
        final int count = rel.getMembersCount();
        final Set<Way> ways = new HashSet<>(polygon.ways);
        for (int i = 0; i < count; i++) {
            final RelationMember m = rel.getMember(i);
            if (ways.contains(m.getMember()) && !role.equals(m.getRole())) {
                rel.setMember(i, new RelationMember(role, m.getMember()));
            }
        }
        ways.removeAll(rel.getMemberPrimitivesList());
        for (final Way way : ways) {
            rel.addMember(new RelationMember(role, way));
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
        if (!Main.pref.getBoolean("multipoly.alltags", false)) {
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

        for (String linearTag : Main.pref.getCollection("multipoly.lineartagstokeep", DEFAULT_LINEAR_TAGS)) {
            values.remove(linearTag);
        }

        if ("coastline".equals(values.get("natural")))
            values.remove("natural");

        values.put("area", OsmUtils.TRUE_VALUE);

        List<Command> commands = new ArrayList<>();
        boolean moveTags = Main.pref.getBoolean("multipoly.movetags", true);

        for (Entry<String, String> entry : values.entrySet()) {
            List<OsmPrimitive> affectedWays = new ArrayList<>();
            String key = entry.getKey();
            String value = entry.getValue();

            for (Way way : innerWays) {
                if (value.equals(way.get(key))) {
                    affectedWays.add(way);
                }
            }

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
            if (fixed && !relation.isNew())
                commands.add(new ChangeCommand(relation, r2));
        }

        return commands;
    }
}
