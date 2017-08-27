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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrameListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.io.session.SessionLayerExporter;
import org.openstreetmap.josm.io.session.SessionWriter;
import org.openstreetmap.josm.tools.GBC;
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

    /**
     * Constructs a new {@code SessionSaveAsAction}.
     */
    public SessionSaveAsAction() {
        this(true, true);
    }

    /**
     * Constructs a new {@code SessionSaveAsAction}.
     * @param toolbar Register this action for the toolbar preferences?
     * @param installAdapters False, if you don't want to install layer changed and selection changed adapters
     */
    protected SessionSaveAsAction(boolean toolbar, boolean installAdapters) {
        super(tr("Save Session As..."), "session", tr("Save the current session to a new file."),
                null, toolbar, "save_as-session", installAdapters);
        putValue("help", ht("/Action/SessionSaveAs"));
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

        boolean zipRequired = false;
        for (Layer l : layers) {
            SessionLayerExporter ex = exporters.get(l);
            if (ex != null && ex.requiresZip()) {
                zipRequired = true;
                break;
            }
        }

        FileFilter joz = new ExtensionFileFilter("joz", "joz", tr("Session file (archive) (*.joz)"));
        FileFilter jos = new ExtensionFileFilter("jos", "jos", tr("Session file (*.jos)"));

        AbstractFileChooser fc;

        if (zipRequired) {
            fc = createAndOpenFileChooser(false, false, tr("Save session"), joz, JFileChooser.FILES_ONLY, "lastDirectory");
        } else {
            fc = createAndOpenFileChooser(false, false, tr("Save session"), Arrays.asList(jos, joz), jos,
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

        List<Layer> layersOut = new ArrayList<>();
        for (Layer layer : layers) {
            if (exporters.get(layer) == null || !exporters.get(layer).shallExport()) continue;
            // TODO: resolve dependencies for layers excluded by the user
            layersOut.add(layer);
        }

        int active = -1;
        Layer activeLayer = getLayerManager().getActiveLayer();
        if (activeLayer != null) {
            active = layersOut.indexOf(activeLayer);
        }

        SessionWriter sw = new SessionWriter(layersOut, active, exporters, dependencies, zip);
        try {
            sw.write(file);
            SaveActionBase.addToFileOpenHistory(file);
        } catch (IOException ex) {
            Logging.error(ex);
            HelpAwareOptionPane.showMessageDialogInEDT(
                    Main.parent,
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
            super(Main.parent, tr("Save Session"), tr("Save As"), tr("Cancel"));
            initialize();
            setButtonIcons("save_as", "cancel");
            setDefaultButton(1);
            setRememberWindowGeometry(getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(Main.parent, new Dimension(350, 450)));
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
                SessionLayerExporter exporter = SessionWriter.getSessionLayerExporter(layer);
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
            return tabs;
        }

        protected final Component getDisabledExportPanel(Layer layer) {
            JPanel p = new JPanel(new GridBagLayout());
            JCheckBox include = new JCheckBox();
            include.setEnabled(false);
            JLabel lbl = new JLabel(layer.getName(), layer.getIcon(), SwingConstants.LEFT);
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
