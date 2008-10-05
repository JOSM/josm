// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.ShortCut;

import java.awt.event.KeyEvent;

public class AudioFasterAction extends AudioFastSlowAction {

	public AudioFasterAction() {
		super(tr("Faster"), "audio-faster", tr("Faster Forward"),
		ShortCut.registerShortCut("audio:faster", tr("Audio: Faster"), KeyEvent.VK_F9, ShortCut.GROUP_DIRECT), true);
	}
}
