// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.LinkSelector;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Environment is a data object to provide access to various "global" parameters.
 * It is used during processing of MapCSS rules and for the generation of
 * style elements.
 */
public class Environment {

    /**
     * The primitive that is currently evaluated
     */
    public IPrimitive osm;

    /**
     * The cascades that are currently evaluated
     */
    public MultiCascade mc;
    /**
     * The current MapCSS layer
     */
    public String layer;
    /**
     * The style source that is evaluated
     */
    public StyleSource source;
    private Context context = Context.PRIMITIVE;

    /**
     * The name of the default layer. It is used if no layer is specified in the MapCSS rule
     */
    public static final String DEFAULT_LAYER = "default";

    /**
     * If not null, this is the matching parent object if a condition or an expression
     * is evaluated in a {@link LinkSelector} (within a child selector)
     */
    public IPrimitive parent;

    /**
     * The same for parent selector. Only one of the 2 fields (parent or child) is not null in any environment.
     */
    public IPrimitive child;

    /**
     * index of node in parent way or member in parent relation. Must be != null in LINK context.
     */
    public Integer index;

    /**
     * count of nodes in parent way or members in parent relation. Must be != null in LINK context.
     */
    public Integer count;

    /**
     * Creates a new uninitialized environment.
     */
    public Environment() {
        // environment can be initialized later through with* methods
    }

    /**
     * Creates a new environment.
     * @param osm OSM primitive
     * @since 8415
     * @since 13810 (signature)
     */
    public Environment(IPrimitive osm) {
        this.osm = osm;
    }

    /**
     * Creates a new environment.
     * @param osm OSM primitive
     * @param mc multi cascade
     * @param layer layer
     * @param source style source
     * @since 13810 (signature)
     */
    public Environment(IPrimitive osm, MultiCascade mc, String layer, StyleSource source) {
        this.osm = osm;
        this.mc = mc;
        this.layer = layer;
        this.source = source;
    }

    /**
     * Creates a clone of the environment {@code other}.
     *
     * @param other the other environment. Must not be null.
     * @throws IllegalArgumentException if {@code param} is {@code null}
     */
    public Environment(Environment other) {
        CheckParameterUtil.ensureParameterNotNull(other);
        this.osm = other.osm;
        this.mc = other.mc;
        this.layer = other.layer;
        this.parent = other.parent;
        this.child = other.child;
        this.source = other.source;
        this.index = other.index;
        this.count = other.count;
        this.context = other.getContext();
    }

    /**
     * Creates a clone of this environment, with the specified primitive.
     * @param osm OSM primitive
     * @return A clone of this environment, with the specified primitive
     * @see #osm
     * @since 13810 (signature)
     */
    public Environment withPrimitive(IPrimitive osm) {
        Environment e = new Environment(this);
        e.osm = osm;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified parent.
     * @param parent the matching parent object
     * @return A clone of this environment, with the specified parent
     * @see #parent
     * @since 13810 (signature)
     */
    public Environment withParent(IPrimitive parent) {
        Environment e = new Environment(this);
        e.parent = parent;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified parent, index, and context set to {@link Context#LINK}.
     * @param parent the matching parent object
     * @param index index of node in parent way or member in parent relation
     * @param count count of nodes in parent way or members in parent relation
     * @return A clone of this environment, with the specified parent, index, and context set to {@link Context#LINK}
     * @see #parent
     * @see #index
     * @since 6175
     * @since 13810 (signature)
     */
    public Environment withParentAndIndexAndLinkContext(IPrimitive parent, int index, int count) {
        Environment e = new Environment(this);
        e.parent = parent;
        e.index = index;
        e.count = count;
        e.context = Context.LINK;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified child.
     * @param child the matching child object
     * @return A clone of this environment, with the specified child
     * @see #child
     * @since 13810 (signature)
     */
    public Environment withChild(IPrimitive child) {
        Environment e = new Environment(this);
        e.child = child;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified child, index, and context set to {@link Context#LINK}.
     * @param child the matching child object
     * @param index index of node in parent way or member in parent relation
     * @param count count of nodes in parent way or members in parent relation
     * @return A clone of this environment, with the specified child, index, and context set to {@code Context#LINK}
     * @see #child
     * @see #index
     * @since 6175
     * @since 13810 (signature)
     */
    public Environment withChildAndIndexAndLinkContext(IPrimitive child, int index, int count) {
        Environment e = new Environment(this);
        e.child = child;
        e.index = index;
        e.count = count;
        e.context = Context.LINK;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified index.
     * @param index index of node in parent way or member in parent relation
     * @param count count of nodes in parent way or members in parent relation
     * @return A clone of this environment, with the specified index
     * @see #index
     */
    public Environment withIndex(int index, int count) {
        Environment e = new Environment(this);
        e.index = index;
        e.count = count;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified {@link Context}.
     * @param context context
     * @return A clone of this environment, with the specified {@code Context}
     */
    public Environment withContext(Context context) {
        Environment e = new Environment(this);
        e.context = context == null ? Context.PRIMITIVE : context;
        return e;
    }

    /**
     * Creates a clone of this environment, with context set to {@link Context#LINK}.
     * @return A clone of this environment, with context set to {@code Context#LINK}
     */
    public Environment withLinkContext() {
        Environment e = new Environment(this);
        e.context = Context.LINK;
        return e;
    }

    /**
     * Determines if the context of this environment is {@link Context#LINK}.
     * @return {@code true} if the context of this environment is {@code Context#LINK}, {@code false} otherwise
     */
    public boolean isLinkContext() {
        return Context.LINK.equals(context);
    }

    /**
     * Determines if this environment has a relation as parent.
     * @return {@code true} if this environment has a relation as parent, {@code false} otherwise
     * @see #parent
     */
    public boolean hasParentRelation() {
        return parent instanceof Relation;
    }

    /**
     * Replies the current context.
     *
     * @return the current context
     */
    public Context getContext() {
        return context == null ? Context.PRIMITIVE : context;
    }

    /**
     * Gets the role of the matching primitive in the relation
     * @return The role
     */
    public String getRole() {
        if (getContext().equals(Context.PRIMITIVE))
            return null;

        if (parent instanceof Relation)
            return ((Relation) parent).getMember(index).getRole();
        if (child != null && osm instanceof Relation)
            return ((Relation) osm).getMember(index).getRole();
        return null;
    }

    /**
     * Clears all matching context information
     */
    public void clearSelectorMatchingInformation() {
        parent = null;
        child = null;
        index = null;
        count = null;
    }

    /**
     * Gets the current cascade for a given layer
     * @param layer The layer to use, <code>null</code> to use the layer of the {@link Environment}
     * @return The cascade
     */
    public Cascade getCascade(String layer) {
        return mc == null ? null : mc.getCascade(layer == null ? this.layer : layer);
    }
}
