// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class DrawingPreference implements PreferenceSetting {

	private JCheckBox drawRawGpsLines = new JCheckBox(tr("Draw lines between raw gps points."));
	private JTextField drawRawGpsMaxLineLength = new JTextField(8);
	private JCheckBox forceRawGpsLines = new JCheckBox(tr("Force lines if no segments imported."));
	private JCheckBox largeGpsPoints = new JCheckBox(tr("Draw large GPS points."));
	private JCheckBox colorTracks = new JCheckBox(tr("Color tracks by velocity."));
	private JCheckBox directionHint = new JCheckBox(tr("Draw Direction Arrows"));
	private JCheckBox drawGpsArrows = new JCheckBox(tr("Draw Direction Arrows"));
	private JCheckBox drawGpsArrowsFast = new JCheckBox(tr("Fast drawing (looks uglier)"));
	private JCheckBox interestingDirections = new JCheckBox(tr("Only interesting direction hints (e.g. with oneway tag)."));
	private JCheckBox segmentOrderNumber = new JCheckBox(tr("Draw segment order numbers"));
	private JCheckBox sourceBounds = new JCheckBox(tr("Draw boundaries of downloaded data"));
	private JCheckBox inactive = new JCheckBox(tr("Draw inactive layers in other color"));
	private JCheckBox useAntialiasing = new JCheckBox(tr("Smooth map graphics (antialiasing)"));

	public void addGui(PreferenceDialog gui) {
		// drawRawGpsLines
		drawRawGpsLines.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
                            forceRawGpsLines.setEnabled(drawRawGpsLines.isSelected());
                            drawRawGpsMaxLineLength.setEnabled(drawRawGpsLines.isSelected());
                            drawGpsArrows.setEnabled(drawRawGpsLines.isSelected());
                            drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
                            colorTracks.setEnabled(drawRawGpsLines.isSelected());
			}
		});
		drawRawGpsLines.setSelected(Main.pref.getBoolean("draw.rawgps.lines"));
		drawRawGpsLines.setToolTipText(tr("If your gps device draw too few lines, select this to draw lines along your way."));
		gui.display.add(drawRawGpsLines, GBC.eol().insets(20,0,0,0));

		// drawRawGpsMaxLineLength
		drawRawGpsMaxLineLength.setText(Main.pref.get("draw.rawgps.max-line-length", "-1"));
		drawRawGpsMaxLineLength.setToolTipText(tr("Maximum length (in meters) to draw lines. Set to '-1' to draw all lines."));
		drawRawGpsMaxLineLength.setEnabled(drawRawGpsLines.isSelected());
		gui.display.add(new JLabel(tr("Maximum length (meters)")), GBC.std().insets(40,0,0,0));
		gui.display.add(drawRawGpsMaxLineLength, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

		// forceRawGpsLines
		forceRawGpsLines.setToolTipText(tr("Force drawing of lines if the imported data contain no line information."));
		forceRawGpsLines.setSelected(Main.pref.getBoolean("draw.rawgps.lines.force"));
		forceRawGpsLines.setEnabled(drawRawGpsLines.isSelected());
		gui.display.add(forceRawGpsLines, GBC.eop().insets(40,0,0,0));
		
		// drawGpsArrows
		drawGpsArrows.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
                            drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
			}
		});
		drawGpsArrows.setToolTipText(tr("Draw direction arrows for lines, connecting GPS points."));
		drawGpsArrows.setSelected(Main.pref.getBoolean("draw.rawgps.direction"));
		drawGpsArrows.setEnabled(drawRawGpsLines.isSelected());
		gui.display.add(drawGpsArrows, GBC.eop().insets(40,0,0,0));

		// drawGpsArrowsFast
		drawGpsArrowsFast.setToolTipText(tr("Draw the direction arrows using table lookups instead of complex math."));
		drawGpsArrowsFast.setSelected(Main.pref.getBoolean("draw.rawgps.alternatedirection"));
		drawGpsArrowsFast.setEnabled(drawGpsArrows.isSelected() && drawGpsArrows.isEnabled());
		gui.display.add(drawGpsArrowsFast, GBC.eop().insets(60,0,0,0));

		// colorTracks
		colorTracks.setSelected(Main.pref.getBoolean("draw.rawgps.colors"));
		colorTracks.setToolTipText(tr("Choose the hue for the track color by the velocity at that point."));
		colorTracks.setEnabled(drawRawGpsLines.isSelected());
		gui.display.add(colorTracks, GBC.eop().insets(40,0,0,0));
		
		// largeGpsPoints
		largeGpsPoints.setSelected(Main.pref.getBoolean("draw.rawgps.large"));
		largeGpsPoints.setToolTipText(tr("Draw larger dots for the GPS points."));
		gui.display.add(largeGpsPoints, GBC.eop().insets(20,0,0,0));
		
		// directionHint
		directionHint.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
                            if (directionHint.isSelected()){
                                interestingDirections.setSelected(Main.pref.getBoolean("draw.segment.relevant_directions_only"));
                            }else{
                                interestingDirections.setSelected(false);
                            }
                            interestingDirections.setEnabled(directionHint.isSelected());
			}
		});
		directionHint.setToolTipText(tr("Draw direction hints for way segments."));
		directionHint.setSelected(Main.pref.getBoolean("draw.segment.direction"));
		gui.display.add(directionHint, GBC.eop().insets(20,0,0,0));

		// only interesting directions
		interestingDirections.setToolTipText(tr("Only interesting direction hints (e.g. with oneway tag)."));
		interestingDirections.setSelected(Main.pref.getBoolean("draw.segment.relevant_directions_only"));
		interestingDirections.setEnabled(directionHint.isSelected());
		gui.display.add(interestingDirections, GBC.eop().insets(40,0,0,0));
		
		// segment order number
		segmentOrderNumber.setToolTipText(tr("Draw the order numbers of all segments within their way."));
		segmentOrderNumber.setSelected(Main.pref.getBoolean("draw.segment.order_number"));
		gui.display.add(segmentOrderNumber, GBC.eop().insets(20,0,0,0));
		
		// antialiasing
		useAntialiasing.setToolTipText(tr("Apply antialiasing to the map view resulting in a smoother appearance."));
		useAntialiasing.setSelected(Main.pref.getBoolean("mappaint.use-antialiasing"));
		gui.display.add(useAntialiasing, GBC.eop().insets(20,0,0,0));
		
		// downloaded area
		sourceBounds.setToolTipText(tr("Draw the boundaries of data loaded from the server."));
		sourceBounds.setSelected(Main.pref.getBoolean("draw.data.downloaded_area", true));
		gui.display.add(sourceBounds, GBC.eop().insets(20,0,0,0));
		
		// background layers in inactive color
		inactive.setToolTipText(tr("Draw the inactive data layers in a different color."));
		inactive.setSelected(Main.pref.getBoolean("draw.data.inactive_color", true));
		gui.display.add(inactive, GBC.eop().insets(20,0,0,0));
	}

	public void ok() {
		Main.pref.put("draw.rawgps.lines", drawRawGpsLines.isSelected());
		Main.pref.put("draw.rawgps.max-line-length", drawRawGpsMaxLineLength.getText());
		Main.pref.put("draw.rawgps.lines.force", forceRawGpsLines.isSelected());
		Main.pref.put("draw.rawgps.direction", drawGpsArrows.isSelected());
		Main.pref.put("draw.rawgps.alternatedirection", drawGpsArrowsFast.isSelected());
		Main.pref.put("draw.rawgps.colors", colorTracks.isSelected());
		Main.pref.put("draw.rawgps.large", largeGpsPoints.isSelected());
		Main.pref.put("draw.segment.direction", directionHint.isSelected());
		Main.pref.put("draw.segment.relevant_directions_only", interestingDirections.isSelected());
		Main.pref.put("draw.segment.order_number", segmentOrderNumber.isSelected());
		Main.pref.put("draw.data.downloaded_area", sourceBounds.isSelected());
		Main.pref.put("draw.data.inactive_color", inactive.isSelected());
		Main.pref.put("mappaint.use-antialiasing", useAntialiasing.isSelected());
    }
}
