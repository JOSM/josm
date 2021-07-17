// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.actions.DiskAccessAction;
import org.openstreetmap.josm.data.gpx.GpxTimezone;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.io.importexport.ImageImporter;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Dialog to synchronize time from a photo of the GPS receiver
 * @since 18045 (extracted from {@link CorrelateGpxWithImages})
 */
class SynchronizeTimeFromPhotoDialog extends ExtendedDialog {

    private JCheckBox ckDst;
    private ImageDisplay imgDisp;
    private JLabel lbExifTime;
    private JosmTextField tfGpsTime;
    private JosmComboBox<TimeZoneItem> cbTimezones;

    private final SimpleDateFormat dateFormat = getDateTimeFormat();

    /**
     * Constructs a new {@code SynchronizeTimeFromPhotoDialog}.
     * @param parent The parent element that will be used for position and maximum size
     * @param images list of image entries
     */
    SynchronizeTimeFromPhotoDialog(Component parent, List<ImageEntry> images) {
        super(parent, tr("Synchronize time from a photo of the GPS receiver"), tr("OK"), tr("Cancel"));
        setButtonIcons("ok", "cancel");
        setContent(buildContent(images));
    }

    private Component buildContent(List<ImageEntry> images) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(tr("<html>Take a photo of your GPS receiver while it displays the time.<br>"
                + "Display that photo here.<br>"
                + "And then, simply capture the time you read on the photo and select a timezone<hr></html>")),
                BorderLayout.NORTH);

        imgDisp = new ImageDisplay();
        imgDisp.setPreferredSize(new Dimension(300, 225));
        panel.add(imgDisp, BorderLayout.CENTER);

        JPanel panelTf = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = gc.gridy = 0;
        gc.gridwidth = gc.gridheight = 1;
        gc.weightx = gc.weighty = 0.0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.LINE_START;
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
        gc.anchor = GridBagConstraints.LINE_START;
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
        panelTf.add(new JLabel(" ["+dateFormat.toLocalizedPattern()+']'), gc);

        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = gc.gridheight = 1;
        gc.weightx = gc.weighty = 0.0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.LINE_START;
        panelTf.add(new JLabel(tr("Photo taken in the timezone of: ")), gc);

        ckDst = new JCheckBox(tr("Use daylight saving time (where applicable)"), Config.getPref().getBoolean("geoimage.timezoneid.dst"));

        String[] tmp = TimeZone.getAvailableIDs();
        List<TimeZoneItem> vtTimezones = new ArrayList<>(tmp.length);

        String defTzStr = Config.getPref().get("geoimage.timezoneid", "");
        if (defTzStr.isEmpty()) {
            defTzStr = TimeZone.getDefault().getID();
        }
        TimeZoneItem defTzItem = null;

        for (String tzStr : tmp) {
            TimeZoneItem tz = new TimeZoneItem(TimeZone.getTimeZone(tzStr));
            vtTimezones.add(tz);
            if (defTzStr.equals(tzStr)) {
                defTzItem = tz;
            }
        }

        Collections.sort(vtTimezones);

        cbTimezones = new JosmComboBox<>(vtTimezones.toArray(new TimeZoneItem[0]));

        if (defTzItem != null) {
            cbTimezones.setSelectedItem(defTzItem);
        }

        gc.gridx = 1;
        gc.weightx = 1.0;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        panelTf.add(cbTimezones, gc);

        gc.gridy = 3;
        panelTf.add(ckDst, gc);

        ckDst.addActionListener(x -> cbTimezones.repaint());

        panel.add(panelTf, BorderLayout.SOUTH);

        JPanel panelLst = new JPanel(new BorderLayout());

        JList<String> imgList = new JList<>(new AbstractListModel<String>() {
            @Override
            public String getElementAt(int i) {
                return images.get(i).getDisplayName();
            }

            @Override
            public int getSize() {
                return images.size();
            }
        });
        imgList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        imgList.getSelectionModel().addListSelectionListener(evt -> updateExifComponents(images.get(imgList.getSelectedIndex())));
        panelLst.add(new JScrollPane(imgList), BorderLayout.CENTER);

        JButton openButton = new JButton(tr("Open another photo"));
        openButton.addActionListener(ae -> {
            AbstractFileChooser fc = DiskAccessAction.createAndOpenFileChooser(true, false, null,
                    ImageImporter.FILE_FILTER_WITH_FOLDERS, JFileChooser.FILES_ONLY, "geoimage.lastdirectory");
            if (fc == null)
                return;
            ImageEntry entry = new ImageEntry(fc.getSelectedFile());
            entry.extractExif();
            updateExifComponents(entry);
        });
        panelLst.add(openButton, BorderLayout.PAGE_END);

        panel.add(panelLst, BorderLayout.LINE_START);

        return panel;
    }

    final long getDelta() throws ParseException {
        return dateFormat.parse(lbExifTime.getText()).getTime()
             - dateFormat.parse(tfGpsTime.getText()).getTime();
    }

    final TimeZoneItem getTimeZoneItem() {
        return (TimeZoneItem) cbTimezones.getSelectedItem();
    }

    /**
     * Determines if daylight saving time is selected.
     * @return {@code true} if daylight saving time is selected
     */
    final boolean isDstSelected() {
        return ckDst.isSelected();
    }

    protected void updateExifComponents(ImageEntry img) {
        imgDisp.setImage(img);
        Instant date = img.getExifInstant();
        if (date != null) {
            DateFormat df = getDateTimeFormat();
            df.setTimeZone(DateUtils.UTC); // EXIF data does not contain timezone information and is read as UTC
            lbExifTime.setText(df.format(Date.from(date)));
            tfGpsTime.setText(df.format(Date.from(date)));
            tfGpsTime.setCaretPosition(tfGpsTime.getText().length());
            tfGpsTime.setEnabled(true);
            tfGpsTime.requestFocus();
        } else {
            lbExifTime.setText(tr("No date"));
            tfGpsTime.setText("");
            tfGpsTime.setEnabled(false);
        }
    }

    private static SimpleDateFormat getDateTimeFormat() {
        return (SimpleDateFormat) DateUtils.getDateTimeFormat(DateFormat.SHORT, DateFormat.MEDIUM);
    }

    class TimeZoneItem implements Comparable<TimeZoneItem> {
        private final TimeZone tz;
        private String rawString;
        private String dstString;

        TimeZoneItem(TimeZone tz) {
            this.tz = tz;
        }

        public String getFormattedString() {
            if (ckDst.isSelected()) {
                return getDstString();
            } else {
                return getRawString();
            }
        }

        public String getDstString() {
            if (dstString == null) {
                dstString = formatTimezone(tz.getRawOffset() + tz.getDSTSavings());
            }
            return dstString;
        }

        public String getRawString() {
            if (rawString == null) {
                rawString = formatTimezone(tz.getRawOffset());
            }
            return rawString;
        }

        public String getID() {
            return tz.getID();
        }

        @Override
        public String toString() {
            return getID() + " (" + getFormattedString() + ')';
        }

        @Override
        public int compareTo(TimeZoneItem o) {
            return getID().compareTo(o.getID());
        }

        private String formatTimezone(int offset) {
            return new GpxTimezone((double) offset / TimeUnit.HOURS.toMillis(1)).formatTimezone();
        }
    }
}
