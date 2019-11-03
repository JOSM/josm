// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Objects;
import java.util.Optional;

import org.openstreetmap.josm.data.gpx.GpxData.XMLNamespace;
import org.xml.sax.Attributes;

/**
 * A GpxExtension that has attributes and child extensions (implements {@link IWithAttributes} and {@link GpxConstants}).
 * @since 15496
 */
public class GpxExtension extends WithAttributes {
    private final String qualifiedName, prefix, key;
    private IWithAttributes parent;
    private String value;
    private boolean visible = true;

    /**
     * Constructs a new {@link GpxExtension}.
     * @param prefix the prefix
     * @param key the key
     * @param value the value
     */
    public GpxExtension(String prefix, String key, String value) {
        this.prefix = Optional.ofNullable(prefix).orElse("");
        this.key = key;
        this.value = value;
        this.qualifiedName = (this.prefix.isEmpty() ? "" : this.prefix + ":") + key;
    }

    /**
     * Creates a new {@link GpxExtension}
     *
     * @param namespaceURI the URI of the XML namespace, used to determine supported extensions
     *                     (josm, gpxx, gpxd) regardless of the prefix that could legally vary from file to file.
     * @param qName the qualified name of the XML element including prefix
     * @param atts the attributes
     */
    public GpxExtension(String namespaceURI, String qName, Attributes atts) {
        qualifiedName = qName;
        int dot = qName.indexOf(':');
        String p = findPrefix(namespaceURI);
        if (p == null) {
            if (dot != -1) {
                prefix = qName.substring(0, dot);
            } else {
                prefix = "";
            }
        } else {
            prefix = p;
        }
        key = qName.substring(dot + 1);
        for (int i = 0; i < atts.getLength(); i++) {
            attr.put(atts.getLocalName(i), atts.getValue(i));
        }
    }

    /**
     * Finds the default prefix used by JOSM for the given namespaceURI as the document is free specify another one.
     * @param namespaceURI namespace URI
     * @return the prefix
     */
    public static String findPrefix(String namespaceURI) {
        if (XML_URI_EXTENSIONS_DRAWING.equals(namespaceURI))
            return "gpxd";

        if (XML_URI_EXTENSIONS_GARMIN.equals(namespaceURI))
            return "gpxx";

        if (XML_URI_EXTENSIONS_JOSM.equals(namespaceURI))
            return "josm";

        return null;
    }

    /**
     * Finds the namespace for the given default prefix, if supported with schema location
     * @param prefix the prefix used by JOSM
     * @return the {@link XMLNamespace} element, location and URI can be <code>null</code> if not found.
     */
    public static XMLNamespace findNamespace(String prefix) {
        switch (prefix) {
        case "gpxx":
            return new XMLNamespace("gpxx", XML_URI_EXTENSIONS_GARMIN, XML_XSD_EXTENSIONS_GARMIN);
        case "gpxd":
            return new XMLNamespace("gpxd", XML_URI_EXTENSIONS_DRAWING, XML_XSD_EXTENSIONS_DRAWING);
        case "josm":
            return new XMLNamespace("josm", XML_URI_EXTENSIONS_JOSM, XML_XSD_EXTENSIONS_JOSM);
        }
        return null;
    }

    /**
     * @return the qualified name of the XML element
     */
    public String getQualifiedName() {
        return qualifiedName;
    }

    /**
     * @return the prefix of the XML namespace
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return the key (local element name) of the extension
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the flattened extension key of this extension, used for conversion to OSM layers
     */
    public String getFlatKey() {
        String ret = "";
        if (parent != null && parent instanceof GpxExtension) {
            GpxExtension ext = (GpxExtension) parent;
            ret = ext.getFlatKey() + ":";
        }
        return ret + getKey();
    }

    /**
     * Searches recursively for the extension with the given key in all children
     * @param sPrefix the prefix to look for
     * @param sKey the key to look for
     * @return the extension if found, otherwise <code>null</code>
     */
    public GpxExtension findExtension(String sPrefix, String sKey) {
        if (prefix.equalsIgnoreCase(sPrefix) && key.equalsIgnoreCase(sKey)) {
            return this;
        } else {
            for (GpxExtension child : getExtensions()) {
                GpxExtension ext = child.findExtension(sPrefix, sKey);
                if (ext != null) {
                    return ext;
                }
            }
            return null;
        }
    }

    /**
     * @return the value of the extension
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Removes this extension from its parent and all then-empty parents
     * @throws IllegalStateException if parent not set
     */
    public void remove() {
        if (parent == null)
            throw new IllegalStateException("Extension " + qualifiedName + " has no parent, can't remove it.");

        parent.getExtensions().remove(this);
        if (parent instanceof GpxExtension) {
            GpxExtension gpx = ((GpxExtension) parent);
            if ((gpx.getValue() == null || gpx.getValue().trim().isEmpty())
                    && gpx.getAttributes().isEmpty()
                    && gpx.getExtensions().isEmpty()) {
                gpx.remove();
            }
        }
    }

    /**
     * Hides this extension and all then-empty parents so it isn't written
     * @see #isVisible()
     */
    public void hide() {
        visible = false;
        if (parent != null && parent instanceof GpxExtension) {
            GpxExtension gpx = (GpxExtension) parent;
            if ((gpx.getValue() == null || gpx.getValue().trim().isEmpty())
                    && gpx.getAttributes().isEmpty()
                    && !gpx.getExtensions().isVisible()) {
                gpx.hide();
            }
        }
    }

    /**
     * Shows this extension and all parents so it can be written
     * @see #isVisible()
     */
    public void show() {
        visible = true;
        if (parent != null && parent instanceof GpxExtension) {
            ((GpxExtension) parent).show();
        }
    }

    /**
     * @return if this extension should be written, used for hiding colors during export without removing them
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @return the parent element of this extension, can be another extension or gpx elements (data, track, segment, ...)
     */
    public IWithAttributes getParent() {
        return parent;
    }

    /**
     * Sets the parent for this extension
     * @param parent the parent
     * @throws IllegalStateException if parent already set
     */
    public void setParent(IWithAttributes parent) {
        if (this.parent != null)
            throw new IllegalStateException("Parent of extension " + qualifiedName + " is already set");

        this.parent = parent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, key, value, attr, parent, visible, super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof GpxExtension))
            return false;
        GpxExtension other = (GpxExtension) obj;
        if (visible != other.visible)
            return false;
        if (prefix == null) {
            if (other.prefix != null)
                return false;
        } else if (!prefix.equals(other.prefix))
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        if (attr == null) {
            if (other.attr != null)
                return false;
        } else if (!attr.equals(other.attr))
            return false;
        if (parent == null) {
            if (other.parent != null)
                return false;
        } else if (!parent.equals(other.parent))
            return false;
        return true;
    }
}
