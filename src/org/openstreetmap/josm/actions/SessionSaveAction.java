// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.data.PreferencesUtils;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrameListener;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.io.session.PluginSessionExporter;
import org.openstreetmap.josm.io.session.SessionLayerExporter;
import org.openstreetmap.josm.io.session.SessionWriter;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.UserCancelException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Saves a JOSM session
 * @since 18466
 */
public class SessionSaveAction extends DiskAccessAction implements MapFrameListener, LayerChangeListener {

    private transient List<Layer> layers;
    private transient Map<Layer, SessionLayerExporter> exporters;
    private transient MultiMap<Layer, Layer> dependencies;

    private static final BooleanProperty SAVE_LOCAL_FILES_PROPERTY = new BooleanProperty("session.savelocal", true);
    private static final BooleanProperty SAVE_PLUGIN_INFORMATION_PROPERTY = new BooleanProperty("session.saveplugins", false);
    private static final String TOOLTIP_DEFAULT = tr("Save the current session.");

    protected transient FileFilter joz = new ExtensionFileFilter("joz", "joz", tr("Session file (archive) (*.joz)"));
    protected transient FileFilter jos = new ExtensionFileFilter("jos", "jos", tr("Session file (*.jos)"));

    private File removeFileOnSuccess;

    private static String tooltip = TOOLTIP_DEFAULT;
    static File sessionFile;
    static boolean isZipSessionFile;
    private static boolean pluginData;
    static List<WeakReference<Layer>> layersInSessionFile;

    private static final SessionSaveAction instance = new SessionSaveAction();

    /**
     * Returns the instance
     * @return the instance
     */
    public static SessionSaveAction getInstance() {
        return instance;
    }

    /**
     * Constructs a new {@code SessionSaveAction}.
     */
    public SessionSaveAction() {
        this(true, false);
        updateEnabledState();
    }

    /**
     * Constructs a new {@code SessionSaveAction}.
     * @param toolbar Register this action for the toolbar preferences?
     * @param installAdapters False, if you don't want to install layer changed and selection changed adapters
     */
    protected SessionSaveAction(boolean toolbar, boolean installAdapters) {
        this(tr("Save Session"), "session", TOOLTIP_DEFAULT,
                Shortcut.registerShortcut("system:savesession", tr("File: {0}", tr("Save Session...")), KeyEvent.VK_S, Shortcut.ALT_CTRL),
                toolbar, "save-session", installAdapters);
        setHelpId(ht("/Action/SessionSave"));
    }

