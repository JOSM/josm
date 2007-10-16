// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Epsg4326;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.GettingStarted;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.PleaseWaitDialog;
import org.openstreetmap.josm.gui.download.BoundingBoxSelection;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.PluginProxy;
import org.openstreetmap.josm.tools.ImageProvider;

abstract public class Main {
	/**
	 * Global parent component for all dialogs and message boxes
	 */
	public static Component parent;
	/**
	 * Global application.
	 */
	public static Main main;
	/**
	 * The worker thread slave. This is for executing all long and intensive
	 * calculations. The executed runnables are guaranteed to be executed seperatly
	 * and sequenciel.
	 */
	public final static Executor worker = Executors.newSingleThreadExecutor();
	/**
	 * Global application preferences
	 */
	public static Preferences pref = new Preferences();
	/**
	 * The global dataset.
	 */
	public static DataSet ds = new DataSet();
	/**
	 * The projection method used.
	 */
	public static Projection proj;
	/**
	 * The MapFrame. Use setMapFrame to set or clear it.
	 */
	public static MapFrame map;
	/**
	 * All installed and loaded plugins (resp. their main classes)
	 */
	public final static Collection<PluginProxy> plugins = new LinkedList<PluginProxy>();
	/**
	 * The dialog that gets displayed during background task execution.
	 */
	public static PleaseWaitDialog pleaseWaitDlg;

	/**
	 * True, when in applet mode
	 */
	public static boolean applet = false;

	/**
	 * The toolbar preference control to register new actions.
	 */
	public static ToolbarPreferences toolbar = new ToolbarPreferences();


	public UndoRedoHandler undoRedo = new UndoRedoHandler();

	/**
	 * The main menu bar at top of screen.
	 */
	public final MainMenu menu;




	/**
	 * Set or clear (if passed <code>null</code>) the map.
	 */
	public final void setMapFrame(final MapFrame map) {
		MapFrame old = Main.map;
		Main.map = map;
		panel.setVisible(false);
		panel.removeAll();
		if (map != null)
			map.fillPanel(panel);
		else {
			old.destroy();
			panel.add(new GettingStarted(), BorderLayout.CENTER);
		}
		panel.setVisible(true);
		redoUndoListener.commandChanged(0,0);

		for (PluginProxy plugin : plugins)
			plugin.mapFrameInitialized(old, map);
	}

	/**
	 * Set the layer menu (changed when active layer changes).
	 */
	public final void setLayerMenu(Component[] entries) {
		if (entries == null || entries.length == 0)
			menu.layerMenu.setVisible(false);
		else {
			menu.layerMenu.removeAll();
			for (Component c : entries)
				menu.layerMenu.add(c);
			menu.layerMenu.setVisible(true);
		}
	}

	/**
	 * Remove the specified layer from the map. If it is the last layer, remove the map as well.
	 */
	public final void removeLayer(final Layer layer) {
		map.mapView.removeLayer(layer);
		if (layer instanceof OsmDataLayer)
			ds = new DataSet();
		if (map.mapView.getAllLayers().isEmpty())
			setMapFrame(null);
	}


	public Main() {
		main = this;
		contentPane.add(panel, BorderLayout.CENTER);
		panel.add(new GettingStarted(), BorderLayout.CENTER);
		menu = new MainMenu();

		undoRedo.listenerCommands.add(redoUndoListener);
		
		// creating toolbar
		contentPane.add(toolbar.control, BorderLayout.NORTH);

		contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "Help");
		contentPane.getActionMap().put("Help", menu.help);

		TaggingPresetPreference.initialize();

		toolbar.refreshToolbarControl();

