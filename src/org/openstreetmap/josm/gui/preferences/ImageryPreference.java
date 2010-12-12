package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.io.imagery.HTMLGrabber;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;

public class ImageryPreference implements PreferenceSetting {
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ImageryPreference();
        }
    }
    ImageryProvidersPanel imageryProviders;

    // Common settings
    private Color colFadeColor;
    private JButton btnFadeColor;
    private JSlider fadeAmount = new JSlider(0, 100);
    private JComboBox sharpen;

    // WMS Settings
    private JComboBox browser;
    JCheckBox overlapCheckBox;
    JSpinner spinEast;
    JSpinner spinNorth;
    JSpinner spinSimConn;

    //TMS settings controls
    private JCheckBox autozoomActive = new JCheckBox();
    private JCheckBox autoloadTiles = new JCheckBox();
    private JSpinner minZoomLvl;
    private JSpinner maxZoomLvl;
    private JCheckBox addToSlippyMapChosser = new JCheckBox();

    private JPanel buildCommonSettingsPanel(final PreferenceTabbedPane gui) {
        final JPanel p = new JPanel(new GridBagLayout());

        this.colFadeColor = ImageryLayer.getFadeColor();
        this.btnFadeColor = new JButton();
        this.btnFadeColor.setBackground(colFadeColor);
        this.btnFadeColor.setText(ColorHelper.color2html(colFadeColor));

        this.btnFadeColor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JColorChooser chooser = new JColorChooser(colFadeColor);
                int answer = JOptionPane.showConfirmDialog(
                        gui, chooser,
                        tr("Choose a color for {0}", tr("imagery fade")),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                if (answer == JOptionPane.OK_OPTION) {
                    colFadeColor = chooser.getColor();
                    btnFadeColor.setBackground(colFadeColor);
                    btnFadeColor.setText(ColorHelper.color2html(colFadeColor));
                }
            }
        });

        p.add(new JLabel(tr("Fade Color: ")), GBC.std());
        p.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        p.add(this.btnFadeColor, GBC.eol().fill(GBC.HORIZONTAL));

        p.add(new JLabel(tr("Fade amount: ")), GBC.std());
        p.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        p.add(this.fadeAmount, GBC.eol().fill(GBC.HORIZONTAL));
        this.fadeAmount.setValue(ImageryLayer.PROP_FADE_AMOUNT.get());

        this.sharpen = new JComboBox(new String[] {
                tr("None"),
                tr("Soft"),
                tr("Strong")});
        p.add(new JLabel(tr("Sharpen (requires layer re-add): ")));
        p.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        p.add(this.sharpen, GBC.std().fill(GBC.HORIZONTAL));
        this.sharpen.setSelectedIndex(ImageryLayer.PROP_SHARPEN_LEVEL.get());

        return p;
    }

    private JPanel buildWMSSettingsPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        browser = new JComboBox(new String[] {
                "webkit-image {0}",
                "gnome-web-photo --mode=photo --format=png {0} /dev/stdout",
                "gnome-web-photo-fixed {0}",
        "webkit-image-gtk {0}"});
        browser.setEditable(true);
        browser.setSelectedItem(HTMLGrabber.PROP_BROWSER.get());
        p.add(new JLabel(tr("Downloader:")), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(browser);

        // Overlap
        p.add(Box.createHorizontalGlue(), GBC.eol().fill(GBC.HORIZONTAL));

        overlapCheckBox = new JCheckBox(tr("Overlap tiles"), WMSLayer.PROP_OVERLAP.get());
        JLabel labelEast = new JLabel(tr("% of east:"));
        JLabel labelNorth = new JLabel(tr("% of north:"));
        spinEast = new JSpinner(new SpinnerNumberModel(WMSLayer.PROP_OVERLAP_EAST.get(), 1, 50, 1));
        spinNorth = new JSpinner(new SpinnerNumberModel(WMSLayer.PROP_OVERLAP_NORTH.get(), 1, 50, 1));

        JPanel overlapPanel = new JPanel(new FlowLayout());
        overlapPanel.add(overlapCheckBox);
        overlapPanel.add(labelEast);
        overlapPanel.add(spinEast);
        overlapPanel.add(labelNorth);
        overlapPanel.add(spinNorth);

        p.add(overlapPanel);

        // Simultaneous connections
        p.add(Box.createHorizontalGlue(), GBC.eol().fill(GBC.HORIZONTAL));
        JLabel labelSimConn = new JLabel(tr("Simultaneous connections"));
        spinSimConn = new JSpinner(new SpinnerNumberModel(WMSLayer.PROP_SIMULTANEOUS_CONNECTIONS.get(), 1, 30, 1));
        JPanel overlapPanelSimConn = new JPanel(new FlowLayout(FlowLayout.LEFT));
        overlapPanelSimConn.add(labelSimConn);
        overlapPanelSimConn.add(spinSimConn);
        p.add(overlapPanelSimConn, GBC.eol().fill(GBC.HORIZONTAL));

        return p;
    }

    private JPanel buildTMSSettingsPanel() {
        JPanel tmsTab = new JPanel(new GridBagLayout());

        minZoomLvl = new JSpinner(new SpinnerNumberModel(TMSLayer.DEFAULT_MIN_ZOOM, TMSLayer.MIN_ZOOM, TMSLayer.MAX_ZOOM, 1));
        maxZoomLvl = new JSpinner(new SpinnerNumberModel(TMSLayer.DEFAULT_MAX_ZOOM, TMSLayer.MIN_ZOOM, TMSLayer.MAX_ZOOM, 1));

        tmsTab.add(new JLabel(tr("Auto zoom by default: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        tmsTab.add(autozoomActive, GBC.eol().fill(GBC.HORIZONTAL));

        tmsTab.add(new JLabel(tr("Autoload tiles by default: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        tmsTab.add(autoloadTiles, GBC.eol().fill(GBC.HORIZONTAL));

        tmsTab.add(new JLabel(tr("Min zoom lvl: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        tmsTab.add(this.minZoomLvl, GBC.eol().fill(GBC.HORIZONTAL));

        tmsTab.add(new JLabel(tr("Max zoom lvl: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        tmsTab.add(this.maxZoomLvl, GBC.eol().fill(GBC.HORIZONTAL));

        tmsTab.add(new JLabel(tr("Add to slippymap chooser: ")), GBC.std());
        tmsTab.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        tmsTab.add(addToSlippyMapChosser, GBC.eol().fill(GBC.HORIZONTAL));

        this.autozoomActive.setSelected(TMSLayer.PROP_DEFAULT_AUTOZOOM.get());
        this.autoloadTiles.setSelected(TMSLayer.PROP_DEFAULT_AUTOLOAD.get());
        this.addToSlippyMapChosser.setSelected(TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get());
        this.maxZoomLvl.setValue(TMSLayer.getMaxZoomLvl(null));
        this.minZoomLvl.setValue(TMSLayer.getMinZoomLvl(null));
        return tmsTab;
    }

    private void addSettingsSection(final JPanel p, String name, JPanel section) {
        final JLabel lbl = new JLabel(name);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        p.add(lbl,GBC.std());
        p.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(5, 0, 0, 0));
        p.add(section,GBC.eol().insets(20,5,0,10));
    }

    private Component buildSettingsPanel(final PreferenceTabbedPane gui) {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        addSettingsSection(p, tr("Common Settings"), buildCommonSettingsPanel(gui));
        addSettingsSection(p, tr("WMS Settings"), buildWMSSettingsPanel());
        addSettingsSection(p, tr("TMS Settings"), buildTMSSettingsPanel());

        p.add(new JPanel(),GBC.eol().fill(GBC.BOTH));
        return new JScrollPane(p);
    }

    @Override
    public void addGui(final PreferenceTabbedPane gui) {
        JPanel p = gui.createPreferenceTab("imagery", tr("Imagery Preferences"), tr("Modify list of imagery layers displayed in the Imagery menu"));
        JTabbedPane pane = new JTabbedPane();
        imageryProviders = new ImageryProvidersPanel(gui, ImageryLayerInfo.instance);
        pane.add(imageryProviders);
        pane.add(buildSettingsPanel(gui));
        pane.add(new OffsetBookmarksPanel(gui));
        pane.setTitleAt(0, tr("Imagery providers"));
        pane.setTitleAt(1, tr("Settings"));
        pane.setTitleAt(2, tr("Offset bookmarks"));
        p.add(pane,GBC.std().fill(GBC.BOTH));
    }

    @Override
    public boolean ok() {
        boolean restartRequired = false;
        ImageryLayerInfo.instance.save();
        Main.main.menu.imageryMenuUpdater.refreshImageryMenu();
        Main.main.menu.imageryMenuUpdater.refreshOffsetMenu();
        OffsetBookmark.saveBookmarks();

        WMSLayer.PROP_OVERLAP.put(overlapCheckBox.getModel().isSelected());
        WMSLayer.PROP_OVERLAP_EAST.put((Integer) spinEast.getModel().getValue());
        WMSLayer.PROP_OVERLAP_NORTH.put((Integer) spinNorth.getModel().getValue());
        WMSLayer.PROP_SIMULTANEOUS_CONNECTIONS.put((Integer) spinSimConn.getModel().getValue());

        HTMLGrabber.PROP_BROWSER.put(browser.getEditor().getItem().toString());

        if (TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get() != this.addToSlippyMapChosser.isSelected()) {
            restartRequired = true;
        }
        TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.put(this.addToSlippyMapChosser.isSelected());
        TMSLayer.PROP_DEFAULT_AUTOZOOM.put(this.autozoomActive.isSelected());
        TMSLayer.PROP_DEFAULT_AUTOLOAD.put(this.autoloadTiles.isSelected());
        TMSLayer.setMaxZoomLvl((Integer)this.maxZoomLvl.getValue());
        TMSLayer.setMinZoomLvl((Integer)this.minZoomLvl.getValue());

        ImageryLayer.PROP_FADE_AMOUNT.put(this.fadeAmount.getValue());
        ImageryLayer.setFadeColor(this.colFadeColor);
        ImageryLayer.PROP_SHARPEN_LEVEL.put(sharpen.getSelectedIndex());

        return restartRequired;
    }


    /**
     * Updates a server URL in the preferences dialog. Used by plugins.
     *
     * @param server
     *            The server name
     * @param url
     *            The server URL
     */
    public void setServerUrl(String server, String url) {
        for (int i = 0; i < imageryProviders.model.getRowCount(); i++) {
            if (server.equals(imageryProviders.model.getValueAt(i, 0).toString())) {
                imageryProviders.model.setValueAt(url, i, 1);
                return;
            }
        }
        imageryProviders.model.addRow(new String[] { server, url });
    }

    /**
     * Gets a server URL in the preferences dialog. Used by plugins.
     *
     * @param server
     *            The server name
     * @return The server URL
     */
    public String getServerUrl(String server) {
        for (int i = 0; i < imageryProviders.model.getRowCount(); i++) {
            if (server.equals(imageryProviders.model.getValueAt(i, 0).toString()))
                return imageryProviders.model.getValueAt(i, 1).toString();
        }
        return null;
    }

    static class ImageryProvidersPanel extends JPanel {
        final ImageryLayerTableModel model;
        private final ImageryLayerInfo layerInfo;

        public ImageryProvidersPanel(final PreferenceTabbedPane gui, ImageryLayerInfo layerInfo) {
            super(new GridBagLayout());
            this.layerInfo = layerInfo;
            this.model = new ImageryLayerTableModel();

            final JTable list = new JTable(model) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    java.awt.Point p = e.getPoint();
                    return model.getValueAt(rowAtPoint(p), columnAtPoint(p)).toString();
                }
            };
            JScrollPane scroll = new JScrollPane(list);
            add(scroll, GBC.eol().fill(GridBagConstraints.BOTH));
            scroll.setPreferredSize(new Dimension(200, 200));

            final ImageryDefaultLayerTableModel modeldef = new ImageryDefaultLayerTableModel();
            final JTable listdef = new JTable(modeldef) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    java.awt.Point p = e.getPoint();
                    return (String) modeldef.getValueAt(rowAtPoint(p), columnAtPoint(p));
                }
            };
            JScrollPane scrolldef = new JScrollPane(listdef);
            // scrolldef is added after the buttons so it's clearer the buttons
            // control the top list and not the default one
            scrolldef.setPreferredSize(new Dimension(200, 200));

            TableColumnModel mod = listdef.getColumnModel();
            mod.getColumn(1).setPreferredWidth(800);
            mod.getColumn(0).setPreferredWidth(200);
            mod = list.getColumnModel();
            mod.getColumn(2).setPreferredWidth(50);
            mod.getColumn(1).setPreferredWidth(800);
            mod.getColumn(0).setPreferredWidth(200);

            JPanel buttonPanel = new JPanel(new FlowLayout());

            JButton add = new JButton(tr("Add"));
            buttonPanel.add(add, GBC.std().insets(0, 5, 0, 0));
            add.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AddWMSLayerPanel p = new AddWMSLayerPanel();
                    int answer = JOptionPane.showConfirmDialog(
                            gui, p,
                            tr("Add Imagery URL"),
                            JOptionPane.OK_CANCEL_OPTION);
                    if (answer == JOptionPane.OK_OPTION) {
                        model.addRow(new ImageryInfo(p.getUrlName(), p.getUrl()));
                    }
                }
            });

            JButton delete = new JButton(tr("Delete"));
            buttonPanel.add(delete, GBC.std().insets(0, 5, 0, 0));
            delete.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (list.getSelectedRow() == -1) {
                        JOptionPane.showMessageDialog(gui, tr("Please select the row to delete."));
                    } else {
                        Integer i;
                        while ((i = list.getSelectedRow()) != -1) {
                            model.removeRow(i);
                        }
                    }
                }
            });

            JButton copy = new JButton(tr("Copy Selected Default(s)"));
            buttonPanel.add(copy, GBC.std().insets(0, 5, 0, 0));
            copy.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int[] lines = listdef.getSelectedRows();
                    if (lines.length == 0) {
                        JOptionPane.showMessageDialog(
                                gui,
                                tr("Please select at least one row to copy."),
                                tr("Information"),
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    outer: for (int i = 0; i < lines.length; i++) {
                        ImageryInfo info = modeldef.getRow(lines[i]);

                        // Check if an entry with exactly the same values already
                        // exists
                        for (int j = 0; j < model.getRowCount(); j++) {
                            if (info.equalsBaseValues(model.getRow(j))) {
                                // Select the already existing row so the user has
                                // some feedback in case an entry exists
                                list.getSelectionModel().setSelectionInterval(j, j);
                                list.scrollRectToVisible(list.getCellRect(j, 0, true));
                                continue outer;
                            }
                        }

                        if (info.eulaAcceptanceRequired != null) {
                            if (!confirmEulaAcceptance(gui, info.eulaAcceptanceRequired)) {
                                continue outer;
                            }
                        }

                        model.addRow(new ImageryInfo(info));
                        int lastLine = model.getRowCount() - 1;
                        list.getSelectionModel().setSelectionInterval(lastLine, lastLine);
                        list.scrollRectToVisible(list.getCellRect(lastLine, 0, true));
                    }
                }
            });

            add(buttonPanel);
            add(Box.createHorizontalGlue(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
            // Add default item list
            add(scrolldef, GBC.eol().insets(0, 5, 0, 0).fill(GridBagConstraints.BOTH));
        }

        /**
         * The table model for imagery layer list
         */
        class ImageryLayerTableModel extends DefaultTableModel {
            public ImageryLayerTableModel() {
                setColumnIdentifiers(new String[] { tr("Menu Name"), tr("Imagery URL"), trc("layer", "Zoom") });
            }

            public ImageryInfo getRow(int row) {
                return layerInfo.getLayers().get(row);
            }

            public void addRow(ImageryInfo i) {
                layerInfo.add(i);
                int p = getRowCount() - 1;
                fireTableRowsInserted(p, p);
            }

            @Override
            public void removeRow(int i) {
                layerInfo.remove(getRow(i));
                fireTableRowsDeleted(i, i);
            }

            @Override
            public int getRowCount() {
                return layerInfo.getLayers().size();
            }

            @Override
            public Object getValueAt(int row, int column) {
                ImageryInfo info = layerInfo.getLayers().get(row);
                switch (column) {
                case 0:
                    return info.getName();
                case 1:
                    return info.getFullURL();
                case 2:
                    return (info.getImageryType() == ImageryType.WMS || info.getImageryType() == ImageryType.HTML) ?
                            (info.getPixelPerDegree() == 0.0 ? "" : info.getPixelPerDegree()) :
                                (info.getMaxZoom() == 0 ? "" : info.getMaxZoom());
                default:
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            @Override
            public void setValueAt(Object o, int row, int column) {
                ImageryInfo info = layerInfo.getLayers().get(row);
                switch (column) {
                case 0:
                    info.setName((String) o);
                    break;
                case 1:
                    info.setURL((String)o);
                    break;
                case 2:
                    info.setPixelPerDegree(0);
                    info.setMaxZoom(0);
                    try {
                        if(info.getImageryType() == ImageryType.WMS || info.getImageryType() == ImageryType.HTML) {
                            info.setPixelPerDegree(Double.parseDouble((String) o));
                        } else {
                            info.setMaxZoom(Integer.parseInt((String) o));
                        }
                    } catch (NumberFormatException e) {
                    }
                    break;
                default:
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        }

        /**
         * The table model for the default imagery layer list
         */
        class ImageryDefaultLayerTableModel extends DefaultTableModel {
            public ImageryDefaultLayerTableModel() {
                setColumnIdentifiers(new String[] { tr("Menu Name (Default)"), tr("Imagery URL (Default)") });
            }

            public ImageryInfo getRow(int row) {
                return layerInfo.getDefaultLayers().get(row);
            }

            @Override
            public int getRowCount() {
                return layerInfo.getDefaultLayers().size();
            }

            @Override
            public Object getValueAt(int row, int column) {
                ImageryInfo info = layerInfo.getDefaultLayers().get(row);
                switch (column) {
                case 0:
                    return info.getName();
                case 1:
                    return info.getFullURL();
                }
                return null;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        }

        private boolean confirmEulaAcceptance(PreferenceTabbedPane gui, String eulaUrl) {
            URL url = null;
            try {
                url = new URL(eulaUrl.replaceAll("\\{lang\\}", Locale.getDefault().toString()));
                JEditorPane htmlPane = null;
                try {
                    htmlPane = new JEditorPane(url);
                } catch (IOException e1) {
                    // give a second chance with a default Locale 'en'
                    try {
                        url = new URL(eulaUrl.replaceAll("\\{lang\\}", "en"));
                        htmlPane = new JEditorPane(url);
                    } catch (IOException e2) {
                        JOptionPane.showMessageDialog(gui ,tr("EULA license URL not available: {0}", eulaUrl));
                        return false;
                    }
                }
                Box box = Box.createVerticalBox();
                htmlPane.setEditable(false);
                JScrollPane scrollPane = new JScrollPane(htmlPane);
                scrollPane.setPreferredSize(new Dimension(400, 400));
                box.add(scrollPane);
                int option = JOptionPane.showConfirmDialog(Main.parent, box, tr("Please abort if you are not sure"), JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (option == JOptionPane.YES_OPTION)
                    return true;
            } catch (MalformedURLException e2) {
                JOptionPane.showMessageDialog(gui ,tr("Malformed URL for the EULA licence: {0}", eulaUrl));
            }
            return false;
        }
    }

    static class OffsetBookmarksPanel extends JPanel {
        List<OffsetBookmark> bookmarks = OffsetBookmark.allBookmarks;
        OffsetsBookmarksModel model = new OffsetsBookmarksModel();

        public OffsetBookmarksPanel(final PreferenceTabbedPane gui) {
            super(new GridBagLayout());
            final JTable list = new JTable(model) {
                @Override
                public String getToolTipText(MouseEvent e) {
                    java.awt.Point p = e.getPoint();
                    return model.getValueAt(rowAtPoint(p), columnAtPoint(p)).toString();
                }
            };
            JScrollPane scroll = new JScrollPane(list);
            add(scroll, GBC.eol().fill(GridBagConstraints.BOTH));
            scroll.setPreferredSize(new Dimension(200, 200));

            TableColumnModel mod = list.getColumnModel();
            mod.getColumn(0).setPreferredWidth(150);
            mod.getColumn(1).setPreferredWidth(200);
            mod.getColumn(2).setPreferredWidth(300);
            mod.getColumn(3).setPreferredWidth(150);
            mod.getColumn(4).setPreferredWidth(150);

            JPanel buttonPanel = new JPanel(new FlowLayout());

            JButton add = new JButton(tr("Add"));
            buttonPanel.add(add, GBC.std().insets(0, 5, 0, 0));
            add.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    OffsetBookmark b = new OffsetBookmark(Main.proj,"","",0,0);
                    model.addRow(b);
                }
            });

            JButton delete = new JButton(tr("Delete"));
            buttonPanel.add(delete, GBC.std().insets(0, 5, 0, 0));
            delete.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (list.getSelectedRow() == -1) {
                        JOptionPane.showMessageDialog(gui, tr("Please select the row to delete."));
                    } else {
                        Integer i;
                        while ((i = list.getSelectedRow()) != -1) {
                            model.removeRow(i);
                        }
                    }
                }
            });

            add(buttonPanel,GBC.eol());
        }

        /**
         * The table model for imagery offsets list
         */
        class OffsetsBookmarksModel extends DefaultTableModel {
            public OffsetsBookmarksModel() {
                setColumnIdentifiers(new String[] { tr("Projection"),  tr("Layer"), tr("Name"), tr("Easting"), tr("Northing"),});
            }

            public OffsetBookmark getRow(int row) {
                return bookmarks.get(row);
            }

            public void addRow(OffsetBookmark i) {
                bookmarks.add(i);
                int p = getRowCount() - 1;
                fireTableRowsInserted(p, p);
            }

            @Override
            public void removeRow(int i) {
                bookmarks.remove(getRow(i));
                fireTableRowsDeleted(i, i);
            }

            @Override
            public int getRowCount() {
                return bookmarks.size();
            }

            @Override
            public Object getValueAt(int row, int column) {
                OffsetBookmark info = bookmarks.get(row);
                switch (column) {
                case 0:
                    return info.proj.toString();
                case 1:
                    return info.layerName;
                case 2:
                    return info.name;
                case 3:
                    return info.dx;
                case 4:
                    return info.dy;
                default:
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            @Override
            public void setValueAt(Object o, int row, int column) {
                OffsetBookmark info = bookmarks.get(row);
                switch (column) {
                case 1:
                    info.layerName = o.toString();
                    break;
                case 2:
                    info.name = o.toString();
                    break;
                case 3:
                    info.dx = Double.parseDouble((String) o);
                    break;
                case 4:
                    info.dy = Double.parseDouble((String) o);
                    break;
                default:
                    throw new ArrayIndexOutOfBoundsException();
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 1;
            }
        }
    }

    public static void initialize() {
        migrateWMSPlugin();
        migrateSlippyMapPlugin();
        ImageryLayerInfo.instance.load();
        OffsetBookmark.loadBookmarks();
        Main.main.menu.imageryMenuUpdater.refreshImageryMenu();
        Main.main.menu.imageryMenuUpdater.refreshOffsetMenu();
    }

    // Migration of WMSPlugin and SlippyMap settings
    static boolean wmsLayersConflict;
    static boolean wmsSettingsConflict;
    static boolean tmsSettingsConflict;

    static class SettingsConflictException extends Exception {
    }

    static void migrateProperty(String oldProp, String newProp)
    throws SettingsConflictException {
        String oldValue = Main.pref.get(oldProp, null);
        if (oldValue == null) return;
        String newValue = Main.pref.get(newProp, null);
        if (newValue != null && !oldValue.equals(newValue)) {
            System.out.println(tr("Imagery settings migration: conflict when moving property {0} -> {1}",
                    oldProp, newProp));
            throw new SettingsConflictException();
        }
        Main.pref.put(newProp, oldValue);
        Main.pref.put(oldProp, null);
    }

    static void migrateWMSPlugin() {
        try {
            migrateProperty("wmslayers", "imagery.layers");
        } catch (SettingsConflictException e) {
            wmsLayersConflict = true;
        }
        try {
            Main.pref.put("wmslayers.default", null);
            migrateProperty("imagery.remotecontrol", "remotecontrol.permission.imagery");
            migrateProperty("wmsplugin.remotecontrol", "remotecontrol.permission.imagery");
            migrateProperty("wmsplugin.alpha_channel", "imagery.wms.alpha_channel");
            migrateProperty("wmsplugin.browser", "imagery.wms.browser");
            migrateProperty("wmsplugin.user_agent", "imagery.wms.user_agent");
            migrateProperty("wmsplugin.timeout.connect", "imagery.wms.timeout.connect");
            migrateProperty("wmsplugin.timeout.read", "imagery.wms.timeout.read");
            migrateProperty("wmsplugin.simultaneousConnections", "imagery.wms.simultaneousConnections");
            migrateProperty("wmsplugin.overlap", "imagery.wms.overlap");
            migrateProperty("wmsplugin.overlapEast", "imagery.wms.overlapEast");
            migrateProperty("wmsplugin.overlapNorth", "imagery.wms.overlapNorth");
            Map<String, String> unknownProps = Main.pref.getAllPrefix("wmsplugin");
            if (!unknownProps.isEmpty()) {
                System.out.println(tr("There are {0} unknown WMSPlugin settings", unknownProps.size()));
                wmsSettingsConflict = true;
            }
        } catch (SettingsConflictException e) {
            wmsSettingsConflict = true;
        }
    }

    static void migrateSlippyMapPlugin() {
        try {
            Main.pref.put("slippymap.tile_source", null);
            Main.pref.put("slippymap.last_zoom_lvl", null);
            migrateProperty("slippymap.draw_debug", "imagery.tms.draw_debug");
            migrateProperty("slippymap.autoload_tiles", "imagery.tms.autoload");
            migrateProperty("slippymap.autozoom", "imagery.tms.autozoom");
            migrateProperty("slippymap.min_zoom_lvl", "imagery.tms.min_zoom_lvl");
            migrateProperty("slippymap.max_zoom_lvl", "imagery.tms.max_zoom_lvl");
            if (Main.pref.get("slippymap.fade_background_100", null) == null) {
                try {
                    Main.pref.putInteger("slippymap.fade_background_100", (int)Math.round(
                            Double.valueOf(Main.pref.get("slippymap.fade_background", "0"))*100.0));
                } catch (NumberFormatException e) {
                }
            }
            Main.pref.put("slippymap.fade_background", null);
            migrateProperty("slippymap.fade_background_100", "imagery.fade_amount");
            Map<String, String> unknownProps = Main.pref.getAllPrefix("slippymap");
            if (!unknownProps.isEmpty()) {
                System.out.println(tr("There are {0} unknown slippymap plugin settings", unknownProps.size()));
                wmsSettingsConflict = true;
            }
        } catch (SettingsConflictException e) {
            tmsSettingsConflict = true;
        }
    }

}
