// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.corrector.ReverseWayNoTagCorrector;
import org.openstreetmap.josm.corrector.ReverseWayTagCorrector;
import org.openstreetmap.josm.corrector.UserCancelException;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

public final class ReverseWayAction extends JosmAction {

    public static class ReverseWayResult {
        private Way newWay;
        private Collection<Command> tagCorrectionCommands;
        private Command reverseCommand;

        public ReverseWayResult(Way newWay, Collection<Command> tagCorrectionCommands, Command reverseCommand) {
            this.newWay = newWay;
            this.tagCorrectionCommands = tagCorrectionCommands;
            this.reverseCommand = reverseCommand;
        }

        public Way getNewWay() {
            return newWay;
        }

        public Collection<Command> getCommands() {
            List<Command> c = new ArrayList<Command>();
            c.addAll(tagCorrectionCommands);
            c.add(reverseCommand);
            return c;
        }

        public Command getAsSequenceCommand() {
            return new SequenceCommand(tr("Reverse way"), getCommands());
        }

        public Command getReverseCommand() {
            return reverseCommand;
        }

        public Collection<Command> getTagCorrectionCommands() {
            return tagCorrectionCommands;
        }
    }

    public ReverseWayAction() {
        super(tr("Reverse Ways"), "wayflip", tr("Reverse the direction of all selected ways."),
                Shortcut.registerShortcut("tools:reverse", tr("Tool: {0}", tr("Reverse Ways")), KeyEvent.VK_R, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/ReverseWays"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (! isEnabled())
            return;
        if (getCurrentDataSet() == null)
            return;

        final Collection<Way> sel = getCurrentDataSet().getSelectedWays();
        if (sel.isEmpty()) {
            new Notification(
                    tr("Please select at least one way."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        boolean propertiesUpdated = false;
        Collection<Command> c = new LinkedList<Command>();
        for (Way w : sel) {
            ReverseWayResult revResult;
            try {
                revResult = reverseWay(w);
            } catch (UserCancelException ex) {
                return;
            }
            c.addAll(revResult.getCommands());
            propertiesUpdated |= !revResult.getTagCorrectionCommands().isEmpty();
        }
        Main.main.undoRedo.add(new SequenceCommand(tr("Reverse ways"), c));
        if (propertiesUpdated) {
            getCurrentDataSet().fireSelectionChanged();
        }
        Main.map.repaint();
    }

    /**
     * @param w the way
     * @return the reverse command and the tag correction commands
     * @throws UserCancelException if user cancels a reverse warning dialog
     */
    public static ReverseWayResult reverseWay(Way w) throws UserCancelException {
        ReverseWayNoTagCorrector.checkAndConfirmReverseWay(w);
        Way wnew = new Way(w);
        List<Node> nodesCopy = wnew.getNodes();
        Collections.reverse(nodesCopy);
        wnew.setNodes(nodesCopy);

        Collection<Command> corrCmds = Collections.<Command>emptyList();
        if (Main.pref.getBoolean("tag-correction.reverse-way", true)) {
            corrCmds = (new ReverseWayTagCorrector()).execute(w, wnew);
        }
        return new ReverseWayResult(wnew, corrCmds, new ChangeCommand(w, wnew));
    }

    protected int getNumWaysInSelection() {
        if (getCurrentDataSet() == null) return 0;
        int ret = 0;
        for (OsmPrimitive primitive : getCurrentDataSet().getSelected()) {
            if (primitive instanceof Way) {
                ret++;
            }
        }
        return ret;
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(Utils.exists(selection, OsmPrimitive.wayPredicate));
    }
}
