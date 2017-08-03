// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Play the sound track from the Audio Marker before the one most recently played.<br>
 * Play from the first such Marker if none has been played or already at the first marker.
 * @since 547
 */
public class AudioPrevAction extends AbstractAudioAction {

    /**
     * Constructs a new {@code AudioPrevAction}.
     */
    public AudioPrevAction() {
        super(trc("audio", "Previous Marker"), "audio-prev", trc("audio", "Play previous marker."),
        Shortcut.registerShortcut("audio:prev", tr("Audio: {0}", trc("audio", "Previous Marker")), KeyEvent.VK_F5, Shortcut.DIRECT), true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MarkerLayer.playPreviousMarker();
    }
}
