// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.data.gpx.GpxTimeOffset;
import org.openstreetmap.josm.data.gpx.GpxTimezone;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;

/**
 * Dialog used to manually adjust timezone and offset for GPX correlation.
 * @since 18043 (extracted from CorrelateGpxWithImages)
 */
public class AdjustTimezoneAndOffsetDialog extends ExtendedDialog {

    private AdjustListener listener;

    /**
     * Constructs a new {@code AdjustTimezoneAndOffsetDialog}
     * @param parent The parent element that will be used for position and maximum size
     * @param tz initial timezone
     * @param offset initial time offset
     * @param dayOffset days offset
     */
    public AdjustTimezoneAndOffsetDialog(Component parent, GpxTimezone tz, GpxTimeOffset offset, int dayOffset) {
        super(parent, tr("Adjust timezone and offset"), tr("Close"));
        setContent(buildContent(tz, offset, dayOffset));
        setButtonIcons("ok");
    }

    private Component buildContent(GpxTimezone a, GpxTimeOffset b, int dayOffset) {
        // Info Labels
        final JLabel lblMatches = new JLabel();

        // Timezone Slider
        // The slider allows to switch timezone from -12:00 to 12:00 in 30 minutes steps. Therefore the range is -24 to 24.
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
            sldTimezone.setValue((int) (a.getHours() * 2));
            sldMinutes.setValue((int) (b.getSeconds() / 60));
            final long deciSeconds = b.getMilliseconds() / 100;
            sldSeconds.setValue((int) (deciSeconds % 600));
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException ex) {
            Logging.warn(ex);
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(),
                    tr("An error occurred while trying to match the photos to the GPX track."
                            +" You can adjust the sliders to manually match the photos."),
                            tr("Matching photos to track failed"),
                            JOptionPane.WARNING_MESSAGE);
        }

        // This is called whenever one of the sliders is moved.
        // It updates the labels
        ChangeListener sliderListener = x -> {
            final GpxTimezone timezone = new GpxTimezone(sldTimezone.getValue() / 2.);
            final int minutes = sldMinutes.getValue();
            final int seconds = sldSeconds.getValue();

            lblTimezone.setText(tr("Timezone: {0}", timezone.formatTimezone()));
            lblMinutes.setText(tr("Minutes: {0}", minutes));
            lblSeconds.setText(tr("Seconds: {0}", GpxTimeOffset.milliseconds(100L * seconds).formatOffset()));

            StringBuilder sb = new StringBuilder("<html>");
            if (listener != null) {
                sb.append(listener.valuesChanged(timezone, minutes, seconds)).append("<br>");
            }

            lblMatches.setText(sb.append(trn("(Time difference of {0} day)", "Time difference of {0} days",
                    Math.abs(dayOffset), Math.abs(dayOffset))).append("</html>").toString());
        };

        // Call the sliderListener once manually so labels get adjusted
        sliderListener.stateChanged(null);

        // Listeners added here, otherwise it tries to match three times
        // (when setting the default values)
        sldTimezone.addChangeListener(sliderListener);
        sldMinutes.addChangeListener(sliderListener);
        sldSeconds.addChangeListener(sliderListener);

        return p;
    }

    /**
     * Listener called when the sliders are moved.
     */
    public interface AdjustListener {
        /**
         * Provides a textual description matching the new state after the change of values.
         * @param timezone new timezone
         * @param minutes new minutes offset
         * @param seconds new seconds offset
         * @return an HTML textual description matching the new state after the change of values
         */
        String valuesChanged(GpxTimezone timezone, int minutes, int seconds);
    }

    /**
     * Sets the {@link AdjustListener}.
     * @param listener adjuust listener. Can be null
     * @return {@code this}
     */
    public final AdjustTimezoneAndOffsetDialog adjustListener(AdjustListener listener) {
        this.listener = listener;
        return this;
    }
}
