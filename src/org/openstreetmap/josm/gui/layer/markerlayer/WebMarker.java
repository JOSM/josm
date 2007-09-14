// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * Marker class with Web URL activation.
 * 
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class WebMarker extends ButtonMarker {

	public URL webUrl;

	public static WebMarker create (LatLon ll, String url) {
		try {
			return new WebMarker(ll, new URL(url));
		} catch (Exception ex) {
			return null;
		}
	}

	private WebMarker(LatLon ll, URL webUrl) {
		super(ll, "web.png");
		this.webUrl = webUrl;
	}

	@Override public void actionPerformed(ActionEvent ev) {
		String error = OpenBrowser.displayUrl(webUrl.toString());
		if (error != null) {
			JOptionPane.showMessageDialog(Main.parent, 
					"<html><b>" + 
					tr("There was an error while trying to display the URL for this marker") +
					"</b><br>" + tr("(URL was: ") + webUrl.toString() + ")" + "<br>" + error, 
					tr("Error displaying URL"), JOptionPane.ERROR_MESSAGE);
		}
	}
}
