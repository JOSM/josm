// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.PolarCoor;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Tools / Orthogonalize
 *
 * Align edges of a way so all angles are angles of 90 or 180 degrees.
 * See USAGE String below.
 */
public final class OrthogonalizeAction extends JosmAction {
    private static final String USAGE = tr(
            "<h3>When one or more ways are selected, the shape is adjusted such, that all angles are 90 or 180 degrees.</h3>"+
            "You can add two nodes to the selection. Then, the direction is fixed by these two reference nodes. "+
            "(Afterwards, you can undo the movement for certain nodes:<br>"+
            "Select them and press the shortcut for Orthogonalize / Undo. The default is Shift-Q.)");

    private static final double EPSILON = 1E-6;

    /**
     * Constructs a new {@code OrthogonalizeAction}.
     */
    public OrthogonalizeAction() {
        super(tr("Orthogonalize Shape"),
                "ortho",
                tr("Move nodes so all angles are 90 or 180 degrees"),
                Shortcut.registerShortcut("tools:orthogonalize", tr("Tool: {0}", tr("Orthogonalize Shape")),
                        KeyEvent.VK_Q,
                        Shortcut.DIRECT), true);
        putValue("help", ht("/Action/OrthogonalizeShape"));
    }

    /**
     * excepted deviation from an angle of 0, 90, 180, 360 degrees
     * maximum value: 45 degrees
     *
     * Current policy is to except just everything, no matter how strange the result would be.
     */
    private static final double TOLERANCE1 = Utils.toRadians(45.);   // within a way
    private static final double TOLERANCE2 = Utils.toRadians(45.);   // ways relative to each other

    /**
     * Remember movements, so the user can later undo it for certain nodes
     */
    private static final Map<Node, EastNorth> rememberMovements = new HashMap<>();

    /**
     * Undo the previous orthogonalization for certain nodes.
     *
     * This is useful, if the way shares nodes that you don't like to change, e.g. imports or
     * work of another user.
     *
     * This action can be triggered by shortcut only.
     */
    public static class Undo extends JosmAction {
        /**
         * Constructor
         */
        public Undo() {
            super(tr("Orthogonalize Shape / Undo"), "ortho",
                    tr("Undo orthogonalization for certain nodes"),
                    Shortcut.registerShortcut("tools:orthogonalizeUndo", tr("Tool: {0}", tr("Orthogonalize Shape / Undo")),
                            KeyEvent.VK_Q,
                            Shortcut.SHIFT),
                    true, "action/orthogonalize/undo", true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            final Collection<Command> commands = new LinkedList<>();
            final Collection<OsmPrimitive> sel = getLayerManager().getEditDataSet().getSelected();
            try {
                for (OsmPrimitive p : sel) {
                    if (!(p instanceof Node)) throw new InvalidUserInputException("selected object is not a node");
                    Node n = (Node) p;
                    if (rememberMovements.containsKey(n)) {
                        EastNorth tmp = rememberMovements.get(n);
                        commands.add(new MoveCommand(n, -tmp.east(), -tmp.north()));
                        rememberMovements.remove(n);
                    }
                }
                if (!commands.isEmpty()) {
                    MainApplication.undoRedo.add(new SequenceCommand(tr("Orthogonalize / Undo"), commands));
                } else {
                    throw new InvalidUserInputException("Commands are empty");
                }
            } catch (InvalidUserInputException ex) {
                Logging.debug(ex);
                new Notification(
                        tr("Orthogonalize Shape / Undo<br>"+
                        "Please select nodes that were moved by the previous Orthogonalize Shape action!"))
                        .setIcon(JOptionPane.INFORMATION_MESSAGE)
                        .show();
            }
        }

        @Override
        protected void updateEnabledState() {
            updateEnabledStateOnCurrentSelection();
        }

        @Override
        protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
            setEnabled(selection != null && !selection.isEmpty());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        if ("EPSG:4326".equals(Main.getProjection().toString())) {
            String msg = tr("<html>You are using the EPSG:4326 projection which might lead<br>" +
                    "to undesirable results when doing rectangular alignments.<br>" +
                    "Change your projection to get rid of this warning.<br>" +
            "Do you want to continue?</html>");
            if (!ConditionalOptionPaneUtil.showConfirmationDialog(
                    "align_rectangular_4326",
                    Main.parent,
                    msg,
                    tr("Warning"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.YES_OPTION))
                return;
        }

        final Collection<OsmPrimitive> sel = getLayerManager().getEditDataSet().getSelected();

        try {
            final SequenceCommand command = orthogonalize(sel);
            MainApplication.undoRedo.add(new SequenceCommand(tr("Orthogonalize"), command));
        } catch (InvalidUserInputException ex) {
            Logging.debug(ex);
            String msg;
            if ("usage".equals(ex.getMessage())) {
                msg = "<h2>" + tr("Usage") + "</h2>" + USAGE;
            } else {
                msg = ex.getMessage() + "<br><hr><h2>" + tr("Usage") + "</h2>" + USAGE;
            }
            new Notification(msg)
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_DEFAULT)
                    .show();
        }
    }

