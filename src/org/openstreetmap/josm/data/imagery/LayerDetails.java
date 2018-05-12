// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.Bounds;

/**
 * The details of a layer of this WMS server.
 */
public class LayerDetails {
    private Map<String, String> styles = new ConcurrentHashMap<>(); // name -> title
    private Collection<String> crs = new ArrayList<>();
    /**
     * The layer name (WMS {@code Title})
     */
    private String title;
    /**
     * The layer name (WMS {@code Name})
     */
    private String name;
    /**
     * The layer abstract (WMS {@code Abstract})
     * @since 13199
     */
    private String abstr;
    private LayerDetails parentLayer;
    private Bounds bounds;
    private List<LayerDetails> children = new ArrayList<>();

    /**
     * Constructor pointing to parent layer. Set to null if this is topmost layer.
     * This is needed to properly handle layer attributes inheritance.
     *
     * @param parentLayer
     */
    public LayerDetails(LayerDetails parentLayer) {
        this.parentLayer = parentLayer;
    }

    /**
     * @return projections that are supported by this layer
     */
    public Collection<String> getCrs() {
        Collection<String> ret = new ArrayList<>();
        if (parentLayer != null) {
            ret.addAll(parentLayer.getCrs());
        }
        ret.addAll(crs);
        return crs;
    }

    /**
     *
     * @return styles defined for this layer
     */
    public Map<String, String> getStyles() {
        Map<String, String> ret = new ConcurrentHashMap<>();
        if (parentLayer != null) {
            ret.putAll(parentLayer.getStyles());
        }
        ret.putAll(styles);
        return ret;
    }

    /**
     * @see LayerDetails#getName()
     * @return title "Human readable" title of this layer
     */
    public String getTitle() {
        return title;
    }

    /**
     * @see LayerDetails#getName()
     * @param title set title of this layer
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *
     * Citation from OGC WMS specification (WMS 1.3.0):
     * > A number of elements have both a <Name> and a <Title>. The Name is a text string used for machine-to-machine
     * > communication while the Title is for the benefit of humans. For example, a dataset might have the descriptive Title
     * > “Maximum Atmospheric Temperature” and be requested using the abbreviated Name “ATMAX”.
     *
     * And second citation:
     * > If, and only if, a layer has a <Name>, then it is a map layer that can be requested by using that Name in the
     * > LAYERS parameter of a GetMap request. A Layer that contains a <Name> element is referred to as a “named
     * > layer” in this International Standard. If the layer has a Title but no Name, then that layer is only a category title for
     * > all the layers nested within.
     * @return name of this layer
     */
    public String getName() {
        return name;
    }

    /**
     * @see LayerDetails#getName()
     * @param name sets the name of this Layer
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Add style to list of styles defined by this layer
     * @param name machine-to-machine name of this style
     * @param title human readable title of this style
     */
    public void addStyle(String name, String title) {
        this.styles.put(name, title);
    }

    /**
     * Add projection supported by this layer
     * @param crs projection code
     */
    public void addCrs(String crs) {
        this.crs.add(crs);
    }

    /**
     *
     * @return bounds within layer might be queried
     */
    public Bounds getBounds() {
        return bounds;
    }

    /**
     * sets bounds of this layer
     * @param bounds
     */
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public String toString() {
        String baseName = (title == null || title.isEmpty()) ? name : title;
        return abstr == null || abstr.equalsIgnoreCase(baseName) ? baseName : baseName + " (" + abstr + ')';
    }

    /**
     *
     * @return parent layer for his layer
     */
    public LayerDetails getParent() {
        return parentLayer;
    }

    /**
     * sets children layers for this layer
     * @param children
     */
    public void setChildren(List<LayerDetails> children) {
        this.children = children;

    }

    /**
     *
     * @return children layers of this layer
     */
    public List<LayerDetails> getChildren() {
        return children;
    }

    /**
     * if user may select this layer (is it possible to request it from server)
     * @return true if user may select this layer, false if this layer is only grouping other layers
     */
    public boolean isSelectable() {
        return !(name == null || name.isEmpty());
    }

    /**
     * @return "Narrative description of the layer"
     */
    public String getAbstract() {
        return abstr;
    }

    /**
     * Sets abstract of this layer
     * @param abstr
     */
    public void setAbstract(String abstr) {
        this.abstr = abstr;
    }

    /**
     * @return flattened stream of this layer and its children (as well as recursively children of its children)
     */
    public Stream<LayerDetails> flattened() {
        return Stream.concat(
                Stream.of(this),
                getChildren().stream().flatMap(LayerDetails::flattened)
                );
    }
}
