// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.Shortcut;

public class AudioPrevAction extends JosmAction {

    public AudioPrevAction() {
        super(tr("Previous Marker"), "audio-prev", tr("Play previous marker."),
        Shortcut.registerShortcut("audio:prev", tr("Audio: {0}", trc("audio", "Previous Marker")), KeyEvent.VK_F5, Shortcut.GROUP_DIRECT), true);
    }

    public void actionPerformed(ActionEvent e) {
        MarkerLayer.playPreviousMarker();
    }
}
