// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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

	// the JOptionPane that contains this dialog. required for the closeDialog() method.
	private JOptionPane optionPane;

	public interface DownloadTask {
		/**
		 * Execute the download.
		 */
		void download(DownloadAction action, double minlat, double minlon, double maxlat, double maxlon);
        void loadUrl(boolean newLayer, String url);
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
	public final JLabel sizeCheck = new JLabel();

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
			// don't override defaults, if we (initially) don't have any preferences
			if(Main.pref.hasKey("download."+task.getPreferencesSuffix())) {
				task.getCheckBox().setSelected(Main.pref.getBoolean("download."+task.getPreferencesSuffix()));
			}
		}

		// predefined download selections
		downloadSelections.add(new BoundingBoxSelection());
		downloadSelections.add(new TileSelection());
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
		else if (Main.pref.hasKey("osm-download.bounds")) {
			// read the bounding box from the preferences
			try {
				String bounds[] = Main.pref.get("osm-download.bounds").split(";");
				minlat = Double.parseDouble(bounds[0]);
				minlon = Double.parseDouble(bounds[1]);
				maxlat = Double.parseDouble(bounds[2]);
				maxlon = Double.parseDouble(bounds[3]);
				boundingBoxChanged(null);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
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

		Font labelFont = sizeCheck.getFont();
		sizeCheck.setFont(labelFont.deriveFont(Font.PLAIN, labelFont.getSize()));
		add(sizeCheck, GBC.eop().insets(0,5,5,10));
	}

	private void updateSizeCheck() {
		if ((maxlon-minlon)*(maxlat-minlat) > Main.pref.getDouble("osm-server.max-request-area", 0.25)) {
			sizeCheck.setText(tr("Download area too large; will probably be rejected by server"));
			sizeCheck.setForeground(Color.red);
		} else {
			sizeCheck.setText(tr("Download area ok, size probably acceptable to server"));
			sizeCheck.setForeground(Color.darkGray);
		}
	}

	/**
	 * Distributes a "bounding box changed" from one DownloadSelection
	 * object to the others, so they may update or clear their input
	 * fields.
	 *
	 * @param eventSource - the DownloadSelection object that fired this notification.
	 */
	public void boundingBoxChanged(DownloadSelection eventSource) {
		for (DownloadSelection s : downloadSelections) {
			if (s != eventSource) s.boundingBoxChanged(this);
		}
		updateSizeCheck();
	}

	/*
	 * Returns currently selected tab.
	 */
	public int getSelectedTab() {
		return tabpane.getSelectedIndex();
	}

	/**
	 * Closes the download dialog. This is intended to be called by one of
	 * the various download area selection "plugins".
	 *
	 * @param download true to download selected data, false to cancel download
	 */
	public void closeDownloadDialog(boolean download) {
		optionPane.setValue(download ? JOptionPane.OK_OPTION : JOptionPane.CANCEL_OPTION);
	}

	/**
	 * Has to be called after this dialog has been added to a JOptionPane.
	 * @param optionPane
	 */
	public void setOptionPane(JOptionPane optionPane) {
		this.optionPane = optionPane;
	}
}
