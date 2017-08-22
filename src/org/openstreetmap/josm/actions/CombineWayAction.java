// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.corrector.ReverseWayTagCorrector;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeGraph;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * Combines multiple ways into one.
 * @since 213
 */
public class CombineWayAction extends JosmAction {

    private static final BooleanProperty PROP_REVERSE_WAY = new BooleanProperty("tag-correction.reverse-way", true);

    /**
     * Constructs a new {@code CombineWayAction}.
     */
    public CombineWayAction() {
        super(tr("Combine Way"), "combineway", tr("Combine several ways into one."),
                Shortcut.registerShortcut("tools:combineway", tr("Tool: {0}", tr("Combine Way")), KeyEvent.VK_C, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/CombineWay"));
    }

    protected static boolean confirmChangeDirectionOfWays() {
        return new ExtendedDialog(Main.parent,
                tr("Change directions?"),
                tr("Reverse and Combine"), tr("Cancel"))
            .setButtonIcons("wayflip", "cancel")
            .setContent(tr("The ways can not be combined in their current directions.  "
                + "Do you want to reverse some of them?"))
            .toggleEnable("combineway-reverse")
            .showDialog()
            .getValue() == 1;
    }

    protected static void warnCombiningImpossible() {
        String msg = tr("Could not combine ways<br>"
                + "(They could not be merged into a single string of nodes)");
        new Notification(msg)
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .show();
    }

    protected static Way getTargetWay(Collection<Way> combinedWays) {
        // init with an arbitrary way
        Way targetWay = combinedWays.iterator().next();

        // look for the first way already existing on
        // the server
        for (Way w : combinedWays) {
            targetWay = w;
            if (!w.isNew()) {
                break;
            }
        }
        return targetWay;
    }

    /**
     * Combine multiple ways into one.
     * @param ways the way to combine to one way
     * @return null if ways cannot be combined. Otherwise returns the combined ways and the commands to combine
     * @throws UserCancelException if the user cancelled a dialog.
     */
    public static Pair<Way, Command> combineWaysWorker(Collection<Way> ways) throws UserCancelException {

        // prepare and clean the list of ways to combine
        //
        if (ways == null || ways.isEmpty())
            return null;
        ways.remove(null); // just in case -  remove all null ways from the collection

        // remove duplicates, preserving order
        ways = new LinkedHashSet<>(ways);
        // remove incomplete ways
        ways.removeIf(OsmPrimitive::isIncomplete);
        // we need at least two ways
        if (ways.size() < 2)
            return null;

        List<DataSet> dataSets = ways.stream().map(Way::getDataSet).distinct().collect(Collectors.toList());
        if (dataSets.size() != 1) {
            throw new IllegalArgumentException("Cannot combine ways of multiple data sets.");
        }

        // try to build a new way which includes all the combined ways
        NodeGraph graph = NodeGraph.createNearlyUndirectedGraphFromNodeWays(ways);
        List<Node> path = graph.buildSpanningPath();
        if (path == null) {
            warnCombiningImpossible();
            return null;
        }
        // check whether any ways have been reversed in the process
        // and build the collection of tags used by the ways to combine
        //
        TagCollection wayTags = TagCollection.unionOfAllPrimitives(ways);

        final List<Command> reverseWayTagCommands = new LinkedList<>();
        List<Way> reversedWays = new LinkedList<>();
        List<Way> unreversedWays = new LinkedList<>();
        for (Way w: ways) {
            // Treat zero or one-node ways as unreversed as Combine action action is a good way to fix them (see #8971)
            if (w.getNodesCount() < 2 || (path.indexOf(w.getNode(0)) + 1) == path.lastIndexOf(w.getNode(1))) {
                unreversedWays.add(w);
            } else {
                reversedWays.add(w);
            }
        }
        // reverse path if all ways have been reversed
        if (unreversedWays.isEmpty()) {
            Collections.reverse(path);
            unreversedWays = reversedWays;
            reversedWays = null;
        }
        if ((reversedWays != null) && !reversedWays.isEmpty()) {
            if (!confirmChangeDirectionOfWays()) return null;
            // filter out ways that have no direction-dependent tags
            unreversedWays = ReverseWayTagCorrector.irreversibleWays(unreversedWays);
            reversedWays = ReverseWayTagCorrector.irreversibleWays(reversedWays);
            // reverse path if there are more reversed than unreversed ways with direction-dependent tags
            if (reversedWays.size() > unreversedWays.size()) {
                Collections.reverse(path);
                List<Way> tempWays = unreversedWays;
                unreversedWays = null;
                reversedWays = tempWays;
            }
            // if there are still reversed ways with direction-dependent tags, reverse their tags
            if (!reversedWays.isEmpty() && PROP_REVERSE_WAY.get()) {
                List<Way> unreversedTagWays = new ArrayList<>(ways);
                unreversedTagWays.removeAll(reversedWays);
                ReverseWayTagCorrector reverseWayTagCorrector = new ReverseWayTagCorrector();
                List<Way> reversedTagWays = new ArrayList<>(reversedWays.size());
                for (Way w : reversedWays) {
                    Way wnew = new Way(w);
                    reversedTagWays.add(wnew);
                    reverseWayTagCommands.addAll(reverseWayTagCorrector.execute(w, wnew));
                }
                if (!reverseWayTagCommands.isEmpty()) {
                    // commands need to be executed for CombinePrimitiveResolverDialog
                    Main.main.undoRedo.add(new SequenceCommand(tr("Reverse Ways"), reverseWayTagCommands));
                }
                wayTags = TagCollection.unionOfAllPrimitives(reversedTagWays);
                wayTags.add(TagCollection.unionOfAllPrimitives(unreversedTagWays));
            }
        }

        // create the new way and apply the new node list
        //
        Way targetWay = getTargetWay(ways);
        Way modifiedTargetWay = new Way(targetWay);
        modifiedTargetWay.setNodes(path);

        final List<Command> resolution;
        try {
            resolution = CombinePrimitiveResolverDialog.launchIfNecessary(wayTags, ways, Collections.singleton(targetWay));
        } finally {
            if (!reverseWayTagCommands.isEmpty()) {
                // undo reverseWayTagCorrector and merge into SequenceCommand below
                Main.main.undoRedo.undo();
            }
        }

        List<Command> cmds = new LinkedList<>();
        List<Way> deletedWays = new LinkedList<>(ways);
        deletedWays.remove(targetWay);

        cmds.add(new ChangeCommand(dataSets.get(0), targetWay, modifiedTargetWay));
        cmds.addAll(reverseWayTagCommands);
        cmds.addAll(resolution);
        cmds.add(new DeleteCommand(dataSets.get(0), deletedWays));
        final Command sequenceCommand = new SequenceCommand(/* for correct i18n of plural forms - see #9110 */
                trn("Combine {0} way", "Combine {0} ways", ways.size(), ways.size()), cmds);

        return new Pair<>(targetWay, sequenceCommand);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null)
            return;
        Collection<Way> selectedWays = ds.getSelectedWays();
        if (selectedWays.size() < 2) {
            new Notification(
                    tr("Please select at least two ways to combine."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }
        // combine and update gui
        Pair<Way, Command> combineResult;
        try {
            combineResult = combineWaysWorker(selectedWays);
        } catch (UserCancelException ex) {
            Logging.trace(ex);
            return;
        }

        if (combineResult == null)
            return;
        final Way selectedWay = combineResult.a;
        Main.main.undoRedo.add(combineResult.b);
        if (selectedWay != null) {
            GuiHelper.runInEDT(() -> ds.setSelected(selectedWay));
        }
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        int numWays = 0;
        for (OsmPrimitive osm : selection) {
            if (osm instanceof Way && !osm.isIncomplete() && ++numWays >= 2) {
                break;
            }
        }
        setEnabled(numWays >= 2);
    }
}
