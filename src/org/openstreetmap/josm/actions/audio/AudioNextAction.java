// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.audio;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;

public class AudioNextAction extends JosmAction {

	public AudioNextAction() {
		super(tr("Next Marker"), "audio-next", tr("Play next marker."), KeyEvent.VK_F8, 0, true);
	}

	public void actionPerformed(ActionEvent e) {
		MarkerLayer.playNextMarker();
	}
}
