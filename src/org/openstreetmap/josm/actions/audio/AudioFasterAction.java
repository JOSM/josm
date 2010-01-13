// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.KeyEvent;

import org.openstreetmap.josm.tools.Shortcut;

public class AudioFasterAction extends AudioFastSlowAction {

    public AudioFasterAction() {
        super(tr("Faster"), "audio-faster", tr("Faster Forward"),
        Shortcut.registerShortcut("audio:faster", tr("Audio: {0}", trc("audio", "Faster")), KeyEvent.VK_F9, Shortcut.GROUP_DIRECT), true);
    }
}