    /**
     * Rectifies the selection
     * @param selection the selection which should be rectified
     * @return a rectifying command
     * @throws InvalidUserInputException if the selection is invalid
     */
    static SequenceCommand orthogonalize(Iterable<OsmPrimitive> selection) throws InvalidUserInputException {
        final List<Node> nodeList = new ArrayList<>();
        final List<WayData> wayDataList = new ArrayList<>();
        // collect nodes and ways from the selection
        for (OsmPrimitive p : selection) {
            if (p instanceof Node) {
                nodeList.add((Node) p);
            } else if (p instanceof Way) {
                if (!p.isIncomplete()) {
                    wayDataList.add(new WayData(((Way) p).getNodes()));
                }
            } else {
                throw new InvalidUserInputException(tr("Selection must consist only of ways and nodes."));
            }
        }
        if (wayDataList.isEmpty() && nodeList.size() > 2) {
            final WayData data = new WayData(nodeList);
            final Collection<Command> commands = orthogonalize(Collections.singletonList(data), Collections.<Node>emptyList());
            return new SequenceCommand(tr("Orthogonalize"), commands);
        } else if (wayDataList.isEmpty()) {
            throw new InvalidUserInputException("usage");
        } else {
            if (nodeList.size() == 2 || nodeList.isEmpty()) {
                OrthogonalizeAction.rememberMovements.clear();
                final Collection<Command> commands = new LinkedList<>();

                if (nodeList.size() == 2) {  // fixed direction
                    commands.addAll(orthogonalize(wayDataList, nodeList));
                } else if (nodeList.isEmpty()) {
                    List<List<WayData>> groups = buildGroups(wayDataList);
                    for (List<WayData> g: groups) {
                        commands.addAll(orthogonalize(g, nodeList));
                    }
                } else {
                    throw new IllegalStateException();
                }

                return new SequenceCommand(tr("Orthogonalize"), commands);

            } else {
                throw new InvalidUserInputException("usage");
            }
        }
    }

    /**
     * Collect groups of ways with common nodes in order to orthogonalize each group separately.
     * @param wayDataList list of ways
     * @return groups of ways with common nodes
     */
    private static List<List<WayData>> buildGroups(List<WayData> wayDataList) {
        List<List<WayData>> groups = new ArrayList<>();
        Set<WayData> remaining = new HashSet<>(wayDataList);
        while (!remaining.isEmpty()) {
            List<WayData> group = new ArrayList<>();
            groups.add(group);
            Iterator<WayData> it = remaining.iterator();
            WayData next = it.next();
            it.remove();
            extendGroupRec(group, next, new ArrayList<>(remaining));
            remaining.removeAll(group);
        }
        return groups;
    }

    private static void extendGroupRec(List<WayData> group, WayData newGroupMember, List<WayData> remaining) {
        group.add(newGroupMember);
        for (int i = 0; i < remaining.size(); ++i) {
            WayData candidate = remaining.get(i);
            if (candidate == null) continue;
            if (!Collections.disjoint(candidate.wayNodes, newGroupMember.wayNodes)) {
                remaining.set(i, null);
                extendGroupRec(group, candidate, remaining);
            }
        }
    }

