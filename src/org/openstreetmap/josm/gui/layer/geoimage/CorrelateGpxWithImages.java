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
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer.ImageEntry;
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

    public static class CorrelateParameters {
        GpxData gpxData;
        float timezone;
        long offset;
    }

    GeoImageLayer yLayer = null;

    private static class GpxDataWrapper {
        String name;
        GpxData data;
        File file;

        public GpxDataWrapper(String name, GpxData data, File file) {
            this.name = name;
            this.data = data;
            this.file = file;
        }

        public String toString() {
            return name;
        }
    }

    Vector gpxLst = new Vector();
    JPanel panel = null;
    JComboBox cbGpx = null;
    JTextField tfTimezone = null;
    JTextField tfOffset = null;
    JRadioButton rbAllImg = null;
    JRadioButton rbUntaggedImg = null;
    JRadioButton rbNoExifImg = null;

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
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                Main.pref.put("lastDirectory", sel.getPath());

                for (int i = gpxLst.size() - 1 ; i >= 0 ; i--) {
                    if (gpxLst.get(i) instanceof GpxDataWrapper) {
                        GpxDataWrapper wrapper = (GpxDataWrapper) gpxLst.get(i);
                        if (sel.equals(wrapper.file)) {
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
                if (gpxLst.get(0) instanceof String) {
                    gpxLst.remove(0);
                }
                gpxLst.add(new GpxDataWrapper(sel.getName(), data, sel));
                cbGpx.setSelectedIndex(cbGpx.getItemCount() - 1);
            } finally {
                panel.setCursor(Cursor.getDefaultCursor());
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

            tfGpsTime = new JTextField();
            tfGpsTime.setEnabled(false);
            tfGpsTime.setMinimumSize(new Dimension(150, tfGpsTime.getMinimumSize().height));
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

            Vector vtTimezones = new Vector<String>();
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
                        tfGpsTime.setText(new SimpleDateFormat("dd/MM/yyyy ").format(date));
                        tfGpsTime.setCaretPosition(tfGpsTime.getText().length());
                        tfGpsTime.setEnabled(true);
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
                    if (sel == null) {
                        return;
                    }

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
                if (answer == JOptionPane.CANCEL_OPTION) {
                    return;
                }

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

        }
    }

    public CorrelateGpxWithImages(GeoImageLayer layer) {
        this.yLayer = layer;
    }

    public void actionPerformed(ActionEvent arg0) {
        // Construct the list of loaded GPX tracks
        Collection<Layer> layerLst = Main.main.map.mapView.getAllLayers();
        Iterator<Layer> iterLayer = layerLst.iterator();
        while (iterLayer.hasNext()) {
            Layer cur = iterLayer.next();
            if (cur instanceof GpxLayer) {
                gpxLst.add(new GpxDataWrapper(((GpxLayer) cur).getName(),
                                              ((GpxLayer) cur).data,
                                              ((GpxLayer) cur).data.storageFile));
            }
        }
        for (GpxData data : loadedGpxData) {
            gpxLst.add(new GpxDataWrapper(data.storageFile.getName(),
                                          data,
                                          data.storageFile));
        }

        if (gpxLst.size() == 0) {
            gpxLst.add(tr("<No GPX track loaded yet>"));
        }

        JPanel panelCb = new JPanel();
        panelCb.setLayout(new FlowLayout());

        panelCb.add(new JLabel(tr("GPX track: ")));

        cbGpx = new JComboBox(gpxLst);
        panelCb.add(cbGpx);

        JButton buttonOpen = new JButton(tr("Open another GPX trace"));
        buttonOpen.setIcon(ImageProvider.get("dialogs/geoimage/geoimage-open"));
        buttonOpen.addActionListener(new LoadGpxDataActionListener());

        panelCb.add(buttonOpen);

        JPanel panelTf = new JPanel();
        panelTf.setLayout(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = gc.gridy = 0;
        gc.gridwidth = gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = gc.weighty = 0.0;
        panelTf.add(new JLabel(tr("Timezone: ")), gc);

        float gpstimezone = Float.parseFloat(Main.pref.get("geoimage.doublegpstimezone", "0.0"));
        if (gpstimezone == 0.0) {
            gpstimezone = - Long.parseLong(Main.pref.get("geoimage.gpstimezone", "0"));
        }
        tfTimezone = new JTextField();
        tfTimezone.setText(formatTimezone(gpstimezone));

        gc.gridx = 1;
        gc.gridy = 0;
        gc.gridwidth = gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        panelTf.add(tfTimezone, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = gc.weighty = 0.0;
        panelTf.add(new JLabel(tr("Offset:")), gc);

        long delta = Long.parseLong(Main.pref.get("geoimage.delta", "0")) / 1000;
        tfOffset = new JTextField();
        tfOffset.setText(Long.toString(delta));
        gc.gridx = gc.gridy = 1;
        gc.gridwidth = gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        panelTf.add(tfOffset, gc);

        JButton buttonViewGpsPhoto = new JButton(tr("<html>I can take a picture of my GPS receiver.<br>"
                                                    + "Can this help?</html>"));
        buttonViewGpsPhoto.addActionListener(new SetOffsetActionListener());
        gc.gridx = 2;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 0.5;
        gc.weighty = 1.0;
        panelTf.add(buttonViewGpsPhoto, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = gc.weighty = 0.0;
        panelTf.add(new JLabel(tr("Update position for: ")), gc);

        gc.gridx = 1;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        rbAllImg = new JRadioButton(tr("All images"));
        panelTf.add(rbAllImg, gc);

        gc.gridx = 1;
        gc.gridy = 3;
        gc.gridwidth = 2;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        rbNoExifImg = new JRadioButton(tr("Images with no exif position"));
        panelTf.add(rbNoExifImg, gc);

        gc.gridx = 1;
        gc.gridy = 4;
        gc.gridwidth = 2;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        rbUntaggedImg = new JRadioButton(tr("Not yet tagged images"));
        panelTf.add(rbUntaggedImg, gc);

        gc.gridx = 0;
        gc.gridy = 5;
        gc.gridwidth = 2;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = gc.weighty = 0.0;
        yLayer.useThumbs = Main.pref.getBoolean("geoimage.showThumbs", false);
        JCheckBox cbShowThumbs = new JCheckBox(tr("Show Thumbnail images on the map"), yLayer.useThumbs);
        panelTf.add(cbShowThumbs, gc);

        ButtonGroup group = new ButtonGroup();
        group.add(rbAllImg);
        group.add(rbNoExifImg);
        group.add(rbUntaggedImg);

        rbUntaggedImg.setSelected(true);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        panel.add(panelCb, BorderLayout.PAGE_START);
        panel.add(panelTf, BorderLayout.CENTER);

        boolean isOk = false;
        GpxDataWrapper selectedGpx = null;
        while (! isOk) {
            ExtendedDialog dialog = new ExtendedDialog(
                    Main.parent,
                tr("Correlate images with GPX track"),
                new String[] { tr("Correlate"), tr("Auto-Guess"), tr("Cancel") }
                    );

            dialog.setContent(panel);
            dialog.setButtonIcons(new String[] { "ok.png", "dialogs/geoimage/gpx2imgManual.png", "cancel.png" });
            dialog.showDialog();
            int answer = dialog.getValue();
            if(answer != 1 && answer != 2)
                return;

            // Check the selected values
            Object item = cbGpx.getSelectedItem();

            if (item == null || ! (item instanceof GpxDataWrapper)) {
                JOptionPane.showMessageDialog(Main.parent, tr("You should select a GPX track"),
                                              tr("No selected GPX track"), JOptionPane.ERROR_MESSAGE );
                continue;
            }
            selectedGpx = ((GpxDataWrapper) item);

            if (answer == 2) {
                autoGuess(selectedGpx.data);
                return;
            }

            Float timezoneValue = parseTimezone(tfTimezone.getText().trim());
            if (timezoneValue == null) {
                JOptionPane.showMessageDialog(Main.parent, tr("Error while parsing timezone.\nExpected format: {0}", "+H:MM"),
                        tr("Invalid timezone"), JOptionPane.ERROR_MESSAGE);
                continue;
            }
            gpstimezone = timezoneValue.floatValue();

            String deltaText = tfOffset.getText().trim();
            if (deltaText.length() > 0) {
                try {
                    if(deltaText.startsWith("+"))
                        deltaText = deltaText.substring(1);
                    delta = Long.parseLong(deltaText);
                } catch(NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(Main.parent, tr("Error while parsing offset.\nExpected format: {0}", "number"),
                            tr("Invalid offset"), JOptionPane.ERROR_MESSAGE);
                    continue;
                }
            } else {
                delta = 0;
            }

            yLayer.useThumbs = cbShowThumbs.isSelected();

            Main.pref.put("geoimage.doublegpstimezone", Double.toString(gpstimezone));
            Main.pref.put("geoimage.gpstimezone", Long.toString(- ((long) gpstimezone)));
            Main.pref.put("geoimage.delta", Long.toString(delta * 1000));
            Main.pref.put("geoimage.showThumbs", yLayer.useThumbs);
            isOk = true;

            if (yLayer.useThumbs) {
                yLayer.thumbsloader = new ThumbsLoader(yLayer);
                Thread t = new Thread(yLayer.thumbsloader);
                t.setPriority(Thread.MIN_PRIORITY);
                t.start();
            }

        }

        // Construct a list of images that have a date, and sort them on the date.
        ArrayList<ImageEntry> dateImgLst = getSortedImgList(rbAllImg.isSelected(), rbNoExifImg.isSelected());

        int matched = matchGpxTrack(dateImgLst, selectedGpx.data, (long) (gpstimezone * 3600) + delta);

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

        Main.map.repaint();

        JOptionPane.showMessageDialog(Main.parent, tr("Found {0} matches of {1} in GPX track {2}", matched, dateImgLst.size(), selectedGpx.name),
                tr("GPX Track loaded"),
                ((dateImgLst.size() > 0 && matched == 0) ? JOptionPane.WARNING_MESSAGE
                                                         : JOptionPane.INFORMATION_MESSAGE));

    }

    // These variables all belong to "auto guess" but need to be accessible
    // from the slider change listener
    private int dayOffset;
    private JLabel lblMatches;
    private JLabel lblOffset;
    private JLabel lblTimezone;
    private JLabel lblMinutes;
    private JLabel lblSeconds;
    private JSlider sldTimezone;
    private JSlider sldMinutes;
    private JSlider sldSeconds;
    private GpxData autoGpx;
    private ArrayList<ImageEntry> autoImgs;
    private long firstGPXDate = -1;
    private long firstExifDate = -1;

    /**
     * Tries to automatically match opened photos to a given GPX track. Changes are applied
     * immediately. Presents dialog with sliders for manual adjust.
     * @param GpxData The GPX track to match against
     */
    private void autoGuess(GpxData gpx) {
        autoGpx = gpx;
        autoImgs = getSortedImgList(true, false);
        PrimaryDateParser dateParser = new PrimaryDateParser();

        // no images found, exit
        if(autoImgs.size() <= 0) {
            JOptionPane.showMessageDialog(Main.parent,
                tr("The selected photos don't contain time information."),
                tr("Photos don't contain time information"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        ImageViewerDialog dialog = ImageViewerDialog.getInstance();
        dialog.showDialog();
        // Will show first photo if none is selected yet
        if(!dialog.hasImage())
            yLayer.showNextPhoto();
        // FIXME: If the dialog is minimized it will not be maximized. ToggleDialog is
        // in need of a complete re-write to allow this in a reasonable way.

        // Init variables
        firstExifDate = autoImgs.get(0).time.getTime()/1000;


        // Finds first GPX point
        outer: for (GpxTrack trk : gpx.tracks) {
            for (Collection<WayPoint> segment : trk.trackSegs) {
                for (WayPoint curWp : segment) {
                    String curDateWpStr = (String) curWp.attr.get("time");
                    if (curDateWpStr == null) continue;

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
        long diff = (yLayer.hasTimeoffset)
            ? yLayer.timeoffset
            : firstExifDate - firstGPXDate;
        yLayer.timeoffset = diff;
        yLayer.hasTimeoffset = true;

        double diffInH = (double)diff/(60*60);    // hours

        // Find day difference
        dayOffset = (int)Math.round(diffInH / 24); // days
        double timezone = diff - dayOffset*24*60*60;  // seconds

        // In hours, rounded to two decimal places
        timezone = (double)Math.round(timezone*100/(60*60)) / 100;

        // Due to imprecise clocks we might get a "+3:28" timezone, which should obviously be 3:30 with
        // -2 minutes offset. This determines the real timezone and finds offset.
        double fixTimezone = (double)Math.round(timezone * 2)/2; // hours, rounded to one decimal place
        int offset = (int)Math.round(diff - fixTimezone*60*60) - dayOffset*24*60*60; // seconds

        /*System.out.println("phto " + firstExifDate);
        System.out.println("gpx  " + firstGPXDate);
        System.out.println("diff " + diff);
        System.out.println("difh " + diffInH);
        System.out.println("days " + dayOffset);
        System.out.println("time " + timezone);
        System.out.println("fix  " + fixTimezone);
        System.out.println("offt " + offset);*/

        // This is called whenever one of the sliders is moved.
        // It updates the labels and also calls the "match photos" code
        class sliderListener implements ChangeListener {
            public void stateChanged(ChangeEvent e) {
                // parse slider position into real timezone
                double tz = Math.abs(sldTimezone.getValue());
                String zone = tz % 2 == 0
                    ? (int)Math.floor(tz/2) + ":00"
                    : (int)Math.floor(tz/2) + ":30";
                if(sldTimezone.getValue() < 0) zone = "-" + zone;

                lblTimezone.setText(tr("Timezone: {0}", zone));
                lblMinutes.setText(tr("Minutes: {0}", sldMinutes.getValue()));
                lblSeconds.setText(tr("Seconds: {0}", sldSeconds.getValue()));

                float gpstimezone = parseTimezone(zone).floatValue();

                // Reset previous position
                for(ImageEntry x : autoImgs) {
                    x.pos = null;
                }

                long timediff = (long) (gpstimezone * 3600)
                        + dayOffset*24*60*60
                        + sldMinutes.getValue()*60
                        + sldSeconds.getValue();

                int matched = matchGpxTrack(autoImgs, autoGpx, timediff);

                lblMatches.setText(
                    tr("Matched {0} of {1} photos to GPX track.", matched, autoImgs.size())
                    + ((Math.abs(dayOffset) == 0)
                        ? ""
                        : " " + tr("(Time difference of {0} days)", Math.abs(dayOffset))
                      )
                );

                int offset = (int)(firstGPXDate+timediff-firstExifDate);
                int o = Math.abs(offset);
                lblOffset.setText(
                    tr("Offset between track and photos: {0}m {1}s",
                          (offset < 0 ? "-" : "") + Long.toString(Math.round(o/60)),
                          Long.toString(Math.round(o%60))
                    )
                );

                yLayer.timeoffset = timediff;
                Main.main.map.repaint();
            }
        }

        // Info Labels
        lblMatches = new JLabel();
        lblOffset = new JLabel();

        // Timezone Slider
        // The slider allows to switch timezon from -12:00 to 12:00 in 30 minutes
        // steps. Therefore the range is -24 to 24.
        lblTimezone = new JLabel();
        sldTimezone = new JSlider(-24, 24, 0);
        sldTimezone.setPaintLabels(true);
        Hashtable<Integer,JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(-24, new JLabel("-12:00"));
        labelTable.put(-12, new JLabel( "-6:00"));
        labelTable.put(  0, new JLabel(  "0:00"));
        labelTable.put( 12, new JLabel(  "6:00"));
        labelTable.put( 24, new JLabel( "12:00"));
        sldTimezone.setLabelTable(labelTable);

        // Minutes Slider
        lblMinutes = new JLabel();
        sldMinutes = new JSlider(-15, 15, 0);
        sldMinutes.setPaintLabels(true);
        sldMinutes.setMajorTickSpacing(5);

        // Seconds slider
        lblSeconds = new JLabel();
        sldSeconds = new JSlider(-60, 60, 0);
        sldSeconds.setPaintLabels(true);
        sldSeconds.setMajorTickSpacing(30);

        // Put everything together
        JPanel p = new JPanel(new GridBagLayout());
        p.setPreferredSize(new Dimension(400, 230));
        p.add(lblMatches, GBC.eol().fill());
        p.add(lblOffset, GBC.eol().fill().insets(0, 0, 0, 10));
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
        ExtendedDialog d = new ExtendedDialog(Main.parent,
            tr("Adjust timezone and offset"),
            new String[] { tr("Close"),  tr("Default Values") }
        );

        d.setContent(p);
        d.setButtonIcons(new String[] { "ok.png", "dialogs/refresh.png"});
        d.showDialog();
        int answer = d.getValue();
        // User wants default values; discard old result and re-open dialog
        if(answer == 2) {
            yLayer.hasTimeoffset = false;
            autoGuess(gpx);
        }
    }

    /**
     * Returns a list of images that fulfill the given criteria.
     * Default setting is to return untagged images, but may be overwritten.
     * @param boolean all -- returns all available images
     * @param boolean noexif -- returns untagged images without EXIF-GPS coords
     * @return ArrayList<ImageEntry> matching images
     */
    private ArrayList<ImageEntry> getSortedImgList(boolean all, boolean noexif) {
        ArrayList<ImageEntry> dateImgLst = new ArrayList<ImageEntry>(yLayer.data.size());
        if (all) {
            for (ImageEntry e : yLayer.data) {
                if (e.time != null) {
                    // Reset previous position
                    e.pos = null;
                    dateImgLst.add(e);
                }
            }

        } else if (noexif) {
            for (ImageEntry e : yLayer.data) {
                if (e.time != null && e.exifCoor == null) {
                    dateImgLst.add(e);
                }
            }

        } else {
            for (ImageEntry e : yLayer.data) {
                if (e.time != null && e.pos == null) {
                    dateImgLst.add(e);
                }
            }
        }

        Collections.sort(dateImgLst, new Comparator<ImageEntry>() {
            public int compare(ImageEntry arg0, ImageEntry arg1) {
                return arg0.time.compareTo(arg1.time);
            }
        });

        return dateImgLst;
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
            if (curDateWp > prevDateWp)
                speed = 3.6 * distance / (curDateWp - prevDateWp);
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
                if(dateImgLst.get(i).pos == null) {
                    dateImgLst.get(i).setCoor(curWp.getCoor());
                    dateImgLst.get(i).speed = speed;
                    dateImgLst.get(i).elevation = curElevation;
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

            if(dateImgLst.get(i).pos == null) {
                // The values of timeDiff are between 0 and 1, it is not seconds but a dimensionless
                // variable
                double timeDiff = (double)(imgDate - prevDateWp) / interval;
                dateImgLst.get(i).setCoor(prevWp.getCoor().interpolate(curWp.getCoor(), timeDiff));
                dateImgLst.get(i).speed = speed;

                if (curElevation != null && prevElevation != null)
                    dateImgLst.get(i).elevation = prevElevation + (curElevation - prevElevation) * timeDiff;

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
            curIndex= (int) Math.round((double)(endIndex + startIndex)/2);
            if (searchedDate > dateImgLst.get(curIndex).time.getTime()/1000)
                startIndex= curIndex;
            else
                endIndex= curIndex;
        }
        if (searchedDate < dateImgLst.get(endIndex).time.getTime()/1000)
            return startIndex;

        // This final loop is to check if photos with the exact same EXIF time follows
        while ((endIndex < (lstSize-1)) && (dateImgLst.get(endIndex).time.getTime()
                                                == dateImgLst.get(endIndex + 1).time.getTime()))
            endIndex++;
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

    private Float parseTimezone(String timezone) {
        if (timezone.length() == 0) {
            return new Float(0);
        }

        char sgnTimezone = '+';
        StringBuffer hTimezone = new StringBuffer();
        StringBuffer mTimezone = new StringBuffer();
        int state = 1; // 1=start/sign, 2=hours, 3=minutes.
        for (int i = 0; i < timezone.length(); i++) {
            char c = timezone.charAt(i);
            switch (c) {
            case ' ' :
                if (state != 2 || hTimezone.length() != 0) {
                    return null;
                }
                break;
            case '+' :
            case '-' :
                if (state == 1) {
                    sgnTimezone = c;
                    state = 2;
                } else {
                    return null;
                }
                break;
            case ':' :
            case '.' :
                if (state == 2) {
                    state = 3;
                } else {
                    return null;
                }
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
                    return null;
                }
                break;
            default :
                return null;
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
            return null;
        }

        if (h > 12 || m > 59 ) {
            return null;
        } else {
            return new Float((h + m / 60.0) * (sgnTimezone == '-' ? -1 : 1));
        }
    }
}
