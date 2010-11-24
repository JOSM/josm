// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.util;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Able to create a name and an icon for a collection of elements.
 *
 * @author frsantos
 */
public class MultipleNameVisitor extends NameVisitor
{
    /** The class name of the combined primitives */
    String multipleClassname;
    /* name to be displayed */
    String displayName;
    /** Size of the collection */
    int size;

    /**
     * Visits a collection of primitives
     * @param data The collection of primitives
     */
    public void visit(Collection<? extends OsmPrimitive> data)
    {
        String multipleName = null;
        String multiplePluralClassname = null;
        String firstName = null;
        boolean initializedname = false;
        size = data.size();

        multipleClassname = null;
        for (OsmPrimitive osm : data)
        {
            String name = osm.get("name");
            if(name == null) name = osm.get("ref");
            if(!initializedname)
            {
                multipleName = name; initializedname = true;
            }
            else if(multipleName != null && (name == null  || !name.equals(multipleName)))
            {
                multipleName = null;
            }

            if(firstName == null && name != null)
                firstName = name;
            osm.visit(this);
            if (multipleClassname == null)
            {
                multipleClassname = className;
                multiplePluralClassname = classNamePlural;
            }
            else if (!multipleClassname.equals(className))
            {
                multipleClassname = "object";
                multiplePluralClassname = trn("object", "objects", 2);
            }
        }

        if( size == 1 )
            displayName = name;
        else if(multipleName != null)
            displayName = size + " " + trn(multipleClassname, multiplePluralClassname, size) + ": " + multipleName;
        else if(firstName != null)
            displayName = size + " " + trn(multipleClassname, multiplePluralClassname, size) + ": " + tr("{0}, ...", firstName);
        else
            displayName = size + " " + trn(multipleClassname, multiplePluralClassname, size);
    }

    @Override
    public JLabel toLabel()
    {
        return new JLabel(getText(), getIcon(), JLabel.HORIZONTAL);
    }

    /**
     * Gets the name of the items
     * @return the name of the items
     */
    public String getText()
    {
        return displayName;
    }

    /**
     * Gets the icon of the items
     * @return the icon of the items
     */
    public Icon getIcon()
    {
        if( size == 1 )
            return icon;
        else
            return ImageProvider.get("data", multipleClassname);
    }
}
