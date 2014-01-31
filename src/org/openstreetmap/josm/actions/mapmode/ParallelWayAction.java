// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.NavigatableComponent.SystemOfMeasurement;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

//// TODO: (list below)
/* == Functionality ==
 *
 * 1. Use selected nodes as split points for the selected ways.
 *
 * The ways containing the selected nodes will be split and only the "inner"
 * parts will be copied
 *
 * 2. Enter exact offset
 *
 * 3. Improve snapping
 *
 * 4. Visual cues could be better
 *
 * 5. Cursors (Half-done)
 *
 * 6. (long term) Parallelize and adjust offsets of existing ways
 *
 * == Code quality ==
 *
 * a) The mode, flags, and modifiers might be updated more than necessary.
 *
 * Not a performance problem, but better if they where more centralized
 *
 * b) Extract generic MapMode services into a super class and/or utility class
 *
 * c) Maybe better to simply draw our own source way highlighting?
 *
 * Current code doesn't not take into account that ways might been highlighted
 * by other than us. Don't think that situation should ever happen though.
 */

/**
 * MapMode for making parallel ways.
 *
 * All calculations are done in projected coordinates.
 *
 * @author Ole Jørgen Brønner (olejorgenb)
 */
public class ParallelWayAction extends MapMode implements AWTEventListener, MapViewPaintable, PreferenceChangedListener {

    private enum Mode {
        dragging, normal
    }

    //// Preferences and flags
    // See updateModeLocalPreferences for defaults
    private Mode mode;
    private boolean copyTags;
    private boolean copyTagsDefault;

    private boolean snap;
    private boolean snapDefault;

    private double snapThreshold;
    private double snapDistanceMetric;
    private double snapDistanceImperial;
    private double snapDistanceChinese;
    private double snapDistanceNautical;

    private ModifiersSpec snapModifierCombo;
    private ModifiersSpec copyTagsModifierCombo;
    private ModifiersSpec addToSelectionModifierCombo;
    private ModifiersSpec toggleSelectedModifierCombo;
    private ModifiersSpec setSelectedModifierCombo;

    private int initialMoveDelay;

    private final MapView mv;

    // Mouse tracking state
    private Point mousePressedPos;
    private boolean mouseIsDown;
    private long mousePressedTime;
    private boolean mouseHasBeenDragged;

    private WaySegment referenceSegment;
    private ParallelWays pWays;
    private Set<Way> sourceWays;
    private EastNorth helperLineStart;
    private EastNorth helperLineEnd;

    Stroke helpLineStroke;
    Stroke refLineStroke;
    Color mainColor;

    public ParallelWayAction(MapFrame mapFrame) {
        super(tr("Parallel"), "parallel", tr("Make parallel copies of ways"),
            Shortcut.registerShortcut("mapmode:parallel", tr("Mode: {0}",
                tr("Parallel")), KeyEvent.VK_P, Shortcut.SHIFT),
            mapFrame, ImageProvider.getCursor("normal", "parallel"));
        putValue("help", ht("/Action/Parallel"));
        mv = mapFrame.mapView;
        updateModeLocalPreferences();
        Main.pref.addPreferenceChangeListener(this);
    }

