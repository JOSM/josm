// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Follow line action - Makes easier to draw a line that shares points with another line
 *
 * Aimed at those who want to draw two or more lines related with
 * each other, but carry different information (i.e. a river acts as boundary at
 * some part of its course. It preferable to have a separated boundary line than to
 * mix totally different kind of features in one single way).
 *
 * @author Germán Márquez Mejía
 */
public class FollowLineAction extends JosmAction {

    public FollowLineAction() {
        super(
                tr("Follow line"),
                "followline.png",
                tr("Continues drawing a line that shares nodes with another line."),
                Shortcut.registerShortcut("tools:followline", tr(
                "Tool: {0}", tr("Follow")),
                KeyEvent.VK_F, Shortcut.DIRECT), true);
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

    @Override
    public void actionPerformed(ActionEvent evt) {
        OsmDataLayer osmLayer = Main.main.getEditLayer();
        if (osmLayer == null)
            return;
        if (!(Main.map.mapMode instanceof DrawAction)) return; // We are not on draw mode

        Collection<Node> selectedPoints = osmLayer.data.getSelectedNodes();
        Collection<Way> selectedLines = osmLayer.data.getSelectedWays();
        if ((selectedPoints.size() > 1) || (selectedLines.size() != 1)) // Unsuitable selection
            return;

        Node last = ((DrawAction) Main.map.mapMode).getCurrentBaseNode();
        if (last == null)
            return;
        Way follower = selectedLines.iterator().next();
        if (follower.isClosed())    /* Don't loop until OOM */
            return;
        Node prev = follower.getNode(1);
        boolean reversed = true;
        if (follower.lastNode().equals(last)) {
            prev = follower.getNode(follower.getNodesCount() - 2);
            reversed = false;
        }
        List<OsmPrimitive> referrers = last.getReferrers();
        if (referrers.size() < 2) return; // There's nothing to follow

        Node newPoint = null;
        for (OsmPrimitive referrer : referrers) {
            if (!referrer.getType().equals(OsmPrimitiveType.WAY)) { // Can't follow points or relations
                continue;
            }
            Way toFollow = (Way) referrer;
            if (toFollow.equals(follower)) {
                continue;
            }
            Set<Node> points = toFollow.getNeighbours(last);
            if (!points.remove(prev) || points.isEmpty())
                continue;
            if (points.size() > 1)    // Ambiguous junction?
                return;

            Node newPointCandidate = points.iterator().next();

            if ((newPoint != null) && (newPoint != newPointCandidate))
                return;         // Ambiguous junction, force to select next

            newPoint = newPointCandidate;
        }
        if (newPoint != null) {
            Way newFollower = new Way(follower);
            if (reversed) {
                newFollower.addNode(0, newPoint);
            } else {
                newFollower.addNode(newPoint);
            }
            Main.main.undoRedo.add(new ChangeCommand(follower, newFollower));
            osmLayer.data.clearSelection();
            osmLayer.data.addSelected(newFollower);
            osmLayer.data.addSelected(newPoint);
            // "viewport following" mode for tracing long features
            // from aerial imagery or GPS tracks.
            if (Main.map.mapView.viewportFollowing) {
                Main.map.mapView.smoothScrollTo(newPoint.getEastNorth());
            }
        }
    }
}
