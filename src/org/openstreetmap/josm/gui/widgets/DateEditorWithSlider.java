// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Widget originally created for date filtering of GPX tracks.
 * Allows to enter the date or choose it by using slider
 * @since 5815
 */
public class DateEditorWithSlider extends JPanel {
    private final JSpinner spinner;
    private final JSlider slider;
    private Instant dateMin;
    private Instant dateMax;
    private static final int MAX_SLIDER = 300;
    private boolean watchSlider = true;

    private final transient List<ChangeListener> listeners = new ArrayList<>();

    /**
     * Constructs a new {@code DateEditorWithSlider} with a given label
     * @param labelText The label to display
     */
    public DateEditorWithSlider(String labelText) {
        super(new GridBagLayout());
        spinner = new JSpinner(new SpinnerDateModel());
        String pattern = ((SimpleDateFormat) DateUtils.getDateFormat(DateFormat.DEFAULT)).toPattern();
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(spinner, pattern);
        spinner.setEditor(timeEditor);

        spinner.setPreferredSize(new Dimension(spinner.getPreferredSize().width+5,
        spinner.getPreferredSize().height));

        slider = new JSlider(0, MAX_SLIDER);
        spinner.addChangeListener(e -> {
            int i = slider.getValue();
            Date d = (Date) spinner.getValue();
            int j = intFromDate(d);
            if (i != j) {
                watchSlider = false;
                slider.setValue(j);
                watchSlider = true;
            }
            for (ChangeListener l : listeners) {
                l.stateChanged(e);
            }
        });
        slider.addChangeListener(e -> {
            if (!watchSlider) return;
            Date d = (Date) spinner.getValue();
            Date d1 = dateFromInt(slider.getValue());
            if (!d.equals(d1)) {
                spinner.setValue(d1);
            }
        });
        add(new JLabel(labelText), GBC.std());
        add(spinner, GBC.std().insets(10, 0, 0, 0));
        add(slider, GBC.eol().insets(10, 0, 0, 0).fill(GBC.HORIZONTAL));

        dateMin = Instant.EPOCH;
        dateMax = Instant.now();
    }

    protected Date dateFromInt(int value) {
        double k = 1.0*value/MAX_SLIDER;
        return new Date((long) (dateMax.toEpochMilli()*k+ dateMin.toEpochMilli()*(1-k)));
    }

    protected int intFromDate(Date date) {
        return (int) (300.0*(date.getTime()-dateMin.getEpochSecond()) /
                (dateMax.getEpochSecond()-dateMin.getEpochSecond()));
    }

    /**
     * Sets the date range that is available using the slider
     * @param dateMin The min date
     * @param dateMax The max date
     */
    public void setRange(Instant dateMin, Instant dateMax) {
        this.dateMin = dateMin;
        this.dateMax = dateMax;
    }

    /**
     * Sets the slider to the given value
     * @param date The date
     */
    public void setDate(Instant date) {
        spinner.setValue(Date.from(date));
    }

    /**
     * Gets the date that was selected by the user
     * @return The date
     */
    public Instant getDate() {
        return ((Date) spinner.getValue()).toInstant();
    }

    /**
     * Adds a change listener to this date editor.
     * @param l The listener
     */
    public void addDateListener(ChangeListener l) {
        listeners.add(l);
    }

    /**
     * Removes a change listener from this date editor.
     * @param l The listener
     */
    public void removeDateListener(ChangeListener l) {
        listeners.remove(l);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component c: getComponents()) {
            c.setEnabled(enabled);
        }
    }
}
