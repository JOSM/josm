// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.jcs.access.exception.InvalidArgumentException;
import org.openstreetmap.josm.io.GpxReader;
import org.xml.sax.Attributes;

/**
 * Class extending <code>ArrayList&lt;GpxExtension&gt;</code>.
 * Can be used to collect {@link GpxExtension}s while reading GPX files, see {@link GpxReader}
 * @since 15496
 */
public class GpxExtensionCollection extends ArrayList<GpxExtension> {

    private static final long serialVersionUID = 1L;

    private Stack<GpxExtension> childStack = new Stack<>();
    private IWithAttributes parent;

    /**
     * Constructs a new {@link GpxExtensionCollection}
     */
    public GpxExtensionCollection() {}

    /**
     * Constructs a new {@link GpxExtensionCollection} with the given parent
     * @param parent the parent extending {@link IWithAttributes}
     */
    public GpxExtensionCollection(IWithAttributes parent) {
        this.parent = parent;
    }

    /**
     * Adds a child extension to the last extension and pushes it to the stack.
     * @param namespaceURI the URI of the XML namespace, used to determine supported
     *                     extensions (josm, gpxx, gpxd) regardless of the prefix.
     * @param qName the qualified name of the XML element including prefix
     * @param atts the attributes
     */
    public void openChild(String namespaceURI, String qName, Attributes atts) {
        GpxExtension child = new GpxExtension(namespaceURI, qName, atts);
        if (!childStack.isEmpty()) {
            childStack.lastElement().getExtensions().add(child);
        } else {
            this.add(child);
        }
        childStack.add(child);
    }

    /**
     * Sets the value for the last child and pops it from the stack, so the next one will be added to its parent.
     * The qualified name is verified.
     * @param qName the qualified name
     * @param value the value
     */
    public void closeChild(String qName, String value) {
        if (childStack.isEmpty())
            throw new InvalidArgumentException("Can't close child " + qName + ", no element in stack.");

        GpxExtension child = childStack.pop();

        String childQN = child.getQualifiedName();

        if (!childQN.equals(qName))
            throw new InvalidArgumentException("Can't close child " + qName + ", must close " + childQN + " first.");

        child.setValue(value);
    }

    @Override
    public boolean add(GpxExtension gpx) {
        gpx.setParent(parent);
        return super.add(gpx);
    }

    /**
     * Creates and adds a new {@link GpxExtension} from the given parameters.
     * @param prefix the prefix
     * @param key the key/tag
     * @return the added GpxExtension
     */
    public GpxExtension add(String prefix, String key) {
        return add(prefix, key, null);
    }

    /**
     * Creates and adds a new {@link GpxExtension} from the given parameters.
     * @param prefix the prefix
     * @param key the key/tag
     * @param value the value, can be <code>null</code>
     * @return the added GpxExtension
     */
    public GpxExtension add(String prefix, String key, String value) {
        GpxExtension gpx = new GpxExtension(prefix, key, value);
        add(gpx);
        return gpx;
    }

    /**
     * Creates and adds a new {@link GpxExtension}, if it hasn't been added yet. Shows it if it has.
     * @param prefix the prefix
     * @param key the key/tag
     * @return the added or found GpxExtension
     * @see GpxExtension#show()
     */
    public GpxExtension addIfNotPresent(String prefix, String key) {
        GpxExtension gpx = get(prefix, key);
        if (gpx != null) {
            gpx.show();
            return gpx;
        }
        return add(prefix, key);
    }

    /**
     * Creates and adds a new {@link GpxExtension} or updates its value and shows it if already present.
     * @param prefix the prefix
     * @param key the key/tag
     * @param value the value
     * @return the added or found GpxExtension
     * @see GpxExtension#show()
     */
    public GpxExtension addOrUpdate(String prefix, String key, String value) {
        GpxExtension gpx = get(prefix, key);
        if (gpx != null) {
            gpx.show();
            gpx.setValue(value);
            return gpx;
        } else {
            return add(prefix, key, value);
        }
    }

