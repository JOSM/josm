// License: GPL. See LICENSE file for details.
// Copyright 2007 by Christian Gallioz (aka khris78)
// Parts of code from Geotagged plugin (by Rob Neild)
// and the core JOSM source code (by Immanuel Scholz and others)

package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.PrimaryDateParser;
import org.xml.sax.SAXException;


/** This class displays the window to select the GPX file and the offset (timezone + delta).
 * Then it correlates the images of the layer with that GPX file.
 */
public class CorrelateGpxWithImages implements ActionListener {

    private static List<GpxData> loadedGpxData = new ArrayList<GpxData>();

    GeoImageLayer yLayer = null;
    double timezone;
    long delta;
    
    public CorrelateGpxWithImages(GeoImageLayer layer) {
        this.yLayer = layer;
    }

    private static class GpxDataWrapper {
        String name;
        GpxData data;
        File file;

        public GpxDataWrapper(String name, GpxData data, File file) {
            this.name = name;
            this.data = data;
            this.file = file;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    ExtendedDialog syncDialog;
    Vector<GpxDataWrapper> gpxLst = new Vector<GpxDataWrapper>();
    JPanel outerPanel;
    JComboBox cbGpx;
    JTextField tfTimezone;
    JTextField tfOffset;
    JCheckBox cbExifImg;
    JCheckBox cbTaggedImg;
    JCheckBox cbShowThumbs;
    JLabel statusBarText;
    StatusBarListener statusBarListener;
    
    // remember the last number of matched photos
    int lastNumMatched = 0;

    /** This class is called when the user doesn't find the GPX file he needs in the files that have
     * been loaded yet. It displays a FileChooser dialog to select the GPX file to be loaded.
     */
    private class LoadGpxDataActionListener implements ActionListener {

        public void actionPerformed(ActionEvent arg0) {
            JFileChooser fc = new JFileChooser(Main.pref.get("lastDirectory"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setAcceptAllFileFilterUsed(false);
            fc.setMultiSelectionEnabled(false);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setFileFilter(new FileFilter(){
                @Override public boolean accept(File f) {
                    return (f.isDirectory()
                            || f .getName().toLowerCase().endsWith(".gpx")
                            || f.getName().toLowerCase().endsWith(".gpx.gz"));
                }
                @Override public String getDescription() {
                    return tr("GPX Files (*.gpx *.gpx.gz)");
                }
            });
            fc.showOpenDialog(Main.parent);
            File sel = fc.getSelectedFile();
            if (sel == null)
                return;

            try {
                outerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                Main.pref.put("lastDirectory", sel.getPath());

                for (int i = gpxLst.size() - 1 ; i >= 0 ; i--) {
                    GpxDataWrapper wrapper = gpxLst.get(i);
                    if (wrapper.file != null && sel.equals(wrapper.file)) {
                        cbGpx.setSelectedIndex(i);
                        if (!sel.getName().equals(wrapper.name)) {
                            JOptionPane.showMessageDialog(
                                    Main.parent,
                                    tr("File {0} is loaded yet under the name \"{1}\"", sel.getName(), wrapper.name),
                                    tr("Error"),
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                        return;
                    }
                }
                GpxData data = null;
                try {
                    InputStream iStream;
                    if (sel.getName().toLowerCase().endsWith(".gpx.gz")) {
                        iStream = new GZIPInputStream(new FileInputStream(sel));
                    } else {
                        iStream = new FileInputStream(sel);
                    }
                    data = new GpxReader(iStream, sel).data;
                    data.storageFile = sel;

                } catch (SAXException x) {
                    x.printStackTrace();
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Error while parsing {0}",sel.getName())+": "+x.getMessage(),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                } catch (IOException x) {
                    x.printStackTrace();
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Could not read \"{0}\"",sel.getName())+"\n"+x.getMessage(),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                loadedGpxData.add(data);
                if (gpxLst.get(0).file == null) {
                    gpxLst.remove(0);
                }
                gpxLst.add(new GpxDataWrapper(sel.getName(), data, sel));
                cbGpx.setSelectedIndex(cbGpx.getItemCount() - 1);
            } finally {
                outerPanel.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    /** This action listener is called when the user has a photo of the time of his GPS receiver. It
     * displays the list of photos of the layer, and upon selection displays the selected photo.
     * From that photo, the user can key in the time of the GPS.
     * Then values of timezone and delta are set.
     * @author chris
     *
     */
    private class SetOffsetActionListener implements ActionListener {
        JPanel panel;
        JLabel lbExifTime;
        JTextField tfGpsTime;
        JComboBox cbTimezones;
        ImageDisplay imgDisp;
        JList imgList;

        public void actionPerformed(ActionEvent arg0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

            panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(new JLabel(tr("<html>Take a photo of your GPS receiver while it displays the time.<br>"
                    + "Display that photo here.<br>"
                    + "And then, simply capture the time you read on the photo and select a timezone<hr></html>")),
                    BorderLayout.NORTH);

            imgDisp = new ImageDisplay();
            imgDisp.setPreferredSize(new Dimension(300, 225));
            panel.add(imgDisp, BorderLayout.CENTER);

            JPanel panelTf = new JPanel();
            panelTf.setLayout(new GridBagLayout());

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = gc.gridy = 0;
            gc.gridwidth = gc.gridheight = 1;
            gc.weightx = gc.weighty = 0.0;
            gc.fill = GridBagConstraints.NONE;
            gc.anchor = GridBagConstraints.WEST;
            panelTf.add(new JLabel(tr("Photo time (from exif):")), gc);

            lbExifTime = new JLabel();
            gc.gridx = 1;
            gc.weightx = 1.0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.gridwidth = 2;
            panelTf.add(lbExifTime, gc);

            gc.gridx = 0;
            gc.gridy = 1;
            gc.gridwidth = gc.gridheight = 1;
            gc.weightx = gc.weighty = 0.0;
            gc.fill = GridBagConstraints.NONE;
            gc.anchor = GridBagConstraints.WEST;
            panelTf.add(new JLabel(tr("Gps time (read from the above photo): ")), gc);

            tfGpsTime = new JTextField(12);
            tfGpsTime.setEnabled(false);
            tfGpsTime.setMinimumSize(new Dimension(155, tfGpsTime.getMinimumSize().height));
            gc.gridx = 1;
            gc.weightx = 1.0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            panelTf.add(tfGpsTime, gc);

            gc.gridx = 2;
            gc.weightx = 0.2;
            panelTf.add(new JLabel(tr(" [dd/mm/yyyy hh:mm:ss]")), gc);

            gc.gridx = 0;
            gc.gridy = 2;
            gc.gridwidth = gc.gridheight = 1;
            gc.weightx = gc.weighty = 0.0;
            gc.fill = GridBagConstraints.NONE;
            gc.anchor = GridBagConstraints.WEST;
            panelTf.add(new JLabel(tr("I'm in the timezone of: ")), gc);

            Vector<String> vtTimezones = new Vector<String>();
            String[] tmp = TimeZone.getAvailableIDs();

            for (String tzStr : tmp) {
                TimeZone tz = TimeZone.getTimeZone(tzStr);

                String tzDesc = new StringBuffer(tzStr).append(" (")
                .append(formatTimezone(tz.getRawOffset() / 3600000.0))
                .append(')').toString();
                vtTimezones.add(tzDesc);
            }

            Collections.sort(vtTimezones);

            cbTimezones = new JComboBox(vtTimezones);

            String tzId = Main.pref.get("geoimage.timezoneid", "");
            TimeZone defaultTz;
            if (tzId.length() == 0) {
                defaultTz = TimeZone.getDefault();
            } else {
                defaultTz = TimeZone.getTimeZone(tzId);
            }

            cbTimezones.setSelectedItem(new StringBuffer(defaultTz.getID()).append(" (")
                    .append(formatTimezone(defaultTz.getRawOffset() / 3600000.0))
                    .append(')').toString());

            gc.gridx = 1;
            gc.weightx = 1.0;
            gc.gridwidth = 2;
            gc.fill = GridBagConstraints.HORIZONTAL;
            panelTf.add(cbTimezones, gc);

            panel.add(panelTf, BorderLayout.SOUTH);

            JPanel panelLst = new JPanel();
            panelLst.setLayout(new BorderLayout());

            imgList = new JList(new AbstractListModel() {
                public Object getElementAt(int i) {
                    return yLayer.data.get(i).file.getName();
                }

                public int getSize() {
                    return yLayer.data.size();
                }
            });
            imgList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            imgList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                public void valueChanged(ListSelectionEvent arg0) {
                    int index = imgList.getSelectedIndex();
                    imgDisp.setImage(yLayer.data.get(index).file);
                    Date date = yLayer.data.get(index).time;
                    if (date != null) {
                        lbExifTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date));
                        tfGpsTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date));
                        tfGpsTime.setCaretPosition(tfGpsTime.getText().length());
                        tfGpsTime.setEnabled(true);
                        tfGpsTime.requestFocus();
                    } else {
                        lbExifTime.setText(tr("No date"));
                        tfGpsTime.setText("");
                        tfGpsTime.setEnabled(false);
                    }
                }

            });
            panelLst.add(new JScrollPane(imgList), BorderLayout.CENTER);

            JButton openButton = new JButton(tr("Open an other photo"));
            openButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent arg0) {
                    JFileChooser fc = new JFileChooser(Main.pref.get("geoimage.lastdirectory"));
                    fc.setAcceptAllFileFilterUsed(false);
                    fc.setMultiSelectionEnabled(false);
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fc.setFileFilter(JpegFileFilter.getInstance());
                    fc.showOpenDialog(Main.parent);
                    File sel = fc.getSelectedFile();
                    if (sel == null)
                        return;

                    imgDisp.setImage(sel);

                    Date date = null;
                    try {
                        date = ExifReader.readTime(sel);
                    } catch (Exception e) {
                    }
                    if (date != null) {
                        lbExifTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date));
                        tfGpsTime.setText(new SimpleDateFormat("dd/MM/yyyy ").format(date));
                        tfGpsTime.setEnabled(true);
                    } else {
                        lbExifTime.setText(tr("No date"));
                        tfGpsTime.setText("");
                        tfGpsTime.setEnabled(false);
                    }
                }
            });
            panelLst.add(openButton, BorderLayout.PAGE_END);

