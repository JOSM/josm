// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.audio;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

/**
 * Audio preferences.
 */
public final class AudioPreference extends DefaultTabPreferenceSetting {

    /**
     * Factory used to create a new {@code AudioPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new AudioPreference();
        }
    }

    private AudioPreference() {
        super(/* ICON(preferences/) */ "audio", tr("Audio Settings"), tr("Settings for the audio player and audio markers."));
    }

    private final JCheckBox audioMenuVisible = new JCheckBox(tr("Display the Audio menu."));
    private final JCheckBox markerButtonLabels = new JCheckBox(tr("Label audio (and image and web) markers."));
    private final JCheckBox markerAudioTraceVisible = new JCheckBox(tr("Display live audio trace."));

    // various methods of making markers on import audio
    private final JCheckBox audioMarkersFromExplicitWaypoints = new JCheckBox(tr("Explicit waypoints with valid timestamps."));
    private final JCheckBox audioMarkersFromUntimedWaypoints = new JCheckBox(tr("Explicit waypoints with time estimated from track position."));
    private final JCheckBox audioMarkersFromNamedTrackpoints = new JCheckBox(tr("Named trackpoints."));
    private final JCheckBox audioMarkersFromWavTimestamps = new JCheckBox(tr("Modified times (time stamps) of audio files."));
    private final JCheckBox audioMarkersFromStart = new JCheckBox(tr("Start of track (will always do this if no other markers available)."));

    private final JosmTextField audioLeadIn = new JosmTextField(8);
    private final JosmTextField audioForwardBackAmount = new JosmTextField(8);
    private final JosmTextField audioFastForwardMultiplier = new JosmTextField(8);
    private final JosmTextField audioCalibration = new JosmTextField(8);

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        JPanel audio = new VerticallyScrollablePanel(new GridBagLayout());

        // audioMenuVisible
        audioMenuVisible.setSelected(!Config.getPref().getBoolean("audio.menuinvisible"));
        audioMenuVisible.setToolTipText(tr("Show or hide the audio menu entry on the main menu bar."));
        audio.add(audioMenuVisible, GBC.eol().insets(0, 0, 0, 0));

        // audioTraceVisible
        markerAudioTraceVisible.setSelected(Config.getPref().getBoolean("marker.traceaudio", true));
        markerAudioTraceVisible.setToolTipText(
                tr("Display a moving icon representing the point on the synchronized track where the audio currently playing was recorded."));
        audio.add(markerAudioTraceVisible, GBC.eol().insets(0, 0, 0, 0));

        // buttonLabels
        markerButtonLabels.setSelected(Config.getPref().getBoolean("marker.buttonlabels", true));
        markerButtonLabels.setToolTipText(tr("Put text labels against audio (and image and web) markers as well as their button icons."));
        audio.add(markerButtonLabels, GBC.eol().insets(0, 0, 0, 0));

        audio.add(new JLabel(tr("When importing audio, make markers from...")), GBC.eol());

        // audioMarkersFromExplicitWaypoints
        audioMarkersFromExplicitWaypoints.setSelected(Config.getPref().getBoolean("marker.audiofromexplicitwaypoints", true));
        audioMarkersFromExplicitWaypoints.setToolTipText(tr("When importing audio, apply it to any waypoints in the GPX layer."));
        audio.add(audioMarkersFromExplicitWaypoints, GBC.eol().insets(10, 0, 0, 0));

        // audioMarkersFromUntimedWaypoints
        audioMarkersFromUntimedWaypoints.setSelected(Config.getPref().getBoolean("marker.audiofromuntimedwaypoints", true));
        audioMarkersFromUntimedWaypoints.setToolTipText(tr("When importing audio, apply it to any waypoints in the GPX layer."));
        audio.add(audioMarkersFromUntimedWaypoints, GBC.eol().insets(10, 0, 0, 0));

        // audioMarkersFromNamedTrackpoints
        audioMarkersFromNamedTrackpoints.setSelected(Config.getPref().getBoolean("marker.audiofromnamedtrackpoints", false));
        audioMarkersFromNamedTrackpoints.setToolTipText(
                tr("Automatically create audio markers from trackpoints (rather than explicit waypoints) with names or descriptions."));
        audio.add(audioMarkersFromNamedTrackpoints, GBC.eol().insets(10, 0, 0, 0));

        // audioMarkersFromWavTimestamps
        audioMarkersFromWavTimestamps.setSelected(Config.getPref().getBoolean("marker.audiofromwavtimestamps", false));
        audioMarkersFromWavTimestamps.setToolTipText(
                tr("Create audio markers at the position on the track corresponding to the modified time of each audio WAV file imported."));
        audio.add(audioMarkersFromWavTimestamps, GBC.eol().insets(10, 0, 0, 0));

        // audioMarkersFromStart
        audioMarkersFromStart.setSelected(Config.getPref().getBoolean("marker.audiofromstart"));
        audioMarkersFromStart.setToolTipText(
                tr("Automatically create audio markers from trackpoints (rather than explicit waypoints) with names or descriptions."));
        audio.add(audioMarkersFromStart, GBC.eol().insets(10, 0, 0, 0));

        audioForwardBackAmount.setText(Config.getPref().get("audio.forwardbackamount", "10.0"));
        audioForwardBackAmount.setToolTipText(tr("The number of seconds to jump forward or back when the relevant button is pressed"));
        audio.add(new JLabel(tr("Forward/back time (seconds)")), GBC.std());
        audio.add(audioForwardBackAmount, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));

        audioFastForwardMultiplier.setText(Config.getPref().get("audio.fastfwdmultiplier", "1.3"));
        audioFastForwardMultiplier.setToolTipText(tr("The amount by which the speed is multiplied for fast forwarding"));
        audio.add(new JLabel(tr("Fast forward multiplier")), GBC.std());
        audio.add(audioFastForwardMultiplier, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));

        audioLeadIn.setText(Config.getPref().get("audio.leadin", "1.0"));
        audioLeadIn.setToolTipText(
                tr("Playback starts this number of seconds before (or after, if negative) the audio track position requested"));
        audio.add(new JLabel(tr("Lead-in time (seconds)")), GBC.std());
        audio.add(audioLeadIn, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));

        audioCalibration.setText(Config.getPref().get("audio.calibration", "1.0"));
        audioCalibration.setToolTipText(tr("The ratio of voice recorder elapsed time to true elapsed time"));
        audio.add(new JLabel(tr("Voice recorder calibration")), GBC.std());
        audio.add(audioCalibration, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 5));

        audio.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));

        createPreferenceTabWithScrollPane(gui, audio);
    }

    @Override
    public boolean ok() {
        Config.getPref().putBoolean("audio.menuinvisible", !audioMenuVisible.isSelected());
        Config.getPref().putBoolean("marker.traceaudio", markerAudioTraceVisible.isSelected());
        Config.getPref().putBoolean("marker.buttonlabels", markerButtonLabels.isSelected());
        Config.getPref().putBoolean("marker.audiofromexplicitwaypoints", audioMarkersFromExplicitWaypoints.isSelected());
        Config.getPref().putBoolean("marker.audiofromuntimedwaypoints", audioMarkersFromUntimedWaypoints.isSelected());
        Config.getPref().putBoolean("marker.audiofromnamedtrackpoints", audioMarkersFromNamedTrackpoints.isSelected());
        Config.getPref().putBoolean("marker.audiofromwavtimestamps", audioMarkersFromWavTimestamps.isSelected());
        Config.getPref().putBoolean("marker.audiofromstart", audioMarkersFromStart.isSelected());
        Config.getPref().put("audio.forwardbackamount", audioForwardBackAmount.getText());
        Config.getPref().put("audio.fastfwdmultiplier", audioFastForwardMultiplier.getText());
        Config.getPref().put("audio.leadin", audioLeadIn.getText());
        Config.getPref().put("audio.calibration", audioCalibration.getText());
        return false;
    }
}