    @Override
    public boolean addAll(Collection<? extends GpxExtension> extensions) {
        extensions.forEach(e -> e.setParent(parent));
        return super.addAll(extensions);
    }

    /**
     * Adds an extension from a flat chain without prefix, e.g. when converting from OSM
     * @param chain the full key chain, e.g. ["extension", "gpxx", "TrackExtensions", "DisplayColor"]
     * @param value the value
     */
    public void addFlat(String[] chain, String value) {
        if (chain.length >= 3 && "extension".equals(chain[0])) {
            String prefix = "other".equals(chain[1]) ? "" : chain[1];
            GpxExtensionCollection previous = this;
            for (int i = 2; i < chain.length; i++) {
                if (i != 2 || !"segment".equals(chain[2])) {
                    previous = previous.add(prefix, chain[i], i == chain.length - 1 ? value : null).getExtensions();
                }
            }
        }
    }

    /**
     * Gets the extension with the given prefix and key
     * @param prefix the prefix
     * @param key the key/tag
     * @return the {@link GpxExtension} if found or <code>null</code>
     */
    public GpxExtension get(String prefix, String key) {
        return stream(prefix, key).findAny().orElse(null);
    }

    /**
     * Gets all extensions with the given prefix and key
     * @param prefix the prefix
     * @param key the key/tag
     * @return a {@link GpxExtensionCollection} with the extensions, empty collection if none found
     */
    public GpxExtensionCollection getAll(String prefix, String key) {
        GpxExtensionCollection copy = new GpxExtensionCollection(this.parent);
        copy.addAll(stream(prefix, key).collect(Collectors.toList()));
        return copy;
    }

    /**
     * Gets a stream with all extensions with the given prefix and key
     * @param prefix the prefix
     * @param key the key/tag
     * @return the <code>Stream&lt;{@link GpxExtension}&gt;</code>
     */
    public Stream<GpxExtension> stream(String prefix, String key) {
        return stream().filter(e -> Objects.equals(prefix, e.getPrefix()) && Objects.equals(key, e.getKey()));
    }

    /**
     * Searches recursively for the extension with the given prefix and key in all children
     * @param prefix the prefix to look for
     * @param key the key to look for
     * @return the extension if found, otherwise <code>null</code>
     */
    public GpxExtension find(String prefix, String key) {
        for (GpxExtension child : this) {
            GpxExtension ext = child.findExtension(prefix, key);
            if (ext != null) {
                return ext;
            }
        }
        return null;
    }

    /**
     * Searches and removes recursively all extensions with the given prefix and key in all children
     * @param prefix the prefix to look for
     * @param key the key to look for
      */
    public void findAndRemove(String prefix, String key) {
        Optional.ofNullable(find(prefix, key)).ifPresent(GpxExtension::remove);
    }

    /**
     * Removes all {@link GpxExtension}s with the given prefix and key in direct children
     * @param prefix the prefix
     * @param key the key/tag
     */
    public void remove(String prefix, String key) {
        stream(prefix, key)
        .collect(Collectors.toList()) //needs to be collected to avoid concurrent modification
        .forEach(e -> super.remove(e));
    }

    /**
     * Removes all extensions with the given prefix in direct children
     * @param prefix the prefix
     */
    public void removeAllWithPrefix(String prefix) {
        stream()
        .filter(e -> Objects.equals(prefix, e.getPrefix()))
        .collect(Collectors.toList()) //needs to be collected to avoid concurrent modification
        .forEach(e -> super.remove(e));
    }

    /**
     * Gets all prefixes of direct (writable) children
     * @return stream with the prefixes
     */
    public Stream<String> getPrefixesStream() {
        return stream()
                .filter(GpxExtension::isVisible)
                .map(GpxExtension::getPrefix)
                .distinct();
    }

    /**
     * @return <code>true</code> if this collection contains writable extensions
     */
    public boolean isVisible() {
        return stream().anyMatch(GpxExtension::isVisible);
    }

    @Override
    public void clear() {
        childStack.clear();
        super.clear();
    }

}
