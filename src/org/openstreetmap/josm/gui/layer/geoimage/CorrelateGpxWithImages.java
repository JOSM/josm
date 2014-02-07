// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
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
import org.openstreetmap.josm.actions.DiskAccessAction;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.PrimaryDateParser;
import org.xml.sax.SAXException;

/**
 * This class displays the window to select the GPX file and the offset (timezone + delta).
 * Then it correlates the images of the layer with that GPX file.
 */
public class CorrelateGpxWithImages extends AbstractAction {

    private static List<GpxData> loadedGpxData = new ArrayList<GpxData>();

    GeoImageLayer yLayer = null;
    double timezone;
    long delta;

    /**
     * Constructs a new {@code CorrelateGpxWithImages} action.
     * @param layer The image layer
     */
    public CorrelateGpxWithImages(GeoImageLayer layer) {
        super(tr("Correlate to GPX"), ImageProvider.get("dialogs/geoimage/gpx2img"));
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
    List<GpxDataWrapper> gpxLst = new ArrayList<GpxDataWrapper>();
    JPanel outerPanel;
    JosmComboBox cbGpx;
    JosmTextField tfTimezone;
    JosmTextField tfOffset;
    JCheckBox cbExifImg;
    JCheckBox cbTaggedImg;
    JCheckBox cbShowThumbs;
    JLabel statusBarText;

    // remember the last number of matched photos
    int lastNumMatched = 0;

    /** This class is called when the user doesn't find the GPX file he needs in the files that have
     * been loaded yet. It displays a FileChooser dialog to select the GPX file to be loaded.
     */
    private class LoadGpxDataActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {
            FileFilter filter = new FileFilter(){
                @Override public boolean accept(File f) {
                    return (f.isDirectory()
                            || f .getName().toLowerCase().endsWith(".gpx")
                            || f.getName().toLowerCase().endsWith(".gpx.gz"));
                }
                @Override public String getDescription() {
                    return tr("GPX Files (*.gpx *.gpx.gz)");
                }
            };
            JFileChooser fc = DiskAccessAction.createAndOpenFileChooser(true, false, null, filter, JFileChooser.FILES_ONLY, null);
            if (fc == null)
                return;
            File sel = fc.getSelectedFile();

            try {
                outerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

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
                    GpxReader reader = new GpxReader(iStream);
                    reader.parse(false);
                    data = reader.getGpxData();
                    data.storageFile = sel;

                } catch (SAXException x) {
                    Main.error(x);
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Error while parsing {0}",sel.getName())+": "+x.getMessage(),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                } catch (IOException x) {
                    Main.error(x);
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

    /**
     * This action listener is called when the user has a photo of the time of his GPS receiver. It
     * displays the list of photos of the layer, and upon selection displays the selected photo.
     * From that photo, the user can key in the time of the GPS.
     * Then values of timezone and delta are set.
     * @author chris
     *
     */
    private class SetOffsetActionListener implements ActionListener {
        JPanel panel;
        JLabel lbExifTime;
        JosmTextField tfGpsTime;
        JosmComboBox cbTimezones;
        ImageDisplay imgDisp;
        JList imgList;

        @Override
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

            tfGpsTime = new JosmTextField(12);
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
            panelTf.add(new JLabel(tr("I am in the timezone of: ")), gc);

            String[] tmp = TimeZone.getAvailableIDs();
            List<String> vtTimezones = new ArrayList<String>(tmp.length);

            for (String tzStr : tmp) {
                TimeZone tz = TimeZone.getTimeZone(tzStr);

                String tzDesc = new StringBuilder(tzStr).append(" (")
                .append(formatTimezone(tz.getRawOffset() / 3600000.0))
                .append(')').toString();
                vtTimezones.add(tzDesc);
            }

            Collections.sort(vtTimezones);

            cbTimezones = new JosmComboBox(vtTimezones.toArray());

            String tzId = Main.pref.get("geoimage.timezoneid", "");
            TimeZone defaultTz;
            if (tzId.length() == 0) {
                defaultTz = TimeZone.getDefault();
            } else {
                defaultTz = TimeZone.getTimeZone(tzId);
            }

            cbTimezones.setSelectedItem(new StringBuilder(defaultTz.getID()).append(" (")
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
                @Override
                public Object getElementAt(int i) {
                    return yLayer.data.get(i).getFile().getName();
                }

                @Override
                public int getSize() {
                    return yLayer.data.size();
                }
            });
            imgList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            imgList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

                @Override
                public void valueChanged(ListSelectionEvent arg0) {
                    int index = imgList.getSelectedIndex();
                    Integer orientation = null;
                    try {
                        orientation = ExifReader.readOrientation(yLayer.data.get(index).getFile());
                    } catch (Exception e) {
                        Main.warn(e);
                    }
                    imgDisp.setImage(yLayer.data.get(index).getFile(), orientation);
                    Date date = yLayer.data.get(index).getExifTime();
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

            JButton openButton = new JButton(tr("Open another photo"));
            openButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    JFileChooser fc = DiskAccessAction.createAndOpenFileChooser(true, false, null, JpegFileFilter.getInstance(), JFileChooser.FILES_ONLY, "geoimage.lastdirectory");
                    if (fc == null)
                        return;
                    File sel = fc.getSelectedFile();

                    Integer orientation = null;
                    try {
                        orientation = ExifReader.readOrientation(sel);
                    } catch (Exception e) {
                        Main.warn(e);
                    }
                    imgDisp.setImage(sel, orientation);

                    Date date = null;
                    try {
                        date = ExifReader.readTime(sel);
                    } catch (Exception e) {
                        Main.warn(e);
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
            statusBarUpdater.updateStatusBar();
            yLayer.updateBufferAndRepaint();
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // Construct the list of loaded GPX tracks
        Collection<Layer> layerLst = Main.map.mapView.getAllLayers();
        GpxDataWrapper defaultItem = null;
        for (Layer cur : layerLst) {
            if (cur instanceof GpxLayer) {
                GpxLayer curGpx = (GpxLayer) cur;
                GpxDataWrapper gdw = new GpxDataWrapper(curGpx.getName(), curGpx.data, curGpx.data.storageFile);
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

        if (gpxLst.isEmpty()) {
            gpxLst.add(new GpxDataWrapper(tr("<No GPX track loaded yet>"), null, null));
        }

        JPanel panelCb = new JPanel();

        panelCb.add(new JLabel(tr("GPX track: ")));

        cbGpx = new JosmComboBox(gpxLst.toArray());
        if (defaultItem != null) {
            cbGpx.setSelectedItem(defaultItem);
        }
        cbGpx.addActionListener(statusBarUpdaterWithRepaint);
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

        tfTimezone = new JosmTextField(10);
        tfTimezone.setText(formatTimezone(timezone));

        try {
            delta = parseOffset(Main.pref.get("geoimage.delta", "0"));
        } catch (ParseException e) {
            delta = 0;
        }
        delta = delta / 1000;  // milliseconds -> seconds

        tfOffset = new JosmTextField(10);
        tfOffset.setText(Long.toString(delta));

        JButton buttonViewGpsPhoto = new JButton(tr("<html>Use photo of an accurate clock,<br>"
                + "e.g. GPS receiver display</html>"));
        buttonViewGpsPhoto.setIcon(ImageProvider.get("clock"));
        buttonViewGpsPhoto.addActionListener(new SetOffsetActionListener());

        JButton buttonAutoGuess = new JButton(tr("Auto-Guess"));
        buttonAutoGuess.setToolTipText(tr("Matches first photo with first gpx point"));
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

        tfTimezone.addFocusListener(repaintTheMap);
        tfOffset.addFocusListener(repaintTheMap);

        tfTimezone.getDocument().addDocumentListener(statusBarUpdater);
        tfOffset.getDocument().addDocumentListener(statusBarUpdater);
        cbExifImg.addItemListener(statusBarUpdaterWithRepaint);
        cbTaggedImg.addItemListener(statusBarUpdaterWithRepaint);

        statusBarUpdater.updateStatusBar();

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
            final static int CANCEL = -1;
            final static int DONE = 0;
            final static int AGAIN = 1;
            final static int NOTHING = 2;
            private int checkAndSave() {
                if (syncDialog.isVisible())
                    // nothing happened: JOSM was minimized or similar
                    return NOTHING;
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

            @Override
            public void windowDeactivated(WindowEvent e) {
                int result = checkAndSave();
                switch (result) {
                case NOTHING:
                    break;
                case CANCEL:
                {
                    if (yLayer != null) {
                        for (ImageEntry ie : yLayer.data) {
                            ie.tmp = null;
                        }
                        yLayer.updateBufferAndRepaint();
                    }
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

                    yLayer.useThumbs = cbShowThumbs.isSelected();
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

    StatusBarUpdater statusBarUpdater = new StatusBarUpdater(false);
    StatusBarUpdater statusBarUpdaterWithRepaint = new StatusBarUpdater(true);

    private class StatusBarUpdater implements  DocumentListener, ItemListener, ActionListener {
        private boolean doRepaint;

        public StatusBarUpdater(boolean doRepaint) {
            this.doRepaint = doRepaint;
        }

        @Override
        public void insertUpdate(DocumentEvent ev) {
            updateStatusBar();
        }
        @Override
        public void removeUpdate(DocumentEvent ev) {
            updateStatusBar();
        }
        @Override
        public void changedUpdate(DocumentEvent ev) {
        }
        @Override
        public void itemStateChanged(ItemEvent e) {
            updateStatusBar();
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            updateStatusBar();
        }

        public void updateStatusBar() {
            statusBarText.setText(statusText());
            if (doRepaint) {
                yLayer.updateBufferAndRepaint();
            }
        }

        private String statusText() {
            try {
                timezone = parseTimezone(tfTimezone.getText().trim());
                delta = parseOffset(tfOffset.getText().trim());
            } catch (ParseException e) {
                return e.getMessage();
            }

            // The selection of images we are about to correlate may have changed.
            // So reset all images.
            for (ImageEntry ie: yLayer.data) {
                ie.tmp = null;
            }

            // Construct a list of images that have a date, and sort them on the date.
            List<ImageEntry> dateImgLst = getSortedImgList();
            // Create a temporary copy for each image
            for (ImageEntry ie : dateImgLst) {
                ie.cleanTmp();
            }

            GpxDataWrapper selGpx = selectedGPX(false);
            if (selGpx == null)
                return tr("No gpx selected");

            final long offset_ms = ((long) (timezone * 3600) + delta) * 1000; // in milliseconds
            lastNumMatched = matchGpxTrack(dateImgLst, selGpx.data, offset_ms);

            return trn("<html>Matched <b>{0}</b> of <b>{1}</b> photo to GPX track.</html>",
                    "<html>Matched <b>{0}</b> of <b>{1}</b> photos to GPX track.</html>",
                    dateImgLst.size(), lastNumMatched, dateImgLst.size());
        }
    }

    RepaintTheMapListener repaintTheMap = new RepaintTheMapListener();
    private class RepaintTheMapListener implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) { // do nothing
        }

        @Override
        public void focusLost(FocusEvent e) {
            yLayer.updateBufferAndRepaint();
        }
    }

    /**
     * Presents dialog with sliders for manual adjust.
     */
    private class AdjustActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {

            long diff = delta + Math.round(timezone*60*60);

            double diffInH = (double)diff/(60*60);    // hours

            // Find day difference
            final int dayOffset = (int)Math.round(diffInH / 24); // days
            double tmz = diff - dayOffset*24*60*60L;  // seconds

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
            Dictionary<Integer,JLabel> labelTable = new Hashtable<Integer, JLabel>();
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
            class SliderListener implements ChangeListener {
                @Override
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
                        throw new RuntimeException(pe);
                    }
                    delta = sldMinutes.getValue()*60 + sldSeconds.getValue();

                    tfTimezone.getDocument().removeDocumentListener(statusBarUpdater);
                    tfOffset.getDocument().removeDocumentListener(statusBarUpdater);

                    tfTimezone.setText(formatTimezone(timezone));
                    tfOffset.setText(Long.toString(delta + 24*60*60L*dayOffset));    // add the day offset to the offset field

                    tfTimezone.getDocument().addDocumentListener(statusBarUpdater);
                    tfOffset.getDocument().addDocumentListener(statusBarUpdater);

                    lblMatches.setText(statusBarText.getText() + "<br>" + trn("(Time difference of {0} day)", "Time difference of {0} days", Math.abs(dayOffset), Math.abs(dayOffset)));

                    statusBarUpdater.updateStatusBar();
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
            new SliderListener().stateChanged(null);
            // Listeners added here, otherwise it tries to match three times
            // (when setting the default values)
            sldTimezone.addChangeListener(new SliderListener());
            sldMinutes.addChangeListener(new SliderListener());
            sldSeconds.addChangeListener(new SliderListener());

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

        @Override
        public void actionPerformed(ActionEvent arg0) {
            GpxDataWrapper gpxW = selectedGPX(true);
            if (gpxW == null)
                return;
            GpxData gpx = gpxW.data;

            List<ImageEntry> imgs = getSortedImgList();
            PrimaryDateParser dateParser = new PrimaryDateParser();

            // no images found, exit
            if(imgs.size() <= 0) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("The selected photos do not contain time information."),
                        tr("Photos do not contain time information"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Init variables
            long firstExifDate = imgs.get(0).getExifTime().getTime()/1000;

            long firstGPXDate = -1;
            // Finds first GPX point
            outer: for (GpxTrack trk : gpx.tracks) {
                for (GpxTrackSegment segment : trk.getSegments()) {
                    for (WayPoint curWp : segment.getWayPoints()) {
                        String curDateWpStr = (String) curWp.attr.get("time");
                        if (curDateWpStr == null) {
                            continue;
                        }

                        try {
                            firstGPXDate = dateParser.parse(curDateWpStr).getTime()/1000;
                            break outer;
                        } catch(Exception e) {
                            Main.warn(e);
                        }
                    }
                }
            }

            // No GPX timestamps found, exit
            if(firstGPXDate < 0) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("The selected GPX track does not contain timestamps. Please select another one."),
                        tr("GPX Track has no time information"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            // seconds
            long diff = firstExifDate - firstGPXDate;

            double diffInH = (double)diff/(60*60);    // hours

            // Find day difference
            int dayOffset = (int)Math.round(diffInH / 24); // days
            double tz = diff - dayOffset*24*60*60L;  // seconds

            // In hours, rounded to two decimal places
            tz = (double)Math.round(tz*100/(60*60)) / 100;

            // Due to imprecise clocks we might get a "+3:28" timezone, which should obviously be 3:30 with
            // -2 minutes offset. This determines the real timezone and finds offset.
            timezone = (double)Math.round(tz * 2)/2; // hours, rounded to one decimal place
            delta = Math.round(diff - timezone*60*60); // seconds

            tfTimezone.getDocument().removeDocumentListener(statusBarUpdater);
            tfOffset.getDocument().removeDocumentListener(statusBarUpdater);

            tfTimezone.setText(formatTimezone(timezone));
            tfOffset.setText(Long.toString(delta));
            tfOffset.requestFocus();

            tfTimezone.getDocument().addDocumentListener(statusBarUpdater);
            tfOffset.getDocument().addDocumentListener(statusBarUpdater);

            statusBarUpdater.updateStatusBar();
            yLayer.updateBufferAndRepaint();
        }
    }

    private List<ImageEntry> getSortedImgList() {
        return getSortedImgList(cbExifImg.isSelected(), cbTaggedImg.isSelected());
    }

    /**
     * Returns a list of images that fulfill the given criteria.
     * Default setting is to return untagged images, but may be overwritten.
     * @param exif also returns images with exif-gps info
     * @param tagged also returns tagged images
     * @return matching images
     */
    private List<ImageEntry> getSortedImgList(boolean exif, boolean tagged) {
        List<ImageEntry> dateImgLst = new ArrayList<ImageEntry>(yLayer.data.size());
        for (ImageEntry e : yLayer.data) {
            if (!e.hasExifTime()) {
                continue;
            }

            if (e.getExifCoor() != null) {
                if (!exif) {
                    continue;
                }
            }

            if (e.isTagged() && e.getExifCoor() == null) {
                if (!tagged) {
                    continue;
                }
            }

            dateImgLst.add(e);
        }

        Collections.sort(dateImgLst, new Comparator<ImageEntry>() {
            @Override
            public int compare(ImageEntry arg0, ImageEntry arg1) {
                return arg0.getExifTime().compareTo(arg1.getExifTime());
            }
        });

        return dateImgLst;
    }

    private GpxDataWrapper selectedGPX(boolean complain) {
        Object item = cbGpx.getSelectedItem();

        if (item == null || ((GpxDataWrapper) item).file == null) {
            if (complain) {
                JOptionPane.showMessageDialog(Main.parent, tr("You should select a GPX track"),
                        tr("No selected GPX track"), JOptionPane.ERROR_MESSAGE );
            }
            return null;
        }
        return (GpxDataWrapper) item;
    }

    /**
     * Match a list of photos to a gpx track with a given offset.
     * All images need a exifTime attribute and the List must be sorted according to these times.
     */
    private int matchGpxTrack(List<ImageEntry> images, GpxData selectedGpx, long offset) {
        int ret = 0;

        PrimaryDateParser dateParser = new PrimaryDateParser();

        for (GpxTrack trk : selectedGpx.tracks) {
            for (GpxTrackSegment segment : trk.getSegments()) {

                long prevWpTime = 0;
                WayPoint prevWp = null;

                for (WayPoint curWp : segment.getWayPoints()) {

                    String curWpTimeStr = (String) curWp.attr.get("time");
                    if (curWpTimeStr != null) {

                        try {
                            long curWpTime = dateParser.parse(curWpTimeStr).getTime() + offset;
                            ret += matchPoints(images, prevWp, prevWpTime, curWp, curWpTime, offset);

                            prevWp = curWp;
                            prevWpTime = curWpTime;

                        } catch(ParseException e) {
                            Main.error("Error while parsing date \"" + curWpTimeStr + '"');
                            Main.error(e);
                            prevWp = null;
                            prevWpTime = 0;
                        }
                    } else {
                        prevWp = null;
                        prevWpTime = 0;
                    }
                }
            }
        }
        return ret;
    }

    private static Double getElevation(WayPoint wp) {
        String value = (String) wp.attr.get("ele");
        if (value != null) {
            try {
                return new Double(value);
            } catch (NumberFormatException e) {
                Main.warn(e);
            }
        }
        return null;
    }

    private int matchPoints(List<ImageEntry> images, WayPoint prevWp, long prevWpTime,
            WayPoint curWp, long curWpTime, long offset) {
        // Time between the track point and the previous one, 5 sec if first point, i.e. photos take
        // 5 sec before the first track point can be assumed to be take at the starting position
        long interval = prevWpTime > 0 ? ((long)Math.abs(curWpTime - prevWpTime)) : 5*1000;
        int ret = 0;

        // i is the index of the timewise last photo that has the same or earlier EXIF time
        int i = getLastIndexOfListBefore(images, curWpTime);

        // no photos match
        if (i < 0)
            return 0;

        Double speed = null;
        Double prevElevation = null;

        if (prevWp != null) {
            double distance = prevWp.getCoor().greatCircleDistance(curWp.getCoor());
            // This is in km/h, 3.6 * m/s
            if (curWpTime > prevWpTime) {
                speed = 3600 * distance / (curWpTime - prevWpTime);
            }
            prevElevation = getElevation(prevWp);
        }

        Double curElevation = getElevation(curWp);

        // First trackpoint, then interval is set to five seconds, i.e. photos up to five seconds
        // before the first point will be geotagged with the starting point
        if (prevWpTime == 0 || curWpTime <= prevWpTime) {
            while (true) {
                if (i < 0) {
                    break;
                }
                final ImageEntry curImg = images.get(i);
                long time = curImg.getExifTime().getTime();
                if (time > curWpTime || time < curWpTime - interval) {
                    break;
                }
                if (curImg.tmp.getPos() == null) {
                    curImg.tmp.setPos(curWp.getCoor());
                    curImg.tmp.setSpeed(speed);
                    curImg.tmp.setElevation(curElevation);
                    curImg.tmp.setGpsTime(new Date(curImg.getExifTime().getTime() - offset));
                    curImg.flagNewGpsData();
                    ret++;
                }
                i--;
            }
            return ret;
        }

        // This code gives a simple linear interpolation of the coordinates between current and
        // previous track point assuming a constant speed in between
        while (true) {
            if (i < 0) {
                break;
            }
            ImageEntry curImg = images.get(i);
            long imgTime = curImg.getExifTime().getTime();
            if (imgTime < prevWpTime) {
                break;
            }

            if (curImg.tmp.getPos() == null && prevWp != null) {
                // The values of timeDiff are between 0 and 1, it is not seconds but a dimensionless variable
                double timeDiff = (double)(imgTime - prevWpTime) / interval;
                curImg.tmp.setPos(prevWp.getCoor().interpolate(curWp.getCoor(), timeDiff));
                curImg.tmp.setSpeed(speed);
                if (curElevation != null && prevElevation != null) {
                    curImg.tmp.setElevation(prevElevation + (curElevation - prevElevation) * timeDiff);
                }
                curImg.tmp.setGpsTime(new Date(curImg.getExifTime().getTime() - offset));
                curImg.flagNewGpsData();

                ret++;
            }
            i--;
        }
        return ret;
    }

    private int getLastIndexOfListBefore(List<ImageEntry> images, long searchedTime) {
        int lstSize= images.size();

        // No photos or the first photo taken is later than the search period
        if(lstSize == 0 || searchedTime < images.get(0).getExifTime().getTime())
            return -1;

        // The search period is later than the last photo
        if (searchedTime > images.get(lstSize - 1).getExifTime().getTime())
            return lstSize-1;

        // The searched index is somewhere in the middle, do a binary search from the beginning
        int curIndex= 0;
        int startIndex= 0;
        int endIndex= lstSize-1;
        while (endIndex - startIndex > 1) {
            curIndex= (endIndex + startIndex) / 2;
            if (searchedTime > images.get(curIndex).getExifTime().getTime()) {
                startIndex= curIndex;
            } else {
                endIndex= curIndex;
            }
        }
        if (searchedTime < images.get(endIndex).getExifTime().getTime())
            return startIndex;

        // This final loop is to check if photos with the exact same EXIF time follows
        while ((endIndex < (lstSize-1)) && (images.get(endIndex).getExifTime().getTime()
                == images.get(endIndex + 1).getExifTime().getTime())) {
            endIndex++;
        }
        return endIndex;
    }

    private String formatTimezone(double timezone) {
        StringBuilder ret = new StringBuilder();

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
        StringBuilder hTimezone = new StringBuilder();
        StringBuilder mTimezone = new StringBuilder();
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
        } else
            return 0;
    }
}
