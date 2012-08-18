// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Toolkit;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.ComboPopup;

/**
 * Class overriding each {@link JComboBox} in JOSM to control consistently the number of displayed items at once.<br/>
 * This is needed because of the default Java behaviour that may display the top-down list off the screen (see #7917).
 * 
 * @since 5429
 */
public class JosmComboBox extends JComboBox {

    /**
     * The default prototype value used to compute the maximum number of elements to be displayed at once before 
     * displaying a scroll bar
     */
    public static final String DEFAULT_PROTOTYPE_DISPLAY_VALUE = "Prototype display value";
    
    /**
     * Creates a <code>JosmComboBox</code> with a default data model.
     * The default data model is an empty list of objects.
     * Use <code>addItem</code> to add items. By default the first item
     * in the data model becomes selected.
     *
     * @see DefaultComboBoxModel
     */
    public JosmComboBox() {
        this(DEFAULT_PROTOTYPE_DISPLAY_VALUE);
    }

    /**
     * Creates a <code>JosmComboBox</code> with a default data model and
     * the specified prototype display value.
     * The default data model is an empty list of objects.
     * Use <code>addItem</code> to add items. By default the first item
     * in the data model becomes selected.
     * 
     * @param prototypeDisplayValue the <code>Object</code> used to compute 
     *      the maximum number of elements to be displayed at once before 
     *      displaying a scroll bar
     *
     * @see DefaultComboBoxModel
     * @since 5450
     */
    public JosmComboBox(Object prototypeDisplayValue) {
        super();
        init(prototypeDisplayValue);
    }

    /**
     * Creates a <code>JosmComboBox</code> that takes its items from an
     * existing <code>ComboBoxModel</code>. Since the
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
        init(aModel != null && aModel.getSize() > 0 ? aModel.getElementAt(0) : null);
    }

    /** 
     * Creates a <code>JosmComboBox</code> that contains the elements
     * in the specified array. By default the first item in the array
     * (and therefore the data model) becomes selected.
     *
     * @param items  an array of objects to insert into the combo box
     * @see DefaultComboBoxModel
     */
    public JosmComboBox(Object[] items) {
        super(items);
        init(items != null && items.length > 0 ? items[0] : null);
    }

    /**
     * Creates a <code>JosmComboBox</code> that contains the elements
     * in the specified Vector. By default the first item in the vector
     * (and therefore the data model) becomes selected.
     *
     * @param items  an array of vectors to insert into the combo box
     * @see DefaultComboBoxModel
     */
    public JosmComboBox(Vector<?> items) {
        super(items);
        init(items != null && !items.isEmpty() ? items.get(0) : null);
    }
    
    protected void init(Object prototype) {
        if (prototype != null) {
            setPrototypeDisplayValue(prototype);
            int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
            // Compute maximum number of visible items based on the preferred size of the combo box. 
            // This assumes that items have the same height as the combo box, which is not granted by the look and feel
            int maxsize = (screenHeight/getPreferredSize().height) / 2;
            // If possible, adjust the maximum number of items with the real height of items
            // It is not granted this works on every platform (tested OK on Windows)
            for (int i = 0; i < getUI().getAccessibleChildrenCount(this); i++) {
                Accessible child = getUI().getAccessibleChild(this, i);
                if (child instanceof ComboPopup) {
                    JList list = ((ComboPopup)child).getList();
                    if (list != null) {
                        if (list.getPrototypeCellValue() != prototype) {
                            list.setPrototypeCellValue(prototype);
                        }
                        int height = list.getFixedCellHeight();
                        if (height > 0) {
                            maxsize = (screenHeight/height) / 2;
                        }
                    }
                    break;
                }
            }
            setMaximumRowCount(Math.max(getMaximumRowCount(), maxsize));
        }
    }
}
