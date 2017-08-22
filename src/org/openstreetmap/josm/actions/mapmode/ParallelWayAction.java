// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.preferences.AbstractToStringProperty;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.CachingProperty;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.data.preferences.DoubleProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.StrokeProperty;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.ModifierExListener;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * MapMode for making parallel ways.
 *
 * All calculations are done in projected coordinates.
 *
 * TODO:
 * == Functionality ==
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
 * 5. (long term) Parallelize and adjust offsets of existing ways
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
 *
 * @author Ole Jørgen Brønner (olejorgenb)
 */
public class ParallelWayAction extends MapMode implements ModifierExListener {

    private static final CachingProperty<BasicStroke> HELPER_LINE_STROKE = new StrokeProperty(prefKey("stroke.hepler-line"), "1").cached();
    private static final CachingProperty<BasicStroke> REF_LINE_STROKE = new StrokeProperty(prefKey("stroke.ref-line"), "2 2 3").cached();

    // @formatter:off
    // CHECKSTYLE.OFF: SingleSpaceSeparator
    private static final CachingProperty<Double> SNAP_THRESHOLD         = new DoubleProperty(prefKey("snap-threshold-percent"), 0.70).cached();
    private static final CachingProperty<Boolean> SNAP_DEFAULT          = new BooleanProperty(prefKey("snap-default"),      true).cached();
    private static final CachingProperty<Boolean> COPY_TAGS_DEFAULT     = new BooleanProperty(prefKey("copy-tags-default"), true).cached();
    private static final CachingProperty<Integer> INITIAL_MOVE_DELAY    = new IntegerProperty(prefKey("initial-move-delay"), 200).cached();
    private static final CachingProperty<Double> SNAP_DISTANCE_METRIC   = new DoubleProperty(prefKey("snap-distance-metric"), 0.5).cached();
    private static final CachingProperty<Double> SNAP_DISTANCE_IMPERIAL = new DoubleProperty(prefKey("snap-distance-imperial"), 1).cached();
    private static final CachingProperty<Double> SNAP_DISTANCE_CHINESE  = new DoubleProperty(prefKey("snap-distance-chinese"), 1).cached();
    private static final CachingProperty<Double> SNAP_DISTANCE_NAUTICAL = new DoubleProperty(prefKey("snap-distance-nautical"), 0.1).cached();
    private static final CachingProperty<Color> MAIN_COLOR = new ColorProperty(marktr("make parallel helper line"), Color.RED).cached();

    private static final CachingProperty<Map<Modifier, Boolean>> SNAP_MODIFIER_COMBO
            = new KeyboardModifiersProperty(prefKey("snap-modifier-combo"),             "?sC").cached();
    private static final CachingProperty<Map<Modifier, Boolean>> COPY_TAGS_MODIFIER_COMBO
            = new KeyboardModifiersProperty(prefKey("copy-tags-modifier-combo"),        "As?").cached();
    private static final CachingProperty<Map<Modifier, Boolean>> ADD_TO_SELECTION_MODIFIER_COMBO
            = new KeyboardModifiersProperty(prefKey("add-to-selection-modifier-combo"), "aSc").cached();
    private static final CachingProperty<Map<Modifier, Boolean>> TOGGLE_SELECTED_MODIFIER_COMBO
            = new KeyboardModifiersProperty(prefKey("toggle-selection-modifier-combo"), "asC").cached();
    private static final CachingProperty<Map<Modifier, Boolean>> SET_SELECTED_MODIFIER_COMBO
            = new KeyboardModifiersProperty(prefKey("set-selection-modifier-combo"),    "asc").cached();
    // CHECKSTYLE.ON: SingleSpaceSeparator
    // @formatter:on

    enum Mode {
        DRAGGING, NORMAL
    }

    //// Preferences and flags
    // See updateModeLocalPreferences for defaults
    private Mode mode;
    private boolean copyTags;

    private boolean snap;

    private final MapView mv;

    // Mouse tracking state
    private Point mousePressedPos;
    private boolean mouseIsDown;
    private long mousePressedTime;
    private boolean mouseHasBeenDragged;

    private transient WaySegment referenceSegment;
    private transient ParallelWays pWays;
    private transient Set<Way> sourceWays;
    private EastNorth helperLineStart;
    private EastNorth helperLineEnd;

    private final ParallelWayLayer temporaryLayer = new ParallelWayLayer();

    /**
     * Constructs a new {@code ParallelWayAction}.
     * @param mapFrame Map frame
     */
    public ParallelWayAction(MapFrame mapFrame) {
        super(tr("Parallel"), "parallel", tr("Make parallel copies of ways"),
            Shortcut.registerShortcut("mapmode:parallel", tr("Mode: {0}",
                tr("Parallel")), KeyEvent.VK_P, Shortcut.SHIFT),
            ImageProvider.getCursor("normal", "parallel"));
        putValue("help", ht("/Action/Parallel"));
        mv = mapFrame.mapView;
    }

