// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.ParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.ExpertToggleAction.ExpertModeChangeListener;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxData.GpxDataChangeEvent;
import org.openstreetmap.josm.data.gpx.GpxData.GpxDataChangeListener;
import org.openstreetmap.josm.data.gpx.GpxDataContainer;
import org.openstreetmap.josm.data.gpx.GpxImageCorrelation;
import org.openstreetmap.josm.data.gpx.GpxImageCorrelationSettings;
import org.openstreetmap.josm.data.gpx.GpxTimeOffset;
import org.openstreetmap.josm.data.gpx.GpxTimezone;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.geoimage.AdjustTimezoneAndOffsetDialog.AdjustListener;
import org.openstreetmap.josm.gui.layer.geoimage.SynchronizeTimeFromPhotoDialog.TimeZoneItem;
import org.openstreetmap.josm.gui.layer.gpx.GpxDataHelper;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmComboBoxModel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;

/**
 * This class displays the window to select the GPX file and the offset (timezone + delta).
 * Then it correlates the images of the layer with that GPX file.
 * @since 2566
 */
public class CorrelateGpxWithImages extends AbstractAction implements ExpertModeChangeListener, Destroyable {

    private static JosmComboBoxModel<GpxDataWrapper> gpxModel;
    private static boolean forceTags;

    private final transient GeoImageLayer yLayer;
    private transient CorrelationSupportLayer supportLayer;
    private transient GpxTimezone timezone;
    private transient GpxTimeOffset delta;

    /**
     * Constructs a new {@code CorrelateGpxWithImages} action.
     * @param layer The image layer
     */
    public CorrelateGpxWithImages(GeoImageLayer layer) {
        super(tr("Correlate to GPX"));
        new ImageProvider("dialogs/geoimage/gpx2img").getResource().attachImageIcon(this, true);
        this.yLayer = layer;
        ExpertToggleAction.addExpertModeChangeListener(this);
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
                    yLayer.discardTmp();
                    yLayer.updateBufferAndRepaint();
                }
                removeSupportLayer();
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

                yLayer.applyTmp();
                yLayer.updateBufferAndRepaint();
                removeSupportLayer();

