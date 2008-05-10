// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;

public class AudioSlowerAction extends AudioFastSlowAction {
	
	public AudioSlowerAction() {
		super(tr("Slower"), "audio-slower", tr("Slower Forward"), KeyEvent.VK_F9, KeyEvent.SHIFT_MASK, false);
	}
}
