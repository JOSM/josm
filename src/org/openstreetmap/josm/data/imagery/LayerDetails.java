// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.tools.Utils;

/**
 * The details of a layer of this WMS server.
 */
public class LayerDetails {
    private final Map<String, String> styles = new ConcurrentHashMap<>(); // name -> title
    private final Collection<String> crs = new ArrayList<>();
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
    private final LayerDetails parentLayer;
    private Bounds bounds;
    private List<LayerDetails> children = new ArrayList<>();

    /**
     * Constructor pointing to parent layer. Set to null if this is topmost layer.
     * This is needed to properly handle layer attributes inheritance.
     *
     * @param parentLayer parent layer
     */
    public LayerDetails(LayerDetails parentLayer) {
        this.parentLayer = parentLayer;
    }

    /**
     * Returns projections that are supported by this layer.
     * @return projections that are supported by this layer
     */
    public Collection<String> getCrs() {
        Collection<String> ret = new ArrayList<>();
        if (parentLayer != null) {
            ret.addAll(parentLayer.getCrs());
        }
        ret.addAll(crs);
        return ret;
    }

    /**
     * Returns styles defined for this layer.
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
     * Returns "Human readable" title of this layer
     * @return "Human readable" title of this layer
     * @see LayerDetails#getName()
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets title of this layer
     * @param title title of this layer
     * @see LayerDetails#getName()
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *
     * Citation from OGC WMS specification (WMS 1.3.0):<p><i>
     * A number of elements have both a {@literal <Name>} and a {@literal <Title>}. The Name is a text string used for machine-to-machine
     * communication while the Title is for the benefit of humans. For example, a dataset might have the descriptive Title
     * “Maximum Atmospheric Temperature” and be requested using the abbreviated Name “ATMAX”.</i></p>
     *
     * And second citation:<p><i>
     * If, and only if, a layer has a {@literal <Name>}, then it is a map layer that can be requested by using that Name in the
     * LAYERS parameter of a GetMap request. A Layer that contains a {@literal <Name>} element is referred to as a “named
     * layer” in this International Standard. If the layer has a Title but no Name, then that layer is only a category title for
     * all the layers nested within.</i></p>
     * @return name of this layer
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this Layer.
     * @param name the name of this Layer
     * @see LayerDetails#getName()
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
        this.styles.put(name, title == null ? "" : title);
    }

    /**
     * Add projection supported by this layer
     * @param crs projection code
     */
    public void addCrs(String crs) {
        this.crs.add(crs);
    }

    /**
     * Returns bounds within layer might be queried.
     * @return bounds within layer might be queried
     */
    public Bounds getBounds() {
        return bounds;
    }

    /**
     * Sets bounds of this layer
     * @param bounds of this layer
     */
    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public String toString() {
        String baseName = Utils.isEmpty(title) ? name : title;
        return abstr == null || abstr.equalsIgnoreCase(baseName) ? baseName : baseName + " (" + abstr + ')';
    }

    /**
     * Returns parent layer for this layer.
     * @return parent layer for this layer
     */
    public LayerDetails getParent() {
        return parentLayer;
    }

    /**
     * sets children layers for this layer
     * @param children children of this layer
     */
    public void setChildren(List<LayerDetails> children) {
        this.children = children;

    }

    /**
     * Returns children layers of this layer.
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
        return !Utils.isEmpty(name);
    }

    /**
     * Returns abstract of this layer.
     * @return "Narrative description of the layer"
     */
    public String getAbstract() {
        return abstr;
    }

    /**
     * Sets abstract of this layer
     * @param abstr abstract of this layer
     */
    public void setAbstract(String abstr) {
        this.abstr = abstr;
    }

    /**
     * Returns flattened stream of this layer and its children.
     * @return flattened stream of this layer and its children (as well as recursively children of its children)
     */
    public Stream<LayerDetails> flattened() {
        return Stream.concat(
                Stream.of(this),
                getChildren().stream().flatMap(LayerDetails::flattened)
                );
    }
}