                break;
            default:
                throw new IllegalStateException(Integer.toString(result));
            }
        }
    }

    private void removeSupportLayer() {
        if (supportLayer != null) {
            MainApplication.getLayerManager().removeLayer(supportLayer);
            supportLayer = null;
        }
    }

    private static class GpxDataWrapper {
        private String name;
        private final GpxData data;
        private final File file;

        GpxDataWrapper(String name, GpxData data, File file) {
            this.name = name;
            this.data = data;
            this.file = file;
        }

        void setName(String name) {
            this.name = name;
            forEachLayer(CorrelateGpxWithImages::repaintCombobox);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class NoGpxDataWrapper extends GpxDataWrapper {
        NoGpxDataWrapper() {
            super(null, null, null);
        }

        @Override
        public String toString() {
            return tr("<No GPX track loaded yet>");
        }
    }

    private ExtendedDialog syncDialog;
    private JPanel outerPanel;
    private JosmComboBox<GpxDataWrapper> cbGpx;
    private JButton buttonSupport;
    private JosmTextField tfTimezone;
    private JosmTextField tfOffset;
    private JCheckBox cbExifImg;
    private JCheckBox cbTaggedImg;
    private JCheckBox cbShowThumbs;
    private JLabel statusBarText;
    private JSeparator sepDirectionPosition;
    private ImageDirectionPositionPanel pDirectionPosition;

    // remember the last number of matched photos
    private int lastNumMatched;

    /**
     * This class is called when the user doesn't find the GPX file he needs in the files that have
     * been loaded yet. It displays a FileChooser dialog to select the GPX file to be loaded.
     */
    private final class LoadGpxDataActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            File sel = GpxDataHelper.chooseGpxDataFile();
            if (sel != null) {
                try {
                    outerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    removeDuplicates(sel);
                    GpxData data = GpxDataHelper.loadGpxData(sel);
                    if (data != null) {
                        GpxDataWrapper elem = new GpxDataWrapper(sel.getName(), data, sel);
                        gpxModel.addElement(elem);
                        gpxModel.setSelectedItem(elem);
                        statusBarUpdater.matchAndUpdateStatusBar();
                    }
                } finally {
                    outerPanel.setCursor(Cursor.getDefaultCursor());
                }
            }
        }
    }

    private final class UseSupportLayerActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            Optional.ofNullable(selectedGPX(true)).ifPresent(gpx -> {
                supportLayer = new CorrelationSupportLayer(gpx.data);
                supportLayer.getGpxData().addChangeListener(statusBarUpdaterWithRepaint);
                MainApplication.getLayerManager().addLayer(supportLayer);
            });
        }
    }

    private final class AdvancedSettingsActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            AdvancedCorrelationSettingsDialog ed = new AdvancedCorrelationSettingsDialog(MainApplication.getMainFrame(), forceTags);
            if (ed.showDialog().getValue() == 1) {
                forceTags = ed.isForceTaggingSelected(); // This setting is not supposed to be saved permanently

                statusBarUpdater.matchAndUpdateStatusBar();
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
     */
    private final class SetOffsetActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean isOk = false;
            while (!isOk) {
                SynchronizeTimeFromPhotoDialog ed = new SynchronizeTimeFromPhotoDialog(
                        MainApplication.getMainFrame(), yLayer.getImageData().getImages());
                int answer = ed.showDialog().getValue();
                if (answer != 1)
                    return;

                long delta;

                try {
                    delta = ed.getDelta();
                } catch (ParseException ex) {
                    JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("Error while parsing the date.\n"
                            + "Please use the requested format"),
                            tr("Invalid date"), JOptionPane.ERROR_MESSAGE);
                    continue;
                }

                TimeZoneItem selectedTz = ed.getTimeZoneItem();

                Config.getPref().put("geoimage.timezoneid", selectedTz.getID());
                Config.getPref().putBoolean("geoimage.timezoneid.dst", ed.isDstSelected());
                tfOffset.setText(GpxTimeOffset.milliseconds(delta).formatOffset());
                tfTimezone.setText(selectedTz.getFormattedString());

                isOk = true;
            }
            statusBarUpdater.matchAndUpdateStatusBar();
            yLayer.updateBufferAndRepaint();
        }
    }

    private static final class GpxLayerAddedListener implements LayerChangeListener {
        @Override
        public void layerAdded(LayerAddEvent e) {
            Layer layer = e.getAddedLayer();
            if (layer instanceof GpxDataContainer) {
                GpxData gpx = ((GpxDataContainer) layer).getGpxData();
                File file = gpx.storageFile;
                removeDuplicates(file);
                GpxDataWrapper gdw = new GpxDataWrapper(layer.getName(), gpx, file);
                layer.addPropertyChangeListener(new GpxLayerRenamedListener(gdw));
                gpxModel.addElement(gdw);
                forEachLayer(correlateAction -> {
                    correlateAction.repaintCombobox();
                    if (layer.equals(correlateAction.supportLayer)) {
                        correlateAction.buttonSupport.setEnabled(false);
                    }
                });
            }
        }

        @Override
        public void layerRemoving(LayerRemoveEvent e) {
            Layer layer = e.getRemovedLayer();
            if (layer instanceof GpxDataContainer) {
                GpxData removedGpxData = ((GpxDataContainer) layer).getGpxData();
                for (int i = gpxModel.getSize() - 1; i >= 0; i--) {
                    GpxData data = gpxModel.getElementAt(i).data;
                    // removedGpxData can be null if gpx layer has been destroyed before this listener
                    if (data.equals(removedGpxData) || (removedGpxData == null && data.isEmpty())) {
                        gpxModel.removeElementAt(i);
                        forEachLayer(correlateAction -> {
                            correlateAction.repaintCombobox();
                            if (layer.equals(correlateAction.supportLayer)) {
                                correlateAction.supportLayer.getGpxData()
                                    .removeChangeListener(correlateAction.statusBarUpdaterWithRepaint);
                                correlateAction.supportLayer = null;
                                correlateAction.buttonSupport.setEnabled(true);
                            }
                        });
                        break;
                    }
                }
            }
        }

        @Override
        public void layerOrderChanged(LayerOrderChangeEvent e) {
            // Not used
        }
    }

    private static class GpxLayerRenamedListener implements PropertyChangeListener {
        private final GpxDataWrapper gdw;
        GpxLayerRenamedListener(GpxDataWrapper gdw) {
            this.gdw = gdw;
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if (Layer.NAME_PROP.equals(e.getPropertyName())) {
                gdw.setName(e.getNewValue().toString());
            }
        }
    }

    /**
     * Construct the list of loaded GPX tracks
     * @param nogdw Data wrapper with no GPX data
     */
    private void constructGpxModel(NoGpxDataWrapper nogdw) {
        gpxModel = new JosmComboBoxModel<>();
        GpxDataWrapper defaultItem = null;
        for (AbstractModifiableLayer cur : MainApplication.getLayerManager().getLayersOfType(AbstractModifiableLayer.class)) {
            if (cur instanceof GpxDataContainer) {
                GpxData data = ((GpxDataContainer) cur).getGpxData();
                GpxDataWrapper gdw = new GpxDataWrapper(cur.getName(), data, data.storageFile);
                cur.addPropertyChangeListener(new GpxLayerRenamedListener(gdw));
                gpxModel.addElement(gdw);
                if (data.equals(yLayer.gpxData) || defaultItem == null) {
                    defaultItem = gdw;
                }
            }
        }

        if (gpxModel.getSize() == 0) {
            gpxModel.addElement(nogdw);
        } else if (defaultItem != null) {
            gpxModel.setSelectedItem(defaultItem);
        }
        MainApplication.getLayerManager().addLayerChangeListener(new GpxLayerAddedListener());
    }

    static GpxTimezone loadTimezone() {
        try {
            String tz = Config.getPref().get("geoimage.timezone");
            if (!tz.isEmpty()) {
                return GpxTimezone.parseTimezone(tz);
            } else {
                return new GpxTimezone(TimeUnit.MILLISECONDS.toMinutes(TimeZone.getDefault().getRawOffset()) / 60.); //hours is double
            }
        } catch (ParseException e) {
            Logging.trace(e);
            return GpxTimezone.ZERO;
        }
    }

    static GpxTimeOffset loadDelta() {
        try {
            return GpxTimeOffset.parseOffset(Config.getPref().get("geoimage.delta", "0"));
        } catch (ParseException e) {
            Logging.trace(e);
            return GpxTimeOffset.ZERO;
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        NoGpxDataWrapper nogdw = new NoGpxDataWrapper();
        if (gpxModel == null) {
            constructGpxModel(nogdw);
        }

        JPanel panelCb = new JPanel();

        panelCb.add(new JLabel(tr("GPX track: ")));

        cbGpx = new JosmComboBox<>(gpxModel);
        cbGpx.setPrototypeDisplayValue(nogdw);
        cbGpx.addActionListener(statusBarUpdaterWithRepaint);
        panelCb.add(cbGpx);

        JButton buttonOpen = new JButton(tr("Open another GPX trace"));
        buttonOpen.addActionListener(new LoadGpxDataActionListener());
        panelCb.add(buttonOpen);

        buttonSupport = new JButton(tr("Use support layer"));
        buttonSupport.addActionListener(new UseSupportLayerActionListener());
        panelCb.add(buttonSupport);

        JPanel panelTf = new JPanel(new GridBagLayout());

        timezone = loadTimezone();

        tfTimezone = new JosmTextField(10);
        tfTimezone.setText(timezone.formatTimezone());

        delta = loadDelta();

        tfOffset = new JosmTextField(10);
        tfOffset.setText(delta.formatOffset());

        JButton buttonViewGpsPhoto = new JButton(tr("<html>Use photo of an accurate clock,<br>e.g. GPS receiver display</html>"));
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

        int numAll = yLayer.getSortedImgList(true, true).size();
        int numExif = numAll - yLayer.getSortedImgList(false, true).size();
        int numTagged = numAll - yLayer.getSortedImgList(true, false).size();

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

        gbc = GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(0, 0, 0, 12);
        gbc.gridx = 0;
        gbc.gridy = y++;
        panelTf.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        gbc = GBC.std();
        gbc.gridx = 0;
        gbc.gridy = y;
        panelTf.add(new JLabel(tr("Timezone: ")), gbc);

        gbc = GBC.std().fill(GridBagConstraints.HORIZONTAL);
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.weightx = 1.;
        panelTf.add(tfTimezone, gbc);

        gbc = GBC.std();
        gbc.gridx = 0;
        gbc.gridy = y;
        panelTf.add(new JLabel(tr("Offset:")), gbc);

        gbc = GBC.std().fill(GridBagConstraints.HORIZONTAL);
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

        gbc = GBC.std().fill(GridBagConstraints.BOTH).insets(5, 5, 5, 5);
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.weightx = 0.5;
        panelTf.add(buttonAdvanced, gbc);

        gbc.gridx = 2;
        panelTf.add(buttonAutoGuess, gbc);

        gbc.gridx = 3;
        panelTf.add(buttonAdjust, gbc);

        gbc = GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(0, 12, 0, 0);
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

        gbc = GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(0, 12, 0, 0);
        sepDirectionPosition = new JSeparator(SwingConstants.HORIZONTAL);
        panelTf.add(sepDirectionPosition, gbc);

        gbc = GBC.eol();
        gbc.gridwidth = 3;
        pDirectionPosition = ImageDirectionPositionPanel.forGpxTrace();
        panelTf.add(pDirectionPosition, gbc);

        expertChanged(ExpertToggleAction.isExpert());

        final JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        statusBarText = new JLabel(" ");
        statusBarText.setFont(statusBarText.getFont().deriveFont(Font.PLAIN, 8));
        statusBar.add(statusBarText);

        RepaintTheMapListener repaintTheMap = new RepaintTheMapListener(yLayer);
        pDirectionPosition.addFocusListenerOnComponent(repaintTheMap);
        tfTimezone.addFocusListener(repaintTheMap);
        tfOffset.addFocusListener(repaintTheMap);

        tfTimezone.getDocument().addDocumentListener(statusBarUpdater);
        tfOffset.getDocument().addDocumentListener(statusBarUpdater);
        cbExifImg.addItemListener(statusBarUpdaterWithRepaint);
        cbTaggedImg.addItemListener(statusBarUpdaterWithRepaint);
        pDirectionPosition.addChangeListenerOnComponents(statusBarUpdaterWithRepaint);
        pDirectionPosition.addItemListenerOnComponents(statusBarUpdaterWithRepaint);

        outerPanel = new JPanel(new BorderLayout());
        outerPanel.add(statusBar, BorderLayout.PAGE_END);

        if (!GraphicsEnvironment.isHeadless()) {
            forEachLayer(CorrelateGpxWithImages::closeDialog);
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

            statusBarUpdater.matchAndUpdateStatusBar();
            yLayer.updateBufferAndRepaint();
        }
    }

    @Override
    public void expertChanged(boolean isExpert) {
        if (buttonSupport != null) {
            buttonSupport.setVisible(isExpert);
        }
        if (sepDirectionPosition != null) {
            sepDirectionPosition.setVisible(isExpert);
        }
        if (pDirectionPosition != null) {
            pDirectionPosition.setVisible(isExpert);
        }
        if (syncDialog != null) {
            syncDialog.pack();
        }
    }

    private static void removeDuplicates(File file) {
        for (int i = gpxModel.getSize() - 1; i >= 0; i--) {
            GpxDataWrapper wrapper = gpxModel.getElementAt(i);
            if (wrapper instanceof NoGpxDataWrapper || (file != null && file.equals(wrapper.file))) {
                gpxModel.removeElement(wrapper);
            }
        }
    }

    private static void forEachLayer(Consumer<CorrelateGpxWithImages> action) {
        MainApplication.getLayerManager().getLayersOfType(GeoImageLayer.class)
                .forEach(geo -> action.accept(geo.getGpxCorrelateAction()));
    }

    private final transient StatusBarUpdater statusBarUpdater = new StatusBarUpdater(false);
    private final transient StatusBarUpdater statusBarUpdaterWithRepaint = new StatusBarUpdater(true);

    private class StatusBarUpdater implements DocumentListener, ItemListener, ChangeListener, ActionListener, GpxDataChangeListener {
        private final boolean doRepaint;

        StatusBarUpdater(boolean doRepaint) {
            this.doRepaint = doRepaint;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            matchAndUpdateStatusBar();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            matchAndUpdateStatusBar();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            // Do nothing
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            matchAndUpdateStatusBar();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            matchAndUpdateStatusBar();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            matchAndUpdateStatusBar();
        }

        @Override
        public void gpxDataChanged(GpxDataChangeEvent e) {
            matchAndUpdateStatusBar();
        }

        public void matchAndUpdateStatusBar() {
            if (syncDialog != null && syncDialog.isVisible()) {
                statusBarText.setText(matchAndGetStatusText());
                if (doRepaint) {
                    yLayer.updateBufferAndRepaint();
                }
            }
        }

        private String matchAndGetStatusText() {
            try {
                timezone = GpxTimezone.parseTimezone(tfTimezone.getText().trim());
                delta = GpxTimeOffset.parseOffset(tfOffset.getText().trim());
            } catch (ParseException e) {
                return e.getMessage();
            }

            // The selection of images we are about to correlate may have changed.
            // So reset all images.
            yLayer.discardTmp();

            // Construct a list of images that have a date, and sort them on the date.
            List<ImageEntry> dateImgLst = getSortedImgList();
            // Create a temporary copy for each image
            dateImgLst.forEach(ie -> ie.createTmp().unflagNewGpsData());

            GpxDataWrapper selGpx = selectedGPX(false);
            if (selGpx == null)
                return tr("No gpx selected");

            final long offsetMs = ((long) (timezone.getHours() * TimeUnit.HOURS.toMillis(1))) + delta.getMilliseconds(); // in milliseconds
            lastNumMatched = GpxImageCorrelation.matchGpxTrack(dateImgLst, selGpx.data,
                    pDirectionPosition.isVisible() ?
                            new GpxImageCorrelationSettings(offsetMs, forceTags, pDirectionPosition.getSettings()) :
                            new GpxImageCorrelationSettings(offsetMs, forceTags));

            return trn("<html>Matched <b>{0}</b> of <b>{1}</b> photo to GPX track.</html>",
                    "<html>Matched <b>{0}</b> of <b>{1}</b> photos to GPX track.</html>",
                    dateImgLst.size(), lastNumMatched, dateImgLst.size());
        }
    }

    static class RepaintTheMapListener implements FocusListener {

        private final GeoImageLayer yLayer;

        RepaintTheMapListener(GeoImageLayer yLayer) {
            this.yLayer = Objects.requireNonNull(yLayer);
        }

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
    private final class AdjustActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            final GpxTimeOffset offset = GpxTimeOffset.milliseconds(
                    delta.getMilliseconds() + Math.round(timezone.getHours() * TimeUnit.HOURS.toMillis(1)));
            final int dayOffset = offset.getDayOffset();
            final Pair<GpxTimezone, GpxTimeOffset> timezoneOffsetPair = offset.withoutDayOffset().splitOutTimezone();

            // This is called whenever one of the sliders is moved.
            // It calls the "match photos" code
            AdjustListener listener = (tz, min, sec) -> {
                timezone = tz;

                delta = GpxTimeOffset.milliseconds(100L * sec
                        + TimeUnit.MINUTES.toMillis(min)
                        + TimeUnit.DAYS.toMillis(dayOffset));

                tfTimezone.getDocument().removeDocumentListener(statusBarUpdater);
                tfOffset.getDocument().removeDocumentListener(statusBarUpdater);

                tfTimezone.setText(timezone.formatTimezone());
                tfOffset.setText(delta.formatOffset());

                tfTimezone.getDocument().addDocumentListener(statusBarUpdater);
                tfOffset.getDocument().addDocumentListener(statusBarUpdater);

                statusBarUpdater.matchAndUpdateStatusBar();
                yLayer.updateBufferAndRepaint();

                return statusBarText.getText();
            };

            // There is no way to cancel this dialog, all changes get applied
            // immediately. Therefore "Close" is marked with an "OK" icon.
            // Settings are only saved temporarily to the layer.
            new AdjustTimezoneAndOffsetDialog(MainApplication.getMainFrame(),
                    timezoneOffsetPair.a, timezoneOffsetPair.b, dayOffset)
            .adjustListener(listener).showDialog();
        }
    }

    static class NoGpxTimestamps extends Exception {
    }

    void closeDialog() {
        if (syncDialog != null) {
            syncDialog.setVisible(false);
            new SyncDialogWindowListener().windowDeactivated(null);
            syncDialog.dispose();
            syncDialog = null;
        }
    }

    void repaintCombobox() {
        if (cbGpx != null) {
            cbGpx.repaint();
        }
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
        long firstExifDate = imgs.get(0).getExifInstant().toEpochMilli();

        // Finds first GPX point
        long firstGPXDate = gpx.tracks.stream()
                .flatMap(trk -> trk.getSegments().stream())
                .flatMap(segment -> segment.getWayPoints().stream())
                .filter(WayPoint::hasDate)
                .map(WayPoint::getTimeInMillis)
                .findFirst()
                .orElseThrow(NoGpxTimestamps::new);

        return GpxTimeOffset.milliseconds(firstExifDate - firstGPXDate).splitOutTimezone();
    }

    private final class AutoGuessActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
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

            statusBarUpdater.matchAndUpdateStatusBar();
            yLayer.updateBufferAndRepaint();
        }
    }

    private List<ImageEntry> getSortedImgList() {
        return yLayer.getSortedImgList(cbExifImg.isSelected(), cbTaggedImg.isSelected());
    }

    private static GpxDataWrapper selectedGPX(boolean complain) {
        Object item = gpxModel.getSelectedItem();

        if (item == null || ((GpxDataWrapper) item).data == null) {
            if (complain) {
                JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr("You should select a GPX track"),
                        tr("No selected GPX track"), JOptionPane.ERROR_MESSAGE);
            }
            return null;
        }
        return (GpxDataWrapper) item;
    }

    @Override
    public void destroy() {
        ExpertToggleAction.removeExpertModeChangeListener(this);
        if (cbGpx != null) {
            // Force the JCombobox to remove its eventListener from the static GpxDataWrapper
            cbGpx.setModel(new DefaultComboBoxModel<>());
            cbGpx = null;
        }

        closeDialog();

        outerPanel = null;
        tfTimezone = null;
        tfOffset = null;
        cbExifImg = null;
        cbTaggedImg = null;
        cbShowThumbs = null;
        statusBarText = null;
        sepDirectionPosition = null;
        pDirectionPosition = null;
    }
}
