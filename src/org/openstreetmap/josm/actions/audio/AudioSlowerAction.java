// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import org.openstreetmap.josm.tools.ShortCut;

public class AudioSlowerAction extends AudioFastSlowAction {

	public AudioSlowerAction() {
		super(tr("Slower"), "audio-slower", tr("Slower Forward"),
		ShortCut.registerShortCut("audio:slower", tr("Audio: Slower"), KeyEvent.VK_F4, ShortCut.GROUP_DIRECT), true);
	}
}
