// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.io.session.SessionWriter.ExportSupport;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;
import org.w3c.dom.Element;

public class GpxTracksSessionExporter implements SessionLayerExporter {

    private GpxLayer layer;
    private JRadioButton link, include;
    private JCheckBox export;

    public GpxTracksSessionExporter(GpxLayer layer) {
        this.layer = layer;
    }

    @Override
    public Collection<Layer> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public Component getExportPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        JPanel topRow = new JPanel(new GridBagLayout());
        export = new JCheckBox();
        export.setSelected(true);
        final JLabel lbl = new JLabel(layer.getName(), layer.getIcon(), SwingConstants.LEFT);
        lbl.setToolTipText(layer.getToolTipText());

        JLabel lblData = new JLabel(tr("Data:"));
        /* I18n: Refer to a OSM data file in session file */ link = new JRadioButton(tr("local file"));
        link.putClientProperty("actionname", "link");
        link.setToolTipText(tr("Link to a GPX file on your local disk."));
        /* I18n: Include OSM data in session file */ include = new JRadioButton(tr("include"));
        include.setToolTipText(tr("Include GPX data in the .joz session file."));
        include.putClientProperty("actionname", "include");
        ButtonGroup group = new ButtonGroup();
        group.add(link);
        group.add(include);

        JPanel cardLink = new JPanel(new GridBagLayout());
        final File file = layer.getAssociatedFile();
        if (file != null) {
            JosmTextField tf = new JosmTextField();
            tf.setText(file.getPath());
            tf.setEditable(false);
            cardLink.add(tf, GBC.std());
        } else {
            cardLink.add(new JLabel(tr("No file association")), GBC.eol());
        }

        JPanel cardInclude = new JPanel(new GridBagLayout());
        JLabel lblIncl = new JLabel(tr("GPX data will be included in the session file."));
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
            @Override
            public void actionPerformed(ActionEvent e) {
                cl.show(cards, "link");
            }
        });
        include.addActionListener(new ActionListener() {
            @Override
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
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    GuiHelper.setEnabledRec(p, false);
                    export.setEnabled(true);
                } else {
                    GuiHelper.setEnabledRec(p, true);
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
        layerEl.setAttribute("type", "tracks");
        layerEl.setAttribute("version", "0.1");

        Element file = support.createElement("file");
        layerEl.appendChild(file);

        if (requiresZip()) {
            String zipPath = "layers/" + String.format("%02d", support.getLayerIndex()) + "/data.gpx";
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
        Writer writer = new OutputStreamWriter(out, Utils.UTF_8);
        GpxWriter w = new GpxWriter(new PrintWriter(writer));
        w.write(layer.data);
        w.flush();
    }

}