    /**
     *
     *  Outline:
     *  1. Find direction of all segments
     *      - direction = 0..3 (right,up,left,down)
     *      - right is not really right, you may have to turn your screen
     *  2. Find average heading of all segments
     *      - heading = angle of a vector in polar coordinates
     *      - sum up horizontal segments (those with direction 0 or 2)
     *      - sum up vertical segments
     *      - turn the vertical sum by 90 degrees and add it to the horizontal sum
     *      - get the average heading from this total sum
     *  3. Rotate all nodes by the average heading so that right is really right
     *      and all segments are approximately NS or EW.
     *  4. If nodes are connected by a horizontal segment: Replace their y-Coordinate by
     *      the mean value of their y-Coordinates.
     *      - The same for vertical segments.
     *  5. Rotate back.
     * @param wayDataList list of ways
     * @param headingNodes list of heading nodes
     * @return list of commands to perform
     * @throws InvalidUserInputException if selected ways have an angle different from 90 or 180 degrees
     **/
    private static Collection<Command> orthogonalize(List<WayData> wayDataList, List<Node> headingNodes) throws InvalidUserInputException {
        // find average heading
        double headingAll;
        try {
            if (headingNodes.isEmpty()) {
                // find directions of the segments and make them consistent between different ways
                wayDataList.get(0).calcDirections(Direction.RIGHT);
                double refHeading = wayDataList.get(0).heading;
                EastNorth totSum = new EastNorth(0., 0.);
                for (WayData w : wayDataList) {
                    w.calcDirections(Direction.RIGHT);
                    int directionOffset = angleToDirectionChange(w.heading - refHeading, TOLERANCE2);
                    w.calcDirections(Direction.RIGHT.changeBy(directionOffset));
                    if (angleToDirectionChange(refHeading - w.heading, TOLERANCE2) != 0)
                        throw new JosmRuntimeException("orthogonalize error");
                    totSum = EN.sum(totSum, w.segSum);
                }
                headingAll = EN.polar(new EastNorth(0., 0.), totSum);
            } else {
                headingAll = EN.polar(headingNodes.get(0).getEastNorth(), headingNodes.get(1).getEastNorth());
                for (WayData w : wayDataList) {
                    w.calcDirections(Direction.RIGHT);
                    int directionOffset = angleToDirectionChange(w.heading - headingAll, TOLERANCE2);
                    w.calcDirections(Direction.RIGHT.changeBy(directionOffset));
                }
            }
        } catch (RejectedAngleException ex) {
            throw new InvalidUserInputException(
                    tr("<html>Please make sure all selected ways head in a similar direction<br>"+
                    "or orthogonalize them one by one.</html>"), ex);
        }

        // put the nodes of all ways in a set
        final Set<Node> allNodes = new HashSet<>();
        for (WayData w : wayDataList) {
            allNodes.addAll(w.wayNodes);
        }

        // the new x and y value for each node
        final Map<Node, Double> nX = new HashMap<>();
        final Map<Node, Double> nY = new HashMap<>();

        // calculate the centroid of all nodes
        // it is used as rotation center
        EastNorth pivot = new EastNorth(0., 0.);
        for (Node n : allNodes) {
            pivot = EN.sum(pivot, n.getEastNorth());
        }
        pivot = new EastNorth(pivot.east() / allNodes.size(), pivot.north() / allNodes.size());

        // rotate
        for (Node n: allNodes) {
            EastNorth tmp = EN.rotateCC(pivot, n.getEastNorth(), -headingAll);
            nX.put(n, tmp.east());
            nY.put(n, tmp.north());
        }

        // orthogonalize
        final Direction[] horizontal = {Direction.RIGHT, Direction.LEFT};
        final Direction[] vertical = {Direction.UP, Direction.DOWN};
        final Direction[][] orientations = {horizontal, vertical};
        for (Direction[] orientation : orientations) {
            final Set<Node> s = new HashSet<>(allNodes);
            int size = s.size();
            for (int dummy = 0; dummy < size; ++dummy) {
                if (s.isEmpty()) {
                    break;
                }
                final Node dummyN = s.iterator().next();     // pick arbitrary element of s

                final Set<Node> cs = new HashSet<>(); // will contain each node that can be reached from dummyN
                cs.add(dummyN);                      // walking only on horizontal / vertical segments

                boolean somethingHappened = true;
                while (somethingHappened) {
                    somethingHappened = false;
                    for (WayData w : wayDataList) {
                        for (int i = 0; i < w.nSeg; ++i) {
                            Node n1 = w.wayNodes.get(i);
                            Node n2 = w.wayNodes.get(i+1);
                            if (Arrays.asList(orientation).contains(w.segDirections[i])) {
                                if (cs.contains(n1) && !cs.contains(n2)) {
                                    cs.add(n2);
                                    somethingHappened = true;
                                }
                                if (cs.contains(n2) && !cs.contains(n1)) {
                                    cs.add(n1);
                                    somethingHappened = true;
                                }
                            }
                        }
                    }
                }

                final Map<Node, Double> nC = (orientation == horizontal) ? nY : nX;

                double average = 0;
                for (Node n : cs) {
                    s.remove(n);
                    average += nC.get(n).doubleValue();
                }
                average = average / cs.size();

                // if one of the nodes is a heading node, forget about the average and use its value
                for (Node fn : headingNodes) {
                    if (cs.contains(fn)) {
                        average = nC.get(fn);
                    }
                }

                // At this point, the two heading nodes (if any) are horizontally aligned, i.e. they
                // have the same y coordinate. So in general we shouldn't find them in a vertical string
                // of segments. This can still happen in some pathological cases (see #7889). To avoid
                // both heading nodes collapsing to one point, we simply skip this segment string and
                // don't touch the node coordinates.
                if (orientation == vertical && headingNodes.size() == 2 && cs.containsAll(headingNodes)) {
                    continue;
                }

                for (Node n : cs) {
                    nC.put(n, average);
                }
            }
            if (!s.isEmpty()) throw new JosmRuntimeException("orthogonalize error");
        }

        // rotate back and log the change
        final Collection<Command> commands = new LinkedList<>();
        for (Node n: allNodes) {
            EastNorth tmp = new EastNorth(nX.get(n), nY.get(n));
            tmp = EN.rotateCC(pivot, tmp, headingAll);
            final double dx = tmp.east() - n.getEastNorth().east();
            final double dy = tmp.north() - n.getEastNorth().north();
            if (headingNodes.contains(n)) { // The heading nodes should not have changed
                if (Math.abs(dx) > Math.abs(EPSILON * tmp.east()) ||
                    Math.abs(dy) > Math.abs(EPSILON * tmp.east()))
                    throw new AssertionError("heading node has changed");
            } else {
                OrthogonalizeAction.rememberMovements.put(n, new EastNorth(dx, dy));
                commands.add(new MoveCommand(n, dx, dy));
            }
        }
        return commands;
    }

