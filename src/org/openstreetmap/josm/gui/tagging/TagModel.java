// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import java.util.ArrayList;
import java.util.List;

/**
 * Tag model.
 * @since 1762
 */
public class TagModel {

    /** the name of the tag */
    private String name;

    /** the list of values */
    private final List<String> values;

    /**
     * constructor
     */
    public TagModel() {
        values = new ArrayList<>();
        setName("");
        setValue("");
    }

    /**
     * constructor
     * @param name the tag name
     */
    public TagModel(String name) {
        this();
        setName(name);
    }

    /**
     * constructor
     *
     * @param name the tag name
     * @param value the tag value
     */
    public TagModel(String name, String value) {
        this();
        setName(name);
        setValue(value);
    }

    /**
     * sets the name. Converts name to "" if null.
     * @param name the tag name
     */
    public final void setName(String name) {
        this.name = (name == null) ? "" : name;
    }

    /**
     * returns the tag name (key).
     * @return the tag name
     */
    public String getName() {
        return name;
    }

    /**
     * removes all values from the list of values
     */
    public void clearValues() {
        this.values.clear();
    }

    /**
     * sets a unique value for this tag. Converts value to "", if null.
     * @param value the value.
     */
    public final void setValue(String value) {
        clearValues();
        this.values.add((value == null) ? "" : value);
    }

    /**
     * determines if this tag model has a specific value
     * @param value the value to be checked; converted to "" if null
     * @return true, if the values of this tag include <code>value</code>; false otherwise
     */
    public boolean hasValue(String value) {
        return values.contains((value == null) ? "" : value);
    }

    /**
     * adds a tag value
     * @param value the value to add; converted to "" if null
     */
    public void addValue(String value) {
        String val = (value == null) ? "" : value;
        if (hasValue(val)) {
            return;
        }
        values.add(val);
    }

    /**
     * removes a value from the list of values. Converts value to "" if null
     * @param value the value
     */
    public void removeValue(String value) {
        values.remove((value == null) ? "" : value);
    }

    /**
     * returns the list of values
     * @return the list of values
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * returns the value(s) as string
     * @return the value(s) as string, joined with a semicolon (;) if multiple values
     */
    public String getValue() {
        if (getValueCount() == 0) {
            return "";
        } else if (getValueCount() == 1) {
            return values.get(0);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < values.size(); i++) {
                sb.append(values.get(i));
                if (i + 1 < values.size()) {
                    sb.append(';');
                }
            }
            return sb.toString();
        }
    }

    /**
     * returns the number of values
     * @return the number of values
     */
    public int getValueCount() {
        return values.size();
    }
}
