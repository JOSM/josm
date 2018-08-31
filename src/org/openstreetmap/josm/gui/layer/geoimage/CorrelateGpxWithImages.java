// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
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
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.actions.DiskAccessAction;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxImageCorrelation;
import org.openstreetmap.josm.data.gpx.GpxTimeOffset;
import org.openstreetmap.josm.data.gpx.GpxTimezone;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxTrackSegment;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter;
import org.openstreetmap.josm.gui.io.importexport.JpgImporter;
import org.openstreetmap.josm.gui.io.importexport.NMEAImporter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.gui.widgets.FileChooserManager;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.IGpxReader;
import org.openstreetmap.josm.io.nmea.NmeaReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.SAXException;

/**
 * This class displays the window to select the GPX file and the offset (timezone + delta).
 * Then it correlates the images of the layer with that GPX file.
 */
public class CorrelateGpxWithImages extends AbstractAction {

    private static final List<GpxData> loadedGpxData = new ArrayList<>();

    private final transient GeoImageLayer yLayer;
    private transient GpxTimezone timezone;
    private transient GpxTimeOffset delta;
    private static boolean forceTags;

    /**
     * Constructs a new {@code CorrelateGpxWithImages} action.
     * @param layer The image layer
     */
    public CorrelateGpxWithImages(GeoImageLayer layer) {
        super(tr("Correlate to GPX"));
        new ImageProvider("dialogs/geoimage/gpx2img").getResource().attachImageIcon(this, true);
        this.yLayer = layer;
        MainApplication.getLayerManager().addLayerChangeListener(new GpxLayerAddedListener());
    }

    private final class SyncDialogWindowListener extends WindowAdapter {
        private static final int CANCEL = -1;
        private static final int DONE = 0;
        private static final int AGAIN = 1;
        private static final int NOTHING = 2;

        private int checkAndSave() {
            if (syncDialog.isVisible())
                // nothing happened: JOSM was minimized or similar
                return NOTHING;
            int answer = syncDialog.getValue();
            if (answer != 1)
                return CANCEL;

            // Parse values again, to display an error if the format is not recognized
            try {
                timezone = GpxTimezone.parseTimezone(tfTimezone.getText().trim());
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), e.getMessage(),
                        tr("Invalid timezone"), JOptionPane.ERROR_MESSAGE);
                return AGAIN;
            }

