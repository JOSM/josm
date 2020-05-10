// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

import org.apache.commons.jcs3.access.exception.InvalidArgumentException;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.gpx.GpxDrawHelper;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane.ValidationListener;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.template_engine.ParseError;
import org.openstreetmap.josm.tools.template_engine.TemplateParser;

/**
 * Panel for GPX settings.
 */
public class GPXSettingsPanel extends JPanel implements ValidationListener {

    private static final int WAYPOINT_LABEL_CUSTOM = 6;
    private static final String[] LABEL_PATTERN_TEMPLATE = {Marker.LABEL_PATTERN_AUTO, Marker.LABEL_PATTERN_NAME,
        Marker.LABEL_PATTERN_DESC, "{special:everything}", "?{ '{name}' | '{desc}' | '{formattedWaypointOffset}' }", " "};
    private static final String[] LABEL_PATTERN_DESC = {tr("Auto"), /* gpx data field name */ trc("gpx_field", "Name"),
        /* gpx data field name */ trc("gpx_field", "Desc(ription)"), tr("Everything"), tr("Name or offset"), tr("None"), tr("Custom")};

    private final JRadioButton drawRawGpsLinesGlobal = new JRadioButton(tr("Use global settings"));
    private final JRadioButton drawRawGpsLinesAll = new JRadioButton(tr("All"));
    private final JRadioButton drawRawGpsLinesLocal = new JRadioButton(tr("Local files"));
    private final JRadioButton drawRawGpsLinesNone = new JRadioButton(tr("None"));
    private transient ActionListener drawRawGpsLinesActionListener;
    private final JosmTextField drawRawGpsMaxLineLength = new JosmTextField(8);
    private final JosmTextField drawRawGpsMaxLineLengthLocal = new JosmTextField(8);
    private final JosmTextField drawLineWidth = new JosmTextField(2);
    private final JCheckBox forceRawGpsLines = new JCheckBox(tr("Force lines if no segments imported"));
    private final JCheckBox largeGpsPoints = new JCheckBox(tr("Draw large GPS points"));
    private final JCheckBox hdopCircleGpsPoints = new JCheckBox(tr("Draw a circle from HDOP value"));
    private final JRadioButton colorTypeVelocity = new JRadioButton(tr("Velocity (red = slow, green = fast)"));
    private final JRadioButton colorTypeDirection = new JRadioButton(tr("Direction (red = west, yellow = north, green = east, blue = south)"));
    private final JRadioButton colorTypeDilution = new JRadioButton(tr("Dilution of Position (red = high, green = low, if available)"));
    private final JRadioButton colorTypeQuality = new JRadioButton(tr("Quality (RTKLib only, if available)"));
    private final JRadioButton colorTypeTime = new JRadioButton(tr("Track date"));
    private final JRadioButton colorTypeHeatMap = new JRadioButton(tr("Heat Map (dark = few, bright = many)"));
    private final JRadioButton colorTypeNone = new JRadioButton(tr("Single Color (can be customized in the layer manager)"));
    private final JRadioButton colorTypeGlobal = new JRadioButton(tr("Use global settings"));
    private final JosmComboBox<String> colorTypeVelocityTune = new JosmComboBox<>(new String[] {tr("Car"), tr("Bicycle"), tr("Foot")});
    private final JosmComboBox<String> colorTypeHeatMapTune = new JosmComboBox<>(new String[] {
        trc("Heat map", "User Normal"),
        trc("Heat map", "User Light"),
        trc("Heat map", "Traffic Lights"),
        trc("Heat map", "Inferno"),
        trc("Heat map", "Viridis"),
        trc("Heat map", "Wood"),
        trc("Heat map", "Heat")});
    private final JCheckBox colorTypeHeatMapPoints = new JCheckBox(tr("Use points instead of lines for heat map"));
    private final JSlider colorTypeHeatMapGain = new JSlider();
    private final JSlider colorTypeHeatMapLowerLimit = new JSlider();
    private final JCheckBox makeAutoMarkers = new JCheckBox(tr("Create markers when reading GPX"));
    private final JCheckBox drawGpsArrows = new JCheckBox(tr("Draw Direction Arrows"));
    private final JCheckBox drawGpsArrowsFast = new JCheckBox(tr("Fast drawing (looks uglier)"));
    private final JosmTextField drawGpsArrowsMinDist = new JosmTextField(8);
    private final JCheckBox colorDynamic = new JCheckBox(tr("Dynamic color range based on data limits"));
    private final JosmComboBox<String> waypointLabel = new JosmComboBox<>(LABEL_PATTERN_DESC);
    private final JosmTextField waypointLabelPattern = new JosmTextField();
    private final JosmComboBox<String> audioWaypointLabel = new JosmComboBox<>(LABEL_PATTERN_DESC);
    private final JosmTextField audioWaypointLabelPattern = new JosmTextField();
    private final JCheckBox useGpsAntialiasing = new JCheckBox(tr("Smooth GPX graphics (antialiasing)"));
    private final JCheckBox drawLineWithAlpha = new JCheckBox(tr("Draw with Opacity (alpha blending) "));

