// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.plugins.PluginProxy;
import org.openstreetmap.josm.tools.GBC;

/**
 * Main download dialog.
 * 
 * Can be extended by plugins in two ways:
 * (1) by adding download tasks that are then called with the selected bounding box
 * (2) by adding "DownloadSelection" objects that implement different ways of selecting a bounding box
 * 
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class DownloadDialog extends JPanel {
	

	public interface DownloadTask {
		/**
		 * Execute the download.
		 */
		void download(DownloadAction action, double minlat, double minlon, double maxlat, double maxlon);
		/**
		 * @return The checkbox presented to the user
		 */
		JCheckBox getCheckBox();
		/**
		 * @return The name of the preferences suffix to use for storing the
		 * selection state.
		 */
		String getPreferencesSuffix();
	}

	/**
	 * The list of download tasks. First entry should be the osm data entry
	 * and the second the gps entry. After that, plugins can register additional
	 * download possibilities.
	 */
	public final List<DownloadTask> downloadTasks = new ArrayList<DownloadTask>(5);

	public final List<DownloadSelection> downloadSelections = new ArrayList<DownloadSelection>();
	public final JTabbedPane tabpane = new JTabbedPane();
	public final JCheckBox newLayer;
	
	public double minlon;
	public double minlat;
	public double maxlon;
	public double maxlat;
	
	
	public DownloadDialog() {
		setLayout(new GridBagLayout());
		
		downloadTasks.add(new DownloadOsmTask());
		downloadTasks.add(new DownloadGpsTask());
		
		// adding the download tasks
		add(new JLabel(tr("Data Sources and Types")), GBC.eol().insets(0,5,0,0));
		for (DownloadTask task : downloadTasks) {
			add(task.getCheckBox(), GBC.eol().insets(20,0,0,0));
			task.getCheckBox().setSelected(Main.pref.getBoolean("download."+task.getPreferencesSuffix()));
		}
		
		// predefined download selections
		downloadSelections.add(new BoundingBoxSelection());
		downloadSelections.add(new BookmarkSelection());	
		downloadSelections.add(new WorldChooser());
		
		// add selections from plugins
		for (PluginProxy p : Main.plugins) {
			p.addDownloadSelection(downloadSelections);
		}
		
		// now everybody may add their tab to the tabbed pane
		// (not done right away to allow plugins to remove one of
		// the default selectors!)
		for (DownloadSelection s : downloadSelections) {
			s.addGui(this);
		}
		
		if (Main.map != null) {
			MapView mv = Main.map.mapView;
			minlon = mv.getLatLon(0, mv.getHeight()).lon();
			minlat = mv.getLatLon(0, mv.getHeight()).lat();
			maxlon = mv.getLatLon(mv.getWidth(), 0).lon();
			maxlat = mv.getLatLon(mv.getWidth(), 0).lat();
			boundingBoxChanged(null);
		}

		newLayer = new JCheckBox(tr("Download as new layer"), Main.pref.getBoolean("download.newlayer", false));
		add(newLayer, GBC.eol().insets(0,5,0,0));

		add(new JLabel(tr("Download Area")), GBC.eol().insets(0,5,0,0));
		add(tabpane, GBC.eol().fill());
		
		try {
			tabpane.setSelectedIndex(Integer.parseInt(Main.pref.get("download.tab", "0")));
		} catch (Exception ex) {
			Main.pref.put("download.tab", "0");
		}
	}
	
	/**
	 * Distributes a "bounding box changed" from ohne DownloadSelection 
	 * object to the others, so they may update or clear their input 
	 * fields.
	 * 
	 * @param eventSource - the DownloadSelection object that fired this notification.
	 */
	public void boundingBoxChanged(DownloadSelection eventSource) {
		for (DownloadSelection s : downloadSelections) {
			if (s != eventSource) s.boundingBoxChanged(this);
		}	
	}

	/*
	 * Returns currently selected tab.
	 */
	public int getSelectedTab() {
		return tabpane.getSelectedIndex();
	}
}
