// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.Shortcut;

public class AudioNextAction extends JosmAction {

    public AudioNextAction() {
        super(tr("Next Marker"), "audio-next", tr("Play next marker."),
        Shortcut.registerShortcut("audio:next", tr("Audio: {0}", trc("audio", "Next Marker")), KeyEvent.VK_F8, Shortcut.GROUP_DIRECT), true);
    }

    public void actionPerformed(ActionEvent e) {
        MarkerLayer.playNextMarker();
    }
}