    /**
     * Class contains everything we need to know about a single way.
     */
    private static class WayData {
        public final List<Node> wayNodes;       // The assigned way
        public final int nSeg;                  // Number of Segments of the Way
        public final int nNode;                 // Number of Nodes of the Way
        public final Direction[] segDirections; // Direction of the segments
        // segment i goes from node i to node (i+1)
        public EastNorth segSum;          // (Vector-)sum of all horizontal segments plus the sum of all vertical
        // segments turned by 90 degrees
        public double heading;            // heading of segSum == approximate heading of the way

        WayData(List<Node> wayNodes) {
            this.wayNodes = wayNodes;
            this.nNode = wayNodes.size();
            this.nSeg = nNode - 1;
            this.segDirections = new Direction[nSeg];
        }

        /**
         * Estimate the direction of the segments, given the first segment points in the
         * direction <code>pInitialDirection</code>.
         * Then sum up all horizontal / vertical segments to have a good guess for the
         * heading of the entire way.
         * @param pInitialDirection initial direction
         * @throws InvalidUserInputException if selected ways have an angle different from 90 or 180 degrees
         */
        public void calcDirections(Direction pInitialDirection) throws InvalidUserInputException {
            final EastNorth[] en = new EastNorth[nNode]; // alias: wayNodes.get(i).getEastNorth() ---> en[i]
            for (int i = 0; i < nNode; i++) {
                en[i] = wayNodes.get(i).getEastNorth();
            }
            Direction direction = pInitialDirection;
            segDirections[0] = direction;
            for (int i = 0; i < nSeg - 1; i++) {
                double h1 = EN.polar(en[i], en[i+1]);
                double h2 = EN.polar(en[i+1], en[i+2]);
                try {
                    direction = direction.changeBy(angleToDirectionChange(h2 - h1, TOLERANCE1));
                } catch (RejectedAngleException ex) {
                    throw new InvalidUserInputException(tr("Please select ways with angles of approximately 90 or 180 degrees."), ex);
                }
                segDirections[i+1] = direction;
            }

            // sum up segments
            EastNorth h = new EastNorth(0., 0.);
            EastNorth v = new EastNorth(0., 0.);
            for (int i = 0; i < nSeg; ++i) {
                EastNorth segment = EN.diff(en[i+1], en[i]);
                if (segDirections[i] == Direction.RIGHT) {
                    h = EN.sum(h, segment);
                } else if (segDirections[i] == Direction.UP) {
                    v = EN.sum(v, segment);
                } else if (segDirections[i] == Direction.LEFT) {
                    h = EN.diff(h, segment);
                } else if (segDirections[i] == Direction.DOWN) {
                    v = EN.diff(v, segment);
                } else throw new IllegalStateException();
            }
            // rotate the vertical vector by 90 degrees (clockwise) and add it to the horizontal vector
            segSum = EN.sum(h, new EastNorth(v.north(), -v.east()));
            this.heading = EN.polar(new EastNorth(0., 0.), segSum);
        }
    }

