// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.DoubleStream;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.draw.SymbolShape;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Class that enables the user to draw way segments in angles of exactly 30, 45,
 * 60, 90 degrees.
 *
 * With enabled snapping, the new way node will be projected onto the helper line
 * that indicates a certain fixed angle relative to the previous segment.
 */
class DrawSnapHelper {

    private final DrawAction drawAction;

    /**
     * Constructs a new {@code SnapHelper}.
     * @param drawAction enclosing DrawAction
     */
    DrawSnapHelper(DrawAction drawAction) {
        this.drawAction = drawAction;
        this.anglePopupListener = new PopupMenuLauncher(new AnglePopupMenu(this)) {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    toggleSnapping();
                    drawAction.updateStatusLine();
                }
            }
        };
    }

    private static final String DRAW_ANGLESNAP_ANGLES = "draw.anglesnap.angles";

    private static final class RepeatedAction extends AbstractAction {
        RepeatedAction(DrawSnapHelper snapHelper) {
            super(tr("Toggle snapping by {0}", snapHelper.drawAction.getShortcut().getKeyText()));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean sel = ((JCheckBoxMenuItem) e.getSource()).getState();
            DrawAction.USE_REPEATED_SHORTCUT.put(sel);
        }
    }

    private static final class HelperAction extends AbstractAction {
        private final transient DrawSnapHelper snapHelper;

        HelperAction(DrawSnapHelper snapHelper) {
            super(tr("Show helper geometry"));
            this.snapHelper = snapHelper;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean sel = ((JCheckBoxMenuItem) e.getSource()).getState();
            DrawAction.DRAW_CONSTRUCTION_GEOMETRY.put(sel);
            DrawAction.SHOW_PROJECTED_POINT.put(sel);
            DrawAction.SHOW_ANGLE.put(sel);
            snapHelper.enableSnapping();
        }
    }

    private static final class ProjectionAction extends AbstractAction {
        private final transient DrawSnapHelper snapHelper;

        ProjectionAction(DrawSnapHelper snapHelper) {
            super(tr("Snap to node projections"));
            this.snapHelper = snapHelper;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean sel = ((JCheckBoxMenuItem) e.getSource()).getState();
            DrawAction.SNAP_TO_PROJECTIONS.put(sel);
            snapHelper.enableSnapping();
        }
    }

    private static final class DisableAction extends AbstractAction {
        private final transient DrawSnapHelper snapHelper;

        DisableAction(DrawSnapHelper snapHelper) {
            super(tr("Disable"));
            this.snapHelper = snapHelper;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            snapHelper.saveAngles("180");
            snapHelper.init();
            snapHelper.enableSnapping();
        }
    }

    private static final class Snap90DegreesAction extends AbstractAction {
        private final transient DrawSnapHelper snapHelper;

        Snap90DegreesAction(DrawSnapHelper snapHelper) {
            super(tr("0,90,..."));
            this.snapHelper = snapHelper;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            snapHelper.saveAngles("0", "90", "180");
            snapHelper.init();
            snapHelper.enableSnapping();
        }
    }

    private static final class Snap45DegreesAction extends AbstractAction {
        private final transient DrawSnapHelper snapHelper;

        Snap45DegreesAction(DrawSnapHelper snapHelper) {
            super(tr("0,45,90,..."));
            this.snapHelper = snapHelper;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            snapHelper.saveAngles("0", "45", "90", "135", "180");
            snapHelper.init();
            snapHelper.enableSnapping();
        }
    }

    private static final class Snap30DegreesAction extends AbstractAction {
        private final transient DrawSnapHelper snapHelper;

        Snap30DegreesAction(DrawSnapHelper snapHelper) {
            super(tr("0,30,45,60,90,..."));
            this.snapHelper = snapHelper;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            snapHelper.saveAngles("0", "30", "45", "60", "90", "120", "135", "150", "180");
            snapHelper.init();
            snapHelper.enableSnapping();
        }
    }

    private static final class AnglePopupMenu extends JPopupMenu {

        private AnglePopupMenu(final DrawSnapHelper snapHelper) {
            JCheckBoxMenuItem repeatedCb = new JCheckBoxMenuItem(new RepeatedAction(snapHelper));
            JCheckBoxMenuItem helperCb = new JCheckBoxMenuItem(new HelperAction(snapHelper));
            JCheckBoxMenuItem projectionCb = new JCheckBoxMenuItem(new ProjectionAction(snapHelper));

            helperCb.setState(DrawAction.DRAW_CONSTRUCTION_GEOMETRY.get());
            projectionCb.setState(DrawAction.SNAP_TO_PROJECTIONS.get());
            repeatedCb.setState(DrawAction.USE_REPEATED_SHORTCUT.get());
            add(repeatedCb);
            add(helperCb);
            add(projectionCb);
            add(new DisableAction(snapHelper));
            add(new Snap90DegreesAction(snapHelper));
            add(new Snap45DegreesAction(snapHelper));
            add(new Snap30DegreesAction(snapHelper));
        }
    }

    private boolean snapOn; // snapping is turned on

    private boolean active; // snapping is active for current mouse position
    private boolean fixed; // snap angle is fixed
    private boolean absoluteFix; // snap angle is absolute

    EastNorth dir2;
    private EastNorth projected;
    private String labelText;
    private double lastAngle;

    private double customBaseHeading = -1; // angle of base line, if not last segment)
    private EastNorth segmentPoint1; // remembered first point of base segment
    private EastNorth segmentPoint2; // remembered second point of base segment
    private EastNorth projectionSource; // point that we are projecting to the line

    private double[] snapAngles;

    private double pe, pn; // (pe, pn) - direction of snapping line
    private double e0, n0; // (e0, n0) - origin of snapping line

    private final String fixFmt = "%d "+tr("FIX");

    private JCheckBoxMenuItem checkBox;

    final MouseListener anglePopupListener;

    /**
     * Set the initial state
     */
    public void init() {
        snapOn = false;
        checkBox.setState(snapOn);
        fixed = false;
        absoluteFix = false;

        computeSnapAngles();
        Preferences.main().addWeakKeyPreferenceChangeListener(DRAW_ANGLESNAP_ANGLES, e -> this.computeSnapAngles());
    }

    private void computeSnapAngles() {
        snapAngles = Config.getPref().getList(DRAW_ANGLESNAP_ANGLES,
                Arrays.asList("0", "30", "45", "60", "90", "120", "135", "150", "180"))
                .stream()
                .mapToDouble(DrawSnapHelper::parseSnapAngle)
                .flatMap(s -> DoubleStream.of(s, 360-s))
                .toArray();
    }

    private static double parseSnapAngle(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException e) {
            Logging.warn("Incorrect number in draw.anglesnap.angles preferences: {0}", string);
            return 0;
        }
    }

    /**
     * Save the snap angles
     * @param angles The angles
     */
    public void saveAngles(String... angles) {
        Config.getPref().putList(DRAW_ANGLESNAP_ANGLES, Arrays.asList(angles));
    }

    /**
     * Sets the menu checkbox.
     * @param checkBox menu checkbox
     */
    public void setMenuCheckBox(JCheckBoxMenuItem checkBox) {
        this.checkBox = checkBox;
    }

    /**
     * Draw the snap hint line.
     * @param g2 graphics
     * @param mv MapView state
     * @since 10874
     */
    public void drawIfNeeded(Graphics2D g2, MapViewState mv) {
        if (!snapOn || !active)
            return;
        MapViewPoint p1 = mv.getPointFor(drawAction.getCurrentBaseNode());
        MapViewPoint p2 = mv.getPointFor(dir2);
        MapViewPoint p3 = mv.getPointFor(projected);
        if (DrawAction.DRAW_CONSTRUCTION_GEOMETRY.get()) {
            g2.setColor(DrawAction.SNAP_HELPER_COLOR.get());
            g2.setStroke(DrawAction.HELPER_STROKE.get());

            MapViewPath b = new MapViewPath(mv);
            b.moveTo(p2);
            if (absoluteFix) {
                b.lineTo(p2.interpolate(p1, 2)); // bi-directional line
            } else {
                b.lineTo(p3);
            }
            g2.draw(b);
        }
        if (projectionSource != null) {
            g2.setColor(DrawAction.SNAP_HELPER_COLOR.get());
            g2.setStroke(DrawAction.HELPER_STROKE.get());
            MapViewPath b = new MapViewPath(mv);
            b.moveTo(p3);
            b.lineTo(projectionSource);
            g2.draw(b);
        }

        if (customBaseHeading >= 0) {
            g2.setColor(DrawAction.HIGHLIGHT_COLOR.get());
            g2.setStroke(DrawAction.HIGHLIGHT_STROKE.get());
            MapViewPath b = new MapViewPath(mv);
            b.moveTo(segmentPoint1);
            b.lineTo(segmentPoint2);
            g2.draw(b);
        }

        g2.setColor(DrawAction.RUBBER_LINE_COLOR.get());
        g2.setStroke(DrawAction.RUBBER_LINE_STROKE.get());
        MapViewPath b = new MapViewPath(mv);
        b.moveTo(p1);
        b.lineTo(p3);
        g2.draw(b);

        g2.drawString(labelText, (int) p3.getInViewX()-5, (int) p3.getInViewY()+20);
        if (DrawAction.SHOW_PROJECTED_POINT.get()) {
            g2.setStroke(DrawAction.RUBBER_LINE_STROKE.get());
            g2.draw(new MapViewPath(mv).shapeAround(p3, SymbolShape.CIRCLE, 10)); // projected point
        }

        g2.setColor(DrawAction.SNAP_HELPER_COLOR.get());
        g2.setStroke(DrawAction.HELPER_STROKE.get());
    }

    /**
     * If mouse position is close to line at 15-30-45-... angle, remembers this direction
     * @param currentEN Current position
     * @param baseHeading The heading
     * @param curHeading The current mouse heading
     */
    public void checkAngleSnapping(EastNorth currentEN, double baseHeading, double curHeading) {
        MapView mapView = MainApplication.getMap().mapView;
        EastNorth p0 = drawAction.getCurrentBaseNode().getEastNorth();
        EastNorth snapPoint = currentEN;
        double angle = -1;

        double activeBaseHeading = (customBaseHeading >= 0) ? customBaseHeading : baseHeading;

        if (snapOn && (activeBaseHeading >= 0)) {
            angle = curHeading - activeBaseHeading;
            if (angle < 0) {
                angle += 360;
            }
            if (angle > 360) {
                angle = 0;
            }

            double nearestAngle;
            if (fixed) {
                nearestAngle = lastAngle; // if direction is fixed use previous angle
                active = true;
            } else {
                nearestAngle = getNearestAngle(angle);
                if (getAngleDelta(nearestAngle, angle) < DrawAction.SNAP_ANGLE_TOLERANCE.get()) {
                    active = customBaseHeading >= 0 || Math.abs(nearestAngle - 180) > 1e-3;
                    // if angle is to previous segment, exclude 180 degrees
                    lastAngle = nearestAngle;
                } else {
                    active = false;
                }
            }

            if (active) {
                double phi;
                e0 = p0.east();
                n0 = p0.north();
                buildLabelText((nearestAngle <= 180) ? nearestAngle : (nearestAngle-360));

                phi = (nearestAngle + activeBaseHeading) * Math.PI / 180;
                // (pe,pn) - direction of snapping line
                pe = Math.sin(phi);
                pn = Math.cos(phi);
                double scale = 20 * mapView.getDist100Pixel();
                dir2 = new EastNorth(e0 + scale * pe, n0 + scale * pn);
                snapPoint = getSnapPoint(currentEN);
            } else {
                noSnapNow();
            }
        }

        // find out the distance, in metres, between the base point and projected point
        LatLon mouseLatLon = mapView.getProjection().eastNorth2latlon(snapPoint);
        double distance = this.drawAction.getCurrentBaseNode().getCoor().greatCircleDistance(mouseLatLon);
        double hdg = Utils.toDegrees(p0.heading(snapPoint));
        // heading of segment from current to calculated point, not to mouse position

        if (baseHeading >= 0) { // there is previous line segment with some heading
            angle = hdg - baseHeading;
            if (angle < 0) {
                angle += 360;
            }
            if (angle > 360) {
                angle = 0;
            }
        }
        DrawAction.showStatusInfo(angle, hdg, distance, isSnapOn());
    }

    private void buildLabelText(double nearestAngle) {
        if (DrawAction.SHOW_ANGLE.get()) {
            if (fixed) {
                if (absoluteFix) {
                    labelText = "=";
                } else {
                    labelText = String.format(fixFmt, (int) nearestAngle);
                }
            } else {
                labelText = String.format("%d", (int) nearestAngle);
            }
        } else {
            if (fixed) {
                if (absoluteFix) {
                    labelText = "=";
                } else {
                    labelText = String.format(tr("FIX"), 0);
                }
            } else {
                labelText = "";
            }
        }
    }

    /**
     * Gets a snap point close to p. Stores the result for display.
     * @param p The point
     * @return The snap point close to p.
     */
    public EastNorth getSnapPoint(EastNorth p) {
        if (!active)
            return p;
        double de = p.east()-e0;
        double dn = p.north()-n0;
        double l = de*pe+dn*pn;
        double delta = MainApplication.getMap().mapView.getDist100Pixel()/20;
        if (!absoluteFix && l < delta) {
            active = false;
            return p;
        } //  do not go backward!

        projectionSource = null;
        if (DrawAction.SNAP_TO_PROJECTIONS.get()) {
            DataSet ds = drawAction.getLayerManager().getActiveDataSet();
            Collection<Way> selectedWays = ds.getSelectedWays();
            if (selectedWays.size() == 1) {
                Way w = selectedWays.iterator().next();
                Collection<EastNorth> pointsToProject = new ArrayList<>();
                if (w.getNodesCount() < 1000) {
                    for (Node n: w.getNodes()) {
                        pointsToProject.add(n.getEastNorth());
                    }
                }
                if (customBaseHeading >= 0) {
                    pointsToProject.add(segmentPoint1);
                    pointsToProject.add(segmentPoint2);
                }
                EastNorth enOpt = null;
                double dOpt = 1e5;
                for (EastNorth en: pointsToProject) { // searching for besht projection
                    double l1 = (en.east()-e0)*pe+(en.north()-n0)*pn;
                    double d1 = Math.abs(l1-l);
                    if (d1 < delta && d1 < dOpt) {
                        l = l1;
                        enOpt = en;
                        dOpt = d1;
                    }
                }
                if (enOpt != null) {
                    projectionSource = enOpt;
                }
            }
        }
        projected = new EastNorth(e0+l*pe, n0+l*pn);
        return projected;
    }

    /**
     * Disables snapping
     */
    void noSnapNow() {
        active = false;
        dir2 = null;
        projected = null;
        labelText = null;
    }

    void setBaseSegment(WaySegment seg) {
        if (seg == null) return;
        segmentPoint1 = seg.getFirstNode().getEastNorth();
        segmentPoint2 = seg.getSecondNode().getEastNorth();

        double hdg = segmentPoint1.heading(segmentPoint2);
        hdg = Utils.toDegrees(hdg);
        if (hdg < 0) {
            hdg += 360;
        }
        if (hdg > 360) {
            hdg -= 360;
        }
        customBaseHeading = hdg;
    }

    /**
     * Enable snapping.
     */
    void enableSnapping() {
        snapOn = true;
        checkBox.setState(snapOn);
        customBaseHeading = -1;
        unsetFixedMode();
    }

    void toggleSnapping() {
        snapOn = !snapOn;
        checkBox.setState(snapOn);
        customBaseHeading = -1;
        unsetFixedMode();
    }

    void setFixedMode() {
        if (active) {
            fixed = true;
        }
    }

    void unsetFixedMode() {
        fixed = false;
        absoluteFix = false;
        lastAngle = 0;
        active = false;
    }

    boolean isActive() {
        return active;
    }

    boolean isSnapOn() {
        return snapOn;
    }

    private double getNearestAngle(double angle) {
        double bestAngle = DoubleStream.of(snapAngles).boxed()
                .min(Comparator.comparing(snapAngle -> getAngleDelta(angle, snapAngle))).orElse(0.0);
        if (Math.abs(bestAngle-360) < 1e-3) {
            bestAngle = 0;
        }
        return bestAngle;
    }

    private static double getAngleDelta(double a, double b) {
        double delta = Math.abs(a-b);
        if (delta > 180)
            return 360-delta;
        else
            return delta;
    }

    void unFixOrTurnOff() {
        if (absoluteFix) {
            unsetFixedMode();
        } else {
            toggleSnapping();
        }
    }
}
