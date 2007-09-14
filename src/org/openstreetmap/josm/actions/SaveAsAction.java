// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Export the data  as OSM intern xml file.
 * 
 * @author imi
 */
public class SaveAsAction extends SaveActionBase {
    
	/**
	 * Construct the action with "Save" as label.
	 * @param layer Save this layer.
	 */
	public SaveAsAction(OsmDataLayer layer) {
		super(tr("Save as"), "save_as", tr("Save the current data to a new file."), KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, layer);
	}
	
	@Override protected File getFile(OsmDataLayer layer) {
		return openFileDialog();
	}
}
