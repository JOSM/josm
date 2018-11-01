// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.KeyEvent;

import org.openstreetmap.josm.tools.Shortcut;

/**
 * Decrease the speed of audio playback.
 * Each use decreases the speed further until one of the other controls is used.
 * @since 563
 */
public class AudioSlowerAction extends AudioFastSlowAction {

    /**
     * Constructs a new {@code AudioSlowerAction}.
     */
    public AudioSlowerAction() {
        super(trc("audio", "Slower"), "audio-slower", trc("audio", "Slower Forward"),
        Shortcut.registerShortcut("audio:slower", tr("Audio: {0}", trc("audio", "Slower")), KeyEvent.VK_F4, Shortcut.DIRECT), false);
        setHelpId(ht("/Action/AudioSlower"));
    }
}
