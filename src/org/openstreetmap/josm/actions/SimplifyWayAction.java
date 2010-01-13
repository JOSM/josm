// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

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
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

public class SimplifyWayAction extends JosmAction {
    public SimplifyWayAction() {
        super(tr("Simplify Way"), "simplify", tr("Delete unnecessary nodes from a way."), Shortcut.registerShortcut("tools:simplify", tr("Tool: {0}", tr("Simplify Way")),
        KeyEvent.VK_Y, Shortcut.GROUP_EDIT, Shortcut.SHIFT_DEFAULT), true);
    }

    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> selection = Main.main.getCurrentDataSet().getSelected();

        int ways = 0;
        LinkedList<Bounds> bounds = new LinkedList<Bounds>();
        OsmDataLayer dataLayer = Main.map.mapView.getEditLayer();
        for (DataSource ds : dataLayer.data.dataSources) {
            if (ds.bounds != null)
                bounds.add(ds.bounds);
        }
        for (OsmPrimitive prim : selection) {
            if (prim instanceof Way) {
                if (bounds.size() > 0) {
                    Way way = (Way) prim;
                    // We check if each node of each way is at least in one download
                    // bounding box. Otherwise nodes may get deleted that are necessary by
                    // unloaded ways (see Ticket #1594)
                    for (Node node : way.getNodes()) {
                        boolean isInsideOneBoundingBox = false;
                        for (Bounds b : bounds) {
                            if (b.contains(node.getCoor())) {
                                isInsideOneBoundingBox = true;
                                break;
                            }
                        }
                        if (!isInsideOneBoundingBox) {
                            int option = JOptionPane.showConfirmDialog(Main.parent,
                                    trn("The selected way has nodes outside of the downloaded data region.",
                                            "The selected ways have nodes outside of the downloaded data region.",
                                            Main.main.getCurrentDataSet().getSelectedWays().size()) + "\n"
                                            + tr("This can lead to nodes being deleted accidentally.") + "\n"
                                            + tr("Are you really sure to continue?"),
                                    tr("Please abort if you are not sure"), JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.WARNING_MESSAGE);

                            if (option != JOptionPane.YES_OPTION)
                                return;
                            break;
                        }
                    }
                }

                ways++;
            }
        }

        if (ways == 0) {
            JOptionPane.showMessageDialog(Main.parent, tr("Please select at least one way to simplify."));
            return;
        } else if (ways > 10) {
            int option = JOptionPane.showConfirmDialog(Main.parent, trn(
                    "The selection contains {0} way. Are you sure you want to simplify it?",
                    "The selection contains {0} ways. Are you sure you want to simplify them all?",
                    ways,ways),
                    tr("Are you sure?"), JOptionPane.YES_NO_OPTION);
            if (option != JOptionPane.YES_OPTION)
                return;
        }

        for (OsmPrimitive prim : selection) {
            if (prim instanceof Way) {
                simplifyWay((Way) prim);
            }
        }
    }

    public void simplifyWay(Way w) {
        double threshold = Double.parseDouble(Main.pref.get("simplify-way.max-error", "3"));

        Way wnew = new Way(w);

        int toI = wnew.getNodesCount() - 1;
        List<OsmPrimitive> parents = new ArrayList<OsmPrimitive>();
        for (int i = wnew.getNodesCount() - 1; i >= 0; i--) {
            parents.addAll(w.getNode(i).getReferrers());
            boolean used = false;
            if (parents.size() == 2) {
                used = Collections.frequency(w.getNodes(), wnew.getNode(i)) > 1;
            } else {
                parents.remove(w);
                parents.remove(wnew);
                used = !parents.isEmpty();
            }
            if (!used)
                used = wnew.getNode(i).isTagged();

            if (used) {
                simplifyWayRange(wnew, i, toI, threshold);
                toI = i;
            }
        }
        simplifyWayRange(wnew, 0, toI, threshold);

        HashSet<Node> delNodes = new HashSet<Node>();
        delNodes.addAll(w.getNodes());
        delNodes.removeAll(wnew.getNodes());

        if (wnew.getNodesCount() != w.getNodesCount()) {
            Collection<Command> cmds = new LinkedList<Command>();
            cmds.add(new ChangeCommand(w, wnew));
            cmds.add(new DeleteCommand(delNodes));
            Main.main.undoRedo.add(new SequenceCommand(trn("Simplify Way (remove {0} node)", "Simplify Way (remove {0} nodes)", delNodes.size(), delNodes.size()), cmds));
            Main.map.repaint();
        }
    }

    public void simplifyWayRange(Way wnew, int from, int to, double thr) {
        if (to - from >= 2) {
            ArrayList<Node> ns = new ArrayList<Node>();
            simplifyWayRange(wnew, from, to, ns, thr);
            List<Node> nodes = wnew.getNodes();
            for (int j = to - 1; j > from; j--) {
                nodes.remove(j);
            }
            nodes.addAll(from + 1, ns);
            wnew.setNodes(nodes);
        }
    }

    /*
     * Takes an interval [from,to] and adds nodes from (from,to) to ns.
     * (from and to are indices of wnew.nodes.)
     */
    public void simplifyWayRange(Way wnew, int from, int to, ArrayList<Node> ns, double thr) {
        Node fromN = wnew.getNode(from), toN = wnew.getNode(to);

        int imax = -1;
        double xtemax = 0;
        for (int i = from + 1; i < to; i++) {
            Node n = wnew.getNode(i);
            double xte = Math.abs(EARTH_RAD
                    * xtd(fromN.getCoor().lat() * Math.PI / 180, fromN.getCoor().lon() * Math.PI / 180, toN.getCoor().lat() * Math.PI
                            / 180, toN.getCoor().lon() * Math.PI / 180, n.getCoor().lat() * Math.PI / 180, n.getCoor().lon() * Math.PI
                            / 180));
            if (xte > xtemax) {
                xtemax = xte;
                imax = i;
            }
        }

        if (imax != -1 && xtemax >= thr) {
            simplifyWayRange(wnew, from, imax, ns, thr);
            ns.add(wnew.getNode(imax));
            simplifyWayRange(wnew, imax, to, ns, thr);
        }
    }

    public static double EARTH_RAD = 6378137.0;

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