    protected SessionSaveAction(String name, String iconName, String tooltip,
            Shortcut shortcut, boolean register, String toolbarId, boolean installAdapters) {

        super(name, iconName, tooltip, shortcut, register, toolbarId, installAdapters);
        addListeners();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            saveSession(false, false);
        } catch (UserCancelException exception) {
            Logging.trace(exception);
        }
    }

    @Override
    public void destroy() {
        removeListeners();
        super.destroy();
    }

    /**
     * Attempts to save the session.
     * @param saveAs true shows the dialog
     * @param forceSaveAll saves all layers
     * @return if the session and all layers were successfully saved
     * @throws UserCancelException when the user has cancelled the save process
     */
    public boolean saveSession(boolean saveAs, boolean forceSaveAll) throws UserCancelException {
        try {
            return saveSessionImpl(saveAs, forceSaveAll);
        } finally {
            cleanup();
        }
    }

    private boolean saveSessionImpl(boolean saveAs, boolean forceSaveAll) throws UserCancelException {
        if (!isEnabled()) {
            return false;
        }

        removeFileOnSuccess = null;

        SessionSaveAsDialog dlg = new SessionSaveAsDialog();
        if (saveAs) {
            dlg.showDialog();
            if (dlg.getValue() != 1) {
                throw new UserCancelException();
            }
        }

        // TODO: resolve dependencies for layers excluded by the user
        List<Layer> layersOut = layers.stream()
                .filter(layer -> exporters.get(layer) != null && exporters.get(layer).shallExport())
                .collect(Collectors.toList());

        boolean zipRequired = layersOut.stream().map(l -> exporters.get(l))
                .anyMatch(ex -> ex != null && ex.requiresZip()) || pluginsWantToSave();

        saveAs = !doGetFile(saveAs, zipRequired);

        String fn = sessionFile.getName();

        if (!saveAs && layersInSessionFile != null) {
            List<String> missingLayers = layersInSessionFile.stream()
                    .map(WeakReference::get)
                    .filter(Objects::nonNull)
                    .filter(l -> !layersOut.contains(l))
                    .map(Layer::getName)
                    .collect(Collectors.toList());

            if (!missingLayers.isEmpty() &&
                    !ConditionalOptionPaneUtil.showConfirmationDialog(
                            "savesession_layerremoved",
                            null,
                            new JLabel("<html>"
                                    + trn("The following layer has been removed since the session was last saved:",
                                          "The following layers have been removed since the session was last saved:", missingLayers.size())
                                    + "<ul><li>"
                                    + String.join("<li>", missingLayers)
                                    + "</ul><br>"
                                    + tr("You are about to overwrite the session file \"{0}\". Would you like to proceed?", fn)),
                            tr("Layers removed"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                            JOptionPane.OK_OPTION)) {
                throw new UserCancelException();
            }
        }
        setCurrentLayers(layersOut);

        updateSessionFile(fn);

        Stream<Layer> layersToSaveStream = layersOut.stream()
                .filter(layer -> layer.isSavable()
                        && layer instanceof AbstractModifiableLayer
                        && ((AbstractModifiableLayer) layer).requiresSaveToFile()
                        && exporters.get(layer) != null
                        && !exporters.get(layer).requiresZip());

        boolean success = true;
        if (forceSaveAll || Boolean.TRUE.equals(SAVE_LOCAL_FILES_PROPERTY.get())) {
            // individual files must be saved before the session file as the location may change
            if (layersToSaveStream
                .map(layer -> SaveAction.getInstance().doSave(layer, true))
                .collect(Collectors.toList()) // force evaluation of all elements
                .contains(false)) {

                new Notification(tr("Not all local files referenced by the session file could be saved."
                        + "<br>Make sure you save them before closing JOSM."))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .setDuration(Notification.TIME_LONG)
                    .show();
                success = false;
            }
        } else if (layersToSaveStream.anyMatch(l -> true)) {
            new Notification(tr("Not all local files referenced by the session file are saved yet."
                    + "<br>Make sure you save them before closing JOSM."))
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .setDuration(Notification.TIME_LONG)
                .show();
        }

        int active = -1;
        Layer activeLayer = getLayerManager().getActiveLayer();
        if (activeLayer != null) {
            active = layersOut.indexOf(activeLayer);
        }

        final EnumSet<SessionWriter.SessionWriterFlags> flags = EnumSet.noneOf(SessionWriter.SessionWriterFlags.class);
        if (pluginData || (Boolean.TRUE.equals(SAVE_PLUGIN_INFORMATION_PROPERTY.get()) && pluginsWantToSave())) {
            flags.add(SessionWriter.SessionWriterFlags.SAVE_PLUGIN_INFORMATION);
        }
        if (isZipSessionFile) {
            flags.add(SessionWriter.SessionWriterFlags.IS_ZIP);
        }
        SessionWriter sw = new SessionWriter(layersOut, active, exporters, dependencies, flags.toArray(new SessionWriter.SessionWriterFlags[0]));
        try {
            Notification savingNotification = showSavingNotification(sessionFile.getName());
            sw.write(sessionFile);
            SaveActionBase.addToFileOpenHistory(sessionFile);
            if (removeFileOnSuccess != null) {
                PreferencesUtils.removeFromList(Config.getPref(), "file-open.history", removeFileOnSuccess.getCanonicalPath());
                Files.deleteIfExists(removeFileOnSuccess.toPath());
                removeFileOnSuccess = null;
            }
            showSavedNotification(savingNotification, sessionFile.getName());
        } catch (SecurityException ex) {
            Logging.error(ex);
            if (removeFileOnSuccess != null) {
                final String path = removeFileOnSuccess.getPath();
                GuiHelper.runInEDT(() -> {
                    Notification notification = new Notification(tr("Could not delete file: {0}<br>{1}", path, ex.getMessage()));
                    notification.setIcon(JOptionPane.WARNING_MESSAGE);
                    notification.show();
                });
            } else {
                // We should never hit this, unless something changes in the try block.
                throw new JosmRuntimeException(ex);
            }
        } catch (IOException ex) {
            Logging.error(ex);
            HelpAwareOptionPane.showMessageDialogInEDT(
                    MainApplication.getMainFrame(),
                    tr("<html>Could not save session file ''{0}''.<br>Error is:<br>{1}</html>",
                            sessionFile.getName(), Utils.escapeReservedCharactersHTML(ex.getMessage())),
                    tr("IO Error"),
                    JOptionPane.ERROR_MESSAGE,
                    null
            );
            success = false;
        }
        return success;
    }

    /**
     * Sets the current session file. Asks the user if necessary
     * @param saveAs always ask the user
     * @param zipRequired zip
     * @return if the user was asked
     * @throws UserCancelException when the user has cancelled the save process
     */
    protected boolean doGetFile(boolean saveAs, boolean zipRequired) throws UserCancelException {
        if (!saveAs && sessionFile != null) {

            if (isZipSessionFile || !zipRequired)
                return true;

            Logging.info("Converting *.jos to *.joz because a new layer has been added that requires zip format");
            String oldPath = sessionFile.getAbsolutePath();
            int i = oldPath.lastIndexOf('.');
            File jozFile = new File(i < 0 ? oldPath : oldPath.substring(0, i) + ".joz");
            if (!jozFile.exists()) {
                removeFileOnSuccess = sessionFile;
                setCurrentSession(jozFile, true);
                return true;
            }
            Logging.warn("Asking user to choose a new location for the *.joz file because it already exists");
        }

        doGetFileChooser(zipRequired);
        return false;
    }

    protected void doGetFileChooser(boolean zipRequired) throws UserCancelException {
        AbstractFileChooser fc;

        if (zipRequired) {
            fc = createAndOpenFileChooser(false, false, tr("Save Session"), joz, JFileChooser.FILES_ONLY, "lastDirectory");
        } else {
            fc = createAndOpenFileChooser(false, false, tr("Save Session"), Arrays.asList(jos, joz), jos,
                    JFileChooser.FILES_ONLY, "lastDirectory");
        }

        if (fc == null) {
            throw new UserCancelException();
        }

        File f = fc.getSelectedFile();
        FileFilter ff = fc.getFileFilter();
        boolean zip;

        if (zipRequired || joz.equals(ff)) {
            zip = true;
        } else if (jos.equals(ff)) {
            zip = false;
        } else {
            zip = Utils.hasExtension(f.getName(), "joz");
        }
        setCurrentSession(f, zip);
    }

    /**
     * The "Save Session" dialog
     */
    public class SessionSaveAsDialog extends ExtendedDialog {

        /**
         * Constructs a new {@code SessionSaveAsDialog}.
         */
        public SessionSaveAsDialog() {
            super(MainApplication.getMainFrame(), tr("Save Session"), tr("Save As"), tr("Cancel"));
            configureContextsensitiveHelp("Action/SessionSaveAs", true /* show help button */);
            initialize();
            setButtonIcons("save_as", "cancel");
            setDefaultButton(1);
            setRememberWindowGeometry(getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(MainApplication.getMainFrame(), new Dimension(450, 450)));
            setContent(build(), false);
        }

        /**
         * Initializes some action fields.
         */
        private void initialize() {
            layers = new ArrayList<>(getLayerManager().getLayers());
            exporters = new HashMap<>();
            dependencies = new MultiMap<>();

            Set<Layer> noExporter = new HashSet<>();

            for (Layer layer : layers) {
                SessionLayerExporter exporter = null;
                try {
                    exporter = SessionWriter.getSessionLayerExporter(layer);
                } catch (IllegalArgumentException | JosmRuntimeException e) {
                    Logging.error(e);
                }
                if (exporter != null) {
                    exporters.put(layer, exporter);
                    Collection<Layer> deps = exporter.getDependencies();
                    if (deps != null) {
                        dependencies.putAll(layer, deps);
                    } else {
                        dependencies.putVoid(layer);
                    }
                } else {
                    noExporter.add(layer);
                    exporters.put(layer, null);
                }
            }

            int numNoExporter = 0;
            while (numNoExporter != noExporter.size()) {
                numNoExporter = noExporter.size();
                updateExporters(noExporter);
            }
        }

        private void updateExporters(Collection<Layer> noExporter) {
            for (Layer layer : layers) {
                if (noExporter.contains(layer)) continue;
                for (Layer depLayer : dependencies.get(layer)) {
                    if (noExporter.contains(depLayer)) {
                        noExporter.add(layer);
                        exporters.put(layer, null);
                        return;
                    }
                }
            }
        }

        protected final Component build() {
            JPanel op = new JPanel(new GridBagLayout());
            JPanel ip = new JPanel(new GridBagLayout());
            for (Layer layer : layers) {
                Component exportPanel;
                SessionLayerExporter exporter = exporters.get(layer);
                if (exporter == null) {
                    if (!exporters.containsKey(layer)) throw new AssertionError();
                    exportPanel = getDisabledExportPanel(layer);
                } else {
                    exportPanel = exporter.getExportPanel();
                }
                if (exportPanel == null) continue;
                JPanel wrapper = new JPanel(new GridBagLayout());
                wrapper.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
                wrapper.add(exportPanel, GBC.std().fill(GBC.HORIZONTAL));
                ip.add(wrapper, GBC.eol().fill(GBC.HORIZONTAL).insets(2, 2, 4, 2));
            }
            ip.add(GBC.glue(0, 1), GBC.eol().fill(GBC.VERTICAL));
            JScrollPane sp = new JScrollPane(ip);
            sp.setBorder(BorderFactory.createEmptyBorder());
            JPanel p = new JPanel(new GridBagLayout());
            p.add(sp, GBC.eol().fill());
            final JTabbedPane tabs = new JTabbedPane();
            tabs.addTab(tr("Layers"), p);
            op.add(tabs, GBC.eol().fill());
            JCheckBox chkSaveLocal = new JCheckBox(tr("Save all local files to disk"), SAVE_LOCAL_FILES_PROPERTY.get());
            chkSaveLocal.addChangeListener(l -> SAVE_LOCAL_FILES_PROPERTY.put(chkSaveLocal.isSelected()));
            op.add(chkSaveLocal, GBC.eol());
            if (pluginsWantToSave()) {
                JCheckBox chkSavePlugins = new JCheckBox(tr("Save plugin information to disk"), SAVE_PLUGIN_INFORMATION_PROPERTY.get());
                chkSavePlugins.addChangeListener(l -> SAVE_PLUGIN_INFORMATION_PROPERTY.put(chkSavePlugins.isSelected()));
                chkSavePlugins.setToolTipText(tr("Plugins may have additional information that can be saved"));
                op.add(chkSavePlugins, GBC.eol());
            }
            return op;
        }

        protected final Component getDisabledExportPanel(Layer layer) {
            JPanel p = new JPanel(new GridBagLayout());
            JCheckBox include = new JCheckBox();
            include.setEnabled(false);
            JLabel lbl = new JLabel(layer.getName(), layer.getIcon(), SwingConstants.LEADING);
            lbl.setToolTipText(tr("No exporter for this layer"));
            lbl.setLabelFor(include);
            lbl.setEnabled(false);
            p.add(include, GBC.std());
            p.add(lbl, GBC.std());
            p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
            return p;
        }
    }

    protected void addListeners() {
        MainApplication.addMapFrameListener(this);
        MainApplication.getLayerManager().addLayerChangeListener(this);
    }

    protected void removeListeners() {
        MainApplication.removeMapFrameListener(this);
        MainApplication.getLayerManager().removeLayerChangeListener(this);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(MainApplication.isDisplayingMapView());
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        updateEnabledState();
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        // not used
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (e.isLastLayer()) {
            setCurrentSession(null, false);
        }
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // not used
    }

    /**
     * Update the session file
     * @param fileName The filename to use. If there are no periods in the file, we update the extension.
     * @throws UserCancelException If the user does not want to overwrite a previously existing file.
     */
    private static void updateSessionFile(String fileName) throws UserCancelException {
        if (fileName.indexOf('.') == -1) {
            sessionFile = new File(sessionFile.getPath() + (isZipSessionFile ? ".joz" : ".jos"));
            if (!SaveActionBase.confirmOverwrite(sessionFile)) {
                throw new UserCancelException();
            }
        }
    }

    /**
     * Sets the current session file and the layers included in that file
     * @param file file
     * @param zip if it is a zip session file
     * @param layers layers that are currently represented in the session file
     * @deprecated since 18833, use {@link #setCurrentSession(File, List, SessionWriter.SessionWriterFlags...)} instead
     */
    @Deprecated
    public static void setCurrentSession(File file, boolean zip, List<Layer> layers) {
        if (zip) {
            setCurrentSession(file, layers, SessionWriter.SessionWriterFlags.IS_ZIP);
        } else {
            setCurrentSession(file, layers);
        }
    }

    /**
     * Sets the current session file and the layers included in that file
     * @param file file
     * @param layers layers that are currently represented in the session file
     * @param flags The flags for the current session
     * @since 18833
     */
    public static void setCurrentSession(File file, List<Layer> layers, SessionWriter.SessionWriterFlags... flags) {
        final EnumSet<SessionWriter.SessionWriterFlags> flagSet = EnumSet.noneOf(SessionWriter.SessionWriterFlags.class);
        flagSet.addAll(Arrays.asList(flags));
        setCurrentSession(file, layers, flagSet);
    }

    /**
     * Sets the current session file and the layers included in that file
     * @param file file
     * @param layers layers that are currently represented in the session file
     * @param flags The flags for the current session
     * @since 18833
     */
    public static void setCurrentSession(File file, List<Layer> layers, Set<SessionWriter.SessionWriterFlags> flags) {
        setCurrentLayers(layers);
        setCurrentSession(file, flags.contains(SessionWriter.SessionWriterFlags.IS_ZIP));
        pluginData = flags.contains(SessionWriter.SessionWriterFlags.SAVE_PLUGIN_INFORMATION);
    }

    /**
     * Sets the current session file
     * @param file file
     * @param zip if it is a zip session file
     */
    public static void setCurrentSession(File file, boolean zip) {
        sessionFile = file;
        isZipSessionFile = zip;
        if (file == null) {
            tooltip = TOOLTIP_DEFAULT;
        } else {
            tooltip = tr("Save the current session file \"{0}\".", file.getName());
        }
        getInstance().setTooltip(tooltip);
    }

    /**
     * Sets the layers that are currently represented in the session file
     * @param layers layers
     */
    public static void setCurrentLayers(List<Layer> layers) {
        layersInSessionFile = layers.stream()
                .filter(AbstractModifiableLayer.class::isInstance)
                .map(WeakReference::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns the tooltip for the component
     * @return the tooltip for the component
     */
    public static String getTooltip() {
        return tooltip;
    }

    /**
     * Check to see if any plugins want to save their state
     * @return {@code true} if the plugin wants to save their state
     */
    private static boolean pluginsWantToSave() {
        for (PluginSessionExporter exporter : PluginHandler.load(PluginSessionExporter.class)) {
            if (exporter.requiresSaving()) {
                return true;
            }
        }
        return false;
    }

    protected void cleanup() {
        layers = null;
        exporters = null;
        dependencies = null;
    }

}