    @Override
    public void enterMode() {
        // super.enterMode() updates the status line and cursor so we need our state to be set correctly
        setMode(Mode.NORMAL);
        pWays = null;

        super.enterMode();

        mv.addMouseListener(this);
        mv.addMouseMotionListener(this);
        mv.addTemporaryLayer(temporaryLayer);

        //// Needed to update the mouse cursor if modifiers are changed when the mouse is motionless
        Main.map.keyDetector.addModifierExListener(this);
        sourceWays = new LinkedHashSet<>(getLayerManager().getEditDataSet().getSelectedWays());
        for (Way w : sourceWays) {
            w.setHighlighted(true);
        }
    }

    @Override
    public void exitMode() {
        super.exitMode();
        mv.removeMouseListener(this);
        mv.removeMouseMotionListener(this);
        mv.removeTemporaryLayer(temporaryLayer);
        Main.map.statusLine.setDist(-1);
        Main.map.statusLine.repaint();
        Main.map.keyDetector.removeModifierExListener(this);
        removeWayHighlighting(sourceWays);
        pWays = null;
        sourceWays = null;
        referenceSegment = null;
    }

    @Override
    public String getModeHelpText() {
        // TODO: add more detailed feedback based on modifier state.
        // TODO: dynamic messages based on preferences. (Could be problematic translation wise)
        switch (mode) {
        case NORMAL:
            // CHECKSTYLE.OFF: LineLength
            return tr("Select ways as in Select mode. Drag selected ways or a single way to create a parallel copy (Alt toggles tag preservation)");
            // CHECKSTYLE.ON: LineLength
        case DRAGGING:
            return tr("Hold Ctrl to toggle snapping");
        }
        return ""; // impossible ..
    }

    @Override
    public boolean layerIsSupported(Layer layer) {
        return layer instanceof OsmDataLayer;
    }

    @Override
    public void modifiersExChanged(int modifiers) {
        if (Main.map == null || mv == null || !mv.isActiveLayerDrawable())
            return;

        // Should only get InputEvents due to the mask in enterMode
        if (updateModifiersState(modifiers)) {
            updateStatusLine();
            updateCursor();
        }
    }

    private boolean updateModifiersState(int modifiers) {
        boolean oldAlt = alt, oldShift = shift, oldCtrl = ctrl;
        updateKeyModifiersEx(modifiers);
        return oldAlt != alt || oldShift != shift || oldCtrl != ctrl;
    }

