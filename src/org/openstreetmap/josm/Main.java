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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.GettingStarted;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.SplashScreen;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.gui.io.SaveLayersDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.gui.preferences.MapPaintPreference;
import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.PlatformHook;
import org.openstreetmap.josm.tools.PlatformHookOsx;
import org.openstreetmap.josm.tools.PlatformHookUnixoid;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.Shortcut;

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
     * calculations. The executed runnables are guaranteed to be executed separately
     * and sequential.
     */
    public final static ExecutorService worker = Executors.newSingleThreadExecutor();
    /**
     * Global application preferences
     */
    public static Preferences pref = new Preferences();

    /**
     * The global paste buffer.
     */
    public static DataSet pasteBuffer = new DataSet();
    public static Layer pasteSource;
    /**
     * The projection method used.
     */
    public static Projection proj;
    /**
     * The MapFrame. Use setMapFrame to set or clear it.
     */
    public static MapFrame map;
    /**
     * The dialog that gets displayed during background task execution.
     */
    //public static PleaseWaitDialog pleaseWaitDlg;

    /**
     * True, when in applet mode
     */
    public static boolean applet = false;

    /**
     * The toolbar preference control to register new actions.
     */
    public static ToolbarPreferences toolbar;


    public UndoRedoHandler undoRedo = new UndoRedoHandler();

    /**
     * The main menu bar at top of screen.
     */
    public final MainMenu menu;

    /**
     * The MOTD Layer.
     */
    private GettingStarted gettingStarted=new GettingStarted();

    /**
     * Print a debug message if debugging is on.
     */
    static public int debug_level = 1;
    static public final void debug(String msg) {
        if (debug_level <= 0)
            return;
        System.out.println(msg);
    }

    /**
     * Platform specific code goes in here.
     * Plugins may replace it, however, some hooks will be called before any plugins have been loeaded.
     * So if you need to hook into those early ones, split your class and send the one with the early hooks
     * to the JOSM team for inclusion.
     */
    public static PlatformHook platform;

    /**
     * Set or clear (if passed <code>null</code>) the map.
     */
    public final void setMapFrame(final MapFrame map) {
        MapFrame old = Main.map;
        Main.map = map;
        panel.setVisible(false);
        panel.removeAll();
        if (map != null) {
            map.fillPanel(panel);
        } else {
            old.destroy();
            panel.add(gettingStarted, BorderLayout.CENTER);
        }
        panel.setVisible(true);
        redoUndoListener.commandChanged(0,0);

        PluginHandler.setMapFrame(old, map);
    }

    /**
     * Remove the specified layer from the map. If it is the last layer,
     * remove the map as well.
     */
    public final void removeLayer(final Layer layer) {
        if (map != null) {
            map.mapView.removeLayer(layer);
            if (map.mapView.getAllLayers().isEmpty()) {
                setMapFrame(null);
            }
        }
    }

    public Main() {
        this(null);
    }

    public Main(SplashScreen splash) {
        main = this;
        //        platform = determinePlatformHook();
        platform.startupHook();
        contentPane.add(panel, BorderLayout.CENTER);
        panel.add(gettingStarted, BorderLayout.CENTER);

        if(splash != null) {
            splash.setStatus(tr("Creating main GUI"));
        }
        menu = new MainMenu();

        undoRedo.listenerCommands.add(redoUndoListener);

        // creating toolbar
        contentPane.add(toolbar.control, BorderLayout.NORTH);

        contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(Shortcut.registerShortcut("system:help", tr("Help"),
                KeyEvent.VK_F1, Shortcut.GROUP_DIRECT).getKeyStroke(), "Help");
        contentPane.getActionMap().put("Help", menu.help);

        TaggingPresetPreference.initialize();
        MapPaintPreference.initialize();

        toolbar.refreshToolbarControl();

        toolbar.control.updateUI();
        contentPane.updateUI();
    }

    /**
     * Add a new layer to the map. If no map exists, create one.
     */
    public final void addLayer(final Layer layer) {
        if (map == null) {
            final MapFrame mapFrame = new MapFrame();
            setMapFrame(mapFrame);
            mapFrame.selectMapMode((MapMode)mapFrame.getDefaultButtonAction());
            mapFrame.setVisible(true);
            mapFrame.setVisibleDialogs();
            // bootstrapping problem: make sure the layer list dialog is going to
            // listen to change events of the very first layer
            //
            layer.addPropertyChangeListener(LayerListDialog.getInstance().getModel());
        }
        map.mapView.addLayer(layer);
    }

    /**
     * Replies true if there is an edit layer
     *
     * @return true if there is an edit layer
     */
    public boolean hasEditLayer() {
        if (map == null) return false;
        if (map.mapView == null) return false;
        if (map.mapView.getEditLayer() == null) return false;
        return true;
    }

    /**
     * Replies the current edit layer
     * 
     * @return the current edit layer. null, if no current edit layer exists
     */
    public OsmDataLayer getEditLayer() {
        if (map == null) return null;
        if (map.mapView == null) return null;
        return map.mapView.getEditLayer();
    }

    /**
     * Replies the current data set.
     *
     * @return the current data set. null, if no current data set exists
     */
    public DataSet getCurrentDataSet() {
        if (!hasEditLayer()) return null;
        return getEditLayer().data;
    }

    /**
     * Use this to register shortcuts to
     */
    public static final JPanel contentPane = new JPanel(new BorderLayout());

    ///////////////////////////////////////////////////////////////////////////
    //  Implementation part
    ///////////////////////////////////////////////////////////////////////////

    public static JPanel panel = new JPanel(new BorderLayout());

    protected static Rectangle bounds;

    private final CommandQueueListener redoUndoListener = new CommandQueueListener(){
        public void commandChanged(final int queueSize, final int redoSize) {
            menu.undo.setEnabled(queueSize > 0);
            menu.redo.setEnabled(redoSize > 0);
        }
    };

    static public void setProjection(String name)
    {
        Bounds b = (map != null && map.mapView != null) ? map.mapView.getRealBounds() : null;
        Projection oldProj = Main.proj;
        try {
            Main.proj = (Projection)Class.forName(name).newInstance();
        } catch (final Exception e) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("The projection {0} could not be activated. Using Mercator", name),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            Main.proj = new Mercator();
        }
        if(!Main.proj.equals(oldProj))
        {
            if(b != null) {
                map.mapView.zoomTo(b);
                /* TODO - remove layers with fixed projection */
            }
        }
    }

    /**
     * Should be called before the main constructor to setup some parameter stuff
     * @param args The parsed argument list.
     */
    public static void preConstructorInit(Map<String, Collection<String>> args) {
        setProjection(Main.pref.get("projection", Mercator.class.getName()));

        try {
            try {
                String laf = Main.pref.get("laf");
                if(laf != null && laf.length() > 0) {
                    UIManager.setLookAndFeel(laf);
                }
            }
            catch (final javax.swing.UnsupportedLookAndFeelException e) {
                System.out.println("Look and Feel not supported: " + Main.pref.get("laf"));
            }
            toolbar = new ToolbarPreferences();
            contentPane.updateUI();
            panel.updateUI();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        UIManager.put("OptionPane.okIcon", ImageProvider.get("ok"));
        UIManager.put("OptionPane.yesIcon", UIManager.get("OptionPane.okIcon"));
        UIManager.put("OptionPane.cancelIcon", ImageProvider.get("cancel"));
        UIManager.put("OptionPane.noIcon", UIManager.get("OptionPane.cancelIcon"));

        // init default coordinate format
        //
        try {
            //CoordinateFormat format = CoordinateFormat.valueOf(Main.pref.get("coordinates"));
            CoordinateFormat.setCoordinateFormat(CoordinateFormat.valueOf(Main.pref.get("coordinates")));
        } catch (IllegalArgumentException iae) {
            CoordinateFormat.setCoordinateFormat(CoordinateFormat.DECIMAL_DEGREES);
        }


        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        String geometry = Main.pref.get("gui.geometry");
        if (args.containsKey("geometry")) {
            geometry = args.get("geometry").iterator().next();
        }
        if (geometry.length() != 0) {
            final Matcher m = Pattern.compile("(\\d+)x(\\d+)(([+-])(\\d+)([+-])(\\d+))?").matcher(geometry);
            if (m.matches()) {
                int w = Integer.valueOf(m.group(1));
                int h = Integer.valueOf(m.group(2));
                int x = 0, y = 0;
                if (m.group(3) != null) {
                    x = Integer.valueOf(m.group(5));
                    y = Integer.valueOf(m.group(7));
                    if (m.group(4).equals("-")) {
                        x = screenDimension.width - x - w;
                    }
                    if (m.group(6).equals("-")) {
                        y = screenDimension.height - y - h;
                    }
                }
                bounds = new Rectangle(x,y,w,h);
                if(!Main.pref.get("gui.geometry").equals(geometry)) {
                    // remember this geometry
                    Main.pref.put("gui.geometry", geometry);
                }
            } else {
                System.out.println("Ignoring malformed geometry: "+geometry);
            }
        }
        if (bounds == null) {
            bounds = !args.containsKey("no-maximize") ? new Rectangle(0,0,screenDimension.width,screenDimension.height) : new Rectangle(1000,740);
        }
    }

    public void postConstructorProcessCmdLine(Map<String, Collection<String>> args) {
        if (args.containsKey("download")) {
            for (String s : args.get("download")) {
                downloadFromParamString(false, s);
            }
        }
        if (args.containsKey("downloadgps")) {
            for (String s : args.get("downloadgps")) {
                downloadFromParamString(true, s);
            }
        }
        if (args.containsKey("selection")) {
            for (String s : args.get("selection")) {
                SearchAction.search(s, SearchAction.SearchMode.add, false, false);
            }
        }
    }

    public static boolean saveUnsavedModifications() {
        if (map == null) return true;
        SaveLayersDialog dialog = new SaveLayersDialog(Main.parent);
        List<OsmDataLayer> layersWithUnmodifiedChanges = new ArrayList<OsmDataLayer>();
        for (OsmDataLayer l: Main.map.mapView.getLayersOfType(OsmDataLayer.class)) {
            if (l.requiresSaveToFile() || l.requiresUploadToServer()) {
                layersWithUnmodifiedChanges.add(l);
            }
        }
        dialog.prepareForSavingAndUpdatingLayersBeforeExit();
        if (!layersWithUnmodifiedChanges.isEmpty()) {
            dialog.getModel().populate(layersWithUnmodifiedChanges);
            dialog.setVisible(true);
            switch(dialog.getUserAction()) {
                case CANCEL: return false;
                case PROCEED: return true;
                default: return false;
            }
        }
        return true;
    }

    private static void downloadFromParamString(final boolean rawGps, String s) {
        if (s.startsWith("http:")) {
            final Bounds b = OsmUrlToBounds.parse(s);
            if (b == null) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Ignoring malformed URL: \"{0}\"", s),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
            } else {
                //DownloadTask osmTask = main.menu.download.downloadTasks.get(0);
                DownloadTask osmTask = new DownloadOsmTask();
                osmTask.download(main.menu.download, b.min.lat(), b.min.lon(), b.max.lat(), b.max.lon(), null);
            }
            return;
        }

        if (s.startsWith("file:")) {
            File f = null;
            try {
                f = new File(new URI(s));
            } catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Ignoring malformed file URL: \"{0}\"", s),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
            }
            try {
                if (f!=null) {
                    OpenFileAction.openFile(f);
                }
            } catch(IllegalDataException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Could not read file ''{0}\''.<br> Error is: <br>{1}</html>", f.getName(), e.getMessage()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }catch(IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Could not read file ''{0}\''.<br> Error is: <br>{1}</html>", f.getName(), e.getMessage()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
            return;
        }

        final StringTokenizer st = new StringTokenizer(s, ",");
        if (st.countTokens() == 4) {
            try {
                DownloadTask task = rawGps ? new DownloadGpsTask() : new DownloadOsmTask();
                task.download(main.menu.download, Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), null);
                return;
            } catch (final NumberFormatException e) {
            }
        }
        File f = new File(s);
        try {
            OpenFileAction.openFile(f);
        }catch(IllegalDataException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Could not read file ''{0}\''.<br> Error is: <br>{1}</html>", f.getName(), e.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }catch(IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Could not read file ''{0}\''.<br> Error is: <br>{1}</html>", f.getName(), e.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void determinePlatformHook() {
        String os = System.getProperty("os.name");
        if (os == null) {
            System.err.println("Your operating system has no name, so I'm guessing its some kind of *nix.");
            platform = new PlatformHookUnixoid();
        } else if (os.toLowerCase().startsWith("windows")) {
            platform = new PlatformHookWindows();
        } else if (os.equals("Linux") || os.equals("Solaris") ||
                os.equals("SunOS") || os.equals("AIX") ||
                os.equals("FreeBSD") || os.equals("NetBSD") || os.equals("OpenBSD")) {
            platform = new PlatformHookUnixoid();
        } else if (os.toLowerCase().startsWith("mac os x")) {
            platform = new PlatformHookOsx();
        } else {
            System.err.println("I don't know your operating system '"+os+"', so I'm guessing its some kind of *nix.");
            platform = new PlatformHookUnixoid();
        }
    }

    static public void saveGuiGeometry() {
        // save the current window geometry and the width of the toggle dialog area
        String newGeometry = "";
        String newToggleDlgWidth = "";
        try {
            if (((JFrame)parent).getExtendedState() == JFrame.NORMAL) {
                Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
                Rectangle bounds = parent.getBounds();
                int width = (int)bounds.getWidth();
                int height = (int)bounds.getHeight();
                int x = (int)bounds.getX();
                int y = (int)bounds.getY();
                if (width > screenDimension.width) {
                    width = screenDimension.width;
                }
                if (height > screenDimension.height) {
                    width = screenDimension.height;
                }
                if (x < 0) {
                    x = 0;
                }
                if (y < 0) {
                    y = 0;
                }
                newGeometry = width + "x" + height + "+" + x + "+" + y;
            }

            newToggleDlgWidth = Integer.toString(map.getToggleDlgWidth());
            if (newToggleDlgWidth.equals(Integer.toString(map.DEF_TOGGLE_DLG_WIDTH))) {
                newToggleDlgWidth = "";
            }
        }
        catch (Exception e) {
            System.out.println("Failed to save GUI geometry: " + e);
        }
        pref.put("gui.geometry", newGeometry);
        pref.put("toggleDialogs.width", newToggleDlgWidth);
    }
}
