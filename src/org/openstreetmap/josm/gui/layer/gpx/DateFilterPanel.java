// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.widgets.DateEditorWithSlider;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

/**
 * A panel that allows the user to input a date range he wants to filter the GPX data for.
 */
public class DateFilterPanel extends JPanel {
    private final DateEditorWithSlider dateFrom = new DateEditorWithSlider(tr("From"));
    private final DateEditorWithSlider dateTo = new DateEditorWithSlider(tr("To"));
    private final JCheckBox noTimestampCb = new JCheckBox(tr("No timestamp"));
    private final transient GpxLayer layer;

    private transient ActionListener filterAppliedListener;

    private final String prefDate0;
    private final String prefDateMin;
    private final String prefDateMax;

    /**
     * Create the panel to filter tracks on GPX layer {@code layer} by date
     * Preferences will be stored in {@code preferencePrefix}
     * If {@code enabled = true}, then the panel is created as active and filtering occurs immediately.
     * @param layer GPX layer
     * @param preferencePrefix preference prefix
     * @param enabled panel initial enabled state
     */
    public DateFilterPanel(GpxLayer layer, String preferencePrefix, boolean enabled) {
        super(new GridBagLayout());
        prefDate0 = preferencePrefix+".showzerotimestamp";
        prefDateMin = preferencePrefix+".mintime";
        prefDateMax = preferencePrefix+".maxtime";
        this.layer = layer;

        final Date startTime, endTime;
        Date[] bounds = layer.data.getMinMaxTimeForAllTracks();
        startTime = (bounds.length == 0) ? Date.from(ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()).toInstant()) : bounds[0];
        endTime = (bounds.length == 0) ? new Date() : bounds[1];

        dateFrom.setDate(startTime);
        dateTo.setDate(endTime);
        dateFrom.setRange(startTime, endTime);
        dateTo.setRange(startTime, endTime);

        add(noTimestampCb, GBC.std().grid(1, 1).insets(0, 0, 5, 0));
        add(dateFrom, GBC.std().grid(2, 1).fill(GBC.HORIZONTAL));
        add(dateTo, GBC.eol().grid(3, 1).fill(GBC.HORIZONTAL));

        setEnabled(enabled);

        ChangeListener changeListener = e -> {
            if (isEnabled()) applyFilterWithDelay();
        };

        dateFrom.addDateListener(changeListener);
        dateTo.addDateListener(changeListener);
        noTimestampCb.addChangeListener(changeListener);
    }

    private final Timer t = new Timer(200, e -> applyFilter());

    /**
     * Do filtering but little bit later (to reduce cpu load)
     */
    public void applyFilterWithDelay() {
        if (t.isRunning()) {
            t.restart();
        } else {
            t.start();
        }
    }

    /**
     * Applies the filter that was input by the user to the GPX track
     */
    public void applyFilter() {
        t.stop();
        filterTracksByDate();
        if (filterAppliedListener != null)
           filterAppliedListener.actionPerformed(null);
    }

    /**
     * Called by other components when it is correct time to save date filtering parameters
     */
    public void saveInPrefs() {
        Config.getPref().putLong(prefDateMin, dateFrom.getDate().getTime());
        Config.getPref().putLong(prefDateMax, dateTo.getDate().getTime());
        Config.getPref().putBoolean(prefDate0, noTimestampCb.isSelected());
    }

    /**
     * If possible, load date ragne and "zero timestamp" option from preferences
     * Called by other components when it is needed.
     */
    public void loadFromPrefs() {
        long t1 = Config.getPref().getLong(prefDateMin, 0);
        if (t1 != 0) dateFrom.setDate(new Date(t1));
        long t2 = Config.getPref().getLong(prefDateMax, 0);
        if (t2 != 0) dateTo.setDate(new Date(t2));
        noTimestampCb.setSelected(Config.getPref().getBoolean(prefDate0, false));
    }

    /**
     * Sets a listener that should be called after the filter was applied
     * @param filterAppliedListener The listener to call
     */
    public void setFilterAppliedListener(ActionListener filterAppliedListener) {
        this.filterAppliedListener = filterAppliedListener;
    }

    private void filterTracksByDate() {
        Date from = dateFrom.getDate();
        Date to = dateTo.getDate();
        layer.filterTracksByDate(from, to, noTimestampCb.isSelected());
    }

    @Override
    public final void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component c: getComponents()) {
            c.setEnabled(enabled);
        }
    }
}
