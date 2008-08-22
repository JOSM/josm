// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.Action;

import org.openstreetmap.josm.gui.tagging.TaggingPreset;

public class TaggingPresetMenu extends TaggingPreset {
	public void setDisplayName() {
		String n = getName();
		putValue(Action.NAME, n);
		putValue(SHORT_DESCRIPTION, "<html>"+tr("Preset group ''{0}''", n)+"</html>");
		putValue("toolbar", "tagginggroup_" + getRawName());
	}
	public void setIcon(String iconName) {
		super.setIcon(iconName);
	}
}