    private void updateCursor() {
        Cursor newCursor = null;
        switch (mode) {
        case NORMAL:
            if (matchesCurrentModifiers(SET_SELECTED_MODIFIER_COMBO)) {
                newCursor = ImageProvider.getCursor("normal", "parallel");
            } else if (matchesCurrentModifiers(ADD_TO_SELECTION_MODIFIER_COMBO)) {
                newCursor = ImageProvider.getCursor("normal", "parallel_add");
            } else if (matchesCurrentModifiers(TOGGLE_SELECTED_MODIFIER_COMBO)) {
                newCursor = ImageProvider.getCursor("normal", "parallel_remove");
            }
            break;
        case DRAGGING:
            newCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            break;
        default: throw new AssertionError();
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
        assert areWeSane; // mad == bad
        return areWeSane;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInMapView();
        updateModifiersState(e.getModifiersEx());
        // Other buttons are off limit, but we still get events.
        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        if (!sanityCheck())
            return;

        updateFlagsOnlyChangeableOnPress();
        updateFlagsChangeableAlways();

        // Since the created way is left selected, we need to unselect again here
        if (pWays != null && pWays.getWays() != null) {
            getLayerManager().getEditDataSet().clearSelection(pWays.getWays());
            pWays = null;
        }

        mouseIsDown = true;
        mousePressedPos = e.getPoint();
        mousePressedTime = System.currentTimeMillis();

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        updateModifiersState(e.getModifiersEx());
        // Other buttons are off limit, but we still get events.
        if (e.getButton() != MouseEvent.BUTTON1)
            return;

        if (!mouseHasBeenDragged) {
            // use point from press or click event? (or are these always the same)
            Way nearestWay = mv.getNearestWay(e.getPoint(), OsmPrimitive::isSelectable);
            if (nearestWay == null) {
                if (matchesCurrentModifiers(SET_SELECTED_MODIFIER_COMBO)) {
                    clearSourceWays();
                }
                resetMouseTrackingState();
                return;
            }
            boolean isSelected = nearestWay.isSelected();
            if (matchesCurrentModifiers(ADD_TO_SELECTION_MODIFIER_COMBO)) {
                if (!isSelected) {
                    addSourceWay(nearestWay);
                }
            } else if (matchesCurrentModifiers(TOGGLE_SELECTED_MODIFIER_COMBO)) {
                if (isSelected) {
                    removeSourceWay(nearestWay);
                } else {
                    addSourceWay(nearestWay);
                }
            } else if (matchesCurrentModifiers(SET_SELECTED_MODIFIER_COMBO)) {
                clearSourceWays();
                addSourceWay(nearestWay);
            } // else -> invalid modifier combination
        } else if (mode == Mode.DRAGGING) {
            clearSourceWays();
        }

        setMode(Mode.NORMAL);
        resetMouseTrackingState();
        temporaryLayer.invalidate();
    }

    private static void removeWayHighlighting(Collection<Way> ways) {
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

        boolean modifiersChanged = updateModifiersState(e.getModifiersEx());
        updateFlagsChangeableAlways();

        if (modifiersChanged) {
            // Since this could be remotely slow, do it conditionally
            updateStatusLine();
            updateCursor();
        }

        if ((System.currentTimeMillis() - mousePressedTime) < INITIAL_MOVE_DELAY.get())
            return;
        // Assuming this event only is emitted when the mouse has moved
        // Setting this after the check above means we tolerate clicks with some movement
        mouseHasBeenDragged = true;

        if (mode == Mode.NORMAL) {
            // Should we ensure that the copyTags modifiers are still valid?

            // Important to use mouse position from the press, since the drag
            // event can come quite late
            if (!isModifiersValidForDragMode())
                return;
            if (!initParallelWays(mousePressedPos, copyTags))
                return;
            setMode(Mode.DRAGGING);
        }

        // Calculate distance to the reference line
        Point p = e.getPoint();
        EastNorth enp = mv.getEastNorth((int) p.getX(), (int) p.getY());
        EastNorth nearestPointOnRefLine = Geometry.closestPointToLine(referenceSegment.getFirstNode().getEastNorth(),
                referenceSegment.getSecondNode().getEastNorth(), enp);

        // Note: d is the distance in _projected units_
        double d = enp.distance(nearestPointOnRefLine);
        double realD = mv.getProjection().eastNorth2latlon(enp).greatCircleDistance(mv.getProjection().eastNorth2latlon(nearestPointOnRefLine));
        double snappedRealD = realD;

        boolean toTheRight = Geometry.angleIsClockwise(
                referenceSegment.getFirstNode(), referenceSegment.getSecondNode(), new Node(enp));

        if (snap) {
            // TODO: Very simple snapping
            // - Snap steps relative to the distance?
            double snapDistance;
            SystemOfMeasurement som = SystemOfMeasurement.getSystemOfMeasurement();
            if (som.equals(SystemOfMeasurement.CHINESE)) {
                snapDistance = SNAP_DISTANCE_CHINESE.get() * SystemOfMeasurement.CHINESE.aValue;
            } else if (som.equals(SystemOfMeasurement.IMPERIAL)) {
                snapDistance = SNAP_DISTANCE_IMPERIAL.get() * SystemOfMeasurement.IMPERIAL.aValue;
            } else if (som.equals(SystemOfMeasurement.NAUTICAL_MILE)) {
                snapDistance = SNAP_DISTANCE_NAUTICAL.get() * SystemOfMeasurement.NAUTICAL_MILE.aValue;
            } else {
                snapDistance = SNAP_DISTANCE_METRIC.get(); // Metric system by default
            }
            double closestWholeUnit;
            double modulo = realD % snapDistance;
            if (modulo < snapDistance/2.0) {
                closestWholeUnit = realD - modulo;
            } else {
                closestWholeUnit = realD + (snapDistance-modulo);
            }
            if (Math.abs(closestWholeUnit - realD) < (SNAP_THRESHOLD.get() * snapDistance)) {
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
        temporaryLayer.invalidate();
    }

    private boolean matchesCurrentModifiers(CachingProperty<Map<Modifier, Boolean>> spec) {
        return matchesCurrentModifiers(spec.get());
    }

    private boolean matchesCurrentModifiers(Map<Modifier, Boolean> spec) {
        EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        if (ctrl) {
            modifiers.add(Modifier.CTRL);
        }
        if (alt) {
            modifiers.add(Modifier.ALT);
        }
        if (shift) {
            modifiers.add(Modifier.SHIFT);
        }
        return spec.entrySet().stream().allMatch(entry -> modifiers.contains(entry.getKey()) == entry.getValue().booleanValue());
    }

    private boolean isModifiersValidForDragMode() {
        return (!alt && !shift && !ctrl) || matchesCurrentModifiers(SNAP_MODIFIER_COMBO)
                || matchesCurrentModifiers(COPY_TAGS_MODIFIER_COMBO);
    }

    private void updateFlagsOnlyChangeableOnPress() {
        copyTags = COPY_TAGS_DEFAULT.get().booleanValue() != matchesCurrentModifiers(COPY_TAGS_MODIFIER_COMBO);
    }

    private void updateFlagsChangeableAlways() {
        snap = SNAP_DEFAULT.get().booleanValue() != matchesCurrentModifiers(SNAP_MODIFIER_COMBO);
    }

    // We keep the source ways and the selection in sync so the user can see the source way's tags
    private void addSourceWay(Way w) {
        assert sourceWays != null;
        getLayerManager().getEditDataSet().addSelected(w);
        w.setHighlighted(true);
        sourceWays.add(w);
    }

    private void removeSourceWay(Way w) {
        assert sourceWays != null;
        getLayerManager().getEditDataSet().clearSelection(w);
        w.setHighlighted(false);
        sourceWays.remove(w);
    }

    private void clearSourceWays() {
        assert sourceWays != null;
        getLayerManager().getEditDataSet().clearSelection(sourceWays);
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
        referenceSegment = mv.getNearestWaySegment(p, OsmPrimitive::isUsable, true);
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
            getLayerManager().getEditDataSet().setSelected(pWays.getWays());
            return true;
        } catch (IllegalArgumentException e) {
            Logging.debug(e);
            new Notification(tr("ParallelWayAction\n" +
                    "The ways selected must form a simple branchless path"))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .show();
            // The error dialog prevents us from getting the mouseReleased event
            resetMouseTrackingState();
            pWays = null;
            return false;
        }
    }

    private static String prefKey(String subKey) {
        return "edit.make-parallel-way-action." + subKey;
    }

    /**
     * A property that holds the keyboard modifiers.
     * @author Michael Zangl
     * @since 10869
     */
    private static class KeyboardModifiersProperty extends AbstractToStringProperty<Map<Modifier, Boolean>> {

        KeyboardModifiersProperty(String key, String defaultValue) {
            super(key, createFromString(defaultValue));
        }

        KeyboardModifiersProperty(String key, Map<Modifier, Boolean> defaultValue) {
            super(key, defaultValue);
        }

        @Override
        protected String toString(Map<Modifier, Boolean> t) {
            StringBuilder sb = new StringBuilder();
            for (Modifier mod : Modifier.values()) {
                Boolean val = t.get(mod);
                if (val == null) {
                    sb.append('?');
                } else if (val) {
                    sb.append(Character.toUpperCase(mod.shortChar));
                } else {
                    sb.append(mod.shortChar);
                }
            }
            return sb.toString();
        }

        @Override
        protected Map<Modifier, Boolean> fromString(String string) {
            return createFromString(string);
        }

        private static Map<Modifier, Boolean> createFromString(String string) {
            Map<Modifier, Boolean> ret = new EnumMap<>(Modifier.class);
            for (char c : string.toCharArray()) {
                if (c == '?') {
                    continue;
                }
                Optional<Modifier> mod = Modifier.findWithShortCode(c);
                if (mod.isPresent()) {
                    ret.put(mod.get(), Character.isUpperCase(c));
                } else {
                    Logging.debug("Ignoring unknown modifier {0}", c);
                }
            }
            return Collections.unmodifiableMap(ret);
        }
    }

    enum Modifier {
        CTRL('c'),
        ALT('a'),
        SHIFT('s');

        private final char shortChar;

        Modifier(char shortChar) {
            this.shortChar = Character.toLowerCase(shortChar);
        }

        /**
         * Find the modifier with the given short code
         * @param charCode The short code
         * @return The modifier
         */
        public static Optional<Modifier> findWithShortCode(int charCode) {
            return Stream.of(values()).filter(m -> m.shortChar == Character.toLowerCase(charCode)).findAny();
        }
    }

    private class ParallelWayLayer extends AbstractMapViewPaintable {
        @Override
        public void paint(Graphics2D g, MapView mv, Bounds bbox) {
            if (mode == Mode.DRAGGING) {
                CheckParameterUtil.ensureParameterNotNull(mv, "mv");

                Color mainColor = MAIN_COLOR.get();
                g.setStroke(REF_LINE_STROKE.get());
                g.setColor(mainColor);
                MapViewPath line = new MapViewPath(mv);
                line.moveTo(referenceSegment.getFirstNode());
                line.lineTo(referenceSegment.getSecondNode());
                g.draw(line.computeClippedLine(g.getStroke()));

                g.setStroke(HELPER_LINE_STROKE.get());
                g.setColor(mainColor);
                line = new MapViewPath(mv);
                line.moveTo(helperLineStart);
                line.lineTo(helperLineEnd);
                g.draw(line.computeClippedLine(g.getStroke()));
            }
        }
    }
}
