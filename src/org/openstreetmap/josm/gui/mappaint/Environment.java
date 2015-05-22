// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
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

    public OsmPrimitive osm;

    public MultiCascade mc;
    public String layer;
    public StyleSource source;
    private Context context = Context.PRIMITIVE;
    public static final String DEFAULT_LAYER = "default";

    /**
     * If not null, this is the matching parent object if a condition or an expression
     * is evaluated in a {@link LinkSelector} (within a child selector)
     */
    public OsmPrimitive parent;

    /**
     * The same for parent selector. Only one of the 2 fields (parent or child) is not null in any environment.
     */
    public OsmPrimitive child;

    /**
     * index of node in parent way or member in parent relation. Must be != null in LINK context.
     */
    public Integer index = null;

    /**
     * count of nodes in parent way or members in parent relation. Must be != null in LINK context.
     */
    public Integer count = null;

    /**
     * Creates a new uninitialized environment.
     */
    public Environment() {
        // environment can be initialized later through with* methods
    }

    /**
     * Creates a new environment.
     * @since 8415
     */
    public Environment(OsmPrimitive osm) {
        this.osm = osm;
    }

    /**
     * Creates a new environment.
     */
    public Environment(OsmPrimitive osm, MultiCascade mc, String layer, StyleSource source) {
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
     * @return A clone of this environment, with the specified primitive
     * @see #osm
     */
    public Environment withPrimitive(OsmPrimitive osm) {
        Environment e = new Environment(this);
        e.osm = osm;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified parent.
     * @param parent the matching parent object
     * @return A clone of this environment, with the specified parent
     * @see #parent
     */
    public Environment withParent(OsmPrimitive parent) {
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
     */
    public Environment withParentAndIndexAndLinkContext(OsmPrimitive parent, int index, int count) {
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
     */
    public Environment withChild(OsmPrimitive child) {
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
     * @since 6175
     * @see #child
     * @see #index
     */
    public Environment withChildAndIndexAndLinkContext(OsmPrimitive child, int index, int count) {
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

    public String getRole() {
        if (getContext().equals(Context.PRIMITIVE))
            return null;

        if (parent instanceof Relation)
            return ((Relation) parent).getMember(index).getRole();
        if (child != null && osm instanceof Relation)
            return ((Relation) osm).getMember(index).getRole();
        return null;
    }

    public void clearSelectorMatchingInformation() {
        parent = null;
        child = null;
        index = null;
        count = null;
    }

    public Cascade getCascade(String layer) {
        return mc == null ? null : mc.getCascade(layer == null ? this.layer : layer);
    }
}
