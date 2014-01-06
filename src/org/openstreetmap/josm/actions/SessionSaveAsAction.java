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
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.session.SessionLayerExporter;
import org.openstreetmap.josm.io.session.SessionWriter;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * Saves a JOSM session
 * @since 4685
 */
public class SessionSaveAsAction extends DiskAccessAction {

    private List<Layer> layers;
    private Map<Layer, SessionLayerExporter> exporters;
    private MultiMap<Layer, Layer> dependencies;

    /**
     * Constructs a new {@code SessionSaveAsAction}.
     */
    public SessionSaveAsAction() {
        super(tr("Save Session As..."), "save_as", tr("Save the current session to a new file."), null, true, "save_as-session", true);
        putValue("help", ht("/Action/SessionSaveAs"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }

        SessionSaveAsDialog dlg = new SessionSaveAsDialog();
        dlg.showDialog();
        if (dlg.getValue() != 1) return;

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

        JFileChooser fc;

        if (zipRequired) {
            fc = createAndOpenFileChooser(false, false, tr("Save session"), joz, JFileChooser.FILES_ONLY, "lastDirectory");
        } else {
            fc = createAndOpenFileChooser(false, false, tr("Save session"), Arrays.asList(new FileFilter[]{jos, joz}), jos, JFileChooser.FILES_ONLY, "lastDirectory");
        }

        if (fc == null)
            return;

        File file = fc.getSelectedFile();
        String fn = file.getName();

        boolean zip;
        FileFilter ff = fc.getFileFilter();
        if (zipRequired) {
            zip = true;
        } else if (ff == joz) {
            zip = true;
        } else if (ff == jos) {
            zip = false;
        } else {
            if (fn.toLowerCase().endsWith(".joz")) {
                zip = true;
            } else {
                zip = false;
            }
        }
        if (fn.indexOf('.') == -1) {
            file = new File(file.getPath() + (zip ? ".joz" : ".jos"));
            if (!SaveActionBase.confirmOverwrite(file))
                return;
        }

        List<Layer> layersOut = new ArrayList<Layer>();
        for (Layer layer : layers) {
            if (exporters.get(layer) == null || !exporters.get(layer).shallExport()) continue;
            // TODO: resolve dependencies for layers excluded by the user
            layersOut.add(layer);
        }
        
        int active = -1;
        Layer activeLayer = Main.map.mapView.getActiveLayer();
        if (activeLayer != null) {
            active = layersOut.indexOf(activeLayer);
        }

        SessionWriter sw = new SessionWriter(layersOut, active, exporters, dependencies, zip);
        try {
            sw.write(file);
        } catch (IOException ex) {
            Main.error(ex);
            HelpAwareOptionPane.showMessageDialogInEDT(
                    Main.parent,
                    tr("<html>Could not save session file ''{0}''.<br>Error is:<br>{1}</html>", file.getName(), ex.getMessage()),
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
            super(Main.parent, tr("Save Session"), new String[] {tr("Save As"), tr("Cancel")});
            initialize();
            setButtonIcons(new String[] {"save_as", "cancel"});
            setDefaultButton(1);
            setRememberWindowGeometry(getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(Main.parent, new Dimension(350, 450)));
            setContent(build(), false);
        }

        /**
         * Initializes action.
         */
        public void initialize() {
            layers = new ArrayList<Layer>(Main.map.mapView.getAllLayersAsList());
            exporters = new HashMap<Layer, SessionLayerExporter>();
            dependencies = new MultiMap<Layer, Layer>();

            Set<Layer> noExporter = new HashSet<Layer>();

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
            WHILE:while (numNoExporter != noExporter.size()) {
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

        protected Component build() {
            JPanel p = new JPanel(new GridBagLayout());
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
                ip.add(wrapper, GBC.eol().fill(GBC.HORIZONTAL).insets(2,2,4,2));
            }
            ip.add(GBC.glue(0,1), GBC.eol().fill(GBC.VERTICAL));
            JScrollPane sp = new JScrollPane(ip);
            sp.setBorder(BorderFactory.createEmptyBorder());
            p.add(sp, GBC.eol().fill());
            final JTabbedPane tabs = new JTabbedPane();
            tabs.addTab(tr("Layers"), p);
            return tabs;
        }

        protected Component getDisabledExportPanel(Layer layer) {
            JPanel p = new JPanel(new GridBagLayout());
            JCheckBox include = new JCheckBox();
            include.setEnabled(false);
            JLabel lbl = new JLabel(layer.getName(), layer.getIcon(), SwingConstants.LEFT);
            lbl.setToolTipText(tr("No exporter for this layer"));
            lbl.setEnabled(false);
            p.add(include, GBC.std());
            p.add(lbl, GBC.std());
            p.add(GBC.glue(1,0), GBC.std().fill(GBC.HORIZONTAL));
            return p;
        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.isDisplayingMapView());
    }
}
