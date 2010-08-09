// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class DrawingPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new DrawingPreference();
        }
    }

    private JRadioButton drawRawGpsLinesAll = new JRadioButton(tr("All"));
    private JRadioButton drawRawGpsLinesLocal = new JRadioButton(tr("Local files"));
    private JRadioButton drawRawGpsLinesNone = new JRadioButton(tr("None"));
    private ActionListener drawRawGpsLinesActionListener;
    private JTextField drawRawGpsMaxLineLength = new JTextField(8);
    private JTextField drawRawGpsMaxLineLengthLocal = new JTextField(8);
    private JCheckBox forceRawGpsLines = new JCheckBox(tr("Force lines if no segments imported."));
    private JCheckBox largeGpsPoints = new JCheckBox(tr("Draw large GPS points."));
    private JCheckBox hdopCircleGpsPoints = new JCheckBox(tr("Draw a circle form HDOP value."));
    private ButtonGroup colorGroup;
    private JRadioButton colorTypeVelocity = new JRadioButton(tr("Velocity (red = slow, green = fast)"));
    private JRadioButton colorTypeDilution = new JRadioButton(tr("Dilution of Position (red = high, green = low, if available)"));
    private JRadioButton colorTypeNone = new JRadioButton(tr("Single Color (can be customized for named layers)"));
    private JComboBox colorTypeVelocityTune = new JComboBox(new String[] {tr("Car"), tr("Bicycle"), tr("Foot")});
    private JCheckBox directionHint = new JCheckBox(tr("Draw Direction Arrows"));
    private JCheckBox drawGpsArrows = new JCheckBox(tr("Draw Direction Arrows"));
    private JCheckBox drawGpsArrowsFast = new JCheckBox(tr("Fast drawing (looks uglier)"));
    private JTextField drawGpsArrowsMinDist = new JTextField(8);
    private JCheckBox interestingDirections = new JCheckBox(tr("Only interesting direction hints (e.g. with oneway tag)."));
    private JCheckBox headArrow = new JCheckBox(tr("Only on the head of a way."));
    private JCheckBox segmentOrderNumber = new JCheckBox(tr("Draw segment order numbers"));
    private JCheckBox sourceBounds = new JCheckBox(tr("Draw boundaries of downloaded data"));
    private JCheckBox virtualNodes = new JCheckBox(tr("Draw virtual nodes in select mode"));
    private JCheckBox inactive = new JCheckBox(tr("Draw inactive layers in other color"));
    private JCheckBox useAntialiasing = new JCheckBox(tr("Smooth map graphics (antialiasing)"));
    private JCheckBox makeAutoMarkers = new JCheckBox(tr("Create markers when reading GPX."));
    private JComboBox waypointLabel = new JComboBox(new String[] {tr("Auto"), /* gpx data field name */ trc("gpx_field", "Name"),
                                      /* gpx data field name */ trc("gpx_field", "Desc(ription)"), tr("Both"), tr("None")});

    public void addGui(PreferenceTabbedPane gui) {
        gui.display.setPreferredSize(new Dimension(400,600));
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // makeAutoMarkers
        makeAutoMarkers.setSelected(Main.pref.getBoolean("marker.makeautomarkers", true));
        makeAutoMarkers.setToolTipText(tr("Automatically make a marker layer from any waypoints when opening a GPX layer."));
        panel.add(makeAutoMarkers, GBC.eol().insets(20,0,0,5));

        // drawRawGpsLines
        ButtonGroup gpsLinesGroup = new ButtonGroup();
        gpsLinesGroup.add(drawRawGpsLinesNone);
        gpsLinesGroup.add(drawRawGpsLinesLocal);
        gpsLinesGroup.add(drawRawGpsLinesAll);


        /* ensure that default is in data base */
        Boolean lf = Main.pref.getBoolean("draw.rawgps.lines.localfiles", false);
        if(Main.pref.getBoolean("draw.rawgps.lines", true)) {
            drawRawGpsLinesAll.setSelected(true);
        } else if (lf) {
            drawRawGpsLinesLocal.setSelected(true);
        } else {
            drawRawGpsLinesNone.setSelected(true);
        }

        panel.add(new JLabel(tr("Draw lines between raw GPS points")), GBC.eol().insets(20,0,0,0));
        panel.add(drawRawGpsLinesNone, GBC.eol().insets(40,0,0,0));
        panel.add(drawRawGpsLinesLocal, GBC.eol().insets(40,0,0,0));
        panel.add(drawRawGpsLinesAll, GBC.eol().insets(40,0,0,0));

        drawRawGpsLinesActionListener = new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                forceRawGpsLines.setEnabled(!drawRawGpsLinesNone.isSelected());
                drawRawGpsMaxLineLength.setEnabled(!(drawRawGpsLinesNone.isSelected() || drawRawGpsLinesLocal.isSelected()));
                drawRawGpsMaxLineLengthLocal.setEnabled(!drawRawGpsLinesNone.isSelected());
                drawGpsArrows.setEnabled(!drawRawGpsLinesNone.isSelected() );
                drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
                drawGpsArrowsMinDist.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
            }
        };

        drawRawGpsLinesNone.addActionListener(drawRawGpsLinesActionListener);
        drawRawGpsLinesLocal.addActionListener(drawRawGpsLinesActionListener);
        drawRawGpsLinesAll.addActionListener(drawRawGpsLinesActionListener);

        // drawRawGpsMaxLineLengthLocal
        drawRawGpsMaxLineLengthLocal.setText(Integer.toString(Main.pref.getInteger("draw.rawgps.max-line-length.local", -1)));
        drawRawGpsMaxLineLengthLocal.setToolTipText(tr("Maximum length (in meters) to draw lines for local files. Set to ''-1'' to draw all lines."));
        drawRawGpsMaxLineLengthLocal.setEnabled(!drawRawGpsLinesNone.isSelected());
        panel.add(new JLabel(tr("Maximum length for local files (meters)")), GBC.std().insets(40,0,0,0));
        panel.add(drawRawGpsMaxLineLengthLocal, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

        // drawRawGpsMaxLineLength
        drawRawGpsMaxLineLength.setText(Integer.toString(Main.pref.getInteger("draw.rawgps.max-line-length", 200)));
        drawRawGpsMaxLineLength.setToolTipText(tr("Maximum length (in meters) to draw lines. Set to ''-1'' to draw all lines."));
        drawRawGpsMaxLineLength.setEnabled(!drawRawGpsLinesNone.isSelected());
        panel.add(new JLabel(tr("Maximum length (meters)")), GBC.std().insets(40,0,0,0));
        panel.add(drawRawGpsMaxLineLength, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

        // forceRawGpsLines
        forceRawGpsLines.setToolTipText(tr("Force drawing of lines if the imported data contain no line information."));
        forceRawGpsLines.setSelected(Main.pref.getBoolean("draw.rawgps.lines.force", false));
        forceRawGpsLines.setEnabled(!drawRawGpsLinesNone.isSelected());
        panel.add(forceRawGpsLines, GBC.eop().insets(40,0,0,0));

        // drawGpsArrows
        drawGpsArrows.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
                drawGpsArrowsMinDist.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
            }
        });
        drawGpsArrows.setToolTipText(tr("Draw direction arrows for lines, connecting GPS points."));
        drawGpsArrows.setSelected(Main.pref.getBoolean("draw.rawgps.direction", false));
        drawGpsArrows.setEnabled(!drawRawGpsLinesNone.isSelected());
        panel.add(drawGpsArrows, GBC.eop().insets(40,0,0,0));

        // drawGpsArrowsFast
        drawGpsArrowsFast.setToolTipText(tr("Draw the direction arrows using table lookups instead of complex math."));
        drawGpsArrowsFast.setSelected(Main.pref.getBoolean("draw.rawgps.alternatedirection", false));
        drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
        panel.add(drawGpsArrowsFast, GBC.eop().insets(60,0,0,0));

        // drawGpsArrowsMinDist
        drawGpsArrowsMinDist.setToolTipText(tr("Do not draw arrows if they are not at least this distance away from the last one."));
        drawGpsArrowsMinDist.setText(Integer.toString(Main.pref.getInteger("draw.rawgps.min-arrow-distance", 0)));
        drawGpsArrowsMinDist.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
        panel.add(new JLabel(tr("Minimum distance (pixels)")), GBC.std().insets(60,0,0,0));
        panel.add(drawGpsArrowsMinDist, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

        // hdopCircleGpsPoints
        hdopCircleGpsPoints.setSelected(Main.pref.getBoolean("draw.rawgps.hdopcircle", true));
        hdopCircleGpsPoints.setToolTipText(tr("Draw a circle form HDOP value."));
        panel.add(hdopCircleGpsPoints, GBC.eop().insets(20,0,0,0));

        // largeGpsPoints
        largeGpsPoints.setSelected(Main.pref.getBoolean("draw.rawgps.large", false));
        largeGpsPoints.setToolTipText(tr("Draw larger dots for the GPS points."));
        panel.add(largeGpsPoints, GBC.eop().insets(20,0,0,0));

        // colorTracks
        colorGroup = new ButtonGroup();
        colorGroup.add(colorTypeNone);
        colorGroup.add(colorTypeVelocity);
        colorGroup.add(colorTypeDilution);

        colorTypeVelocity.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e) {
                colorTypeVelocityTune.setEnabled(colorTypeVelocity.isSelected());
            }
        });

        switch(Main.pref.getInteger("draw.rawgps.colors", 0)) {
        case 0:
            colorTypeNone.setSelected(true);
            break;
        case 1:
            colorTypeVelocity.setSelected(true);
            break;
        case 2:
            colorTypeDilution.setSelected(true);
            break;
        }

        colorTypeNone.setToolTipText(tr("All points and track segments will have the same color. Can be customized in Layer Manager."));
        colorTypeVelocity.setToolTipText(tr("Colors points and track segments by velocity."));
        colorTypeDilution.setToolTipText(tr("Colors points and track segments by dilution of position (HDOP). Your capture device needs to log that information."));

        // color Tracks by Velocity Tune
        int ccts = Main.pref.getInteger("draw.rawgps.colorTracksTune", 45);
        colorTypeVelocityTune.setSelectedIndex(ccts==10 ? 2 : (ccts==20 ? 1 : 0));
        colorTypeVelocityTune.setToolTipText(tr("Allows to tune the track coloring for different average speeds."));
        colorTypeVelocityTune.setEnabled(colorTypeVelocity.isSelected() && colorTypeVelocity.isEnabled());

        panel.add(Box.createVerticalGlue(), GBC.eol().insets(0, 20, 0, 0));

        panel.add(new JLabel(tr("Track and Point Coloring")), GBC.eol().insets(20,0,0,0));
        panel.add(colorTypeNone, GBC.eol().insets(40,0,0,0));
        panel.add(colorTypeVelocity, GBC.std().insets(40,0,0,0));
        panel.add(colorTypeVelocityTune, GBC.eop().insets(5,0,0,5));
        panel.add(colorTypeDilution, GBC.eol().insets(40,0,0,0));

        // waypointLabel
        panel.add(Box.createVerticalGlue(), GBC.eol().insets(0, 20, 0, 0));

        waypointLabel.setSelectedIndex(Main.pref.getInteger("draw.rawgps.layer.wpt", 0 ));
        colorTypeDilution.setToolTipText(tr("Allows to change the labelling of track waypoints."));
        panel.add(new JLabel(tr("Waypoint labelling")), GBC.std().insets(20,0,0,0));
        panel.add(waypointLabel, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

        panel.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
        JScrollPane scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        gui.displaycontent.addTab(tr("GPS Points"), scrollpane);
        panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // directionHint
        directionHint.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                if (directionHint.isSelected()){
                    interestingDirections.setSelected(Main.pref.getBoolean("draw.segment.relevant_directions_only", true));
                    headArrow.setSelected(Main.pref.getBoolean("draw.segment.head_only", false));
                }else{
                    interestingDirections.setSelected(false);
                    headArrow.setSelected(false);
                }
                interestingDirections.setEnabled(directionHint.isSelected());
                headArrow.setEnabled(directionHint.isSelected());
            }
        });
        directionHint.setToolTipText(tr("Draw direction hints for way segments."));
        directionHint.setSelected(Main.pref.getBoolean("draw.segment.direction", true));
        panel.add(directionHint, GBC.eop().insets(20,0,0,0));

        // only interesting directions
        interestingDirections.setToolTipText(tr("Only interesting direction hints (e.g. with oneway tag)."));
        interestingDirections.setSelected(Main.pref.getBoolean("draw.segment.relevant_directions_only", true));
        interestingDirections.setEnabled(directionHint.isSelected());
        panel.add(interestingDirections, GBC.eop().insets(40,0,0,0));

        // only on the head of a way
        headArrow.setToolTipText(tr("Only on the head of a way."));
        headArrow.setSelected(Main.pref.getBoolean("draw.segment.head_only", false));
        headArrow.setEnabled(directionHint.isSelected());
        panel.add(headArrow, GBC.eop().insets(40, 0, 0, 0));

        // segment order number
        segmentOrderNumber.setToolTipText(tr("Draw the order numbers of all segments within their way."));
        segmentOrderNumber.setSelected(Main.pref.getBoolean("draw.segment.order_number", false));
        panel.add(segmentOrderNumber, GBC.eop().insets(20,0,0,0));

        // antialiasing
        useAntialiasing.setToolTipText(tr("Apply antialiasing to the map view resulting in a smoother appearance."));
        useAntialiasing.setSelected(Main.pref.getBoolean("mappaint.use-antialiasing", false));
        panel.add(useAntialiasing, GBC.eop().insets(20,0,0,0));

        // downloaded area
        sourceBounds.setToolTipText(tr("Draw the boundaries of data loaded from the server."));
        sourceBounds.setSelected(Main.pref.getBoolean("draw.data.downloaded_area", true));
        panel.add(sourceBounds, GBC.eop().insets(20,0,0,0));

        // virtual nodes
        virtualNodes.setToolTipText(tr("Draw virtual nodes in select mode for easy way modification."));
        virtualNodes.setSelected(Main.pref.getInteger("mappaint.node.virtual-size", 8) != 0);
        panel.add(virtualNodes, GBC.eop().insets(20,0,0,0));

        // background layers in inactive color
        inactive.setToolTipText(tr("Draw the inactive data layers in a different color."));
        inactive.setSelected(Main.pref.getBoolean("draw.data.inactive_color", true));
        panel.add(inactive, GBC.eop().insets(20,0,0,0));

        panel.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
        scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        gui.displaycontent.addTab(tr("OSM Data"), scrollpane);
    }

    public boolean ok() {
        Main.pref.put("marker.makeautomarkers", makeAutoMarkers.isSelected());
        Main.pref.put("draw.rawgps.lines", drawRawGpsLinesAll.isSelected());
        Main.pref.put("draw.rawgps.lines.localfiles", drawRawGpsLinesLocal.isSelected());
        Main.pref.put("draw.rawgps.max-line-length", drawRawGpsMaxLineLength.getText());
        Main.pref.put("draw.rawgps.max-line-length.local", drawRawGpsMaxLineLengthLocal.getText());
        Main.pref.put("draw.rawgps.lines.force", forceRawGpsLines.isSelected());
        Main.pref.put("draw.rawgps.direction", drawGpsArrows.isSelected());
        Main.pref.put("draw.rawgps.alternatedirection", drawGpsArrowsFast.isSelected());
        Main.pref.put("draw.rawgps.min-arrow-distance", drawGpsArrowsMinDist.getText());
        if(colorTypeVelocity.isSelected()) {
            Main.pref.putInteger("draw.rawgps.colors", 1);
        } else if(colorTypeDilution.isSelected()) {
            Main.pref.putInteger("draw.rawgps.colors", 2);
        } else {
            Main.pref.putInteger("draw.rawgps.colors", 0);
        }
        int ccti=colorTypeVelocityTune.getSelectedIndex();
        Main.pref.putInteger("draw.rawgps.colorTracksTune", ccti==2 ? 10 : (ccti==1 ? 20 : 45));
        Main.pref.put("draw.rawgps.hdopcircle", hdopCircleGpsPoints.isSelected());
        Main.pref.put("draw.rawgps.large", largeGpsPoints.isSelected());
        Main.pref.put("draw.segment.direction", directionHint.isSelected());
        Main.pref.put("draw.segment.relevant_directions_only", interestingDirections.isSelected());
        Main.pref.put("draw.segment.head_only", headArrow.isSelected());
        Main.pref.put("draw.segment.order_number", segmentOrderNumber.isSelected());
        Main.pref.put("draw.data.downloaded_area", sourceBounds.isSelected());
        Main.pref.put("draw.data.inactive_color", inactive.isSelected());
        Main.pref.put("mappaint.use-antialiasing", useAntialiasing.isSelected());
        int vn = Main.pref.getInteger("mappaint.node.virtual-size", 8);
        if (virtualNodes.isSelected()) {
            if (vn < 1) {
                vn = 8;
            }
        }
        else {
            vn = 0;
        }
        Main.pref.putInteger("mappaint.node.virtual-size", vn);
        Main.pref.putInteger("draw.rawgps.layer.wpt", waypointLabel.getSelectedIndex());
        return false;
    }
}