    private final List<GpxLayer> layers;
    private final GpxLayer firstLayer;
    private final boolean global; // global settings vs. layer specific settings
    private final boolean hasLocalFile; // flag to display LocalOnly checkbooks
    private final boolean hasNonLocalFile; // flag to display AllLines checkbox

    private static final Map<String, Object> DEFAULT_PREFS = getDefaultPrefs();

    private static Map<String, Object> getDefaultPrefs() {
        HashMap<String, Object> m = new HashMap<>();
        m.put("colormode", -1);
        m.put("colormode.dynamic-range", false);
        m.put("colormode.heatmap.colormap", 0);
        m.put("colormode.heatmap.gain", 0);
        m.put("colormode.heatmap.line-extra", false); //Einstein only
        m.put("colormode.heatmap.lower-limit", 0);
        m.put("colormode.heatmap.use-points", false);
        m.put("colormode.time.min-distance", 60); //Einstein only
        m.put("colormode.velocity.tune", 45);
        m.put("lines", -1);
        m.put("lines.alpha-blend", false);
        m.put("lines.arrows", false);
        m.put("lines.arrows.fast", false);
        m.put("lines.arrows.min-distance", 40);
        m.put("lines.force", false);
        m.put("lines.max-length", 200);
        m.put("lines.max-length.local", -1);
        m.put("lines.width", 0);
        m.put("markers.color", "");
        m.put("markers.show-text", true);
        m.put("markers.pattern", Marker.LABEL_PATTERN_AUTO);
        m.put("markers.audio.pattern", "?{ '{name}' | '{desc}' | '{" + Marker.MARKER_FORMATTED_OFFSET + "}' }");
        m.put("points.hdopcircle", false);
        m.put("points.large", false);
        m.put("points.large.alpha", -1); //Einstein only
        m.put("points.large.size", 3); //Einstein only
        return Collections.unmodifiableMap(m);
    }

    /**
     * Constructs a new {@code GPXSettingsPanel} for the given layers.
     * @param layers the GPX layers
     */
    public GPXSettingsPanel(List<GpxLayer> layers) {
        super(new GridBagLayout());
        this.layers = layers;
        if (layers == null || layers.isEmpty()) {
            throw new InvalidArgumentException("At least one layer required");
        }
        firstLayer = layers.get(0);
        global = false;
        hasLocalFile = layers.stream().anyMatch(l -> !l.data.fromServer);
        hasNonLocalFile = layers.stream().anyMatch(l -> l.data.fromServer);
        initComponents();
        loadPreferences();
    }

    /**
     * Constructs a new {@code GPXSettingsPanel}.
     */
    public GPXSettingsPanel() {
        super(new GridBagLayout());
        layers = null;
        firstLayer = null;
        global = hasLocalFile = hasNonLocalFile = true;
        initComponents();
        loadPreferences(); // preferences -> controls
    }

    /**
     * Reads the preference for the given layer or the default preference if not available
     * @param layer the GpxLayer. Can be <code>null</code>, default preference will be returned then
     * @param key the drawing key to be read, without "draw.rawgps."
     * @return the value
     */
    public static String getLayerPref(GpxLayer layer, String key) {
        Object d = DEFAULT_PREFS.get(key);
        String ds;
        if (d != null) {
            ds = d.toString();
        } else {
            Logging.warn("No default value found for layer preference \"" + key + "\".");
            ds = null;
        }
        return Optional.ofNullable(tryGetLayerPrefLocal(layer, key)).orElse(Config.getPref().get("draw.rawgps." + key, ds));
    }

