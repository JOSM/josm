// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.CardLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.session.SessionWriter.ExportSupport;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.w3c.dom.Element;

/**
 * Generic superclass of {@link OsmDataSessionExporter} and {@link GpxTracksSessionExporter} layer exporters.
 * @param <T> Type of exported layer
 * @since 9470
 */
public abstract class GenericSessionExporter<T extends Layer> extends AbstractSessionExporter<T> {

    private final String type;
    private final String version;
    private final String extension;

    private final JRadioButton link;
    private final JRadioButton include;

    /**
     * Constructs a new {@code GenericSessionExporter}.
     * @param layer layer to export
     * @param type layer session type
     * @param version layer session version
     * @param extension data file extension
     */
    protected GenericSessionExporter(T layer, String type, String version, String extension) {
        super(layer);
        this.type = type;
        this.version = version;
        this.extension = extension;
        /* I18n: Refer to a OSM/GPX data file in session file */
        this.link = new JRadioButton(tr("local file"));
        /* I18n: Include OSM/GPX data in session file */
        this.include = new JRadioButton(tr("include"));
    }

    private class LayerSaveAction extends AbstractAction {
        /**
         * Constructs a new {@code LayerSaveAction}.
         */
        LayerSaveAction() {
            new ImageProvider("save").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, ((AbstractModifiableLayer) layer).requiresSaveToFile() ?
                    tr("Layer contains unsaved data - save to file.") :
                    tr("Layer does not contain unsaved data."));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SaveAction.getInstance().doSave(layer);
            updateEnabledState();
        }

        public final void updateEnabledState() {
            setEnabled(((AbstractModifiableLayer) layer).requiresSaveToFile());
        }
    }

    @Override
    public JPanel getExportPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        JPanel topRow = new JPanel(new GridBagLayout());
        export.setSelected(true);
        final JLabel lbl = new JLabel(layer.getName(), layer.getIcon(), SwingConstants.LEFT);
        lbl.setToolTipText(layer.getToolTipText());
        lbl.setLabelFor(export);
        JLabel lblData = new JLabel(tr("Data:"));
        link.putClientProperty("actionname", "link");
        if (layer instanceof OsmDataLayer) {
            link.setToolTipText(tr("Link to a OSM data file on your local disk."));
            include.setToolTipText(tr("Include OSM data in the .joz session file."));
        } else if (layer instanceof GpxLayer) {
            link.setToolTipText(tr("Link to a GPX data file on your local disk."));
            include.setToolTipText(tr("Include GPX data in the .joz session file."));
        }
        include.putClientProperty("actionname", "include");
        ButtonGroup group = new ButtonGroup();
        group.add(link);
        group.add(include);

        JPanel cardLink = new JPanel(new GridBagLayout());
        final File file = layer.getAssociatedFile();
        final boolean modifiable = layer instanceof AbstractModifiableLayer;
        final LayerSaveAction saveAction = modifiable ? new LayerSaveAction() : null;
        final JButton save = modifiable ? new JButton(saveAction) : null;
        if (file != null && file.exists()) {
            JosmTextField tf = new JosmTextField();
            tf.setText(file.getPath());
            tf.setEditable(false);
            cardLink.add(tf, GBC.std());
            if (save != null) {
                save.setMargin(new Insets(0, 0, 0, 0));
                cardLink.add(save, GBC.eol().insets(2, 0, 0, 0));
            }
        } else {
            cardLink.add(new JLabel(tr("No file association")), GBC.eol());
        }

        JPanel cardInclude = new JPanel(new GridBagLayout());
        JLabel lblIncl = new JLabel(layer instanceof GpxLayer ?
                tr("GPX data will be included in the session file.") :
                tr("OSM data will be included in the session file."));
        lblIncl.setFont(lblIncl.getFont().deriveFont(Font.PLAIN));
        cardInclude.add(lblIncl, GBC.eol().fill(GBC.HORIZONTAL));

        final CardLayout cl = new CardLayout();
        final JPanel cards = new JPanel(cl);
        cards.add(cardLink, "link");
        cards.add(cardInclude, "include");

        if (file != null && file.exists()) {
            link.setSelected(true);
        } else {
            link.setEnabled(false);
            link.setToolTipText(tr("No file association"));
            include.setSelected(true);
            cl.show(cards, "include");
        }

        link.addActionListener(e -> cl.show(cards, "link"));
        include.addActionListener(e -> cl.show(cards, "include"));

        topRow.add(export, GBC.std());
        topRow.add(lbl, GBC.std());
        topRow.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        p.add(topRow, GBC.eol().fill(GBC.HORIZONTAL));
        p.add(lblData, GBC.std().insets(10, 0, 0, 0));
        p.add(link, GBC.std());
        p.add(include, GBC.eol());
        p.add(cards, GBC.eol().insets(15, 0, 3, 3));

        export.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.DESELECTED) {
                GuiHelper.setEnabledRec(p, false);
                export.setEnabled(true);
            } else {
                GuiHelper.setEnabledRec(p, true);
                if (save != null && saveAction != null) {
                    save.setEnabled(saveAction.isEnabled());
                }
                link.setEnabled(file != null && file.exists());
            }
        });
        return p;
    }

    @Override
    public Element export(ExportSupport support) throws IOException {
        Element layerEl = support.createElement("layer");
        layerEl.setAttribute("type", type);
        layerEl.setAttribute("version", version);

        Element file = support.createElement("file");
        layerEl.appendChild(file);

        if (requiresZip()) {
            String zipPath = "layers/" + String.format("%02d", support.getLayerIndex()) + "/data." + extension;
            file.appendChild(support.createTextNode(zipPath));
            addDataFile(support.getOutputStreamZip(zipPath));
        } else {
            File f = layer.getAssociatedFile();
            if (f != null) {
                final Path sessionDirectory = support.getOutput().getParent();
                final String fileString;
                if (f.toPath().startsWith(sessionDirectory)) {
                    fileString = sessionDirectory.relativize(f.toPath()).toString();
                } else {
                    fileString = f.toPath().toString();
                }
                file.appendChild(support.createTextNode(fileString));
            }
        }
        return layerEl;
    }

    @Override
    public boolean requiresZip() {
        return include.isSelected();
    }

    protected abstract void addDataFile(OutputStream out) throws IOException;
}
