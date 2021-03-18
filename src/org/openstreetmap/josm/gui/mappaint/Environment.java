// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.geom.Area;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
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

    /**
     * The name of the default layer. It is used if no layer is specified in the MapCSS rule
     */
    public static final String DEFAULT_LAYER = "default";

    public static class LinkEnvironment extends Environment {

        public LinkEnvironment() {
        }

        public LinkEnvironment(Environment other) {
            super(other);
        }

        public LinkEnvironment(LinkEnvironment other) {
            super(other);
            this.parent = other.parent;
            this.child = other.child;
            this.index = other.index;
            this.count = other.count;
            this.children = other.children == null ? null : new LinkedHashSet<>(other.children);
            this.intersections = other.intersections;
            this.crossingWaysMap = other.crossingWaysMap;
            this.mpAreaCache = other.mpAreaCache;
        }

        /**
         * If not null, this is the matching parent object if a condition or an expression
         * is evaluated in a {@link LinkSelector} (within a child selector)
         * Implies that {@link #child} is null.
         */
        public IPrimitive parent;

        /**
         * If not null, this is the matching child object if a condition or an expression
         * is evaluated in a {@link LinkSelector} (within a child selector)
         * Implies that {@link #parent} is null.
         */
        public IPrimitive child;

        /**
         * index of node in parent way or member in parent relation. Must be >=0 context.
         */
        public int index = -1;

        /**
         * count of nodes in parent way or members in parent relation. Must be >=0 context.
         */
        public int count = -1;

        /**
         * Set of matched children filled by ContainsFinder and CrossingFinder, null if nothing matched
         */
        public Set<IPrimitive> children;

        /**
         * Crossing ways result from CrossingFinder, filled for incomplete ways/relations
         */
        public Map<IPrimitive, Map<List<Way>, List<WaySegment>>> crossingWaysMap;

        /**
         * Intersection areas (only filled with CrossingFinder if children is not null)
         */
        public Map<IPrimitive, Area> intersections;

        /**
         * Cache for multipolygon areas, can be null, used with CrossingFinder
         */
        public Map<IPrimitive, Area> mpAreaCache;

        @Override
        public Context getContext() {
            return Context.LINK;
        }

        @Override
        public boolean isLinkContext() {
            return true;
        }

        /**
         * Gets the role of the matching primitive in the relation
         * @return The role
         */
        @Override
        public String getRole() {
            if (parent instanceof Relation)
                return ((Relation) parent).getMember(index).getRole();
            if (child != null && osm instanceof Relation)
                return ((Relation) osm).getMember(index).getRole();
            return null;
        }

        @Override
        public LinkEnvironment clearSelectorMatchingInformation() {
            parent = null;
            child = null;
            index = -1;
            count = -1;
            children = null;
            intersections = null;
            crossingWaysMap = null;
            return this;
        }
    }

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
        this.source = other.source;
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
     * @see LinkEnvironment#parent
     * @since 13810 (signature)
     */
    public LinkEnvironment withParent(IPrimitive parent) {
        LinkEnvironment e = new LinkEnvironment(this);
        e.parent = parent;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified parent, index, and context set to {@link Context#LINK}.
     * @param parent the matching parent object
     * @param index index of node in parent way or member in parent relation
     * @param count count of nodes in parent way or members in parent relation
     * @return A clone of this environment, with the specified parent, index, and context set to {@link Context#LINK}
     * @see LinkEnvironment#parent
     * @see LinkEnvironment#index
     * @since 6175
     * @since 13810 (signature)
     */
    public LinkEnvironment withParentAndIndexAndLinkContext(IPrimitive parent, int index, int count) {
        LinkEnvironment e = new LinkEnvironment(this);
        e.parent = parent;
        e.index = index;
        e.count = count;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified child.
     * @param child the matching child object
     * @return A clone of this environment, with the specified child
     * @see LinkEnvironment#child
     * @since 13810 (signature)
     */
    public LinkEnvironment withChild(IPrimitive child) {
        LinkEnvironment e = new LinkEnvironment(this);
        e.child = child;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified child, index, and context set to {@link Context#LINK}.
     * @param child the matching child object
     * @param index index of node in parent way or member in parent relation
     * @param count count of nodes in parent way or members in parent relation
     * @return A clone of this environment, with the specified child, index, and context set to {@code Context#LINK}
     * @see LinkEnvironment#child
     * @see LinkEnvironment#index
     * @since 6175
     * @since 13810 (signature)
     */
    public LinkEnvironment withChildAndIndexAndLinkContext(IPrimitive child, int index, int count) {
        LinkEnvironment e = new LinkEnvironment(this);
        e.child = child;
        e.index = index;
        e.count = count;
        return e;
    }

    /**
     * Creates a clone of this environment, with the specified index.
     * @param index index of node in parent way or member in parent relation
     * @param count count of nodes in parent way or members in parent relation
     * @return A clone of this environment, with the specified index
     * @see LinkEnvironment#index
     */
    public LinkEnvironment withIndex(int index, int count) {
        LinkEnvironment e = new LinkEnvironment(this);
        e.index = index;
        e.count = count;
        return e;
    }

    /**
     * Creates a clone of this environment, with context set to {@link Context#LINK}.
     * @return A clone of this environment, with context set to {@code Context#LINK}
     */
    public LinkEnvironment withLinkContext() {
        return new LinkEnvironment(this);
    }

    /**
     * Determines if the context of this environment is {@link Context#LINK}.
     * @return {@code true} if the context of this environment is {@code Context#LINK}, {@code false} otherwise
     */
    public boolean isLinkContext() {
        return false;
    }

    /**
     * Replies the current context.
     *
     * @return the current context
     */
    public Context getContext() {
        return Context.PRIMITIVE;
    }

    /**
     * Gets the role of the matching primitive in the relation
     * @return The role
     */
    public String getRole() {
        return null;
    }

    /**
     * Clears all matching context information
     * @return this
     */
    public Environment clearSelectorMatchingInformation() {
        return this;
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
