// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;

/**
 * Class overriding each {@link JComboBox} in JOSM to control consistently the number of displayed items at once.<br/>
 * This is needed because of the default Java behaviour that may display the top-down list off the screen (see #).
 * <p>
 * The property {@code gui.combobox.maximum-row-count} can be setup to control this behaviour.
 * 
 * @since 5429
 */
public class JosmComboBox extends JComboBox implements PreferenceChangedListener {

    /**
     * This property allows to control the {@link #getMaximumRowCount} of all combo boxes used in JOSM.
     */
    public static final String PROP_MAXIMUM_ROW_COUNT = "gui.combobox.maximum-row-count";
    
    /**
     * Creates a <code>JosmComboBox</code> with a default data model.
     * The default data model is an empty list of objects.
     * Use <code>addItem</code> to add items.  By default the first item
     * in the data model becomes selected.
     *
     * @see DefaultComboBoxModel
     */
    public JosmComboBox() {
        super();
        init();
    }

    /**
     * Creates a <code>JosmComboBox</code> that takes its items from an
     * existing <code>ComboBoxModel</code>.  Since the
     * <code>ComboBoxModel</code> is provided, a combo box created using
     * this constructor does not create a default combo box model and
     * may impact how the insert, remove and add methods behave.
     *
     * @param aModel the <code>ComboBoxModel</code> that provides the 
     *      displayed list of items
     * @see DefaultComboBoxModel
     */
    public JosmComboBox(ComboBoxModel aModel) {
        super(aModel);
        init();
    }

    /** 
     * Creates a <code>JosmComboBox</code> that contains the elements
     * in the specified array.  By default the first item in the array
     * (and therefore the data model) becomes selected.
     *
     * @param items  an array of objects to insert into the combo box
     * @see DefaultComboBoxModel
     */
    public JosmComboBox(Object[] items) {
        super(items);
        init();
    }

    /**
     * Creates a <code>JosmComboBox</code> that contains the elements
     * in the specified Vector.  By default the first item in the vector
     * (and therefore the data model) becomes selected.
     *
     * @param items  an array of vectors to insert into the combo box
     * @see DefaultComboBoxModel
     */
    public JosmComboBox(Vector<?> items) {
        super(items);
        init();
    }
    
    protected void init() {
        setMaximumRowCount(Main.pref.getInteger(PROP_MAXIMUM_ROW_COUNT, 20));
    }
    
    /**
     * Registers this combo box to the change of the property {@code gui.combobox.maximum-row-count}.<br/>
     * Do not forget to call {@link #unregisterFromPreferenceChange} when the combo box is no longer needed.
     * @see #unregisterFromPreferenceChange
     */
    public final void registerToPreferenceChange() {
        Main.pref.addPreferenceChangeListener(this);
    }
    
    /**
     * Unregisters this combo box previously registered by {@link #registerToPreferenceChange}.
     * @see #registerToPreferenceChange
     */
    public final void unregisterFromPreferenceChange() {
        Main.pref.removePreferenceChangeListener(this);
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (PROP_MAXIMUM_ROW_COUNT.equals(e.getKey())) {
            setMaximumRowCount(Main.pref.getInteger(PROP_MAXIMUM_ROW_COUNT, 20));
        }
    }
}
