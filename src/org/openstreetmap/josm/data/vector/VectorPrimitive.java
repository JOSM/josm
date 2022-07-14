// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.gui.mappaint.StyleCache;
import org.openstreetmap.josm.tools.Utils;

/**
 * The base class for Vector primitives
 * @author Taylor Smock
 * @since 17862
 */
public abstract class VectorPrimitive extends AbstractPrimitive implements DataLayer<String> {
    private VectorDataSet dataSet;
    private boolean highlighted;
    private StyleCache mappaintStyle;
    private final String layer;

    /**
     * Create a primitive for a specific vector layer
     * @param layer The layer for the primitive
     */
    protected VectorPrimitive(String layer) {
        this.layer = layer;
        this.id = getIdGenerator().generateUniqueId();
    }

    @Override
    protected void keysChangedImpl(Map<String, String> originalKeys) {
        clearCachedStyle();
        if (dataSet != null) {
            for (IPrimitive ref : getReferrers()) {
                ref.clearCachedStyle();
            }
        }
    }

    @Override
    public boolean isHighlighted() {
        return this.highlighted;
    }

    @Override
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    @Override
    public boolean isTagged() {
        return !this.getInterestingTags().isEmpty();
    }

    @Override
    public boolean isAnnotated() {
        return this.getInterestingTags().size() - this.getKeys().size() > 0;
    }

    @Override
    public VectorDataSet getDataSet() {
        return dataSet;
    }

    protected void setDataSet(VectorDataSet newDataSet) {
        dataSet = newDataSet;
    }

    /*----------
     * MAPPAINT
     *--------*/

    @Override
    public final StyleCache getCachedStyle() {
        return mappaintStyle;
    }

    @Override
    public final void setCachedStyle(StyleCache mappaintStyle) {
        this.mappaintStyle = mappaintStyle;
    }

    @Override
    public final boolean isCachedStyleUpToDate() {
        return mappaintStyle != null && mappaintCacheIdx == dataSet.getMappaintCacheIndex();
    }

    @Override
    public final void declareCachedStyleUpToDate() {
        this.mappaintCacheIdx = dataSet.getMappaintCacheIndex();
    }

    @Override
    public boolean hasDirectionKeys() {
        return false;
    }

    @Override
    public boolean reversedDirection() {
        return false;
    }

    /*------------
     * Referrers
     ------------*/
    // Largely the same as OsmPrimitive, OsmPrimitive not modified at this time to avoid breaking binary compatibility

    private Object referrers;

    @Override
    public final List<VectorPrimitive> getReferrers(boolean allowWithoutDataset) {
        if (this.referrers == null) {
            return Collections.emptyList();
        } else if (this.referrers instanceof VectorPrimitive) {
            return Collections.singletonList((VectorPrimitive) this.referrers);
        }
        return referrers(allowWithoutDataset, VectorPrimitive.class)
          .collect(Collectors.toList());
    }

    /**
     * Add new referrer. If referrer is already included then no action is taken
     * @param referrer The referrer to add
     */
    protected void addReferrer(IPrimitive referrer) {
        if (referrers == null) {
            referrers = referrer;
        } else if (referrers instanceof IPrimitive) {
            if (referrers != referrer) {
                referrers = new IPrimitive[] {(IPrimitive) referrers, referrer};
            }
        } else {
            for (IPrimitive primitive:(IPrimitive[]) referrers) {
                if (primitive == referrer)
                    return;
            }
            referrers = Utils.addInArrayCopy((IPrimitive[]) referrers, referrer);
        }
    }

    /**
     * Remove referrer. No action is taken if referrer is not registered
     * @param referrer The referrer to remove
     */
    protected void removeReferrer(IPrimitive referrer) {
        if (referrers instanceof IPrimitive) {
            if (referrers == referrer) {
                referrers = null;
            }
        } else if (referrers instanceof IPrimitive[]) {
            IPrimitive[] orig = (IPrimitive[]) referrers;
            int idx = IntStream.range(0, orig.length)
              .filter(i -> orig[i] == referrer)
              .findFirst().orElse(-1);
            if (idx == -1)
                return;

            if (orig.length == 2) {
                referrers = orig[1-idx]; // idx is either 0 or 1, take the other
            } else { // downsize the array
                IPrimitive[] smaller = new IPrimitive[orig.length-1];
                System.arraycopy(orig, 0, smaller, 0, idx);
                System.arraycopy(orig, idx+1, smaller, idx, smaller.length-idx);
                referrers = smaller;
            }
        }
    }

    private <T extends IPrimitive> Stream<T> referrers(boolean allowWithoutDataset, Class<T> filter) {
        // Returns only referrers that are members of the same dataset (primitive can have some fake references, for example
        // when way is cloned

        if (dataSet == null && !allowWithoutDataset) {
            return Stream.empty();
        }
        if (referrers == null) {
            return Stream.empty();
        }
        final Stream<IPrimitive> stream = referrers instanceof IPrimitive // NOPMD
          ? Stream.of((IPrimitive) referrers)
          : Arrays.stream((IPrimitive[]) referrers);
        return stream
          .filter(p -> p.getDataSet() == dataSet)
          .filter(filter::isInstance)
          .map(filter::cast);
    }

    /**
     * Gets all primitives in the current dataset that reference this primitive.
     * @param filter restrict primitives to subclasses
     * @param <T> type of primitives
     * @return the referrers as Stream
     */
    public final <T extends IPrimitive> Stream<T> referrers(Class<T> filter) {
        return referrers(false, filter);
    }

    @Override
    public void visitReferrers(PrimitiveVisitor visitor) {
        if (visitor != null)
            doVisitReferrers(o -> o.accept(visitor));
    }

    private void doVisitReferrers(Consumer<IPrimitive> visitor) {
        if (this.referrers instanceof IPrimitive) {
            IPrimitive ref = (IPrimitive) this.referrers;
            if (ref.getDataSet() == dataSet) {
                visitor.accept(ref);
            }
        } else if (this.referrers instanceof IPrimitive[]) {
            IPrimitive[] refs = (IPrimitive[]) this.referrers;
            for (IPrimitive ref: refs) {
                if (ref.getDataSet() == dataSet) {
                    visitor.accept(ref);
                }
            }
        }
    }

    /**
     * Set the id of the object
     * @param id The id
     */
    protected void setId(long id) {
        this.id = id;
    }

    /**
     * Make this object disabled
     * @param disabled {@code true} to disable the object
     */
    public void setDisabled(boolean disabled) {
        this.updateFlags(FLAG_DISABLED, disabled);
    }

    /**
     * Make this object visible
     * @param visible {@code true} to make this object visible (default)
     */
    @Override
    public void setVisible(boolean visible) {
        this.updateFlags(FLAG_VISIBLE, visible);
    }

    /**************************
     * Data layer information *
     **************************/
    @Override
    public String getLayer() {
        return this.layer;
    }
}