    @Override
    public void enterMode() {
        // super.enterMode() updates the status line and cursor so we need our state to be set correctly
        setMode(Mode.normal);
        pWays = null;
        updateAllPreferences(); // All default values should've been set now

        super.enterMode();

        mv.addMouseListener(this);
        mv.addMouseMotionListener(this);
        mv.addTemporaryLayer(this);

        helpLineStroke = GuiHelper.getCustomizedStroke(getStringPref("stroke.hepler-line", "1" ));
        refLineStroke = GuiHelper.getCustomizedStroke(getStringPref("stroke.ref-line", "1 2 2"));
        mainColor = Main.pref.getColor(marktr("make parallel helper line"), null);
        if (mainColor == null) mainColor = PaintColors.SELECTED.get();

        //// Needed to update the mouse cursor if modifiers are changed when the mouse is motionless
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        } catch (SecurityException ex) {
            Main.warn(ex);
        }
        sourceWays = new LinkedHashSet<Way>(getCurrentDataSet().getSelectedWays());
        for (Way w : sourceWays) {
            w.setHighlighted(true);
        }
        mv.repaint();
    }

    @Override
    public void exitMode() {
        super.exitMode();
        mv.removeMouseListener(this);
        mv.removeMouseMotionListener(this);
        mv.removeTemporaryLayer(this);
        Main.map.statusLine.setDist(-1);
        Main.map.statusLine.repaint();
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        } catch (SecurityException ex) {
            Main.warn(ex);
        }
        removeWayHighlighting(sourceWays);
        pWays = null;
        sourceWays = null;
        referenceSegment = null;
        mv.repaint();
    }

    @Override
    public String getModeHelpText() {
        // TODO: add more detailed feedback based on modifier state.
        // TODO: dynamic messages based on preferences. (Could be problematic translation wise)
        switch (mode) {
        case normal:
            return tr("Select ways as in Select mode. Drag selected ways or a single way to create a parallel copy (Alt toggles tag preservation)");
        case dragging:
            return tr("Hold Ctrl to toggle snapping");
        }
        return ""; // impossible ..
    }

    // Separated due to "race condition" between default values
    private void updateAllPreferences() {
        updateModeLocalPreferences();
        // @formatter:off
        // @formatter:on
    }

    private void updateModeLocalPreferences() {
        // @formatter:off
        snapThreshold        = Main.pref.getDouble (prefKey("snap-threshold-percent"), 0.70);
        snapDefault          = Main.pref.getBoolean(prefKey("snap-default"),      true);
        copyTagsDefault      = Main.pref.getBoolean(prefKey("copy-tags-default"), true);
        initialMoveDelay     = Main.pref.getInteger(prefKey("initial-move-delay"), 200);
        snapDistanceMetric   = Main.pref.getDouble(prefKey("snap-distance-metric"), 0.5);
        snapDistanceImperial = Main.pref.getDouble(prefKey("snap-distance-imperial"), 1);
        snapDistanceChinese  = Main.pref.getDouble(prefKey("snap-distance-chinese"), 1);
        snapDistanceNautical = Main.pref.getDouble(prefKey("snap-distance-nautical"), 0.1);

        snapModifierCombo           = new ModifiersSpec(getStringPref("snap-modifier-combo",             "?sC"));
        copyTagsModifierCombo       = new ModifiersSpec(getStringPref("copy-tags-modifier-combo",        "As?"));
        addToSelectionModifierCombo = new ModifiersSpec(getStringPref("add-to-selection-modifier-combo", "aSc"));
        toggleSelectedModifierCombo = new ModifiersSpec(getStringPref("toggle-selection-modifier-combo", "asC"));
        setSelectedModifierCombo    = new ModifiersSpec(getStringPref("set-selection-modifier-combo",    "asc"));
        // @formatter:on
    }

    @Override
    public boolean layerIsSupported(Layer layer) {
        return layer instanceof OsmDataLayer;
    }

    @Override
    public void eventDispatched(AWTEvent e) {
        if (Main.map == null || mv == null || !mv.isActiveLayerDrawable())
            return;

        // Should only get InputEvents due to the mask in enterMode
        if (updateModifiersState((InputEvent) e)) {
            updateStatusLine();
            updateCursor();
        }
    }

    private boolean updateModifiersState(InputEvent e) {
        boolean oldAlt = alt, oldShift = shift, oldCtrl = ctrl;
        updateKeyModifiers(e);
        return (oldAlt != alt || oldShift != shift || oldCtrl != ctrl);
    }

    private void updateCursor() {
        Cursor newCursor = null;
        switch (mode) {
        case normal:
            if (matchesCurrentModifiers(setSelectedModifierCombo)) {
                newCursor = ImageProvider.getCursor("normal", "parallel");
            } else if (matchesCurrentModifiers(addToSelectionModifierCombo)) {
                newCursor = ImageProvider.getCursor("normal", "parallel"); // FIXME
            } else if (matchesCurrentModifiers(toggleSelectedModifierCombo)) {
                newCursor = ImageProvider.getCursor("normal", "parallel"); // FIXME
            } else {
                // TODO: set to a cursor indicating an error
            }
            break;
        case dragging:
            if (snap) {
                // TODO: snapping cursor?
                newCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            } else {
                newCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            }
        }
        if (newCursor != null) {
            mv.setNewCursor(newCursor, this);
        }
    }

    private void setMode(Mode mode) {
        this.mode = mode;
        updateCursor();
        updateStatusLine();
    }

    private boolean sanityCheck() {
        // @formatter:off
        boolean areWeSane =
            mv.isActiveLayerVisible() &&
            mv.isActiveLayerDrawable() &&
            ((Boolean) this.getValue("active"));
        // @formatter:on
        assert (areWeSane); // mad == bad
        return areWeSane;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInMapView();
        updateModifiersState(e);
        // Other buttons are off limit, but we still get events.
        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        if (!sanityCheck())
            return;

        updateFlagsOnlyChangeableOnPress();
        updateFlagsChangeableAlways();

        // Since the created way is left selected, we need to unselect again here
        if (pWays != null && pWays.ways != null) {
            getCurrentDataSet().clearSelection(pWays.ways);
            pWays = null;
        }

        mouseIsDown = true;
        mousePressedPos = e.getPoint();
        mousePressedTime = System.currentTimeMillis();

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        updateModifiersState(e);
        // Other buttons are off limit, but we still get events.
        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        if (!mouseHasBeenDragged) {
            // use point from press or click event? (or are these always the same)
            Way nearestWay = mv.getNearestWay(e.getPoint(), OsmPrimitive.isSelectablePredicate);
            if (nearestWay == null) {
                if (matchesCurrentModifiers(setSelectedModifierCombo)) {
                    clearSourceWays();
                }
                resetMouseTrackingState();
                return;
            }
            boolean isSelected = nearestWay.isSelected();
            if (matchesCurrentModifiers(addToSelectionModifierCombo)) {
                if (!isSelected) {
                    addSourceWay(nearestWay);
                }
            } else if (matchesCurrentModifiers(toggleSelectedModifierCombo)) {
                if (isSelected) {
                    removeSourceWay(nearestWay);
                } else {
                    addSourceWay(nearestWay);
                }
            } else if (matchesCurrentModifiers(setSelectedModifierCombo)) {
                clearSourceWays();
                addSourceWay(nearestWay);
            } // else -> invalid modifier combination
        } else if (mode == Mode.dragging) {
            clearSourceWays();
        }

        setMode(Mode.normal);
        resetMouseTrackingState();
        mv.repaint();
    }

    private void removeWayHighlighting(Collection<Way> ways) {
        if (ways == null)
            return;
        for (Way w : ways) {
            w.setHighlighted(false);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // WTF.. the event passed here doesn't have button info?
        // Since we get this event from other buttons too, we must check that
        // _BUTTON1_ is down.
        if (!mouseIsDown)
            return;

        boolean modifiersChanged = updateModifiersState(e);
        updateFlagsChangeableAlways();

        if (modifiersChanged) {
            // Since this could be remotely slow, do it conditionally
            updateStatusLine();
            updateCursor();
        }

        if ((System.currentTimeMillis() - mousePressedTime) < initialMoveDelay)
            return;
        // Assuming this event only is emitted when the mouse has moved
        // Setting this after the check above means we tolerate clicks with some movement
        mouseHasBeenDragged = true;

        Point p = e.getPoint();
        if (mode == Mode.normal) {
            // Should we ensure that the copyTags modifiers are still valid?

            // Important to use mouse position from the press, since the drag
            // event can come quite late
            if (!isModifiersValidForDragMode())
                return;
            if (!initParallelWays(mousePressedPos, copyTags))
                return;
            setMode(Mode.dragging);
        }

        // Calculate distance to the reference line
        EastNorth enp = mv.getEastNorth((int) p.getX(), (int) p.getY());
        EastNorth nearestPointOnRefLine = Geometry.closestPointToLine(referenceSegment.getFirstNode().getEastNorth(),
                referenceSegment.getSecondNode().getEastNorth(), enp);

        // Note: d is the distance in _projected units_
        double d = enp.distance(nearestPointOnRefLine);
        double realD = mv.getProjection().eastNorth2latlon(enp).greatCircleDistance(mv.getProjection().eastNorth2latlon(nearestPointOnRefLine));
        double snappedRealD = realD;

        // TODO: abuse of isToTheRightSideOfLine function.
        boolean toTheRight = Geometry.isToTheRightSideOfLine(referenceSegment.getFirstNode(),
                referenceSegment.getFirstNode(), referenceSegment.getSecondNode(), new Node(enp));

        if (snap) {
            // TODO: Very simple snapping
            // - Snap steps relative to the distance?
            double snapDistance;
            SystemOfMeasurement som = NavigatableComponent.getSystemOfMeasurement();
            if (som.equals(NavigatableComponent.CHINESE_SOM)) {
                snapDistance = snapDistanceChinese * NavigatableComponent.CHINESE_SOM.aValue;
            } else if (som.equals(NavigatableComponent.IMPERIAL_SOM)) {
                snapDistance = snapDistanceImperial * NavigatableComponent.IMPERIAL_SOM.aValue;
            } else if (som.equals(NavigatableComponent.NAUTICAL_MILE_SOM)) {
                snapDistance = snapDistanceNautical * NavigatableComponent.NAUTICAL_MILE_SOM.aValue;
            } else {
                snapDistance = snapDistanceMetric; // Metric system by default
            }
            double closestWholeUnit;
            double modulo = realD % snapDistance;
            if (modulo < snapDistance/2.0) {
                closestWholeUnit = realD - modulo;
            } else {
                closestWholeUnit = realD + (snapDistance-modulo);
            }
            if (Math.abs(closestWholeUnit - realD) < (snapThreshold * snapDistance)) {
                snappedRealD = closestWholeUnit;
            } else {
                snappedRealD = closestWholeUnit + Math.signum(realD - closestWholeUnit) * snapDistance;
            }
        }
        d = snappedRealD * (d/realD); // convert back to projected distance. (probably ok on small scales)
        helperLineStart = nearestPointOnRefLine;
        helperLineEnd = enp;
        if (toTheRight) {
            d = -d;
        }
        pWays.changeOffset(d);

        Main.map.statusLine.setDist(Math.abs(snappedRealD));
        Main.map.statusLine.repaint();
        mv.repaint();
    }

    private boolean matchesCurrentModifiers(ModifiersSpec spec) {
        return spec.matchWithKnown(alt, shift, ctrl);
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bbox) {
        if (mode == Mode.dragging) {
            // sanity checks
            if (mv == null)
                return;

            // FIXME: should clip the line (gets insanely slow when zoomed in on a very long line
            g.setStroke(refLineStroke);
            g.setColor(mainColor);
            Point p1 = mv.getPoint(referenceSegment.getFirstNode().getEastNorth());
            Point p2 = mv.getPoint(referenceSegment.getSecondNode().getEastNorth());
            g.drawLine(p1.x, p1.y, p2.x, p2.y);

            g.setStroke(helpLineStroke);
            g.setColor(mainColor);
            p1 = mv.getPoint(helperLineStart);
            p2 = mv.getPoint(helperLineEnd);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    private boolean isModifiersValidForDragMode() {
        return (!alt && !shift && !ctrl) || matchesCurrentModifiers(snapModifierCombo)
                || matchesCurrentModifiers(copyTagsModifierCombo);
    }

    private void updateFlagsOnlyChangeableOnPress() {
        copyTags = copyTagsDefault != matchesCurrentModifiers(copyTagsModifierCombo);
    }

    private void updateFlagsChangeableAlways() {
        snap = snapDefault != matchesCurrentModifiers(snapModifierCombo);
    }

    //// We keep the source ways and the selection in sync so the user can see the source way's tags
    private void addSourceWay(Way w) {
        assert (sourceWays != null);
        getCurrentDataSet().addSelected(w);
        w.setHighlighted(true);
        sourceWays.add(w);
    }

    private void removeSourceWay(Way w) {
        assert (sourceWays != null);
        getCurrentDataSet().clearSelection(w);
        w.setHighlighted(false);
        sourceWays.remove(w);
    }

    private void clearSourceWays() {
        assert (sourceWays != null);
        getCurrentDataSet().clearSelection(sourceWays);
        for (Way w : sourceWays) {
            w.setHighlighted(false);
        }
        sourceWays.clear();
    }

    private void resetMouseTrackingState() {
        mouseIsDown = false;
        mousePressedPos = null;
        mouseHasBeenDragged = false;
    }

    // TODO: rename
    private boolean initParallelWays(Point p, boolean copyTags) {
        referenceSegment = mv.getNearestWaySegment(p, Way.isUsablePredicate, true);
        if (referenceSegment == null)
            return false;

        if (!sourceWays.contains(referenceSegment.way)) {
            clearSourceWays();
            addSourceWay(referenceSegment.way);
        }

        try {
            int referenceWayIndex = -1;
            int i = 0;
            for (Way w : sourceWays) {
                if (w == referenceSegment.way) {
                    referenceWayIndex = i;
                    break;
                }
                i++;
            }
            pWays = new ParallelWays(sourceWays, copyTags, referenceWayIndex);
            pWays.commit();
            getCurrentDataSet().setSelected(pWays.ways);
            return true;
        } catch (IllegalArgumentException e) {
            // TODO: Not ideal feedback. Maybe changing the cursor could be a good mechanism?
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("ParallelWayAction\n" +
                            "The ways selected must form a simple branchless path"),
                    tr("Make parallel way error"),
                    JOptionPane.INFORMATION_MESSAGE);
            // The error dialog prevents us from getting the mouseReleased event
            resetMouseTrackingState();
            pWays = null;
            return false;
        }
    }

    private String prefKey(String subKey) {
        return "edit.make-parallel-way-action." + subKey;
    }

    private String getStringPref(String subKey, String def) {
        return Main.pref.get(prefKey(subKey), def);
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey().startsWith(prefKey(""))) {
            updateAllPreferences();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        Main.pref.removePreferenceChangeListener(this);
    }
}
