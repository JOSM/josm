// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;

/**
 * Helper class for all actions, that access the disk
 */
abstract public class DiskAccessAction extends JosmAction {

	public DiskAccessAction(String name, String iconName, String tooltip, int shortCut, int modifiers) {
		super(name, iconName, tooltip, shortCut, modifiers, true);
	}
	
	protected static JFileChooser createAndOpenFileChooser(boolean open, boolean multiple) {
		String curDir = Main.pref.get("lastDirectory");
		if (curDir.equals(""))
			curDir = ".";
		JFileChooser fc = new JFileChooser(new File(curDir));
		fc.setMultiSelectionEnabled(multiple);
		for (int i = 0; i < ExtensionFileFilter.filters.length; ++i)
			fc.addChoosableFileFilter(ExtensionFileFilter.filters[i]);
		fc.setAcceptAllFileFilterUsed(true);
	
		int answer = open ? fc.showOpenDialog(Main.parent) : fc.showSaveDialog(Main.parent);
		if (answer != JFileChooser.APPROVE_OPTION)
			return null;
		
		if (!fc.getCurrentDirectory().getAbsolutePath().equals(curDir))
			Main.pref.put("lastDirectory", fc.getCurrentDirectory().getAbsolutePath());

		if (!open) {
			File file = fc.getSelectedFile();
			if (file == null || (file.exists() && JOptionPane.YES_OPTION != 
					JOptionPane.showConfirmDialog(Main.parent, tr("File exists. Overwrite?"), tr("Overwrite"), JOptionPane.YES_NO_OPTION)))
				return null;
		}
		
		return fc;
	}
}
