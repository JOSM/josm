// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.Box;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class DrawingPreference implements PreferenceSetting {

    private JCheckBox drawRawGpsLines = new JCheckBox(tr("Draw lines between raw gps points."));
    private JTextField drawRawGpsMaxLineLength = new JTextField(8);
    private JCheckBox forceRawGpsLines = new JCheckBox(tr("Force lines if no segments imported."));
    private JCheckBox largeGpsPoints = new JCheckBox(tr("Draw large GPS points."));
    private JCheckBox colorTracks = new JCheckBox(tr("Color tracks by velocity."));
    private JComboBox colorTracksTune = new JComboBox(new String[] {tr("Car"), tr("Bicycle"), tr("Foot")});
    private JCheckBox directionHint = new JCheckBox(tr("Draw Direction Arrows"));
    private JCheckBox drawGpsArrows = new JCheckBox(tr("Draw Direction Arrows"));
    private JCheckBox drawGpsArrowsFast = new JCheckBox(tr("Fast drawing (looks uglier)"));
    private JTextField drawGpsArrowsMinDist = new JTextField(8);
    private JCheckBox interestingDirections = new JCheckBox(tr("Only interesting direction hints (e.g. with oneway tag)."));
    private JCheckBox segmentOrderNumber = new JCheckBox(tr("Draw segment order numbers"));
    private JCheckBox sourceBounds = new JCheckBox(tr("Draw boundaries of downloaded data"));
    private JCheckBox virtualNodes = new JCheckBox(tr("Draw virtual nodes in select mode"));
    private JCheckBox inactive = new JCheckBox(tr("Draw inactive layers in other color"));
    private JCheckBox useAntialiasing = new JCheckBox(tr("Smooth map graphics (antialiasing)"));

    public void addGui(PreferenceDialog gui) {
        gui.display.setPreferredSize(new Dimension(400,600));
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // drawRawGpsLines
        drawRawGpsLines.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                            forceRawGpsLines.setEnabled(drawRawGpsLines.isSelected());
                            drawRawGpsMaxLineLength.setEnabled(drawRawGpsLines.isSelected());
                            drawGpsArrows.setEnabled(drawRawGpsLines.isSelected());
                            drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
                            drawGpsArrowsMinDist.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
                            colorTracks.setEnabled(drawRawGpsLines.isSelected());
                            colorTracksTune.setEnabled(colorTracks.isSelected() && drawRawGpsLines.isSelected());
            }
        });
        drawRawGpsLines.setSelected(Main.pref.getBoolean("draw.rawgps.lines"));
        drawRawGpsLines.setToolTipText(tr("If your gps device draws too few lines, select this to draw lines along your way."));
        panel.add(drawRawGpsLines, GBC.eol().insets(20,0,0,0));

        // drawRawGpsMaxLineLength
        drawRawGpsMaxLineLength.setText(Integer.toString(Main.pref.getInteger("draw.rawgps.max-line-length", -1)));
        drawRawGpsMaxLineLength.setToolTipText(tr("Maximum length (in meters) to draw lines. Set to '-1' to draw all lines."));
        drawRawGpsMaxLineLength.setEnabled(drawRawGpsLines.isSelected());
        panel.add(new JLabel(tr("Maximum length (meters)")), GBC.std().insets(40,0,0,0));
        panel.add(drawRawGpsMaxLineLength, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

        // forceRawGpsLines
        forceRawGpsLines.setToolTipText(tr("Force drawing of lines if the imported data contain no line information."));
        forceRawGpsLines.setSelected(Main.pref.getBoolean("draw.rawgps.lines.force"));
        forceRawGpsLines.setEnabled(drawRawGpsLines.isSelected());
        panel.add(forceRawGpsLines, GBC.eop().insets(40,0,0,0));

        // drawGpsArrows
        drawGpsArrows.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                            drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
                            drawGpsArrowsMinDist.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
            }
        });
        drawGpsArrows.setToolTipText(tr("Draw direction arrows for lines, connecting GPS points."));
        drawGpsArrows.setSelected(Main.pref.getBoolean("draw.rawgps.direction"));
        drawGpsArrows.setEnabled(drawRawGpsLines.isSelected());
        panel.add(drawGpsArrows, GBC.eop().insets(40,0,0,0));

        // drawGpsArrowsFast
        drawGpsArrowsFast.setToolTipText(tr("Draw the direction arrows using table lookups instead of complex math."));
        drawGpsArrowsFast.setSelected(Main.pref.getBoolean("draw.rawgps.alternatedirection"));
        drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
        panel.add(drawGpsArrowsFast, GBC.eop().insets(60,0,0,0));

        // drawGpsArrowsMinDist
        drawGpsArrowsMinDist.setToolTipText(tr("Don't draw arrows if they are not at least this distance away from the last one."));
        drawGpsArrowsMinDist.setText(Integer.toString(Main.pref.getInteger("draw.rawgps.min-arrow-distance", 0)));
        drawGpsArrowsMinDist.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
        panel.add(new JLabel(tr("Minimum distance (pixels)")), GBC.std().insets(60,0,0,0));
        panel.add(drawGpsArrowsMinDist, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

        // colorTracks
        colorTracks.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                            colorTracksTune.setEnabled(colorTracks.isSelected() && drawRawGpsLines.isSelected());
            }
        });
        colorTracks.setSelected(Main.pref.getBoolean("draw.rawgps.colors"));
        colorTracks.setToolTipText(tr("Choose the hue for the track color by the velocity at that point."));
        colorTracks.setEnabled(drawRawGpsLines.isSelected());
        panel.add(colorTracks, GBC.std().insets(40,0,0,0));
        
        // color Tracks by Velocity Tune
        int ccts = Main.pref.getInteger("draw.rawgps.colorTracksTune", 45);
        colorTracksTune.setSelectedIndex(ccts==10 ? 2 : (ccts==20 ? 1 : 0));
        colorTracksTune.setToolTipText(tr("Allows to tune the track coloring for different average speeds."));
        colorTracksTune.setEnabled(colorTracks.isSelected() && colorTracks.isEnabled());
        panel.add(colorTracksTune, GBC.eop().insets(5,0,0,5));
        
        // largeGpsPoints
        largeGpsPoints.setSelected(Main.pref.getBoolean("draw.rawgps.large"));
        largeGpsPoints.setToolTipText(tr("Draw larger dots for the GPS points."));
        panel.add(largeGpsPoints, GBC.eop().insets(20,0,0,0));

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
                            }else{
                                interestingDirections.setSelected(false);
                            }
                            interestingDirections.setEnabled(directionHint.isSelected());
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
        Main.pref.put("draw.rawgps.lines", drawRawGpsLines.isSelected());
        Main.pref.put("draw.rawgps.max-line-length", drawRawGpsMaxLineLength.getText());
        Main.pref.put("draw.rawgps.lines.force", forceRawGpsLines.isSelected());
        Main.pref.put("draw.rawgps.direction", drawGpsArrows.isSelected());
        Main.pref.put("draw.rawgps.alternatedirection", drawGpsArrowsFast.isSelected());
        Main.pref.put("draw.rawgps.min-arrow-distance", drawGpsArrowsMinDist.getText());
        Main.pref.put("draw.rawgps.colors", colorTracks.isSelected());
        int ccti=colorTracksTune.getSelectedIndex();
        Main.pref.putInteger("draw.rawgps.colorTracksTune", ccti==2 ? 10 : (ccti==1 ? 20 : 45));
        Main.pref.put("draw.rawgps.large", largeGpsPoints.isSelected());
        Main.pref.put("draw.segment.direction", directionHint.isSelected());
        Main.pref.put("draw.segment.relevant_directions_only", interestingDirections.isSelected());
        Main.pref.put("draw.segment.order_number", segmentOrderNumber.isSelected());
        Main.pref.put("draw.data.downloaded_area", sourceBounds.isSelected());
        Main.pref.put("draw.data.inactive_color", inactive.isSelected());
        Main.pref.put("mappaint.use-antialiasing", useAntialiasing.isSelected());
        int vn = Main.pref.getInteger("mappaint.node.virtual-size", 8);
        if(virtualNodes.isSelected()) { if (vn < 1) vn = 8; }
        else { vn = 0; }
        Main.pref.putInteger("mappaint.node.virtual-size", vn);
        return false;
    }
}
