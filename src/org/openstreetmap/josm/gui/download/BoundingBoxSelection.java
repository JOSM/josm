// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * Bounding box selector.
 *
 * Provides max/min lat/lon input fields as well as the "URL from www.openstreetmap.org" text field.
 *
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class BoundingBoxSelection implements DownloadSelection {

    private JTextField[] latlon = new JTextField[] {
            new JTextField(11),
            new JTextField(11),
            new JTextField(11),
            new JTextField(11) };
    final JTextArea osmUrl = new JTextArea();
    final JTextArea showUrl = new JTextArea();

    public void addGui(final DownloadDialog gui) {

        JPanel dlg = new JPanel(new GridBagLayout());

        final FocusListener dialogUpdater = new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            double minlat = Double.parseDouble(latlon[0].getText());
                            double minlon = Double.parseDouble(latlon[1].getText());
                            double maxlat = Double.parseDouble(latlon[2].getText());
                            double maxlon = Double.parseDouble(latlon[3].getText());
                            if (minlat != gui.minlat || minlon != gui.minlon || maxlat != gui.maxlat || maxlon != gui.maxlon) {
                                gui.minlat = minlat; gui.minlon = minlon;
                                gui.maxlat = maxlat; gui.maxlon = maxlon;
                                gui.boundingBoxChanged(BoundingBoxSelection.this);
                            }
                        } catch (NumberFormatException x) {
                            // ignore
                        }
                        updateUrl(gui);
                    }
                });
            }
        };

        for (JTextField f : latlon) {
            f.setMinimumSize(new Dimension(100,new JTextField().getMinimumSize().height));
            f.addFocusListener(dialogUpdater);
        }

        class osmUrlRefresher implements DocumentListener {
            public void changedUpdate(DocumentEvent e) { parseURL(gui); }
            public void insertUpdate(DocumentEvent e) { parseURL(gui); }
            public void removeUpdate(DocumentEvent e) { parseURL(gui); }
        }

        KeyListener osmUrlKeyListener = new KeyListener() {
            public void keyPressed(KeyEvent keyEvent) {}
            public void keyReleased(KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER && parseURL(gui))
                    gui.closeDownloadDialog(true);
            }
            public void keyTyped(KeyEvent keyEvent) {}
        };

        osmUrl.addKeyListener(osmUrlKeyListener);
        osmUrl.getDocument().addDocumentListener(new osmUrlRefresher());

        // select content on receiving focus. this seems to be the default in the
        // windows look+feel but not for others. needs invokeLater to avoid strange
        // side effects that will cancel out the newly made selection otherwise.
        osmUrl.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            osmUrl.selectAll();
                        }
                });
            }
        });
        osmUrl.setLineWrap(true);
        osmUrl.setBorder(latlon[0].getBorder());

        dlg.add(new JLabel(tr("min lat")), GBC.std().insets(10,20,5,0));
        dlg.add(latlon[0], GBC.std().insets(0,20,0,0));
        dlg.add(new JLabel(tr("min lon")), GBC.std().insets(10,20,5,0));
        dlg.add(latlon[1], GBC.eol().insets(0,20,0,0));
        dlg.add(new JLabel(tr("max lat")), GBC.std().insets(10,0,5,0));
        dlg.add(latlon[2], GBC.std());
        dlg.add(new JLabel(tr("max lon")), GBC.std().insets(10,0,5,0));
        dlg.add(latlon[3], GBC.eol());

        dlg.add(new JLabel(tr("URL from www.openstreetmap.org (you can paste an URL here to download the area)")), GBC.eol().insets(10,20,5,0));
        dlg.add(osmUrl, GBC.eop().insets(10,0,5,0).fill());
        dlg.add(showUrl, GBC.eop().insets(10,0,5,5));
        showUrl.setEditable(false);
        showUrl.setBackground(dlg.getBackground());
        showUrl.addFocusListener(new FocusAdapter(){
           @Override
            public void focusGained(FocusEvent e) {
                showUrl.selectAll();
            }
        });

        gui.tabpane.addTab(tr("Bounding Box"), dlg);
    }

    /**
     * Called when bounding box is changed by one of the other download dialog tabs.
     */
    public void boundingBoxChanged(DownloadDialog gui) {
        updateBboxFields(gui);
        updateUrl(gui);
    }

    private boolean parseURL(DownloadDialog gui) {
        Bounds b = OsmUrlToBounds.parse(osmUrl.getText());
        if(b == null) return false;
        gui.minlon = b.min.lon();
        gui.minlat = b.min.lat();
        gui.maxlon = b.max.lon();
        gui.maxlat = b.max.lat();
        gui.boundingBoxChanged(BoundingBoxSelection.this);
        updateBboxFields(gui);
        updateUrl(gui);
        return true;
    }

    private void updateBboxFields(DownloadDialog gui) {
        latlon[0].setText(Double.toString(gui.minlat));
        latlon[1].setText(Double.toString(gui.minlon));
        latlon[2].setText(Double.toString(gui.maxlat));
        latlon[3].setText(Double.toString(gui.maxlon));
        for (JTextField f : latlon)
            f.setCaretPosition(0);
    }

    private void updateUrl(DownloadDialog gui) {
        showUrl.setText(OsmUrlToBounds.getURL(new Bounds(
        new LatLon(gui.minlat, gui.minlon), new LatLon(gui.maxlat, gui.maxlon))));
    }
}
