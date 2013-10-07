// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import java.util.ArrayList;
import java.util.List;

public class TagModel {

    /** the name of the tag */
    private String name = null;

    /** the list of values */
    private List<String> values = null;

    /**
     * constructor
     */
    public TagModel() {
        values = new ArrayList<String>();
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
    public void setName(String name) {
        name = (name == null) ? "" : name;
        this.name = name;
    }

    /**
     * @return the tag name
     */
    public String getName(){
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
    public void setValue(String value) {
        value = (value == null) ? "" : value;
        clearValues();
        this.values.add(value);
    }

    /**
     *
     * @param value the value to be checked; converted to "" if null
     * @return true, if the values of this tag include <code>value</code>; false otherwise
     */
    public boolean hasValue(String value) {
        value = (value == null) ? "" : value;
        return values.contains(value);
    }

    public void addValue(String value) {
        value = (value == null) ? "" : value;
        if (hasValue(value)) {
            return;
        }
        values.add(value);
    }

    /**
     * removes a value from the list of values. Converts value to "" if null
     * @param value the value
     */
    public void removeValue(String value){
        value = (value == null) ? "" : value;
        values.remove(value);
    }

    public List<String> getValues() {
        return values;
    }

    public String getValue() {
        if (getValueCount() == 0) {
            return "";
        } else if (getValueCount() == 1) {
            return values.get(0);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i =0; i < values.size(); i++) {
                sb.append(values.get(i));
                if (i + 1 < values.size()) {
                    sb.append(";");
                }
            }
            return sb.toString();
        }
    }

    public int getValueCount() {
        return values.size();
    }
}
