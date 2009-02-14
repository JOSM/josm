package org.openstreetmap.josm.tools;

import java.util.Comparator;

import javax.swing.JMenuItem;

public class PresetTextComparator implements Comparator {
	//TODO add error checking and stuff
	public int compare(Object arg0, Object arg1) {
		return ((JMenuItem)arg0).getText().compareTo(((JMenuItem)arg1).getText());
	}


}