            panel.add(panelLst, BorderLayout.LINE_START);

            boolean isOk = false;
            while (! isOk) {
                int answer = JOptionPane.showConfirmDialog(
                        Main.parent, panel,
                        tr("Synchronize time from a photo of the GPS receiver"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (answer == JOptionPane.CANCEL_OPTION)
                    return;

                long delta;

                try {
                    delta = dateFormat.parse(lbExifTime.getText()).getTime()
                    - dateFormat.parse(tfGpsTime.getText()).getTime();
                } catch(ParseException e) {
                    JOptionPane.showMessageDialog(Main.parent, tr("Error while parsing the date.\n"
                            + "Please use the requested format"),
                            tr("Invalid date"), JOptionPane.ERROR_MESSAGE );
                    continue;
                }

                String selectedTz = (String) cbTimezones.getSelectedItem();
                int pos = selectedTz.lastIndexOf('(');
                tzId = selectedTz.substring(0, pos - 1);
                String tzValue = selectedTz.substring(pos + 1, selectedTz.length() - 1);

                Main.pref.put("geoimage.timezoneid", tzId);
                tfOffset.setText(Long.toString(delta / 1000));
                tfTimezone.setText(tzValue);

                isOk = true;

            }
            statusBarListener.updateStatusBar();
            yLayer.updateBufferAndRepaint();
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        // Construct the list of loaded GPX tracks
        Collection<Layer> layerLst = Main.main.map.mapView.getAllLayers();
        GpxDataWrapper defaultItem = null;
        Iterator<Layer> iterLayer = layerLst.iterator();
        while (iterLayer.hasNext()) {
            Layer cur = iterLayer.next();
            if (cur instanceof GpxLayer) {
                GpxDataWrapper gdw = new GpxDataWrapper(((GpxLayer) cur).getName(),
                        ((GpxLayer) cur).data,
                        ((GpxLayer) cur).data.storageFile);
                gpxLst.add(gdw);
                if (cur == yLayer.gpxLayer) {
                    defaultItem = gdw;
                }
            }
        }
        for (GpxData data : loadedGpxData) {
            gpxLst.add(new GpxDataWrapper(data.storageFile.getName(),
                    data,
                    data.storageFile));
        }

        if (gpxLst.size() == 0) {
            gpxLst.add(new GpxDataWrapper(tr("<No GPX track loaded yet>"), null, null));
        }

        JPanel panelCb = new JPanel();

        panelCb.add(new JLabel(tr("GPX track: ")));

        cbGpx = new JComboBox(gpxLst);
        if (defaultItem != null) {
            cbGpx.setSelectedItem(defaultItem);
        }
        panelCb.add(cbGpx);

        JButton buttonOpen = new JButton(tr("Open another GPX trace"));
        buttonOpen.addActionListener(new LoadGpxDataActionListener());
        panelCb.add(buttonOpen);

        JPanel panelTf = new JPanel();
        panelTf.setLayout(new GridBagLayout());

        String prefTimezone = Main.pref.get("geoimage.timezone", "0:00");
        if (prefTimezone == null) {
            prefTimezone = "0:00";
        }
        try {
            timezone = parseTimezone(prefTimezone);
        } catch (ParseException e) {
            timezone = 0;
        }
        
        tfTimezone = new JTextField(10);
        tfTimezone.setText(formatTimezone(timezone));

        try {
        delta = parseOffset(Main.pref.get("geoimage.delta", "0"));
        } catch (ParseException e) {
            delta = 0;
        }
        delta = delta / 1000;
        
        tfOffset = new JTextField(10);
        tfOffset.setText(Long.toString(delta));
        
        JPanel panelBtn = new JPanel();
        
        JButton buttonViewGpsPhoto = new JButton(tr("<html>Use photo of an accurate clock,<br>"
                + "e.g. GPS receiver display</html>"));
        buttonViewGpsPhoto.setIcon(ImageProvider.get("clock"));
        buttonViewGpsPhoto.addActionListener(new SetOffsetActionListener());

        JButton buttonAutoGuess = new JButton(tr("Auto-Guess"));
        buttonAutoGuess.addActionListener(new AutoGuessActionListener());

        JButton buttonAdjust = new JButton(tr("Manual adjust"));
        buttonAdjust.addActionListener(new AdjustActionListener());

        JLabel labelPosition = new JLabel(tr("Override position for: "));
 
        int numAll = getSortedImgList(true, true).size();
        int numExif = numAll - getSortedImgList(false, true).size();
        int numTagged = numAll - getSortedImgList(true, false).size();

        cbExifImg = new JCheckBox(tr("Images with geo location in exif data ({0}/{1})", numExif, numAll));
        cbExifImg.setEnabled(numExif != 0);

        cbTaggedImg = new JCheckBox(tr("Images that are already tagged ({0}/{1})", numTagged, numAll), true);
        cbTaggedImg.setEnabled(numTagged != 0);
        
        labelPosition.setEnabled(cbExifImg.isEnabled() || cbTaggedImg.isEnabled());

        boolean ticked = yLayer.thumbsLoaded || Main.pref.getBoolean("geoimage.showThumbs", false);
        cbShowThumbs = new JCheckBox(tr("Show Thumbnail images on the map"), ticked);
        cbShowThumbs.setEnabled(!yLayer.thumbsLoaded);
        /*cbShowThumbs.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    yLayer.loadThumbs();
                } else {
                }        
            }
        });*/

        int y=0;
        GBC gbc = GBC.eol();
        gbc.gridx = 0;
        gbc.gridy = y++;
        panelTf.add(panelCb, gbc);
        
        
        gbc = GBC.eol().fill(GBC.HORIZONTAL).insets(0,0,0,12);
        gbc.gridx = 0;
        gbc.gridy = y++;
        panelTf.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);


        gbc = GBC.std();
        gbc.gridx = 0;
        gbc.gridy = y;
        panelTf.add(new JLabel(tr("Timezone: ")), gbc);

        gbc = GBC.std().fill(GBC.HORIZONTAL);
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.weightx = 1.;
        panelTf.add(tfTimezone, gbc);

        gbc = GBC.std();
        gbc.gridx = 0;
        gbc.gridy = y;
        panelTf.add(new JLabel(tr("Offset:")), gbc);

        gbc = GBC.std().fill(GBC.HORIZONTAL);
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.weightx = 1.;
        panelTf.add(tfOffset, gbc);
        
        gbc = GBC.std().insets(5,5,5,5);
        gbc.gridx = 2;
        gbc.gridy = y-2;
        gbc.gridheight = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.5;
        panelTf.add(buttonViewGpsPhoto, gbc);

        gbc = GBC.std().fill(GBC.BOTH).insets(5,5,5,5);
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.weightx = 0.5;
        panelTf.add(buttonAutoGuess, gbc);
        
        gbc.gridx = 3;
        panelTf.add(buttonAdjust, gbc);

        gbc = GBC.eol().fill(GBC.HORIZONTAL).insets(0,12,0,0);
        gbc.gridx = 0;
        gbc.gridy = y++;
        panelTf.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        gbc = GBC.eol();
        gbc.gridx = 0;
        gbc.gridy = y++;
        panelTf.add(labelPosition, gbc);

        gbc = GBC.eol();
        gbc.gridx = 1;
        gbc.gridy = y++;
        panelTf.add(cbExifImg, gbc);

        gbc = GBC.eol();
        gbc.gridx = 1;
        gbc.gridy = y++;
        panelTf.add(cbTaggedImg, gbc);

        gbc = GBC.eol();
        gbc.gridx = 0;
        gbc.gridy = y++;
        panelTf.add(cbShowThumbs, gbc);

        final JPanel statusBar = new JPanel();
        statusBar.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        statusBarText = new JLabel(" ");
        statusBarText.setFont(statusBarText.getFont().deriveFont(8));
        statusBar.add(statusBarText);
        
        statusBarListener = new StatusBarListener() {
            @Override
            public void updateStatusBar() {
                statusBarText.setText(statusText());
            }
            private String statusText() {
                try {
                    timezone = parseTimezone(tfTimezone.getText().trim());
                    delta = parseOffset(tfOffset.getText().trim());
                } catch (ParseException e) {
                     return e.getMessage();
                }
                
                // Construct a list of images that have a date, and sort them on the date.
                ArrayList<ImageEntry> dateImgLst = getSortedImgList();
                for (ImageEntry ie : dateImgLst) {
                    ie.cleanTmp();
                }
                
                GpxDataWrapper selGpx = selectedGPX(false);
                if (selGpx == null)
                    return tr("No gpx selected");
                    
                lastNumMatched = matchGpxTrack(dateImgLst, selGpx.data, (long) (timezone * 3600) + delta);

                return tr("<html>Matched <b>{0}</b> of <b>{1}</b> photos to GPX track.", lastNumMatched, dateImgLst.size());
            }
        };
        
        tfTimezone.getDocument().addDocumentListener(statusBarListener);
        tfOffset.getDocument().addDocumentListener(statusBarListener);
        cbExifImg.addItemListener(statusBarListener);
        cbTaggedImg.addItemListener(statusBarListener);
        
        statusBarListener.updateStatusBar();

        outerPanel = new JPanel();
        outerPanel.setLayout(new BorderLayout());
        outerPanel.add(statusBar, BorderLayout.PAGE_END);


        syncDialog = new ExtendedDialog(
                Main.parent,
                tr("Correlate images with GPX track"),
                new String[] { tr("Correlate"), tr("Cancel") },
                false
        );
        syncDialog.setContent(panelTf, false);
        syncDialog.setButtonIcons(new String[] { "ok.png", "cancel.png" });
        syncDialog.setupDialog();
        outerPanel.add(syncDialog.getContentPane(), BorderLayout.PAGE_START);
        syncDialog.setContentPane(outerPanel);
        syncDialog.pack();
        syncDialog.addWindowListener(new WindowAdapter() {
            final int CANCEL = -1;
            final int DONE = 0;
            final int AGAIN = 1;
            final int NOTHING = 2;
            private int checkAndSave() {
                if (syncDialog.isVisible()) {
                    // nothing happened: JOSM was minimized or similar
                    return NOTHING;             
                }
                int answer = syncDialog.getValue();
                if(answer != 1)
                    return CANCEL;

                // Parse values again, to display an error if the format is not recognized
                try {
                    timezone = parseTimezone(tfTimezone.getText().trim());
                } catch (ParseException e) {
                    JOptionPane.showMessageDialog(Main.parent, e.getMessage(),
                            tr("Invalid timezone"), JOptionPane.ERROR_MESSAGE);
                    return AGAIN;
                }
                
                try {
                    delta = parseOffset(tfOffset.getText().trim());
                } catch (ParseException e) {
                        JOptionPane.showMessageDialog(Main.parent, e.getMessage(),
                                tr("Invalid offset"), JOptionPane.ERROR_MESSAGE);
                    return AGAIN;
                }
                
                if (lastNumMatched == 0) {
                    if (new ExtendedDialog(
                            Main.parent,
                            tr("Correlate images with GPX track"),
                            new String[] { tr("OK"), tr("Try Again") }).
                        setContent(tr("No images could be matched!")).
                        setButtonIcons(new String[] { "ok.png", "dialogs/refresh.png"}).
                        showDialog().getValue() == 2)
                            return AGAIN;
                }
                return DONE;
            }
            
            public void windowDeactivated(WindowEvent e) {
                int result = checkAndSave();
                switch (result) {
                    case NOTHING:
                        break;
                    case CANCEL:
                    {
                        for (ImageEntry ie : yLayer.data) {
                            ie.tmp = null;
                        }
                        yLayer.updateBufferAndRepaint();
                        break;
                    }
                    case AGAIN:
                        actionPerformed(null);
                        break;
                    case DONE:
                    {
                        Main.pref.put("geoimage.timezone", formatTimezone(timezone));
                        Main.pref.put("geoimage.delta", Long.toString(delta * 1000));
                        Main.pref.put("geoimage.showThumbs", yLayer.useThumbs);

                        yLayer.useThumbs = cbShowThumbs.isSelected();//FIXME
                        yLayer.loadThumbs();
                        
                        // Search whether an other layer has yet defined some bounding box.
                        // If none, we'll zoom to the bounding box of the layer with the photos.
                        boolean boundingBoxedLayerFound = false;
                        for (Layer l: Main.map.mapView.getAllLayers()) {
                            if (l != yLayer) {
                                BoundingXYVisitor bbox = new BoundingXYVisitor();
                                l.visitBoundingBox(bbox);
                                if (bbox.getBounds() != null) {
                                    boundingBoxedLayerFound = true;
                                    break;
                                }
                            }
                        }
                        if (! boundingBoxedLayerFound) {
                            BoundingXYVisitor bbox = new BoundingXYVisitor();
                            yLayer.visitBoundingBox(bbox);
                            Main.map.mapView.recalculateCenterScale(bbox);
                        }


                        for (ImageEntry ie : yLayer.data) {
                            ie.applyTmp();
                        }

                        yLayer.updateBufferAndRepaint();


                        break;
                    }
                    default:
                        throw new IllegalStateException();
                }
            }
        });
        syncDialog.showDialog();
    }

