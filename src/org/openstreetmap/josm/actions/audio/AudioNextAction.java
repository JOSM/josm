// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Play the sound track from the Audio Marker after the one most recently played.<br>
 * Play from the first such Marker if none has been played, or repeat the last marker if at the end.
 * @since 547
 */
public class AudioNextAction extends AbstractAudioAction {

    /**
     * Constructs a new {@code AudioNextAction}.
     */
    public AudioNextAction() {
        super(trc("audio", "Next Marker"), "audio-next", trc("audio", "Play next marker."),
        Shortcut.registerShortcut("audio:next", tr("Audio: {0}", trc("audio", "Next Marker")), KeyEvent.VK_F8, Shortcut.DIRECT), true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MarkerLayer.playNextMarker();
    }
}
