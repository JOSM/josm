package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.Utils.equal;

/**
 * A source entry primarily used to save the user's selection of mappaint styles, 
 * but also for preset sources.
 */
public class SourceEntry {

    /**
     *  A URL can be anything that MirroredInputStream understands, i.e.
     *  a local file, http://, or a file from the current jar
     */
    public String url;

    /**
     *  Name is used as a namespace for color preferences and (currently) only
     *  one file with a name can be loaded at a time. Additional styles must
     *  either have the same name as the main style or no name at all.
     *  If no name is provided, it will be set to the default value "standard".
     *  The name can also be given in the xml file as attribute for the rules tag.
     *  (This overrides the name given in the preferences, otherwise both
     *  methods are equivalent.)
     */
    public String name;

    /**
     * A short description that can be used as menu entry.
     */
    public String shortdescription;

    /**
     * active is a boolean flag that can be used to turn the style on or off
     * at runtime.
     */
    public boolean active;

    public SourceEntry(String url, String name, String shortdescription, Boolean active) {
        this.url = url;
        this.name = equal(name, "") ? null : name;
        this.shortdescription = equal(shortdescription, "") ? null : shortdescription;
        this.active = active;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        final SourceEntry other = (SourceEntry) obj;
        return equal(other.url, url) && 
                equal(other.name, name) &&
                equal(other.shortdescription, shortdescription) &&
                other.active == active;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + (this.url != null ? this.url.hashCode() : 0);
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 89 * hash + (this.shortdescription != null ? this.shortdescription.hashCode() : 0);
        hash = 89 * hash + (this.active ? 1 : 0);
        return hash;
    }

    @Override
    public String toString() {
        return shortdescription != null ? shortdescription : url;
    }


}
