package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

/*
 * marker.audiosampleminsecs
 * marker.audiosampleminmetres
 * marker.buttonlabels
 * markers.namedtrackpoints
 * audio.forwardbackamount
 * audio.leadin
 * audio.menuinvisible
 * marker.audiotraceVisible
 * audio.toolbar ??
 */

public class AudioPreference implements PreferenceSetting {
	private JCheckBox audioMenuVisible = new JCheckBox(tr("Display the Audio menu."));
	/*
	private JCheckBox audioToolbarVisible = new JCheckBox(tr("Display Audio control buttons on toolbar."));
	*/
	private JCheckBox markerButtonLabels = new JCheckBox(tr("Label audio (and image and web) markers."));
	private JCheckBox markerAudioTraceVisible = new JCheckBox(tr("Display live audio trace."));
	private JCheckBox markersNamedTrackpoints = new JCheckBox(tr("Create audio markers from named trackpoints."));

	private JTextField audioSampleMinSecs = new JTextField(8);
	private JTextField audioSampleMinMetres = new JTextField(8);
	private JTextField audioLeadIn = new JTextField(8);
	private JTextField audioForwardBackAmount = new JTextField(8);
	private JTextField audioFastForwardMultiplier = new JTextField(8);
	private JTextField audioCalibration = new JTextField(8);

	public void addGui(PreferenceDialog gui) {
		// audioMenuVisible
		audioMenuVisible.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!audioMenuVisible.isSelected())
					audioMenuVisible.setSelected(false);
				audioMenuVisible.setEnabled(audioMenuVisible.isSelected());
			}
		});
		audioMenuVisible.setSelected(! Main.pref.getBoolean("audio.menuinvisible"));
		audioMenuVisible.setToolTipText(tr("Show or hide the audio menu entry on the main menu bar."));
		gui.audio.add(audioMenuVisible, GBC.eol().insets(0,0,0,0));

		// audioTraceVisible
		markerAudioTraceVisible.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!markerAudioTraceVisible.isSelected())
					markerAudioTraceVisible.setSelected(false);
				markerAudioTraceVisible.setEnabled(markerAudioTraceVisible.isSelected());
			}
		});
		markerAudioTraceVisible.setSelected(Main.pref.getBoolean("marker.traceaudio", true));
		markerAudioTraceVisible.setToolTipText(tr("Display a moving icon representing the point on the synchronized track where the audio currently playing was recorded."));
		gui.audio.add(markerAudioTraceVisible, GBC.eol().insets(0,0,0,0));
		
		// buttonLabels
		markerButtonLabels.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!markerButtonLabels.isSelected())
					markerButtonLabels.setSelected(false);
				markerButtonLabels.setEnabled(markerButtonLabels.isSelected());
			}
		});
		markerButtonLabels.setSelected(Main.pref.getBoolean("marker.buttonlabels"));
		markerButtonLabels.setToolTipText(tr("Put text labels against audio (and image and web) markers as well as their button icons."));
		gui.audio.add(markerButtonLabels, GBC.eol().insets(0,0,0,0));
		
		// markersNamedTrackpoints
		markersNamedTrackpoints.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!markersNamedTrackpoints.isSelected())
					markersNamedTrackpoints.setSelected(false);
				markersNamedTrackpoints.setEnabled(markersNamedTrackpoints.isSelected());
			}
		});
		markersNamedTrackpoints.setSelected(Main.pref.getBoolean("marker.namedtrackpoints"));
		markersNamedTrackpoints.setToolTipText(tr("Automatically create audio markers from trackpoints (rather than explicit waypoints) with names or descriptions."));
		gui.audio.add(markersNamedTrackpoints, GBC.eol().insets(0,0,0,0));
		
		audioSampleMinSecs.setText(Main.pref.get("marker.audiosampleminsecs", "15"));
		audioSampleMinSecs.setToolTipText(tr("Minimum time in seconds between audio samples when creating sampled audio markers from waypoints"));
		gui.audio.add(new JLabel(tr("Min audio marker sample rate (seconds)")), GBC.std());
		gui.audio.add(audioSampleMinSecs, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

		audioSampleMinMetres.setText(Main.pref.get("marker.audiosampleminmetres", "75"));
		audioSampleMinMetres.setToolTipText(tr("Minimum distance in metres between audio samples when creating sampled audio markers from waypoints"));
		gui.audio.add(new JLabel(tr("Min audio marker sample rate (metres)")), GBC.std());
		gui.audio.add(audioSampleMinMetres, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

		audioForwardBackAmount.setText(Main.pref.get("audio.forwardbackamount", "10"));
		audioForwardBackAmount.setToolTipText(tr("The number of seconds to jump forward or back when the relevant button is pressed"));
		gui.audio.add(new JLabel(tr("Forward/back time (seconds)")), GBC.std());
		gui.audio.add(audioForwardBackAmount, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

		audioFastForwardMultiplier.setText(Main.pref.get("audio.fastfwdmultiplier", "1.3"));
		audioFastForwardMultiplier.setToolTipText(tr("The amount by which the speed is multiplied for fast forwarding"));
		gui.audio.add(new JLabel(tr("Fast forward multiplier")), GBC.std());
		gui.audio.add(audioFastForwardMultiplier, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

		audioLeadIn.setText(Main.pref.get("audio.leadin", "1"));
		audioLeadIn.setToolTipText(tr("Playback starts this number of seconds before (or after, if negative) the audio track position requested"));
		gui.audio.add(new JLabel(tr("Lead-in time (seconds)")), GBC.std());
		gui.audio.add(audioLeadIn, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

		audioCalibration.setText(Main.pref.get("audio.calibration", "1.0"));
		audioCalibration.setToolTipText(tr("The ratio of voice recorder elapsed time to true elapsed time"));
		gui.audio.add(new JLabel(tr("Voice recorder calibration")), GBC.std());
		gui.audio.add(audioCalibration, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));

		gui.audio.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));
	}

	public void ok() {
		Main.pref.put("audio.menuinvisible", ! audioMenuVisible.isSelected());
		Main.pref.put("marker.traceaudio", markerAudioTraceVisible.isSelected());
		Main.pref.put("marker.buttonlabels", markerButtonLabels.isSelected());
		Main.pref.put("marker.namedtrackpoints", markersNamedTrackpoints.isSelected());
		Main.pref.put("marker.audiosampleminsecs", audioSampleMinSecs.getText());		
		Main.pref.put("marker.audiosampleminmetres", audioSampleMinMetres.getText());		
		Main.pref.put("audio.forwardbackamount", audioForwardBackAmount.getText());		
		Main.pref.put("audio.fastfwdmultiplier", audioFastForwardMultiplier.getText());		
		Main.pref.put("audio.leadin", audioLeadIn.getText());		
		Main.pref.put("audio.calibration", audioCalibration.getText());		
    }
}
