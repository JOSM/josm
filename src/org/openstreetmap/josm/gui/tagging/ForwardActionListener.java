// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.openstreetmap.josm.gui.dialogs.PropertiesDialog;

/**
 * Just an ActionListener that forwards calls to actionPerformed to some other
 * listener doing some refresh stuff on the way.
 * @author imi
 */
public final class ForwardActionListener implements ActionListener {
	public final TaggingPreset preset;

	private final PropertiesDialog propertiesDialog;

	public ForwardActionListener(PropertiesDialog propertiesDialog, TaggingPreset preset) {
		this.propertiesDialog = propertiesDialog;
		this.preset = preset;
	}

	public void actionPerformed(ActionEvent e) {
		this.propertiesDialog.taggingPresets.setSelectedIndex(0);
		e.setSource(this);
		preset.actionPerformed(e);
	}
}
