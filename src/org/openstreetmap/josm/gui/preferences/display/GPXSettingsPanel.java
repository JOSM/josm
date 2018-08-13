// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Enumeration;

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

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.PreferencesUtils;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.gpx.GpxDrawHelper;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker.TemplateEntryProperty;
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
    private static final String[] LABEL_PATTERN_TEMPLATE = new String[] {Marker.LABEL_PATTERN_AUTO, Marker.LABEL_PATTERN_NAME,
        Marker.LABEL_PATTERN_DESC, "{special:everything}", "?{ '{name}' | '{desc}' | '{formattedWaypointOffset}' }", " "};
    private static final String[] LABEL_PATTERN_DESC = new String[] {tr("Auto"), /* gpx data field name */ trc("gpx_field", "Name"),
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
    private final JRadioButton colorTypeTime = new JRadioButton(tr("Track date"));
    private final JRadioButton colorTypeHeatMap = new JRadioButton(tr("Heat Map (dark = few, bright = many)"));
    private final JRadioButton colorTypeNone = new JRadioButton(tr("Single Color (can be customized for named layers)"));
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

    private String layerName;
    private final boolean local; // flag to display LocalOnly checkbox
    private final boolean nonlocal; // flag to display AllLines checkbox

    /**
     * Constructs a new {@code GPXSettingsPanel} for a given layer name.
     * @param layerName The GPX layer name
     * @param local flag to display LocalOnly checkbox
     * @param nonlocal flag to display AllLines checkbox
     */
    public GPXSettingsPanel(String layerName, boolean local, boolean nonlocal) {
        super(new GridBagLayout());
        this.local = local;
        this.nonlocal = nonlocal;
        this.layerName = "layer "+layerName;
        initComponents();
        loadPreferences();
    }

    /**
     * Constructs a new {@code GPXSettingsPanel}.
     */
    public GPXSettingsPanel() {
        super(new GridBagLayout());
        initComponents();
        local = false;
        nonlocal = false;
        loadPreferences(); // preferences -> controls
    }

    // CHECKSTYLE.OFF: ExecutableStatementCountCheck
    private void initComponents() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // makeAutoMarkers
        makeAutoMarkers.setToolTipText(tr("Automatically make a marker layer from any waypoints when opening a GPX layer."));
        ExpertToggleAction.addVisibilitySwitcher(makeAutoMarkers);
        add(makeAutoMarkers, GBC.eol().insets(20, 0, 0, 5));

        // drawRawGpsLines
        ButtonGroup gpsLinesGroup = new ButtonGroup();
        if (layerName != null) {
            gpsLinesGroup.add(drawRawGpsLinesGlobal);
        }
        gpsLinesGroup.add(drawRawGpsLinesNone);
        gpsLinesGroup.add(drawRawGpsLinesLocal);
        gpsLinesGroup.add(drawRawGpsLinesAll);

        /* ensure that default is in data base */

        JLabel label = new JLabel(tr("Draw lines between raw GPS points"));
        add(label, GBC.eol().insets(20, 0, 0, 0));
        if (layerName != null) {
            add(drawRawGpsLinesGlobal, GBC.eol().insets(40, 0, 0, 0));
        }
        add(drawRawGpsLinesNone, GBC.eol().insets(40, 0, 0, 0));
        if (layerName == null || local) {
            add(drawRawGpsLinesLocal, GBC.eol().insets(40, 0, 0, 0));
        }
        if (layerName == null || nonlocal) {
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
        if (layerName != null) {
            colorGroup.add(colorTypeGlobal);
        }
        colorGroup.add(colorTypeNone);
        colorGroup.add(colorTypeVelocity);
        colorGroup.add(colorTypeDirection);
        colorGroup.add(colorTypeDilution);
        colorGroup.add(colorTypeTime);
        colorGroup.add(colorTypeHeatMap);

        colorTypeNone.setToolTipText(tr("All points and track segments will have the same color. Can be customized in Layer Manager."));
        colorTypeVelocity.setToolTipText(tr("Colors points and track segments by velocity."));
        colorTypeDirection.setToolTipText(tr("Colors points and track segments by direction."));
        colorTypeDilution.setToolTipText(
                tr("Colors points and track segments by dilution of position (HDOP). Your capture device needs to log that information."));
        colorTypeTime.setToolTipText(tr("Colors points and track segments by its timestamp."));
        colorTypeHeatMap.setToolTipText(tr("Collected points and track segments for a position and displayed as heat map."));

        // color Tracks by Velocity Tune
        colorTypeVelocityTune.setToolTipText(tr("Allows to tune the track coloring for different average speeds."));

        colorTypeHeatMapTune.setToolTipText(tr("Selects the color schema for heat map."));
        JLabel colorTypeHeatIconLabel = new JLabel();

        add(Box.createVerticalGlue(), GBC.eol().insets(0, 20, 0, 0));

        add(new JLabel(tr("Track and Point Coloring")), GBC.eol().insets(20, 0, 0, 0));
        if (layerName != null) {
            add(colorTypeGlobal, GBC.eol().insets(40, 0, 0, 0));
        }
        add(colorTypeNone, GBC.eol().insets(40, 0, 0, 0));
        add(colorTypeVelocity, GBC.std().insets(40, 0, 0, 0));
        add(colorTypeVelocityTune, GBC.eop().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));
        add(colorTypeDirection, GBC.eol().insets(40, 0, 0, 0));
        add(colorTypeDilution, GBC.eol().insets(40, 0, 0, 0));
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
                final Color color;
                // ask the GPX draw for the correct color of that layer ( if there is one )
                if (null != layerName) {
                    color = GpxDrawHelper.DEFAULT_COLOR.getChildColor(
                            NamedColorProperty.COLOR_CATEGORY_LAYER, layerName, GpxDrawHelper.DEFAULT_COLOR.getName()).get();
                } else {
                    color = GpxDrawHelper.DEFAULT_COLOR.getDefaultValue();
                }
                colorTypeHeatIconLabel.setIcon(GpxDrawHelper.getColorMapImageIcon(color, colorTypeHeatMapTune.getSelectedIndex(), iconSize));
            }
        });

        ExpertToggleAction.addVisibilitySwitcher(colorTypeDirection);
        ExpertToggleAction.addVisibilitySwitcher(colorTypeDilution);
        ExpertToggleAction.addVisibilitySwitcher(colorTypeHeatMapLowerLimit);
        ExpertToggleAction.addVisibilitySwitcher(colorTypeHeatMapLowerLimitLabel);

        colorDynamic.setToolTipText(tr("Colors points and track segments by data limits."));
        add(colorDynamic, GBC.eop().insets(40, 0, 0, 0));
        ExpertToggleAction.addVisibilitySwitcher(colorDynamic);

        if (layerName == null) {
            // Setting waypoints for gpx layer doesn't make sense - waypoints are shown in marker layer that has different name - so show
            // this only for global config

            // waypointLabel
            label = new JLabel(tr("Waypoint labelling"));
            add(label, GBC.std().insets(20, 0, 0, 0));
            label.setLabelFor(waypointLabel);
            add(waypointLabel, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));
            waypointLabel.addActionListener(e -> updateWaypointPattern(waypointLabel, waypointLabelPattern));
            updateWaypointLabelCombobox(waypointLabel, waypointLabelPattern, TemplateEntryProperty.forMarker(layerName));
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
            updateWaypointLabelCombobox(audioWaypointLabel, audioWaypointLabelPattern, TemplateEntryProperty.forAudioMarker(layerName));
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
        if (layerName != null && Config.getPref().get("draw.rawgps.lines."+layerName).isEmpty()
                && Config.getPref().get("draw.rawgps.lines.local."+layerName).isEmpty()) {
            // no line preferences for layer is found
            drawRawGpsLinesGlobal.setSelected(true);
        } else {
            Boolean lf = PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.lines.local", layerName, true);
            if (PreferencesUtils.getBoolean(Config.getPref(), "draw.rawgps.lines", layerName, true)) {
                drawRawGpsLinesAll.setSelected(true);
            } else if (lf) {
                drawRawGpsLinesLocal.setSelected(true);
            } else {
                drawRawGpsLinesNone.setSelected(true);
            }
        }

        drawRawGpsMaxLineLengthLocal.setText(Integer.toString(PreferencesUtils.getInteger(Config.getPref(),
                "draw.rawgps.max-line-length.local", layerName, -1)));
        drawRawGpsMaxLineLength.setText(Integer.toString(PreferencesUtils.getInteger(Config.getPref(),
                "draw.rawgps.max-line-length", layerName, 200)));
        drawLineWidth.setText(Integer.toString(PreferencesUtils.getInteger(Config.getPref(),
                "draw.rawgps.linewidth", layerName, 0)));
        drawLineWithAlpha.setSelected(PreferencesUtils.getBoolean(Config.getPref(),
                "draw.rawgps.lines.alpha-blend", layerName, false));
        forceRawGpsLines.setSelected(PreferencesUtils.getBoolean(Config.getPref(),
                "draw.rawgps.lines.force", layerName, false));
        drawGpsArrows.setSelected(PreferencesUtils.getBoolean(Config.getPref(),
                "draw.rawgps.direction", layerName, false));
        drawGpsArrowsFast.setSelected(PreferencesUtils.getBoolean(Config.getPref(),
                "draw.rawgps.alternatedirection", layerName, false));
        drawGpsArrowsMinDist.setText(Integer.toString(PreferencesUtils.getInteger(Config.getPref(),
                "draw.rawgps.min-arrow-distance", layerName, 40)));
        hdopCircleGpsPoints.setSelected(PreferencesUtils.getBoolean(Config.getPref(),
                "draw.rawgps.hdopcircle", layerName, false));
        largeGpsPoints.setSelected(PreferencesUtils.getBoolean(Config.getPref(),
                "draw.rawgps.large", layerName, false));
        useGpsAntialiasing.setSelected(Config.getPref().getBoolean("mappaint.gpx.use-antialiasing", false));

        drawRawGpsLinesActionListener.actionPerformed(null);

        if (layerName != null && Config.getPref().get("draw.rawgps.colors."+layerName).isEmpty()) {
            colorTypeGlobal.setSelected(true);
            colorDynamic.setSelected(false);
            colorDynamic.setEnabled(false);
            colorTypeHeatMapPoints.setSelected(false);
            colorTypeHeatMapGain.setValue(0);
            colorTypeHeatMapLowerLimit.setValue(0);
        } else {
            int colorType = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.colors", layerName, 0);
            switch (colorType) {
            case 0: colorTypeNone.setSelected(true); break;
            case 1: colorTypeVelocity.setSelected(true); break;
            case 2: colorTypeDilution.setSelected(true); break;
            case 3: colorTypeDirection.setSelected(true); break;
            case 4: colorTypeTime.setSelected(true); break;
            case 5: colorTypeHeatMap.setSelected(true); break;
            default: Logging.warn("Unknown color type: " + colorType);
            }
            int ccts = PreferencesUtils.getInteger(Config.getPref(), "draw.rawgps.colorTracksTune", layerName, 45);
            colorTypeVelocityTune.setSelectedIndex(ccts == 10 ? 2 : (ccts == 20 ? 1 : 0));
            colorTypeHeatMapTune.setSelectedIndex(PreferencesUtils.getInteger(Config.getPref(),
                    "draw.rawgps.heatmap.colormap", layerName, 0));
            colorDynamic.setSelected(PreferencesUtils.getBoolean(Config.getPref(),
                    "draw.rawgps.colors.dynamic", layerName, false));
            colorTypeHeatMapPoints.setSelected(PreferencesUtils.getBoolean(Config.getPref(),
                    "draw.rawgps.heatmap.use-points", layerName, false));
            colorTypeHeatMapGain.setValue(PreferencesUtils.getInteger(Config.getPref(),
                    "draw.rawgps.heatmap.gain", layerName, 0));
            colorTypeHeatMapLowerLimit.setValue(PreferencesUtils.getInteger(Config.getPref(),
                    "draw.rawgps.heatmap.lower-limit", layerName, 0));
        }
    }

    /**
     * Save preferences from UI controls, globally or for a specified layer.
     * @param layerName The GPX layer name. Can be {@code null}, in that case, global preferences are written
     * @param locLayer {@code true} if the GPX layer is a local one. Ignored if {@code layerName} is null
     * @return {@code true} when restart is required, {@code false} otherwise
     */
    public boolean savePreferences(String layerName, boolean locLayer) {
        String layerNameDot = ".layer "+layerName;
        if (layerName == null) {
            layerNameDot = "";
        }
        Config.getPref().putBoolean("marker.makeautomarkers"+layerNameDot, makeAutoMarkers.isSelected());
        if (drawRawGpsLinesGlobal.isSelected()) {
            Config.getPref().put("draw.rawgps.lines" + layerNameDot, null);
            Config.getPref().put("draw.rawgps.max-line-length" + layerNameDot, null);
            Config.getPref().put("draw.rawgps.lines.local" + layerNameDot, null);
            Config.getPref().put("draw.rawgps.max-line-length.local" + layerNameDot, null);
            Config.getPref().put("draw.rawgps.lines.force"+layerNameDot, null);
            Config.getPref().put("draw.rawgps.direction"+layerNameDot, null);
            Config.getPref().put("draw.rawgps.alternatedirection"+layerNameDot, null);
            Config.getPref().put("draw.rawgps.min-arrow-distance"+layerNameDot, null);
        } else {
            if (layerName == null || !locLayer) {
                Config.getPref().putBoolean("draw.rawgps.lines" + layerNameDot, drawRawGpsLinesAll.isSelected());
                Config.getPref().put("draw.rawgps.max-line-length" + layerNameDot, drawRawGpsMaxLineLength.getText());
            }
            if (layerName == null || locLayer) {
                Config.getPref().putBoolean("draw.rawgps.lines.local" + layerNameDot,
                        drawRawGpsLinesAll.isSelected() || drawRawGpsLinesLocal.isSelected());
                Config.getPref().put("draw.rawgps.max-line-length.local" + layerNameDot,
                        drawRawGpsMaxLineLengthLocal.getText());
            }
            Config.getPref().putBoolean("draw.rawgps.lines.force"+layerNameDot, forceRawGpsLines.isSelected());
            Config.getPref().putBoolean("draw.rawgps.direction"+layerNameDot, drawGpsArrows.isSelected());
            Config.getPref().putBoolean("draw.rawgps.alternatedirection"+layerNameDot, drawGpsArrowsFast.isSelected());
            Config.getPref().put("draw.rawgps.min-arrow-distance"+layerNameDot, drawGpsArrowsMinDist.getText());
        }

        Config.getPref().putBoolean("draw.rawgps.hdopcircle"+layerNameDot, hdopCircleGpsPoints.isSelected());
        Config.getPref().putBoolean("draw.rawgps.large"+layerNameDot, largeGpsPoints.isSelected());
        Config.getPref().put("draw.rawgps.linewidth"+layerNameDot, drawLineWidth.getText());
        Config.getPref().putBoolean("draw.rawgps.lines.alpha-blend"+layerNameDot, drawLineWithAlpha.isSelected());

        Config.getPref().putBoolean("mappaint.gpx.use-antialiasing", useGpsAntialiasing.isSelected());

        TemplateEntryProperty.forMarker(layerName).put(waypointLabelPattern.getText());
        TemplateEntryProperty.forAudioMarker(layerName).put(audioWaypointLabelPattern.getText());

        if (colorTypeGlobal.isSelected()) {
            Config.getPref().put("draw.rawgps.colors"+layerNameDot, null);
            Config.getPref().put("draw.rawgps.colors.dynamic"+layerNameDot, null);
            Config.getPref().put("draw.rawgps.colorTracksTunec"+layerNameDot, null);
            return false;
        } else if (colorTypeVelocity.isSelected()) {
            Config.getPref().putInt("draw.rawgps.colors"+layerNameDot, 1);
        } else if (colorTypeDilution.isSelected()) {
            Config.getPref().putInt("draw.rawgps.colors"+layerNameDot, 2);
        } else if (colorTypeDirection.isSelected()) {
            Config.getPref().putInt("draw.rawgps.colors"+layerNameDot, 3);
        } else if (colorTypeTime.isSelected()) {
            Config.getPref().putInt("draw.rawgps.colors"+layerNameDot, 4);
        } else if (colorTypeHeatMap.isSelected()) {
            Config.getPref().putInt("draw.rawgps.colors"+layerNameDot, 5);
        } else {
            Config.getPref().putInt("draw.rawgps.colors"+layerNameDot, 0);
        }
        Config.getPref().putBoolean("draw.rawgps.colors.dynamic"+layerNameDot, colorDynamic.isSelected());
        int ccti = colorTypeVelocityTune.getSelectedIndex();
        Config.getPref().putInt("draw.rawgps.colorTracksTune"+layerNameDot, ccti == 2 ? 10 : (ccti == 1 ? 20 : 45));
        Config.getPref().putInt("draw.rawgps.heatmap.colormap"+layerNameDot, colorTypeHeatMapTune.getSelectedIndex());
        Config.getPref().putBoolean("draw.rawgps.heatmap.use-points"+layerNameDot, colorTypeHeatMapPoints.isSelected());
        Config.getPref().putInt("draw.rawgps.heatmap.gain"+layerNameDot, colorTypeHeatMapGain.getValue());
        Config.getPref().putInt("draw.rawgps.heatmap.lower-limit"+layerNameDot, colorTypeHeatMapLowerLimit.getValue());

        return false;
    }

    /**
     * Save preferences from UI controls for initial layer or globally
     * @return {@code true} when restart is required, {@code false} otherwise
     */
    public boolean savePreferences() {
        return savePreferences(null, false);
    }

    private static void updateWaypointLabelCombobox(JosmComboBox<String> cb, JosmTextField tf, TemplateEntryProperty property) {
        String labelPattern = property.getAsString();
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
