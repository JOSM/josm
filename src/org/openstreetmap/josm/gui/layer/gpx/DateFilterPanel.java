// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.widgets.DateEditorWithSlider;
import org.openstreetmap.josm.tools.GBC;

public class DateFilterPanel extends JPanel {
    private DateEditorWithSlider dateFrom = new DateEditorWithSlider(tr("From"));
    private DateEditorWithSlider dateTo = new DateEditorWithSlider(tr("To"));
    private JCheckBox noTimestampCb  = new JCheckBox(tr("No timestamp"));
    private transient GpxLayer layer;

    private transient ActionListener filterAppliedListener;

    private final String prefDate0;
    private final String prefDateMin;
    private final String prefDateMax;

    /**
     * Create the panel to filter tracks on GPX layer @param layer by date
     * Preferences will be stored in @param preferencePrefix
     * If @param enabled = true, the the panel is created as active and filtering occurs immediately.
     */
    public DateFilterPanel(GpxLayer layer, String preferencePrefix, boolean enabled) {
        super(new GridBagLayout());
        prefDate0 = preferencePrefix+".showzerotimestamp";
        prefDateMin = preferencePrefix+".mintime";
        prefDateMax = preferencePrefix+".maxtime";
        this.layer = layer;

        final Date startTime, endTime;
        Date[] bounds = layer.data.getMinMaxTimeForAllTracks();
        startTime = (bounds==null) ? new GregorianCalendar(2000, 1, 1).getTime():bounds[0];
        endTime = (bounds==null) ? new Date() : bounds[1];

        dateFrom.setDate(startTime);
        dateTo.setDate(endTime);
        dateFrom.setRange(startTime, endTime);
        dateTo.setRange(startTime, endTime);

        add(noTimestampCb, GBC.std().grid(1,1).insets(0, 0, 5, 0));
        add(dateFrom, GBC.std().grid(2,1).fill(GBC.HORIZONTAL));
        add(dateTo, GBC.eol().grid(3,1).fill(GBC.HORIZONTAL));

        setEnabled(enabled);

        dateFrom.addDateListener(changeListener);
        dateTo.addDateListener(changeListener);
        noTimestampCb.addChangeListener(changeListener);
    }

    private transient ChangeListener changeListener = new ChangeListener() {
        @Override public void stateChanged(ChangeEvent e) {
            if (isEnabled()) applyFilterWithDelay();
        }
    };

    private Timer t = new Timer(200 , new ActionListener() {
        @Override  public void actionPerformed(ActionEvent e) {
            applyFilter();
        }
    });

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

    public void applyFilter() {
        t.stop();
        filterTracksByDate();
        if (filterAppliedListener!=null)
           filterAppliedListener.actionPerformed(null);
    }

    /**
     * Called by other components when it is correct time to save date filtering parameters
     */
    public void saveInPrefs() {
        Main.pref.putLong(prefDateMin, dateFrom.getDate().getTime());
        Main.pref.putLong(prefDateMax, dateTo.getDate().getTime());
        Main.pref.put(prefDate0, noTimestampCb.isSelected());
    }

    /**
     * If possible, load date ragne and "zero timestamp" option from preferences
     * Called by other components when it is needed.
     */
    public void loadFromPrefs() {
        long t1 =Main.pref.getLong(prefDateMin, 0);
        if (t1!=0) dateFrom.setDate(new Date(t1));
        long t2 =Main.pref.getLong(prefDateMax, 0);
        if (t2!=0) dateTo.setDate(new Date(t2));
        noTimestampCb.setSelected(Main.pref.getBoolean(prefDate0, false));
    }

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
