package org.openstreetmap.gui.jmapviewer.interfaces;

//License: GPL.

import java.util.EventListener;

import org.openstreetmap.gui.jmapviewer.events.JMVCommandEvent;

/**
 * Must be implemented for processing commands while user
 * interacts with map viewer.
 * 
 * @author Jason Huntley
 *
 */
public interface JMapViewerEventListener extends EventListener {
	public void processCommand(JMVCommandEvent command);
}