    private static abstract class StatusBarListener implements  DocumentListener, ItemListener {
        public void insertUpdate(DocumentEvent ev) {
            updateStatusBar();
        }
        public void removeUpdate(DocumentEvent ev) {
            updateStatusBar();
        }
        public void changedUpdate(DocumentEvent ev) {
        }
        public void itemStateChanged(ItemEvent e) {
            updateStatusBar();
        }
        abstract public void updateStatusBar();
    }

    /**
     * Presents dialog with sliders for manual adjust.
     */
    private class AdjustActionListener implements ActionListener {
    
        public void actionPerformed(ActionEvent arg0) {

            long diff = delta + Math.round(timezone*60*60);
            
            double diffInH = (double)diff/(60*60);    // hours

            // Find day difference
            final int dayOffset = (int)Math.round(diffInH / 24); // days
            double tmz = diff - dayOffset*24*60*60;  // seconds

            // In hours, rounded to two decimal places
            tmz = (double)Math.round(tmz*100/(60*60)) / 100;

            // Due to imprecise clocks we might get a "+3:28" timezone, which should obviously be 3:30 with
            // -2 minutes offset. This determines the real timezone and finds offset.
            double fixTimezone = (double)Math.round(tmz * 2)/2; // hours, rounded to one decimal place
            int offset = (int)Math.round(diff - fixTimezone*60*60) - dayOffset*24*60*60; // seconds

            // Info Labels
            final JLabel lblMatches = new JLabel();

            // Timezone Slider
            // The slider allows to switch timezon from -12:00 to 12:00 in 30 minutes
            // steps. Therefore the range is -24 to 24.
            final JLabel lblTimezone = new JLabel();
            final JSlider sldTimezone = new JSlider(-24, 24, 0);
            sldTimezone.setPaintLabels(true);
            Hashtable<Integer,JLabel> labelTable = new Hashtable<Integer, JLabel>();
            labelTable.put(-24, new JLabel("-12:00"));
            labelTable.put(-12, new JLabel( "-6:00"));
            labelTable.put(  0, new JLabel(  "0:00"));
            labelTable.put( 12, new JLabel(  "6:00"));
            labelTable.put( 24, new JLabel( "12:00"));
            sldTimezone.setLabelTable(labelTable);

            // Minutes Slider
            final JLabel lblMinutes = new JLabel();
            final JSlider sldMinutes = new JSlider(-15, 15, 0);
            sldMinutes.setPaintLabels(true);
            sldMinutes.setMajorTickSpacing(5);

            // Seconds slider
            final JLabel lblSeconds = new JLabel();
            final JSlider sldSeconds = new JSlider(-60, 60, 0);
            sldSeconds.setPaintLabels(true);
            sldSeconds.setMajorTickSpacing(30);

            // This is called whenever one of the sliders is moved.
            // It updates the labels and also calls the "match photos" code
            class sliderListener implements ChangeListener {
                public void stateChanged(ChangeEvent e) {
                    // parse slider position into real timezone
                    double tz = Math.abs(sldTimezone.getValue());
                    String zone = tz % 2 == 0
                    ? (int)Math.floor(tz/2) + ":00"
                            : (int)Math.floor(tz/2) + ":30";
                    if(sldTimezone.getValue() < 0) {
                        zone = "-" + zone;
                    }

                    lblTimezone.setText(tr("Timezone: {0}", zone));
                    lblMinutes.setText(tr("Minutes: {0}", sldMinutes.getValue()));
                    lblSeconds.setText(tr("Seconds: {0}", sldSeconds.getValue()));

                    try {
                        timezone = parseTimezone(zone);
                    } catch (ParseException pe) {
                        throw new RuntimeException();
                    }
                    delta = sldMinutes.getValue()*60 + sldSeconds.getValue();

                    tfTimezone.getDocument().removeDocumentListener(statusBarListener);
                    tfOffset.getDocument().removeDocumentListener(statusBarListener);
                    
                    tfTimezone.setText(formatTimezone(timezone));
                    tfOffset.setText(Long.toString(delta + dayOffset*24*60*60));    // add the day offset to the offset field

                    tfTimezone.getDocument().addDocumentListener(statusBarListener);
                    tfOffset.getDocument().addDocumentListener(statusBarListener);



                    lblMatches.setText(statusBarText.getText() + tr("<br>(Time difference of {0} days)", Math.abs(dayOffset)));

                    statusBarListener.updateStatusBar();
                    yLayer.updateBufferAndRepaint();
                }
            }

            // Put everything together
            JPanel p = new JPanel(new GridBagLayout());
            p.setPreferredSize(new Dimension(400, 230));
            p.add(lblMatches, GBC.eol().fill());
            p.add(lblTimezone, GBC.eol().fill());
            p.add(sldTimezone, GBC.eol().fill().insets(0, 0, 0, 10));
            p.add(lblMinutes, GBC.eol().fill());
            p.add(sldMinutes, GBC.eol().fill().insets(0, 0, 0, 10));
            p.add(lblSeconds, GBC.eol().fill());
            p.add(sldSeconds, GBC.eol().fill());

            // If there's an error in the calculation the found values
            // will be off range for the sliders. Catch this error
            // and inform the user about it.
            try {
                sldTimezone.setValue((int)(fixTimezone*2));
                sldMinutes.setValue(offset/60);
                sldSeconds.setValue(offset%60);
            } catch(Exception e) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("An error occurred while trying to match the photos to the GPX track."
                                +" You can adjust the sliders to manually match the photos."),
                                tr("Matching photos to track failed"),
                                JOptionPane.WARNING_MESSAGE);
            }

