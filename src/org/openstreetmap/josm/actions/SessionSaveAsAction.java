// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrameListener;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.io.session.SessionLayerExporter;
import org.openstreetmap.josm.io.session.SessionWriter;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.UserCancelException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Saves a JOSM session
 * @since 4685
 */
public class SessionSaveAsAction extends DiskAccessAction implements MapFrameListener {

    private transient List<Layer> layers;
    private transient Map<Layer, SessionLayerExporter> exporters;
    private transient MultiMap<Layer, Layer> dependencies;

    private static final BooleanProperty SAVE_LOCAL_FILES_PROPERTY = new BooleanProperty("session.savelocal", true);

    /**
     * Constructs a new {@code SessionSaveAsAction}.
     */
    public SessionSaveAsAction() {
        this(true, false);
        updateEnabledState();
    }

    /**
     * Constructs a new {@code SessionSaveAsAction}.
     * @param toolbar Register this action for the toolbar preferences?
     * @param installAdapters False, if you don't want to install layer changed and selection changed adapters
     */
    protected SessionSaveAsAction(boolean toolbar, boolean installAdapters) {
        super(tr("Save Session As..."), "session", tr("Save the current session to a new file."),
                null, toolbar, "save_as-session", installAdapters);
        setHelpId(ht("/Action/SessionSaveAs"));
        MainApplication.addMapFrameListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            saveSession();
        } catch (UserCancelException ignore) {
            Logging.trace(ignore);
        }
    }

    @Override
    public void destroy() {
        MainApplication.removeMapFrameListener(this);
        super.destroy();
    }

    /**
     * Attempts to save the session.
     * @throws UserCancelException when the user has cancelled the save process.
     * @since 8913
     */
    public void saveSession() throws UserCancelException {
        if (!isEnabled()) {
            return;
        }

        SessionSaveAsDialog dlg = new SessionSaveAsDialog();
        dlg.showDialog();
        if (dlg.getValue() != 1) {
            throw new UserCancelException();
        }

        // TODO: resolve dependencies for layers excluded by the user
        List<Layer> layersOut = layers.stream()
                .filter(layer -> exporters.get(layer) != null && exporters.get(layer).shallExport())
                .collect(Collectors.toList());

        boolean zipRequired = layersOut.stream().map(exporters::get)
                .anyMatch(ex -> ex != null && ex.requiresZip());

        FileFilter joz = new ExtensionFileFilter("joz", "joz", tr("Session file (archive) (*.joz)"));
        FileFilter jos = new ExtensionFileFilter("jos", "jos", tr("Session file (*.jos)"));

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

        File file = fc.getSelectedFile();
        String fn = file.getName();

        boolean zip;
        FileFilter ff = fc.getFileFilter();
        if (zipRequired || joz.equals(ff)) {
            zip = true;
        } else if (jos.equals(ff)) {
            zip = false;
        } else {
            if (Utils.hasExtension(fn, "joz")) {
                zip = true;
            } else {
                zip = false;
            }
        }
        if (fn.indexOf('.') == -1) {
            file = new File(file.getPath() + (zip ? ".joz" : ".jos"));
            if (!SaveActionBase.confirmOverwrite(file)) {
                throw new UserCancelException();
            }
        }

        Stream<Layer> layersToSaveStream = layersOut.stream()
                .filter(layer -> layer.isSavable()
                        && layer instanceof AbstractModifiableLayer
                        && ((AbstractModifiableLayer) layer).requiresSaveToFile()
                        && exporters.get(layer) != null
                        && !exporters.get(layer).requiresZip());

        if (SAVE_LOCAL_FILES_PROPERTY.get()) {
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

        SessionWriter sw = new SessionWriter(layersOut, active, exporters, dependencies, zip);
        try {
            Notification savingNotification = showSavingNotification(file.getName());
            sw.write(file);
            SaveActionBase.addToFileOpenHistory(file);
            showSavedNotification(savingNotification, file.getName());
        } catch (IOException ex) {
            Logging.error(ex);
            HelpAwareOptionPane.showMessageDialogInEDT(
                    MainApplication.getMainFrame(),
                    tr("<html>Could not save session file ''{0}''.<br>Error is:<br>{1}</html>",
                            file.getName(), Utils.escapeReservedCharactersHTML(ex.getMessage())),
                    tr("IO Error"),
                    JOptionPane.ERROR_MESSAGE,
                    null
            );
        }
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
         * Initializes action.
         */
        public final void initialize() {
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
            WHILE: while (numNoExporter != noExporter.size()) {
                numNoExporter = noExporter.size();
                for (Layer layer : layers) {
                    if (noExporter.contains(layer)) continue;
                    for (Layer depLayer : dependencies.get(layer)) {
                        if (noExporter.contains(depLayer)) {
                            noExporter.add(layer);
                            exporters.put(layer, null);
                            break WHILE;
                        }
                    }
                }
            }
        }

        protected final Component build() {
            JPanel op = new JPanel(new GridBagLayout());
            JPanel ip = new JPanel(new GridBagLayout());
            for (Layer layer : layers) {
                JPanel wrapper = new JPanel(new GridBagLayout());
                wrapper.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
                Component exportPanel;
                SessionLayerExporter exporter = exporters.get(layer);
                if (exporter == null) {
                    if (!exporters.containsKey(layer)) throw new AssertionError();
                    exportPanel = getDisabledExportPanel(layer);
                } else {
                    exportPanel = exporter.getExportPanel();
                }
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
            chkSaveLocal.addChangeListener(l -> {
                SAVE_LOCAL_FILES_PROPERTY.put(chkSaveLocal.isSelected());
            });
            op.add(chkSaveLocal);
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

    @Override
    protected void updateEnabledState() {
        setEnabled(MainApplication.isDisplayingMapView());
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        updateEnabledState();
    }
}
