// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openstreetmap.josm.tools.GBC;


/**
 * Widget originally created for date filtering of GPX tracks. 
 * Allows to enter the date or choose it by using slider
 */
public class DateEditorWithSlider extends JPanel {
    private JSpinner spinner;
    private JSlider slider;
    private Date dateMin;
    private Date dateMax;
    private static final int MAX_SLIDER=300;
    private boolean watchSlider = true;
    
    private List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    public DateEditorWithSlider(String labelText) {
        super(new GridBagLayout());
        spinner = new JSpinner( new SpinnerDateModel() );
        String pattern = ((SimpleDateFormat)DateFormat.getDateInstance()).toPattern();
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(spinner,pattern);
        spinner.setEditor(timeEditor);
        

        spinner.setPreferredSize(new Dimension(spinner.getPreferredSize().width+5,
        spinner.getPreferredSize().height));
        
        slider = new JSlider(0,MAX_SLIDER);
        spinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int i = slider.getValue();
                Date d = (Date) spinner.getValue();
                int j = intFromDate(d);
                if (i!=j) {
                    watchSlider=false;
                    slider.setValue(j);
                    watchSlider=true;
                }
                for (ChangeListener l : listeners) {
                    l.stateChanged(e);
                }
            }
        });
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!watchSlider) return;
                Date d = (Date) spinner.getValue();
                Date d1 = dateFromInt(slider.getValue());
                if (!d.equals(d1)) {
                    spinner.setValue(d1);
                }
            }
        });
        add(new JLabel(labelText),GBC.std());
        add(spinner,GBC.std().insets(10,0,0,0));
        add(slider,GBC.eol().insets(10,0,0,0).fill(GBC.HORIZONTAL));

        dateMin = new Date(0); dateMax =new Date();
    }

    protected Date dateFromInt(int value) {
        double k = 1.0*value/MAX_SLIDER;
        return new Date((long)(dateMax.getTime()*k+ dateMin.getTime()*(1-k)));
    }
    
    protected int intFromDate(Date date) {
        return (int)(300.0*(date.getTime()-dateMin.getTime()) /
                (dateMax.getTime()-dateMin.getTime()));
    }

    public void setRange(Date dateMin, Date dateMax) {
        this.dateMin = dateMin;
        this.dateMax = dateMax;
    }

    public void setDate(Date date) {
        spinner.setValue(date);
    }

    public Date getDate() {
        return (Date) spinner.getValue();
    }
    
    public void addDateListener(ChangeListener l) {
        listeners.add(l);
    }

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
