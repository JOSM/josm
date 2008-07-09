// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Export the data as an OSM xml file.
 * 
 * @author imi
 */
public class SaveAction extends SaveActionBase {
    
	/**
	 * Construct the action with "Save" as label.
	 * @param layer Save this layer.
	 */
	public SaveAction(Layer layer) {
		super(tr("Save"), "save", tr("Save the current data."), KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, layer);
	}
	
	@Override public File getFile(Layer layer) {
		if (layer instanceof OsmDataLayer) {
			File f = ((OsmDataLayer)layer).associatedFile;
			if (f != null) {
				return f;
			}
		}
		if (layer instanceof GpxLayer) {
			File f = ((GpxLayer)layer).data.storageFile;
			if (f != null) {
				return f;
			}
		}
		return openFileDialog(layer);
	}
}
