// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;

public class MapPaintPreference implements PreferenceSetting {
	
	public void addGui(final PreferenceDialog gui) {
		// this is intended for a future configuration panel for mappaint!
	}

	public void ok() {
		// dummy
	}

	/** 
	 * Initialize the styles
	 */
	public static void initialize() {
		MapPaintStyles.readFromPreferences();
	}
}
