// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Registry for a single, global projection instance.
 * @since 14120
 */
public final class ProjectionRegistry {

    /**
     * The projection method used.
     * Use {@link #getProjection()} and {@link #setProjection(Projection)} for access.
     * Use {@link #setProjection(Projection)} in order to trigger a projection change event.
     */
    private static volatile Projection proj;

    private static ProjectionBoundsProvider boundsProvider;

    /*
     * Keep WeakReferences to the listeners. This relieves clients from the burden of
     * explicitly removing the listeners and allows us to transparently register every
     * created dataset as projection change listener.
     */
    private static final List<WeakReference<ProjectionChangeListener>> listeners = new CopyOnWriteArrayList<>();

    private ProjectionRegistry() {
        // hide constructor
    }

    /**
     * Replies the current projection.
     *
     * @return the currently active projection
     */
    public static Projection getProjection() {
        return proj;
    }

    /**
     * Sets the current projection
     *
     * @param p the projection
     */
    public static void setProjection(Projection p) {
        CheckParameterUtil.ensureParameterNotNull(p);
        Projection oldValue = proj;
        Bounds b = boundsProvider != null ? boundsProvider.getRealBounds() : null;
        proj = p;
        fireProjectionChanged(oldValue, proj, b);
    }

    private static void fireProjectionChanged(Projection oldValue, Projection newValue, Bounds oldBounds) {
        if ((newValue == null ^ oldValue == null)
                || (newValue != null && oldValue != null && !Objects.equals(newValue.toCode(), oldValue.toCode()))) {
            listeners.removeIf(x -> x.get() == null);
            listeners.stream().map(WeakReference::get).filter(Objects::nonNull).forEach(x -> x.projectionChanged(oldValue, newValue));
            if (newValue != null && oldBounds != null && boundsProvider != null) {
                boundsProvider.restoreOldBounds(oldBounds);
            }
            /* TODO - remove layers with fixed projection */
        }
    }

    /**
     * Register a projection change listener.
     * The listener is registered to be weak, so keep a reference of it if you want it to be preserved.
     *
     * @param listener the listener. Ignored if <code>null</code>.
     */
    public static void addProjectionChangeListener(ProjectionChangeListener listener) {
        if (listener == null) return;
        for (WeakReference<ProjectionChangeListener> wr : listeners) {
            // already registered ? => abort
            if (wr.get() == listener) return;
        }
        listeners.add(new WeakReference<>(listener));
    }

    /**
     * Removes a projection change listener.
     *
     * @param listener the listener. Ignored if <code>null</code>.
     */
    public static void removeProjectionChangeListener(ProjectionChangeListener listener) {
        if (listener == null) return;
        // remove the listener - and any other listener which got garbage collected in the meantime
        listeners.removeIf(wr -> wr.get() == null || wr.get() == listener);
    }

    /**
     * Remove all projection change listeners. For testing purposes only.
     */
    public static void clearProjectionChangeListeners() {
        listeners.clear();
    }

    /**
     * Returns the bounds provider called in projection events.
     * @return the bounds provider
     */
    public static ProjectionBoundsProvider getBoundsProvider() {
        return boundsProvider;
    }

    /**
     * Sets the bounds provider called in projection events. Must not be null
     * @param provider the bounds provider
     */
    public static void setboundsProvider(ProjectionBoundsProvider provider) {
        boundsProvider = Objects.requireNonNull(provider);
    }
}
