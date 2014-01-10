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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Delete unnecessary nodes from a way
 * @since 2575
 */
public class SimplifyWayAction extends JosmAction {
    
    /**
     * Constructs a new {@code SimplifyWayAction}.
     */
    public SimplifyWayAction() {
        super(tr("Simplify Way"), "simplify", tr("Delete unnecessary nodes from a way."), Shortcut.registerShortcut("tools:simplify", tr("Tool: {0}", tr("Simplify Way")),
                KeyEvent.VK_Y, Shortcut.SHIFT), true);
        putValue("help", ht("/Action/SimplifyWay"));
    }

    protected boolean confirmWayWithNodesOutsideBoundingBox(List<? extends OsmPrimitive> primitives) {
        return DeleteCommand.checkAndConfirmOutlyingDelete(primitives, null);
    }

    protected void alertSelectAtLeastOneWay() {
        new Notification(
                tr("Please select at least one way to simplify."))
                .setIcon(JOptionPane.WARNING_MESSAGE)
                .setDuration(Notification.TIME_SHORT)
                .setHelpTopic(HelpUtil.ht("/Action/SimplifyWay#SelectAWayToSimplify"))
                .show();
    }

    protected boolean confirmSimplifyManyWays(int numWays) {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Yes"),
                        ImageProvider.get("ok"),
                        tr("Simplify all selected ways"),
                        null
                        ),
                        new ButtonSpec(
                                tr("Cancel"),
                                ImageProvider.get("cancel"),
                                tr("Cancel operation"),
                                null
                                )
        };
        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                tr(
                        "The selection contains {0} ways. Are you sure you want to simplify them all?",
                        numWays
                        ),
                        tr("Simplify ways?"),
                        JOptionPane.WARNING_MESSAGE,
                        null, // no special icon
                        options,
                        options[0],
                        HelpUtil.ht("/Action/SimplifyWay#ConfirmSimplifyAll")
                );
        return ret == 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet ds = getCurrentDataSet();
        ds.beginUpdate();
        try
        {
            List<Way> ways = OsmPrimitive.getFilteredList(ds.getSelected(), Way.class);
            if (ways.isEmpty()) {
                alertSelectAtLeastOneWay();
                return;
            } else if (!confirmWayWithNodesOutsideBoundingBox(ways))
                return;
            else if (ways.size() > 10) {
                if (!confirmSimplifyManyWays(ways.size()))
                    return;
            }

            Collection<Command> allCommands = new LinkedList<Command>();
            for (Way way: ways) {
                SequenceCommand simplifyCommand = simplifyWay(way);
                if (simplifyCommand == null) {
                    continue;
                }
                allCommands.add(simplifyCommand);
            }
            if (allCommands.isEmpty()) return;
            SequenceCommand rootCommand = new SequenceCommand(
                    trn("Simplify {0} way", "Simplify {0} ways", allCommands.size(), allCommands.size()),
                    allCommands
                    );
            Main.main.undoRedo.add(rootCommand);
        } finally {
            ds.endUpdate();
        }
        Main.map.repaint();
    }

    /**
     * Replies true if <code>node</code> is a required node which can't be removed
     * in order to simplify the way.
     *
     * @param way the way to be simplified
     * @param node the node to check
     * @return true if <code>node</code> is a required node which can't be removed
     * in order to simplify the way.
     */
    protected boolean isRequiredNode(Way way, Node node) {
        boolean isRequired =  Collections.frequency(way.getNodes(), node) > 1;
        if (! isRequired) {
            List<OsmPrimitive> parents = new LinkedList<OsmPrimitive>();
            parents.addAll(node.getReferrers());
            parents.remove(way);
            isRequired = !parents.isEmpty();
        }
        if (!isRequired) {
            isRequired = node.isTagged();
        }
        return isRequired;
    }

    /**
     * Simplifies a way with default threshold (read from preferences).
     *
     * @param w the way to simplify
     * @return The sequence of commands to run
     * @since 6411
     */
    public final SequenceCommand simplifyWay(Way w) {
        return simplifyWay(w, Main.pref.getDouble("simplify-way.max-error", 3.0));
    }

    /**
     * Simplifies a way with a given threshold.
     *
     * @param w the way to simplify
     * @return The sequence of commands to run
     * @since 6411
     */
    public SequenceCommand simplifyWay(Way w, double threshold) {
        int lower = 0;
        int i = 0;
        List<Node> newNodes = new ArrayList<Node>(w.getNodesCount());
        while(i < w.getNodesCount()){
            if (isRequiredNode(w,w.getNode(i))) {
                // copy a required node to the list of new nodes. Simplify not possible
                newNodes.add(w.getNode(i));
                i++;
                lower++;
                continue;
            }
            i++;
            // find the longest sequence of not required nodes ...
            while(i<w.getNodesCount() && !isRequiredNode(w,w.getNode(i))) {
                i++;
            }
            // ... and simplify them
            buildSimplifiedNodeList(w.getNodes(), lower, Math.min(w.getNodesCount()-1, i), threshold,newNodes);
            lower=i;
            i++;
        }

        HashSet<Node> delNodes = new HashSet<Node>();
        delNodes.addAll(w.getNodes());
        delNodes.removeAll(newNodes);

        if (delNodes.isEmpty()) return null;

        Collection<Command> cmds = new LinkedList<Command>();
        Way newWay = new Way(w);
        newWay.setNodes(newNodes);
        cmds.add(new ChangeCommand(w, newWay));
        cmds.add(new DeleteCommand(delNodes));
        w.getDataSet().clearSelection(delNodes);
        return new SequenceCommand(trn("Simplify Way (remove {0} node)", "Simplify Way (remove {0} nodes)", delNodes.size(), delNodes.size()), cmds);
    }

    /**
     * Builds the simplified list of nodes for a way segment given by a lower index <code>from</code>
     * and an upper index <code>to</code>
     *
     * @param wnew the way to simplify
     * @param from the lower index
     * @param to the upper index
     * @param threshold
     */
    protected void buildSimplifiedNodeList(List<Node> wnew, int from, int to, double threshold, List<Node> simplifiedNodes) {

        Node fromN = wnew.get(from);
        Node toN = wnew.get(to);

        // Get max xte
        int imax = -1;
        double xtemax = 0;
        for (int i = from + 1; i < to; i++) {
            Node n = wnew.get(i);
            double xte = Math.abs(EARTH_RAD
                    * xtd(fromN.getCoor().lat() * Math.PI / 180, fromN.getCoor().lon() * Math.PI / 180, toN.getCoor().lat() * Math.PI
                            / 180, toN.getCoor().lon() * Math.PI / 180, n.getCoor().lat() * Math.PI / 180, n.getCoor().lon() * Math.PI
                            / 180));
            if (xte > xtemax) {
                xtemax = xte;
                imax = i;
            }
        }

        if (imax != -1 && xtemax >= threshold) {
            // Segment cannot be simplified - try shorter segments
            buildSimplifiedNodeList(wnew, from, imax,threshold,simplifiedNodes);
            buildSimplifiedNodeList(wnew, imax, to, threshold,simplifiedNodes);
        } else {
            // Simplify segment
            if (simplifiedNodes.isEmpty() || simplifiedNodes.get(simplifiedNodes.size()-1) != fromN) {
                simplifiedNodes.add(fromN);
            }
            if (fromN != toN) {
                simplifiedNodes.add(toN);
            }
        }
    }

    public static final double EARTH_RAD = 6378137.0;

    /* From Aviaton Formulary v1.3
     * http://williams.best.vwh.net/avform.htm
     */
    public static double dist(double lat1, double lon1, double lat2, double lon2) {
        return 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((lat1 - lat2) / 2), 2) + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin((lon1 - lon2) / 2), 2)));
    }

    public static double course(double lat1, double lon1, double lat2, double lon2) {
        return Math.atan2(Math.sin(lon1 - lon2) * Math.cos(lat2), Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(lon1 - lon2))
                % (2 * Math.PI);
    }

    public static double xtd(double lat1, double lon1, double lat2, double lon2, double lat3, double lon3) {
        double dist_AD = dist(lat1, lon1, lat3, lon3);
        double crs_AD = course(lat1, lon1, lat3, lon3);
        double crs_AB = course(lat1, lon1, lat2, lon2);
        return Math.asin(Math.sin(dist_AD) * Math.sin(crs_AD - crs_AB));
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
        setEnabled(selection != null && !selection.isEmpty());
    }
}
