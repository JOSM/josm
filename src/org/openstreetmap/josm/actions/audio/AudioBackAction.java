// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.AudioPlayer;
import org.openstreetmap.josm.tools.Shortcut;

public class AudioBackAction extends JosmAction {

    public AudioBackAction() {
        super(tr("Back"), "audio-back", tr("Jump back."),
        Shortcut.registerShortcut("audio:back", tr("Audio: {0}", trc("audio", "Back")), KeyEvent.VK_F6, Shortcut.GROUP_DIRECT), true);
        this.putValue("help", "Action/Back");
    }

    public void actionPerformed(ActionEvent e) {
        try {
            if (AudioPlayer.playing() || AudioPlayer.paused())
                AudioPlayer.play(AudioPlayer.url(), AudioPlayer.position()
                - Main.pref.getDouble("audio.forwardbackamount","10.0"));
            else
                MarkerLayer.playAudio();
        } catch (Exception ex) {
            AudioPlayer.audioMalfunction(ex);
        }
    }
}