    enum Direction {
        RIGHT, UP, LEFT, DOWN;
        public Direction changeBy(int directionChange) {
            int tmp = (this.ordinal() + directionChange) % 4;
            if (tmp < 0) {
                tmp += 4;          // the % operator can return negative value
            }
            return Direction.values()[tmp];
        }
    }

    /**
     * Make sure angle (up to 2*Pi) is in interval [ 0, 2*Pi ).
     * @param a angle
     * @return correct angle
     */
    private static double standardAngle0to2PI(double a) {
        while (a >= 2 * Math.PI) {
            a -= 2 * Math.PI;
        }
        while (a < 0) {
            a += 2 * Math.PI;
        }
        return a;
    }

    /**
     * Make sure angle (up to 2*Pi) is in interval ( -Pi, Pi ].
     * @param a angle
     * @return correct angle
     */
    private static double standardAngleMPItoPI(double a) {
        while (a > Math.PI) {
            a -= 2 * Math.PI;
        }
        while (a <= -Math.PI) {
            a += 2 * Math.PI;
        }
        return a;
    }

    /**
     * Class contains some auxiliary functions
     */
    static final class EN {
        private EN() {
            // Hide implicit public constructor for utility class
        }

        /**
         * Rotate counter-clock-wise.
         * @param pivot pivot
         * @param en original east/north
         * @param angle angle, in radians
         * @return new east/north
         */
        public static EastNorth rotateCC(EastNorth pivot, EastNorth en, double angle) {
            double cosPhi = Math.cos(angle);
            double sinPhi = Math.sin(angle);
            double x = en.east() - pivot.east();
            double y = en.north() - pivot.north();
            double nx = cosPhi * x - sinPhi * y + pivot.east();
            double ny = sinPhi * x + cosPhi * y + pivot.north();
            return new EastNorth(nx, ny);
        }

        public static EastNorth sum(EastNorth en1, EastNorth en2) {
            return new EastNorth(en1.east() + en2.east(), en1.north() + en2.north());
        }

        public static EastNorth diff(EastNorth en1, EastNorth en2) {
            return new EastNorth(en1.east() - en2.east(), en1.north() - en2.north());
        }

        public static double polar(EastNorth en1, EastNorth en2) {
            return PolarCoor.computeAngle(en2, en1);
        }
    }

    /**
     * Recognize angle to be approximately 0, 90, 180 or 270 degrees.
     * returns an integral value, corresponding to a counter clockwise turn.
     * @param a angle, in radians
     * @param deltaMax maximum tolerance, in radians
     * @return an integral value, corresponding to a counter clockwise turn
     * @throws RejectedAngleException in case of invalid angle
     */
    private static int angleToDirectionChange(double a, double deltaMax) throws RejectedAngleException {
        a = standardAngleMPItoPI(a);
        double d0 = Math.abs(a);
        double d90 = Math.abs(a - Math.PI / 2);
        double dm90 = Math.abs(a + Math.PI / 2);
        int dirChange;
        if (d0 < deltaMax) {
            dirChange = 0;
        } else if (d90 < deltaMax) {
            dirChange = 1;
        } else if (dm90 < deltaMax) {
            dirChange = -1;
        } else {
            a = standardAngle0to2PI(a);
            double d180 = Math.abs(a - Math.PI);
            if (d180 < deltaMax) {
                dirChange = 2;
            } else
                throw new RejectedAngleException();
        }
        return dirChange;
    }

    /**
     * Exception: unsuited user input
     */
    protected static class InvalidUserInputException extends Exception {
        InvalidUserInputException(String message) {
            super(message);
        }

        InvalidUserInputException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception: angle cannot be recognized as 0, 90, 180 or 270 degrees
     */
    protected static class RejectedAngleException extends Exception {
        RejectedAngleException() {
            super();
        }
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