    /**
     * Reads the integer preference for the given layer or the default preference if not available
     * @param layer the GpxLayer. Can be <code>null</code>, default preference will be returned then
     * @param key the drawing key to be read, without "draw.rawgps."
     * @return the integer value
     */
    public static int getLayerPrefInt(GpxLayer layer, String key) {
        String s = getLayerPref(layer, key);
        if (s != null) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                Object d = DEFAULT_PREFS.get(key);
                if (d instanceof Integer) {
                    return (int) d;
                } else {
                    Logging.warn("No valid default value found for layer preference \"" + key + "\".");
                }
            }
        }
        return 0;
    }

    /**
     * Try to read the preference for the given layer
     * @param layer the GpxLayer
     * @param key the drawing key to be read, without "draw.rawgps."
     * @return the value or <code>null</code> if not found
     */
    public static String tryGetLayerPrefLocal(GpxLayer layer, String key) {
        return layer != null ? tryGetLayerPrefLocal(layer.data, key) : null;
    }

    /**
     * Try to read the preference for the given GpxData
     * @param data the GpxData
     * @param key the drawing key to be read, without "draw.rawgps."
     * @return the value or <code>null</code> if not found
     */
    public static String tryGetLayerPrefLocal(GpxData data, String key) {
        return data != null ? data.getLayerPrefs().get(key) : null;
    }

    /**
     * Puts the preference for the given layers or the default preference if layers is <code>null</code>
     * @param layers List of <code>GpxLayer</code> to put the drawingOptions
     * @param key the drawing key to be written, without "draw.rawgps."
     * @param value (can be <code>null</code> to remove option)
     */
    public static void putLayerPref(List<GpxLayer> layers, String key, Object value) {
        String v = value == null ? null : value.toString();
        if (layers != null) {
            for (GpxLayer l : layers) {
                putLayerPrefLocal(l.data, key, v);
            }
        } else {
            Config.getPref().put("draw.rawgps." + key, v);
        }
    }

    /**
     * Puts the preference for the given layer
     * @param layer <code>GpxLayer</code> to put the drawingOptions
     * @param key the drawing key to be written, without "draw.rawgps."
     * @param value the value or <code>null</code> to remove key
     */
    public static void putLayerPrefLocal(GpxLayer layer, String key, String value) {
        if (layer == null) return;
        putLayerPrefLocal(layer.data, key, value);
    }

    /**
     * Puts the preference for the given layer
     * @param data <code>GpxData</code> to put the drawingOptions. Must not be <code>null</code>
     * @param key the drawing key to be written, without "draw.rawgps."
     * @param value the value or <code>null</code> to remove key
     */
    public static void putLayerPrefLocal(GpxData data, String key, String value) {
        if (value == null || value.trim().isEmpty() ||
                (getLayerPref(null, key).equals(value) && DEFAULT_PREFS.get(key) != null && DEFAULT_PREFS.get(key).toString().equals(value))) {
            data.getLayerPrefs().remove(key);
        } else {
            data.getLayerPrefs().put(key, value);
        }
    }

    private String pref(String key) {
        return getLayerPref(firstLayer, key);
    }

    private boolean prefBool(String key) {
        return Boolean.parseBoolean(pref(key));
    }

    private int prefInt(String key) {
        return getLayerPrefInt(firstLayer, key);
    }

    private int prefIntLocal(String key) {
        try {
            return Integer.parseInt(tryGetLayerPrefLocal(firstLayer, key));
        } catch (NumberFormatException ex) {
            return -1;
        }

    }

    private void putPref(String key, Object value) {
        putLayerPref(layers, key, value);
    }

    // CHECKSTYLE.OFF: ExecutableStatementCountCheck
    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (global) {
            // makeAutoMarkers
            makeAutoMarkers.setToolTipText(tr("Automatically make a marker layer from any waypoints when opening a GPX layer."));
            ExpertToggleAction.addVisibilitySwitcher(makeAutoMarkers);
            add(makeAutoMarkers, GBC.eol().insets(20, 0, 0, 5));
        }

        // drawRawGpsLines
        ButtonGroup gpsLinesGroup = new ButtonGroup();
        if (!global) {
            gpsLinesGroup.add(drawRawGpsLinesGlobal);
        }
        gpsLinesGroup.add(drawRawGpsLinesNone);
        gpsLinesGroup.add(drawRawGpsLinesLocal);
        gpsLinesGroup.add(drawRawGpsLinesAll);

        /* ensure that default is in data base */

        JLabel label = new JLabel(tr("Draw lines between raw GPS points"));
        add(label, GBC.eol().insets(20, 0, 0, 0));
        if (!global) {
            add(drawRawGpsLinesGlobal, GBC.eol().insets(40, 0, 0, 0));
        }
        add(drawRawGpsLinesNone, GBC.eol().insets(40, 0, 0, 0));
        if (hasLocalFile) {
            add(drawRawGpsLinesLocal, GBC.eol().insets(40, 0, 0, 0));
        }
        if (hasNonLocalFile) {
            add(drawRawGpsLinesAll, GBC.eol().insets(40, 0, 0, 0));
        }
        ExpertToggleAction.addVisibilitySwitcher(label);
        ExpertToggleAction.addVisibilitySwitcher(drawRawGpsLinesGlobal);
        ExpertToggleAction.addVisibilitySwitcher(drawRawGpsLinesNone);
        ExpertToggleAction.addVisibilitySwitcher(drawRawGpsLinesLocal);
        ExpertToggleAction.addVisibilitySwitcher(drawRawGpsLinesAll);

        drawRawGpsLinesActionListener = e -> {
            boolean f = drawRawGpsLinesNone.isSelected() || drawRawGpsLinesGlobal.isSelected();
            forceRawGpsLines.setEnabled(!f);
            drawRawGpsMaxLineLength.setEnabled(!(f || drawRawGpsLinesLocal.isSelected()));
            drawRawGpsMaxLineLengthLocal.setEnabled(!f);
            drawGpsArrows.setEnabled(!f);
            drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
            drawGpsArrowsMinDist.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
        };

        drawRawGpsLinesGlobal.addActionListener(drawRawGpsLinesActionListener);
        drawRawGpsLinesNone.addActionListener(drawRawGpsLinesActionListener);
        drawRawGpsLinesLocal.addActionListener(drawRawGpsLinesActionListener);
        drawRawGpsLinesAll.addActionListener(drawRawGpsLinesActionListener);

        // drawRawGpsMaxLineLengthLocal
        drawRawGpsMaxLineLengthLocal.setToolTipText(
                tr("Maximum length (in meters) to draw lines for local files. Set to ''-1'' to draw all lines."));
        label = new JLabel(tr("Maximum length for local files (meters)"));
        add(label, GBC.std().insets(40, 0, 0, 0));
        add(drawRawGpsMaxLineLengthLocal, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));
        ExpertToggleAction.addVisibilitySwitcher(label);
        ExpertToggleAction.addVisibilitySwitcher(drawRawGpsMaxLineLengthLocal);

        // drawRawGpsMaxLineLength
        drawRawGpsMaxLineLength.setToolTipText(tr("Maximum length (in meters) to draw lines. Set to ''-1'' to draw all lines."));
        label = new JLabel(tr("Maximum length (meters)"));
        add(label, GBC.std().insets(40, 0, 0, 0));
        add(drawRawGpsMaxLineLength, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));
        ExpertToggleAction.addVisibilitySwitcher(label);
        ExpertToggleAction.addVisibilitySwitcher(drawRawGpsMaxLineLength);

        // forceRawGpsLines
        forceRawGpsLines.setToolTipText(tr("Force drawing of lines if the imported data contain no line information."));
        add(forceRawGpsLines, GBC.eop().insets(40, 0, 0, 0));
        ExpertToggleAction.addVisibilitySwitcher(forceRawGpsLines);

        // drawGpsArrows
        drawGpsArrows.addActionListener(e -> {
            drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
            drawGpsArrowsMinDist.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
        });
        drawGpsArrows.setToolTipText(tr("Draw direction arrows for lines, connecting GPS points."));
        add(drawGpsArrows, GBC.eop().insets(20, 0, 0, 0));

        // drawGpsArrowsFast
        drawGpsArrowsFast.setToolTipText(tr("Draw the direction arrows using table lookups instead of complex math."));
        add(drawGpsArrowsFast, GBC.eop().insets(40, 0, 0, 0));
        ExpertToggleAction.addVisibilitySwitcher(drawGpsArrowsFast);

        // drawGpsArrowsMinDist
        drawGpsArrowsMinDist.setToolTipText(tr("Do not draw arrows if they are not at least this distance away from the last one."));
        add(new JLabel(tr("Minimum distance (pixels)")), GBC.std().insets(40, 0, 0, 0));
        add(drawGpsArrowsMinDist, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));

        // hdopCircleGpsPoints
        hdopCircleGpsPoints.setToolTipText(tr("Draw a circle from HDOP value"));
        add(hdopCircleGpsPoints, GBC.eop().insets(20, 0, 0, 0));
        ExpertToggleAction.addVisibilitySwitcher(hdopCircleGpsPoints);

        // largeGpsPoints
        largeGpsPoints.setToolTipText(tr("Draw larger dots for the GPS points."));
        add(largeGpsPoints, GBC.eop().insets(20, 0, 0, 0));

        // drawLineWidth
        drawLineWidth.setToolTipText(tr("Width of drawn GPX line (0 for default)"));
        add(new JLabel(tr("Drawing width of GPX lines")), GBC.std().insets(20, 0, 0, 0));
        add(drawLineWidth, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));

        // antialiasing
        useGpsAntialiasing.setToolTipText(tr("Apply antialiasing to the GPX lines resulting in a smoother appearance."));
        add(useGpsAntialiasing, GBC.eop().insets(20, 0, 0, 0));
        ExpertToggleAction.addVisibilitySwitcher(useGpsAntialiasing);

        // alpha blending
        drawLineWithAlpha.setToolTipText(tr("Apply dynamic alpha-blending and adjust width based on zoom level for all GPX lines."));
        add(drawLineWithAlpha, GBC.eop().insets(20, 0, 0, 0));
        ExpertToggleAction.addVisibilitySwitcher(drawLineWithAlpha);

        // colorTracks
        ButtonGroup colorGroup = new ButtonGroup();
        if (!global) {
            colorGroup.add(colorTypeGlobal);
        }
        colorGroup.add(colorTypeNone);
        colorGroup.add(colorTypeVelocity);
        colorGroup.add(colorTypeDirection);
        colorGroup.add(colorTypeDilution);
        colorGroup.add(colorTypeQuality);
        colorGroup.add(colorTypeTime);
        colorGroup.add(colorTypeHeatMap);

        colorTypeNone.setToolTipText(tr("All points and track segments will have their own color. Can be customized in Layer Manager."));
        colorTypeVelocity.setToolTipText(tr("Colors points and track segments by velocity."));
        colorTypeDirection.setToolTipText(tr("Colors points and track segments by direction."));
        colorTypeDilution.setToolTipText(
                tr("Colors points and track segments by dilution of position (HDOP). Your capture device needs to log that information."));
        colorTypeQuality.setToolTipText(
                tr("Colors points and track segments by RTKLib quality flag (Q). Your capture device needs to log that information."));
        colorTypeTime.setToolTipText(tr("Colors points and track segments by its timestamp."));
        colorTypeHeatMap.setToolTipText(tr("Collected points and track segments for a position and displayed as heat map."));

        // color Tracks by Velocity Tune
        colorTypeVelocityTune.setToolTipText(tr("Allows to tune the track coloring for different average speeds."));

        colorTypeHeatMapTune.setToolTipText(tr("Selects the color schema for heat map."));
        JLabel colorTypeHeatIconLabel = new JLabel();

        add(Box.createVerticalGlue(), GBC.eol().insets(0, 20, 0, 0));

        add(new JLabel(tr("Track and Point Coloring")), GBC.eol().insets(20, 0, 0, 0));
        if (!global) {
            add(colorTypeGlobal, GBC.eol().insets(40, 0, 0, 0));
        }
        add(colorTypeNone, GBC.eol().insets(40, 0, 0, 0));
        add(colorTypeVelocity, GBC.std().insets(40, 0, 0, 0));
        add(colorTypeVelocityTune, GBC.eop().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));
        add(colorTypeDirection, GBC.eol().insets(40, 0, 0, 0));
        add(colorTypeDilution, GBC.eol().insets(40, 0, 0, 0));
        add(colorTypeQuality, GBC.eol().insets(40, 0, 0, 0));
        add(colorTypeTime, GBC.eol().insets(40, 0, 0, 0));
        add(colorTypeHeatMap, GBC.std().insets(40, 0, 0, 0));
        add(colorTypeHeatIconLabel, GBC.std().insets(5, 0, 0, 5));
        add(colorTypeHeatMapTune, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));

        JLabel colorTypeHeatMapGainLabel = new JLabel(tr("Overlay gain adjustment"));
        JLabel colorTypeHeatMapLowerLimitLabel = new JLabel(tr("Lower limit of visibility"));
        add(colorTypeHeatMapGainLabel, GBC.std().insets(80, 0, 0, 0));
        add(colorTypeHeatMapGain, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));
        add(colorTypeHeatMapLowerLimitLabel, GBC.std().insets(80, 0, 0, 0));
        add(colorTypeHeatMapLowerLimit, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));
        add(colorTypeHeatMapPoints, GBC.eol().insets(60, 0, 0, 0));

        colorTypeHeatMapGain.setToolTipText(tr("Adjust the gain of overlay blending."));
        colorTypeHeatMapGain.setOrientation(JSlider.HORIZONTAL);
        colorTypeHeatMapGain.setPaintLabels(true);
        colorTypeHeatMapGain.setMinimum(-10);
        colorTypeHeatMapGain.setMaximum(+10);
        colorTypeHeatMapGain.setMinorTickSpacing(1);
        colorTypeHeatMapGain.setMajorTickSpacing(5);

        colorTypeHeatMapLowerLimit.setToolTipText(tr("Draw all GPX traces that exceed this threshold."));
        colorTypeHeatMapLowerLimit.setOrientation(JSlider.HORIZONTAL);
        colorTypeHeatMapLowerLimit.setMinimum(0);
        colorTypeHeatMapLowerLimit.setMaximum(254);
        colorTypeHeatMapLowerLimit.setPaintLabels(true);
        colorTypeHeatMapLowerLimit.setMinorTickSpacing(10);
        colorTypeHeatMapLowerLimit.setMajorTickSpacing(100);

        colorTypeHeatMapPoints.setToolTipText(tr("Render engine uses points with simulated position error instead of lines. "));

        // iterate over the buttons, add change listener to any change event
        for (Enumeration<AbstractButton> button = colorGroup.getElements(); button.hasMoreElements();) {
            (button.nextElement()).addChangeListener(e -> {
                colorTypeVelocityTune.setEnabled(colorTypeVelocity.isSelected());
                colorTypeHeatMapTune.setEnabled(colorTypeHeatMap.isSelected());
                colorTypeHeatMapPoints.setEnabled(colorTypeHeatMap.isSelected());
                colorTypeHeatMapGain.setEnabled(colorTypeHeatMap.isSelected());
                colorTypeHeatMapLowerLimit.setEnabled(colorTypeHeatMap.isSelected());
                colorTypeHeatMapGainLabel.setEnabled(colorTypeHeatMap.isSelected());
                colorTypeHeatMapLowerLimitLabel.setEnabled(colorTypeHeatMap.isSelected());
                colorDynamic.setEnabled(colorTypeVelocity.isSelected() || colorTypeDilution.isSelected());
            });
        }

        colorTypeHeatMapTune.addActionListener(e -> {
            final Dimension dim = colorTypeHeatMapTune.getPreferredSize();
            if (null != dim) {
                // get image size of environment
                final int iconSize = (int) dim.getHeight();
                colorTypeHeatIconLabel.setIcon(GpxDrawHelper.getColorMapImageIcon(
                        GpxDrawHelper.DEFAULT_COLOR_PROPERTY.get(),
                        colorTypeHeatMapTune.getSelectedIndex(),
                        iconSize));
            }
        });

        ExpertToggleAction.addVisibilitySwitcher(colorTypeDirection);
        ExpertToggleAction.addVisibilitySwitcher(colorTypeDilution);
        ExpertToggleAction.addVisibilitySwitcher(colorTypeQuality);
        ExpertToggleAction.addVisibilitySwitcher(colorTypeHeatMapLowerLimit);
        ExpertToggleAction.addVisibilitySwitcher(colorTypeHeatMapLowerLimitLabel);

        colorDynamic.setToolTipText(tr("Colors points and track segments by data limits."));
        add(colorDynamic, GBC.eop().insets(40, 0, 0, 0));
        ExpertToggleAction.addVisibilitySwitcher(colorDynamic);

        if (global) {
            // Setting waypoints for gpx layer doesn't make sense - waypoints are shown in marker layer that has different name - so show
            // this only for global config

            // waypointLabel
            label = new JLabel(tr("Waypoint labelling"));
            add(label, GBC.std().insets(20, 0, 0, 0));
            label.setLabelFor(waypointLabel);
            add(waypointLabel, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));
            waypointLabel.addActionListener(e -> updateWaypointPattern(waypointLabel, waypointLabelPattern));
            add(waypointLabelPattern, GBC.eol().fill(GBC.HORIZONTAL).insets(20, 0, 0, 5));
            ExpertToggleAction.addVisibilitySwitcher(label);
            ExpertToggleAction.addVisibilitySwitcher(waypointLabel);
            ExpertToggleAction.addVisibilitySwitcher(waypointLabelPattern);

            // audioWaypointLabel
            Component glue = Box.createVerticalGlue();
            add(glue, GBC.eol().insets(0, 20, 0, 0));
            ExpertToggleAction.addVisibilitySwitcher(glue);

            label = new JLabel(tr("Audio waypoint labelling"));
            add(label, GBC.std().insets(20, 0, 0, 0));
            label.setLabelFor(audioWaypointLabel);
            add(audioWaypointLabel, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));
            audioWaypointLabel.addActionListener(e -> updateWaypointPattern(audioWaypointLabel, audioWaypointLabelPattern));
            add(audioWaypointLabelPattern, GBC.eol().fill(GBC.HORIZONTAL).insets(20, 0, 0, 5));
            ExpertToggleAction.addVisibilitySwitcher(label);
            ExpertToggleAction.addVisibilitySwitcher(audioWaypointLabel);
            ExpertToggleAction.addVisibilitySwitcher(audioWaypointLabelPattern);
        }

        add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
    }
    // CHECKSTYLE.ON: ExecutableStatementCountCheck

    /**
     * Loads preferences to UI controls
     */
    public final void loadPreferences() {
        makeAutoMarkers.setSelected(Config.getPref().getBoolean("marker.makeautomarkers", true));
        int lines = global ? prefInt("lines") : prefIntLocal("lines");
        // -1 = global (default: all)
        //  0 = none
        //  1 = local
        //  2 = all
        if ((lines == 2 && hasNonLocalFile) || (lines == -1 && global)) {
            drawRawGpsLinesAll.setSelected(true);
        } else if (lines == 1 && hasLocalFile) {
            drawRawGpsLinesLocal.setSelected(true);
        } else if (lines == 0) {
            drawRawGpsLinesNone.setSelected(true);
        } else if (lines == -1) {
            drawRawGpsLinesGlobal.setSelected(true);
        } else {
            Logging.warn("Unknown line type: " + lines);
        }
        drawRawGpsMaxLineLengthLocal.setText(pref("lines.max-length.local"));
        drawRawGpsMaxLineLength.setText(pref("lines.max-length"));
        drawLineWidth.setText(pref("lines.width"));
        drawLineWithAlpha.setSelected(prefBool("lines.alpha-blend"));
        forceRawGpsLines.setSelected(prefBool("lines.force"));
        drawGpsArrows.setSelected(prefBool("lines.arrows"));
        drawGpsArrowsFast.setSelected(prefBool("lines.arrows.fast"));
        drawGpsArrowsMinDist.setText(pref("lines.arrows.min-distance"));
        hdopCircleGpsPoints.setSelected(prefBool("points.hdopcircle"));
        largeGpsPoints.setSelected(prefBool("points.large"));
        useGpsAntialiasing.setSelected(Config.getPref().getBoolean("mappaint.gpx.use-antialiasing", false));

        drawRawGpsLinesActionListener.actionPerformed(null);
        if (!global && prefIntLocal("colormode") == -1) {
            colorTypeGlobal.setSelected(true);
            colorDynamic.setSelected(false);
            colorDynamic.setEnabled(false);
            colorTypeHeatMapPoints.setSelected(false);
            colorTypeHeatMapGain.setValue(0);
            colorTypeHeatMapLowerLimit.setValue(0);
        } else {
            int colorType = prefInt("colormode");
            switch (colorType) {
            case -1: case 0: colorTypeNone.setSelected(true); break;
            case 1: colorTypeVelocity.setSelected(true); break;
            case 2: colorTypeDilution.setSelected(true); break;
            case 3: colorTypeDirection.setSelected(true); break;
            case 4: colorTypeTime.setSelected(true); break;
            case 5: colorTypeHeatMap.setSelected(true); break;
            case 6: colorTypeQuality.setSelected(true); break;
            default: Logging.warn("Unknown color type: " + colorType);
            }
            int ccts = prefInt("colormode.velocity.tune");
            colorTypeVelocityTune.setSelectedIndex(ccts == 10 ? 2 : (ccts == 20 ? 1 : 0));
            colorTypeHeatMapTune.setSelectedIndex(prefInt("colormode.heatmap.colormap"));
            colorDynamic.setSelected(prefBool("colormode.dynamic-range"));
            colorTypeHeatMapPoints.setSelected(prefBool("colormode.heatmap.use-points"));
            colorTypeHeatMapGain.setValue(prefInt("colormode.heatmap.gain"));
            colorTypeHeatMapLowerLimit.setValue(prefInt("colormode.heatmap.lower-limit"));
        }
        updateWaypointLabelCombobox(waypointLabel, waypointLabelPattern, pref("markers.pattern"));
        updateWaypointLabelCombobox(audioWaypointLabel, audioWaypointLabelPattern, pref("markers.audio.pattern"));

    }

    /**
     * Save preferences from UI controls, globally or for the specified layers.
     * @return {@code true} when restart is required, {@code false} otherwise
     */
    public boolean savePreferences() {
        if (global) {
            Config.getPref().putBoolean("marker.makeautomarkers", makeAutoMarkers.isSelected());
            putPref("markers.pattern", waypointLabelPattern.getText());
            putPref("markers.audio.pattern", audioWaypointLabelPattern.getText());
        }
        boolean g;
        if (!global && ((g = drawRawGpsLinesGlobal.isSelected()) || drawRawGpsLinesNone.isSelected())) {
            if (g) {
                putPref("lines", null);
            } else {
                putPref("lines", 0);
            }
            putPref("lines.max-length", null);
            putPref("lines.max-length.local", null);
            putPref("lines.force", null);
            putPref("lines.arrows", null);
            putPref("lines.arrows.fast", null);
            putPref("lines.arrows.min-distance", null);
        } else {
            if (drawRawGpsLinesLocal.isSelected()) {
                putPref("lines", 1);
            } else if (drawRawGpsLinesAll.isSelected()) {
                putPref("lines", 2);
            }
            putPref("lines.max-length", drawRawGpsMaxLineLength.getText());
            putPref("lines.max-length.local", drawRawGpsMaxLineLengthLocal.getText());
            putPref("lines.force", forceRawGpsLines.isSelected());
            putPref("lines.arrows", drawGpsArrows.isSelected());
            putPref("lines.arrows.fast", drawGpsArrowsFast.isSelected());
            putPref("lines.arrows.min-distance", drawGpsArrowsMinDist.getText());
        }

        putPref("points.hdopcircle", hdopCircleGpsPoints.isSelected());
        putPref("points.large", largeGpsPoints.isSelected());
        putPref("lines.width", drawLineWidth.getText());
        putPref("lines.alpha-blend", drawLineWithAlpha.isSelected());

        Config.getPref().putBoolean("mappaint.gpx.use-antialiasing", useGpsAntialiasing.isSelected());

        if (colorTypeGlobal.isSelected()) {
            putPref("colormode", null);
            putPref("colormode.dynamic-range", null);
            putPref("colormode.velocity.tune", null);
            return false;
        } else if (colorTypeVelocity.isSelected()) {
            putPref("colormode", 1);
        } else if (colorTypeDilution.isSelected()) {
            putPref("colormode", 2);
        } else if (colorTypeDirection.isSelected()) {
            putPref("colormode", 3);
        } else if (colorTypeTime.isSelected()) {
            putPref("colormode", 4);
        } else if (colorTypeHeatMap.isSelected()) {
            putPref("colormode", 5);
        } else if (colorTypeQuality.isSelected()) {
            putPref("colormode", 6);
        } else {
            putPref("colormode", 0);
        }
        putPref("colormode.dynamic-range", colorDynamic.isSelected());
        int ccti = colorTypeVelocityTune.getSelectedIndex();
        putPref("colormode.velocity.tune", ccti == 2 ? 10 : (ccti == 1 ? 20 : 45));
        putPref("colormode.heatmap.colormap", colorTypeHeatMapTune.getSelectedIndex());
        putPref("colormode.heatmap.use-points", colorTypeHeatMapPoints.isSelected());
        putPref("colormode.heatmap.gain", colorTypeHeatMapGain.getValue());
        putPref("colormode.heatmap.lower-limit", colorTypeHeatMapLowerLimit.getValue());

        if (!global && layers != null && !layers.isEmpty()) {
            layers.forEach(l -> l.data.invalidate());
        }

        return false;
    }

    private static void updateWaypointLabelCombobox(JosmComboBox<String> cb, JosmTextField tf, String labelPattern) {
        boolean found = false;
        for (int i = 0; i < LABEL_PATTERN_TEMPLATE.length; i++) {
            if (LABEL_PATTERN_TEMPLATE[i].equals(labelPattern)) {
                cb.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        if (!found) {
            cb.setSelectedIndex(WAYPOINT_LABEL_CUSTOM);
            tf.setEnabled(true);
            tf.setText(labelPattern);
        }
    }

    private static void updateWaypointPattern(JosmComboBox<String> cb, JosmTextField tf) {
        if (cb.getSelectedIndex() == WAYPOINT_LABEL_CUSTOM) {
            tf.setEnabled(true);
        } else {
            tf.setEnabled(false);
            tf.setText(LABEL_PATTERN_TEMPLATE[cb.getSelectedIndex()]);
        }
    }

    @Override
    public boolean validatePreferences() {
        TemplateParser parser = new TemplateParser(waypointLabelPattern.getText());
        try {
            parser.parse();
        } catch (ParseError e) {
            Logging.warn(e);
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    tr("Incorrect waypoint label pattern: {0}", e.getMessage()), tr("Incorrect pattern"), JOptionPane.ERROR_MESSAGE);
            waypointLabelPattern.requestFocus();
            return false;
        }
        parser = new TemplateParser(audioWaypointLabelPattern.getText());
        try {
            parser.parse();
        } catch (ParseError e) {
            Logging.warn(e);
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    tr("Incorrect audio waypoint label pattern: {0}", e.getMessage()), tr("Incorrect pattern"), JOptionPane.ERROR_MESSAGE);
            audioWaypointLabelPattern.requestFocus();
            return false;
        }
        return true;
    }
}