            try {
                delta = GpxTimeOffset.parseOffset(tfOffset.getText().trim());
            } catch (ParseException e) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), e.getMessage(),
                        tr("Invalid offset"), JOptionPane.ERROR_MESSAGE);
                return AGAIN;
            }

            if (lastNumMatched == 0 && new ExtendedDialog(
                        MainApplication.getMainFrame(),
                        tr("Correlate images with GPX track"),
                        tr("OK"), tr("Try Again")).
                        setContent(tr("No images could be matched!")).
                        setButtonIcons("ok", "dialogs/refresh").
                        showDialog().getValue() == 2)
                return AGAIN;
            return DONE;
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            int result = checkAndSave();
            switch (result) {
            case NOTHING:
                break;
            case CANCEL:
                if (yLayer != null) {
                    if (yLayer.data != null) {
                        for (ImageEntry ie : yLayer.data) {
                            ie.discardTmp();
                        }
                    }
                    yLayer.updateBufferAndRepaint();
                }
                break;
            case AGAIN:
                actionPerformed(null);
                break;
            case DONE:
                Config.getPref().put("geoimage.timezone", timezone.formatTimezone());
                Config.getPref().put("geoimage.delta", delta.formatOffset());
                Config.getPref().putBoolean("geoimage.showThumbs", yLayer.useThumbs);

                yLayer.useThumbs = cbShowThumbs.isSelected();
                yLayer.startLoadThumbs();

                // Search whether an other layer has yet defined some bounding box.
                // If none, we'll zoom to the bounding box of the layer with the photos.
                boolean boundingBoxedLayerFound = false;
                for (Layer l: MainApplication.getLayerManager().getLayers()) {
                    if (l != yLayer) {
                        BoundingXYVisitor bbox = new BoundingXYVisitor();
                        l.visitBoundingBox(bbox);
                        if (bbox.getBounds() != null) {
                            boundingBoxedLayerFound = true;
                            break;
                        }
                    }
                }
                if (!boundingBoxedLayerFound) {
                    BoundingXYVisitor bbox = new BoundingXYVisitor();
                    yLayer.visitBoundingBox(bbox);
                    MainApplication.getMap().mapView.zoomTo(bbox);
                }

                if (yLayer.data != null) {
                    for (ImageEntry ie : yLayer.data) {
                        ie.applyTmp();
                    }
                }

                yLayer.updateBufferAndRepaint();

                break;
            default:
                throw new IllegalStateException();
            }
        }
    }

    private static class GpxDataWrapper {
        private final String name;
        private final GpxData data;
        private final File file;

        GpxDataWrapper(String name, GpxData data, File file) {
            this.name = name;
            this.data = data;
            this.file = file;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private ExtendedDialog syncDialog;
    private final transient List<GpxDataWrapper> gpxLst = new ArrayList<>();
    private JPanel outerPanel;
    private JosmComboBox<GpxDataWrapper> cbGpx;
    private JosmTextField tfTimezone;
    private JosmTextField tfOffset;
    private JCheckBox cbExifImg;
    private JCheckBox cbTaggedImg;
    private JCheckBox cbShowThumbs;
    private JLabel statusBarText;

    // remember the last number of matched photos
    private int lastNumMatched;

    /** This class is called when the user doesn't find the GPX file he needs in the files that have
     * been loaded yet. It displays a FileChooser dialog to select the GPX file to be loaded.
     */
    private class LoadGpxDataActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            ExtensionFileFilter gpxFilter = GpxImporter.getFileFilter();
            AbstractFileChooser fc = new FileChooserManager(true, null).createFileChooser(false, null,
                    Arrays.asList(gpxFilter, NMEAImporter.FILE_FILTER), gpxFilter, JFileChooser.FILES_ONLY).openFileChooser();
            if (fc == null)
                return;
            File sel = fc.getSelectedFile();

            try {
                outerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                for (int i = gpxLst.size() - 1; i >= 0; i--) {
                    GpxDataWrapper wrapper = gpxLst.get(i);
                    if (sel.equals(wrapper.file)) {
                        cbGpx.setSelectedIndex(i);
                        if (!sel.getName().equals(wrapper.name)) {
                            JOptionPane.showMessageDialog(
                                    MainApplication.getMainFrame(),
                                    tr("File {0} is loaded yet under the name \"{1}\"", sel.getName(), wrapper.name),
                                    tr("Error"),
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                        return;
                    }
                }
                GpxData data = null;
                try (InputStream iStream = Compression.getUncompressedFileInputStream(sel)) {
                    IGpxReader reader = gpxFilter.accept(sel) ? new GpxReader(iStream) : new NmeaReader(iStream);
                    reader.parse(false);
                    data = reader.getGpxData();
                    data.storageFile = sel;

                } catch (SAXException ex) {
                    Logging.error(ex);
                    JOptionPane.showMessageDialog(
                            MainApplication.getMainFrame(),
                            tr("Error while parsing {0}", sel.getName())+": "+ex.getMessage(),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                } catch (IOException ex) {
                    Logging.error(ex);
                    JOptionPane.showMessageDialog(
                            MainApplication.getMainFrame(),
                            tr("Could not read \"{0}\"", sel.getName())+'\n'+ex.getMessage(),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                MutableComboBoxModel<GpxDataWrapper> model = (MutableComboBoxModel<GpxDataWrapper>) cbGpx.getModel();
                loadedGpxData.add(data);
                if (gpxLst.get(0).file == null) {
                    gpxLst.remove(0);
                    model.removeElementAt(0);
                }
                GpxDataWrapper elem = new GpxDataWrapper(sel.getName(), data, sel);
                gpxLst.add(elem);
                model.addElement(elem);
                cbGpx.setSelectedIndex(cbGpx.getItemCount() - 1);
            } finally {
                outerPanel.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    private class AdvancedSettingsActionListener implements ActionListener {

        private class CheckBoxActionListener implements ActionListener {
            private final JComponent[] comps;

            CheckBoxActionListener(JComponent... c) {
                comps = Objects.requireNonNull(c);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                setEnabled((JCheckBox) e.getSource());
            }

            public void setEnabled(JCheckBox cb) {
                for (JComponent comp : comps) {
                    if (comp instanceof JSpinner) {
                        comp.setEnabled(cb.isSelected());
                    } else if (comp instanceof JPanel) {
                        boolean en = cb.isSelected();
                        for (Component c : comp.getComponents()) {
                            if (c instanceof JSpinner) {
                                c.setEnabled(en);
                            } else {
                                c.setEnabled(cb.isSelected());
                                if (en && c instanceof JCheckBox) {
                                    en = ((JCheckBox) c).isSelected();
                                }
                            }
                        }
                    }
                }
            }
        }

        private void addCheckBoxActionListener(JCheckBox cb, JComponent... c) {
            CheckBoxActionListener listener = new CheckBoxActionListener(c);
            cb.addActionListener(listener);
            listener.setEnabled(cb);
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            IPreferences s = Config.getPref();
            JPanel p = new JPanel(new GridBagLayout());

            Border border1 = BorderFactory.createEmptyBorder(0, 20, 0, 0);
            Border border2 = BorderFactory.createEmptyBorder(10, 0, 5, 0);
            Border border = BorderFactory.createEmptyBorder(0, 40, 0, 0);
            FlowLayout layout = new FlowLayout();

            JLabel l = new JLabel(tr("Segment settings"));
            l.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            p.add(l, GBC.eol());
            JCheckBox cInterpolSeg = new JCheckBox(tr("Interpolate between segments"), s.getBoolean("geoimage.seg.int", true));
            cInterpolSeg.setBorder(border1);
            p.add(cInterpolSeg, GBC.eol());

            JCheckBox cInterpolSegTime = new JCheckBox(tr("only when the segments are less than # minutes apart:"),
                    s.getBoolean("geoimage.seg.int.time", true));
            JSpinner sInterpolSegTime = new JSpinner(
                    new SpinnerNumberModel(s.getInt("geoimage.seg.int.time.val", 60), 0, Integer.MAX_VALUE, 1));
            ((JSpinner.DefaultEditor) sInterpolSegTime.getEditor()).getTextField().setColumns(3);
            JPanel pInterpolSegTime = new JPanel(layout);
            pInterpolSegTime.add(cInterpolSegTime);
            pInterpolSegTime.add(sInterpolSegTime);
            pInterpolSegTime.setBorder(border);
            p.add(pInterpolSegTime, GBC.eol());

            JCheckBox cInterpolSegDist = new JCheckBox(tr("only when the segments are less than # meters apart:"),
                    s.getBoolean("geoimage.seg.int.dist", true));
            JSpinner sInterpolSegDist = new JSpinner(
                    new SpinnerNumberModel(s.getInt("geoimage.seg.int.dist.val", 50), 0, Integer.MAX_VALUE, 1));
            ((JSpinner.DefaultEditor) sInterpolSegDist.getEditor()).getTextField().setColumns(3);
            JPanel pInterpolSegDist = new JPanel(layout);
            pInterpolSegDist.add(cInterpolSegDist);
            pInterpolSegDist.add(sInterpolSegDist);
            pInterpolSegDist.setBorder(border);
            p.add(pInterpolSegDist, GBC.eol());

            JCheckBox cTagSeg = new JCheckBox(tr("Tag images at the closest end of a segment, when not interpolated"),
                    s.getBoolean("geoimage.seg.tag", true));
            cTagSeg.setBorder(border1);
            p.add(cTagSeg, GBC.eol());

            JCheckBox cTagSegTime = new JCheckBox(tr("only within # minutes of the closest trackpoint:"),
                    s.getBoolean("geoimage.seg.tag.time", true));
            JSpinner sTagSegTime = new JSpinner(
                    new SpinnerNumberModel(s.getInt("geoimage.seg.tag.time.val", 2), 0, Integer.MAX_VALUE, 1));
            ((JSpinner.DefaultEditor) sTagSegTime.getEditor()).getTextField().setColumns(3);
            JPanel pTagSegTime = new JPanel(layout);
            pTagSegTime.add(cTagSegTime);
            pTagSegTime.add(sTagSegTime);
            pTagSegTime.setBorder(border);
            p.add(pTagSegTime, GBC.eol());

            l = new JLabel(tr("Track settings (note that multiple tracks can be in one GPX file)"));
            l.setBorder(border2);
            p.add(l, GBC.eol());
            JCheckBox cInterpolTrack = new JCheckBox(tr("Interpolate between tracks"), s.getBoolean("geoimage.trk.int", false));
            cInterpolTrack.setBorder(border1);
            p.add(cInterpolTrack, GBC.eol());

            JCheckBox cInterpolTrackTime = new JCheckBox(tr("only when the tracks are less than # minutes apart:"),
                    s.getBoolean("geoimage.trk.int.time", false));
            JSpinner sInterpolTrackTime = new JSpinner(
                    new SpinnerNumberModel(s.getInt("geoimage.trk.int.time.val", 60), 0, Integer.MAX_VALUE, 1));
            ((JSpinner.DefaultEditor) sInterpolTrackTime.getEditor()).getTextField().setColumns(3);
            JPanel pInterpolTrackTime = new JPanel(layout);
            pInterpolTrackTime.add(cInterpolTrackTime);
            pInterpolTrackTime.add(sInterpolTrackTime);
            pInterpolTrackTime.setBorder(border);
            p.add(pInterpolTrackTime, GBC.eol());

            JCheckBox cInterpolTrackDist = new JCheckBox(tr("only when the tracks are less than # meters apart:"),
                    s.getBoolean("geoimage.trk.int.dist", false));
            JSpinner sInterpolTrackDist = new JSpinner(
                    new SpinnerNumberModel(s.getInt("geoimage.trk.int.dist.val", 50), 0, Integer.MAX_VALUE, 1));
            ((JSpinner.DefaultEditor) sInterpolTrackDist.getEditor()).getTextField().setColumns(3);
            JPanel pInterpolTrackDist = new JPanel(layout);
            pInterpolTrackDist.add(cInterpolTrackDist);
            pInterpolTrackDist.add(sInterpolTrackDist);
            pInterpolTrackDist.setBorder(border);
            p.add(pInterpolTrackDist, GBC.eol());

            JCheckBox cTagTrack = new JCheckBox("<html>" +
                    tr("Tag images at the closest end of a track, when not interpolated<br>" +
                    "(also applies before the first and after the last track)") + "</html>",
                    s.getBoolean("geoimage.trk.tag", true));
            cTagTrack.setBorder(border1);
            p.add(cTagTrack, GBC.eol());

            JCheckBox cTagTrackTime = new JCheckBox(tr("only within # minutes of the closest trackpoint:"),
                    s.getBoolean("geoimage.trk.tag.time", true));
            JSpinner sTagTrackTime = new JSpinner(
                    new SpinnerNumberModel(s.getInt("geoimage.trk.tag.time.val", 2), 0, Integer.MAX_VALUE, 1));
            ((JSpinner.DefaultEditor) sTagTrackTime.getEditor()).getTextField().setColumns(3);
            JPanel pTagTrackTime = new JPanel(layout);
            pTagTrackTime.add(cTagTrackTime);
            pTagTrackTime.add(sTagTrackTime);
            pTagTrackTime.setBorder(border);
            p.add(pTagTrackTime, GBC.eol());

            l = new JLabel(tr("Advanced"));
            l.setBorder(border2);
            p.add(l, GBC.eol());
            JCheckBox cForce = new JCheckBox("<html>" +
                    tr("Force tagging of all pictures (temporarily overrides the settings above).") + "<br>" +
                    tr("This option will not be saved permanently.") + "</html>", forceTags);
            cForce.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 0));
            p.add(cForce, GBC.eol());

            addCheckBoxActionListener(cInterpolSegTime, sInterpolSegTime);
            addCheckBoxActionListener(cInterpolSegDist, sInterpolSegDist);
            addCheckBoxActionListener(cInterpolSeg, pInterpolSegTime, pInterpolSegDist);

            addCheckBoxActionListener(cTagSegTime, sTagSegTime);
            addCheckBoxActionListener(cTagSeg, pTagSegTime);

            addCheckBoxActionListener(cInterpolTrackTime, sInterpolTrackTime);
            addCheckBoxActionListener(cInterpolTrackDist, sInterpolTrackDist);
            addCheckBoxActionListener(cInterpolTrack, pInterpolTrackTime, pInterpolTrackDist);

            addCheckBoxActionListener(cTagTrackTime, sTagTrackTime);
            addCheckBoxActionListener(cTagTrack, pTagTrackTime);


            ExtendedDialog ed = new ExtendedDialog(MainApplication.getMainFrame(), tr("Advanced settings"), tr("OK"), tr("Cancel"))
                            .setButtonIcons("ok", "cancel").setContent(p);
            if (ed.showDialog().getValue() == 1) {

                s.putBoolean("geoimage.seg.int", cInterpolSeg.isSelected());
                s.putBoolean("geoimage.seg.int.dist", cInterpolSegDist.isSelected());
                s.putInt("geoimage.seg.int.dist.val", (int) sInterpolSegDist.getValue());
                s.putBoolean("geoimage.seg.int.time", cInterpolSegTime.isSelected());
                s.putInt("geoimage.seg.int.time.val", (int) sInterpolSegTime.getValue());
                s.putBoolean("geoimage.seg.tag", cTagSeg.isSelected());
                s.putBoolean("geoimage.seg.tag.time", cTagSegTime.isSelected());
                s.putInt("geoimage.seg.tag.time.val", (int) sTagSegTime.getValue());

                s.putBoolean("geoimage.trk.int", cInterpolTrack.isSelected());
                s.putBoolean("geoimage.trk.int.dist", cInterpolTrackDist.isSelected());
                s.putInt("geoimage.trk.int.dist.val", (int) sInterpolTrackDist.getValue());
                s.putBoolean("geoimage.trk.int.time", cInterpolTrackTime.isSelected());
                s.putInt("geoimage.trk.int.time.val", (int) sInterpolTrackTime.getValue());
                s.putBoolean("geoimage.trk.tag", cTagTrack.isSelected());
                s.putBoolean("geoimage.trk.tag.time", cTagTrackTime.isSelected());
                s.putInt("geoimage.trk.tag.time.val", (int) sTagTrackTime.getValue());

                forceTags = cForce.isSelected(); // This setting is not supposed to be saved permanently

                statusBarUpdater.updateStatusBar();
                yLayer.updateBufferAndRepaint();
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

        @Override
        public void actionPerformed(ActionEvent arg0) {
            SimpleDateFormat dateFormat = (SimpleDateFormat) DateUtils.getDateTimeFormat(DateFormat.SHORT, DateFormat.MEDIUM);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel(tr("<html>Take a photo of your GPS receiver while it displays the time.<br>"
                    + "Display that photo here.<br>"
                    + "And then, simply capture the time you read on the photo and select a timezone<hr></html>")),
                    BorderLayout.NORTH);

            ImageDisplay imgDisp = new ImageDisplay();
            imgDisp.setPreferredSize(new Dimension(300, 225));
            panel.add(imgDisp, BorderLayout.CENTER);

            JPanel panelTf = new JPanel(new GridBagLayout());

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = gc.gridy = 0;
            gc.gridwidth = gc.gridheight = 1;
            gc.weightx = gc.weighty = 0.0;
            gc.fill = GridBagConstraints.NONE;
            gc.anchor = GridBagConstraints.WEST;
            panelTf.add(new JLabel(tr("Photo time (from exif):")), gc);

            JLabel lbExifTime = new JLabel();
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

            JosmTextField tfGpsTime = new JosmTextField(12);
            tfGpsTime.setEnabled(false);
            tfGpsTime.setMinimumSize(new Dimension(155, tfGpsTime.getMinimumSize().height));
            gc.gridx = 1;
            gc.weightx = 1.0;
            gc.fill = GridBagConstraints.HORIZONTAL;
            panelTf.add(tfGpsTime, gc);

            gc.gridx = 2;
            gc.weightx = 0.2;
            panelTf.add(new JLabel(" ["+dateFormat.toLocalizedPattern()+']'), gc);

            gc.gridx = 0;
            gc.gridy = 2;
            gc.gridwidth = gc.gridheight = 1;
            gc.weightx = gc.weighty = 0.0;
            gc.fill = GridBagConstraints.NONE;
            gc.anchor = GridBagConstraints.WEST;
            panelTf.add(new JLabel(tr("I am in the timezone of: ")), gc);

            String[] tmp = TimeZone.getAvailableIDs();
            List<String> vtTimezones = new ArrayList<>(tmp.length);

            for (String tzStr : tmp) {
                TimeZone tz = TimeZone.getTimeZone(tzStr);

                String tzDesc = tzStr + " (" +
                        new GpxTimezone(((double) tz.getRawOffset()) / TimeUnit.HOURS.toMillis(1)).formatTimezone() +
                        ')';
                vtTimezones.add(tzDesc);
            }

            Collections.sort(vtTimezones);

            JosmComboBox<String> cbTimezones = new JosmComboBox<>(vtTimezones.toArray(new String[0]));

            String tzId = Config.getPref().get("geoimage.timezoneid", "");
            TimeZone defaultTz;
            if (tzId.isEmpty()) {
                defaultTz = TimeZone.getDefault();
            } else {
                defaultTz = TimeZone.getTimeZone(tzId);
            }

            cbTimezones.setSelectedItem(defaultTz.getID() + " (" +
                    new GpxTimezone(((double) defaultTz.getRawOffset()) / TimeUnit.HOURS.toMillis(1)).formatTimezone() +
                    ')');

            gc.gridx = 1;
            gc.weightx = 1.0;
            gc.gridwidth = 2;
            gc.fill = GridBagConstraints.HORIZONTAL;
            panelTf.add(cbTimezones, gc);

            panel.add(panelTf, BorderLayout.SOUTH);

            JPanel panelLst = new JPanel(new BorderLayout());

            JList<String> imgList = new JList<>(new AbstractListModel<String>() {
                @Override
                public String getElementAt(int i) {
                    return yLayer.data.get(i).getFile().getName();
                }

                @Override
                public int getSize() {
                    return yLayer.data != null ? yLayer.data.size() : 0;
                }
            });
            imgList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            imgList.getSelectionModel().addListSelectionListener(evt -> {
                int index = imgList.getSelectedIndex();
                imgDisp.setImage(yLayer.data.get(index));
                Date date = yLayer.data.get(index).getExifTime();
                if (date != null) {
                    DateFormat df = DateUtils.getDateTimeFormat(DateFormat.SHORT, DateFormat.MEDIUM);
                    lbExifTime.setText(df.format(date));
                    tfGpsTime.setText(df.format(date));
                    tfGpsTime.setCaretPosition(tfGpsTime.getText().length());
                    tfGpsTime.setEnabled(true);
                    tfGpsTime.requestFocus();
                } else {
                    lbExifTime.setText(tr("No date"));
                    tfGpsTime.setText("");
                    tfGpsTime.setEnabled(false);
                }
            });
            panelLst.add(new JScrollPane(imgList), BorderLayout.CENTER);

            JButton openButton = new JButton(tr("Open another photo"));
            openButton.addActionListener(ae -> {
                AbstractFileChooser fc = DiskAccessAction.createAndOpenFileChooser(true, false, null,
                        JpgImporter.FILE_FILTER_WITH_FOLDERS, JFileChooser.FILES_ONLY, "geoimage.lastdirectory");
                if (fc == null)
                    return;
                ImageEntry entry = new ImageEntry(fc.getSelectedFile());
                entry.extractExif();
                imgDisp.setImage(entry);

                Date date = entry.getExifTime();
                if (date != null) {
                    lbExifTime.setText(DateUtils.getDateTimeFormat(DateFormat.SHORT, DateFormat.MEDIUM).format(date));
                    tfGpsTime.setText(DateUtils.getDateFormat(DateFormat.SHORT).format(date)+' ');
                    tfGpsTime.setEnabled(true);
                } else {
                    lbExifTime.setText(tr("No date"));
                    tfGpsTime.setText("");
                    tfGpsTime.setEnabled(false);
                }
            });
            panelLst.add(openButton, BorderLayout.PAGE_END);

            panel.add(panelLst, BorderLayout.LINE_START);

            boolean isOk = false;
            while (!isOk) {
                int answer = JOptionPane.showConfirmDialog(
                        MainApplication.getMainFrame(), panel,
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
                } catch (ParseException e) {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Error while parsing the date.\n"
                            + "Please use the requested format"),
                            tr("Invalid date"), JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                String selectedTz = (String) cbTimezones.getSelectedItem();
                int pos = selectedTz.lastIndexOf('(');
                tzId = selectedTz.substring(0, pos - 1);
                String tzValue = selectedTz.substring(pos + 1, selectedTz.length() - 1);

                Config.getPref().put("geoimage.timezoneid", tzId);
                tfOffset.setText(GpxTimeOffset.milliseconds(delta).formatOffset());
                tfTimezone.setText(tzValue);

                isOk = true;

            }
            statusBarUpdater.updateStatusBar();
            yLayer.updateBufferAndRepaint();
        }
    }

    private class GpxLayerAddedListener implements LayerChangeListener {
        @Override
        public void layerAdded(LayerAddEvent e) {
            if (syncDialog != null && syncDialog.isVisible()) {
                Layer layer = e.getAddedLayer();
                if (layer instanceof GpxLayer) {
                    GpxLayer gpx = (GpxLayer) layer;
                    GpxDataWrapper gdw = new GpxDataWrapper(gpx.getName(), gpx.data, gpx.data.storageFile);
                    gpxLst.add(gdw);
                    MutableComboBoxModel<GpxDataWrapper> model = (MutableComboBoxModel<GpxDataWrapper>) cbGpx.getModel();
                    if (gpxLst.get(0).file == null) {
                        gpxLst.remove(0);
                        model.removeElementAt(0);
                    }
                    model.addElement(gdw);
                }
            }
        }

        @Override
        public void layerRemoving(LayerRemoveEvent e) {
            // Not used
        }

        @Override
        public void layerOrderChanged(LayerOrderChangeEvent e) {
            // Not used
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        // Construct the list of loaded GPX tracks
        Collection<Layer> layerLst = MainApplication.getLayerManager().getLayers();
        gpxLst.clear();
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

        cbGpx = new JosmComboBox<>(gpxLst.toArray(new GpxDataWrapper[0]));
        if (defaultItem != null) {
            cbGpx.setSelectedItem(defaultItem);
        } else {
            // select first GPX track associated to a file
            for (GpxDataWrapper item : gpxLst) {
                if (item.file != null) {
                    cbGpx.setSelectedItem(item);
                    break;
                }
            }
        }
        cbGpx.addActionListener(statusBarUpdaterWithRepaint);
        panelCb.add(cbGpx);

        JButton buttonOpen = new JButton(tr("Open another GPX trace"));
        buttonOpen.addActionListener(new LoadGpxDataActionListener());
        panelCb.add(buttonOpen);

        JPanel panelTf = new JPanel(new GridBagLayout());

        try {
            timezone = GpxTimezone.parseTimezone(Optional.ofNullable(Config.getPref().get("geoimage.timezone", "0:00")).orElse("0:00"));
        } catch (ParseException e) {
            timezone = GpxTimezone.ZERO;
            Logging.trace(e);
        }

        tfTimezone = new JosmTextField(10);
        tfTimezone.setText(timezone.formatTimezone());

        try {
            delta = GpxTimeOffset.parseOffset(Config.getPref().get("geoimage.delta", "0"));
        } catch (ParseException e) {
            delta = GpxTimeOffset.ZERO;
            Logging.trace(e);
        }

        tfOffset = new JosmTextField(10);
        tfOffset.setText(delta.formatOffset());

        JButton buttonViewGpsPhoto = new JButton(tr("<html>Use photo of an accurate clock,<br>"
                + "e.g. GPS receiver display</html>"));
        buttonViewGpsPhoto.setIcon(ImageProvider.get("clock"));
        buttonViewGpsPhoto.addActionListener(new SetOffsetActionListener());

        JButton buttonAutoGuess = new JButton(tr("Auto-Guess"));
        buttonAutoGuess.setToolTipText(tr("Matches first photo with first gpx point"));
        buttonAutoGuess.addActionListener(new AutoGuessActionListener());

        JButton buttonAdjust = new JButton(tr("Manual adjust"));
        buttonAdjust.addActionListener(new AdjustActionListener());

        JButton buttonAdvanced = new JButton(tr("Advanced settings..."));
        buttonAdvanced.addActionListener(new AdvancedSettingsActionListener());

        JLabel labelPosition = new JLabel(tr("Override position for: "));

        int numAll = getSortedImgList(true, true).size();
        int numExif = numAll - getSortedImgList(false, true).size();
        int numTagged = numAll - getSortedImgList(true, false).size();

        cbExifImg = new JCheckBox(tr("Images with geo location in exif data ({0}/{1})", numExif, numAll));
        cbExifImg.setEnabled(numExif != 0);

        cbTaggedImg = new JCheckBox(tr("Images that are already tagged ({0}/{1})", numTagged, numAll), true);
        cbTaggedImg.setEnabled(numTagged != 0);

        labelPosition.setEnabled(cbExifImg.isEnabled() || cbTaggedImg.isEnabled());

        boolean ticked = yLayer.thumbsLoaded || Config.getPref().getBoolean("geoimage.showThumbs", false);
        cbShowThumbs = new JCheckBox(tr("Show Thumbnail images on the map"), ticked);
        cbShowThumbs.setEnabled(!yLayer.thumbsLoaded);

        int y = 0;
        GBC gbc = GBC.eol();
        gbc.gridx = 0;
        gbc.gridy = y++;
        panelTf.add(panelCb, gbc);

        gbc = GBC.eol().fill(GBC.HORIZONTAL).insets(0, 0, 0, 12);
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

        gbc = GBC.std().insets(5, 5, 5, 5);
        gbc.gridx = 2;
        gbc.gridy = y-2;
        gbc.gridheight = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.5;
        panelTf.add(buttonViewGpsPhoto, gbc);

        gbc = GBC.std().fill(GBC.BOTH).insets(5, 5, 5, 5);
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.weightx = 0.5;
        panelTf.add(buttonAdvanced, gbc);

        gbc.gridx = 2;
        panelTf.add(buttonAutoGuess, gbc);

        gbc.gridx = 3;
        panelTf.add(buttonAdjust, gbc);

        gbc = GBC.eol().fill(GBC.HORIZONTAL).insets(0, 12, 0, 0);
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
        gbc.gridy = y;
        panelTf.add(cbShowThumbs, gbc);

        final JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
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
        yLayer.updateBufferAndRepaint();

        outerPanel = new JPanel(new BorderLayout());
        outerPanel.add(statusBar, BorderLayout.PAGE_END);

        if (!GraphicsEnvironment.isHeadless()) {
            syncDialog = new ExtendedDialog(
                    MainApplication.getMainFrame(),
                    tr("Correlate images with GPX track"),
                    new String[] {tr("Correlate"), tr("Cancel")},
                    false
            );
            syncDialog.setContent(panelTf, false);
            syncDialog.setButtonIcons("ok", "cancel");
            syncDialog.setupDialog();
            outerPanel.add(syncDialog.getContentPane(), BorderLayout.PAGE_START);
            syncDialog.setContentPane(outerPanel);
            syncDialog.pack();
            syncDialog.addWindowListener(new SyncDialogWindowListener());
            syncDialog.showDialog();
        }
    }

    private final transient StatusBarUpdater statusBarUpdater = new StatusBarUpdater(false);
    private final transient StatusBarUpdater statusBarUpdaterWithRepaint = new StatusBarUpdater(true);

    private class StatusBarUpdater implements DocumentListener, ItemListener, ActionListener {
        private final boolean doRepaint;

        StatusBarUpdater(boolean doRepaint) {
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
            // Do nothing
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
                timezone = GpxTimezone.parseTimezone(tfTimezone.getText().trim());
                delta = GpxTimeOffset.parseOffset(tfOffset.getText().trim());
            } catch (ParseException e) {
                return e.getMessage();
            }

            // The selection of images we are about to correlate may have changed.
            // So reset all images.
            if (yLayer.data != null) {
                for (ImageEntry ie: yLayer.data) {
                    ie.discardTmp();
                }
            }

            // Construct a list of images that have a date, and sort them on the date.
            List<ImageEntry> dateImgLst = getSortedImgList();
            // Create a temporary copy for each image
            for (ImageEntry ie : dateImgLst) {
                ie.createTmp();
                ie.getTmp().setPos(null);
            }

            GpxDataWrapper selGpx = selectedGPX(false);
            if (selGpx == null)
                return tr("No gpx selected");

            final long offsetMs = ((long) (timezone.getHours() * TimeUnit.HOURS.toMillis(-1))) + delta.getMilliseconds(); // in milliseconds
            lastNumMatched = GpxImageCorrelation.matchGpxTrack(dateImgLst, selGpx.data, offsetMs, forceTags);

            return trn("<html>Matched <b>{0}</b> of <b>{1}</b> photo to GPX track.</html>",
                    "<html>Matched <b>{0}</b> of <b>{1}</b> photos to GPX track.</html>",
                    dateImgLst.size(), lastNumMatched, dateImgLst.size());
        }
    }

    private final transient RepaintTheMapListener repaintTheMap = new RepaintTheMapListener();

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

            final GpxTimeOffset offset = GpxTimeOffset.milliseconds(
                    delta.getMilliseconds() + Math.round(timezone.getHours() * TimeUnit.HOURS.toMillis(1)));
            final int dayOffset = offset.getDayOffset();
            final Pair<GpxTimezone, GpxTimeOffset> timezoneOffsetPair = offset.withoutDayOffset().splitOutTimezone();

            // Info Labels
            final JLabel lblMatches = new JLabel();

            // Timezone Slider
            // The slider allows to switch timezon from -12:00 to 12:00 in 30 minutes steps. Therefore the range is -24 to 24.
            final JLabel lblTimezone = new JLabel();
            final JSlider sldTimezone = new JSlider(-24, 24, 0);
            sldTimezone.setPaintLabels(true);
            Dictionary<Integer, JLabel> labelTable = new Hashtable<>();
            // CHECKSTYLE.OFF: ParenPad
            for (int i = -12; i <= 12; i += 6) {
                labelTable.put(i * 2, new JLabel(new GpxTimezone(i).formatTimezone()));
            }
            // CHECKSTYLE.ON: ParenPad
            sldTimezone.setLabelTable(labelTable);

            // Minutes Slider
            final JLabel lblMinutes = new JLabel();
            final JSlider sldMinutes = new JSlider(-15, 15, 0);
            sldMinutes.setPaintLabels(true);
            sldMinutes.setMajorTickSpacing(5);

            // Seconds slider
            final JLabel lblSeconds = new JLabel();
            final JSlider sldSeconds = new JSlider(-600, 600, 0);
            sldSeconds.setPaintLabels(true);
            labelTable = new Hashtable<>();
            // CHECKSTYLE.OFF: ParenPad
            for (int i = -60; i <= 60; i += 30) {
                labelTable.put(i * 10, new JLabel(GpxTimeOffset.seconds(i).formatOffset()));
            }
            // CHECKSTYLE.ON: ParenPad
            sldSeconds.setLabelTable(labelTable);
            sldSeconds.setMajorTickSpacing(300);

            // This is called whenever one of the sliders is moved.
            // It updates the labels and also calls the "match photos" code
            class SliderListener implements ChangeListener {
                @Override
                public void stateChanged(ChangeEvent e) {
                    timezone = new GpxTimezone(sldTimezone.getValue() / 2.);

                    lblTimezone.setText(tr("Timezone: {0}", timezone.formatTimezone()));
                    lblMinutes.setText(tr("Minutes: {0}", sldMinutes.getValue()));
                    lblSeconds.setText(tr("Seconds: {0}", GpxTimeOffset.milliseconds(100L * sldSeconds.getValue()).formatOffset()));

                    delta = GpxTimeOffset.milliseconds(100L * sldSeconds.getValue()
                            + TimeUnit.MINUTES.toMillis(sldMinutes.getValue())
                            + TimeUnit.DAYS.toMillis(dayOffset));

                    tfTimezone.getDocument().removeDocumentListener(statusBarUpdater);
                    tfOffset.getDocument().removeDocumentListener(statusBarUpdater);

                    tfTimezone.setText(timezone.formatTimezone());
                    tfOffset.setText(delta.formatOffset());

                    tfTimezone.getDocument().addDocumentListener(statusBarUpdater);
                    tfOffset.getDocument().addDocumentListener(statusBarUpdater);

                    lblMatches.setText(statusBarText.getText() + "<br>" + trn("(Time difference of {0} day)",
                            "Time difference of {0} days", Math.abs(dayOffset), Math.abs(dayOffset)));

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
                sldTimezone.setValue((int) (timezoneOffsetPair.a.getHours() * 2));
                sldMinutes.setValue((int) (timezoneOffsetPair.b.getSeconds() / 60));
                final long deciSeconds = timezoneOffsetPair.b.getMilliseconds() / 100;
                sldSeconds.setValue((int) (deciSeconds % 60));
            } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
                Logging.warn(e);
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
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
            new ExtendedDialog(MainApplication.getMainFrame(),
                    tr("Adjust timezone and offset"),
                    tr("Close")).
                    setContent(p).setButtonIcons("ok").showDialog();
        }
    }

    static class NoGpxTimestamps extends Exception {
    }

    /**
     * Tries to auto-guess the timezone and offset.
     *
     * @param imgs the images to correlate
     * @param gpx the gpx track to correlate to
     * @return a pair of timezone and offset
     * @throws IndexOutOfBoundsException when there are no images
     * @throws NoGpxTimestamps when the gpx track does not contain a timestamp
     */
    static Pair<GpxTimezone, GpxTimeOffset> autoGuess(List<ImageEntry> imgs, GpxData gpx) throws NoGpxTimestamps {

        // Init variables
        long firstExifDate = imgs.get(0).getExifTime().getTime();

        long firstGPXDate = -1;
        // Finds first GPX point
        outer: for (GpxTrack trk : gpx.tracks) {
            for (GpxTrackSegment segment : trk.getSegments()) {
                for (WayPoint curWp : segment.getWayPoints()) {
                    final Date parsedTime = curWp.setTimeFromAttribute();
                    if (parsedTime != null) {
                        firstGPXDate = parsedTime.getTime();
                        break outer;
                    }
                }
            }
        }

        if (firstGPXDate < 0) {
            throw new NoGpxTimestamps();
        }

        return GpxTimeOffset.milliseconds(firstExifDate - firstGPXDate).splitOutTimezone();
    }

    private class AutoGuessActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent arg0) {
            GpxDataWrapper gpxW = selectedGPX(true);
            if (gpxW == null)
                return;
            GpxData gpx = gpxW.data;

            List<ImageEntry> imgs = getSortedImgList();

            try {
                final Pair<GpxTimezone, GpxTimeOffset> r = autoGuess(imgs, gpx);
                timezone = r.a;
                delta = r.b;
            } catch (IndexOutOfBoundsException ex) {
                Logging.debug(ex);
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                        tr("The selected photos do not contain time information."),
                        tr("Photos do not contain time information"), JOptionPane.WARNING_MESSAGE);
                return;
            } catch (NoGpxTimestamps ex) {
                Logging.debug(ex);
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                        tr("The selected GPX track does not contain timestamps. Please select another one."),
                        tr("GPX Track has no time information"), JOptionPane.WARNING_MESSAGE);
                return;
            }

            tfTimezone.getDocument().removeDocumentListener(statusBarUpdater);
            tfOffset.getDocument().removeDocumentListener(statusBarUpdater);

            tfTimezone.setText(timezone.formatTimezone());
            tfOffset.setText(delta.formatOffset());
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
        if (yLayer.data == null) {
            return Collections.emptyList();
        }
        List<ImageEntry> dateImgLst = new ArrayList<>(yLayer.data.size());
        for (ImageEntry e : yLayer.data) {
            if (!e.hasExifTime()) {
                continue;
            }

            if (e.getExifCoor() != null && !exif) {
                continue;
            }

            if (!tagged && e.isTagged() && e.getExifCoor() == null) {
                continue;
            }

            dateImgLst.add(e);
        }

        dateImgLst.sort(Comparator.comparing(ImageEntry::getExifTime));

        return dateImgLst;
    }

    private GpxDataWrapper selectedGPX(boolean complain) {
        Object item = cbGpx.getSelectedItem();

        if (item == null || ((GpxDataWrapper) item).file == null) {
            if (complain) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("You should select a GPX track"),
                        tr("No selected GPX track"), JOptionPane.ERROR_MESSAGE);
            }
            return null;
        }
        return (GpxDataWrapper) item;
    }

}
