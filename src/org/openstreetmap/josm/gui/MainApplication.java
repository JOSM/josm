// License: GPL. Copyright 2007 by Immanuel Scholz and others
//Licence: GPL
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginDownloader;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
/**
 * Main window class application.
 *
 * @author imi
 */
public class MainApplication extends Main {
	/**
	 * Construct an main frame, ready sized and operating. Does not
	 * display the frame.
	 */
	public MainApplication(JFrame mainFrame) {
		mainFrame.setContentPane(contentPane);
		mainFrame.setJMenuBar(menu);
		mainFrame.setBounds(bounds);
		mainFrame.addWindowListener(new WindowAdapter(){
			@Override public void windowClosing(final WindowEvent arg0) {
				if (Main.breakBecauseUnsavedChanges())
					return;
				System.exit(0);
			}
		});
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}

	/**
	 * Main application Startup
	 */
	@SuppressWarnings("deprecation")
	public static void main(final String[] argArray) {
		/////////////////////////////////////////////////////////////////////////
		//                        TO ALL TRANSLATORS
		/////////////////////////////////////////////////////////////////////////
		// Do not translate the early strings below until the locale is set up.
		// (By the eager loaded plugins)
		//
		// These strings cannot be translated. That's life. Really. Sorry.
		//
		//                                                                 Imi.
		/////////////////////////////////////////////////////////////////////////

		Thread.setDefaultUncaughtExceptionHandler(new BugReportExceptionHandler());

		// construct argument table
		List<String> argList = Arrays.asList(argArray);
		final Map<String, Collection<String>> args = new HashMap<String, Collection<String>>();
		for (String arg : argArray) {
			if (!arg.startsWith("--"))
				arg = "--download="+arg;
			int i = arg.indexOf('=');
			String key = i == -1 ? arg.substring(2) : arg.substring(2,i);
			String value = i == -1 ? "" : arg.substring(i+1);
			Collection<String> v = args.get(key);
			if (v == null)
				v = new LinkedList<String>();
			v.add(value);
			args.put(key, v);
		}

		// get the preferences.
		final File prefDir = new File(Main.pref.getPreferencesDir());
		// check if preferences directory has moved (TODO: Update code. Remove this after some time)
		File oldPrefDir = new File(System.getProperty("user.home"), ".josm");
		if (!prefDir.isDirectory() && oldPrefDir.isDirectory()) {
			if (oldPrefDir.renameTo(prefDir)) {
				// do not translate this
				JOptionPane.showMessageDialog(null, "The preference directory has been moved to "+prefDir);
			} else {
				JOptionPane.showMessageDialog(null, "The preference directory location has changed. Please move "+oldPrefDir+" to "+prefDir);
			}
		}

		if (prefDir.exists() && !prefDir.isDirectory()) {
			JOptionPane.showMessageDialog(null, "Cannot open preferences directory: "+Main.pref.getPreferencesDir());
			return;
		}
		if (!prefDir.exists())
			prefDir.mkdirs();

		if (!new File(Main.pref.getPreferencesDir()+"preferences").exists()) {
			Main.pref.resetToDefault();
		}

		try {
			if (args.containsKey("reset-preferences")) {
				Main.pref.resetToDefault();
			} else {
				Main.pref.load();
			}
		} catch (final IOException e1) {
			e1.printStackTrace();
			String backup = Main.pref.getPreferencesDir() + "preferences.bak";
			JOptionPane.showMessageDialog(null, "Preferences file had errors.  Making backup of old one to " + backup);
			new File(Main.pref.getPreferencesDir() + "preferences").renameTo(new File(backup));
			Main.pref.save();
		}

		if (!PluginDownloader.moveUpdatedPlugins()) {
			JOptionPane.showMessageDialog(null,
			        tr("Activating the updated plugins failed."),
			        tr("Plugins"), JOptionPane.ERROR_MESSAGE);
		}
		
		// load the early plugins
		Main.loadPlugins(true);

		if (argList.contains("--help") || argList.contains("-?") || argList.contains("-h")) {
			System.out.println(tr("Java OpenStreetMap Editor")+"\n\n"+
					tr("usage")+":\n"+
					"\tjava -jar josm.jar <option> <option> <option>...\n\n"+
					tr("options")+":\n"+
					"\t--help|-?|-h                              "+tr("Show this help")+"\n"+
					"\t--geometry=widthxheight(+|-)x(+|-)y       "+tr("Standard unix geometry argument")+"\n"+
					"\t[--download=]minlat,minlon,maxlat,maxlon  "+tr("Download the bounding box")+"\n"+
					"\t[--download=]<url>                        "+tr("Download the location at the url (with lat=x&lon=y&zoom=z)")+"\n"+
					"\t[--download=]<filename>                   "+tr("Open file (as raw gps, if .gpx)")+"\n"+
					"\t--downloadgps=minlat,minlon,maxlat,maxlon "+tr("Download the bounding box as raw gps")+"\n"+
					"\t--selection=<searchstring>                "+tr("Select with the given search")+"\n"+
					"\t--no-fullscreen                           "+tr("Don't launch in fullscreen mode")+"\n"+
					"\t--reset-preferences                       "+tr("Reset the preferences to default")+"\n\n"+
					"\t--language=<language>                     "+tr("Set the language. Example: ")+"\n\n"+
					tr("examples")+":\n"+
					"\tjava -jar josm.jar track1.gpx track2.gpx london.osm\n"+
					"\tjava -jar josm.jar http://www.openstreetmap.org/index.html?lat=43.2&lon=11.1&zoom=13\n"+
					"\tjava -jar josm.jar london.osm --selection=http://www.ostertag.name/osm/OSM_errors_node-duplicate.xml\n"+
					"\tjava -jar josm.jar 43.2,11.1,43.4,11.4\n\n"+

					tr("Parameters are read in the order they are specified, so make sure you load\n"+
					"some data before --selection")+"\n\n"+
					tr("Instead of --download=<bbox> you may specify osm://<bbox>\n"));
			System.exit(0);
		}

		preConstructorInit(args);
		JFrame mainFrame = new JFrame(tr("Java OpenStreetMap - Editor"));
		Main.parent = mainFrame;
		final Main main = new MainApplication(mainFrame);
		Main.loadPlugins(false);
		toolbar.refreshToolbarControl();

		mainFrame.setVisible(true);

		if (!args.containsKey("no-fullscreen") && !args.containsKey("geometry") && Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH))
			mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

		EventQueue.invokeLater(new Runnable(){
			public void run() {
				main.postConstructorProcessCmdLine(args);
			}
		});
	}
}