		toolbar.control.updateUI();
		contentPane.updateUI();
	}

	/**
	 * Load all plugins specified in preferences. If the parameter is <code>true</code>, all
	 * early plugins are loaded (before constructor).
	 */
	public static void loadPlugins(boolean early) {
		List<String> plugins = new LinkedList<String>();
		if (Main.pref.hasKey("plugins"))
			plugins.addAll(Arrays.asList(Main.pref.get("plugins").split(",")));
		if (System.getProperty("josm.plugins") != null)
			plugins.addAll(Arrays.asList(System.getProperty("josm.plugins").split(",")));
		if (plugins.isEmpty())
			return;
		SortedMap<Integer, Collection<PluginInformation>> p = new TreeMap<Integer, Collection<PluginInformation>>();
		for (String pluginName : plugins) {
			PluginInformation info = PluginInformation.findPlugin(pluginName);
			if (info != null) {
				if (info.early != early)
					continue;
				if (!p.containsKey(info.stage))
					p.put(info.stage, new LinkedList<PluginInformation>());
				p.get(info.stage).add(info);
			} else {
				if (early)
					System.out.println("Plugin not found: "+pluginName); // do not translate
				else	
					JOptionPane.showMessageDialog(Main.parent, tr("Plugin not found: {0}.", pluginName));
			}
		}
		
		// iterate all plugins and collect all libraries of all plugins:
		List<URL> allPluginLibraries = new ArrayList<URL>();
		for (Collection<PluginInformation> c : p.values())
			for (PluginInformation info : c)
				allPluginLibraries.addAll(info.libraries);
		// create a classloader for all plugins:
		URL[] jarUrls = new URL[allPluginLibraries.size()];
		jarUrls = allPluginLibraries.toArray(jarUrls);
		URLClassLoader pluginClassLoader = new URLClassLoader(jarUrls, Main.class.getClassLoader());
		ImageProvider.sources.add(0, pluginClassLoader);

		for (Collection<PluginInformation> c : p.values()) {
			for (PluginInformation info : c) {
				try {
					Class<?> klass = info.loadClass(pluginClassLoader);
					if (klass != null) {
						System.out.println("loading "+info.name);
						Main.plugins.add(info.load(klass));
					}
				} catch (Throwable e) {
					e.printStackTrace();
					if (early)
						System.out.println("Could not load plugin: "+info.name+" - deleted from preferences"); // do not translate
					else
						JOptionPane.showMessageDialog(Main.parent, tr("Could not load plugin {0}. Deleted from preferences.", info.name));
					plugins.remove(info.name);
					String plist = null;
					for (String pn : plugins) { 
						if (plist==null) plist=""; else plist=plist+",";
						plist=plist+pn;
					}
					Main.pref.put("plugins", plist);
				}
			}
		}
	}

	/**
	 * Add a new layer to the map. If no map exist, create one.
	 */
	public final void addLayer(final Layer layer) {
		if (map == null) {
			final MapFrame mapFrame = new MapFrame();
			setMapFrame(mapFrame);
			mapFrame.selectMapMode((MapMode)mapFrame.getDefaultButtonAction());
			mapFrame.setVisible(true);
			mapFrame.setVisibleDialogs();
		}
		map.mapView.addLayer(layer);
	}
	/**
	 * @return The edit osm layer. If none exist, it will be created.
	 */
	public final OsmDataLayer editLayer() {
		if (map == null || map.mapView.editLayer == null)
			menu.newAction.actionPerformed(null);
		return map.mapView.editLayer;
	}




	/**
	 * Use this to register shortcuts to
	 */
	public static final JPanel contentPane = new JPanel(new BorderLayout());


	////////////////////////////////////////////////////////////////////////////////////////
	//  Implementation part
	////////////////////////////////////////////////////////////////////////////////////////

	public static JPanel panel = new JPanel(new BorderLayout());

	protected static Rectangle bounds;

	private final CommandQueueListener redoUndoListener = new CommandQueueListener(){
		public void commandChanged(final int queueSize, final int redoSize) {
			menu.undo.setEnabled(queueSize > 0);
			menu.redo.setEnabled(redoSize > 0);
		}
	};
	/**
	 * Should be called before the main constructor to setup some parameter stuff
	 * @param args The parsed argument list.
	 */
	public static void preConstructorInit(Map<String, Collection<String>> args) {
		try {
			Main.proj = (Projection)Class.forName(Main.pref.get("projection")).newInstance();
		} catch (final Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, tr("The projection could not be read from preferences. Using EPSG:4263."));
			Main.proj = new Epsg4326();
		}

		try {
			UIManager.setLookAndFeel(Main.pref.get("laf"));
			contentPane.updateUI();
			panel.updateUI();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		UIManager.put("OptionPane.okIcon", ImageProvider.get("ok"));
		UIManager.put("OptionPane.yesIcon", UIManager.get("OptionPane.okIcon"));
		UIManager.put("OptionPane.cancelIcon", ImageProvider.get("cancel"));
		UIManager.put("OptionPane.noIcon", UIManager.get("OptionPane.cancelIcon"));

		Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
		if (args.containsKey("geometry")) {
			String geometry = args.get("geometry").iterator().next();
			final Matcher m = Pattern.compile("(\\d+)x(\\d+)(([+-])(\\d+)([+-])(\\d+))?").matcher(geometry);
			if (m.matches()) {
				int w = Integer.valueOf(m.group(1));
				int h = Integer.valueOf(m.group(2));
				int x = 0, y = 0;
				if (m.group(3) != null) {
					x = Integer.valueOf(m.group(5));
					y = Integer.valueOf(m.group(7));
					if (m.group(4).equals("-"))
						x = screenDimension.width - x - w;
					if (m.group(6).equals("-"))
						y = screenDimension.height - y - h;
				}
				bounds = new Rectangle(x,y,w,h);
			} else
				System.out.println("Ignoring malformed geometry: "+geometry);
		}
		if (bounds == null)
			bounds = !args.containsKey("no-fullscreen") ? new Rectangle(0,0,screenDimension.width,screenDimension.height) : new Rectangle(1000,740);

			// preinitialize a wait dialog for all early downloads (e.g. via command line)
			pleaseWaitDlg = new PleaseWaitDialog(null);
	}

	public void postConstructorProcessCmdLine(Map<String, Collection<String>> args) {
		// initialize the pleaseWaitDialog with the application as parent to handle focus stuff
		pleaseWaitDlg = new PleaseWaitDialog(parent);

		if (args.containsKey("download"))
			for (String s : args.get("download"))
				downloadFromParamString(false, s);
		if (args.containsKey("downloadgps"))
			for (String s : args.get("downloadgps"))
				downloadFromParamString(true, s);
		if (args.containsKey("selection"))
			for (String s : args.get("selection"))
				SearchAction.search(s, SearchAction.SearchMode.add, false);
	}

	public static boolean breakBecauseUnsavedChanges() {
		if (map != null) {
			boolean modified = false;
			boolean uploadedModified = false;
			for (final Layer l : map.mapView.getAllLayers()) {
				if (l instanceof OsmDataLayer && ((OsmDataLayer)l).isModified()) {
					modified = true;
					uploadedModified = ((OsmDataLayer)l).uploadedModified;
					break;
				}
			}
			if (modified) {
				final String msg = uploadedModified ? "\n"+tr("Hint: Some changes came from uploading new data to the server.") : "";
				final int answer = JOptionPane.showConfirmDialog(
						parent, tr("There are unsaved changes. Discard the changes and continue?")+msg,
						tr("Unsaved Changes"), JOptionPane.YES_NO_OPTION);
				if (answer != JOptionPane.YES_OPTION)
					return true;
			}
		}
		return false;
	}

	private static void downloadFromParamString(final boolean rawGps, String s) {
		if (s.startsWith("http:")) {
			final Bounds b = BoundingBoxSelection.osmurl2bounds(s);
			if (b == null)
				JOptionPane.showMessageDialog(Main.parent, tr("Ignoring malformed url: \"{0}\"", s));
			else {
				//DownloadTask osmTask = main.menu.download.downloadTasks.get(0);
				DownloadTask osmTask = new DownloadOsmTask();
				osmTask.download(main.menu.download, b.min.lat(), b.min.lon(), b.max.lat(), b.max.lon());
			}
			return;
		}

		if (s.startsWith("file:")) {
			try {
				main.menu.open.openFile(new File(new URI(s)));
			} catch (URISyntaxException e) {
				JOptionPane.showMessageDialog(Main.parent, tr("Ignoring malformed file url: \"{0}\"", s));
			}
			return;
		}

		final StringTokenizer st = new StringTokenizer(s, ",");
		if (st.countTokens() == 4) {
			try {
				//DownloadTask task = main.menu.download.downloadTasks.get(rawGps ? 1 : 0);
				DownloadTask task = rawGps ? new DownloadGpsTask() : new DownloadOsmTask();
				task.download(main.menu.download, Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
				return;
			} catch (final NumberFormatException e) {
			}
		}

		main.menu.open.openFile(new File(s));
	}
}
