// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.util;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Able to create a name and an icon for a collection of elements.
 *
 * @author frsantos
 */
public class MultipleNameVisitor extends NameVisitor {

    /**
     * Maximum displayed length, in characters.
     */
    public static final IntegerProperty MULTIPLE_NAME_MAX_LENGTH = new IntegerProperty("multiple.name.max.length", 140);
    private static final String MULTI_CLASS_NAME = "object";
    private static final Icon MULTI_CLASS_ICON = ImageProvider.get("data", MULTI_CLASS_NAME);

    /** Name to be displayed */
    private String displayName;

    /**
     * Visits a collection of primitives
     * @param data The collection of primitives
     */
    public void visit(Collection<? extends OsmPrimitive> data) {
        StringBuilder multipleName = new StringBuilder();
        String multiplePluralClassname = null;
        int size = data.size();

        // The class name of the combined primitives
        String multipleClassname = null;
        for (OsmPrimitive osm : data) {
            String name = osm.getDisplayName(DefaultNameFormatter.getInstance());
            if (name != null && !name.isEmpty() && multipleName.length() <= MULTIPLE_NAME_MAX_LENGTH.get()) {
                if (multipleName.length() > 0) {
                    multipleName.append(", ");
                }
                multipleName.append(name);
            }

            osm.accept(this);
            if (multipleClassname == null) {
                multipleClassname = className;
                multiplePluralClassname = classNamePlural;
            } else if (!multipleClassname.equals(className)) {
                multipleClassname = MULTI_CLASS_NAME;
                multiplePluralClassname = trn("object", "objects", 2);
            }
        }

        if (size <= 1) {
            displayName = name;
        } else {
            if (MULTI_CLASS_NAME.equals(multipleClassname)) {
                icon = MULTI_CLASS_ICON;
            }
            StringBuilder sb = new StringBuilder().append(size).append(' ').append(trn(multipleClassname, multiplePluralClassname, size));
            if (multipleName.length() > 0) {
                sb.append(": ");
                if (multipleName.length() <= MULTIPLE_NAME_MAX_LENGTH.get()) {
                    sb.append(multipleName);
                } else {
                    sb.append(multipleName.substring(0, MULTIPLE_NAME_MAX_LENGTH.get())).append("...");
                }
            }
            displayName = sb.toString();
        }
    }

    @Override
    public JLabel toLabel() {
        return new JLabel(getText(), getIcon(), JLabel.HORIZONTAL);
    }

    /**
     * Gets the name of the items
     * @return the name of the items
     */
    public String getText() {
        return displayName;
    }

    /**
     * Gets the icon of the items
     * @return the icon of the items
     */
    public Icon getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return getText();
    }
}