            // Call the sliderListener once manually so labels get adjusted
            new sliderListener().stateChanged(null);
            // Listeners added here, otherwise it tries to match three times
            // (when setting the default values)
            sldTimezone.addChangeListener(new sliderListener());
            sldMinutes.addChangeListener(new sliderListener());
            sldSeconds.addChangeListener(new sliderListener());

            // There is no way to cancel this dialog, all changes get applied
            // immediately. Therefore "Close" is marked with an "OK" icon.
            // Settings are only saved temporarily to the layer.
            new ExtendedDialog(Main.parent,
                    tr("Adjust timezone and offset"),
                    new String[] { tr("Close")}).
                setContent(p).setButtonIcons(new String[] {"ok.png"}).showDialog();
        }
    }

    private class AutoGuessActionListener implements ActionListener {
    
        public void actionPerformed(ActionEvent arg0) {
            GpxDataWrapper gpxW = selectedGPX(true);
            if (gpxW == null)
                return;
            GpxData gpx = gpxW.data;
            
            ArrayList<ImageEntry> imgs = getSortedImgList();
            PrimaryDateParser dateParser = new PrimaryDateParser();

            // no images found, exit
            if(imgs.size() <= 0) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("The selected photos don't contain time information."),
                        tr("Photos don't contain time information"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Init variables
            long firstExifDate = imgs.get(0).time.getTime()/1000;

            long firstGPXDate = -1;
            // Finds first GPX point
            outer: for (GpxTrack trk : gpx.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint curWp : segment) {
                        String curDateWpStr = (String) curWp.attr.get("time");
                        if (curDateWpStr == null) {
                            continue;
                        }

                        try {
                            firstGPXDate = dateParser.parse(curDateWpStr).getTime()/1000;
                            break outer;
                        } catch(Exception e) {}
                    }
                }
            }

            // No GPX timestamps found, exit
            if(firstGPXDate < 0) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("The selected GPX track doesn't contain timestamps. Please select another one."),
                        tr("GPX Track has no time information"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            // seconds
            long diff = firstExifDate - firstGPXDate;

            double diffInH = (double)diff/(60*60);    // hours

            // Find day difference
            int dayOffset = (int)Math.round(diffInH / 24); // days
            double tz = diff - dayOffset*24*60*60;  // seconds

            // In hours, rounded to two decimal places
            tz = (double)Math.round(tz*100/(60*60)) / 100;

            // Due to imprecise clocks we might get a "+3:28" timezone, which should obviously be 3:30 with
            // -2 minutes offset. This determines the real timezone and finds offset.
            timezone = (double)Math.round(tz * 2)/2; // hours, rounded to one decimal place
            delta = (long)Math.round(diff - timezone*60*60); // seconds

            /*System.out.println("phto " + firstExifDate);
            System.out.println("gpx  " + firstGPXDate);
            System.out.println("diff " + diff);
            System.out.println("difh " + diffInH);
            System.out.println("days " + dayOffset);
            System.out.println("time " + tz);
            System.out.println("fix  " + timezone);
            System.out.println("offt " + delta);*/

            tfTimezone.getDocument().removeDocumentListener(statusBarListener);
            tfOffset.getDocument().removeDocumentListener(statusBarListener);
            
            tfTimezone.setText(formatTimezone(timezone));
            tfOffset.setText(Long.toString(delta));
            tfOffset.requestFocus();

            tfTimezone.getDocument().addDocumentListener(statusBarListener);
            tfOffset.getDocument().addDocumentListener(statusBarListener);
            
            statusBarListener.updateStatusBar();
            yLayer.updateBufferAndRepaint();
        }
    }

    private ArrayList<ImageEntry>  getSortedImgList() {
        return getSortedImgList(cbExifImg.isSelected(), cbTaggedImg.isSelected());
    }
    
    /**
     * Returns a list of images that fulfill the given criteria.
     * Default setting is to return untagged images, but may be overwritten.
     * @param boolean all -- returns all available images
     * @param boolean noexif -- returns untagged images without EXIF-GPS coords 
     *                          this parameter is irrelevant if <code>all</code> is true
     * @param boolean exif -- also returns images with exif-gps info
     * @param boolean tagged -- also returns tagged images
     * @return ArrayList<ImageEntry> matching images
     */
    private ArrayList<ImageEntry> getSortedImgList(boolean exif, boolean tagged) {
        ArrayList<ImageEntry> dateImgLst = new ArrayList<ImageEntry>(yLayer.data.size());
        for (ImageEntry e : yLayer.data) {
            if (e.time == null)
                continue;
                
            if (e.exifCoor != null) {
                if (!exif)
                    continue;
            }
                
            if (e.isTagged() && e.exifCoor == null) {
                if (!tagged)
                    continue;
            }
                
            dateImgLst.add(e);
        }
        
        Collections.sort(dateImgLst, new Comparator<ImageEntry>() {
            public int compare(ImageEntry arg0, ImageEntry arg1) {
                return arg0.time.compareTo(arg1.time);
            }
        });

        return dateImgLst;
    }

    private GpxDataWrapper selectedGPX(boolean complain) {
        Object item = cbGpx.getSelectedItem();

        if (item == null || ! (item instanceof GpxDataWrapper)) {
            if (complain) {
                JOptionPane.showMessageDialog(Main.parent, tr("You should select a GPX track"),
                        tr("No selected GPX track"), JOptionPane.ERROR_MESSAGE );
            }
            return null;
        }
        return (GpxDataWrapper) item;
    }

    private int matchGpxTrack(ArrayList<ImageEntry> dateImgLst, GpxData selectedGpx, long offset) {
        int ret = 0;

        PrimaryDateParser dateParser = new PrimaryDateParser();

        for (GpxTrack trk : selectedGpx.tracks) {
            for (Collection<WayPoint> segment : trk.trackSegs) {

                long prevDateWp = 0;
                WayPoint prevWp = null;

                for (WayPoint curWp : segment) {

                    String curDateWpStr = (String) curWp.attr.get("time");
                    if (curDateWpStr != null) {

                        try {
                            long curDateWp = dateParser.parse(curDateWpStr).getTime()/1000 + offset;
                            ret += matchPoints(dateImgLst, prevWp, prevDateWp, curWp, curDateWp);

                            prevWp = curWp;
                            prevDateWp = curDateWp;

                        } catch(ParseException e) {
                            System.err.println("Error while parsing date \"" + curDateWpStr + '"');
                            e.printStackTrace();
                            prevWp = null;
                            prevDateWp = 0;
                        }
                    } else {
                        prevWp = null;
                        prevDateWp = 0;
                    }
                }
            }
        }
        return ret;
    }

    private int matchPoints(ArrayList<ImageEntry> dateImgLst, WayPoint prevWp, long prevDateWp,
            WayPoint curWp, long curDateWp) {
        // Time between the track point and the previous one, 5 sec if first point, i.e. photos take
        // 5 sec before the first track point can be assumed to be take at the starting position
        long interval = prevDateWp > 0 ? ((int)Math.abs(curDateWp - prevDateWp)) : 5;
        int ret = 0;

        // i is the index of the timewise last photo that has the same or earlier EXIF time
        int i = getLastIndexOfListBefore(dateImgLst, curDateWp);

        // no photos match
        if (i < 0)
            return 0;

        Double speed = null;
        Double prevElevation = null;
        Double curElevation = null;

        if (prevWp != null) {
            double distance = prevWp.getCoor().greatCircleDistance(curWp.getCoor());
            // This is in km/h, 3.6 * m/s
            if (curDateWp > prevDateWp) {
                speed = 3.6 * distance / (curDateWp - prevDateWp);
            }
            try {
                prevElevation = new Double((String) prevWp.attr.get("ele"));
            } catch(Exception e) {}
        }

        try {
            curElevation = new Double((String) curWp.attr.get("ele"));
        } catch (Exception e) {}

        // First trackpoint, then interval is set to five seconds, i.e. photos up to five seconds
        // before the first point will be geotagged with the starting point
        if(prevDateWp == 0 || curDateWp <= prevDateWp) {
            while(i >= 0 && (dateImgLst.get(i).time.getTime()/1000) <= curDateWp
                    && (dateImgLst.get(i).time.getTime()/1000) >= (curDateWp - interval)) {
                if(dateImgLst.get(i).tmp.getPos() == null) {
                    dateImgLst.get(i).tmp.setCoor(curWp.getCoor());
                    dateImgLst.get(i).tmp.setSpeed(speed);
                    dateImgLst.get(i).tmp.setElevation(curElevation);
                    ret++;
                }
                i--;
            }
            return ret;
        }

        // This code gives a simple linear interpolation of the coordinates between current and
        // previous track point assuming a constant speed in between
        long imgDate;
        while(i >= 0 && (imgDate = dateImgLst.get(i).time.getTime()/1000) >= prevDateWp) {

            if(dateImgLst.get(i).tmp.getPos() == null) {
                // The values of timeDiff are between 0 and 1, it is not seconds but a dimensionless
                // variable
                double timeDiff = (double)(imgDate - prevDateWp) / interval;
                dateImgLst.get(i).tmp.setCoor(prevWp.getCoor().interpolate(curWp.getCoor(), timeDiff));
                dateImgLst.get(i).tmp.setSpeed(speed);

                if (curElevation != null && prevElevation != null) {
                    dateImgLst.get(i).setElevation(prevElevation + (curElevation - prevElevation) * timeDiff);
                }

                ret++;
            }
            i--;
        }
        return ret;
    }

    private int getLastIndexOfListBefore(ArrayList<ImageEntry> dateImgLst, long searchedDate) {
        int lstSize= dateImgLst.size();

        // No photos or the first photo taken is later than the search period
        if(lstSize == 0 || searchedDate < dateImgLst.get(0).time.getTime()/1000)
            return -1;

        // The search period is later than the last photo
        if (searchedDate > dateImgLst.get(lstSize - 1).time.getTime() / 1000)
            return lstSize-1;

        // The searched index is somewhere in the middle, do a binary search from the beginning
        int curIndex= 0;
        int startIndex= 0;
        int endIndex= lstSize-1;
        while (endIndex - startIndex > 1) {
            curIndex= (endIndex + startIndex) / 2;
            if (searchedDate > dateImgLst.get(curIndex).time.getTime()/1000) {
                startIndex= curIndex;
            } else {
                endIndex= curIndex;
            }
        }
        if (searchedDate < dateImgLst.get(endIndex).time.getTime()/1000)
            return startIndex;

        // This final loop is to check if photos with the exact same EXIF time follows
        while ((endIndex < (lstSize-1)) && (dateImgLst.get(endIndex).time.getTime()
                == dateImgLst.get(endIndex + 1).time.getTime())) {
            endIndex++;
        }
        return endIndex;
    }


    private String formatTimezone(double timezone) {
        StringBuffer ret = new StringBuffer();

        if (timezone < 0) {
            ret.append('-');
            timezone = -timezone;
        } else {
            ret.append('+');
        }
        ret.append((long) timezone).append(':');
        int minutes = (int) ((timezone % 1) * 60);
        if (minutes < 10) {
            ret.append('0');
        }
        ret.append(minutes);

        return ret.toString();
    }

    private double parseTimezone(String timezone) throws ParseException {
 
        String error = tr("Error while parsing timezone.\nExpected format: {0}", "+H:MM");
 
 
        if (timezone.length() == 0)
            return 0;

        char sgnTimezone = '+';
        StringBuffer hTimezone = new StringBuffer();
        StringBuffer mTimezone = new StringBuffer();
        int state = 1; // 1=start/sign, 2=hours, 3=minutes.
        for (int i = 0; i < timezone.length(); i++) {
            char c = timezone.charAt(i);
            switch (c) {
            case ' ' :
                if (state != 2 || hTimezone.length() != 0)
                    throw new ParseException(error,0);
                break;
            case '+' :
            case '-' :
                if (state == 1) {
                    sgnTimezone = c;
                    state = 2;
                } else
                    throw new ParseException(error,0);
                break;
            case ':' :
            case '.' :
                if (state == 2) {
                    state = 3;
                } else
                    throw new ParseException(error,0);
                break;
            case '0' : case '1' : case '2' : case '3' : case '4' :
            case '5' : case '6' : case '7' : case '8' : case '9' :
                switch(state) {
                case 1 :
                case 2 :
                    state = 2;
                    hTimezone.append(c);
                    break;
                case 3 :
                    mTimezone.append(c);
                    break;
                default :
                    throw new ParseException(error,0);
                }
                break;
            default :
                throw new ParseException(error,0);
            }
        }

        int h = 0;
        int m = 0;
        try {
            h = Integer.parseInt(hTimezone.toString());
            if (mTimezone.length() > 0) {
                m = Integer.parseInt(mTimezone.toString());
            }
        } catch (NumberFormatException nfe) {
            // Invalid timezone
            throw new ParseException(error,0);
        }

        if (h > 12 || m > 59 )
            throw new ParseException(error,0);
        else
            return (h + m / 60.0) * (sgnTimezone == '-' ? -1 : 1);
    }

    private long parseOffset(String offset) throws ParseException {
        String error = tr("Error while parsing offset.\nExpected format: {0}", "number");
    
        if (offset.length() > 0) {
            try {
                if(offset.startsWith("+")) {
                    offset = offset.substring(1);
                }
                return Long.parseLong(offset);
            } catch(NumberFormatException nfe) {
                throw new ParseException(error,0);
            }
        } else {
            return 0;
        }
    }
}
