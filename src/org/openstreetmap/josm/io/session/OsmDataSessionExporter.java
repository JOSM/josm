// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.CardLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.w3c.dom.Element;

import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.io.session.SessionWriter.ExportSupport;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageRequest;

public class OsmDataSessionExporter implements SessionLayerExporter {

    private OsmDataLayer layer;

    public OsmDataSessionExporter(OsmDataLayer layer) {
        this.layer = layer;
    }

    public OsmDataSessionExporter() {
    }

    public OsmDataSessionExporter newInstance(OsmDataLayer layer) {
        return new OsmDataSessionExporter(layer);
    }

    @Override
    public Collection<Layer> getDependencies() {
        return Collections.emptySet();
    }

    private class LayerSaveAction extends AbstractAction {
        public LayerSaveAction() {
            putValue(SMALL_ICON, new ImageRequest().setName("save").setWidth(16).get());
            putValue(SHORT_DESCRIPTION, tr("Layer contains unsaved data - save to file."));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            new SaveAction().doSave(layer);
            updateEnabledState();
        }

        public void updateEnabledState() {
            setEnabled(layer.requiresSaveToFile());
        }
    }

    private JRadioButton link, include;
    private JCheckBox export;

    @Override
    public JPanel getExportPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        JPanel topRow = new JPanel(new GridBagLayout());
        export = new JCheckBox();
        export.setSelected(true);
        final JLabel lbl = new JLabel(layer.getName(), layer.getIcon(), SwingConstants.LEFT);
        lbl.setToolTipText(layer.getToolTipText());

        JLabel lblData = new JLabel(tr("Data:"));
        /* I18n: Refer to a OSM data file in session file */ link = new JRadioButton(tr("local file"));
        link.putClientProperty("actionname", "link");
        link.setToolTipText(tr("Link to a OSM data file on your local disk."));
        /* I18n: Include OSM data in session file */ include = new JRadioButton(tr("include"));
        include.setToolTipText(tr("Include OSM data in the .joz session file."));
        include.putClientProperty("actionname", "include");
        ButtonGroup group = new ButtonGroup();
        group.add(link);
        group.add(include);

        JPanel cardLink = new JPanel(new GridBagLayout());
        final File file = layer.getAssociatedFile();
        final LayerSaveAction saveAction = new LayerSaveAction();
        final JButton save = new JButton(saveAction);
        if (file != null) {
            JTextField tf = new JTextField();
            tf.setText(file.getPath());
            tf.setEditable(false);
            cardLink.add(tf, GBC.std());
            save.setMargin(new Insets(0,0,0,0));
            cardLink.add(save, GBC.eol().insets(2,0,0,0));
        } else {
            cardLink.add(new JLabel(tr("No file association")), GBC.eol());
        }

        JPanel cardInclude = new JPanel(new GridBagLayout());
        JLabel lblIncl = new JLabel(tr("OSM data will be included in the session file."));
        lblIncl.setFont(lblIncl.getFont().deriveFont(Font.PLAIN));
        cardInclude.add(lblIncl, GBC.eol().fill(GBC.HORIZONTAL));

        final CardLayout cl = new CardLayout();
        final JPanel cards = new JPanel(cl);
        cards.add(cardLink, "link");
        cards.add(cardInclude, "include");

        if (file != null) {
            link.setSelected(true);
        } else {
            link.setEnabled(false);
            link.setToolTipText(tr("No file association"));
            include.setSelected(true);
            cl.show(cards, "include");
        }

        link.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cl.show(cards, "link");
            }
        });
        include.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cl.show(cards, "include");
            }
        });

        topRow.add(export, GBC.std());
        topRow.add(lbl, GBC.std());
        topRow.add(GBC.glue(1,0), GBC.std().fill(GBC.HORIZONTAL));
        p.add(topRow, GBC.eol().fill(GBC.HORIZONTAL));
        p.add(lblData, GBC.std().insets(10,0,0,0));
        p.add(link, GBC.std());
        p.add(include, GBC.eol());
        p.add(cards, GBC.eol().insets(15,0,3,3));

        export.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    GuiHelper.setEnabledRec(p, false);
                    export.setEnabled(true);
                } else {
                    GuiHelper.setEnabledRec(p, true);
                    save.setEnabled(saveAction.isEnabled());
                    link.setEnabled(file != null);
                }
            }
        });
        return p;
    }

    @Override
    public boolean shallExport() {
        return export.isSelected();
    }

    @Override
    public boolean requiresZip() {
        return include.isSelected();
    }

    @Override
    public Element export(ExportSupport support) throws IOException {
        Element layerEl = support.createElement("layer");
        layerEl.setAttribute("type", "osm-data");
        layerEl.setAttribute("version", "0.1");

        Element file = support.createElement("file");
        layerEl.appendChild(file);

        if (requiresZip()) {
            String zipPath = "layers/" + String.format("%02d", support.getLayerIndex()) + "/data.osm";
            file.appendChild(support.createTextNode(zipPath));
            addDataFile(support.getOutputStreamZip(zipPath));
        } else {
            URI uri = layer.getAssociatedFile().toURI();
            URL url = null;
            try {
                url = uri.toURL();
            } catch (MalformedURLException e) {
                throw new IOException(e);
            }
            file.appendChild(support.createTextNode(url.toString()));
        }
        return layerEl;
    }

    protected void addDataFile(OutputStream out) throws IOException {
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(out, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        OsmWriter w = OsmWriterFactory.createOsmWriter(new PrintWriter(writer), false, layer.data.getVersion());
        layer.data.getReadLock().lock();
        try {
            w.header();
            w.writeDataSources(layer.data);
            w.writeContent(layer.data);
            w.footer();
            w.flush();
        } finally {
            layer.data.getReadLock().unlock();
        }
    }
}

